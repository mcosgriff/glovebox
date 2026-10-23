# Toyota Camry, 2027

How `app/src/main/assets/known-schedules/toyota-camry-2027.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2027 Camry Maintenance Guide (a maintenance-only guide, 27 pages),
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-27Camry/pdf/T-MMS-27Camry.pdf`. The same
name on `assets.sia.toyota.com`, where the earlier years are, answered 403. Read on 2026-10-23.

- The guide has a maintenance chart (PDF pages 13 to 22, printed 12 to 21): jobs as rows, columns every
  5,000 miles from 5,000 to 150,000, six columns a page, each standard chart followed by a "Special
  Operating Conditions" chart. All ten pages were read in full, as page images, and the list written from
  them: 30 milestones, 5,000 miles apart. Nothing says the chart repeats after 150,000.
- **Months.** Unlike 2025 and 2026, each column prints months beside its mileage ("5k/6mo" to
  "140k/168mo"), and the milestones keep them. **The last two columns are misprinted**: they read
  145k/172mo and 150k/178mo, where every other column is 6 months per 5,000 miles; the standard and special
  charts both print it so. By the owner's ruling of 2026-10-23 (#33), those two milestones keep their
  mileage and have no months; nothing is corrected. The reminders take the chart's months (every 5,000 / 6
  and so on), which every column up to 140,000 agrees with.
- Compared with 2026 ([2026](toyota-camry-2026.md)): the same cells, row by row, standard and special
  charts alike. Differences: the months above; no "Reset Scheduled Maintenance Reminder" row; wording only
  ("Nuts and Bolts" for "Nuts or Bolts", "HEV Battery Intake Filter" and "HEV Cooling Intake Filter" on some
  pages). Because of the months and the reset row, the two years are separate lists.
- Every Camry of these years is a hybrid with one engine, so the list has no `engine` block and no
  powertrain. NHTSA's model list gives the name as "Camry" only (checked for 2027 on 2026-10-23).
- **Rows marked "(if applicable)"** are in the table with no `service`, so they never drive a reminder
  (the owner's ruling of 2026-10-23, #33): the radiator, condenser and intercooler inspection, the drive
  shaft nuts and bolts re-torque (both rows), the rear differential oil rows (the inspection, and the
  inspection and replacement when towing) and the two coolant rows, "Engine Coolant / Intercooler Coolant"
  (replace at 100,000 and 150,000; footnote: first at 100,000 miles / 120 months, then every 50,000 / 60)
  and "Inverter Coolant / Intercooler Coolant" (replace at 150,000; footnote: first at 150,000 / 180 months,
  then every 50,000 / 60). Only the intercooler's part of the coolant rows keeps the mark (below). **That
  leaves the rear differential oil with no reminder from this list**, its inspection included.
- **The coolant rows are split.** The mark on "Engine Coolant / Intercooler Coolant" and "Inverter
  Coolant / Intercooler Coolant" belongs to the intercooler: every one of these Camrys has engine coolant
  and an inverter loop. So, under the owner's ruling of 2026-10-23 (#33), each row is two jobs in the
  table: "Replace engine coolant" (`coolant`) or "Replace inverter coolant" (`inverter-coolant`), and
  "Replace intercooler coolant (if applicable)", in the table only, once per milestone. The engine coolant
  is a reminder first at 100,000 miles and the inverter coolant first at 150,000, both then every 50,000
  miles / 60 months, as the footnotes give them. The first changes' "120 months" and "180 months" count
  from the day the car went into service, which Glovebox doesn't know, so the first changes are by mileage
  only, as on 2025's and 2026's.
- The special-conditions charts mark every column the work is done at, as 2026's do: on dirt or dusty roads
  the ball joints, drive shaft boots and steering linkage are inspected and the drive shaft nuts and bolts
  re-torqued at every column, the oil changed at every column, the engine air filter inspected at every
  column but the 30,000s and replaced at the 30,000s; when towing, the automatic transmission fluid and the
  rear differential oil are inspected at 30,000, 90,000 and 150,000 and replaced at 60,000 and 120,000.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected at every column and cleaned every
  20,000 miles; the cleaning is a reminder (`hybrid-battery-filter`), the inspection is in the table only.
- The brake row is I at every column, with a footnote: visually every 5,000, thickness and runout every
  30,000. The table keeps the chart's one row.
- Left out: "Add EFI Tank Additive (Hawaii only)", at every column, which is for one region; the notes on
  oil selection; and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use: dirt or dusty roads, or short trips in
  freezing weather), tire rotation 5,000 / 6, hybrid battery cooling filter cleaning 20,000 / 24, cabin air
  filter 10,000 / 12, engine air filter 30,000 / 36, engine coolant first at 100,000 and then every
  50,000 / 60, inverter coolant first at 150,000 and then every 50,000 / 60, spark plugs 120,000 / 144.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or carrying heavy
  loads. The dusty-roads engine air filter replacement at the 30,000s falls with the normal one and has no
  reminder of its own.
- Inspections are in the table only, with a check line where the catalog has one (fluid levels, wiper
  blades, brakes). Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: 2025 and 2026 (lists of their own), 2024 and earlier (the previous generation), 2028 on
  (no guide looked for).
