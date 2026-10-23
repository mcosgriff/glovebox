# Toyota Highlander 2.5L hybrid, 2025

How `app/src/main/assets/known-schedules/toyota-highlander-2025-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2025 Warranty and Maintenance Guide for the Highlander Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-25HighlanderHV/pdf/T-MMS-25HighlanderHV.pdf`.
Toyota's owners site lists no guide for the hybrid; it was found only at that address. Downloaded on
2026-10-22 and read on 2026-10-23.

- **A chart, not a log.** The guide gives the schedule as a chart (printed pages 38 to 43): jobs as rows,
  columns every 5,000 miles from 5,000 to 150,000, each cell C (clean), I (inspect), P (perform),
  R (replace) or T (torque), with a "Special Operating Conditions" chart beside each standard one that marks
  only cells the standard chart doesn't already have. All six pages were read in full, cell by cell, and the
  list written from them: 30 milestones. Nothing says the chart repeats after 150,000, so the table doesn't.
- **No months on the milestones.** The columns are in miles only, so the milestones have none. A reminder
  has months only where the guide prints them for that job: engine oil 12 months (5,000 miles or 6 months
  on dirt or dusty roads), and the two coolants 60 months for every change after the first (below).
- **The engine:** 2.5-litre hybrid, as on [the 2020–2023 page](toyota-highlander-2020-2023-hybrid.md),
  labelled "2.5L hybrid" (`displacementL` 2.5). The 2025 petrol Highlander is a 2.4-litre turbo with a guide
  and a list of its own, so no `powertrain` is needed.
- **"(If applicable)" rows** (the owner's ruling of 2026-10-23, #33): the rear differential oil and the
  radiator row's intercooler are marked "(if applicable)" in this chart, not "(4WD)" as in the 2020–2024
  logs. They are in the table, named so, with no `service`, so they drive no reminder; they aren't in
  `allWheelDriveOnly`, because the chart doesn't name a drivetrain. That leaves no rear differential
  reminder in this list, under any use.
- **The coolant rows, split** (the owner's ruling of 2026-10-23, #33). The chart prints "Engine Coolant /
  Intercooler Coolant (if applicable)", R at 100,000 and 150,000, and "Inverter Coolant / Intercooler
  Coolant (if applicable)", R at 150,000. The "(if applicable)" mark belongs to the intercooler: every
  Highlander Hybrid has engine coolant and an inverter loop. So each row is split into "Replace engine
  coolant" (`coolant`) or "Replace inverter coolant" (`inverter-coolant`) and "Replace intercooler coolant
  (if applicable)" (table only, once per milestone). The engine coolant is a reminder first at 100,000
  miles and the inverter coolant first at 150,000, each then every 50,000 miles / 60 months, as their
  footnotes give them (engine coolant first at 100,000 miles / 120 months, inverter coolant first at
  150,000 / 180, each then every 50,000 / 60). The first changes' months count from the day the vehicle
  went into service, which Glovebox doesn't know, so the first changes are by mileage only.
- The HV battery cooling intake filter is inspected at every column and cleaned every 20,000 miles in place
  of the inspection (`hybrid-battery-filter`).
- **Special operating conditions**, each named with its condition: on dirt or dusty roads, ball joints,
  drive shaft boots, steering linkage and the engine air filter inspected and the oil replaced at the other
  milestones; the chassis nuts and bolts tightened at every milestone on dusty roads or when towing; the
  transmission fluid and the rear differential oil "(if applicable)" replaced at 60,000 and 120,000 when
  towing.
- In the table only: the EFI tank additive, which the chart lists at every column for "island use only",
  named so; the chassis nuts and bolts, a torque job; and every inspection.
- Left out: the notes on oil selection and the cabin filter. The coolant inspection is in the footnote to
  the fluid-levels row, not a row of its own, so it isn't a line here.
- The brake linings row is I at every column; its footnote says visually every 5,000 and thickness and
  runout every 30,000. The table keeps the chart's one row.
- Reminders: engine oil every 10,000 miles / 12 months (5,000 / 6 for severe use, on dirt or dusty roads),
  tire rotation every 5,000, HV battery cooling intake filter cleaned every 20,000, cabin and engine air
  filters every 30,000, spark plugs every 120,000, engine coolant first at 100,000 and inverter coolant
  first at 150,000, each then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or carrying heavy loads.
- Inspections are in the table only, with a check line where Glovebox has one (fluid levels, wiper blades).
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2026 ([its own list](toyota-highlander-2026-hybrid.md)), 2020–2024 (logs, lists of their own).
