#!/usr/bin/env python3
"""List running Liferay-related processes: portal (Tomcat) instances and `ant` builds."""

from __future__ import annotations

import argparse
import getpass
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass, field

import psutil


# Schemas MySQL ships with — never user data.
DEFAULT_MYSQL_SCHEMAS = {"information_schema", "mysql", "performance_schema", "sys"}

# Where portal source checkouts live. Override with --projects-dir.
DEFAULT_PROJECTS_DIR = os.path.expanduser("~/dev/projects")

# Configuration file (JSON). The per-user file wins if it exists; otherwise we
# fall back to the default shipped alongside this script. Override with --config.
USER_CONFIG_PATH = os.path.expanduser("~/.config/liferay_ps/config.json")
BUNDLED_CONFIG_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "config.json")


def default_config_path() -> str:
    return USER_CONFIG_PATH if os.path.isfile(USER_CONFIG_PATH) else BUNDLED_CONFIG_PATH

# Color only when writing to a real terminal and the user hasn't opted out.
_USE_COLOR = sys.stdout.isatty() and not os.environ.get("NO_COLOR")


def _warn(text: str) -> str:
    return f"\x1b[1;31m{text}\x1b[0m" if _USE_COLOR else text


@dataclass
class Config:
    # Portal source directory names to skip entirely (matched against the checkout
    # directory name under --projects-dir, e.g. "liferay-portal-7.0.x").
    ignored_repos: set[str] = field(default_factory=set)


def load_config(path: str) -> Config:
    """Load configuration from a JSON file. A missing file yields an empty config;
    a malformed one is reported to stderr but never aborts the run."""
    try:
        with open(path) as fh:
            data = json.load(fh)
    except FileNotFoundError:
        return Config()
    except (OSError, ValueError) as e:
        print(_warn(f"! ignoring unreadable config {path}: {e}"), file=sys.stderr)
        return Config()
    if not isinstance(data, dict):
        print(_warn(f"! config {path}: top level must be a JSON object"), file=sys.stderr)
        return Config()
    ignored = data.get("ignored_repositories", [])
    if not isinstance(ignored, list):
        print(
            _warn(f"! config {path}: 'ignored_repositories' must be a list"),
            file=sys.stderr,
        )
        ignored = []
    return Config(ignored_repos={str(r) for r in ignored})

SCHEMA_COUNT_QUERY = (
    "SELECT TABLE_SCHEMA, COUNT(*), MAX(TABLE_NAME = 'Company') "
    "FROM information_schema.tables "
    "GROUP BY TABLE_SCHEMA ORDER BY TABLE_SCHEMA;"
)

# Liferay partition schemas: "lpartition_<companyId>", with companyId being numeric.
_PARTITION_RE = re.compile(r"^lpartition_(\d+)$")

# Schema name out of a jdbc URL: jdbc:mysql://host[:port]/<schema>?...
_JDBC_SCHEMA_RE = re.compile(r"jdbc:(?:mysql|mariadb)://[^/]+/([^?;\s]+)", re.IGNORECASE)


@dataclass
class Hit:
    proc: psutil.Process
    kind: str  # "portal" | "tomcat" | "ant" | "upgrade"
    label: str
    ports: list[int] = field(default_factory=list)
    cwd: str = ""


@dataclass
class DbHit:
    schema: str
    tables: int
    # Set on partition schemas (name matches lpartition_<digits>).
    company_id: int | None = None
    # The main schema that owns this partition (companyId found in its Company table).
    # None on mains, or on orphan partitions whose companyId isn't in any main.
    parent: str | None = None
    has_company: bool = False

    @property
    def is_partition(self) -> bool:
        return self.company_id is not None


@dataclass
class DbResult:
    source: str  # human-readable description of where we got the data
    schemas: list[DbHit] = field(default_factory=list)
    error: str = ""


@dataclass
class PortalSource:
    path: str
    name: str
    branch: str = ""
    remote: str = ""
    is_worktree: bool = False  # git worktree (`.git` is a file, not a dir)
    target_dir: str = ""  # build/deploy target — app.server.parent.dir, resolved
    app_server_dir: str = ""  # the tomcat-* bundle dir inside target_dir, if present
    target_exists: bool = False
    # pid of a portal/tomcat process running out of this source's target, if any.
    running_pid: int | None = None
    last_commit_epoch: int | None = None  # committer date of HEAD, unix seconds
    last_commit_rel: str = ""  # git's relative form, e.g. "6 hours ago"
    dirty: bool = False  # working tree has uncommitted changes
    db_schema_name: str = ""  # schema from the bundle's jdbc.default.url, if any


