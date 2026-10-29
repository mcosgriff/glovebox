# /// script
# requires-python = ">=3.11"
# ///
"""
Makes the bundled maker's bulletins (#459): the free-repair ones (service campaigns, warranty extensions,
over-the-air updates) NHTSA holds for the makes and models Glovebox has maker's schedules for.

NHTSA has no API for these, only its manufacturer communications flat files. Download them by hand from
https://static.nhtsa.gov/odi/ffdd/tsbs/ (TSBS_RECEIVED_2005-2009.zip through the newest; see TSBS.txt for
the fields) into one folder, then:

    uv run scripts/bulletins.py <that folder>

It writes app/src/main/assets/bulletins/<make>-<model>.json, one per model with a schedule, each dated with
the newest "Date Added to File" NHTSA's files hold. Each bulletin's PDF link is the first of a few addresses
NHTSA answers (its paths aren't documented), checked once here; the app fetches nothing.

A model with a schedule but no entry in NHTSA_MODELS stops the script: add its NHTSA spellings there when
adding the vehicle (docs/known-schedules.md).
"""

import glob
import json
import os
import re
import sys
import unicodedata
import urllib.error
import urllib.request
import zipfile
from dataclasses import dataclass, field

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCHEDULES = os.path.join(ROOT, "app/src/main/assets/known-schedules")
OUT = os.path.join(ROOT, "app/src/main/assets/bulletins")

# How NHTSA spells each model Glovebox has a schedule for ("make/model" as the schedule names it). Every
# spelling NHTSA files bulletins under for that vehicle, hybrids included; checked against its files.
NHTSA_MODELS = {
    "Toyota/4Runner": ["4RUNNER", "4RUNNER I-FORCE MAX HYBRID"],
    "Toyota/Camry": ["CAMRY", "CAMRY HYBRID"],
    "Toyota/Corolla": ["COROLLA", "COROLLA HATCHBACK", "COROLLA HYBRID"],
    "Toyota/Corolla Cross": ["COROLLA CROSS", "COROLLA CROSS HYBRID"],
    "Toyota/Grand Highlander": ["GRAND HIGHLANDER", "GRAND HIGHLANDER HYBRID"],
    "Toyota/Highlander": ["HIGHLANDER", "HIGHLANDER HYBRID"],
    "Toyota/Prius": ["PRIUS"],
    "Toyota/Prius Prime (PHEV)": ["PRIUS PRIME", "PRIUS PLUG-IN HYBRID"],
    "Toyota/RAV4": ["RAV4", "RAV4 HYBRID"],
    "Toyota/RAV4 Prime (PHEV)": ["RAV4 PRIME", "RAV4 PLUG-IN HYBRID"],
    "Toyota/Sequoia": ["SEQUOIA", "SEQUOIA HYBRID"],
    "Toyota/Sienna": ["SIENNA", "SIENNA HYBRID"],
    "Toyota/Tacoma": ["TACOMA", "TACOMA HYBRID"],
    "Toyota/Tundra": ["TUNDRA", "TUNDRA HYBRID"],
    "Volkswagen/Beetle": ["BEETLE", "BEETLE CONVERTIBLE", "BEETLE TDI", "BEETLE CONVERTIBLE TDI"],
    "Volkswagen/CC": ["CC", "PASSAT CC"],
    "Volkswagen/Eos": ["EOS"],
    "Volkswagen/Golf": ["GOLF"],
    "Volkswagen/Golf R": ["GOLF R"],
    "Volkswagen/GTI": ["GTI"],
    "Volkswagen/Golf GTI": ["GTI"],
    "Volkswagen/Jetta": ["JETTA", "JETTA GLI", "JETTA HYBRID"],
    "Volkswagen/Jetta SportWagen": ["JETTA SPORTWAGEN", "GOLF WAGON"],
    "Volkswagen/Passat": ["PASSAT", "PASSAT TDI", "PASSAT WAGON"],
    "Volkswagen/e-Golf": ["E-GOLF", "EGOLF"],
}

