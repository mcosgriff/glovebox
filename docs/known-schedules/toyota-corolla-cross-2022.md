# Toyota Corolla Cross, 2022

How `app/src/main/assets/known-schedules/toyota-corolla-cross-2022.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2022 model year,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-22CorollaCross/pdf/T-MMS-22CorollaCross.pdf`
(found through Toyota's owners site). Read on 2026-10-23.

- The guide's maintenance log (pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full, page by page, and the list written from it.
- **One year only.** The 2023 and 2024 guides differ from this one (see
  [toyota-corolla-cross-2023-2024-gas](toyota-corolla-cross-2023-2024-gas.md)): this guide replaces the
  engine oil every 5,000 miles under any of four special operating conditions (dusty roads, towing,
  short trips below freezing, long idling), where the later ones do so for dusty roads only; it says
  "radiator and condenser"; and its 120,000 transfer case oil inspection has no "(AWD)".
- **No hybrid this year.** The Corolla Cross Hybrid starts with the 2023 model year (Toyota's address
  for a 2022 hybrid guide answered 403), so this list names no engine or powertrain and is for every 2022
  Corolla Cross. NHTSA's model list for Toyota, 2022, gives the model only as "Corolla Cross" (checked
  2026-10-23 with `GetModelsForMakeYear`, which is sent only the make and year).
- **All-wheel-drive lines.** The log marks some lines "(AWD)"; they are in this list as printed, in
  `allWheelDriveOnly`, so a Corolla Cross on record as front-wheel drive doesn't get them: re-torque the
  propeller shaft bolt every 15,000 (and at every other milestone on dusty or salted roads or when
  towing), inspect the rear differential oil and the transfer case oil every 15,000, and replace both
  every 15,000 when towing.
- **The 120,000 transfer case inspection is printed without "(AWD)"**, unlike every other transfer case
  line in this guide (the 2023 guide adds the mark). It is kept as printed, "Inspect transfer case oil",
  and so isn't left out for a front-wheel-drive car; nothing is inferred from the other lines.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (printed at every milestone for Hawaii only), the note that the oil is to be replaced at
  5,000 miles if 0W-16 wasn't used at the last change, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval under
  any of its four special operating conditions), tire rotation 5,000 / 6, cabin and engine air filters
  30,000 / 36, spark plugs 120,000 / 144, engine coolant first at 100,000 miles and then every
  50,000 / 60, and the rear differential oil inspection (AWD, in `allWheelDriveOnly`) every 15,000 / 18
  (`rear-diff-check`). The guide's "or 120 months" for the first coolant change isn't used: it counts from
  the day the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): rear differential oil and transfer case oil (AWD) every 15,000 / 18 when
  towing or carrying heavy loads, and automatic transmission fluid every 60,000 / 72 when towing or
  idling for long periods.
- **The rear differential inspection is a reminder**, at the table's own 15,000 / 18. Under the owner's
  ruling of 2026-10-23 (#33), any vehicle with a differential needs it inspected, so every list whose
  guide prints the inspection makes it one; logging a change counts as its check. **The transfer case
  inspection is not**: it is printed "(AWD)" every 15,000 to 105,000 but unmarked at 120,000 (above), so
  no one interval with one drive mark fits it, and it stays in the table only, with its check line
  (`transfer-case-check`).
- The other inspections, drive belts (first at 60,000, then every 15,000) and the brake booster's vacuum
  pump (120,000) included, are in the table only. The propeller shaft bolt re-torque has no service line
  and is in the table only, as on the 4Runner.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2023 on are lists of their own ([gas](toyota-corolla-cross-2023-2024-gas.md),
  [hybrid](toyota-corolla-cross-2023-hybrid.md)).
