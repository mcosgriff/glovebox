# Toyota RAV4 Hybrid, 2026

How `app/src/main/assets/known-schedules/toyota-rav4-2026-hybrid.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). The plug-in hybrid of the same year has its own list,
[toyota-rav4-2026-plug-in](toyota-rav4-2026-plug-in.md).

Source: Toyota's Warranty and Maintenance Guide for the 2026 RAV4 Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-26RAV4HV/pdf/T-MMS-26RAV4HV.pdf` (52 pages,
dated November 2025). Read on 2026-10-23.

- The guide's maintenance chart (PDF pages 40 to 45) was read in full, by eye, and the list written from it.
  A script that reads each cell's position on the page gave the same grid.
- **The layout is a chart**, not the page-per-milestone log of the 2017–2024 4Runner: jobs are rows, and
  the columns are every 5,000 miles from 5,000 to 150,000 (30 milestones). Each page of ten columns has a
  "Special Operating Conditions" page after it. **No months are printed on the columns**, so the milestones
  have none. The introduction's "every 5,000 miles or six months" is not turned into months for each
  column. The chart stops at 150,000, and nothing in the guide says it repeats, so the table doesn't.
- **Months on the reminders** only where the guide prints them for that job: oil under special operating
  conditions is every 5,000 miles or six months (the page explaining the items). Ordinary oil, tire
  rotation, the filters, spark plugs and the battery filter have no months in the guide, so they have none.
  The coolants take their footnotes' 60 months for every change after the first; the first change's months
  count from the in-service date, which Glovebox doesn't know, so the first change is by mileage only.
- **Engine and powertrain.** The guide names no engine size. Toyota's newsroom
  (`https://pressroom.toyota.com/vehicle/2026-toyota-rav4/`) gives the 2026 RAV4 a
  2.5-litre four-cylinder in both the hybrid and the plug-in hybrid, and no petrol-only RAV4 that year. As
  the two share a size and their guides differ, both lists carry `powertrain` (`hybrid` here, `plug-in`
  there), so a 2026 RAV4 whose powertrain isn't on record is asked which it is.
- **Model name.** vPIC's model list for Toyota 2026 (`GetModelsForMakeYear`, read 2026-10-23) has `RAV4`
  and `RAV4 Prime (PHEV)`. The hybrid is `RAV4`; the list has no other spelling.
- **Drivetrain.** The chart has no "2WD", "4WD" or "AWD" marks. Toyota sells this hybrid with front- or
  all-wheel drive, and the chart marks the rear differential oil rows "(if applicable)". By the owner's
  ruling of 2026-10-23 (#33) an "(if applicable)" row is in the table with no `service`, so it drives no
  reminder; it is not put in `allWheelDriveOnly`, as the guide prints no drive mark on it. So the rear
  differential oil inspections (30,000, 60,000 and so on) and the replacement under towing (60,000 and
  120,000) are table-only, and a front-drive car keeps them in its table with "(if applicable)" in the name.
- **Coolant rows are split** (the owner's ruling of 2026-10-23, #33). The "(if applicable)" mark belongs to
  the intercooler: every RAV4 Hybrid has an engine coolant and an inverter coolant.
  - "Engine Coolant/Intercooler Coolant (if applicable)", replaced at 100,000 and 150,000, is two table jobs:
    "Replace engine coolant" (`coolant`) and "Replace intercooler coolant (if applicable)" (table only). The
    footnote gives the engine coolant's first change at 100,000 miles or 120 months, then every 50,000 or
    60, so it is a reminder first at 100,000 then every 50,000 miles / 60 months.
  - "Inverter Coolant/Intercooler Coolant (if applicable)", replaced at 150,000 (the footnote: first at
    150,000 or 180 months, then every 50,000 or 60), is "Replace inverter coolant" (`inverter-coolant`) with
    the same intercooler row (one intercooler row per milestone). It is a reminder first at 150,000 then
    every 50,000 miles / 60 months.
- **Other "(if applicable)" row, table-only, no reminder:** "Radiator/Condenser/Intercooler (if
  applicable)", an inspection.
- **Hybrid battery cooling intake filter:** inspected every 5,000 and cleaned every 20,000. The clean is a
  reminder (`hybrid-battery-filter`); the inspections are in the table only.
- **Reminders:** engine oil 10,000 (5,000 / 6 mo under special conditions), tire rotation 5,000, cabin and
  engine air filters 30,000, spark plugs 120,000 (one cell, at 120,000), hybrid battery cooling intake
  filter clean 20,000, engine coolant first at 100,000 then every 50,000 / 60, inverter coolant first at
  150,000 then every 50,000 / 60.
- **Special operating conditions** (dusty roads; towing, a car-top carrier or heavy loads; long idling or
  low-speed driving; repeated short trips below freezing) are in the table, each job named with its
  condition: oil every 5,000; ball joints, drive shaft boots, engine air filter and steering linkage
  inspected every 5,000 on dusty roads; chassis and body nuts and bolts torqued every 5,000 on dusty roads
  or when towing; rear differential oil (see above). Where a special page only repeats a cell of the
  standard chart (the engine air filter replaced at 30,000, oil at 10,000, the inspections at 15,000), the
  job is listed once.
- **Brake linings:** a footnote has them looked at every 5,000 and measured for thickness and runout every
  30,000; the 30,000 milestones say so, as on the 4Runner list.
- **"Add EFI tank additive (island use only)"**, every 5,000: in the table with no `service`. It has no
  catalog line and applies only where the guide says.
- **Inspections** are in the table only, with a check line where one exists (fluid levels, wiper blades,
  brakes). The owner's ruling of 2026-10-23 (#33) makes a differential oil inspection a reminder, but this
  chart's is "(if applicable)" and stays table-only (above).
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Left out: the explanations of each item and the general notes.
- **Not covered:** 2027 (no guide at `T-MMS-27RAV4HV`, 403 on 2026-10-23); the 2026 petrol RAV4 doesn't
  exist (`T-MMS-26RAV4` is 403 too). 2025 and earlier are an earlier generation, with guides of their own,
  and aren't part of this list.