TYPED = {
    "Service Campaign": "SERVICE_CAMPAIGN",
    "Warranty Program/Extension": "WARRANTY_EXTENSION",
    "Over The Air": "OVER_THE_AIR",
}

# Before May 2024 NHTSA typed every communication "Service Bulletin/Repair Instructions". Those are kept only
# when the summary itself names a free-repair program, and are marked as found by their wording.
# A summary naming both (Toyota's "limited service campaign K0P customer support program 19TE12") is a campaign,
# so every document of one program reads as one kind.
WORDING = [
    (re.compile(r"(limited|special) service campaign|service action", re.I), "SERVICE_CAMPAIGN"),
    (re.compile(r"warranty (enhancement|extension)|extended warranty|customer support program", re.I), "WARRANTY_EXTENSION"),
]
UNTYPED = {"", "Service Bulletin/Repair Instructions"}

# Fields of NHTSA's flat file (TSBS.txt), counted from 0.
ID, ADDED, NUMBER, MFR_DATE, TYPE, MAKE, MODEL, YEAR, COMPONENTS, SUMMARY = 0, 2, 3, 4, 6, 7, 8, 9, 10, 13


@dataclass
class Covered:
    make: str
    model: str
    years: range | list
    nhtsa: list
    aliases: list = field(default_factory=list)

    @property
    def key(self):
        return f"{self.make}/{self.model}"


def kind(type_, summary):
    """(kind, found by wording) for a communication worth bundling, or None."""
    t = type_.strip()
    if t in TYPED:
        return TYPED[t], False
    if t in UNTYPED:
        for pattern, k in WORDING:
            if pattern.search(summary):
                return k, True
    return None


def covered(schedules, names=NHTSA_MODELS):
    """One Covered per make and model with a schedule: every year and alias its schedules have."""
    by = {}
    for s in schedules:
        c = by.setdefault(f"{s['make']}/{s['model']}", Covered(s["make"], s["model"], [], [], []))
        c.years = sorted(set(c.years) | set(range(s["years"]["from"], s["years"]["to"] + 1)))
        c.aliases = sorted(set(c.aliases) | set(s.get("modelAliases", [])))
    missing = [k for k in by if k not in names]
    if missing:
        sys.exit(f"No NHTSA spellings for {', '.join(sorted(missing))}: add them to NHTSA_MODELS in scripts/bulletins.py")
    for k, c in by.items():
        c.nhtsa = names[k]
    return sorted(by.values(), key=lambda c: c.key)


def day(text):
    t = text.strip()
    return f"{t[:4]}-{t[4:6]}-{t[6:8]}" if re.fullmatch(r"\d{8}", t) else None


@dataclass
class Extract:
    """Bulletins by covered model key, newest first, and the newest day NHTSA added anything to its files."""
    bulletins: dict
    as_of: str | None


def extract_from(lines, cover):
    """One entry per NHTSA ID for each covered model."""
    found = {c.key: {} for c in cover}
    newest = ""
    for line in lines:
        fields = line.rstrip("\r\n").split("\t")
        if len(fields) > ADDED and re.fullmatch(r"\d{8}", fields[ADDED].strip()):
            newest = max(newest, fields[ADDED].strip())
        if len(fields) < 14 or not fields[YEAR].strip().isdigit():
            continue
        found_kind = kind(fields[TYPE], fields[SUMMARY])
        if found_kind is None:
            continue
        bulletin_kind, by_wording = found_kind
        model, year = fields[MODEL].strip().upper(), int(fields[YEAR])
        for c in cover:
            if fields[MAKE].strip().upper() != c.make.upper() or model not in c.nhtsa or year not in c.years:
                continue
            e = found[c.key].setdefault(fields[ID].strip(), {
                "id": fields[ID].strip(),
                "number": fields[NUMBER].strip(),
                "date": day(fields[MFR_DATE]),
                "added": fields[ADDED].strip(),
                "kind": bulletin_kind,
                "byWording": by_wording,
                "summary": " ".join("".join(ch for ch in fields[SUMMARY] if not unicodedata.category(ch).startswith("C")).split()),
                "products": {},
                "components": set(),
            })
            e["products"].setdefault(model, set()).add(year)
            e["components"].update(p.strip() for p in fields[COMPONENTS].split(",") if p.strip())
    out = {}
    for key, entries in found.items():
        listed = []
        for e in entries.values():
            order = [m for c in cover if c.key == key for m in c.nhtsa]
            e["products"] = [{"model": m, "years": sorted(e["products"][m])} for m in order if m in e["products"]]
            e["components"] = sorted(e["components"])
            listed.append(e)
        # NHTSA files some documents twice under new IDs; the same number and text is one bulletin.
        seen = set()
        out[key] = []
        for e in sorted(listed, key=lambda e: (e["date"] or "", e["id"]), reverse=True):
            if (e["number"], e["summary"]) not in seen:
                seen.add((e["number"], e["summary"]))
                out[key].append(e)
    return Extract(out, day(newest))


