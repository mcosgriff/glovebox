# Toyota Prius Prime, 2025–2026

How `app/src/main/assets/known-schedules/toyota-prius-prime-2025-2026.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which from 2025 Toyota calls the
"Prius Plug-in Hybrid" (file names `PriusPHV`). Each address came from Toyota's owners site
(`https://www.toyota.com/service/tcom/cs/manuals/downloads/prius-phv/<year>`; `prius-prime` gives no
guides from 2025). Read on 2026-10-23.

| Model year | Guide | How it was read |
|---|---|---|
| 2025 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-25PriusPHV/pdf/T-MMS-25PriusPHV.pdf` | In full, by eye (the maintenance chart, pages 42 to 47); the list was written from it |
| 2026 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-26PriusPHV/pdf/T-MMS-26PriusPHV.pdf` | Every cell compared by script with the year read in full; each difference looked at in the text |

- **A chart, not a log.** Jobs are rows and the columns are every 5,000 miles from 5,000 to 150,000, three
  pages of ten, each cell I, P, R or T, with a "Special Operating Conditions" chart after each page that
  marks only the columns the standard chart doesn't already cover. Nothing says the chart repeats after
  150,000, so the table doesn't.
- **No months on the milestones.** The columns are in miles only, so the milestones have no months. The
  reminders have months only where the guide prints them for that job: the oil footnote's 12 months (6 for
  severe use). Rotation, the filters and the spark plugs have none.
- 2025 and 2026 have the same cells. 2026 renumbers the footnotes and adds a footnote to the refrigerant row
  (below); the script's only other difference was the spacing of a row's name.
- **One engine**, the 2.0L plug-in hybrid, so no `engine` block and no `powertrain`.
- **Model name.** Toyota's guides say "Prius Plug-in Hybrid" from 2025, but NHTSA's model list for Toyota
  (`GetModelsForMakeYear`, sent only the make and year, checked 2026-10-23) still gives the car as
  `Prius Prime (PHEV)` in 2025 and 2026 (and 2027), and lists no "Prius Plug-in Hybrid". So the `model` is
  `Prius Prime (PHEV)` and there are no `modelAliases`: an alias has to be a spelling vPIC gives.
- The Prime's chart is its own: it has no drive shaft nuts, rear differential or hybrid battery intake filter
  rows, and adds the air conditioner refrigerant at 80,000. No row is marked for a drivetrain.
- **Air conditioner refrigerant:** P at 80,000 ("Add air conditioner refrigerant", `air-conditioning`, in
  the table only). The 2026 guide's footnote adds "every 80,000 miles / 96 months thereafter" in the states
  that adopted the ZEV regulation (2025 prints no footnote). That is in a footnote, past the chart's end, and
  for some states only, so it has no reminder.
- **Rows marked "(if applicable)"** are in the table with no `service`, so they never drive a reminder (the
  owner's ruling of 2026-10-23, #33, and the coordinator's rule that a combined row stays whole): the
  radiator, condenser and intercooler; the engine and intercooler coolant (replaced at 100,000 and 150,000);
  and the inverter and intercooler coolant (replaced at 150,000). **So this list has no coolant reminder**,
  though the Prime has an engine coolant and an inverter coolant: the mark is on the whole row, and the owner
  will decide later whether such rows are split.
- The brake linings row is I at every column; its footnote says visually every 5,000, and thickness and
  runout every 30,000. The table keeps the chart's one row.
- Reminders: engine oil every 10,000 miles / 12 months (5,000 / 6 for severe use: dirt or dusty roads, or
  repeated short trips below freezing), tire rotation every 5,000, cabin and engine air filters every
  30,000, spark plugs every 120,000.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or carrying heavy loads.
- Special-condition rows are named with their condition. The oil row's condition is its footnote's; the
  chassis nuts and bolts are tightened at every column on dusty roads or when towing, so that is one line.
- Inspections stay in the table only (rule 7), with a check line where Glovebox has one (fluid levels, wiper
  blades, brakes).
- Left out: "Add EFI Tank Additive (Hawaii only)", at every column (a job for one state only), the note on
  the cabin air filter, and the explanations of each item.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: 2023–2024 ([a log, a list of its own](toyota-prius-prime-2023-2024.md)) and 2027
  ([a new chart with months on its columns, a list of its own](toyota-prius-prime-2027.md)).
