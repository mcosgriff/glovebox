# Maker's bulletins

The free-repair bulletins Glovebox bundles (#459, [ADR-0013](adr/0013-bulletins-are-bundled.md)): service
campaigns, warranty extensions and over-the-air updates that makers sent their dealers, from NHTSA's
manufacturer communications files, for the makes and models with a maker's schedule. One file per model in
`app/src/main/assets/bulletins/`, read by `Bulletins` in `:domain`; `BulletinsTest` reads every one and fails
when a model with a schedule has none.

## What is kept

- NHTSA's own type when it is "Service Campaign", "Warranty Program/Extension" or "Over The Air".
- NHTSA typed nothing before May 2024 (everything reads "Service Bulletin/Repair Instructions"). Those are kept
  only when the summary names a warranty enhancement or extension, a customer support program, a limited or
  special service campaign or a service action, and are shown as found by their wording. The phrase list is
  `WORDING` in the script.
- Only the model years the model's schedules cover. Exact repeats (the same maker's number and text under
  another NHTSA ID) are dropped; other documents of one program each stay.
- The PDF link is the first of NHTSA's undocumented paths that answers when the script runs
  (`/odi/tsbs/<year added or year sent>/MC-<id>-0001.pdf` or `-9999.pdf`), or none.

## Ended programs

NHTSA's file has no end date. A card says its program ended only where the summary says so with a date, read in
the app by `Bulletins.endedOn` (#474); the wordings it reads are its `endings`, with their tests in `BulletinsTest`.
A bundled summary that says "expired" another way fails `BulletinsTest`, so a refresh that brings a new wording has
to teach the reader it first.

## Refreshing

By hand, when wanted; the screen shows the date NHTSA's files were current to.

1. Download `TSBS_RECEIVED_2005-2009.zip` through the newest chunk from `https://static.nhtsa.gov/odi/ffdd/tsbs/`
   (and `TSBS.txt`, to see whether the fields changed) into one folder outside the repo.
2. `uv run scripts/bulletins.py <that folder>`. Links are checked once per bulletin and remembered in
   `pdf-links.json` in that folder, so keep it for the next refresh.
3. Read the diff of `app/src/main/assets/bulletins/`, run `./gradlew :domain:test`, and commit.

`uv run scripts/test_bulletins.py` tests the script; CI runs it too. The script reads NHTSA's files as UTF-8 and
stops on a line that isn't, rather than guess.

## Adding a vehicle

A new maker's schedule needs its bulletins: add the model's NHTSA spellings (every name NHTSA files it under,
hybrids included; look them up in the flat file) to `NHTSA_MODELS` in `scripts/bulletins.py`, then refresh.
The script stops on a schedule's model without spellings.
