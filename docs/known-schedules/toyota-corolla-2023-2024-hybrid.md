# Toyota Corolla Hybrid, 2023–2024

How `app/src/main/assets/known-schedules/toyota-corolla-2023-2024-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the Corolla Hybrid for each model year, which Toyota
publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>CorollaHV/pdf/T-MMS-<yy>CorollaHV.pdf`
(`23` and `24`). Read on 2026-10-23. (Toyota's owners site lists no guide for the hybrid; these addresses
answer.)

- The 2023 guide's maintenance log (pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2024 was compared with it by a script that reads each milestone's words, and by a script that names each
  job it finds at each milestone and holds them to this list. They are the same; the 2024 oil note says
  "recommended oil" where 2023 says "0W-16".
- **Not the 2021–2022 list**: from 2023 the log has all-wheel-drive lines
  ([toyota-corolla-2021-2022-hybrid](toyota-corolla-2021-2022-hybrid.md)).
- **1.8L, no powertrain needed.** NHTSA gives the hybrid and the gas cars all as "Corolla"
  (`GetModelsForMakeYear` for Toyota, 2023 and 2024, checked 2026-10-23: no "Corolla Hybrid"), but the gas
  cars of these years have the 2.0L only, so the engine size tells them apart: this list has `engine` 1.8L
  and no `powertrain`. The log names no engine.
- **All-wheel-drive lines.** The log marks some lines "(AWD)", and Toyota sells the Corolla Hybrid with
  all-wheel drive; they are in this list as printed, in `allWheelDriveOnly`, so a Corolla on record as
  front-wheel drive doesn't get them: re-torque the drive shaft nut every 15,000 (and at every other
  milestone on dusty roads), inspect the rear differential oil every 30,000, and replace it at 60,000 and
  120,000 when towing or carrying heavy loads.
- **The drive shaft nut re-torque** has no service line in Glovebox, so it is in the table only, with no
  reminder, as the propeller shaft bolt is on the 4Runner.
- **The hybrid battery's cooling filter.** The log inspects it at every milestone except each 20,000, where
  it cleans it instead; its footnote says the same. The clean is a reminder every 20,000 / 24, logged with
  the hybrid battery cooling filter line; the inspection is in the table only.
- **Engine and inverter coolant at 100,000.** The 100,000 box prints "Replace engine/inverter coolant", and
  the guide's footnote settles it: the engine coolant is first replaced at 100,000 miles / 120 months, the
  inverter coolant first at 150,000 miles / 180 months, and both every 50,000 / 60 after that. So the
  100,000 job is the engine coolant's, and that is how this list names it. The inverter coolant's first
  change is given only in that footnote and falls past the end of the log, so it has no place in the
  table. Under the owner's ruling of 2026-10-23 (#33) it is an `additional` item, never on the table:
  "Replace inverter coolant" (`inverter-coolant`), first at 150,000 miles and then every 50,000 / 60. The
  "180 months" isn't used, for the same reason as the coolant's 120 (below).
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles
  if the recommended oil wasn't used at the last change, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any of its four special operating conditions), tire rotation 5,000 / 6, cabin and engine air
  filters 30,000 / 36, the hybrid battery's cooling filter 20,000 / 24, spark plugs 120,000 / 144,
  engine coolant first at 100,000 miles and then every 50,000 / 60, the rear differential oil inspection
  (AWD, in `allWheelDriveOnly`) every 30,000 / 36 (`rear-diff-check`), and (`additional`) inverter
  coolant first at 150,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the
  first coolant change isn't used: it counts from the day the vehicle went into service, which Glovebox
  doesn't know.
- Only for severe use (#136): automatic transmission fluid and rear differential oil (AWD) every
  60,000 / 72 when towing or carrying heavy loads.
- **The rear differential inspection is a reminder**, at the table's own 30,000 / 36. Under the owner's
  ruling of 2026-10-23 (#33), any vehicle with a differential needs it inspected, so every list whose
  guide prints the inspection makes it one; logging a change counts as its check. The other inspections
  are in the table only, with the check line where Glovebox has one (fluid levels, wiper blades, brakes,
  engine and inverter coolant, rear differential oil).
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2020–2022 and 2025 on are lists of their own.
