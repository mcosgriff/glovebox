# Toyota Sequoia, 2025–2026

How `app/src/main/assets/known-schedules/toyota-sequoia-2025-2026.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>SequoiaHV/pdf/T-MMS-<yy>SequoiaHV.pdf`
(`25` and `26`). Read on 2026-10-23. Toyota's owners site lists no warranty guide for these years; the
guides were found only at that address (#204).

- These guides give the schedule as a chart: jobs as rows, mileages as columns every 5,000 miles from
  5,000 to 150,000, with a "Recommended Service" chart and a "Special Operating Conditions" chart for each
  run of ten columns. The 2025 guide's charts (pages 38 to 43) were read in full, cell by cell, and the list
  written from them: 30 milestones, to 150,000 miles.
- The 2026 guide's charts were compared page by page, and every cell is the same. What differs is wording
  ("HEV battery" for "HV battery", "Radiator/Condenser/Intercooler"), the footnote numbers, and 2026 dropping
  the footnote on the fuel additive.
- **No months are printed for any column**, so the milestones have none. A reminder has months only where
  the guide prints them for that job: engine oil 12 months (5,000 miles or 6 months on dusty roads). Every other reminder is by distance alone.
- The Sequoia of these years is sold only as a hybrid, with one engine (the i-FORCE MAX), so the list
  names no engine or powertrain. The gearbox is automatic only.
- **"(If applicable)" rows** (the owner's ruling of 2026-10-23, #33): drive shaft boots, front differential
  oil, transfer case oil, the radiator row's intercooler and the intercooler coolant are marked "(If
  applicable)" in these charts, not "(4WD)" as in 2023–2024. They are in the table, named so, with no
  `service`, so they drive no reminder; they are not in `fourWheelDriveOnly`, because the guide doesn't say
  four-wheel drive. That leaves no front differential or transfer case reminder in this list, under any
  use.
- **The coolant rows.** The chart prints "Engine Coolant / Intercooler Coolant (if applicable)" at 100,000
  and 150,000, and "Inverter Coolant / Intercooler Coolant (if applicable)" at 150,000. Each is one row
  marked "(if applicable)", so each is in the table as printed, with no `service`, and **this list has no
  engine or inverter coolant reminder**. The mark reads as the intercooler's (the footnotes print
  "engine/intercooler coolant (if applicable)"), but rows are kept whole so every chart list reads the same
  way until the owner decides whether to split them.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected every 5,000 miles and cleaned every
  20,000 (`hybrid-battery-filter`).
- Left out: the fuel-tank additive the chart lists at every column "for island use only" (a job for one
  region, which the format can't name), the daily checks for off-road driving, and the explanations of
  each item. Coolant inspection is in the footnote to the fluid-levels row, not a row of its own, so it
  isn't a line here.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 on dirt or dusty roads), tire rotation 5,000,
  cleaning the hybrid battery cooling intake filter 20,000, cabin and engine air filters 30,000, spark plugs
  40,000.
- Inspections are in the table only (no reminders), with a check line where there is one: fluid levels,
  wiper blades, drive belts and rear differential oil.
- Only for severe use (#136): rear differential oil every 30,000 and automatic transmission fluid every
  60,000 when towing or carrying heavy loads.
- In the table only: re-torquing the propeller shaft bolts (every 15,000, and at the other milestones on
  dusty or salted roads or when towing) and tightening the chassis and body nuts and bolts.
- Against 2023–2024: the chart has no cabin air filter inspection at 15,000-mile points and no coolant
  inspection rows, and it inspects the automatic transmission fluid where the log inspects the
  transmission for leaks. The jobs and their intervals are otherwise the same.
- Brake fluid has no replacement in the chart at all, so it has no reminder from this list.
- Not covered: 2027 on (no guide found at that address on 2026-10-22), 2023–2024 (a log in place of the
  chart: [the 2023–2024 list](toyota-sequoia-2023-2024.md)).
