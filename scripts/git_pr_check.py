#!/usr/bin/env python3

import subprocess
import sys
import os
from datetime import datetime
import difflib
import re
import time
from concurrent.futures import ThreadPoolExecutor
from html import escape as html_escape

def get_current_branch():
    cmd = ['git', 'rev-parse', '--abbrev-ref', 'HEAD']
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("Error getting current branch:", result.stderr)
        sys.exit(1)
    return result.stdout.strip()

def get_head_sha():
    cmd = ['git', 'rev-parse', 'HEAD']
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("Error getting HEAD SHA:", result.stderr)
        sys.exit(1)
    return result.stdout.strip()

def get_merge_base_sha(branch):
    cmd = ['git', 'rev-list', '--boundary', f'{branch}...master']
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0 or not result.stdout.strip():
        print("Error getting merge base SHA:", result.stderr)
        sys.exit(1)
    # Find lines starting with '-' and strip the '-'
    for line in result.stdout.strip().split('\n'):
        if line.startswith('-'):
            return line[1:]
    print("Merge base SHA not found.")
    sys.exit(1)

def get_commit_details(sha1, sha2):
    cmd = [
        'git', 'log', '--pretty=format:%H|%an|%al|%ad|%cn|%cl|%cd|%s', '--date=iso', '--reverse', f'{sha1}..{sha2}'
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("Error getting commit details:", result.stderr)
        sys.exit(1)
    commits = []
    for line in result.stdout.strip().split('\n'):
        if line:
            parts = line.split('|', 7)
            if len(parts) == 8:
                commits.append({
                    'hash': parts[0],
                    'author': parts[1],
                    'authorId': parts[2],
                    'date': parts[3],
                    'committer': parts[4],
                    'committerId': parts[5],
                    'commitDate': parts[6],
                    'message': parts[7]
                })
    return commits

def get_changed_files(sha1, sha2):
    cmd = ['git', 'diff', '--name-only', sha1, sha2]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("Error running git diff:", result.stderr)
        sys.exit(1)
    files = [f for f in result.stdout.strip().split('\n') if f]
    return files

def get_files_for_commit(commit_sha):
    cmd = ['git', 'show', '--pretty=format:', '--name-only', commit_sha]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error getting files for commit {commit_sha}:", result.stderr)
        sys.exit(1)
    files = [f for f in result.stdout.strip().split('\n') if f]
    return files

# New: get files with status information (A, D, M, R, etc.)
def get_files_with_status(commit_sha):
    cmd = ['git', 'show', '--pretty=format:', '--name-status', commit_sha]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error getting files (with status) for commit {commit_sha}:", result.stderr)
        sys.exit(1)
    lines = [l for l in result.stdout.strip().split('\n') if l]
    files = []
    for line in lines:
        parts = line.split('\t')
        if not parts:
            continue
        status_field = parts[0].strip()
        # status might be like "R100" for rename, so code is first char
        status = status_field[0] if status_field else ''
        if status == 'R' or status == 'C':
            # rename/copy: parts: [Rxxx, old_path, new_path]
            if len(parts) >= 3:
                old_path = parts[1]
                new_path = parts[2]
                files.append({'status': status, 'old_path': old_path, 'path': new_path})
            else:
                # fallback to treat as modified
                files.append({'status': status_field, 'path': parts[-1]})
        else:
            # normal cases: status, path
            if len(parts) >= 2:
                path = parts[1]
                files.append({'status': status, 'path': path})
            else:
                # unexpected format, keep raw
                files.append({'status': status_field, 'path': parts[-1] if parts else ''})
    return files

# Add: populate files into commit dicts so generate_html_report reads in-memory
def populate_files_for_commits(commits):
    for c in commits:
        try:
            # use the richer status-aware function
            c['files'] = get_files_with_status(c['hash'])
        except SystemExit:
            # keep an empty list on error instead of exiting further
            c['files'] = []

def get_last_commits_for_file(file_path, sha1, max_count=5):
    cmd = [
        'git', 'log', f'--pretty=format:%H|%an|%ad|%s', '--date=iso', f'-n{max_count}', sha1, '--', file_path
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error getting commit history for file {file_path}:", result.stderr)
        sys.exit(1)
    commits = []
    for line in result.stdout.strip().split('\n'):
        if line:
            parts = line.split('|', 3)
            if len(parts) == 4:
                commits.append({
                    'hash': parts[0],
                    'author': parts[1],
                    'date': parts[2],
                    'message': parts[3]
                })
    return commits

def should_skip_history(file_path):
    # pattern = r'^modules/apps/.*$'
    pattern = r'^modules/apps/portal-language/portal-language-lang/src/main/resources/content/Language_.*\.properties$'
    return re.match(pattern, file_path) is not None

def linkify_ticket(message):
    # Replace LPD-<number> or LPS-<number> with a link
    pattern = r'(LP[DS]-\d+)'
    def repl(match):
        ticket = match.group(1)
        url = f"https://liferay.atlassian.net/browse/{ticket}"
        return f'<a href="{url}" style="color:#ffd700;font-weight:bold;" target="_blank">{ticket}</a>'
    return re.sub(pattern, repl, message)

def extract_tickets_from_commits(commits):
    ticket_pattern = r'(LP[DS]-\d+)'
    ticket_counts = {}
    for c in commits:
        found = re.findall(ticket_pattern, c['message'])
        for ticket in found:
            ticket_counts[ticket] = ticket_counts.get(ticket, 0) + 1
    return ticket_counts

def write_css_file(report_dir):
    css_content = """
body {
    background-color: #151111;
    color: #aaaaaa;
    font-family: monospace;
}
select {
    background-color: #151111;
    color: #aaaaaa;
}
a {
    text-decoration: none;
}
.generated {
    display: inline-block;
    margin-left: 8px;
    padding: 2px 6px;
    background: #2b2b2b;
    color: #7cf78c;
    border-radius: 4px;
    font-size: 0.85em;
    font-weight: bold;
}
.legend {
    color: #888888;
    font-size: 0.9em;
}
.bl-note {
    color: #c9a227;
}
.bl-details {
    margin: 4px 0 18px 0;
}
.bl-summary {
    cursor: pointer;
    color: #9ecbff;
}
.bl-plus {
    color: #00cc66;
    font-weight: bold;
}
.bl-minus {
    color: #ff6666;
    font-weight: bold;
}
.bl-wrap {
    overflow-x: auto;
    border: 1px solid #333333;
    margin-top: 6px;
}
table.blame {
    border-collapse: collapse;
    font-size: 0.85em;
}
table.blame th {
    background-color: #241d1d;
    color: #dddddd;
    text-align: left;
    padding: 3px 8px;
    border-bottom: 1px solid #333333;
}
table.blame th:first-child {
    border-right: 2px solid #444444;
}
table.blame td:nth-child(3) {
    border-right: 2px solid #444444;
}
table.blame td {
    padding: 0 8px;
    white-space: pre;
    vertical-align: top;
    border: none;
}
table.blame td.bl-commit {
    background-color: #1c1717;
}
table.blame td.bl-commit-pr {
    border-left: 3px solid #ffd700;
    background-color: #292014;
}
table.blame td.bl-lineno {
    color: #666666;
    text-align: right;
    background-color: #1c1717;
}
table.blame td.bl-code {
    color: #8f8f8f;
}
table.blame td.bl-code.bl-del {
    background-color: rgba(255, 80, 80, 0.13);
    color: #f0bfbf;
}
table.blame td.bl-code.bl-add {
    background-color: rgba(0, 200, 100, 0.13);
    color: #b6efcd;
}
table.blame td.bl-empty {
    background-color: #191515;
}
table.blame tr.bl-skiprow td {
    text-align: center;
    color: #6b6b6b;
    font-style: italic;
    background-color: #1b1717;
    border-top: 1px solid #2c2626;
    border-bottom: 1px solid #2c2626;
}
"""
    css_path = os.path.join(report_dir, "pr_report.css")
    with open(css_path, "w") as f:
        f.write(css_content)
    return css_path

def is_generated_java(file_path, sha2):
    # Only consider .java files
    if not file_path.endswith('.java'):
        return False
    # Try to read the file content at sha2 (top of the range)
    cmd = ['git', 'show', f'{sha2}:{file_path}']
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        # file might be deleted/renamed in this range — treat as not generated
        return False
    content = result.stdout
    return '@Generated' in content

BLAME_MAX_LINES = 4000
BLAME_CONTEXT = 3
# Blame dominates report time on repositories with a long history, so run as
# many as the machine can take
BLAME_WORKERS = min(16, os.cpu_count() or 4)

def get_blame_lines(rev, file_path):
    """Blame file_path at rev and return one dict per line.

    Returns None when the file does not exist at that revision (added/deleted
    files), so the caller can render a one-sided diff.
    """
    cmd = ['git', 'blame', '--line-porcelain', rev, '--', file_path]
    result = subprocess.run(cmd, capture_output=True, text=True, errors='replace')
    if result.returncode != 0:
        return None
    lines = []
    current = None
    for raw in result.stdout.split('\n'):
        if raw.startswith('\t'):
            if current is not None:
                current['text'] = raw[1:]
                lines.append(current)
                current = None
            continue
        key, _, value = raw.partition(' ')
        if current is None:
            # block header: <sha> <orig_lineno> <final_lineno> [<num_lines>]
            parts = raw.split(' ')
            if len(parts) >= 3 and re.fullmatch(r'[0-9a-f]{40}', parts[0]):
                current = {
                    'commit': parts[0],
                    'lineno': int(parts[2]),
                    'author': '',
                    'summary': '',
                    'date': '',
                    'text': ''
                }
            continue
        if key == 'author':
            current['author'] = value
        elif key == 'summary':
            current['summary'] = value
        elif key == 'author-time':
            try:
                current['date'] = datetime.fromtimestamp(int(value)).strftime('%Y-%m-%d')
            except ValueError:
                pass
    return lines

def get_line_count(rev, file_path):
    """Line count of file_path at rev, or None when it is absent there.

    Cheap compared to blame, so it is used to skip files that are too large to
    render before paying for their blame.
    """
    cmd = ['git', 'show', f'{rev}:{file_path}']
    result = subprocess.run(cmd, capture_output=True, text=True, errors='replace')
    if result.returncode != 0:
        return None
    return result.stdout.count('\n')

def collect_blames(files, sha1, sha2):
    """Blame every file at both ends of the range, one git process per side.

    Maps each file to a (base_blame, head_blame) tuple, or to a string saying
    why no diff is rendered for it.
    """
    blames = {}
    candidates = []
    for file_path in files:
        if should_skip_history(file_path):
            blames[file_path] = "Blame diff not shown for this file."
        else:
            candidates.append(file_path)

    with ThreadPoolExecutor(max_workers=BLAME_WORKERS) as executor:
        counts = list(executor.map(
            lambda file_path: (
                get_line_count(sha1, file_path), get_line_count(sha2, file_path)
            ),
            candidates
        ))

    jobs = []
    for file_path, (left_count, right_count) in zip(candidates, counts):
        if max(left_count or 0, right_count or 0) > BLAME_MAX_LINES:
            blames[file_path] = (
                f"Diff not shown: file has more than {BLAME_MAX_LINES} lines "
                f"({left_count or 0} base / {right_count or 0} head)."
            )
            continue
        blames[file_path] = [None, None]
        if left_count is not None:
            jobs.append((file_path, 0, sha1))
        if right_count is not None:
            jobs.append((file_path, 1, sha2))

    with ThreadPoolExecutor(max_workers=BLAME_WORKERS) as executor:
        results = executor.map(lambda job: get_blame_lines(job[2], job[0]), jobs)
        for (file_path, side, _), lines in zip(jobs, results):
            blames[file_path][side] = lines

    for file_path, entry in blames.items():
        if isinstance(entry, list):
            blames[file_path] = tuple(entry)
    return blames

def commit_color(sha):
    # Stable per-commit hue so lines touched by the same commit read as a group
    hue = int(sha[:6], 16) % 360
    return f"hsl({hue}, 60%, 68%)"

def render_blame_cells(line, pr_hashes, kind):
    if line is None:
        return "<td class='bl-empty'></td><td class='bl-empty'></td><td class='bl-empty bl-code'></td>"
    sha = line['commit']
    tooltip = html_escape(
        f"{sha[:12]} | {line['author']} | {line['date']} | {line['summary']}", quote=True
    )
    github_url = f"https://github.com/liferay/liferay-portal/commit/{sha}"
    commit_class = "bl-commit bl-commit-pr" if sha in pr_hashes else "bl-commit"
    text = html_escape(line['text']) or '&nbsp;'
    return (
        f"<td class='{commit_class}'>"
        f"<a href=\"{github_url}\" title=\"{tooltip}\" target=\"_blank\" "
        f"style=\"color:{commit_color(sha)};\">{sha[:8]}</a></td>"
        f"<td class='bl-lineno'>{line['lineno']}</td>"
        f"<td class='bl-code bl-{kind}'>{text}</td>"
    )

def get_diff_opcodes(file_path, sha1, sha2, left_count, right_count):
    """Alignment opcodes taken from git's own diff, so the report agrees with git.

    The whole file is asked for as a single hunk; returns None (caller falls back
    to difflib) when the diff cannot be lined up with the blame output.
    """
    cmd = [
        'git', 'diff', '--no-color', '--no-ext-diff', '--unified=1000000000',
        sha1, sha2, '--', file_path
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, errors='replace')
    if result.returncode != 0:
        return None
    markers = []
    in_hunk = False
    for raw in result.stdout.split('\n'):
        if not in_hunk:
            if raw.startswith('@@'):
                in_hunk = True
            continue
        if not raw or raw.startswith('\\') or raw.startswith('@@'):
            continue
        if raw[0] in ' +-':
            markers.append(raw[0])

    opcodes = []
    index = 0
    i = j = 0
    while index < len(markers):
        start_i, start_j = i, j
        if markers[index] == ' ':
            while index < len(markers) and markers[index] == ' ':
                index += 1
                i += 1
                j += 1
            opcodes.append(('equal', start_i, i, start_j, j))
            continue
        while index < len(markers) and markers[index] == '-':
            index += 1
            i += 1
        while index < len(markers) and markers[index] == '+':
            index += 1
            j += 1
        if i > start_i and j > start_j:
            tag = 'replace'
        elif i > start_i:
            tag = 'delete'
        else:
            tag = 'insert'
        opcodes.append((tag, start_i, i, start_j, j))

    if i != left_count or j != right_count:
        return None
    return opcodes

def build_blame_rows(left, right, opcodes):
    """Align both blames into (kind, left_line, right_line) rows.

    Long unchanged stretches collapse into a ('skip', hidden_count, None) row.
    """
    rows = []
    for index, (tag, i1, i2, j1, j2) in enumerate(opcodes):
        if tag == 'equal':
            count = i2 - i1
            head = 0 if index == 0 else BLAME_CONTEXT
            tail = 0 if index == len(opcodes) - 1 else BLAME_CONTEXT
            if count > head + tail + 3:
                for k in range(head):
                    rows.append(('eq', left[i1 + k], right[j1 + k]))
                rows.append(('skip', count - head - tail, None))
                for k in range(count - tail, count):
                    rows.append(('eq', left[i1 + k], right[j1 + k]))
            else:
                for k in range(count):
                    rows.append(('eq', left[i1 + k], right[j1 + k]))
        elif tag == 'replace':
            left_count = i2 - i1
            right_count = j2 - j1
            for k in range(max(left_count, right_count)):
                rows.append((
                    'mod',
                    left[i1 + k] if k < left_count else None,
                    right[j1 + k] if k < right_count else None
                ))
        elif tag == 'delete':
            for k in range(i1, i2):
                rows.append(('del', left[k], None))
        elif tag == 'insert':
            for k in range(j1, j2):
                rows.append(('add', None, right[k]))
    return rows

def render_side_by_side_blame(file_path, left, right, sha1, sha2, pr_hashes):
    """Two-sided blame diff: base blame on the left, head blame on the right."""
    if left is None and right is None:
        return "<p>File is missing at both revisions; no diff shown.</p>"
    note = ""
    if left is None:
        note = "<p class='bl-note'>File added in this range; no base revision to blame.</p>"
    elif right is None:
        note = "<p class='bl-note'>File deleted in this range; no head revision to blame.</p>"
    left = left or []
    right = right or []
    if any('\x00' in line['text'] for line in left + right):
        return note + "<p class='bl-note'>Binary file; no line-level diff shown.</p>"
    if max(len(left), len(right)) > BLAME_MAX_LINES:
        return (
            note
            + f"<p class='bl-note'>Diff not shown: file has more than {BLAME_MAX_LINES} lines "
            f"({len(left)} base / {len(right)} head).</p>"
        )

    opcodes = get_diff_opcodes(file_path, sha1, sha2, len(left), len(right))
    if opcodes is None:
        opcodes = difflib.SequenceMatcher(
            None, [l['text'] for l in left], [r['text'] for r in right], autojunk=False
        ).get_opcodes()

    rows = build_blame_rows(left, right, opcodes)
    added = sum(1 for kind, _, r in rows if kind in ('add', 'mod') and r is not None)
    removed = sum(1 for kind, l, _ in rows if kind in ('del', 'mod') and l is not None)

    html_parts = [note]
    html_parts.append("<details class='bl-details'>")
    html_parts.append(
        f"<summary class='bl-summary'>Side-by-side blame diff "
        f"(<span class='bl-plus'>+{added}</span> / <span class='bl-minus'>-{removed}</span>)</summary>"
    )
    html_parts.append("<div class='bl-wrap'><table class='blame'>")
    html_parts.append(
        f"<tr><th colspan='3'>Base &mdash; {sha1[:8]}</th>"
        f"<th colspan='3'>Head &mdash; {sha2[:8]}</th></tr>"
    )
    for kind, left_line, right_line in rows:
        if kind == 'skip':
            html_parts.append(
                f"<tr class='bl-skiprow'><td colspan='6'>&hellip; {left_line} unchanged lines &hellip;</td></tr>"
            )
            continue
        left_kind = 'del' if kind in ('del', 'mod') else 'eq'
        right_kind = 'add' if kind in ('add', 'mod') else 'eq'
        html_parts.append(
            "<tr>"
            + render_blame_cells(left_line, pr_hashes, left_kind)
            + render_blame_cells(right_line, pr_hashes, right_kind)
            + "</tr>"
        )
    html_parts.append("</table></div></details>")
    return "".join(html_parts)

def generate_html_report(commits, files, sha1, sha2, report_path, css_filename, blames=None):
    pr_hashes = {c['hash'] for c in commits}
    html = []
    html.append(f"<html><head><title>PR Report</title><link rel='stylesheet' type='text/css' href='{css_filename}'></head><body>")
    html.append(f"<h1>PR Report: <code>{branch if branch else '(not set)'}</code> -- {sha1}..{sha2}</h1>")
    html.append(f"<p>Generated at: {datetime.now().isoformat()}</p>")
    # Ticket summary
    ticket_counts = extract_tickets_from_commits(commits)
    if ticket_counts:
        html.append(f"<h2>Ticket Summary ({len(ticket_counts)})</h2>")
        for ticket, count in sorted(ticket_counts.items()):
            html.append(f"<h3>{linkify_ticket(ticket)} ({count} commits)</h3>")
    html.append(f"<h2>Commits ({len(commits)})</h2>")
    html.append("<table border='1'><tr><th>Hash</th><th>Author</th><th>Date</th><th>Committer</th><th>Commit Date</th><th>Message</th><th>Files Changed</th></tr>")
    for c in commits:
        # use files already populated on the commit dict (each entry is a dict with status/path)
        files_changed = c.get('files', [])
        # build list items showing status and path (handle renames)
        list_items = []
        for fc in files_changed:
            status = fc.get('status', '')
            if status == 'A':
                badge = "<span style='color:#00cc66;font-weight:bold;'>[A]</span>"
                path_display = fc.get('path', '')
            elif status == 'D':
                badge = "<span style='color:#ff6666;font-weight:bold;'>[D]</span>"
                path_display = fc.get('path', '')
            elif status == 'M':
                badge = "<span style='color:#cccccc;font-weight:bold;'>[M]</span>"
                path_display = fc.get('path', '')
            elif status == 'R' or status == 'C':
                badge = f"<span style='color:#66a3ff;font-weight:bold;'>[{status}]</span>"
                old = fc.get('old_path', '')
                new = fc.get('path', '')
                path_display = f"{old} → {new}"
            else:
                badge = f"<span style='color:#999999;font-weight:bold;'>[{status}]</span>"
                path_display = fc.get('path', '') or fc.get('old_path', '')
            list_items.append(f"<li>{badge} {path_display}</li>")
        linked_message = linkify_ticket(c['message'])
        html.append(
            f"<tr><td>{c['hash'][:8]}</td><td><span hint='{c['authorId']}'>{c['author']}</span></td><td>{c['date']}</td><td><span hint='{c['committerId']}'>{c['committer']}</span></td><td>{c['commitDate']}</td><td>{linked_message}</td>"
            "<td><ul>" + "".join(list_items) + "</ul></td></tr>"
        )
    html.append("</table>")
    html.append(f"<h2>Changed Files ({len(files)})</h2>")
    show_diffs = blames is not None
    blames = blames or {}
    if show_diffs:
        html.append(
            "<p class='legend'>Each file shows a two-sided blame diff: the base revision on the left, "
            "the head revision on the right. Commit hashes are colored per commit; hashes marked with a "
            "<span style='border-left:3px solid #ffd700;padding-left:4px;'>gold bar</span> belong to this "
            "range. Hover a hash for author, date and subject.</p>"
        )
    for f in files:
        generated = is_generated_java(f, sha2)
        if generated:
            html.append(f"<h3>{f} <span class='generated'>(generated)</span></h3>")
        else:
            html.append(f"<h3>{f}</h3>")

        # Skip history either if matches skip pattern or is a generated java file
        if should_skip_history(f) or generated:
            html.append("<p>Commit history not shown for this file.</p>")
        else:
            file_commits = get_last_commits_for_file(f, sha1)
            if file_commits:
                html.append("<table border='1'><tr><th>Hash</th><th>Author</th><th>Date</th><th>Message</th></tr>")
                for fc in file_commits:
                    linked_message = linkify_ticket(fc['message'])
                    short_hash = fc['hash'][:8]
                    github_url = f"https://github.com/liferay/liferay-portal/commit/{fc['hash']}"
                    html.append(
                        f"<tr><td><a href=\"{github_url}\" style=\"color:#ffd700;font-weight:bold;\" target=\"_blank\">{short_hash}</a></td>"
                        f"<td>{fc['author']}</td><td>{fc['date']}</td><td>{linked_message}</td></tr>"
                    )
                html.append("</table>")
            else:
                html.append("<p>No commit history found for this file.</p>")

        if show_diffs:
            blame_entry = blames.get(f)
            if blame_entry is None or isinstance(blame_entry, str):
                reason = blame_entry or "Blame diff not shown for this file."
                html.append(f"<p class='bl-note'>{reason}</p>")
            else:
                left, right = blame_entry
                html.append(render_side_by_side_blame(f, left, right, sha1, sha2, pr_hashes))
    html.append("</body></html>")
    with open(report_path, 'w') as f:
        f.write('\n'.join(html))

if __name__ == "__main__":
    branch = None  # Ensure branch is always defined
    start_time = time.time()
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    flags = [a for a in sys.argv[1:] if a.startswith('--')]
    with_diff = '--no-diff' not in flags
    if len(args) == 2:
        sha1 = args[0]
        sha2 = args[1]
    elif len(args) == 0:
        branch = get_current_branch()
        sha2 = get_head_sha()
        sha1 = get_merge_base_sha(branch)
    else:
        print("Usage: git_pr_check.py [--no-diff] [<commit_sha1> <commit_sha2>]")
        sys.exit(1)

    commits = get_commit_details(sha1, sha2)
    # Populate files for each commit once (in-memory)
    populate_files_for_commits(commits)

    files = get_changed_files(sha1, sha2)

    print(f"Branch: {branch if branch else '(not set)'}")
    print(f"Base SHA: {sha1}")
    print(f"Top SHA: {sha2}")
    print(f"Commits: {len(commits)}")
    print(f"Files modified: {len(files)}")

    blames = None
    if with_diff:
        blame_start = time.time()
        blames = collect_blames(files, sha1, sha2)
        print(f"Blames collected in {time.time() - blame_start:.3f} seconds "
              f"({BLAME_WORKERS} workers)")

    # Prepare report directory and filename
    script_dir = os.path.dirname(os.path.abspath(__file__))
    report_dir = os.path.join(script_dir, "pr_reports")
    os.makedirs(report_dir, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_filename = f"pr_report_{sha1[:7]}_{sha2[:7]}_{timestamp}.html"
    report_path = os.path.join(report_dir, report_filename)
    css_filename = "pr_report.css"
    write_css_file(report_dir)

    generate_html_report(commits, files, sha1, sha2, report_path, css_filename, blames)
    end_time = time.time()
    elapsed = end_time - start_time
    print("")
    print(f"HTML report generated: file://{report_path}")
    print(f"Report generation time: {elapsed:.3f} seconds")
