# Export and backup

Rules for exports, the PDF report, backup, restore. Read before changing any of it; review holds a diff to every line here.

- `Exports.document` applies the options once; `Reports.build` works out the PDF report from it, and the Markdown
  writer and `ReportRenderer` only lay out what they are given. **An export is written straight into the document
  "save as" hands back** (`ExportSaver.saveExport`): nothing is kept in the app's storage, and photos are decrypted
  into memory a page at a time. A backup or import file still goes through `cache/export/`, deleted either way. An
  export is not encrypted, and every recall or schedule table says what it is.
- The PDF report (design `ReportPage1`/`ReportPage2`) is drawn on a `Canvas` in design pixels, 0.75 to a PDF point.
  **The sheet's preview is the same drawing into a bitmap; never write a PDF to preview one.** It is always light,
  on white paper, in the light `groupColor`s. "Maintenance current" follows the
  garage's rule (never with a job still to log), and nothing is guessed for a job never logged.
- Job prices (#481) are costs: "Costs and notes" off takes them out and leaves a job's wording. The Markdown puts a
  job's labor and parts beside it, its lines under it, and the record's labor, parts and gap ("worked out", no cause
  named) after the cost, from `ExportRecord.breakdown`; nothing about prices shows for a record with none. The PDF
  report has no board for prices, so page 2 takes them with no new layout (the owner's choice): "Labor $x" and
  "Parts $y" join a priced record's meta line after the cost (each only when some job has it entered; the line
  wraps when it runs past the log), and a priced job of one line puts its total on that line's chip
  (`ExportRecord.lineTotal`). A job of several lines puts its price on no chip, since it is never split. No gap on
  the report.
- Backup format: `docs/backup-format.md` and `BackupFormat.kt`. **Change the two together, and never change how a
  version-1 file is read.** Tables are read generically, so new columns need nothing. The password is never
  stored. The last-backup date is recorded only after Android has really written the file.
- **Restore is all or nothing:** read and authenticate everything first, replace every table in one
  transaction, then move the staged files in. `finishInterrupted` completes that on launch and **must run before
  the orphan sweep.** It never deletes a file the database points at, since the restore may have died before the
  database was replaced. App-lock settings are never restored. Replacing data is confirmed by typing REPLACE.
- **Nothing in a backup is trusted further than its own checks**, even with the right password: a file name in a row
  must be a plain name (`PhotoCrypto.isFileName`, checked inside the restore's transaction and again on every open
  and delete), a setting comes back only if this version knows it and only as its own type, and the reader's
  limits (`BackupFormat.MAX_…`) keep a hostile file from running the phone out of memory.
