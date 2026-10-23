# Toyota Camry, 2025

How `app/src/main/assets/known-schedules/toyota-camry-2025.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2025 Camry Warranty and Maintenance Guide,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-25Camry/pdf/T-MMS-25Camry.pdf` (the printing
online has a back cover dated 6/26). Read on 2026-10-23.

- **A chart, not a log.** The guide gives the schedule as a chart (PDF pages 42 to 47, printed 40 to 45):
  jobs as rows, columns every 5,000 miles from 5,000 to 150,000, each cell C (clean), I (inspect),
  P (perform), R (replace) or T (torque), with a "Special Operating Conditions" chart after each standard
  one. All six pages were read in full, as page images, and the list written from them: 30 milestones,
  5,000 miles apart. Nothing says the chart repeats after 150,000, so the table doesn't.
- **The 2025 special-conditions charts mark only the cells the standard chart doesn't already have**: on
  dusty or muddy roads the ball joints, drive shaft boots and steering linkage are inspected and the drive
  shaft nut re-torqued at the columns that aren't 15,000s, the engine air filter inspected at those that
  aren't 30,000s, and the oil changed at the odd 5,000s. The 2026 and 2027 charts draw the same work in
  every column, so each year is its own list ([2026](toyota-camry-2026.md), [2027](toyota-camry-2027.md)).
- **No months on the milestones.** The chart's columns are in miles only, so the milestones have none. A
  reminder has months only where the guide prints them for that job: the oil change (12 months; 5,000 miles
  / 6 months under the conditions, in its footnote) and the two coolants (every 50,000 / 60 months after
  the first change, in theirs; below). The other reminders are by distance only.
- Every Camry of these years is a hybrid with one engine (a 2.5L), and this guide is the hybrid's, so the
  list has no `engine` block and no powertrain. The earlier Camry lists end at 2024, so none overlaps it.
  NHTSA's model list gives the name as "Camry" only (checked for 2025, 2026 and 2027 on 2026-10-23).
- **"(AWD only)".** The chart marks two rows for all-wheel drive: the drive shaft nut re-torque (every
  15,000, and at the other columns on dusty roads) and the rear differential oil replacement when towing
  (60,000 and 120,000). They are named "(AWD)" and are in `allWheelDriveOnly`, so a Camry known to be
  front-wheel drive doesn't get them. The 2025 standard chart has no rear differential inspection.
- **Rows marked "(if applicable)"** are in the table with no `service`, so they never drive a reminder
  (the owner's ruling of 2026-10-23, #33): the radiator, condenser and intercooler inspection, and the
  intercooler coolant.
- **The coolant rows are split.** The chart prints "Engine Coolant / Intercooler Coolant (if applicable)"
  (replace at 100,000 and 150,000; footnote: first at 100,000 miles / 120 months, then every 50,000 / 60)
  and "Inverter Coolant / Intercooler Coolant (if applicable)" (replace at 150,000; footnote: first at
  150,000 / 180 months, then every 50,000 / 60). The mark belongs to the intercooler: every one of these
  Camrys has engine coolant and an inverter loop. So, under the owner's ruling of 2026-10-23 (#33), each
  row is two jobs in the table: "Replace engine coolant" (`coolant`) or "Replace inverter coolant"
  (`inverter-coolant`), and "Replace intercooler coolant (if applicable)", in the table only, once per
  milestone. The engine coolant is a reminder first at 100,000 miles and the inverter coolant first at
  150,000, both then every 50,000 miles / 60 months, as the footnotes give them. The first changes' "120
  months" and "180 months" count from the day the car went into service, which Glovebox doesn't know, so
  the first changes are by mileage only.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected at every column and cleaned every
  20,000 miles (the C takes the I's place); the cleaning is a reminder (`hybrid-battery-filter`), the
  inspection is in the table only.
- The brake linings row is I at every column; the 30,000s cells carry a footnote that thickness and disc
  runout are measured too, so the table names those as their own job, as the earlier lists do.
- "Reset Scheduled Maintenance Reminder" (P at every column) is in the table, in Glovebox's words, with no
  reminder.
- Left out: "Add EFI Tank Additive (Hawaii only)", at every column, which is for one region; the notes on
  oil selection; and the explanations of each item.
- The dusty-roads condition is the guide's "dirt, dusty or muddy roads or roads with melted snow", named
  "dusty or muddy roads"; the oil change's adds short trips in freezing weather, and the nuts and bolts'
  adds towing. Towing is towing, a car-top carrier or heavy loading.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000, hybrid battery
  cooling filter cleaning 20,000, cabin air filter 10,000, engine air filter 30,000, engine coolant first
  at 100,000 and then every 50,000 / 60, inverter coolant first at 150,000 and then every 50,000 / 60,
  spark plugs 120,000.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or carrying heavy loads,
  and rear differential oil (AWD) every 60,000 under the same condition.
- Inspections are in the table only, with a check line where the catalog has one (fluid levels, wiper
  blades, brakes). The drive shaft nut re-torque and the chassis nuts and bolts are in the table only.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: 2024 and earlier (the previous generation, with lists of their own), 2026 and 2027
  (lists of their own), 2028 on (no guide looked for).
