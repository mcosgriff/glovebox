# Service log

Rules for Log service, Undo, record files, History's list, timeline and spending, custom lines and rules, guesses, visits, reading invoices. Read before changing any of it; review holds a diff to every line here.

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
- "When were these last done?" saves a one-line record with `enteredAsLastDone`; nothing else changes. "Assume
  everything was done on schedule" is deliberately not built.
- Read an invoice (#484, `docs/adr/0014-reading-a-shops-invoice.md`, `InvoiceFills`, `InvoiceCard`): on a new Log
  service only, a shop's emailed PDF **fills the form and saves nothing**; the owner checks it and saves. Wording says
  check it against the invoice, never "detected". A figure printed and not vouched for is left blank and outlined
  "Couldn't read" until something is typed there. One invoice is one record: each service a line of the shop's wording
  (under the line the owner's rule files it under, otherwise custom, with guesses only through "File under…") in a job
  of its own with its vouched labor and parts; what was already ticked stays. Recommended work and "odometer in" go to
  the notes, never to the lines. A VIN that isn't the vehicle's is a question by its last four, never a block. The shop
  takes a saved shop's spelling. The read odometer is an offer from the invoice, so the never-prefilled rule doesn't
  cover it. "What was read" is viewing only. The PDF is held in memory and kept, encrypted, among the vehicle's
  documents only while "Keep the invoice" is on; Undo of the record leaves it there. Debug builds alone write
  `files/invoice-traces/` (Delete all data removes it). A scan has no text layer: photos are #513. Filing a custom line
  carries its job and prices with it.
- **Photo and document files are never deleted when a row is**, so Undo can put the row back: the files go in
  `RecordActions.discard` when the Undo bar ends untaken, and `RecordPhotoStore.sweep` removes leftovers on
  launch. An Undo bar lasts `LogEdits.UNDO_SECONDS` (10) and has a ✕; whatever ends it without Undo discards its
  files.
- A file is in the clear only in `cache/capture/` (while the camera writes it) and `cache/shared/` (the copy for
  the share sheet), both in `res/xml/file_paths.xml`.
- A new record, a delete and a restore end on History (`RecordActions.focus` outlines the record); an edit
  returns to the record.
- History's filters are one row. A category with two or more jobs logged on this vehicle
  (`ServiceCatalog.jobsIn`) **is stepped into**: the row becomes a back arrow (to All), the category's own chip
  (all of it) and its jobs, to show one alone (an oil change without the rest of Oil & engine). A category with
  one job is chosen in place. A job matches a line by its `catalogId` only, never by `covers`; a second tap on it
  goes back to the category, and another vehicle lets it go. It counts as a filter wherever a filter is on.
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
- Part rows and the invoice's charges (#502, `design/specs/part-rows.md`, `PartRows`, `InvoiceCharges`): a job keeps
  one labor figure; its parts are one figure or **the invoice's part rows, whose totals make the parts figure on every
  read** (a job with rows stores no `partsCents`). **Two of qty, each and total fill the third on the form only when it
  comes out exact**; otherwise it stays blank, and nothing is rounded. A filled-in figure is worked out again when a
  typed one changes; typing over it makes it the user's. All three typed and disagreeing says so, and the typed total
  counts. A row without a total is left out and said so; no quantity is assumed. **A row's line is a label only**: it
  never puts a price on a line or splits the labor. **Part is typed as the invoice prints it or picked from the job's
  lines**: a picked row is named by its line and saves no wording; a typed one keeps its wording and, in a job of more
  than one line, asks "For line". Typing a line's own name picks it. The line is never worked out from the wording. Shop supplies, tax, and fees and sublet are the record's own
  figures; the gap counts them, and is "Tax and fees" only while neither tax nor fees is entered, "Other charges" once
  either is. An import or a saved record is never filled in: only the form does that.
- Reading an invoice (#483, `InvoiceReader`, `:domain`; the screens are #484): **it only fills the form.** No
  per-shop templates: services come from a heading, a table whose header names labor and parts, a line carrying a
  subtotal, or (with none of those) priced lines whose prices add up to the subtotal. **A figure is taken only when
  a sum the invoice prints agrees** (parts rows, a service's subtotal, every service's labor, parts, fees and sublet
  against the totals block, the totals block or the subtotals against the grand total); otherwise it is left out,
  never guessed or worked out from the others. **Jobs come only from the services, up to the totals**: recommended,
  declined or deferred work and an inspection report's priced items are text offered for notes, never a job. A VIN
  must pass its check digit; on a line labelled VIN, at most two look-alikes (S/5, B/8, Z/2, G/6) are folded, and
  only when exactly one way passes. The odometer follows Read dash's range from the last reading on or before the
  invoice's date; out below in, or more than 1,000 past it, leaves both out. A date after today is left out. Wording
  goes through the user's rules, then `LineGuess.guesses` (a service may guess more than one line), only offered.
  `:domain` returns a typed trace of every decision; `app` words it. The honest figure is the unseen set in
  `InvoiceUnseenTest`. **Never commit a real invoice**: real ones go in the git-ignored
  `domain/src/test/private-invoices/` (`PrivateInvoicesTest`).
- Clear service history deletes imported records only by default, or all; typed DELETE, no Undo.
