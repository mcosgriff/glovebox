# Toyota Corolla Hybrid, 2021–2022

How `app/src/main/assets/known-schedules/toyota-corolla-2021-2022-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the Corolla Hybrid for each model year, which Toyota
publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>CorollaHV/pdf/T-MMS-<yy>CorollaHV.pdf`
(`21` and `22`). Read on 2026-10-23. (Toyota's owners site lists no guide for the hybrid; these addresses
answer.)

- The 2021 guide's maintenance log (pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2022 was compared with it by a script that reads each milestone's words, and by a script that names each
  job it finds at each milestone and holds them to this list. They are the same.
- **Not the 2020 list.** From 2021 the hybrid battery's cooling filter is inspected at every milestone and
  cleaned every 20,000 for everyone, where 2020 cleaned it only under long idling
  ([toyota-corolla-2020-hybrid](toyota-corolla-2020-hybrid.md)). **Not the 2023 list** either: from 2023
  the log has all-wheel-drive lines ([toyota-corolla-2023-2024-hybrid](toyota-corolla-2023-2024-hybrid.md)).
- **Hybrid, not gas.** The Corolla sedan of these years also has a 1.8L engine, and NHTSA gives both as
  "Corolla" (`GetModelsForMakeYear` for Toyota, 2021 and 2022, checked 2026-10-23: no "Corolla Hybrid").
  So this list has `engine.powertrain` `hybrid` and the 1.8L gas list has `gas`; a 1.8L Corolla whose
  powertrain isn't on record is asked which it is. The log names no engine and has no line for a
  drivetrain.
- **The hybrid battery's cooling filter.** The log inspects it at every milestone except each 20,000, where
  it cleans it instead; its footnote says the same. The clean is a reminder every 20,000 / 24, logged with
  the hybrid battery cooling filter line; the inspection is in the table only.
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
  filters 30,000 / 36, the hybrid battery's cooling filter 20,000 / 24, spark plugs 120,000 / 144, and
  engine coolant first at 100,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the
  first coolant change isn't used: it counts from the day the vehicle went into service, which Glovebox
  doesn't know.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or carrying heavy
  loads.
- The inspections are in the table only, with the check line where Glovebox has one (fluid levels, wiper
  blades, brakes, engine and inverter coolant).
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2020, and 2023 on, which are lists of their own.