def _cmdline(p: psutil.Process) -> list[str]:
    try:
        return p.cmdline() or []
    except (psutil.NoSuchProcess, psutil.AccessDenied):
        return []


def _cwd(p: psutil.Process) -> str:
    try:
        return p.cwd() or ""
    except (psutil.NoSuchProcess, psutil.AccessDenied):
        return ""


def _catalina_base(cmd: list[str]) -> str | None:
    for arg in cmd:
        if arg.startswith("-Dcatalina.base="):
            return arg.split("=", 1)[1]
    return None


def _upgrade_bundle(cmd: list[str]) -> str | None:
    """Derive the Liferay bundle root from a db-upgrade-client cmdline, if possible."""
    for arg in cmd:
        if arg.startswith("-Dliferay.shielded.container.lib.portal.dir="):
            path = arg.split("=", 1)[1]
            # Path looks like <bundle>/tomcat-X.Y.Z/.../shielded-container-lib —
            # strip from the first /tomcat- segment to get the bundle root.
            idx = path.find("/tomcat-")
            return path[:idx] if idx > 0 else path
    return None


def _classify(p: psutil.Process) -> Hit | None:
    cmd = _cmdline(p)
    if not cmd:
        return None
    joined = " ".join(cmd)

    # Portal / Tomcat: a Java process running Catalina's Bootstrap.
    if "org.apache.catalina.startup.Bootstrap" in joined:
        base = _catalina_base(cmd) or _cwd(p)
        label = base or "tomcat"
        # Heuristic: only flag as Liferay if path hints at it; otherwise still
        # show it but mark it so the user can tell.
        looks_liferay = any(
            tok in base.lower() for tok in ("liferay", "portal", "bundles")
        ) if base else False
        kind = "portal" if looks_liferay else "tomcat"
        return Hit(proc=p, kind=kind, label=label, cwd=_cwd(p))

    # Liferay DB upgrade: launcher jar, main class, or the wrapper shell script.
    # All three appear at once for a single upgrade (bash → java -jar → forked java)
    # — that's intentional, since each one is a real process the user may want to see.
    is_upgrade_java = "com.liferay.portal.tools.db.upgrade" in joined
    is_upgrade_sh = any(
        os.path.basename(a).startswith("db_upgrade") and a.endswith(".sh") for a in cmd
    )
    if is_upgrade_java or is_upgrade_sh:
        bundle = _upgrade_bundle(cmd) or _cwd(p)
        label = bundle or "db-upgrade"
        return Hit(proc=p, kind="upgrade", label=label, cwd=_cwd(p))

    # Ant build: org.apache.tools.ant.launch.Launcher invoked by the `ant` wrapper.
    if "org.apache.tools.ant.launch.Launcher" in joined or (
        cmd and os.path.basename(cmd[0]) == "ant"
    ):
        # Pull the targets (args that aren't options) so "ant all" stands out.
        targets = [a for a in cmd[1:] if not a.startswith("-")]
        label = "ant " + " ".join(targets) if targets else "ant"
        return Hit(proc=p, kind="ant", label=label.strip(), cwd=_cwd(p))

    return None


def _listening_ports_by_pid() -> dict[int, list[int]]:
    """Map pid -> sorted listening TCP ports. Requires permission to read conns."""
    out: dict[int, set[int]] = {}
    try:
        conns = psutil.net_connections(kind="tcp")
    except (psutil.AccessDenied, PermissionError):
        return {}
    for c in conns:
        if c.status != psutil.CONN_LISTEN or c.pid is None or c.laddr is None:
            continue
        out.setdefault(c.pid, set()).add(c.laddr.port)
    return {pid: sorted(ports) for pid, ports in out.items()}


# pid -> (sample_time, java_state_counts_or_None). None means "we tried, jstack
# didn't work — don't show Java-level info but skip re-trying for the TTL window".
_JSTACK_CACHE: dict[int, tuple[float, dict[str, int] | None]] = {}
_JSTACK_TTL = 10.0  # seconds; jstack triggers a JVM safepoint so we cache aggressively


