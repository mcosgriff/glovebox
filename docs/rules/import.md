# Import

Rules for the import format and schemas, schedule and history imports, the Carfax PDF, writing import files. Read before changing any of it; review holds a diff to every line here.

- `docs/import/README.md` and `docs/import/schema/` are the specification. **Change the schema, the Kotlin reader
  and the example files together, and never change how a version-1 file is read.** `ImportFormatTest` holds the
  schema and the reader to each other. A rule a schema can't state goes in `examples/beyond-schema/` and the
  README. The format is strict: unknown fields, `null`, stray spaces and out-of-range values refuse the file,
  nothing is repaired, every error has its path. A file never names a vehicle.
- A new kind: a schema, a valid example, `examples/invalid/<kind>/` and `examples/beyond-schema/<kind>/`, a
  reader and plan in `:domain`, a case in `ImportFormat.parse` and `supported`, and a `…Preview` in
  `ImportViewModel`. The page is kind-agnostic.
- A file says its unit by its field names (`everyMiles` / `everyKm` …), one unit per file; `FileDistances` puts
  it into the vehicle's unit. A file the app writes is in the vehicle's unit.
- **An import never deletes, is one transaction, has no Undo, and only writes the user's own rows**; nothing
  Glovebox supplies may be changed by one. "Add new only" is the default; "Replace matching" lists what it will
  overwrite first. The picked file is read into memory (2 MB cap) and never written to disk.
- Schedule import: a typical item is never matched or replaced; the only thing an import may change on one is to
  stop tracking it, unticked by default.
- Versions are per kind (`ImportKind.version`); only `service-history` has more than version 1: version 2 adds
  priced jobs (#481), version 3 a job's `partRows` and the record's `shopSupplies`, `tax` and `fees` (#502). One schema
  holds every version (`if` on `version`), so each version's record stays as it was. **A file with no `jobs` says
  nothing about prices:** it never makes a saved record "different", and replacing from it keeps the saved jobs, and
  their part rows, on the same lines (`HistoryImport.keptJobs`). **Nor does one with no charges say anything about
  them** (`HistoryImport.keptCharges`). A part row is read as written: nothing is worked out from its other figures.
  The writers still write version 1.
- History import: **adding a catalog item means adding it to `service-history.schema.json`, the README table,
  `CatalogText.kt`, `strings.xml` and `LineWords`.** Records match on date and odometer. **Readings that look
  mistyped are left out unless the user ticks them** (a shop's typo once added 60,000 miles). "Not in the
  future" is against the `today` passed to `ImportFormat.parse`.
- Carfax report PDF (#160): read in memory with PDFBox, never written to disk, **on a thread of its own with a
  time limit and every error caught** (`PdfWords.guarded`): a crafted PDF has looped or overflowed PDFBox. **Only service visits are
  taken**, and a comment becomes a catalog line only where the wording is plain; pads on an unnamed axle are
  never front or rear. A visit with no mileage is refused, never guessed. Ligatures are repaired only into known
  words, and **a misnamed letter is learnt from the whole report** (two different known words), never from one.
  **Never commit a real report**; tests build a made-up one. Keeping a copy in Documents is off by default.
- Writing import files (#118, #120): **a written file must always be one the app accepts.** The screens are looser
  than the format, so `ImportFiles.sift` runs each item through the reader; what it refuses is listed before
  saving, never dropped quietly or repaired. An unconfirmed bundled torque value is left out. Service history has
  no such file: the export and the backup are for it. A new writer gets a round-trip test in `ImportFilesTest`.
