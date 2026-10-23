# Toyota Sienna, 2025

How `app/src/main/assets/known-schedules/toyota-sienna-2025.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2025 Sienna Warranty and Maintenance Guide,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-25Sienna/pdf/T-MMS-25Sienna.pdf`. Read on
2026-10-23.

- From 2025 the guide has a maintenance chart in place of the log: jobs are rows, mileages are columns from
  5,000 to 150,000 miles (PDF pages 40 to 45, printed 38 to 43: three normal charts, each followed by a
  chart for special operating conditions). It was read in full, as page images, and the list written from
  it: 30 milestones, 5,000 miles apart.
- **The chart prints no months on its columns**, so the milestones have none. A reminder keeps months only
  where the guide prints them for that job: the oil change (12 months, or 5,000 miles / 6 months on dirt or
  dusty roads, in its footnote) and the two coolants (60 months for every change after the first, in their
  footnotes; below). The other reminders are by distance only.
- Every Sienna of these years is a hybrid with one engine, so the list has no `engine` block and no
  powertrain. NHTSA's model list gives the name as "Sienna" only (checked for 2021 and 2025 on 2026-10-23).
- "(if applicable)" rows (the owner's ruling of 2026-10-23) are in the table with no `service`, so they never
  drive a reminder: the radiator, condenser and intercooler inspection, the rear differential oil inspection
  and its replacement when towing, and the intercooler coolant. The chart doesn't say what "(if applicable)"
  hangs on. The 2021–2024 guides mark the rear differential jobs "(AWD)", but nothing is inferred from them:
  these rows keep the chart's "(if applicable)" and are not in `allWheelDriveOnly`, so every Sienna gets
  them in its table.
- **Coolant.** The chart's rows are "Engine Coolant / Intercooler Coolant (if applicable)" (replace at
  100,000 and 150,000; footnote: first at 100,000 miles / 120 months, then every 50,000 / 60) and "Inverter
  Coolant / Intercooler Coolant (if applicable)" (replace at 150,000; footnote: first at 150,000 / 180
  months, then every 50,000 / 60). Each is split into two table jobs (the owner's ruling of 2026-10-23, #33):
  "Replace engine coolant" (`coolant`) or "Replace inverter coolant" (`inverter-coolant`), and "Replace
  intercooler coolant (if applicable)", with no `service`, once per milestone. The "(if applicable)" mark
  belongs to the intercooler: every Sienna has engine coolant and, as a hybrid, an inverter loop. Engine
  coolant is a reminder first at 100,000 and then every 50,000 miles / 60 months, inverter coolant first at
  150,000 and then every 50,000 / 60. The footnotes' "120 months" and "180 months" for the first changes
  count from the day the vehicle went into service, which Glovebox doesn't know, so the first changes are
  by mileage only.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected at every milestone and cleaned every
  20,000 miles; the cleaning is a reminder (`hybrid-battery-filter`), the inspection is in the table only.
- Left out: "Add EFI Tank Additive" at every milestone (island use only), the separate box for "Auto Maas"
  vehicles (replace the auxiliary battery every 12 months; fleet equipment, and not in the chart), and the
  explanations of each item.
- The brake inspection is visual at every 5,000 miles, with thickness and runout every 30,000 (footnote);
  the table names the two as on the earlier lists.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the chart's dusty-roads oil change),
  tire rotation 5,000, hybrid battery cooling filter cleaning 20,000, cabin and engine air filters 30,000,
  spark plugs 120,000, engine coolant first at 100,000 and then every 50,000 / 60, and inverter coolant
  first at 150,000 and then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or carrying heavy loads.
  The rear differential oil beside it is "(if applicable)" and has no reminder.
- Special operating conditions, as the chart draws them: on dirt or dusty roads, the ball joints, drive shaft
  boots and steering linkage are inspected at the milestones where the normal chart doesn't, the engine air
  filter at every milestone but the 30,000s, and the oil is changed at the odd 5,000s; nuts and bolts are
  tightened at every milestone on dusty roads or when towing.
- Inspections are in the table only, with a check line where the catalog has one (fluid levels, wiper
  blades, brakes). The normal chart has no coolant or cabin air filter inspection, unlike the 2021–2024 logs.
  Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Compared with 2026: the normal charts agree; the special-condition charts don't (see
  [2026](toyota-sienna-2026.md)), so the two years are separate lists.
- Not covered here: 2024 and earlier (a log, with lists of their own), 2026 (its own list), 2027 (no guide
  found at Toyota's address on 2026-10-22).
