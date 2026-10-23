# Toyota Tacoma, 2025–2026 (hybrid)

How `app/src/main/assets/known-schedules/toyota-tacoma-2025-2026-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>TacomaHV/pdf/T-MMS-<yy>TacomaHV.pdf` (`25`
and `26`). Read on 2026-10-23. Toyota's owners site lists no hybrid guide under the Tacoma; they were found
only at that address.

- These guides give the schedule as a chart: jobs as rows, mileages as columns every 5,000 miles from 5,000
  to 150,000, with a "Recommended Service" chart and a "Special Operating Conditions" chart for each run of
  ten columns. The 2025 guide's charts (pages 38 to 43) were read in full, cell by cell, and the list written
  from them: 30 milestones, to 150,000 miles.
- The 2026 guide's charts were compared with a script and every difference looked at on the page. The
  Recommended Service charts are the same, cell for cell. The Special Operating Conditions charts differ in
  layout only: 2025 marks a special cell only where the recommended chart doesn't already do that job, and
  2026 marks every column, repeating the recommended cells. Put together with the recommended chart, the two
  come out the same. The list follows 2025's layout, so no job is twice at a milestone.
- What else differs in 2026 is wording: "HEV battery" for "HV battery", "Hawaii only" for "Island use only"
  on the fuel additive, the footnote numbers, and no footnote on the fuel additive. **The 2026 oil footnote
  adds repeated short trips below freezing** to dirt or dusty roads as a reason for the 5,000-mile oil change;
  the 2025 footnote gives dusty roads only. The severe-use oil change is the same 5,000 miles or 6 months in
  both, and Glovebox has one switch for severe use, so the reminder is the same; the job's name follows 2025,
  the guide read in full ("dusty roads").
- **No months are printed for any column**, so the milestones have none. A reminder has months only where
  the guide prints them for that job: engine oil 12 months (5,000 miles or 6 months on dusty roads), and
  the two coolants 60 months for every change after the first (below). Every other reminder is by distance
  alone.
- **The Tacoma has a gas and a hybrid version of one size**, a 2.4L turbo four-cylinder and the i-FORCE MAX
  hybrid built on it, and Toyota prints a guide for each. This list is for the hybrid
  (`"powertrain": "hybrid"`, label "2.4L turbo hybrid"); the gas engine has
  [its own list](toyota-tacoma-2025-2026-gas.md). vPIC names both "Tacoma" for 2025 and 2026 (checked
  2026-10-23).
- The hybrid's chart marks none of its drivetrain rows, neither "(4WD)" nor "(if applicable)", so nothing is
  in `fourWheelDriveOnly` and no job is left out for any drivetrain. The gearbox is automatic only, so there
  are no manual-gearbox jobs.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected every 5,000 miles and cleaned every
  20,000 (`hybrid-battery-filter`).
- **The coolant rows**: the chart prints "Engine Coolant/Intercooler Coolant (if applicable)", replaced at
  100,000 and 150,000, and "Inverter Coolant/Intercooler Coolant (if applicable)", replaced at 150,000. The
  footnotes give the engine coolant's first change at 100,000 miles or 120 months and the inverter
  coolant's at 150,000 or 180 months, each then every 50,000 / 60. Each row is split into two table jobs
  (the owner's ruling of 2026-10-23, #33): "Replace engine coolant" (`coolant`) or "Replace inverter
  coolant" (`inverter-coolant`), and "Replace intercooler coolant (if applicable)", with no `service`, once
  per milestone. The "(if applicable)" mark belongs to the intercooler: every one of these trucks has
  engine coolant and an inverter loop. Engine coolant is a reminder first at 100,000 and then every 50,000
  miles / 60 months, inverter coolant first at 150,000 and then every 50,000 / 60. The first changes' 120
  and 180 months count from the day the truck went into service, which Glovebox doesn't know, so the first
  changes are by mileage only.
- The radiator row, "Radiator/Condenser/Intercooler (if applicable)", is in the table under that name with no
  `service`.
- **The vacuum pump** is inspected at 120,000 miles, and the footnote says to replace its vane and vane caps.
  It is in the table only, as one job, with no catalog line (a one-off part).
- The brakes are one row at every column; the footnote says to inspect visually every 5,000 miles and
  measure thickness and disc runout every 30,000. The row is named for the inspection.
- Left out: the fuel-tank additive (a fuel injector cleaner) the chart lists at every column for island use
  or Hawaii only (a job for one region, which the format can't name), the note on the cabin air filter in
  heavy traffic or dust, the daily checks for off-road driving, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 on dirt or dusty roads), tire rotation 5,000, cleaning
  the hybrid battery cooling intake filter 20,000, cabin and engine air filters 30,000, spark plugs 40,000,
  engine coolant first at 100,000 and then every 50,000 / 60, and inverter coolant first at 150,000 and
  then every 50,000 / 60.
- Inspections that are reminders too: front and rear differential oil every 15,000, transfer case oil every
  30,000, unmarked as the chart prints them. Logging a change of one counts as its check. This is the
  owner's ruling of 2026-10-23 (#33): any vehicle with a differential needs it inspected, so the 4Runner's
  rule applies to every list whose guide prints the inspection. The other inspections are in the table
  only, with a check line where there is one: fluid levels, wiper blades, brakes and drive belts (first at
  60,000, then every 15,000).
- Only for severe use (#136): front and rear differential oil every 15,000 when towing or carrying heavy
  loads; transfer case oil every 30,000 on dusty roads or when towing; automatic transmission fluid every
  60,000 when towing or under long idling or low-speed driving.
- In the table only: re-torquing the propeller shaft bolts (every 15,000, and at the other columns on dusty
  or salted roads or when towing) and tightening the chassis and body nuts and bolts.
- Against 2024: the chart runs to 150,000; it has no cabin air filter inspection and no coolant inspections,
  and it inspects the automatic transmission fluid where the log inspects the transmission for leaks. Its
  inverter coolant is on the table at 150,000, where 2024's is an `additional` item.
- The chart has no brake fluid replacement, so it has no reminder from this list.
- Not covered: 2027 on (no guide found on 2026-10-22: `T-MMS-27TacomaHV` is refused at both of Toyota's
  addresses), 2024 (a log in place of the chart: [the 2024 list](toyota-tacoma-2024-hybrid.md)).
