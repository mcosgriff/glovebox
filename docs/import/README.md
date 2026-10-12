# Importing data into Glovebox

Glovebox can read a JSON file you write (or someone shares with you) and add its contents to one of
your vehicles: **Settings → Import**. This folder is the specification of those files.

- `schema/` — one [JSON Schema](https://json-schema.org/) per kind of import. The schema is the rule
  book: if a file passes it, its shape is right.
- `examples/` — a valid file of each kind to copy from. All values are made up.
- `examples/invalid/` — files that must be refused, one mistake each. They are what the app's tests
  run; they are also a quick way to see what a rule means.
- `examples/beyond-schema/` — the few rules a schema can't state, which the app still enforces.

## What can be imported

| `kind` | What it is | Schema | In the app |
|---|---|---|---|
| `torque-specs` | Fasteners with socket sizes and torque values | [`torque-specs.schema.json`](schema/torque-specs.schema.json) | Yes |
| `interval-schedule` | A schedule with one repeating interval per job | to come (#104) | Not yet |
| `service-history` | Maintenance done before you used Glovebox | to come (#103) | Not yet |
| `milestone-schedule` | A schedule by mileage point, as an owner's manual prints it | to come (#105) | Not yet |

## The shape of every file

```json
{
  "glovebox": "import",
  "version": 1,
  "kind": "torque-specs",
  "items": [ ... ]
}
```

- `glovebox` is always `"import"`.
- `version` is the format version, a whole number. This document describes version `1`. A file with
  a higher version than the app knows is refused as made for a newer version. A version-1 file will
  always stay readable.
- `kind` says what `items` holds. One kind per file.
- `"$schema"` may be added so an editor checks the file as you type (see below). Glovebox ignores it.
- **The file never names a vehicle.** You pick the vehicle in the app, so a file can be passed
  between owners of the same model, and it carries no VIN.

The format is deliberately strict:

- A field Glovebox doesn't know refuses the whole file. A misspelled `"torqe"` is an error, not a
  value quietly dropped.
- Nothing is repaired. Text must not start or end with a space or contain line breaks, numbers are
  written without quotes, and a field you don't have is left out, not set to `null` or `""`.
- Units are always written out. Nothing is assumed from context.
- Every list and every piece of text has a maximum, given in the schema.

## Checking a file before you import it

You don't have to: the app checks every file itself and lists what is wrong with it. But a file is
easier to fix at a keyboard than on a phone.

**From a terminal**, with [uv](https://docs.astral.sh/uv/) installed:

```sh
uvx check-jsonschema --schemafile docs/import/schema/torque-specs.schema.json my-specs.json
```

It prints `ok -- validation done`, or each problem and where it is. Without a copy of this
repository, give the schema's address instead:

```sh
uvx check-jsonschema \
  --schemafile https://raw.githubusercontent.com/mcosgriff/glovebox/main/docs/import/schema/torque-specs.schema.json \
  my-specs.json
```

**In an editor** that understands JSON Schema (VS Code and the JetBrains editors do out of the box),
make this the first line inside the file's braces and mistakes are underlined as you type:

```json
"$schema": "https://raw.githubusercontent.com/mcosgriff/glovebox/main/docs/import/schema/torque-specs.schema.json",
```

## What happens to data you already have

Nothing you have is changed unless you say so.

After a file passes, the app shows what it would do before it does anything:

- **New** — nothing on the vehicle matches. These are added.
- **Fills in** — matches a row of yours that is only a name, with nothing entered yet. These are
  filled in, since nothing is lost.
- **Already the same** — matches something of yours that says the same. Left alone.
- **Different from yours** — matches something of yours that says something else. Listed with your
  value beside the file's.

You then choose:

- **Add new only** (the default) — rows that differ are left exactly as they are.
- **Replace matching** — rows that differ are replaced with the file's version, after you confirm a
  list of what will change.

Either way:

- An import **never deletes** anything the file doesn't mention.
- It is **all or nothing**. If any part can't be saved, none of it is.
- What it adds is **your own data**, the same as if you had typed it. It never changes anything
  Glovebox itself supplies.
- There is no Undo. The result screen says exactly what was added, filled in and replaced.

## Privacy

An import reads the one file you pick, on your phone. Nothing is sent anywhere. An import file is
plain text and not encrypted: treat it like any other document. To move all of your data between
phones, use the encrypted backup instead (Settings → Backup).

## `torque-specs`

[`schema/torque-specs.schema.json`](schema/torque-specs.schema.json) ·
[`examples/torque-specs.json`](examples/torque-specs.json) · up to 500 items

```json
{
  "name": "Lug nuts",
  "category": "wheels",
  "socket": "21 mm",
  "torque": { "value": 100, "unit": "ft-lb" },
  "source": "factory-manual",
  "note": "Star pattern, re-check after 50 mi"
}
```

| Field | Required | Rules |
|---|---|---|
| `name` | yes | 1 to 60 characters |
| `category` | yes | `engine`, `wheels`, `driveline`, `suspension` or `other` |
| `socket` | no | Socket or tool size as you'd say it, up to 30 characters |
| `torque` | no | `value` (a number above 0) and `unit`: `Nm`, `ft-lb` or `in-lb` |
| `source` | no | `own` (the default) or `factory-manual`, which needs a `torque` |
| `note` | no | Up to 140 characters |

- Torque is stored in N·m and shown in the unit you chose in Settings, so write each value in
  whatever unit your source uses. The most accepted is 1000 Nm, 737 ft-lb or 8850 in-lb.
- `factory-manual` is your statement that you copied the value from the factory service manual. It
  is shown as such; Glovebox has no way to check it.
- **Matching:** a row in the file matches a row on the vehicle with the same category and the same
  name, ignoring capitals. The same name can't appear twice under one category in a file (this is
  the rule the schema can't state).
- **Same or different:** socket, torque (compared as a wrench setting, whichever units were used),
  note and, where there is a torque, source.

**Someone else's file is someone else's numbers.** Check every value against your service manual
before you put a wrench on it.