def _jstack_states(pid: int) -> dict[str, int] | None:
    """Cached count of Java thread states (RUNNABLE, BLOCKED, WAITING, ...) via jstack.
    Returns None when jstack isn't available or fails."""
    now = time.time()
    cached = _JSTACK_CACHE.get(pid)
    if cached is not None and now - cached[0] < _JSTACK_TTL:
        return cached[1]

    try:
        java_exe = psutil.Process(pid).exe()
    except (psutil.NoSuchProcess, psutil.AccessDenied):
        _JSTACK_CACHE[pid] = (now, None)
        return None
    jstack = os.path.join(os.path.dirname(java_exe), "jstack")
    if not os.path.exists(jstack):
        _JSTACK_CACHE[pid] = (now, None)
        return None

    try:
        result = subprocess.run(
            [jstack, str(pid)], capture_output=True, text=True, timeout=10,
        )
    except (subprocess.SubprocessError, OSError):
        _JSTACK_CACHE[pid] = (now, None)
        return None
    if result.returncode != 0:
        _JSTACK_CACHE[pid] = (now, None)
        return None

    counts: dict[str, int] = {}
    marker = "java.lang.Thread.State:"
    for line in result.stdout.splitlines():
        i = line.find(marker)
        if i < 0:
            continue
        state = line[i + len(marker):].strip().split(" ", 1)[0]
        if state:
            counts[state] = counts.get(state, 0) + 1
    _JSTACK_CACHE[pid] = (now, counts)
    return counts


def _thread_info(pid: int) -> tuple[dict[str, int], dict[str, int] | None]:
    """Returns (os_states, java_states_or_None). OS states come from /proc; Java
    states from jstack (cached). Either may be empty if unavailable."""
    task_dir = f"/proc/{pid}/task"
    try:
        tids = os.listdir(task_dir)
    except OSError:
        return {}, None
    states: dict[str, int] = {}
    for tid in tids:
        try:
            raw = open(f"{task_dir}/{tid}/stat", "rb").read()
        except OSError:
            continue
        # `comm` field is wrapped in parens and may itself contain ')' — use rfind.
        idx = raw.rfind(b") ")
        if idx < 0:
            continue
        state = raw[idx + 2:idx + 3].decode("ascii", "replace")
        states[state] = states.get(state, 0) + 1
    return states, _jstack_states(pid)


def _thread_summary(os_states: dict[str, int], java_states: dict[str, int] | None) -> str:
    """Short summary like 'threads=487 R=2 B=3' built from pre-collected state counts."""
    total = sum(os_states.values())
    if not total:
        return ""
    parts = [f"threads={total}", f"R={os_states.get('R', 0)}"]
    # Java BLOCKED (waiting on a monitor) is invisible at the OS level — all such
    # threads appear as 'S'. jstack is the only way to surface it.
    if java_states is not None:
        parts.append(f"B={java_states.get('BLOCKED', 0)}")
    # Surface unusual OS states only when present, to keep the common case compact.
    for s in ("D", "Z", "T"):
        if os_states.get(s):
            parts.append(f"{s}={os_states[s]}")
    return " ".join(parts)


def _thread_problems(
    os_states: dict[str, int], java_states: dict[str, int] | None
) -> list[str]:
    """Human-readable warnings derived from thread state counts. Empty when healthy."""
    problems: list[str] = []
    if java_states is not None:
        b = java_states.get("BLOCKED", 0)
        if b > 0:
            problems.append(
                f"{b} BLOCKED thread{'s' if b != 1 else ''} — possible lock contention"
            )
    d = os_states.get("D", 0)
    if d > 0:
        problems.append(
            f"{d} thread{'s' if d != 1 else ''} in uninterruptible I/O wait (disk stall?)"
        )
    z = os_states.get("Z", 0)
    if z > 0:
        problems.append(f"{z} zombie thread{'s' if z != 1 else ''}")
    t = os_states.get("T", 0)
    if t > 0:
        problems.append(
            f"{t} thread{'s' if t != 1 else ''} stopped (debugger attached or SIGSTOP)"
        )
    return problems


def _uptime(p: psutil.Process) -> str:
    try:
        secs = int(time.time() - p.create_time())
    except (psutil.NoSuchProcess, psutil.AccessDenied):
        return "?"
    days, rem = divmod(secs, 86400)
    hours, rem = divmod(rem, 3600)
    minutes, _ = divmod(rem, 60)
    if days:
        return f"{days}d{hours:02d}h"
    if hours:
        return f"{hours}h{minutes:02d}m"
    return f"{minutes}m"


def _onepassword_running() -> bool:
    """True if the 1Password desktop app appears to be running.

    On Linux the app runs as a process named like ``1password`` (sometimes with a
    suffix such as ``1password-gui``). Match case-insensitively on that prefix."""
    for p in psutil.process_iter(["name"]):
        try:
            name = (p.info["name"] or "").lower()
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            continue
        if name.startswith("1password"):
            return True
    return False


