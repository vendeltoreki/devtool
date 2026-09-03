#!/usr/bin/env python3

import subprocess
import sys
import os
from datetime import datetime
import re
import time

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

def generate_html_report(commits, files, sha1, sha2, report_path, css_filename):
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
    html.append("</body></html>")
    with open(report_path, 'w') as f:
        f.write('\n'.join(html))

if __name__ == "__main__":
    branch = None  # Ensure branch is always defined
    start_time = time.time()
    if len(sys.argv) == 3:
        sha1 = sys.argv[1]
        sha2 = sys.argv[2]
    elif len(sys.argv) == 1:
        branch = get_current_branch()
        sha2 = get_head_sha()
        sha1 = get_merge_base_sha(branch)
    else:
        print("Usage: git_pr_check.py <commit_sha1> <commit_sha2>")
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

    # Prepare report directory and filename
    script_dir = os.path.dirname(os.path.abspath(__file__))
    report_dir = os.path.join(script_dir, "pr_reports")
    os.makedirs(report_dir, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_filename = f"pr_report_{sha1[:7]}_{sha2[:7]}_{timestamp}.html"
    report_path = os.path.join(report_dir, report_filename)
    css_filename = "pr_report.css"
    write_css_file(report_dir)

    generate_html_report(commits, files, sha1, sha2, report_path, css_filename)
    end_time = time.time()
    elapsed = end_time - start_time
    print("")
    print(f"HTML report generated: file://{report_path}")
    print(f"Report generation time: {elapsed:.3f} seconds")
