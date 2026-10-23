# Toyota Sienna, 2026

How `app/src/main/assets/known-schedules/toyota-sienna-2026.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2026 Sienna Warranty and Maintenance Guide,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-26Sienna/pdf/T-MMS-26Sienna.pdf`. Read on
2026-10-23.

- The guide has a maintenance chart: jobs are rows, mileages are columns from 5,000 to 150,000 miles (PDF
  pages 40 to 45, printed 38 to 43: three normal charts, each followed by a chart for special operating
  conditions). It was read in full, as page images, and the list written from it: 30 milestones, 5,000 miles
  apart.
- **The chart prints no months on its columns**, so the milestones have none. A reminder keeps months only
  where the guide prints them for that job: the oil change (12 months, or 5,000 miles / 6 months on dirt or
  dusty roads, in its footnote). The other reminders are by distance only.
- Compared with 2025 ([2025](toyota-sienna-2025.md)): the normal charts agree, row by row. The special-
  condition charts don't. 2026's repeats work in every column: on dirt or dusty roads the ball joints,
  drive shaft boots and steering linkage are inspected and the oil changed at every 5,000 miles, the engine
  air filter is inspected at every milestone and replaced at the 30,000s; when towing, the automatic
  transmission fluid and the rear differential oil (if applicable) are inspected at 30,000, 90,000 and
  150,000 and replaced at 60,000 and 120,000. 2025 lists only the extra work and has no towing inspections.
  So 2025 and 2026 are separate lists, each as its own chart draws it.
- Every Sienna of these years is a hybrid with one engine, so the list has no `engine` block and no
  powertrain. NHTSA's model list gives the name as "Sienna" only.
- "(if applicable)" rows (the owner's ruling of 2026-10-23) are in the table with no `service`, so they never
  drive a reminder: the radiator, condenser and intercooler inspection, the rear differential oil rows
  (normal and towing), and the two coolant rows. Nothing is inferred from the 2021–2024 guides' "(AWD)": these
  rows keep the chart's "(if applicable)" and are not in `allWheelDriveOnly`.
- **Coolant has no reminder from this list.** The chart's rows are "Engine Coolant / Intercooler Coolant
  (if applicable)" (replace at 100,000 and 150,000; footnote: first at 100,000 miles / 120 months, then
  every 50,000 / 60) and "Inverter Coolant / Intercooler Coolant (if applicable)" (replace at 150,000;
  footnote: first at 150,000 / 180 months, then every 50,000 / 60). Each is one row marked "(if applicable)",
  so each is kept whole, in the table only (the coordinator's rule of 2026-10-23), even though the mark seems
  to belong to the intercooler. That leaves the engine coolant and the inverter coolant without a reminder;
  the owner will decide later whether such rows are split.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected at every milestone and cleaned every
  20,000 miles; the cleaning is a reminder (`hybrid-battery-filter`), the inspection is in the table only.
- Left out: "Add EFI Tank Additive" at every milestone (island use only), the separate box for "Auto Maas"
  vehicles (replace the auxiliary battery every 12 months; fleet equipment, and not in the chart), and the
  explanations of each item.
- The brake inspection is visual at every 5,000 miles, with thickness and runout every 30,000 (footnote).
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the chart's dusty-roads oil change),
  tire rotation 5,000, hybrid battery cooling filter cleaning 20,000, cabin and engine air filters 30,000,
  spark plugs 120,000.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or carrying heavy loads.
  The dusty-roads engine air filter replacement at the 30,000s falls with the normal one and has no
  reminder of its own; the rear differential oil is "(if applicable)" and has none.
- Inspections are in the table only, with a check line where the catalog has one (fluid levels, wiper
  blades, brakes). Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: 2025 and earlier (lists of their own), 2027 (no guide found at Toyota's address on
  2026-10-22).