def find_hits() -> list[Hit]:
    ports_by_pid = _listening_ports_by_pid()
    hits: list[Hit] = []
    for p in psutil.process_iter(["pid", "name"]):
        try:
            hit = _classify(p)
        except psutil.NoSuchProcess:
            continue
        if hit is None:
            continue
        hit.ports = ports_by_pid.get(p.pid, [])
        hits.append(hit)
    # Portal first, then tomcat, upgrade, ant; within each, sort by pid for stability.
    order = {"portal": 0, "tomcat": 1, "upgrade": 2, "ant": 3}
    hits.sort(key=lambda h: (order.get(h.kind, 9), h.proc.pid))
    return hits


def _git(path: str, *args: str) -> str:
    """Run a read-only git command in ``path``; return stripped stdout or ""."""
    try:
        r = subprocess.run(
            ["git", "-C", path, *args],
            capture_output=True, text=True, timeout=5,
        )
    except (subprocess.SubprocessError, OSError):
        return ""
    return r.stdout.strip() if r.returncode == 0 else ""


def _prop(path: str, key: str) -> str:
    """Read a single ``key=value`` from a .properties file. Skips comments and
    leading whitespace; returns "" if the file or key is absent."""
    try:
        with open(path) as fh:
            for line in fh:
                s = line.strip()
                if s.startswith("#") or "=" not in s:
                    continue
                k, _, v = s.partition("=")
                if k.strip() == key:
                    return v.strip()
    except OSError:
        return ""
    return ""


def _build_target(source_dir: str, user: str) -> str:
    """Resolve a portal source's build/deploy target (``app.server.parent.dir``).

    The per-user ``app.server.<user>.properties`` overrides the checked-in
    ``app.server.properties``; if neither sets it, fall back to the portal
    default of ``${project.dir}/../bundles``. ``${project.dir}`` resolves to the
    source directory itself."""
    raw = ""
    if user:
        raw = _prop(os.path.join(source_dir, f"app.server.{user}.properties"),
                    "app.server.parent.dir")
    if not raw:
        raw = _prop(os.path.join(source_dir, "app.server.properties"),
                    "app.server.parent.dir")
    if not raw:
        raw = "${project.dir}/../bundles"
    resolved = raw.replace("${project.dir}", source_dir)
    # Only normalize when fully resolved — an unknown ${...} would be mangled.
    return os.path.normpath(resolved) if "${" not in resolved else resolved


def _active_jdbc_schema(properties_path: str) -> str:
    """Schema name from the first uncommented ``jdbc.default.url`` in a
    portal-ext.properties. Returns "" if absent."""
    try:
        with open(properties_path) as fh:
            for line in fh:
                s = line.strip()
                if s.startswith("#") or not s.lower().startswith("jdbc.default.url"):
                    continue
                m = _JDBC_SCHEMA_RE.search(s)
                if m:
                    return m.group(1)
    except OSError:
        return ""
    return ""


def find_portal_sources(
    root: str = DEFAULT_PROJECTS_DIR, ignored_repos: set[str] | None = None
) -> list[PortalSource]:
    """Discover portal source checkouts under ``root``: directories that are git
    repos (or worktrees) and carry an ``app.server.properties`` marker. Directory
    names in ``ignored_repos`` are skipped."""
    ignored_repos = ignored_repos or set()
    user = os.environ.get("USER") or getpass.getuser()
    sources: list[PortalSource] = []
    try:
        entries = sorted(os.listdir(root))
    except OSError:
        return []
    for name in entries:
        if name in ignored_repos:
            continue
        path = os.path.join(root, name)
        git_marker = os.path.join(path, ".git")
        if not os.path.exists(git_marker):
            continue
        # Portal source heuristic: the build entry point lives here.
        if not os.path.isfile(os.path.join(path, "app.server.properties")):
            continue
        src = PortalSource(
            path=path, name=name, is_worktree=os.path.isfile(git_marker),
        )
        branch = _git(path, "rev-parse", "--abbrev-ref", "HEAD")
        if branch == "HEAD":  # detached — show the short commit instead
            sha = _git(path, "rev-parse", "--short", "HEAD")
            branch = f"detached@{sha}" if sha else "detached"
        src.branch = branch
        src.remote = _git(path, "config", "--get", "remote.origin.url")
        # Last commit: epoch (for the 30-day filter) + git's relative form (tab-sep).
        info = _git(path, "log", "-1", "--format=%ct%x09%cr")
        if info:
            ts, _, rel = info.partition("\t")
            src.last_commit_rel = rel
            try:
                src.last_commit_epoch = int(ts)
            except ValueError:
                pass
        # Dirty = modified/staged tracked files. `-uno` skips enumerating untracked
        # files, which on a portal tree is ~16x faster (1s -> 60ms) and only costs us
        # flagging dirt from untracked-but-gitignored build artifacts we don't care
        # about. `--no-optional-locks` avoids contending for the index lock.
        src.dirty = bool(
            _git(path, "--no-optional-locks", "status", "--porcelain", "-uno")
        )
        target = _build_target(path, user)
        src.target_dir = target
        src.target_exists = bool(target) and os.path.isdir(target)
        if src.target_exists:
            tomcats = sorted(
                t for t in glob.glob(os.path.join(target, "tomcat-*"))
                if os.path.isdir(t)
            )
            if tomcats:
                src.app_server_dir = tomcats[-1]  # newest version on top
        if target:
            src.db_schema_name = _active_jdbc_schema(
                os.path.join(target, "portal-ext.properties")
            )
        sources.append(src)
    return sources


