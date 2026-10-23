# Toyota Tundra, 2025–2026 (gas)

How `app/src/main/assets/known-schedules/toyota-tundra-2025-2026-gas.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the gas Tundra (the 3.4L twin-turbo V6), which Toyota
publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>Tundra/pdf/T-MMS-<yy>Tundra.pdf`
(`25` and `26`). Read on 2026-10-23.

- The 2025 guide's maintenance chart (pages 39 to 44) was read in full and the list written from it. The chart
  has jobs as rows and mileages as columns, 5,000 to 150,000 in steps of 5,000, over three pages, each followed
  by a page of special operating conditions. Its marks are inspect, perform, replace and torque.
- The 2026 guide was compared with it by script, mark by mark, and its special-conditions pages looked at: the
  marks are the same. What differs is wording ("Radiator/Condenser/Intercooler"), footnote numbers, and the
  special-conditions oil row, which in 2026 cites only the oil footnote where 2025 cites it and the dusty-roads
  one; that footnote gives the same conditions in both years.
- **The chart prints no months for a column**, so the milestones have none. A reminder has months only where
  the guide prints them for that job: the oil (10,000 miles or 12 months, and 5,000 or 6 months on dusty roads
  or short trips in freezing weather, from its footnote).
- **Gas and hybrid.** Toyota prints a separate guide for the hybrid (i-FORCE MAX), which is also 3.4L, so this
  list is marked `"powertrain": "gas"` and the [hybrid](toyota-tundra-2025-2026-hybrid.md) has its own. vPIC
  lists the model as "Tundra" for both.
- **"(if applicable)" rows** are in the table with no `service` and no reminder (the owner's ruling of
  2026-10-23, #33). The chart drops the log's "(4WD)" and marks these rows "(if applicable)" instead, so they
  aren't marked for four-wheel drive and every truck gets them: drive shaft boots, front differential oil,
  transfer case oil, "radiator, condenser and intercooler", and the one row "engine coolant / intercooler
  coolant". **This leaves without a reminder:** the engine and intercooler coolant change (at 100,000 and
  150,000 in the chart, and first at 100,000 then every 50,000 in its footnote), and, under special conditions,
  front differential oil (every 15,000 when towing) and transfer case oil (every 30,000 on dusty roads or when
  towing). Their inspections have no `-check` service either. The 2022–2024 logs, which mark these jobs
  "(4WD)", are not used to fill this in.
- Inspections are in the table, with a `-check` service where Glovebox has one and the row isn't "(if
  applicable)" (fluid levels, wiper blades, brakes, drive belts, rear differential oil). None of them is a
  reminder.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 under special operating conditions), tire rotation
  5,000, cabin and engine air filters 30,000, and spark plugs 40,000 (at 40,000, 80,000 and 120,000 in the
  chart).
- Only for severe use (#136): rear differential oil every 30,000 and automatic transmission fluid every 60,000
  when towing or carrying heavy loads.
- In the table only: the propeller shaft bolt re-torque (every 15,000, or every 5,000 on dusty or salted roads
  or when towing), tightening the chassis and body nuts and bolts under special conditions, and the brake
  booster vacuum pump inspection at 120,000 (the guide's note says to replace its vane and caps; there is no
  Glovebox line for it).
- The chart's brake row is one row, inspected every 5,000; its footnote adds a thickness and runout check
  every 30,000, which isn't a row of its own, so the list has one brake inspection line.
- Left out: "Add EFI Tank Additive (Hawaii only)", at every column, which is for one region; the off-road daily
  checks; and the explanations of each item.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2027 (Toyota's index lists only its general warranty guide for gas, hybrid and plug-in
  vehicles, which has no maintenance schedule); 2022–2024 have logs of their own
  ([2022](toyota-tundra-2022-gas.md), [2023–2024](toyota-tundra-2023-2024-gas.md)).
