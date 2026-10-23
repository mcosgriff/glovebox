# Toyota Tundra, 2025–2026 (hybrid)

How `app/src/main/assets/known-schedules/toyota-tundra-2025-2026-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the Tundra hybrid (i-FORCE MAX, 3.4L), which Toyota
publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>TundraHV/pdf/T-MMS-<yy>TundraHV.pdf`
(`25` and `26`). Toyota's owners-site index lists only the gas guides; the hybrid guides were found at their
own addresses. Read on 2026-10-23.

- The 2025 guide's maintenance chart (pages 40 to 45) was read in full and the list written from it. The chart
  has jobs as rows and mileages as columns, 5,000 to 150,000 in steps of 5,000, over three pages, each followed
  by a page of special operating conditions. Its marks are clean, inspect, perform, replace and torque.
- The 2026 guide was compared with it by script, mark by mark: the marks are the same. What differs is wording
  ("HEV battery", "Radiator/Condenser/Intercooler") and footnote numbers.
- **The chart prints no months for a column**, so the milestones have none. A reminder has months only where
  the guide prints them for that job: the oil (10,000 miles or 12 months, and 5,000 or 6 months on dusty roads
  or short trips in freezing weather, from its footnote), and the two coolants (60 months for every change
  after the first, from their footnotes; below).
- **Gas and hybrid.** The gas Tundra (twin-turbo V6) is also 3.4L and has a guide of its own, so this list is
  marked `"powertrain": "hybrid"` and the [gas list](toyota-tundra-2025-2026-gas.md) `gas`. vPIC lists the
  model as "Tundra" for both.
- What the hybrid chart has that the gas one doesn't, and the reverse:
  - The HV battery cooling intake filter: one row, cleaned at 20,000, 40,000 and so on and inspected at every
    other column. Cleaning it is a reminder, every 20,000 (`hybrid-battery-filter`); the inspection is in the
    table only.
  - "Inverter coolant / intercooler coolant (if applicable)", replaced at 150,000 (split, below).
  - No brake booster vacuum pump inspection (the gas chart has one at 120,000).
- **"(if applicable)" rows** are in the table with no `service` and no reminder (the owner's ruling of
  2026-10-23, #33). The chart drops the log's "(4WD)" and marks these rows "(if applicable)" instead, so they
  aren't marked for four-wheel drive and every truck gets them: drive shaft boots, front differential oil,
  transfer case oil, "radiator, condenser and intercooler", and the intercooler coolant. **This leaves
  without a reminder:** the intercooler coolant change, and, under special conditions, front differential
  oil (every 15,000 when towing) and transfer case oil (every 30,000 on dusty roads or when towing). Their
  inspections have no `-check` service either. The 2022–2024 logs, which mark the drivetrain jobs "(4WD)",
  are not used to fill this in.
- **The coolant rows.** "Engine coolant / intercooler coolant (if applicable)" is replaced at 100,000 and
  150,000 (first at 100,000 then every 50,000 in its footnote), "inverter coolant / intercooler coolant (if
  applicable)" at 150,000 (first at 150,000 then every 50,000 in its footnote). Each is split into two table
  jobs (the owner's ruling of 2026-10-23, #33): "Replace engine coolant" (`coolant`) or "Replace inverter
  coolant" (`inverter-coolant`), and "Replace intercooler coolant (if applicable)", with no `service`, once
  per milestone. The "(if applicable)" mark belongs to the intercooler: every one of these trucks has engine
  coolant and an inverter loop. Engine coolant is a reminder first at 100,000 and then every 50,000 miles /
  60 months, inverter coolant first at 150,000 and then every 50,000 / 60, the months their footnotes print
  after the first change. The first changes' months count from the day the truck went into service, which
  Glovebox doesn't know, so the first changes are by mileage only.
- Inspections are in the table, with a `-check` service where Glovebox has one and the row isn't "(if
  applicable)" (fluid levels, wiper blades, brakes, drive belts, rear differential oil). Only the rear
  differential oil is a reminder too, every 15,000; logging a change counts as its check. This is the
  owner's ruling of 2026-10-23 (#33): any vehicle with a differential needs it inspected, so the 4Runner's
  rule applies to every list whose guide prints the inspection.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 under special operating conditions), tire rotation
  5,000, cabin and engine air filters 30,000, spark plugs 40,000 (at 40,000, 80,000 and 120,000 in the chart),
  the HV battery cooling filter clean 20,000, engine coolant first at 100,000 and then every 50,000 / 60,
  and inverter coolant first at 150,000 and then every 50,000 / 60.
- Only for severe use (#136): rear differential oil every 30,000 and automatic transmission fluid every 60,000
  when towing or carrying heavy loads.
- In the table only: the propeller shaft bolt re-torque (every 15,000, or every 5,000 on dusty or salted roads
  or when towing) and tightening the chassis and body nuts and bolts under special conditions.
- The chart's brake row is one row, inspected every 5,000; its footnote adds a thickness and runout check
  every 30,000, which isn't a row of its own, so the list has one brake inspection line.
- Left out: "Add EFI Tank Additive (Hawaii only)", at every column, which is for one region; the off-road daily
  checks; and the explanations of each item.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2027 (Toyota's index lists only its general warranty guide for gas, hybrid and plug-in
  vehicles, which has no maintenance schedule); 2022–2024 have logs of their own
  ([2022](toyota-tundra-2022-hybrid.md), [2023](toyota-tundra-2023-hybrid.md),
  [2024](toyota-tundra-2024-hybrid.md)).