def _find_mysql_container() -> tuple[str, str] | None:
    """Return (container_name, root_password) for a running MySQL/MariaDB container, or None."""
    if not shutil.which("docker"):
        return None
    try:
        out = subprocess.run(
            ["docker", "ps", "--format", "{{.Names}}\t{{.Image}}"],
            capture_output=True, text=True, timeout=5, check=True,
        ).stdout
    except (subprocess.SubprocessError, OSError):
        return None
    for line in out.splitlines():
        name, _, image = line.partition("\t")
        if not name:
            continue
        if "mysql" not in image.lower() and "mariadb" not in image.lower():
            continue
        try:
            inspect = subprocess.run(
                ["docker", "inspect", name], capture_output=True, text=True,
                timeout=5, check=True,
            ).stdout
            envs = json.loads(inspect)[0].get("Config", {}).get("Env", []) or []
        except (subprocess.SubprocessError, OSError, ValueError, IndexError, KeyError):
            continue
        password = ""
        for e in envs:
            if e.startswith("MYSQL_ROOT_PASSWORD="):
                password = e.split("=", 1)[1]
                break
            if e.startswith("MARIADB_ROOT_PASSWORD="):
                password = e.split("=", 1)[1]
                break
        return name, password
    return None


def _run_mysql(container: str, password: str, query: str, timeout: float = 10.0) -> tuple[str, str, int]:
    """Run a query via `docker exec mysql`. Returns (stdout, stderr_filtered, returncode).
    stderr has the noisy 'Using a password' warning filtered out."""
    cmd = ["docker", "exec", "-i", container, "mysql", "-uroot"]
    if password:
        cmd.append(f"-p{password}")
    cmd += ["-N", "-B", "-e", query]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    err = "\n".join(
        ln for ln in result.stderr.splitlines() if "Using a password" not in ln
    ).strip()
    return result.stdout, err, result.returncode


def _load_company_map(container: str, password: str, mains_with_company: list[str]) -> dict[int, str]:
    """Map companyId -> main schema name. One UNION ALL query covers every main."""
    if not mains_with_company:
        return {}
    # Schema names come straight from information_schema, so injection risk is low,
    # but backtick-quote identifiers and escape backticks to be safe.
    def ident(s: str) -> str:
        return "`" + s.replace("`", "``") + "`"
    parts = [
        f"SELECT '{s.replace(chr(39), chr(39)+chr(39))}' AS s, companyId FROM {ident(s)}.Company"
        for s in mains_with_company
    ]
    query = " UNION ALL ".join(parts) + ";"
    try:
        out, _, rc = _run_mysql(container, password, query, timeout=10.0)
    except (subprocess.SubprocessError, OSError):
        return {}
    if rc != 0:
        return {}
    result: dict[int, str] = {}
    for line in out.splitlines():
        schema, _, cid = line.partition("\t")
        if not schema or not cid:
            continue
        try:
            result[int(cid)] = schema
        except ValueError:
            continue
    return result


