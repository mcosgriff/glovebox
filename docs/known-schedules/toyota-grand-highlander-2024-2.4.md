# Toyota Grand Highlander 2.4L, 2024

How `app/src/main/assets/known-schedules/toyota-grand-highlander-2024-2.4.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2024 Grand Highlander Warranty and Maintenance Guide,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-24GrandHighlander/pdf/T-MMS-24GrandHighlander.pdf`
(listed by Toyota's owners site for the gas model). Read on 2026-10-23.

- The guide's maintenance log (PDF pages 39 to 54, printed 38 to 53: 24 milestones, 5,000 miles or 6 months
  apart, to 120,000 miles) was read in full, as page images, and the list written from it.
- It is the only model year with a log. 2025 and 2026 have a chart in place of it, with lists of their own
  ([2025](toyota-grand-highlander-2025-2.4.md), [2026](toyota-grand-highlander-2026-2.4.md)).
- **Engine and powertrain.** This guide is for the gas model only, with one engine (the 2.4L turbo,
  T24A-FTS). The Hybrid MAX has a 2.4L turbo too, in a guide of its own, so this list has
  `engine.powertrain` `gas` and the Hybrid MAX list has `hybrid`. A 2.4L Grand Highlander whose powertrain
  isn't on record is asked which it is. NHTSA's model list names it "Grand Highlander" only (checked for
  2024, 2025 and 2026 on 2026-10-23), so the list has no other spelling.
- **"(4WD)" printed, "(AWD)" here** (the owner's ruling of 2026-10-23, #33). Toyota sells the Grand
  Highlander with front-wheel or all-wheel drive, and its guide marks the rear differential oil, the
  transfer case oil and the propeller shaft bolt "(4WD)". Those jobs are named "(AWD)" here and are in
  `allWheelDriveOnly`, so a Grand Highlander known to be front-wheel drive doesn't get them.
- Coolant: engine coolant and intercooler coolant are each replaced at 100,000 miles; the footnotes give
  each first at 100,000 miles / 120 months and then every 50,000 / 60. Both are reminders
  (`coolant`, `intercooler-coolant`). The first change's "or 120 months" isn't used: it counts from the day
  the vehicle went into service, which Glovebox doesn't know.
- The brake booster's vacuum pump is inspected at 120,000; its footnote says to replace the pump's vane and
  vane caps. It happens once in the log and there is no catalog line for it, so it is in the table only,
  named "Inspect brake booster vacuum pump (new vane and caps)".
- Left out: "Add Toyota EFI Tank Additive" at every milestone (Hawaii and Puerto Rico only: a job for one
  region, which the format can't name), the note that the oil is to be replaced if the recommended oil
  wasn't used at the last change, the note on the cabin air filter, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the log's dusty-roads oil change; the
  log has no oil change for towing), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36,
  spark plugs 40,000 / 48, rear differential oil (AWD) and transfer case oil (AWD) 30,000 / 36 (15,000 / 18
  when towing or carrying heavy loads), and engine and intercooler coolant first at 100,000 miles and then
  every 50,000 / 60.
- Inspections that are reminders too (the owner's ruling of 2026-10-23, #33: any vehicle with a differential
  needs it inspected, as asked for the 4Runner on 2026-10-13): rear differential oil (AWD) and transfer case
  oil (AWD), first at 15,000 miles and then every 30,000 / 36, as the log prints them. Logging a change of
  one counts as its check.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or with long
  idling or low-speed driving (police, taxi, delivery). Under ordinary driving the log only inspects the
  transmission.
- Special operating conditions, as the log prints them: on dirt or dusty roads, the ball joints, drive
  shaft boots and steering linkage are inspected at the milestones where the ordinary log doesn't, the
  engine air filter at every milestone but the 30,000s, and the oil is changed at the odd 5,000s; the
  propeller shaft bolt (AWD) is re-torqued at the milestones the ordinary log doesn't, on dusty or salted
  roads or when towing; nuts and bolts are tightened at every milestone on dusty roads or when towing.
- The other inspections are in the table only, with a check line where the catalog has one (fluid levels,
  wiper blades, brakes, drive belts, engine coolant). The brake
  inspection is visual at every 5,000 miles, with thickness and runout at the 30,000s. Drive belts are
  first inspected at 60,000, then every 15,000. Brake fluid has no replacement in the log, so it has no
  reminder from this list.
- Not covered: 2025 and 2026 (charts, with lists of their own), 2027 on (Toyota's owners site lists only a
  warranty guide with no schedule for 2027, and the maintenance guide's address answered 403 on
  2026-10-22).
