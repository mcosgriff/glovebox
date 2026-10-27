# Toyota Corolla sedan (gas), 2026

How `app/src/main/assets/known-schedules/toyota-corolla-2026-sedan.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2026 Corolla Warranty and Maintenance Guide (the sedan's; the printing online has a back cover
dated 5/26),
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-26Corolla/pdf/T-MMS-26Corolla.pdf`. Read on
2026-10-26.

- **Why a list per body (#185).** NHTSA gives the sedan and the hatchback both as "Corolla" with the same 2.0L
  engine, and in 2026 their special-operating-conditions charts differ (the hatchback's is on
  [its own page](toyota-corolla-2026-hatchback.md)). So this list has `"body": "sedan"`, as the owner ruled on
  2026-10-25, and a 2026 gas Corolla matches it only when its body style is on record as a sedan: from NHTSA's
  "Sedan/Saloon", or the owner's pick. With none on record, setup asks.
- **A chart, as in 2025.** Jobs are rows and mileages are columns, every 5,000 from 5,000 to 150,000, on three
  pages, each with a special-operating-conditions page beside it (PDF pages 39 to 44, printed 38 to 43). The
  guide was compared with the 2025 sedan's, from which [the 2025 list](toyota-corolla-2025-2.0.md) was written,
  page against page: a script compared the words and the cells of each row, and every difference was looked at;
  the 5,000–50,000 special-conditions page (PDF page 40) was seen as an image. **The cells are the same in every row**, so the
  table and the reminders are the 2025 list's. What differs:
  - footnote numbering, the EFI additive row's footnote (gone) and line breaks;
  - **the engine oil's footnote is gone.** 2025 gave the ordinary change "after 12 months, or over 10,000 miles",
    and 5,000 miles / 6 months on dirt or dusty roads or short trips in freezing weather. 2026 has no footnote on
    the oil row. Its "Explanation of Maintenance Items" still says the oil and filter are changed at 5,000 miles
    or six months under any of the special operating conditions, so the severe-use interval keeps 5,000 / 6.
    **The ordinary change has no months:** nothing in the 2026 guide gives one for it, and the general
    sentence that the car be seen "every 5,000 miles or six months" is about visits, not this job. That is a
    gap, left as it is, not filled from 2025.
- The gas sedan has the 2.0L only and the chart names no engine; the Corolla Hybrid's 1.8L has a list of its own
  ([toyota-corolla-2025-2026-hybrid](toyota-corolla-2025-2026-hybrid.md)), so this list has `engine` 2.0L too.
- **No months at the milestones.** The chart's columns carry mileages only. A reminder keeps months only where the
  guide prints them for that job: the special-conditions oil change (6) and the engine coolant after its first
  change (60, footnote).
- **Rows marked "(if applicable)"** are in the table as printed, with no service line, so none drives a reminder
  (the owner's ruling of 2026-10-23): the radiator, condenser and intercooler inspection, and the intercooler's
  part of the coolant row.
- **The coolant row is split**, as in 2025: "Engine Coolant / Intercooler Coolant (if applicable)" at 100,000
  and 150,000 (footnote: first at 100,000 miles / 120 months, then every 50,000 / 60) is "Replace engine coolant"
  (`coolant`), a reminder first at 100,000 miles and then every 50,000 / 60 months, and "Replace intercooler
  coolant (if applicable)", in the table only. The first change's "120 months" counts from the day the car went
  into service, which Glovebox doesn't know, so the first change is by mileage only.
- **Automatic transmission fluid** is replaced at 60,000 and 120,000 when towing or idling for long periods (one
  cell, both conditions' footnotes), so it is one line, "· towing or long idling".
- Left out: adding Toyota's EFI tank additive (a row for Hawaii, Puerto Rico and Saipan only), the notes on oil
  selection, and the explanations of each item.
- Reminders: engine oil 10,000 mi (5,000 / 6 for severe use), tire rotation 5,000, cabin and engine air filters
  30,000, engine coolant first at 100,000 and then every 50,000 / 60, and spark plugs 120,000.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or idling for long periods.
- The inspections are in the table only, with the check line where Glovebox has one (fluid levels, wiper blades,
  brakes, drive belts). Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: the 2026 hatchback ([its own list](toyota-corolla-2026-hatchback.md)), the 2026 hybrid
  ([its own list](toyota-corolla-2025-2026-hybrid.md)), and 2019–2025 (lists of their own).
