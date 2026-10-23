# Toyota Camry, 2026

How `app/src/main/assets/known-schedules/toyota-camry-2026.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2026 Camry Warranty and Maintenance Guide,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-26Camry/pdf/T-MMS-26Camry.pdf` (the printing
online has a back cover dated 5/26). Read on 2026-10-23.

- The guide has a maintenance chart (PDF pages 42 to 47, printed 40 to 45): jobs as rows, columns every
  5,000 miles from 5,000 to 150,000, three standard charts each followed by a "Special Operating
  Conditions" chart. All six pages were read in full, as page images, and the list written from them: 30
  milestones, 5,000 miles apart. Nothing says the chart repeats after 150,000.
- **No months.** The chart's columns are in miles only, so the milestones have none. A reminder has months
  only where the guide prints them for that job: the oil change (12 months; 5,000 miles / 6 months on dirt
  or dusty roads or short trips in freezing weather, in its footnote).
- Compared with 2025 ([2025](toyota-camry-2025.md)):
  - A new standard row, "Rear Differential Oil (if applicable)", inspected at every 30,000.
  - The 2025 "(AWD only)" rows are "(if applicable)": "Drive Shaft Nuts or Bolts (if applicable)" and the
    towing rear differential oil. So nothing is in `allWheelDriveOnly`; these rows keep the chart's mark
    and nothing is inferred from 2025's.
  - The special-conditions charts mark every column the work is done at, repeating standard cells: on dirt
    or dusty roads the ball joints, drive shaft boots and steering linkage are inspected and the drive
    shaft nuts or bolts re-torqued at every column, the oil changed at every column, the engine air filter
    inspected at every column but the 30,000s and replaced at the 30,000s; when towing, the automatic
    transmission fluid and the rear differential oil are inspected at 30,000, 90,000 and 150,000 and
    replaced at 60,000 and 120,000. The table keeps those cells as drawn.
  - The dusty-roads condition is narrower: dirt or dusty roads (2025 added muddy roads and melted snow).
  - The brake row's footnote moved from the 30,000s cells to the row ("visually every 5,000, thickness and
    runout every 30,000"); the table keeps the chart's one row.
- Compared with 2027 ([2027](toyota-camry-2027.md)): the same cells, but 2027 prints months on its columns
  and has no "Reset Scheduled Maintenance Reminder" row, so the two years are separate lists.
- Every Camry of these years is a hybrid with one engine, so the list has no `engine` block and no
  powertrain. NHTSA's model list gives the name as "Camry" only (checked 2026-10-23).
- **Rows marked "(if applicable)"** are in the table with no `service`, so they never drive a reminder
  (the owner's ruling of 2026-10-23, #33): the radiator, condenser and intercooler inspection, the drive
  shaft nuts or bolts re-torque (both rows), the rear differential oil rows (the inspection, and the
  inspection and replacement when towing) and the two coolant rows, "Engine Coolant / Intercooler Coolant"
  (replace at 100,000 and 150,000; footnote: first at 100,000 miles / 120 months, then every 50,000 / 60)
  and "Inverter Coolant / Intercooler Coolant" (replace at 150,000; footnote: first at 150,000 / 180 months,
  then every 50,000 / 60). Each coolant row is one row with one mark, so it is kept whole (the coordinator's
  rule of 2026-10-23), even though the mark seems to belong to the intercooler. **That leaves the engine
  coolant, the inverter coolant and the rear differential oil with no reminder from this list**; the owner
  will decide later whether such rows are split.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected at every column and cleaned every
  20,000 miles; the cleaning is a reminder (`hybrid-battery-filter`), the inspection is in the table only.
- "Reset Scheduled Maintenance Reminder" (P at every column) is in the table, with no reminder.
- Left out: "Add EFI Tank Additive (Hawaii only)", at every column, which is for one region; the notes on
  oil selection; and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000, hybrid battery
  cooling filter cleaning 20,000, cabin air filter 10,000, engine air filter 30,000, spark plugs 120,000.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or carrying heavy loads.
  The dusty-roads engine air filter replacement at the 30,000s falls with the normal one and has no reminder
  of its own.
- Inspections are in the table only, with a check line where the catalog has one (fluid levels, wiper
  blades, brakes). Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: 2025 and 2027 (lists of their own), 2024 and earlier (the previous generation).
