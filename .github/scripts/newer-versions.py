# /// script
# requires-python = ">=3.9"
# ///
"""
Turns lint's GradleDependency findings into the body of the issue newer-versions.yml keeps open (#639).

Usage: newer-versions.py <body.md> <lint-results.xml>...

Reads each module's XML lint report (from a run with -Pglovebox.newerVersions, so GradleDependency is the
only check), writes a Markdown table of the libraries that have a newer version to <body.md>, and prints
how many it found. Every report must exist: a missing one means lint didn't run, not that nothing is newer.
A library found by more than one module is listed once. RUN_URL, when set, links the run in the body.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

NEWER = re.compile(r"A newer version of (?P<library>\S+) than (?P<used>\S+) is available: (?P<newer>\S+)")


def findings(report: Path) -> list[tuple[str, str, str, str]]:
    rows = []
    for issue in ET.parse(report).getroot().iter("issue"):
        if issue.get("id") != "GradleDependency":
            continue
        message = issue.get("message", "")
        location = issue.find("location")
        where = ""
        if location is not None:
            file = Path(location.get("file", ""))
            try:
                file = file.resolve().relative_to(Path.cwd().resolve())
            except ValueError:
                pass
            where = f"{file}:{location.get('line', '?')}"
        match = NEWER.search(message)
        if match:
            rows.append((match["library"], match["used"], match["newer"], where))
        else:
            # Lint worded it some other way: keep the whole message rather than drop it.
            rows.append((message, "", "", where))
    return rows


def main() -> None:
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    body, reports = Path(sys.argv[1]), [Path(p) for p in sys.argv[2:]]
    for report in reports:
        if not report.is_file():
            sys.exit(f"No lint report at {report}")
    rows = sorted({row for report in reports for row in findings(report)})

    lines = [
        "Lint's `GradleDependency` check finds a newer version of these libraries:",
        "",
        "| Library | In use | Newer | Where |",
        "| --- | --- | --- | --- |",
        *(f"| `{library}` | {used} | {newer} | `{where}` |" for library, used, newer, where in rows),
        "",
        "Dependabot proposes a version only after its 5-day cooldown, so one listed here may not have its PR "
        "yet. A bump by hand also needs the verification metadata regenerated (CLAUDE.md, Commands). An "
        "upgrade that is a migration rather than a bump gets an issue of its own and an ignore in "
        "`app/lint.xml` naming it, which keeps it off this list.",
        "",
        "This issue is written by the weekly `newer-versions` workflow: it is updated each week, and closed "
        "once lint finds nothing newer.",
    ]
    run_url = os.environ.get("RUN_URL")
    if run_url:
        lines += ["", f"Last run: {run_url}"]
    body.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(len(rows))


if __name__ == "__main__":
    main()
