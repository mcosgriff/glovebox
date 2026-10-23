# Toyota Corolla Cross Hybrid, 2024

How `app/src/main/assets/known-schedules/toyota-corolla-cross-2024-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2024 Corolla Cross Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-24CorollaCrossHV/pdf/T-MMS-24CorollaCrossHV.pdf`.
Read on 2026-10-23. (Toyota's owners site lists no guide for the hybrid; this address is the issue's.)

- The guide's maintenance log (pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- **One year only.** The 2023 hybrid guide differs at one milestone: it has no engine oil change under
  dusty-road driving at 45,000, where 2024 has it, as at every other odd 5,000. It is a list of its own
  ([toyota-corolla-cross-2023-hybrid](toyota-corolla-cross-2023-hybrid.md)).
- **Its own guide, not the gas Corolla Cross's.** The hybrid's log has no drive belts, no propeller
  shaft, no transfer case and no brake booster vacuum pump; it re-torques a drive shaft nut; it inspects
  the rear differential oil every 30,000 and replaces it only when towing; it inspects the
  "engine/inverter coolant". It names no engine and marks no line for a drivetrain. NHTSA gives both as
  "Corolla Cross" (`GetModelsForMakeYear` for Toyota, 2024, checked 2026-10-23: no separate hybrid
  model), and both are 2.0L, so this list has `engine.powertrain` `hybrid` and the gas lists have `gas`;
  a 2.0L Corolla Cross whose powertrain isn't on record is asked which it is.
- **No drivetrain marks.** The guide prints no "(AWD)" or "(4WD)" on any line, so no line here is held
  back for a front-wheel-drive car; nothing is inferred about which drivetrains the hybrid was sold with.
- **The hybrid battery's cooling filter.** The log inspects the HV battery cooling intake filter at every
  milestone and cleans it at every 20,000 (where the clean takes the inspection's place), for every
  owner. The clean is a reminder every 20,000 / 24, logged with the hybrid battery cooling filter line.
  The inspection has no line and is in the table only.
- **Engine and inverter coolant at 100,000.** The 100,000 box prints "Replace engine/inverter coolant",
  and the guide's footnote to that line settles it: the engine coolant is first replaced at
  100,000 miles / 120 months, the inverter coolant first at 150,000 miles / 180 months, and both every
  50,000 / 60 after that. So the 100,000 job is the engine coolant's, and that is how this list names
  it. The inverter coolant's first change is given only in that footnote and falls past the end of the
  log (120,000), so it has no place in the table and no reminder. The inverter coolant line is not used
  by this list.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (printed at every milestone for Hawaii only), the note that the oil is to be replaced at
  5,000 miles if the recommended oil wasn't used at the last change, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval on
  dusty roads), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, the hybrid battery's
  cooling filter 20,000 / 24, spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and
  then every 50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts
  from the day the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): automatic transmission fluid and rear differential oil every 60,000 / 72
  when towing or carrying heavy loads.
- The inspections are in the table only. The drive shaft nut re-torque has no service line and is in the
  table only.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2025 on, whose guides have a chart in place of the log
  ([2025](toyota-corolla-cross-2025-hybrid.md), [2026](toyota-corolla-cross-2026-hybrid.md)). There is no
  2022 hybrid.
