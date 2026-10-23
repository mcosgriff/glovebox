# Toyota 4Runner, 2025–2026, 2.4L turbo

How `app/src/main/assets/known-schedules/toyota-4runner-2025-2026-gas.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). The hybrid has a guide and a list of its own:
[toyota-4runner-2025-2026-hybrid.md](toyota-4runner-2025-2026-hybrid.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>4Runner/pdf/T-MMS-<yy>4Runner.pdf`
(`25` and `26`). Read on 2026-10-23.

- The 2025 guide's maintenance chart (pages 39 to 44 of the file: three standard pages and three
  "special operating conditions" pages, every 5,000 miles from 5,000 to 150,000) was read in full and
  the list written from it.
- The 2026 guide was compared with it. The standard chart has the same jobs in the same cells. The
  differences:
  - The fuel injector cleaner row lost its footnote, and the footnotes are numbered from 1 again.
  - The special-conditions pages mark every column a job is due in, so they repeat cells the standard
    chart already has (oil at every 5,000, the engine air filter replaced at 30,000, the transmission
    fluid inspected at 30,000, 90,000 and 150,000). The 2025 pages mark only the columns the standard
    chart doesn't. Taken together with the standard chart, the jobs and mileages are the same in both
    years. The list is laid out as the 2025 pages are, like the 2017–2024 list: a special-conditions
    job isn't repeated where the standard chart already does it.
- **Engine:** these years have two powertrains of one size, the 2.4L turbo and the 2.4L turbo hybrid,
  and Toyota prints a guide for each. This list is `"powertrain": "gas"`; a vehicle gets it only when
  the owner has said it is petrol, or picks "2.4L turbo 4-cylinder" when setup asks which engine.
- **Months:** the chart's columns carry none, so the milestones have no months. The footnotes print
  months for two jobs: the oil (every 10,000 miles or 12 months; 5,000 or 6 on dirt or dusty roads),
  which is the only reminder with months, and the coolant, which has no reminder (below).
- **"(if applicable)":** the guide marks the drive shaft boots, the front differential oil, the
  propeller shaft boots, the transfer case oil, the radiator, condenser and intercooler, and the engine
  and intercooler coolant "(if applicable)". Under the owner's ruling of 2026-10-23 (#33) these rows
  are in the table, named with the mark, with no `service`, so none of them is a reminder. That
  leaves without a reminder:
  - the coolant change (first at 100,000 miles, then every 50,000), because the guide prints the
    engine and intercooler coolant as one row marked "(if applicable)";
  - the front differential oil change every 15,000 and the transfer case oil change every 30,000
    under special conditions.
  No job in this guide is marked "(4WD)" (only the off-road daily checks, which aren't milestone jobs,
  say "4WD models"), so `fourWheelDriveOnly` is empty and no job is left out for a two-wheel-drive
  truck.
- Reminders: engine oil 10,000 / 12 mo (5,000 / 6 on dirt or dusty roads), tire rotation 5,000, cabin
  and engine air filters 30,000, spark plugs 40,000.
- Only for severe use (#136): rear differential oil every 15,000 when towing or carrying heavy loads,
  and automatic transmission fluid every 60,000 when towing or idling for long periods. Long idling
  or low-speed driving (police, taxi or delivery use) is a condition the 2017–2024 guides didn't give.
- Inspections are table-only, as rule 7 says; the 2017–2024 list's inspection reminders were asked for
  that list alone. Where Glovebox has a check line, the row has it as its `service` (fluid levels,
  wiper blades, drive belts, rear differential oil), so a logged check counts against it.
- The brakes are inspected every 5,000 and measured (thickness and runout) every 30,000, as the
  footnote says; at those milestones the row is named so.
- The vacuum pump for the brake booster is marked "inspect" at 120,000, and its footnote says to
  replace the pump's vane and vane caps. It is named as the footnote says and is in the table only,
  with no `service`: it happens once in the chart, and Glovebox's brake booster line is the booster
  itself.
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
- Not covered: 2027 on (no guide at that address on 2026-10-23: `T-MMS-274Runner` answers 403). The
  2017–2024 trucks have their own list ([toyota-4runner-2017-2024.md](toyota-4runner-2017-2024.md)).
