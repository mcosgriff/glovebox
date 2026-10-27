# Labor and parts prices per job (#481)

Design: `design/screens/Prices*.png` (light + `*Dark.png`), source `design/source/PricesLog.dc.html`
and `design/source/PricesRecord.dc.html`. Layout **A** is the chosen one; `PricesLogB` is the rejected
alternative, kept for reference only. The renders still show prices on single lines; the job card with
more than one line (below) is the one change still to draw. Only the new sections are built from these
renders: the category picker and the header stay as the app has them now.

## Data

- **A price belongs to a job, not a line.** A shop prices a job ("Remove & Replace Spark Plugs and Ignition
  Coils": one labor, one parts figure) that can cover more than one catalog line (spark plugs, ignition
  coils). Splitting it between the lines would show figures the shop never printed, so it is never split.
- New table `service_record_jobs`: `id`, `recordId` (FK to `service_records`, `ON DELETE CASCADE`), `wording`
  (the shop's name for the job, as typed or read), `laborCents: Long?`, `partsCents: Long?`, `position`.
  `service_record_items` gets `jobId: Long?` (FK, `ON DELETE SET NULL`).
- A line with no job is a line as today: no price. A job holds one or more lines of the same record. A job
  with one line whose wording is that line's name is shown as just the line (the renders' case).
- Null means "not entered", which is different from zero.
- Fees, sublet, shop supplies and tax are not stored per job: they fall into "Tax and fees" (below).
- The record's `costCents` stays exactly what the user typed (the invoice grand total). It is **never**
  computed from, or overwritten by, job prices.
- US dollars, integer cents. Typed money goes through `typedDecimal()` and `Money.parseCents`, as the total
  cost does today; no new parser.
- Jobs and their prices are carried by the backup and restore, the exports (Markdown, PDF report page 2) and
  a **new version of the history-import schema**. **A version-1 file is read exactly as before**
  (`docs/rules/import.md`): schema, Kotlin reader and examples change together.
- Room migration with its test, and the new schema JSON committed (`docs/rules/database.md`).

## Derived values (pure Kotlin, :domain, unit-tested)

- `laborSum` / `partsSum` = sum of non-null job values; `priced` = any job has labor or parts.
- `gap = total − (laborSum + partsSum)`, only when `total != null && priced`.
  - gap > 0 → **"Tax and fees"** = gap.
  - gap < 0 → **"Less than the line prices"** = |gap|. No cause is named: a discount and a typo in a price
    look the same, and naming one would be guessing for the user. Never an error.
  - gap == 0 → nothing.
- No prices → nothing about breakdowns or gaps shows anywhere.
- Prices but no total → helper "Line prices add up to $X. Type the invoice total if you have it."
  The total is never filled in for the user.
- Worked-out values are never stored; recompute on read.

## Log service / Edit service (layout A)

- New sections after Who did it: **Work and prices**, then **Cost**, then Notes and Save. The rest of the
  screen is unchanged.
- **Work and prices · N lines** lists every ticked line from all tabs (name + category), each as its own job
  until the user combines them.
- **Combining:** a line can be put with another into one job ("Priced together with…"), which then takes one
  Labor and one Parts and lists its lines under its wording. Splitting a job back gives each line its own,
  unpriced. (Still to draw.)
- Price fields are hidden while no job has a price: one row "+ Add labor and parts / For an itemized
  invoice". Once any price exists they stay visible (Edit opens expanded).
- Each priced job shows its total (labor + parts) on the right.
- **Cost** card: Total cost field (helper: "The grand total from the invoice, as printed." for a shop,
  "What you spent on parts and supplies." for DIY). When priced: "Labor on lines", "Parts on lines", then
  the gap row on a tinted background with the dashed "worked out" tag and its caption. Display only.
- Edit service = same screen, title "Edit service", button "Save changes". Changing prices or jobs moves no
  reminder.

## Service record

- Header keeps Odometer and Total cost.
- Work done: a priced job shows its total (right) and "Labor $x  Parts $y" under its wording, with its lines
  (and each line's "Next due") under that. Unpriced lines show no price UI.
- **Cost breakdown** card only when priced: one stacked bar (labor, parts, dashed gap segment; 2dp gaps),
  rows with swatch + label + share % + amount, then Total cost. Caption names unpriced lines ("Tire
  rotation has no price listed."). No bar segment for a negative gap: its row only.

## Visual rules

- Money and mileage in IBM Plex Mono (`PlexMono`); other text IBM Plex Sans.
- Touch targets ≥ 48dp; text contrast ≥ 4.5:1 in light and dark (`shoot` checks both). Theme tokens from
  `ui/theme/Theme.kt`; the hex values in the `.dc.html` files map to them.
- Light mode shows importance with tints (accent-soft), never black panels.
- All wording in `res/values/strings.xml`; figures carry their unit; no phrase built by cutting up another.

## Tests to include

- Gap cases: positive, negative, zero, no total, no prices, labor-only, parts-only, a job with two lines.
- Room migration; backup round trip; history import v1 unchanged and the new version round trip
  (`ImportFormatTest`, `ImportFilesTest`).
- Screenshot tests (`ScreensScreenshotTest`) for Log service priced and unpriced, and the record with and
  without prices, light and dark.
