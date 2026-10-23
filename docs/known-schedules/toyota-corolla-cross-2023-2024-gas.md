# Toyota Corolla Cross (gas), 2023–2024

How `app/src/main/assets/known-schedules/toyota-corolla-cross-2023-2024-gas.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>CorollaCross/pdf/T-MMS-<yy>CorollaCross.pdf`
(`23` and `24`; found through Toyota's owners site). Read on 2026-10-23.

- The 2023 guide's maintenance log (pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2024 was compared with 2023 by a script's count of the words of each milestone. They are the same; the
  only difference is the oil note ("recommended oil" where 2023 says "0W-16").
- **Gas, not hybrid.** The Corolla Cross Hybrid has a 2.0L engine too and a guide of its own, and NHTSA
  gives both as "Corolla Cross" (`GetModelsForMakeYear` for Toyota, 2023 and 2024, checked 2026-10-23:
  no separate hybrid model). So this list has `engine.powertrain` `gas` and the hybrid lists have
  `hybrid`; a 2.0L Corolla Cross whose powertrain isn't on record is asked which it is. Neither guide
  names an engine.
- **Not the 2022 list.** From 2023 the engine oil is replaced every 5,000 miles under dusty-road driving
  only (2022: under any of four conditions), the short-trip condition is gone, "radiator, condenser
  and/or intercooler" is inspected, and the 120,000 transfer case oil inspection is marked "(AWD)". 2022
  is a list of its own ([toyota-corolla-cross-2022](toyota-corolla-cross-2022.md)).
- **All-wheel-drive lines.** The log marks some lines "(AWD)"; they are in this list as printed, in
  `allWheelDriveOnly`, so a Corolla Cross on record as front-wheel drive doesn't get them: re-torque the
  propeller shaft bolt every 15,000 (and at every other milestone on dusty or salted roads or when
  towing), inspect the rear differential oil and the transfer case oil every 15,000, and replace both
  every 15,000 when towing.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (printed at every milestone for Hawaii only), the note that the oil is to be replaced at
  5,000 miles if the recommended oil wasn't used at the last change, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval on
  dusty roads), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark plugs
  120,000 / 144, and engine coolant first at 100,000 miles and then every 50,000 / 60. The guide's "or
  120 months" for the first coolant change isn't used: it counts from the day the vehicle went into
  service, which Glovebox doesn't know.
- Only for severe use (#136): rear differential oil and transfer case oil (AWD) every 15,000 / 18 when
  towing or carrying heavy loads, and automatic transmission fluid every 60,000 / 72 when towing or
  idling for long periods.
- The inspections, drive belts (first at 60,000, then every 15,000) and the brake booster's vacuum pump
  (120,000) included, are in the table only. The propeller shaft bolt re-torque has no service line and
  is in the table only, as on the 4Runner.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2025 on, whose guides have a chart in place of the log
  ([toyota-corolla-cross-2025-2026-gas](toyota-corolla-cross-2025-2026-gas.md)).
