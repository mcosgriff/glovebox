# Toyota Prius Prime, 2027

How `app/src/main/assets/known-schedules/toyota-prius-prime-2027.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2027 Maintenance Guide for the Prius Plug-in Hybrid,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-27PriusPHV/pdf/T-MMS-27PriusPHV.pdf`, found
through Toyota's owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/prius-phv/2027`).
Read on 2026-10-23. From 2027 Toyota splits the old Warranty and Maintenance Guide into a Maintenance Guide, a
warranty guide (`T-MMS-27GHVPHV`) and an emissions guide (`T-MMS-EM-27PriusPHV`); only the Maintenance Guide
has the schedule, and only it was read.

- **A chart of a new design** (PDF pages 13 to 22, all read by eye): six columns a page, every 5,000 miles
  from 5,000 to 150,000, each cell I, P, R or T, with a "Special Operating Conditions" chart after each page.
  Nothing says the chart repeats after 150,000, so the table doesn't.
- **Months are printed on the columns** ("5k/6mo" … "140k/168mo"), so the milestones have them, and the
  reminders take the columns' months; the coolants take their footnotes' (below). **The last two columns are
  misprinted:** they read "145k/172mo" and "150k/178mo" where every other column is 6 months a 5,000 miles
  (174 and 180). By the owner's ruling of 2026-10-23 (#33) those two milestones keep their mileage and have no
  months; nothing is corrected.
- **What differs from 2025–2026** ([a list of its own](toyota-prius-prime-2025-2026.md)): the standard cells
  are the same (compared by script, and every page looked at), but the special-conditions charts now mark
  every column, repeating the standard chart's cells, and add an inspection when towing: the oil is replaced
  at every column (not only the odd 5,000s), the engine air filter is inspected, or replaced at the 30,000s,
  at every column, the ball joints, drive shaft boots and steering linkage at every column, and the
  automatic transmission fluid is inspected at 30,000, 90,000 and 150,000 when towing as well as replaced at
  60,000 and 120,000. The table keeps each special cell as a job named with its condition, beside the
  standard job.
- **One engine**, the 2.0L plug-in hybrid, so no `engine` block and no `powertrain`.
- **Model name.** The guide says "Prius Plug-in Hybrid", but NHTSA's model list for Toyota
  (`GetModelsForMakeYear`, sent only the make and year, checked 2026-10-23) gives the car as
  `Prius Prime (PHEV)` for 2027 and lists no "Prius Plug-in Hybrid". So the `model` is `Prius Prime (PHEV)`
  and there are no `modelAliases`.
- The Prime's chart is its own: it has no drive shaft nuts, rear differential or hybrid battery intake filter
  rows, and adds the air conditioner refrigerant at 80,000. No row is marked for a drivetrain.
- **Air conditioner refrigerant:** P at 80,000 ("Add air conditioner refrigerant", `air-conditioning`, in
  the table only). Its footnote adds "every 80,000 miles / 96 months thereafter" in the states that adopted
  the ZEV regulation. That is in a footnote, past the chart's end, and for some states only, so it has no
  reminder.
- **Coolant rows are split** (the owner's ruling of 2026-10-23, #33). The chart's engine and intercooler
  coolant row (replaced at 100,000 and 150,000) and its inverter and intercooler coolant row (replaced at
  150,000) are marked "(if applicable)". The engine coolant row prints "(if applicable)" at the 150,000 column
  but not at the 100,000 one (pages 18 and 20); its footnote says "engine/intercooler coolant (if applicable)"
  on both, so the row is read the same in both places. The mark belongs to the intercooler: the Prime has an
  engine coolant and an inverter coolant. So the first row is two table jobs, "Replace engine coolant"
  (`coolant`) and "Replace intercooler coolant (if applicable)" (table only), and the second is "Replace
  inverter coolant" (`inverter-coolant`) with the same intercooler row (one intercooler row per milestone).
  The engine coolant is a reminder first at 100,000 then every 50,000 miles / 60 months, and the inverter
  coolant first at 150,000 then every 50,000 / 60, as the guide's footnotes give them. Their first changes'
  "120 months" and "180 months" count from the day the car went into service, which Glovebox doesn't know, so
  the first changes are by mileage only.
- **Rows marked "(if applicable)"** are in the table with no `service`, so they never drive a reminder (the
  owner's ruling of 2026-10-23, #33): the radiator, condenser and intercooler.
- The brake linings row is I at every column; its footnote says visually every 5,000, and thickness and
  runout every 30,000. The table keeps the chart's one row.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use: dirt or dusty roads, or repeated short
  trips below freezing), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark plugs
  120,000 / 144, engine coolant first at 100,000 then every 50,000 / 60, inverter coolant first at
  150,000 then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or carrying heavy
  loads.
- Inspections stay in the table only (rule 7), with a check line where Glovebox has one (fluid levels, wiper
  blades, brakes).
- Left out: "Add EFI Tank Additive (Hawaii only)", at every column (a job for one state only), the note on
  the cabin air filter, and the explanations of each item.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: 2023–2026 (lists of their own, [2023–2024](toyota-prius-prime-2023-2024.md) and
  [2025–2026](toyota-prius-prime-2025-2026.md)).
