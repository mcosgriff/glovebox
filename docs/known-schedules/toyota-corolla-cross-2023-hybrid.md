# Toyota Corolla Cross Hybrid, 2023

How `app/src/main/assets/known-schedules/toyota-corolla-cross-2023-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2023 Corolla Cross Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-23CorollaCrossHV/pdf/T-MMS-23CorollaCrossHV.pdf`.
Read on 2026-10-23. (Toyota's owners site lists no guide for the hybrid; this address is the issue's.)

- The guide's maintenance log (pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it. 2023 is the hybrid's first model year.
- **One year only.** At 45,000 this guide has no engine oil change under dusty-road driving, where the
  2024 guide has one, as at every other odd 5,000; the 2023 page was looked at and the line isn't there.
  It is kept as printed, so this year is a list of its own; the 2024 list
  ([toyota-corolla-cross-2024-hybrid](toyota-corolla-cross-2024-hybrid.md)) is otherwise the same. The
  severe-use oil reminder (5,000 / 6) is the same in both: the guide's interval on dusty roads at every
  other odd 5,000.
- Everything else is as on the 2024 list's page: its own guide, not the gas car's, with no drive belts,
  propeller shaft, transfer case or vacuum pump; `engine.powertrain` `hybrid` (NHTSA gives the model
  only as "Corolla Cross" for 2023, checked 2026-10-23 with `GetModelsForMakeYear`); no drivetrain marks
  on any line; the hybrid battery's cooling filter cleaned every 20,000 / 24 as a reminder; and the
  100,000 "engine/inverter coolant" taken as the engine coolant, by the guide's footnote, with the
  inverter coolant (first at 150,000, past the end of the log) given no reminder.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (printed at every milestone for Hawaii only), the note that the oil is to be replaced at
  5,000 miles if 0W-16 wasn't used at the last change, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000 / 6, cabin and
  engine air filters 30,000 / 36, the hybrid battery's cooling filter 20,000 / 24, spark plugs
  120,000 / 144, and engine coolant first at 100,000 miles and then every 50,000 / 60 (the guide's "or
  120 months" for the first change isn't used).
- Only for severe use (#136): automatic transmission fluid and rear differential oil every 60,000 / 72
  when towing or carrying heavy loads.
- The inspections and the drive shaft nut re-torque are in the table only. Brake fluid has no
  replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2024 on are lists of their own. There is no 2022 hybrid.
