# Toyota 4Runner hybrid, 2025–2026

How `app/src/main/assets/known-schedules/toyota-4runner-2025-2026-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md). The 2.4L turbo without the hybrid system has a guide and
a list of its own: [toyota-4runner-2025-2026-gas.md](toyota-4runner-2025-2026-gas.md).

Source: Toyota's Warranty and Maintenance Guide for the hybrid (i-FORCE MAX) for each model year, which
Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>4RunnerHV/pdf/T-MMS-<yy>4RunnerHV.pdf`
(`25` and `26`). Read on 2026-10-23.

- The 2025 guide's maintenance chart (pages 40 to 45 of the file: three standard pages and three
  "special operating conditions" pages, every 5,000 miles from 5,000 to 150,000) was read in full and
  the list written from it.
- The 2026 guide was compared with it. The standard chart has the same jobs in the same cells. The
  differences:
  - Wording: "HV battery" is "HEV battery", and the radiator row says "Radiator/Condenser/Intercooler"
    for "Radiator, Condenser and/or Intercooler". The fuel injector cleaner row lost its footnote.
  - The special-conditions pages mark every column a job is due in, so they repeat cells the standard
    chart already has. The 2025 pages mark only the columns the standard chart doesn't. Taken together
    with the standard chart, the jobs and mileages are the same in both years, with the one exception
    below. The list is laid out as the 2025 pages are, like the 2017–2024 list: a special-conditions
    job isn't repeated where the standard chart already does it.
  - **One cell taken to be a misprint:** on the 2026 special-conditions page for 105,000 to 150,000,
    the propeller shaft boots row has "T" (torque) at 150,000, where every other cell of that row is
    "I" and the 2025 page has nothing. Boots aren't torqued, and the standard chart inspects them at
    150,000 in both years, which the list has. Nothing more is taken from that cell.
- **Engine:** these years have two powertrains of one size, the 2.4L turbo and the 2.4L turbo hybrid,
  and Toyota prints a guide for each. This list is `"powertrain": "hybrid"`; a vehicle gets it when
  vPIC's decode says it is a strong hybrid, when the owner has said so, or when the owner picks
  "2.4L turbo hybrid" when setup asks which engine.
- **Months:** the chart's columns carry none, so the milestones have no months. The footnotes print
  months for three jobs: the oil (every 10,000 miles or 12 months; 5,000 or 6 on dirt or dusty roads),
  which is the only reminder with months, and the two coolants, which have no reminder (below).
- **Drivetrain:** unlike the 2.4L turbo's guide, this one marks none of its rows "(if applicable)"
  except the radiator, condenser and intercooler and the two coolant rows, and none "(4WD)" (only the
  off-road daily checks, which aren't milestone jobs, say "4WD models"). So the drive shaft boots,
  front differential and transfer case are ordinary jobs here, `fourWheelDriveOnly` is empty, and no
  job is left out for any drivetrain.
- **"(if applicable)":** under the owner's ruling of 2026-10-23 (#33), the three rows so marked are in
  the table, named with the mark, with no `service`. That leaves without a reminder:
  - the engine and intercooler coolant change (first at 100,000 miles, then every 50,000), printed as
    one row;
  - the inverter and intercooler coolant change (first at 150,000 miles, then every 50,000), printed
    as one row. It falls at 150,000 only, the chart's last column; its interval is given only in a
    footnote, so it would get no reminder in any case (the coordinator's rule of 2026-10-23).
- Reminders: engine oil 10,000 / 12 mo (5,000 / 6 on dirt or dusty roads), tire rotation 5,000, cabin
  and engine air filters 30,000, spark plugs 40,000, and cleaning the hybrid battery's cooling intake
  filter every 20,000 (`hybrid-battery-filter`). The filter is inspected at every other 5,000; those
  rows are in the table only.
- Only for severe use (#136): front and rear differential oil every 15,000 when towing or carrying
  heavy loads, transfer case oil every 30,000 on dirt or dusty roads or when towing, and automatic
  transmission fluid every 60,000 when towing or idling for long periods. Long idling or low-speed
  driving (police, taxi or delivery use) is a condition the 2017–2024 guides didn't give.
- Inspections are table-only, as rule 7 says; the 2017–2024 list's inspection reminders were asked for
  that list alone. Where Glovebox has a check line, the row has it as its `service` (fluid levels,
  wiper blades, drive belts, front and rear differential oil, transfer case oil), so a logged check
  counts against it.
- The brakes are inspected every 5,000 and measured (thickness and runout) every 30,000, as the
  footnote says; at those milestones the row is named so.
- The vacuum pump is marked "inspect" at 120,000, and its footnote says to replace the pump's vane and
  vane caps. It is named as the footnote says and is in the table only, with no `service`: it happens
  once in the chart, and Glovebox's brake booster line is the booster itself.
- The fuel injector cleaner added at every 5,000 is for island use only (the guide doesn't say where).
  It is in the table only, named with that condition.
- The special operating conditions, as the guide gives them: dirt or dusty roads; towing, a car-top
  carrier or heavy loads; rough or muddy roads or melted snow; salted roads; long idling or low-speed
  driving. The propeller shaft boots' row is for the first four and is named "dust, mud, salt or
  towing" to fit.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Left out, because they aren't jobs at a milestone: the off-road daily checks, the explanations of
  each item and the footnotes' advice.
- The chart ends at 150,000 and nothing says it repeats, so the table doesn't.
- Not covered: 2027 on (no guide at that address on 2026-10-23: `T-MMS-274RunnerHV` answers 403).
