# Toyota Camry 2.5L, 2020

How `app/src/main/assets/known-schedules/toyota-camry-2020-2.5.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guides for the 2020 Camry, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-20Camry/pdf/T-MMS-20Camry.pdf` and, for
all-wheel drive, `https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-20CamryAWD/pdf/T-MMS-20CamryAWD.pdf`.
Toyota's owners site lists the same two for 2020. Read on 2026-10-23.

- **Two guides, both read in full.** The Camry guide's maintenance log (pages 39 to 54: 24 milestones,
  5,000 miles or 6 months apart, to 120,000 miles) and the all-wheel-drive guide's (pages 40 to 55) were
  read page by page.
- **One log, split by engine** (the owner's ruling of 2026-10-23). The Camry guide prints one log for
  the 2.5L 4-cylinder (A25A-FKS) and the 3.5L V6 (2GR-FKS). Three lines name an engine, and every other
  line names none and so is for both:
  - Replace spark plugs at 60,000, "(2GR-FKS engine only)": left out of this list.
  - Replace the brake booster's vacuum pump at 120,000, "(2GR-FKS engine only)": left out.
  - Inspect the brake booster's vacuum pump at 120,000, "(A25A-FKS engine only)": in this list.
  The spark plug change at 120,000 is unmarked, so it is here, which makes the 2.5L's plugs every
  120,000. Every line's engine is printed plainly, so the split could be made. The V6 has a list of its
  own (`toyota-camry-2018-2020-v6`).
- **The all-wheel-drive guide.** It names no engine. Its log is the Camry guide's log for the 2.5L,
  line for line (no 60,000 spark plugs, the vacuum pump inspected at 120,000, its oil note naming
  0W-16 only), with these lines added and without the EFI tank additive line:
  - Re-torque the propeller shaft bolt every 15,000, and at every other milestone on dirt or dusty
    roads (a footnote adds roads with salt) or when towing.
  - Inspect the rear differential oil and the transfer case oil every 15,000.
  - Replace both every 15,000 when towing, using a car-top carrier or carrying heavy loads.
  Those lines are in this list named "(AWD)" and kept in `allWheelDriveOnly`, so a Camry on record as
  front-wheel drive doesn't get them; every other line is the same in both guides. The guide prints
  them without a mark, since the whole guide is for all-wheel drive.
- **Gas, not hybrid.** The Camry Hybrid also has a 2.5L engine and has a guide of its own, so this list
  has `engine.powertrain` `gas` and the hybrid list
  ([toyota-camry-2018-2020-hybrid](toyota-camry-2018-2020-hybrid.md)) has `hybrid`. A 2.5L Camry whose
  powertrain isn't on record is asked which it is.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's
  EFI tank additive (in the Camry guide at every milestone for Hawaii, Puerto Rico and American Samoa
  only), the note that the oil is to be replaced at 5,000 miles if 0W-16 wasn't used at the last
  change, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval under
  any of its four special operating conditions), tire rotation 5,000 / 6, cabin air filter 10,000 / 12,
  engine air filter 30,000 / 36, spark plugs 120,000 / 144, engine coolant first at 100,000 miles and then
  every 50,000 / 60, and, for all-wheel drive (in `allWheelDriveOnly`), the rear differential oil and
  transfer case oil inspections every 15,000 / 18 (`rear-diff-check`, `transfer-case-check`). The guide's
  "or 120 months" for the first coolant change isn't used: it counts from the day the vehicle went into
  service, which Glovebox doesn't know.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing, carrying
  heavy loads or idling for long periods; and, for all-wheel drive, rear differential oil and transfer
  case oil every 15,000 / 18 when towing or carrying heavy loads. Under ordinary driving the guide only
  inspects the two oils and doesn't change the fluid.
- The other inspections, drive belts (first at 60,000, then every 15,000) and the vacuum pump included,
  are in the table only. The two all-wheel-drive inspections are reminders under the owner's ruling of
  2026-10-23 (#33): any vehicle with a differential needs it inspected, so every list whose guide prints
  the inspection makes it a reminder, at the table's own interval, and logging a change counts as its
  check. The propeller shaft bolt re-torque has no service line and is in the table only, as on the
  4Runner.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered by this list: 2018–2019 ([toyota-camry-2018-2019-2.5](toyota-camry-2018-2019-2.5.md)),
  which had no all-wheel drive, and 2021–2024 ([toyota-camry-2021-2024-2.5](toyota-camry-2021-2024-2.5.md)),
  whose one guide prints the all-wheel-drive oil changes at 90,000 under dusty roads where the 2020
  all-wheel-drive guide prints them under towing.
