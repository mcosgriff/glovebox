# Glovebox design

**These are the design from before the app was built, not screenshots of it.** The app has moved on
since (screens added, wording and layout changed); for what it looks like now, see the screenshots in
the main [README](../README.md#screenshots). They are still the reference for the colours, spacing and look of new screens.

Exported from the Claude design canvas: https://claude.ai/code/artifact/7f521081-34e6-46e9-99aa-641765621ad8

- `screens/*.png` — renders at 390 dp wide, 2× scale (phone screens, each at its canvas frame's height; where a screen scrolls past its frame, the source has the rest).
  Fonts fell back to system fonts in the export; the design uses IBM Plex Sans / Mono.
- `source/*.dc.html` — the exact source of each screen. Inline `style` attributes hold the real colors,
  sizes and spacing; copy text is literal. `{{name}}` holes and the `renderVals()` script at the bottom
  show the interactive state and sample data (sample data is not real).
- `source/canvas.json` — board layout and titles.
- `screens/NavMap.png` — navigation map: four bottom tabs and what hangs off each.

When implementing a screen in Compose, read both the PNG (visual) and the `.dc.html` (exact values).
Map raw hex colors to the theme tokens in `app/.../ui/theme/Theme.kt` rather than hard-coding them.

| Screen | Render | Source |
|---|---|---|
| 1 · Add vehicle (VIN) | `screens/Main.png` | `source/Main.dc.html` |
| 2 · Scan VIN | `screens/Scan.png` | `source/Scan.dc.html` |
| 3 · NHTSA decode result | `screens/Decoded.png` | `source/Decoded.dc.html` |
| 4 · Garage / vehicle home | `screens/Garage.png` | `source/Garage.dc.html` |
| 6 · Service history | `screens/History.png` | `source/History.dc.html` |
| 7 · Reminder rules (scrolls) | `screens/Reminders.png` | `source/Reminders.dc.html` |
| 8 · Recalls (scrolls) | `screens/Recalls.png` | `source/Recalls.dc.html` |
| 3b · Recommended schedule (after decode) | `screens/Schedule.png` | `source/Schedule.dc.html` |
| 9 · Settings — theme toggle is live | `screens/Settings.png` | `source/Settings.dc.html` |
| 0 · Biometric unlock | `screens/Lock.png` | `source/Lock.dc.html` |
| 4a · Garage — two vehicles | `screens/GarageMulti.png` | `source/GarageMulti.dc.html` |
| 10 · Reminders tab (all vehicles) | `screens/Upcoming.png` | `source/Upcoming.dc.html` |
| 4b · Update mileage (sheet) | `screens/UpdateMileage.png` | `source/UpdateMileage.dc.html` |
| 0a · First-run welcome / restore | `screens/Welcome.png` | `source/Welcome.dc.html` |
| 4c · Garage — empty | `screens/GarageEmpty.png` | `source/GarageEmpty.dc.html` |
| 4d · Vehicle details + mark as sold | `screens/VehicleDetails.png` | `source/VehicleDetails.dc.html` |
| 3c · Edit schedule item | `screens/EditInterval.png` | `source/EditInterval.dc.html` |
| 4e · Torque & fastener specs | `screens/Specs.png` | `source/Specs.dc.html` |
| 4 · Garage — dark | `screens/GarageDark.png` | `source/GarageDark.dc.html` |
| 6 · History — dark | `screens/HistoryDark.png` | `source/HistoryDark.dc.html` |
| 9 · Settings — dark | `screens/SettingsDark.png` | `source/SettingsDark.dc.html` |
| 0 · Unlock — dark | `screens/LockDark.png` | `source/LockDark.dc.html` |
| Navigation map | `screens/NavMap.png` | `source/NavMap.dc.html` |
| 1a · Add without a VIN | `screens/ManualAdd.png` | `source/ManualAdd.dc.html` |
| 4f · Archived vehicles | `screens/Archived.png` | `source/Archived.dc.html` |
| 6b · Export sheet (with PDF / Markdown preview) | `screens/ExportSheet.png` | `source/ExportSheet.dc.html` |
| 10a · Snooze sheet | `screens/Snooze.png` | `source/Snooze.dc.html` |
| 9a · Notifications | `screens/Notifications.png` | `source/Notifications.dc.html` |
| 9b · Create encrypted backup | `screens/BackupCreate.png` | `source/BackupCreate.dc.html` |
| 9c · About | `screens/About.png` | `source/About.dc.html` |
| 4g · Delete vehicle + its data | `screens/DeleteVehicle.png` | `source/DeleteVehicle.dc.html` |
| 9d · Delete all data | `screens/DeleteAll.png` | `source/DeleteAll.dc.html` |
| 6d · Delete record + undo | `screens/DeleteRecord.png` | `source/DeleteRecord.dc.html` |
| 6e · History — timeline view | `screens/Timeline.png` | `source/Timeline.dc.html` |
| PDF report · page 1 (infographic) — 816×1056 Letter | `screens/ReportPage1.png` | `source/ReportPage1.dc.html` |
| PDF report · page 2 (service log) — 816×1056 Letter | `screens/ReportPage2.png` | `source/ReportPage2.dc.html` |
| Prices A · state: shop, one job covers two lines | `screens/PricesLog.png` · `screens/PricesLogDark.png` | `source/PricesLog.dc.html` |
| Prices A · state: DIY, nothing priced | `screens/PricesLogDiy.png` · `screens/PricesLogDiyDark.png` | `source/PricesLogDiy.dc.html` → imports `PricesLog` |
| Prices A · state: edit, filled | `screens/PricesEdit.png` · `screens/PricesEditDark.png` | `source/PricesEdit.dc.html` → imports `PricesLog` |
| Prices A · state: "Priced together with…" menu open | `screens/PricesCombine.png` · `screens/PricesCombineDark.png` | `source/PricesCombine.dc.html` (+ `Dark`) → imports `PricesLog` |
| Prices A · state: total below the line prices | `screens/PricesLogUnder.png` · `screens/PricesLogUnderDark.png` | `source/PricesLogUnder.dc.html` (+ `Dark`) → imports `PricesLog` |
| Prices · state: record with job prices | `screens/PricesRecord.png` · `screens/PricesRecordDark.png` | `source/PricesRecord.dc.html` |
| Prices · state: record with no prices | `screens/PricesRecordPlain.png` · `screens/PricesRecordPlainDark.png` | `source/PricesRecordPlain.dc.html` → imports `PricesRecord` |
| Prices · state: record, total below the line prices | `screens/PricesRecordUnder.png` · `screens/PricesRecordUnderDark.png` | `source/PricesRecordUnder.dc.html` (+ `Dark`) → imports `PricesRecord` |
| Prices B · Log service — price sheet per line (alternative, not chosen) | `screens/PricesLogB.png` · `screens/PricesLogBDark.png` | `source/PricesLogB.dc.html` |
| **5 · Log service — build this** (due-now chips, categories, who did it, part rows, charges, photos) | `screens/PartsLog.png` · `screens/PartsLogDark.png` | `source/PartsLog.dc.html` |
| 5 · Log service — state: a figure left blank, no total, mismatched row | `screens/PartsLogBlanks.png` · `screens/PartsLogBlanksDark.png` | `source/PartsLogBlanks.dc.html` → imports `PartsLog` |
| 5 · Log service — state: picking a part (the job's lines, or type it as printed) | `screens/PartsLogPick.png` · `screens/PartsLogPickDark.png` | `source/PartsLogPick.dc.html` → imports `PartsLog` |
| **6a · Service record — build this** | `screens/PartsRecord.png` · `screens/PartsRecordDark.png` | `source/PartsRecord.dc.html` |
| 6a · Service record — state: other charges worked out | `screens/PartsRecordOther.png` · `screens/PartsRecordOtherDark.png` | `source/PartsRecordOther.dc.html` → imports `PartsRecord` |
| **6c · Edit service — build this** (odometer "Was", what this changes, delete) | `screens/PartsEdit.png` · `screens/PartsEditDark.png` | `source/PartsEdit.dc.html` → imports `PartsLog` |
| 6f · Receipt viewer (tap a photo on a record) | `screens/ReceiptViewer.png` | `source/ReceiptViewer.dc.html` |

## PDF report

`ReportPage1` and `ReportPage2` are the exported PDF, one US Letter page each (816×1056 px at 96 dpi
= 8.5×11 in). In the app, render them with Android's `PdfDocument` on a 612×792 pt Letter page (design px × 0.75 = pt). Category colors
(oil `#eb6834`, tires `#2a78d6`, brakes `#eda100`, fluids & 4WD `#1baf7a`, other `#8A8E94`) were
checked for colorblind separation; every color also has a text label, so the report reads in grayscale.

## Labor & parts per job

Spec: `specs/line-prices.md`. `PricesLog.dc.html` and `PricesRecord.dc.html` hold the real markup; the
other `Prices*.dc.html` files only import them with a different `variant` / `theme`.

All `Prices*` PNGs match the current sources (jobs version). Renders use fallback fonts; the app uses IBM Plex Sans / Mono.

## Part rows & charges (#502)

`PartsLog.dc.html` and `PartsRecord.dc.html` hold the real markup; the other `Parts*` files import them.
`PartsLog` (variants `full`, `blanks`, `edit`) and `PartsRecord` are the full screens to build for Log service,
Edit service and Service record. The `Prices*` boards are kept only as states (DIY, combine menu, total below);
where they differ from `Parts*`, `Parts*` wins. Undo after delete is on `DeleteRecord`.

- Part rows collapse to one line (name, qty × each · line, total, status chip). Tap a row to expand and
  edit it inline. Each job's parts header has **Expand all / Collapse all**. New and itemized rows open expanded.
- Expanded row: Qty is a fixed 64dp column; Each and Total share the rest. Trash removes the row, chevron collapses it.
- Qty, each and total: two typed fills the third when exact ("filled in"); inexact leaves it blank ("left blank").
  All three typed and they disagree shows "doesn't match"; the typed total is what counts.
- A row with no total is left out of the parts figure. Status notes are muted, never error red.
- A row not tied to one line says "Whole job".
- "Itemize parts" on a job with a typed parts figure moves that figure into the first row's total.
- Cost card: when the figures equal the total it shows "Adds up to the total" (ok colour). The total field shows
  thousands separators.
- Record: work lines come before "Parts as printed". Breakdown bar segments are at least 4dp wide.

## UX rules (apply to every screen)

- **Touch targets:** every tappable thing is at least 48×48dp, including text links (pad them). Chips and
  segmented options may draw at 40dp, but the touch area is 48dp (M3 default). A settings row with a switch
  toggles from anywhere on the row; the switch is labelled by the row title.
- **Bottom nav:** Garage · History · Reminders · Settings.
- **Buttons:** primary 56dp, secondary and outlined 48–52dp, corner radius 12dp. FAB stays 56dp / 18dp radius.
  One filled orange primary per screen; near-black fill means "selected" (chips, segments), never an action.
- **Type scale:** hero 28/700 (welcome, add vehicle, decoded vehicle, unlock) · headline 26/700 (top-level tab
  titles, destructive confirms) · app bar title 20/600 · card title 20/700 or 18/700 · body 15–16/400 ·
  secondary 13 · caption 12 · chip/tag 11/600 (the only 11sp use). Big figures (odometer, keypad, totals) are
  Plex Mono at 18–40.
- **Plex Mono:** money and mileage that stand alone (a value column, a field, a total, "In 1,688 mi") use Plex
  Mono. Mileage inside a sentence stays in Plex Sans.
- **Colour:** accent text on the soft orange background uses `#9A3412` (light) / `#FDBA74` (dark); `#C2410C` on
  `#FBE9DF` is under 4.5:1. Warnings that aren't errors are muted text with an info icon, not accent or red.
- **Numbers in fields** show thousands separators when not focused (`1,552.60`).

