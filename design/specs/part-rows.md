# Part rows and invoice charges (#502)

Builds on `line-prices.md` (#481), whose rules all still hold. Design: `design/screens/Parts*.png` (light +
`*Dark.png`), source `design/source/PartsLog.dc.html` and `design/source/PartsRecord.dc.html`; the other
`Parts*` files only import those two. The Log boards start at "Work and prices"; the sections above it are as
the app has them now.

## Data

- **A job keeps one labor figure.** Its parts are either one figure (as in #481) or a list of **part rows**:
  the invoice's own parts table for that job. Labor is never itemized.
- A part row: `wording` as printed (free text), `quantity`, `eachCents`, `totalCents`, and an optional tag to
  one of the job's lines. Each is null when not entered. An untagged row reads "Whole job". **The tag is a
  label only**: it never puts a price on a line, for reminders, spending or anything else, and the job's labor
  is never split.
- New table `service_record_job_parts`: `id`, `jobId` (FK to `service_record_jobs`, `ON DELETE CASCADE`),
  `wording`, `quantity` (decimal as typed, text), `eachCents: Long?`, `totalCents: Long?`, `itemId: Long?`
  (FK to `service_record_items`, `ON DELETE SET NULL`), `position`.
- A job with part rows stores no `partsCents`: its parts figure is **the sum of its rows' totals, worked out
  on every read**. A row without a total is left out of that sum and said so; nothing is assumed (no
  quantity of 1).
- Three record-level figures on `service_records`, each `Long?`, null = not entered: `shopSuppliesCents`,
  `taxCents`, `feesCents` ("Fees and sublet"). They belong to the invoice, not to a job.
- The record's `costCents` stays the total as typed, never worked out from or overwritten by any of these.
- Carried by backup and restore, the Markdown export, the PDF report's page 2, and **history-import schema
  version 3**. Versions 1 and 2 are read exactly as before (`docs/rules/import.md`).
- Room migration with its test, and the new schema JSON committed (`docs/rules/database.md`).

## Qty, each and total (pure Kotlin, :domain, unit-tested)

- On the form, **any two typed fill the third**, shown as "filled in" with a dashed field. Typing into a
  filled-in field makes it typed; clearing one of the two typed clears what was filled in.
- **A figure is filled in only when it comes out exact**: a total only in whole cents (5.7 × $8.99 is
  left blank), each only in whole cents ($20.00 ÷ 3 is left blank), a quantity only to three decimals.
  Otherwise it stays blank and says "left blank" with the sum ("$20.00 ÷ 3 isn't whole cents.").
- All three typed and quantity × each ≠ total: "doesn't match" ("2 × $7.50 is $15.00, not $14.00. The total
  as printed is what counts."). Nothing is changed; the typed total is the one used.
- No total: "no total", "Left out of the parts figure.", and under the rows "1 row has no total, so it isn't
  in the parts figure."
- Status notes are muted text with a small dashed tag, never error red.

## Gap

- `gap = total − (labor + parts + shop supplies + tax + fees)`, counting only figures entered; worked out on
  every read, never stored; only when there is a total and anything is entered.
- gap > 0: **"Tax and fees"** while neither tax nor fees is entered, otherwise **"Other charges"**.
- gap < 0: **"Less than the figures above"** (was "Less than the line prices"; the English change is its own
  PR). No cause is named.
- gap == 0: the Cost card says "Adds up to the total" in the ok colour; nothing else about a gap.
- No total but figures entered: "The figures above add up to $X. Type the invoice total if you have it."

## Log service / Edit service

- A job's Labor field stays; its Parts is a field, or (with rows) a dashed "from rows" sum. "Itemize parts"
  turns the field into rows, moving a typed parts figure into the first row's total so nothing typed is lost.
  Removing the last row turns it back into a field.
- Rows collapse to one line (name or "Unnamed part", qty × each · line, total, status tag); tapping one opens it
  inline. The parts header has **Expand all / Collapse all**. A new row, and the first after "Itemize parts",
  opens expanded.
- An open row: Part (wording); Qty in a fixed 64dp column, Each and Total sharing the rest; "For line"
  picker (the job's lines, and "Whole job") only when the job has more than one line; trash removes the row,
  the chevron collapses it. "+ Add part row" under the rows. All targets 48dp.
- Cost card: an "On the whole invoice" group (Shop supplies, Tax, Fees and sublet, each a money field) above
  Total cost, then Labor on lines, Parts on lines and each charge entered, then the gap row.

## Service record

- A job shows its total, "Labor $x  Parts $y", its lines (with Next due), then **"Parts as printed"**: each row's
  wording, "qty × each · line", and its total.
- Cost breakdown: one bar of labor, parts, shop supplies, tax and fees (only those entered) and the dashed
  gap segment when positive; every segment at least 4dp wide; 2dp gaps. Rows with swatch, label, share and
  amount, then Total cost.
- Bar colours, chosen as a set with the dataviz palette validator (adjacent pairs, light and dark):
  labor and parts as in #481; shop supplies `#2A78D6` / dark `#3987E5`, tax `#EDA100` / `#C98500`, fees
  `#1BAF7A` / `#199E70`. Every segment has a labelled row, which covers tax and fees falling under 3:1 on the
  light surface.

## Out of scope here

- History's labor and parts split (#485) stays as it is; adding the charges there is a follow-up.
- The invoice reader (#483/#484) filling part rows and the charges is a follow-up, under its rule that a
  figure is taken only when a sum the invoice prints agrees.

## Tests to include

- Fill: each pair filling the third exactly; inexact each way; decimal quantity; typing over a filled-in
  figure; clearing a typed one; "doesn't match".
- A job's parts from rows with one row missing its total; the gap with each charge entered or not, and each
  label.
- Migration; backup round trip; import v1 and v2 unchanged, v3 round trip.
- Screenshot tests: Log service with rows (collapsed and open) and the charges, and the record with them,
  light and dark.
