# Toyota Highlander 2.4L turbo, 2025

How `app/src/main/assets/known-schedules/toyota-highlander-2025-turbo.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2025 Warranty and Maintenance Guide for the Highlander,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-25Highlander/pdf/T-MMS-25Highlander.pdf`,
which Toyota's owners site lists for the 2025 Highlander. Downloaded on 2026-10-22 and read on 2026-10-23.

- **A chart, not a log.** The guide gives the schedule as a chart (printed pages 38 to 43): jobs as rows,
  columns every 5,000 miles from 5,000 to 150,000, each cell I (inspect), P (perform), R (replace) or
  T (torque), with a "Special Operating Conditions" chart beside each standard one. All six pages were read
  in full, cell by cell, and the list written from them: 30 milestones. Nothing says the chart repeats after
  150,000, so the table doesn't.
- **No months on the milestones.** The columns are in miles only, so the milestones have none. A reminder
  has months only where the guide prints them for that job: engine oil 12 months (5,000 miles or 6 months
  on dirt or dusty roads or short trips below freezing), and the engine coolant 60 months for every change
  after the first (below).
- **No wiper blade inspection from 5,000 to 50,000.** The chart's first page (5,000 to 50,000) has no wiper
  blade row; the other two pages inspect them at every column. The table follows the chart: the wiper
  inspection starts at 55,000. The 2026 chart has the row on its first page too, which is one reason 2026
  is [a list of its own](toyota-highlander-2026-turbo.md).
- **The engine:** Toyota's 2025 Highlander owner's manual (`OM0E144U`, at
  `https://assets.sipb.toyota.com/publications/en/om-s/OM0E144U/pdf/OM0E144U.pdf`) gives one engine, the
  T24A-FTS 2.4-litre turbo four (2393 cm³): `displacementL` 2.4, "2.4L turbo 4-cylinder".
- **"(If applicable)" rows** (the owner's ruling of 2026-10-23, #33): the propeller shaft bolts, rear
  differential oil, transfer case oil and the radiator row's intercooler are marked "(if applicable)" in
  this chart, not "(4WD)" as in the 2023–2024 logs. They are in the table, named so, with no `service`, so
  they drive no reminder; they aren't in `allWheelDriveOnly`, because the chart doesn't name a drivetrain.
  That leaves no rear differential or transfer case reminder in this list, under any use.
- **The coolant row, split** (the owner's ruling of 2026-10-23, #33). The chart prints one row, "Engine
  Coolant / Intercooler Coolant (if applicable)", R at 100,000 and 150,000. The "(if applicable)" mark
  belongs to the intercooler: every Highlander has engine coolant. So the row is two table jobs, "Replace
  engine coolant" (`coolant`) and "Replace intercooler coolant (if applicable)" (table only), and the
  engine coolant is a reminder first at 100,000 miles and then every 50,000 miles / 60 months, as its
  footnote gives it (first at 100,000 miles / 120 months, then every 50,000 / 60). The first change's "120
  months" counts from the day the vehicle went into service, which Glovebox doesn't know, so the first
  change is by mileage only.
- **Spark plugs** are replaced at 40,000, 80,000 and 120,000: a reminder every 40,000.
- **Special operating conditions.** 2025's special charts mark only the cells the standard chart doesn't
  already have, and the table takes them as they are, each named with its condition: on dirt or dusty
  roads, ball joints, drive shaft boots, steering linkage and the engine air filter inspected and the oil
  replaced at the other milestones; the propeller shaft bolts re-torqued at the other milestones on dusty
  or salted roads or when towing; rear differential and transfer case oil replaced at the 15,000s between
  the 30,000s when towing; the chassis nuts and bolts tightened at every milestone on dusty roads or when
  towing; and the transmission fluid replaced at 60,000 and 120,000 when towing or idling long.
- In the table only: the EFI tank additive, which the chart lists at every column for Hawaii, Puerto Rico,
  Saipan and American Samoa only, named so; the propeller shaft bolts and the chassis nuts and bolts, which
  are torque jobs; and every inspection, including the brake booster vacuum pump at 120,000.
- Left out: the notes on oil selection and the cabin filter. The coolant inspection is in the footnote to
  the fluid-levels row, not a row of its own, so it isn't a line here.
- The brake linings row is I at every column; its footnote says visually every 5,000 and thickness and
  runout every 30,000. The table keeps the chart's one row.
- Reminders: engine oil every 10,000 miles / 12 months (5,000 / 6 for severe use), tire rotation every
  5,000, cabin and engine air filters every 30,000, spark plugs every 40,000, engine coolant first at
  100,000 and then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing, carrying heavy loads or
  idling long.
- Inspections are in the table only, with a check line where Glovebox has one (fluid levels, wiper blades,
  drive belts).
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2026 ([its own list](toyota-highlander-2026-turbo.md)), 2023–2024 (logs, lists of their
  own), 2027 (no guide found on 2026-10-22).