def find_db_hits(cached: list | None = None) -> DbResult:
    """Query schema/table counts. ``cached`` is a one-element list used as an in/out
    slot for the container info so follow-mode can avoid re-running docker ps/inspect
    on every refresh. Pass None to force a fresh lookup."""
    found = cached[0] if cached else None
    if found is None:
        found = _find_mysql_container()
        if cached is not None:
            cached[:] = [found]
    if found is None:
        return DbResult(source="(no MySQL/MariaDB container found)", error="not found")
    container, password = found
    try:
        out, err, rc = _run_mysql(container, password, SCHEMA_COUNT_QUERY)
    except (subprocess.SubprocessError, OSError) as e:
        if cached is not None:
            cached[:] = [None]  # force re-detect next time
        return DbResult(source=container, error=str(e))
    if rc != 0:
        if cached is not None:
            cached[:] = [None]  # container may be gone; re-detect next time
        return DbResult(source=container, error=err or "mysql exited non-zero")

    schemas: list[DbHit] = []
    for line in out.splitlines():
        parts = line.split("\t")
        if len(parts) < 2:
            continue
        schema = parts[0]
        if schema in DEFAULT_MYSQL_SCHEMAS:
            continue
        try:
            count = int(parts[1])
        except ValueError:
            continue
        has_company = len(parts) >= 3 and parts[2] == "1"
        hit = DbHit(schema=schema, tables=count, has_company=has_company)
        m = _PARTITION_RE.match(schema)
        if m:
            hit.company_id = int(m.group(1))
        schemas.append(hit)

    # Resolve partition → main by looking up each partition's suffix-as-companyId in
    # every non-partition schema's Company table.
    mains_with_company = [
        s.schema for s in schemas if not s.is_partition and s.has_company
    ]
    company_map = _load_company_map(container, password, mains_with_company)
    for s in schemas:
        if s.is_partition and s.company_id in company_map:
            s.parent = company_map[s.company_id]

    return DbResult(source=container, schemas=schemas)


def _print_db_section(title: str, db: DbResult) -> None:
    suffix = f" [{db.source}]" if db.source else ""
    header = f"{title}{suffix}"
    print(f"\n{header}")
    print("-" * len(header))
    if db.error:
        print(f"  (unavailable: {db.error})")
        return
    if not db.schemas:
        print("  (none)")
        return

    mains = [s for s in db.schemas if not s.is_partition]
    children_by_parent: dict[str, list[DbHit]] = {}
    orphans: list[DbHit] = []
    for s in db.schemas:
        if not s.is_partition:
            continue
        if s.parent:
            children_by_parent.setdefault(s.parent, []).append(s)
        else:
            orphans.append(s)
    # Stable order within each parent: by companyId numerically.
    for kids in children_by_parent.values():
        kids.sort(key=lambda h: h.company_id or 0)

    for m in mains:
        print(f"  {m.tables:<6} {m.schema}")
        for child in children_by_parent.get(m.schema, []):
            print(f"           └ {child.tables:<6} {child.schema}")
    for o in orphans:
        print(f"  {o.tables:<6} {o.schema}  (orphan)")


def _print_portal_group(
    s: PortalSource,
    server: Hit | None,
    builds: list[Hit],
    upgrades: list[Hit],
    db_schemas: list[DbHit],
    show_cwd: bool,
) -> None:
    """Print one portal source and everything that hangs off it: build target,
    running instance, in-flight builds/upgrades, and database."""
    wt = " (worktree)" if s.is_worktree else ""
    dirty = "  (dirty)" if s.dirty else ""
    status_plain = "  ● RUNNING" if server else ""
    head_plain = f"{s.name}{wt}  @{s.branch or '?'}{dirty}{status_plain}"
    status = _warn(status_plain) if server else ""
    print(f"\n{s.name}{wt}  @{s.branch or '?'}{dirty}{status}")
    print("-" * len(head_plain))

    upd = f"  (updated {s.last_commit_rel})" if s.last_commit_rel else ""
    print(f"  source:   {s.path}{upd}")
    if s.remote:
        print(f"  repo:     {s.remote}")
    if s.target_dir:
        built = "" if s.target_exists else "  (not built)"
        srv = f"  [{os.path.basename(s.app_server_dir)}]" if s.app_server_dir else ""
        print(f"  build:    {s.target_dir}{built}{srv}")

    if server:
        descr, problems = _proc_descr(server)
        print(f"  portal:   {descr}")
        for p in problems:
            print(f"            {_warn('! ' + p)}")
    for h in builds:
        descr, _ = _proc_descr(h)
        print(f"  building: {descr}  {h.label}")
    for h in upgrades:
        descr, _ = _proc_descr(h)
        print(f"  upgrade:  {descr}")

    if db_schemas:
        main = db_schemas[0]
        print(f"  DB:       {main.tables} tables  {main.schema}")
        for kid in db_schemas[1:]:
            print(f"              └ {kid.tables} tables  {kid.schema}")
    elif s.db_schema_name:
        print(f"  DB:       {s.db_schema_name}  (not created yet)")


