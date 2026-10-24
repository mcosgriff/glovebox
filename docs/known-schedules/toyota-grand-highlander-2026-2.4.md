# Toyota Grand Highlander 2.4L, 2026

How `app/src/main/assets/known-schedules/toyota-grand-highlander-2026-2.4.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2026 Grand Highlander Warranty and Maintenance Guide,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-26GrandHighlander/pdf/T-MMS-26GrandHighlander.pdf`
(listed by Toyota's owners site for the gas model). Read on 2026-10-23.

- The guide has a maintenance chart: jobs are rows, mileages are columns from 5,000 to 150,000 miles (PDF
  pages 39 to 44, printed 38 to 43: three recommended-service charts, each followed by a chart for special
  operating conditions). It was read in full, cell by cell, as page images, and the list written from it:
  30 milestones, 5,000 miles apart.
- Compared with 2025 ([2025](toyota-grand-highlander-2025-2.4.md)): the recommended-service charts agree,
  row by row and cell by cell; what differs there is wording ("Radiator/Condenser/Intercooler") and the
  footnote numbers. **The special-condition charts don't agree.** 2026 marks the dusty-road work in every
  column: the ball joints, drive shaft boots and steering linkage are inspected and the oil changed at
  every 5,000 miles, the engine air filter is inspected at every milestone and replaced at the 30,000s, and
  the propeller shaft bolts (if applicable) are re-torqued at every milestone. 2025 lists only the work the
  recommended chart doesn't already do. So 2025 and 2026 are separate lists, each as its own chart draws it.
- **The chart prints no months on its columns**, so the milestones have none. A reminder keeps months only
  where the guide prints them for that job: the oil change (12 months, or 5,000 miles / 6 months on dirt or
  dusty roads, in its footnote) and the engine coolant (60 months for every change after the first, in
  its footnote; below). The other reminders are by distance only.
- **Engine and powertrain.** The gas model has one engine (the 2.4L turbo, T24A-FTS). The Hybrid MAX has a
  2.4L turbo too, in a guide of its own, so this list has `engine.powertrain` `gas`. NHTSA's model list
  names it "Grand Highlander" only (checked for 2024, 2025 and 2026 on 2026-10-23).
- **"(if applicable)" rows** (the owner's ruling of 2026-10-23, #33) are in the table with no `service`, so
  they never drive a reminder: the propeller shaft bolts, the radiator, condenser and intercooler, the rear
  differential oil and the transfer case oil (ordinary and when towing), and the intercooler coolant.
  Nothing is inferred from the 2024 log's "(4WD)": these rows keep the chart's "(if applicable)" and are not
  in `allWheelDriveOnly`. That leaves no rear differential or transfer case reminder in this list.
- **Coolant row split** (the owner's ruling of 2026-10-23, #33). The chart's row is "Engine
  Coolant/Intercooler Coolant (if applicable)", replaced at 100,000 and 150,000 (footnote: first at 100,000
  miles / 120 months, then every 50,000 / 60). The "(if applicable)" mark belongs to the intercooler: every
  Grand Highlander has engine coolant. So the row is two table jobs, "Replace engine coolant" (`coolant`) and
  "Replace intercooler coolant (if applicable)" (table only), and the engine coolant is a reminder, first at
  100,000 miles and then every 50,000 miles / 60 months, as the footnote gives it. The footnote's "120
  months" for the first change counts from the day the vehicle went into service, which Glovebox doesn't
  know, so the first change is by mileage only.
- The brake booster's vacuum pump is inspected at 120,000; its footnote says to replace the pump's vane and
  vane caps. It is in the table only, named "Inspect brake booster vacuum pump (new vane and caps)".
- Left out: "Add EFI Tank Additive" at every milestone (Hawaii only: a job for one region, which the format
  can't name), and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the chart's dusty-roads oil change),
  tire rotation 5,000, cabin and engine air filters 30,000, spark plugs 40,000, and engine coolant first at
  100,000 miles and then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or with long idling or
  low-speed driving (police, taxi, delivery). The dusty-roads engine air filter replacement at the 30,000s
  falls with the ordinary one and has no reminder of its own.
- Special operating conditions otherwise as in 2025: nuts and bolts are tightened at every milestone on
  dusty roads or when towing; when towing, the rear differential and transfer case oil (if applicable) are
  replaced every 15,000. The propeller shaft bolts' condition is named "dust, road salt or towing" to fit
  the 80-character limit.
- Inspections are in the table only, with a check line where the catalog has one (fluid levels, wiper
  blades, brakes, drive belts). The brake inspection is visual at every 5,000 miles, with thickness and
  runout at the 30,000s (footnote). Brake fluid has no replacement in the chart, so it has no reminder
  from this list.
- Not covered here: 2024 and 2025 (lists of their own), 2027 on (Toyota's owners site lists only a warranty
  guide with no schedule for 2027, and the maintenance guide's address answered 403 on 2026-10-22).
