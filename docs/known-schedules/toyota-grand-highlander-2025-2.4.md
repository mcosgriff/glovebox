# Toyota Grand Highlander 2.4L, 2025

How `app/src/main/assets/known-schedules/toyota-grand-highlander-2025-2.4.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2025 Grand Highlander Warranty and Maintenance Guide,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-25GrandHighlander/pdf/T-MMS-25GrandHighlander.pdf`
(listed by Toyota's owners site for the gas model). Read on 2026-10-23.

- From 2025 the guide has a maintenance chart in place of the log: jobs are rows, mileages are columns from
  5,000 to 150,000 miles (PDF pages 39 to 44, printed 38 to 43: three recommended-service charts, each
  followed by a chart for special operating conditions). It was read in full, cell by cell, as page images,
  and the list written from it: 30 milestones, 5,000 miles apart.
- **The chart prints no months on its columns**, so the milestones have none. A reminder keeps months only
  where the guide prints them for that job: the oil change (12 months, or 5,000 miles / 6 months on dirt or
  dusty roads, in its footnote) and the engine coolant (60 months for every change after the first, in
  its footnote; below). The other reminders are by distance only.
- **Engine and powertrain.** The gas model has one engine (the 2.4L turbo, T24A-FTS). The Hybrid MAX has a
  2.4L turbo too, in a guide of its own, so this list has `engine.powertrain` `gas` and the Hybrid MAX list
  has `hybrid`. NHTSA's model list names it "Grand Highlander" only (checked for 2024, 2025 and 2026 on
  2026-10-23).
- **"(if applicable)" rows** (the owner's ruling of 2026-10-23, #33) are in the table with no `service`, so
  they never drive a reminder: the propeller shaft bolts, the radiator, condenser and intercooler, the rear
  differential oil and the transfer case oil (ordinary and when towing), and the intercooler coolant. The
  2024 log marks the drivetrain ones "(4WD)", but nothing is inferred from it: they keep the chart's "(if
  applicable)" and are not in `allWheelDriveOnly`, so every Grand Highlander gets them in its table. **That
  leaves no rear differential or transfer case reminder in this list**, where the 2024 list has both.
- **Coolant row split** (the owner's ruling of 2026-10-23, #33). The chart's row is "Engine Coolant /
  Intercooler Coolant (if applicable)", replaced at 100,000 and 150,000 (footnote: first at 100,000 miles /
  120 months, then every 50,000 / 60). The "(if applicable)" mark belongs to the intercooler: every Grand
  Highlander has engine coolant. So the row is two table jobs, "Replace engine coolant" (`coolant`) and
  "Replace intercooler coolant (if applicable)" (table only), and the engine coolant is a reminder, first at
  100,000 miles and then every 50,000 miles / 60 months, as the footnote gives it. The footnote's "120
  months" for the first change counts from the day the vehicle went into service, which Glovebox doesn't
  know, so the first change is by mileage only.
- The brake booster's vacuum pump is inspected at 120,000; its footnote says to replace the pump's vane and
  vane caps. It happens once and there is no catalog line for it, so it is in the table only, named "Inspect
  brake booster vacuum pump (new vane and caps)".
- Left out: "Add EFI Tank Additive" at every milestone (Hawaii only: a job for one region, which the format
  can't name), and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the chart's dusty-roads oil change),
  tire rotation 5,000, cabin and engine air filters 30,000, spark plugs 40,000, and engine coolant first at
  100,000 miles and then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or with long idling or
  low-speed driving (police, taxi, delivery). Under ordinary driving the chart only inspects it.
- Special operating conditions, as this chart draws them (only the work the recommended chart doesn't
  already do there): on dirt or dusty roads, the ball joints, drive shaft boots and steering linkage are
  inspected at the milestones that aren't 15,000s, the engine air filter at every milestone but the
  30,000s, and the oil is changed at the odd 5,000s; the propeller shaft bolts (if applicable) are
  re-torqued at the milestones that aren't 15,000s, on dusty or salted roads or when towing (named "dust,
  road salt or towing" to fit the 80-character limit); nuts and bolts are tightened at every milestone on
  dusty roads or when towing; when towing, the rear differential and transfer case oil (if applicable) are
  replaced every 15,000, the 30,000s repeated from the recommended chart.
- Inspections are in the table only, with a check line where the catalog has one (fluid levels, wiper
  blades, brakes, drive belts). The brake inspection is visual at every 5,000 miles, with thickness and
  runout at the 30,000s (footnote). Drive belts are first inspected at 60,000, then every 15,000. The chart
  has no coolant or cabin air filter inspection, unlike the 2024 log. Brake fluid has no replacement in the
  chart, so it has no reminder from this list.
- Against 2026 ([2026](toyota-grand-highlander-2026-2.4.md)): the recommended-service charts agree cell by
  cell; the special-condition charts don't (2026 marks the dusty-road work in every column), so the two
  years are separate lists, each as its own chart draws it.
- Not covered here: 2024 (a log, with a list of its own:
  [toyota-grand-highlander-2024-2.4](toyota-grand-highlander-2024-2.4.md)), 2026 (its own list), 2027 on
  (Toyota's owners site lists only a warranty guide with no schedule for 2027, and the maintenance guide's
  address answered 403 on 2026-10-22).
