# /// script
# requires-python = ">=3.9"
# ///
"""
Run after `--write-verification-metadata` (#246, #238). Key servers answer on some runs and not others:
a key that couldn't be downloaded once is listed under <ignored-keys>, and when a later run does download
it and trusts groups with it, Gradle keeps the old "ignored" entry. An ignored key wins, so the artifacts
it signs end up with neither a usable key nor a checksum, and verification fails on them. This drops every
ignored key that is also trusted (its key is then in verification-keyring.keys) and changes nothing else.
"""
import re
import sys
from pathlib import Path

path = Path(sys.argv[1] if len(sys.argv) > 1 else "gradle/verification-metadata.xml")
text = path.read_text()
trusted = {fp[-16:] for fp in re.findall(r'<trusted-key id="([0-9A-F]+)"', text)}
stale = [k for k in re.findall(r'<ignored-key id="([0-9A-F]+)"', text) if k in trusted]
for key in stale:
    text = re.sub(r'\n\s*<ignored-key id="%s"[^>]*/>' % key, "", text)
path.write_text(text)
print("Dropped ignored keys that are trusted:", ", ".join(stale) if stale else "none")
