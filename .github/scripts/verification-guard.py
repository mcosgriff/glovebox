# /// script
# requires-python = ">=3.9"
# ///
"""
Run by dependabot-verification.yml before it commits regenerated verification metadata to a Dependabot PR.
That metadata was written by a Gradle run of the PR's own build code with verification off, so a compromised
plugin could have weakened it, and nobody has read the diff yet. This refuses a new file that:

- turns verify-metadata or verify-signatures off, or adds a kind of setting the old one didn't have
  (<trusted-artifacts>, say);
- trusts a key for a regex the old file didn't, unless it is the regex Gradle writes when it merges one key's
  groups (`^org[.]jetbrains($|([.].*))`) and the old file already trusted that key for a group under it. Any
  other regex could cover any group;
- changes a checksum for a version already in the file. A new version brings new checksums; the same version
  never does (CLAUDE.md: never accept a changed checksum for a version already in the file).

    python3 verification-guard.py OLD.xml NEW.xml
"""
import re
import sys
import xml.etree.ElementTree as ET

NS = {"v": "https://schema.gradle.org/dependency-verification"}


def tag(e):
    return e.tag.split("}")[-1]


def regex_trust(root):
    out = set()
    for key in root.iterfind("v:configuration/v:trusted-keys/v:trusted-key", NS):
        for t in [key, *key.iterfind("v:trusting", NS)]:
            if t.get("regex") == "true":
                out.add((key.get("id"), t.get("group"), t.get("name"), t.get("version")))
    return out


def literal_groups(root):
    out = {}
    for key in root.iterfind("v:configuration/v:trusted-keys/v:trusted-key", NS):
        for t in [key, *key.iterfind("v:trusting", NS)]:
            if t.get("group") and t.get("regex") != "true":
                out.setdefault(key.get("id"), set()).add(t.get("group"))
    return out


# What Gradle writes for "this group and every group under it": ^a[.]b($|([.].*))
GRADLE_PREFIX = re.compile(r"\^([A-Za-z0-9_-]+(?:\[\.\][A-Za-z0-9_-]+)*)\(\$\|\(\[\.\]\.\*\)\)")


def merged_by_gradle(key, group, name, version, old_groups):
    m = GRADLE_PREFIX.fullmatch(group or "")
    if not m or name or version:
        return False
    prefix = m.group(1).replace("[.]", ".")
    return any(g == prefix or g.startswith(prefix + ".") for g in old_groups.get(key, ()))


def checksums(root):
    out = {}
    for c in root.iterfind("v:components/v:component", NS):
        for a in c.iterfind("v:artifact", NS):
            ref = (c.get("group"), c.get("name"), c.get("version"), a.get("name"))
            out[ref] = {s.get("value") for s in a.iter() if tag(s) in ("sha256", "also-trust") and s.get("value")}
    return out


def main(old_path, new_path):
    old, new = ET.parse(old_path).getroot(), ET.parse(new_path).getroot()
    problems = []

    for setting in ("verify-metadata", "verify-signatures"):
        value = new.findtext(f"v:configuration/v:{setting}", default="", namespaces=NS).strip()
        if value != "true":
            problems.append(f"{setting} is '{value}', not true")
    old_kinds = {tag(e) for e in old.find("v:configuration", NS)}
    for kind in sorted({tag(e) for e in new.find("v:configuration", NS)} - old_kinds):
        problems.append(f"new setting <{kind}>")

    old_groups = literal_groups(old)
    for key, group, name, version in sorted(regex_trust(new) - regex_trust(old)):
        if merged_by_gradle(key, group, name, version, old_groups):
            continue
        problems.append(f"key {key} newly trusted for the regex group={group} name={name} version={version}")

    was = checksums(old)
    for ref, values in sorted(checksums(new).items()):
        if was.get(ref) and values and values != was[ref]:
            problems.append("changed checksum for %s:%s:%s %s" % ref)

    for p in problems:
        print(f"::error::Verification metadata: {p}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(*sys.argv[1:3]))
