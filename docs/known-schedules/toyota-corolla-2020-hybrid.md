# Toyota Corolla Hybrid, 2020

How `app/src/main/assets/known-schedules/toyota-corolla-2020-hybrid.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2020 Corolla Hybrid,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-20CorollaHV/pdf/T-MMS-20CorollaHV.pdf`.
Read on 2026-10-23. (Toyota's owners site lists no guide for the hybrid; this address answers.)

- The guide's maintenance log (pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to 120,000
  miles) was read in full and the list written from it. A script that names each job it finds at each
  milestone was held to the list as well.
- **One year only.** From 2021 the guide inspects the hybrid battery's cooling filter at every milestone and
  cleans it every 20,000 for everyone; 2021–2022 is a list of its own
  ([toyota-corolla-2021-2022-hybrid](toyota-corolla-2021-2022-hybrid.md)).
- **Its own guide, not the Corolla's.** The hybrid's log has no drive belts, no brake booster vacuum pump and
  no EFI additive; it inspects the "engine/inverter coolant" and the "radiator, condenser and/or
  intercooler"; and its transmission fluid is replaced (at 60,000 and 120,000) only when towing or carrying
  heavy loads. It names no engine and has no line for a drivetrain.
- **Hybrid, not gas.** The 2020 Corolla sedan also has a 1.8L engine, and NHTSA gives both as "Corolla"
  (`GetModelsForMakeYear` for Toyota, 2020, checked 2026-10-23: no "Corolla Hybrid"). So this list has
  `engine.powertrain` `hybrid` and the 1.8L gas list has `gas`; a 1.8L Corolla whose powertrain isn't on
  record is asked which it is.
- **The hybrid battery's cooling filter.** This year prints "clean the HV battery cooling intake filter" at
  every milestone, and only under extensive idling or low-speed driving. So it is a job for severe use only:
  in the table at every milestone, named with its condition, and a reminder every 5,000 / 6 only while "How
  do you drive it?" is on Severe. It is logged with the hybrid battery cooling filter line.
- **Engine and inverter coolant at 100,000.** The 100,000 box prints "Replace engine/inverter coolant", and
  the guide's footnote settles it: the engine coolant is first replaced at 100,000 miles / 120 months, the
  inverter coolant first at 150,000 miles / 180 months, and both every 50,000 / 60 after that. So the
  100,000 job is the engine coolant's, and that is how this list names it. The inverter coolant's first
  change is given only in that footnote and falls past the end of the log, so it has no place in the table
  and no reminder (the coordinator's rule for footnote-only intervals).
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles
  if 0W-16 wasn't used at the last change, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any of its four special operating conditions), tire rotation 5,000 / 6, cabin and engine air
  filters 30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts from the day
  the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): the hybrid battery's cooling filter every 5,000 / 6 for long idling or
  low-speed driving, and automatic transmission fluid every 60,000 / 72 when towing or carrying heavy loads.
- The inspections are in the table only, with the check line where Glovebox has one (fluid levels, wiper
  blades, brakes, engine and inverter coolant).
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2021 on, which are lists of their own. No hybrid guide before 2020 was looked for; the
  issue's survey found the hybrid guides from 2020.
