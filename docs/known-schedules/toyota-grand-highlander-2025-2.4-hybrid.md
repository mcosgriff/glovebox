# Toyota Grand Highlander Hybrid MAX, 2025

How `app/src/main/assets/known-schedules/toyota-grand-highlander-2025-2.4-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2025 Grand Highlander Hybrid Warranty and Maintenance Guide,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-25GrandHighlanderHV/pdf/T-MMS-25GrandHighlanderHV.pdf`.
Toyota's owners site lists no warranty guide for the hybrid; the guide was found only at that address.
Read on 2026-10-23.

- From 2025 the guide has a maintenance chart in place of the log: jobs are rows, mileages are columns from
  5,000 to 150,000 miles (PDF pages 40 to 45, printed 38 to 43: three recommended-service charts, each
  followed by a chart for special operating conditions). It was read in full, cell by cell, as page images,
  and the list written from it: 30 milestones, 5,000 miles apart.
- **The chart prints no months on its columns**, so the milestones have none. A reminder keeps months only
  where the guide prints them for that job: the oil change (12 months, or 5,000 miles / 6 months on dirt or
  dusty roads, in its footnote) and the two coolants (60 months for every change after the first, in their
  footnotes; below). The other reminders are by distance only.
- **One chart, split by engine** (the owner's ruling of 2026-10-23, #33). The guide prints one chart for
  both hybrids: the 2.5L hybrid and the Hybrid MAX (2.4L turbo, T24A-FTS). Every row that is only the
  turbo's says so, and every other row names no engine and so is for both. This list has every row: the
  shared ones and the turbo's own, which are
  - spark plugs at 40,000 and 80,000 ("T24A-FTS"; the change at 120,000 is unmarked, so the turbo's plugs
    are every 40,000);
  - drive belts, inspected at 60,000 and every 15,000 after ("T24A-FTS engine only");
  - the vacuum pump at 120,000 ("T24A-FTS engine only"; its footnote says to replace the vane and vane caps;
    named "Inspect vacuum pump (new vane and caps)" as the hybrid's guide names no brake booster, in the
    table only, since it happens once
    and has no catalog line);
  - the automatic transmission fluid change with long idling or low-speed driving (footnote "T24A-FTS
    engine only"), beside the change when towing, which is for both.
  The 2.5L hybrid has a list of its own
  ([toyota-grand-highlander-2025-2.5-hybrid](toyota-grand-highlander-2025-2.5-hybrid.md)).
- **Engine and powertrain.** The gas Grand Highlander has a 2.4L turbo too, in a guide of its own, so this
  list has `engine.powertrain` `hybrid` and the gas list has `gas`. A 2.4L Grand Highlander whose powertrain
  isn't on record is asked which it is. NHTSA's model list names it "Grand Highlander" only (checked for
  2024, 2025 and 2026 on 2026-10-23).
- **"(if applicable)" rows** (the owner's ruling of 2026-10-23) are in the table with no `service`, so they
  never drive a reminder: the radiator, condenser and intercooler, the rear differential oil (inspected
  every 30,000, and replaced at 60,000 and 120,000 when towing) and the intercooler coolant. Nothing is
  inferred from the 2024 log's "(4WD)": these rows keep the chart's "(if applicable)" and are not in
  `allWheelDriveOnly`.
- **Coolant rows split** (the owner's ruling of 2026-10-23, #33). The chart's rows are "Engine Coolant /
  Intercooler Coolant (if applicable)" (replaced at 100,000 and 150,000; footnote: first at 100,000 miles /
  120 months, then every 50,000 / 60) and "Inverter Coolant / Intercooler Coolant (if applicable)" (replaced
  at 150,000; footnote: first at 150,000 miles / 180 months, then every 50,000 / 60). The "(if applicable)"
  mark belongs to the intercooler: every one of these hybrids has engine coolant and an inverter loop. So
  each row is split into "Replace engine coolant" (`coolant`) or "Replace inverter coolant"
  (`inverter-coolant`) and "Replace intercooler coolant (if applicable)" (table only, once per milestone).
  The engine coolant is a reminder first at 100,000 miles and the inverter coolant first at 150,000, each
  then every 50,000 miles / 60 months, as the footnotes give them. The footnotes' "120 months" and "180
  months" for the first changes count from the day the vehicle went into service, which Glovebox doesn't
  know, so the first changes are by mileage only.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected at every milestone and cleaned every
  20,000 miles; the cleaning is a reminder (`hybrid-battery-filter`), the inspection is in the table only.
- Left out: "Add EFI Tank Additive" at every milestone (island use only: a job for one region, which the
  format can't name), and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the chart's dusty-roads oil change),
  tire rotation 5,000, hybrid battery cooling filter cleaning 20,000, cabin and engine air filters 30,000,
  spark plugs 40,000, engine coolant first at 100,000 miles and inverter coolant first at 150,000, each then
  every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or with long idling or
  low-speed driving (police, taxi, delivery). The rear differential oil beside it is "(if applicable)" and
  has no reminder.
- Special operating conditions, as this chart draws them (only the work the recommended chart doesn't
  already do there): on dirt or dusty roads, the ball joints, drive shaft boots and steering linkage are
  inspected at the milestones that aren't 15,000s, the engine air filter at every milestone but the
  30,000s, and the oil is changed at the odd 5,000s; nuts and bolts are tightened at every milestone on
  dusty roads or when towing.
- Inspections are in the table only, with a check line where the catalog has one (fluid levels, wiper
  blades, brakes, drive belts). The brake inspection is visual at every 5,000 miles, with thickness and
  runout at the 30,000s (footnote). The chart has no coolant or cabin air filter inspection, unlike the 2024
  log. Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: 2024 (a log, with a list of its own:
  [toyota-grand-highlander-2024-2.4-hybrid](toyota-grand-highlander-2024-2.4-hybrid.md)); 2026 (a list of its
  own, [toyota-grand-highlander-2026-2.4-hybrid](toyota-grand-highlander-2026-2.4-hybrid.md), made once the
  owner ruled on 2026-10-25 that its "(if applicable)" drive belt and vacuum pump rows go in both hybrid
  lists); 2027 on (no maintenance guide found on 2026-10-22).
