# Toyota Grand Highlander Hybrid, 2024

How `app/src/main/assets/known-schedules/toyota-grand-highlander-2024-2.5-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2024 Grand Highlander Hybrid Warranty and Maintenance Guide,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-24GrandHighlanderHV/pdf/T-MMS-24GrandHighlanderHV.pdf`.
Toyota's owners site lists no warranty guide for the hybrid; the guide was found only at that address.
Read on 2026-10-23.

- The guide's maintenance log (PDF pages 40 to 55, printed 38 to 53: 24 milestones, 5,000 miles or 6 months
  apart, to 120,000 miles) was read in full, as page images, and the list written from it.
- **One log, split by engine** (the owner's ruling of 2026-10-23, #33). The guide prints one log for both
  hybrids: the 2.5L hybrid and the Hybrid MAX (2.4L turbo, T24A-FTS). Every line that is only the turbo's
  says so ("T24A-FTS only" or "T24A-FTS engine only"), and every other line names no engine and so is for
  both. The lines left out of this list as the turbo's:
  - replace spark plugs at 40,000 and 80,000;
  - inspect the drive belt (first at 60,000, then every 15,000);
  - inspect the intercooler coolant (every 15,000);
  - inspect the vacuum pump at 120,000;
  - replace the automatic transmission fluid with long idling or low-speed driving (60,000 and 120,000).
  The spark plug change at 120,000 is unmarked, so it is here, which makes this engine's plugs every
  120,000. Every line's engine is printed plainly, so the split could be made. The Hybrid MAX has a list of
  its own ([toyota-grand-highlander-2024-2.4-hybrid](toyota-grand-highlander-2024-2.4-hybrid.md)).
- **Engine and powertrain.** No other Grand Highlander has a 2.5L engine, so the size alone tells it apart;
  the list has `engine.powertrain` `hybrid` as well, to say so plainly. NHTSA's model list names it "Grand
  Highlander" only (checked for 2024, 2025 and 2026 on 2026-10-23); the hybrid has no model name of its own.
- The radiator line reads "Radiator, condenser and/or intercooler" for both engines; it is kept as one line,
  "Inspect radiator, condenser and intercooler", although this engine has no intercooler.
- **"(4WD)" printed, "(AWD)" here** (the owner's ruling of 2026-10-23). Toyota sells the Grand Highlander
  with front-wheel or all-wheel drive, and the log marks the rear differential oil (inspected every 30,000,
  replaced at 60,000 and 120,000 when towing) "(4WD)". Those jobs are named "(AWD)" here and are in
  `allWheelDriveOnly`, so a Grand Highlander known to be front-wheel drive doesn't get them. The hybrid has
  no transfer case or propeller shaft lines.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected at every milestone and cleaned every
  20,000 miles (where the cleaning takes the inspection's place). The cleaning is a reminder
  (`hybrid-battery-filter`); the inspection is in the table only.
- **Engine and inverter coolant at 100,000.** The 100,000 box prints "Replace engine/inverter coolant", and
  the footnote settles it: the engine coolant is first replaced at 100,000 miles / 120 months, the inverter
  coolant first at 150,000 miles / 180 months, and both every 50,000 / 60 after that. So the 100,000 job is
  the engine coolant's, named "Replace engine coolant" (`coolant`). **The inverter coolant has no
  reminder**: its first change is past the end of the log and given only in that footnote.
- Left out: "Add Toyota EFI Tank Additive" at every milestone (Hawaii and Puerto Rico only: a job for one
  region, which the format can't name), the note that the oil is to be replaced if the recommended oil
  wasn't used at the last change, the note on the cabin air filter, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the log's dusty-roads oil change),
  tire rotation 5,000 / 6, hybrid battery cooling filter cleaning 20,000 / 24, cabin and engine air filters
  30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The first change's "or 120 months" isn't used (it counts from the day the vehicle went into
  service, which Glovebox doesn't know).
- Only for severe use (#136): automatic transmission fluid and rear differential oil (AWD) every
  60,000 / 72 when towing or carrying heavy loads.
- Special operating conditions, as the log prints them: on dirt or dusty roads, the ball joints, drive
  shaft boots and steering linkage are inspected at the milestones where the ordinary log doesn't, the
  engine air filter at every milestone but the 30,000s, and the oil is changed at the odd 5,000s; nuts and
  bolts are tightened at every milestone on dusty roads or when towing.
- Inspections are in the table only, with a check line where the catalog has one (fluid levels, wiper
  blades, brakes, engine and inverter coolant, rear differential oil). The brake inspection is visual at
  every 5,000 miles, with thickness and runout at the 30,000s. Brake fluid has no replacement in the log,
  so it has no reminder from this list.
- Not covered: 2025 (a chart in place of the log, with a list of its own:
  [toyota-grand-highlander-2025-2.5-hybrid](toyota-grand-highlander-2025-2.5-hybrid.md)); 2026 (its chart
  marks the drive belt and vacuum pump rows "(if applicable)" where 2025 marked them the turbo's, so not
  every line's engine is printed and, by the owner's ruling, the guide isn't split); 2027 on (no maintenance
  guide found on 2026-10-22).
