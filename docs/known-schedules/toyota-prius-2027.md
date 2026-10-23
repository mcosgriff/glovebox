# Toyota Prius, 2027

How `app/src/main/assets/known-schedules/toyota-prius-2027.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2027 Maintenance Guide for the Prius,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-27Prius/pdf/T-MMS-27Prius.pdf`, found through
Toyota's owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/prius/2027`). Read on
2026-10-23. From 2027 Toyota splits the old Warranty and Maintenance Guide into a Maintenance Guide, a
warranty guide (`T-MMS-27GHVPHV`) and an emissions guide (`T-MMS-EM-27Prius`); only the Maintenance Guide
has the schedule, and only it was read.

- **A chart of a new design** (pages 13 to 22, all read by eye): six columns a page, every 5,000 miles from
  5,000 to 150,000, each cell C, I, P, R or T, with a "Special Operating Conditions" chart after each page.
  Nothing says the chart repeats after 150,000, so the table doesn't.
- **Months are printed on the columns** ("5k/6mo" … "140k/168mo"), so the milestones have them, and the
  reminders take the columns' months. **The last two columns are misprinted:** they read "145k/172mo" and
  "150k/178mo" where every other column is 6 months a 5,000 miles (174 and 180). By the owner's ruling of
  2026-10-23 (#33) those two milestones keep their mileage and have no months; nothing is corrected.
- **What differs from 2025–2026** ([a list of its own](toyota-prius-2025-2026.md)): the standard cells are the
  same (compared by script, and every page looked at), but the special-conditions charts now mark every
  column, repeating the standard chart's cells, and add inspections when towing: the oil is replaced at
  every column (not only the odd 5,000s), the engine air filter is inspected, or replaced at the 30,000s, at
  every column, the ball joints, drive shaft boots, drive shaft nuts and bolts and steering linkage at every
  column, and the automatic transmission fluid and rear differential oil are inspected at 30,000, 90,000 and
  150,000 when towing as well as replaced at 60,000 and 120,000. The table keeps each special cell as a job
  named with its condition, beside the standard job.
- **One engine**, the 2.0L hybrid, so no `engine` block and no `powertrain`. **Model name:** NHTSA's model list
  for Toyota (`GetModelsForMakeYear`, checked 2026-10-23) gives `Prius` for 2027. The `Prius Prime (PHEV)` has
  its own list ([2027](toyota-prius-prime-2027.md)).
- **Rows marked "(if applicable)"** are in the table with no `service`, so they never drive a reminder (the
  owner's ruling of 2026-10-23, #33, and the coordinator's rule that a combined row stays whole): the
  radiator, condenser and intercooler; the drive shaft nuts and bolts; the rear differential oil; the engine
  and intercooler coolant (replaced at 100,000 and 150,000); and the inverter and intercooler coolant
  (replaced at 150,000). **So this list has no coolant reminder and no rear differential oil reminder.**
- **AWD.** No line is marked "(AWD)"; the drive shaft and rear differential rows are "(if applicable)", which
  is not a drivetrain mark, so nothing is in `allWheelDriveOnly`.
- **Hybrid battery intake filter:** cleaned at every 20,000 and inspected at every other column. The cleaning
  is a reminder every 20,000 miles / 24 months (`hybrid-battery-filter`).
- The brake linings row is I at every column; its footnote says visually every 5,000, and thickness and
  runout every 30,000. The table keeps the chart's one row.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use: dirt or dusty roads, or repeated short
  trips below freezing), tire rotation 5,000 / 6, hybrid battery intake filter 20,000 / 24, cabin and engine
  air filters 30,000 / 36, spark plugs 120,000 / 144.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or carrying heavy
  loads.
- Inspections stay in the table only (rule 7), with a check line where Glovebox has one (fluid levels, wiper
  blades, brakes).
- Left out: "Add EFI Tank Additive (Hawaii only)", at every column (a job for one state only), the note on
  the cabin air filter, and the explanations of each item.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: 2023–2026 (lists of their own, [2023–2024](toyota-prius-2023-2024.md) and
  [2025–2026](toyota-prius-2025-2026.md)).
