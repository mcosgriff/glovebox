# Toyota Prius, 2025–2026

How `app/src/main/assets/known-schedules/toyota-prius-2025-2026.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year. Each address came from Toyota's
owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/prius/<year>`).
Read on 2026-10-23.

| Model year | Guide | How it was read |
|---|---|---|
| 2025 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-25Prius/pdf/T-MMS-25Prius.pdf` | In full, by eye (the maintenance chart, pages 40 to 45); the list was written from it |
| 2026 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-26Prius/pdf/T-MMS-26Prius.pdf` | Every cell compared by script with the year read in full; each difference looked at on the page |

- **A chart, not a log.** Jobs are rows and the columns are every 5,000 miles from 5,000 to 150,000, three
  pages of ten, each cell C (clean), I (inspect), P (perform), R (replace) or T (torque), with a "Special
  Operating Conditions" chart after each page that marks only the columns the standard chart doesn't
  already cover. Nothing says the chart repeats after 150,000, so the table doesn't.
- **No months on the milestones.** The columns are in miles only, so the milestones have no months. The
  reminders have months only where the guide prints them for that job: the oil footnote's 12 months (6 for
  severe use). Rotation, the filters and the spark plugs have none.
- 2025 and 2026 have the same cells. 2026 renumbers the footnotes, calls the battery "HEV" for "HV", and
  marks the inverter coolant row "(if applicable)" on the row; 2025 marks it in the row's footnote
  ("inverter/intercooler coolant (if applicable)"). Both were looked at on the page; the row is read as
  marked in both years (below).
- **One engine**, the 2.0L hybrid, so no `engine` block and no `powertrain`. **Model name:** NHTSA's model list
  for Toyota (`GetModelsForMakeYear`, checked 2026-10-23) gives `Prius` for 2025 and 2026. The
  `Prius Prime (PHEV)` has its own list ([2025–2026](toyota-prius-prime-2025-2026.md)).
- **Rows marked "(if applicable)"** are in the table with no `service`, so they never drive a reminder (the
  owner's ruling of 2026-10-23, #33, and the coordinator's rule that a combined row stays whole): the
  radiator, condenser and intercooler; the drive shaft nuts and bolts (torqued every 15,000, and at the other
  columns on dusty roads); the rear differential oil (inspected every 30,000, replaced at 60,000 and 120,000
  when towing); the engine and intercooler coolant (replaced at 100,000 and 150,000); and the inverter and
  intercooler coolant (replaced at 150,000). **So this list has no coolant reminder and no rear differential
  oil reminder**, though a Prius has an engine coolant and an inverter coolant and the AWD has a rear
  differential: the "(if applicable)" mark is on the whole row, and the owner will decide later whether such
  rows are split.
- **AWD.** These guides mark no line "(AWD)" (the 2023–2024 log did); the drive shaft and rear differential
  rows are "(if applicable)" instead, which is not a drivetrain mark, so nothing is in `allWheelDriveOnly` and
  those rows are kept for every Prius.
- **Hybrid battery intake filter:** cleaned at every 20,000 (C) and inspected at every other column (I). The
  cleaning is a reminder every 20,000 miles (`hybrid-battery-filter`); the inspection is in the table only.
- The brake linings row is I at every column; its footnote says visually every 5,000, and thickness and
  runout every 30,000. The table keeps the chart's one row.
- Reminders: engine oil every 10,000 miles / 12 months (5,000 / 6 for severe use: dirt or dusty roads, or
  repeated short trips below freezing), tire rotation every 5,000, hybrid battery intake filter every 20,000,
  cabin and engine air filters every 30,000, spark plugs every 120,000.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or carrying heavy loads.
- Special-condition rows are named with their condition. The oil row's condition is its footnote's (dirt or
  dusty roads, or short trips below freezing); the chassis nuts and bolts are tightened at every column on
  dusty roads or when towing, so that is one line.
- Inspections stay in the table only (rule 7), with a check line where Glovebox has one (fluid levels, wiper
  blades, brakes).
- Left out: "Add EFI Tank Additive (Hawaii only)", at every column (a job for one state only), the note on
  the cabin air filter, and the explanations of each item.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: 2023–2024 ([a log, a list of its own](toyota-prius-2023-2024.md)) and 2027
  ([a new chart with months on its columns, a list of its own](toyota-prius-2027.md)).
