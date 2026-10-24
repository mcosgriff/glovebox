# Toyota Highlander 3.5L V6, 2018–2019

How `app/src/main/assets/known-schedules/toyota-highlander-2018-2019-3.5.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>Highlander/pdf/T-MMS-<yy>Highlander.pdf`
(`18` and `19`). Read on 2026-10-23.

- The 2018 guide's maintenance log (PDF pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full, as images, and the list written from it.
- 2019 was compared page by page with a script, and the one difference looked at on the page: the 75,000
  page says "Radiator and condense" where 2018 says "condenser". The jobs and where they fall are the same.
- **One log, split by engine** (the owner's ruling of 2026-10-23). The gasoline guide covers the 2.7L
  four-cylinder (1AR-FE) and the 3.5L V6 (2GR-FKS) in one log. Three lines name an engine, and each says
  it is the V6's: spark plugs at 60,000 "(2GR-FKS engine only)", the brake booster's vacuum pump at
  120,000 "(2GR-FKS engine only)", and the EFI tank additive "(2GR-FKS engine AND Hawaii or Puerto Rico
  only)" at every milestone. No line names the 2.7L, and every other line names no engine, so it is the
  V6's as much as the 2.7L's. This list is the whole log with the V6's lines, except the EFI tank
  additive (below). The 2.7L has a list of its own with the same lines less the V6's.
- **Gas, not hybrid.** The Highlander Hybrid is a 3.5L too, with a guide of its own and a list of its own
  ([2018–2019 Highlander Hybrid](toyota-highlander-2018-2019-hybrid.md)), so this list is marked
  `"powertrain": "gas"`. A Highlander whose powertrain isn't on record is asked which engine it has.
- Left out: the EFI tank additive, which is for Hawaii and Puerto Rico only; Glovebox has no way to say a
  job is for one region, and everyone else with the V6 would be shown it. Also left out, because they
  aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles if 0W-20 wasn't used
  at the last change, and the explanations of each item.
- **"(AWD)"**: the guide marks the jobs of its all-wheel-drive model "(4WD)" (the propeller shaft bolt,
  rear differential oil and transfer case oil). Toyota sells the Highlander as AWD, not 4WD, so these are
  named "(AWD)" here and kept in `allWheelDriveOnly` (the owner's ruling). A Highlander known to be
  front-wheel drive doesn't get them.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the guide's interval under any of
  its four special operating conditions), tire rotation 5,000 / 6, cabin air filter 10,000 / 12, engine
  air filter 30,000 / 36, spark plugs 60,000 / 72 (the log has them at 60,000 for the V6 and at 120,000
  for every engine), and engine coolant first at 100,000 miles and then every 50,000 / 60. The guide's
  "or 120 months" for the first coolant change isn't used: it counts from the day the vehicle went into
  service, which Glovebox doesn't know.
- Only for severe use: rear differential oil (AWD) and transfer case oil (AWD) every 15,000 / 18 when
  towing or carrying heavy loads; automatic transmission fluid every 60,000 / 72 when towing or under
  extensive idling. Under ordinary driving the guide only inspects the differential and transfer case.
- Inspections that are reminders too (the owner's ruling of 2026-10-23, #33: any vehicle with a differential
  needs it inspected, as asked for the 4Runner on 2026-10-13): rear differential oil (AWD) and transfer case
  oil (AWD) every 15,000 / 18. Logging a change of one counts as its check.
- Table only, with no reminder: the brake booster's vacuum pump at 120,000 (a one-off replacement with no
  service line of its own; `brake-booster` is a different part), the propeller shaft bolt re-torque
  (AWD; no service line fits, as on the 4Runner), every other inspection, and drive belts, which are
  inspected first at 60,000 and then every 15,000.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered here: 2016 and 2017 have lists of their own (their logs differ), and 2014–2015 guides were
  found but not read.
