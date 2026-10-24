# Toyota Camry V6, 2021–2024

How `app/src/main/assets/known-schedules/toyota-camry-2021-2024-v6.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>Camry/pdf/T-MMS-<yy>Camry.pdf`
(`21` to `24`). Read on 2026-10-23.

- The 2022 guide's maintenance log (pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2021, 2023 and 2024 were compared with it by a script's count of each job on each log page, and by
  which special operating condition each all-wheel-drive and transmission line is printed under;
  2024's 60,000 and 120,000 pages were looked at. The jobs and where they fall are the same in all
  four. What differs is wording: the 2023 and 2024 guides say "Radiator, condenser and/or
  intercooler", and the 2024 oil note names no engine.
- **One log, split by engine** (the owner's ruling of 2026-10-23). The guide prints one log for the
  2.5L 4-cylinder (A25A-FKS) and the 3.5L V6 (2GR-FKS). Three lines name an engine, and every other
  line names none and so is for both:
  - Replace spark plugs at 60,000, "(2GR-FKS engine only)": in this list. The 120,000 change is
    unmarked, so it is here too, which makes the V6's plugs every 60,000.
  - Replace the brake booster's vacuum pump at 120,000, "(2GR-FKS engine only)": in this list.
  - Inspect the brake booster's vacuum pump at 120,000, "(A25A-FKS engine only)": left out.
  Every line's engine is printed plainly, so the split could be made. This list has `engine`
  3.5 and never matches a 2.5L Camry, gas or hybrid; those are not covered by it.
- **All-wheel-drive lines.** The log marks some lines "(AWD)" and names no engine on them, so by the
  guide they are for any Camry with all-wheel drive, and they are in this list as printed, in
  `allWheelDriveOnly`: a V6 on record as front-wheel drive doesn't get them. They are: re-torque the
  propeller shaft bolt every 15,000 (and at every other milestone on dusty or salted roads or when
  towing), inspect the rear differential oil and the transfer case oil every 15,000, and replace
  both under special operating conditions.
- **A line printed under another condition.** At 90,000 all four guides print the rear differential
  and transfer case oil changes under "dirt roads or dusty roads"; at every other 15,000 they are
  under towing and heavy loads (as they are at 90,000 in the separate 2020 AWD guide). Nothing is
  corrected: the table has them at 90,000 for dusty roads and elsewhere for towing, as printed. Since
  neither condition then gives every 15,000, they have no severe-use reminder from this list; they
  are in the table only.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's
  EFI tank additive (printed at every milestone for Hawaii, Puerto Rico and American Samoa only), the
  note that the oil is to be replaced at 5,000 miles if 0W-20 (the V6's oil) wasn't used at the last
  change, and the explanations of each item.
- The brake booster vacuum pump has no service line in Glovebox (`brake-booster` is the booster
  itself), and it is replaced once in the log, so it is in the table only, with no reminder. The
  propeller shaft bolt re-torque has no service line either and is in the table only, as on the
  4Runner.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval under
  any of its four special operating conditions), tire rotation 5,000 / 6, cabin air filter 10,000 / 12,
  engine air filter 30,000 / 36, spark plugs 60,000 / 72, engine coolant first at 100,000 miles and then
  every 50,000 / 60, and, for all-wheel drive (in `allWheelDriveOnly`), the rear differential oil and
  transfer case oil inspections every 15,000 / 18 (`rear-diff-check`, `transfer-case-check`). The guide's
  "or 120 months" for the first coolant change isn't used: it counts from the day the vehicle went into
  service, which Glovebox doesn't know.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing, carrying
  heavy loads or idling for long periods. Under ordinary driving the guide doesn't change it.
- The other inspections, drive belts included (first at 60,000, then every 15,000), are in the table only.
  The two all-wheel-drive inspections are reminders under the owner's ruling of 2026-10-23 (#33): any
  vehicle with a differential needs it inspected, so every list whose guide prints the inspection makes it
  one, at the table's own interval, and logging a change counts as its check.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2018–2020 are a list of their own
  ([toyota-camry-2018-2020-v6](toyota-camry-2018-2020-v6.md)), since their log has no all-wheel-drive
  lines. 2025 on is a new generation, whose guide wasn't read. The 2.5L gas and hybrid Camry have lists of
  their own, told apart by powertrain ([toyota-camry-2021-2024-2.5](toyota-camry-2021-2024-2.5.md),
  [toyota-camry-2021-2024-hybrid](toyota-camry-2021-2024-hybrid.md)).
