# Service log

Rules for Log service, Undo, record files, History's list, timeline and spending, custom lines and rules, guesses, visits. Read before changing any of it; review holds a diff to every line here.

- **All service-log writes go through `RecordActions`**, which drives the snackbar and its Undo.
- **A vehicle's reminders are read through `VehicleLogs`** (#420, `:data`): the vehicle, all its schedule items,
  records, readings and settings, put together one way in dash terms. A screen, the widget or a worker doesn't
  gather those rows and call `log()` itself. Milestones is the one exception: it goes by actual mileage, with
  no swap, and builds its own on the same functions.
- **"Which reminders would move?" is asked of the log** (#430): `VehicleLog.whatIf` takes the records as they
  would stand (`DraftRecord`s, readings as typed, new ones numbered below zero) and gives the changes and the
  entries after, in dash terms. A delete, an edit, re-filed lines and an import's file all go through it; a
  screen doesn't rebuild the before and after itself. A sold vehicle moves no reminders.
- **A write that takes more than one step is one transaction** (#419): it goes in `ServiceLogWriter` (`:data`),
  which writes rows only. `RecordActions` calls it and keeps the events, focus, Undo and photo files. A process
  that dies part-way must leave the log as it was, never a record without its photos or a recall's record with
  the recall still open.
- Log service: **the odometer is never prefilled** (a prefilled reading gets saved untouched and miscounts every
  reminder); "Use current" fills it on a tap. Future dates are refused. "Due" chips only for reminders that have
  a log entry. Category buttons are tabs, not ticks (the user found the solid "open" button read as chosen).
- "When were these last done?" saves a one-line record with `enteredAsLastDone`; nothing else changes. "Import
  from a dealer record" and "assume everything was done on schedule" are deliberately not built.
- **Photo and document files are never deleted when a row is**, so Undo can put the row back: the files go in
  `RecordActions.discard` when the Undo bar ends untaken, and `RecordPhotoStore.sweep` removes leftovers on
  launch. An Undo bar lasts `LogEdits.UNDO_SECONDS` (10) and has a ✕; whatever ends it without Undo discards its
  files.
- A file is in the clear only in `cache/capture/` (while the camera writes it) and `cache/shared/` (the copy for
  the share sheet), both in `res/xml/file_paths.xml`.
- A new record, a delete and a restore end on History (`RecordActions.focus` outlines the record); an edit
  returns to the record.
- History's spending line (#285) counts records with a cost and is **hidden while a filter or search is on**,
  since a cost covers all a record's lines. Search (#284) and the sort order live in saved state; nothing is
  stored.
- Under it, labor and parts (#485, `Spending.split`) **over only the records with a priced job, saying how many of
  how many records that is** ("Line prices on 2 of 3 records: …"), worded so it can't be read as a split of the
  total above. It covers the same records as the spending line and shows only while that line does; it is absent
  while nothing is priced, and labor or parts that no record entered is left out, never shown as $0.00.
- History's timeline (`HistoryTimeline`, board 6e) is the same records, filter and search as the list, always
  newest first; the List / Timeline choice is saved state. Its "Coming up" is **only `ComingUp.next`**, from the
  Reminders tab's statuses (never a never-logged job), with a date only where vehicle home gives one
  (`paceEstimate`); it goes while a filter or search is on, like the spending line. Gaps are whole months.
- The timeline and the PDF report colour work by `ServiceGroup` (#410): seven groups from the catalog's categories,
  **no more** (small marks past seven aren't told apart). `groupColor` (`Theme.kt`) has a light and a dark step for
  each, chosen as a set with the dataviz palette validator (every pair ΔE ≥ 15, ≥ 6 for red-green colour blindness,
  3:1 on the card and the page ground); **change one only by checking the whole set again.** Every coloured mark
  sits beside its name, and Other's grey is an outline, not a fill.
- "Add custom item" (#309) only offers lines, never ticks one; filing a custom line under a line **keeps its
  wording**. So Custom stays rare.
- Custom lines and category rules (#310): **a line is Custom only when it has no home** (no `catalogId`, no
  `scheduleItemId`, not a recall's line). History's count and the sort screen use the same `CustomLines.count`,
  so they never seem to disagree. Filing keeps labels as written, in one transaction, one Undo. **A rule is the
  whole wording, never part of it**, the user's, not a vehicle's. Rules apply only where lines come in, never to
  saved records; **a file's own `service` always wins**, and on a Carfax report the rule beats Glovebox's
  wording. A rule whose line a later version drops is kept, shown in red and never applied. Changing a rule
  changes no record.
- Guesses (#312, `LineGuess`, `LineWords`): **asked last** (file's `service`, then rules, then
  `CarfaxReport.service`, then this) **and only ever a suggestion labelled a guess, never picked for the
  user.** A check line needs the text to say checked or inspected, and no other line takes text that says only
  that ("Water pump checked" must never reset the pump). A known word is never fuzzed ("lower" is never
  "blower"). **A new catalog line needs its words in `LineWords`** (`LineGuessTest` checks). The honest figure is
  the unseen set in `LineGuessTest`. Nothing is looked up.
- Shop suggestions come from every vehicle's records; what is typed is saved as typed.
- Visits (#307, `Visits`, `service_records.visitId`): **a visit only groups records.** Each keeps its own lines,
  flags, photos, recall link, cost and Undo, and reminders and spending are per record. **Nothing is linked for
  the user;** `Visits.offers` is only an offer. An id held by one record alone means nothing, so deleting needs
  no tidying. Two readings in one visit are both shown, never settled. Link and unlink have no Undo. Exports lay
  a visit out as one entry; a record on its own is written exactly as before.
- Labor and parts (#481, `design/specs/line-prices.md`): **a price belongs to a job, never split between its lines**;
  null is "not entered", not zero. **The record's cost is the total as typed and is never worked out from, or
  overwritten by, the prices.** The gap ("Tax and fees", or "Less than the line prices", naming no cause) is worked
  out on every read (`JobPrices`), never stored, and nothing about it shows while nothing is priced. A line alone
  and unpriced has no job row. Changing prices or jobs moves no reminder.
- Clear service history deletes imported records only by default, or all; typed DELETE, no Undo.