def _print_portals_grouped(
    sources: list[PortalSource],
    portals: list[Hit],
    tomcats: list[Hit],
    upgrades: list[Hit],
    ants: list[Hit],
    db: DbResult,
    args,
) -> None:
    """Render everything grouped per portal source, then trailing sections for
    processes and databases that didn't map to a known source."""
    norm = os.path.normpath
    used_hits: set[int] = set()
    used_schemas: set[str] = set()

    def base_of(h: Hit) -> str:
        return norm(h.label or h.cwd) if (h.label or h.cwd) else ""

    # A running portal/tomcat belongs to the source whose app server dir it runs from.
    server_by_source: dict[str, Hit] = {}
    for s in sources:
        if not s.app_server_dir:
            continue
        tgt = norm(s.app_server_dir)
        for h in portals + tomcats:
            if id(h) in used_hits:
                continue
            base = base_of(h)
            if base == tgt or base.startswith(tgt + os.sep):
                server_by_source[s.path] = h
                s.running_pid = h.proc.pid
                used_hits.add(id(h))
                break

    # An ant build belongs to the source it runs inside (longest path match wins).
    def assign_by_path(hits: list[Hit], key) -> dict[str, list[Hit]]:
        out: dict[str, list[Hit]] = {}
        for h in hits:
            target = norm(h.cwd or "") if key == "cwd" else base_of(h)
            best: PortalSource | None = None
            for s in sources:
                anchor = norm(s.path) if key == "cwd" else norm(s.target_dir or "")
                if not anchor:
                    continue
                if target == anchor or target.startswith(anchor + os.sep):
                    if best is None or len(anchor) > len(
                        norm(best.path if key == "cwd" else (best.target_dir or ""))
                    ):
                        best = s
            if best is not None:
                out.setdefault(best.path, []).append(h)
                used_hits.add(id(h))
        return out

    builds_by_source = assign_by_path(ants, "cwd")
    upgrades_by_source = assign_by_path(upgrades, "target")

    # A schema belongs to the source whose bundle jdbc URL points at it; its
    # partitions ride along.
    schema_index = {sc.schema: sc for sc in db.schemas}
    children_by_parent: dict[str, list[DbHit]] = {}
    for sc in db.schemas:
        if sc.parent:
            children_by_parent.setdefault(sc.parent, []).append(sc)
    db_by_source: dict[str, list[DbHit]] = {}
    for s in sources:
        main = schema_index.get(s.db_schema_name) if s.db_schema_name else None
        if main is None:
            continue
        kids = sorted(children_by_parent.get(main.schema, []),
                      key=lambda h: h.company_id or 0)
        db_by_source[s.path] = [main] + kids
        used_schemas.add(main.schema)
        used_schemas.update(k.schema for k in kids)

    print("\nPortals")
    print("=======")
    if not sources:
        print("  (no portal sources)")
    for s in sources:
        _print_portal_group(
            s, server_by_source.get(s.path), builds_by_source.get(s.path, []),
            upgrades_by_source.get(s.path, []), db_by_source.get(s.path, []), args.cwd,
        )

    # Trailing sections: anything that didn't map to a source.
    orphan_portals = [h for h in portals if id(h) not in used_hits]
    other_tomcats = [h for h in tomcats if id(h) not in used_hits]
    orphan_upgrades = [h for h in upgrades if id(h) not in used_hits]
    orphan_builds = [h for h in ants if id(h) not in used_hits]
    any_portal = bool(server_by_source) or bool(orphan_portals)

    if orphan_portals:
        _print_section("Portal instances (no matching source)", orphan_portals, args.cwd)
    if other_tomcats and (args.all_tomcat or not any_portal):
        _print_section("Other Tomcat instances", other_tomcats, args.cwd)
    if orphan_upgrades:
        _print_section("DB upgrades (no matching source)", orphan_upgrades, args.cwd)
    if orphan_builds:
        _print_section("Builds (no matching source)", orphan_builds, args.cwd)

    leftover = DbResult(
        source=db.source, error=db.error,
        schemas=[sc for sc in db.schemas if sc.schema not in used_schemas],
    )
    title = "Other DBs" if used_schemas else "Liferay DBs"
    if leftover.schemas or leftover.error or not used_schemas:
        _print_db_section(title, leftover)


