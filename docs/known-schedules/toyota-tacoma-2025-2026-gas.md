# Toyota Tacoma, 2025–2026 (gas)

How `app/src/main/assets/known-schedules/toyota-tacoma-2025-2026-gas.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>Tacoma/pdf/T-MMS-<yy>Tacoma.pdf` (`25` and
`26`), the addresses Toyota's owners site gives for them
(`https://www.toyota.com/service/tcom/cs/manuals/downloads/tacoma/<year>`). Read on 2026-10-23.

- These guides give the schedule as a chart: jobs as rows, mileages as columns every 5,000 miles from 5,000
  to 150,000, with a "Recommended Service" chart and a "Special Operating Conditions" chart for each run of
  ten columns. The 2025 guide's charts (pages 38 to 43) were read in full, cell by cell, and the list written
  from them: 30 milestones, to 150,000 miles.
- The 2026 guide's charts were compared with a script and every difference looked at on the page. The
  Recommended Service charts are the same, cell for cell. The Special Operating Conditions charts differ in
  layout only: 2025 marks a special cell only where the recommended chart doesn't already do that job, and
  2026 marks every column, repeating the recommended cells (the automatic transmission fluid inspection at
  30,000, the engine air filter's replacement at every 30,000, the oil at every 10,000 and so on). Put
  together with the recommended chart, the two come out the same. The list follows 2025's layout, so no job
  is twice at a milestone. Otherwise only the footnote numbers differ, and 2026 drops the footnote on the
  fuel additive.
- **No months are printed for any column**, so the milestones have none. A reminder has months only where
  the guide prints them for that job: engine oil 12 months (5,000 miles or 6 months under the special
  conditions), and the engine coolant 60 months for every change after the first (below). Every other
  reminder is by distance alone.
- **The Tacoma has a gas and a hybrid version of one size**, a 2.4L turbo four-cylinder and the i-FORCE MAX
  hybrid built on it, and Toyota prints a guide for each. This list is for the gas engine
  (`"powertrain": "gas"`, label "2.4L turbo gas"); the hybrid has
  [its own list](toyota-tacoma-2025-2026-hybrid.md). vPIC names both "Tacoma" for 2025 and 2026 (checked
  2026-10-23).
- **"(If applicable)" rows** (the owner's ruling of 2026-10-23, #33): these charts mark the drive shaft boots,
  the front differential oil, the transfer case oil, the radiator row's intercooler, the intercooler
  coolant, the automatic transmission fluid and its cooler hoses, the manual transmission oil and
  the clutch fork "(if applicable)", where the 2024 log marked the drivetrain lines "(4WD)". They are in the
  table, named so, with no `service`, so they drive no reminder. The drivetrain rows are not in
  `fourWheelDriveOnly`, because the chart doesn't say four-wheel drive.
- **The coolant row**: the chart prints one row, "Engine Coolant/Intercooler Coolant (if applicable)",
  replaced at 100,000 and 150,000, and its footnote gives the first change at 100,000 miles or 120 months,
  then every 50,000 / 60. It is split into two table jobs (the owner's ruling of 2026-10-23, #33): "Replace
  engine coolant" (`coolant`) and "Replace intercooler coolant (if applicable)", with no `service`. The
  "(if applicable)" mark belongs to the intercooler: every Tacoma has engine coolant. Engine coolant is a
  reminder first at 100,000 and then every 50,000 miles / 60 months. The footnote's "120 months" for the
  first change counts from the day the truck went into service, which Glovebox doesn't know, so the first
  change is by mileage only.
- What the "(if applicable)" rows leave with no reminder, under any use: the intercooler coolant,
  the front differential oil and transfer case oil changes when towing or on dusty roads, the automatic
  transmission fluid change when towing or under long idling, and the manual transmission oil change when
  towing.
- **Gearbox:** the gas Tacoma is sold with a manual or an automatic. The automatic transmission fluid rows
  (its inspection, its cooler hoses and its change) are in `manualExcluded`; the manual transmission oil rows
  (its inspection and its change) and the clutch fork's lubrication are in `manualOnly`. They keep "(if
  applicable)" in their names.
- **The brake booster's vacuum pump** is inspected at 120,000 miles, and the footnote says to replace its
  vane and vane caps. It is in the table only, as one job, with no catalog line (a one-off part).
- The brakes are one row at every column; the footnote says to inspect visually every 5,000 miles and
  measure thickness and disc runout every 30,000. The row is named for the inspection.
- Left out: the fuel-tank additive (a fuel injector cleaner) the chart lists at every column for Hawaii only (a job for one region,
  which the format can't name), the note on the cabin air filter in heavy traffic or dust, the daily checks
  for off-road driving, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 on dirt or dusty roads or repeated short trips below
  freezing), tire rotation 5,000, cabin and engine air filters 30,000, spark plugs 40,000, and engine
  coolant first at 100,000 and then every 50,000 / 60.
- Inspection that is a reminder too: rear differential oil every 15,000. Logging a change counts as its
  check. This is the owner's ruling of 2026-10-23 (#33): any vehicle with a differential needs it
  inspected, so the 4Runner's rule applies to every list whose guide prints the inspection. The front
  differential and transfer case inspections are "(if applicable)" and stay in the table only. The other
  inspections are in the table only, with a check line where there is one: fluid levels, wiper blades,
  brakes and drive belts (first at 60,000, then every 15,000).
- Only for severe use (#136): rear differential oil every 15,000 when towing or carrying heavy loads.
- In the table only: re-torquing the propeller shaft bolts (every 15,000, and at the other columns on dusty
  or salted roads or when towing), tightening the chassis and body nuts and bolts, and the clutch fork
  (on dusty roads, or within a day of driving through water).
- Against 2024: the chart runs to 150,000; it has no cabin air filter inspection, no coolant inspections and
  no check of the transmission for leaks, and it inspects the automatic transmission fluid and the manual
  transmission oil every 30,000; the transmission changes when towing are a line for each gearbox (the 2024
  log's towing line at 60,000 and 120,000 named "fluid or oil"), with the automatic's under long idling on
  the same line; the oil's special condition adds short trips below freezing.
- The chart has no brake fluid replacement, so it has no reminder from this list.
- Not covered: 2027 on (no guide found on 2026-10-22: Toyota's owners site gives only its general 2027
  warranty guide, and `T-MMS-27Tacoma` is refused at both addresses), 2024 (a log in place of the chart:
  [the 2024 list](toyota-tacoma-2024-gas.md)).