def document(c, entries, as_of):
    """A model's file: what it is for, the years its schedules cover, how current NHTSA's files were, and its bulletins."""
    return {"make": c.make, "model": c.model, "modelAliases": c.aliases, "years": list(c.years), "asOf": as_of, "bulletins": entries}


def pdf_link(id_, added, mfr_date, head):
    """The first address NHTSA answers for the bulletin's PDF, or None. [head] says whether a URL answers."""
    years = list(dict.fromkeys(d[:4] for d in (added.strip(), mfr_date.strip()) if re.fullmatch(r"\d{8}", d)))
    for year in years:
        for suffix in ("0001", "9999"):
            url = f"https://static.nhtsa.gov/odi/tsbs/{year}/MC-{id_}-{suffix}.pdf"
            if head(url):
                return url
    return None


def head(url):
    try:
        with urllib.request.urlopen(urllib.request.Request(url, method="HEAD"), timeout=20) as r:
            return r.status == 200
    except (urllib.error.URLError, TimeoutError):
        return False


def decode(raw, file, number):
    """NHTSA's files are UTF-8 (every line was, 2026-10-25); a line that isn't stops the script rather than be guessed at."""
    try:
        return raw.decode("utf-8")
    except UnicodeDecodeError as e:
        sys.exit(f"{file} line {number} isn't UTF-8 ({e.reason}); look at it before changing how it is read")


def lines_of(folder):
    for z in sorted(glob.glob(os.path.join(folder, "TSBS_RECEIVED_*.zip"))):
        with zipfile.ZipFile(z) as zf, zf.open(zf.namelist()[0]) as f:
            for number, raw in enumerate(f, start=1):
                yield decode(raw, os.path.basename(z), number)


def read_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def main(folder):
    schedules = [read_json(p) for p in sorted(glob.glob(os.path.join(SCHEDULES, "*.json")))]
    cover = covered(schedules)
    found = extract_from(lines_of(folder), cover)
    # Links are checked once per NHTSA ID and kept beside the downloads, so a rerun asks NHTSA only about new ones.
    cache_path = os.path.join(folder, "pdf-links.json")
    cache = read_json(cache_path) if os.path.exists(cache_path) else {}
    os.makedirs(OUT, exist_ok=True)
    for c in cover:
        entries = found.bulletins[c.key]
        for e in entries:
            if e["id"] not in cache:
                cache[e["id"]] = pdf_link(e["id"], e["added"], e["date"].replace("-", "") if e["date"] else "", head)
            e["pdf"] = cache[e["id"]]
            del e["added"]
        doc = document(c, entries, found.as_of)
        name = re.sub(r"[^a-z0-9]+", "-", f"{c.make} {c.model}".lower()).strip("-")
        with open(os.path.join(OUT, f"{name}.json"), "w", encoding="utf-8") as f:
            json.dump(doc, f, indent=1, ensure_ascii=False)
            f.write("\n")
        print(f"{c.key}: {len(entries)} bulletins, {sum(1 for e in entries if e['pdf'])} with a PDF")
    with open(cache_path, "w", encoding="utf-8") as f:
        json.dump(cache, f)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