def _proc_descr(h: Hit) -> tuple[str, list[str]]:
    """Compact process descriptor ('pid=.. up=.. ports=.. threads=..') plus any
    thread-health warnings. Thread info is only gathered for portal/tomcat."""
    ports = ",".join(str(p) for p in h.ports) if h.ports else "-"
    extra = ""
    problems: list[str] = []
    if h.kind in ("portal", "tomcat"):
        os_states, java_states = _thread_info(h.proc.pid)
        ts = _thread_summary(os_states, java_states)
        if ts:
            extra = f"  {ts}"
        problems = _thread_problems(os_states, java_states)
    return f"pid={h.proc.pid} up={_uptime(h.proc)} ports={ports}{extra}", problems


def _print_section(title: str, hits: list[Hit], show_cwd: bool) -> None:
    print(f"\n{title}")
    print("-" * len(title))
    if not hits:
        print("  (none)")
        return
    for h in hits:
        descr, problems = _proc_descr(h)
        print(f"  {descr}  {h.label}")
        for problem in problems:
            print(f"          {_warn('! ' + problem)}")
        if show_cwd and h.cwd and h.cwd != h.label:
            print(f"          cwd: {h.cwd}")


def _render(args, db_cache: list | None = None) -> tuple[int, list[Hit], list[Hit], list[Hit], list[Hit], DbResult, list[PortalSource]]:
    if not _onepassword_running():
        print(_warn("! 1Password is not running — secrets/credentials may be unavailable."))

    hits = find_hits()
    portals = [h for h in hits if h.kind == "portal"]
    tomcats = [h for h in hits if h.kind == "tomcat"]
    upgrades = [h for h in hits if h.kind == "upgrade"]
    ants = [h for h in hits if h.kind == "ant"]

    sources = find_portal_sources(args.projects_dir, args.config.ignored_repos)
    db = find_db_hits(cached=db_cache)

    _print_portals_grouped(sources, portals, tomcats, upgrades, ants, db, args)

    return 0, portals, tomcats, upgrades, ants, db, sources


def _follow(args) -> int:
    interval = max(0.5, args.interval)
    db_cache: list = [None]  # in/out slot for cached container info
    try:
        while True:
            # ANSI: clear screen + home cursor. Avoids the flicker of `clear`.
            sys.stdout.write("\x1b[H\x1b[2J")
            print(
                f"liferay_ps -f  every {interval:g}s  "
                f"{time.strftime('%H:%M:%S')}  (Ctrl-C to quit)"
            )
            _render(args, db_cache=db_cache)
            sys.stdout.flush()
            time.sleep(interval)
    except KeyboardInterrupt:
        print()
        return 0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--cwd", action="store_true", help="show each process's cwd")
    ap.add_argument(
        "--all-tomcat",
        action="store_true",
        help="include Tomcats that don't look Liferay-related",
    )
    ap.add_argument(
        "-f", "--follow",
        action="store_true",
        help="continuously refresh until Ctrl-C",
    )
    ap.add_argument(
        "-n", "--interval",
        type=float, default=3.0, metavar="SEC",
        help="refresh interval for --follow (default: 3.0s)",
    )
    ap.add_argument(
        "--projects-dir",
        default=DEFAULT_PROJECTS_DIR, metavar="DIR",
        help=f"where portal source checkouts live (default: {DEFAULT_PROJECTS_DIR})",
    )
    ap.add_argument(
        "--config",
        dest="config_path", default=None, metavar="FILE",
        help=f"JSON config file (default: {USER_CONFIG_PATH}, "
             f"falling back to the bundled {BUNDLED_CONFIG_PATH})",
    )
    args = ap.parse_args()
    args.config = load_config(args.config_path or default_config_path())

    if args.follow:
        return _follow(args)

    _, portals, tomcats, upgrades, ants, db, sources = _render(args)

    if (not portals and not tomcats and not upgrades and not ants
            and not db.schemas and not sources):
        print("\nNo Liferay-related processes found.", file=sys.stderr)
        return 1
    if not psutil.net_connections.__doc__ or not _listening_ports_by_pid():
        # Soft hint when we couldn't read ports — usually a perms thing.
        if any(not h.ports for h in portals + tomcats):
            print(
                "\nNote: some ports may be hidden — re-run with sudo to see all listening sockets.",
                file=sys.stderr,
            )
    return 0


if __name__ == "__main__":
    sys.exit(main())
