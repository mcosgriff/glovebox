# Toyota Camry 2.5L, 2018–2019

How `app/src/main/assets/known-schedules/toyota-camry-2018-2019-2.5.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>Camry/pdf/T-MMS-<yy>Camry.pdf`
(`18` and `19`). Read on 2026-10-23.

- The 2018 guide's maintenance log (pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it. The 2020 guide's log was read in full
  as well (for [toyota-camry-2020-2.5](toyota-camry-2020-2.5.md)) and has the same jobs in the same
  places.
- 2019 was compared with 2020 by a script's count of each line on each log page, sorted by the special
  operating condition it is printed under; the pages where it reported a difference (35,000, 65,000,
  115,000 and 120,000) were looked at, and the differences are wording or how the text came out. The
  jobs and where they fall are the same in all three years. What differs is wording: the 2018 guide is
  laid out differently, and its oil note names no engine ("0W-16/0W-20"); the vacuum pump line says
  "(A25A-FKS only)" in 2018 and 2020 and "(A25A-FKS engine only)" in 2019.
- **One log, split by engine** (the owner's ruling of 2026-10-23). The guide prints one log for the
  2.5L 4-cylinder (A25A-FKS) and the 3.5L V6 (2GR-FKS). Three lines name an engine, and every other
  line names none and so is for both:
  - Replace spark plugs at 60,000, "(2GR-FKS engine only)": left out of this list.
  - Replace the brake booster's vacuum pump at 120,000, "(2GR-FKS engine only)": left out.
  - Inspect the brake booster's vacuum pump at 120,000, "(A25A-FKS only)": in this list.
  The spark plug change at 120,000 is unmarked, so it is here, which makes the 2.5L's plugs every
  120,000. Every line's engine is printed plainly, so the split could be made. The V6 has a list of its
  own (`toyota-camry-2018-2020-v6`).
- **Gas, not hybrid.** The Camry Hybrid also has a 2.5L engine and has a guide of its own, so this list
  has `engine.powertrain` `gas` and the hybrid list
  ([toyota-camry-2018-2020-hybrid](toyota-camry-2018-2020-hybrid.md)) has `hybrid`. A 2.5L Camry whose
  powertrain isn't on record is asked which it is.
- These guides have no line marked for a drivetrain, and no all-wheel-drive guide was found for these
  years (Toyota's address for `T-MMS-19CamryAWD` answered 403); the first is the 2020 one.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's
  EFI tank additive (printed at every milestone for Hawaii, Puerto Rico and American Samoa only), the
  note that the oil is to be replaced at 5,000 miles if 0W-16 (the 2.5L's oil) wasn't used at the last
  change, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any of its four special operating conditions), tire rotation 5,000 / 6, cabin air filter
  10,000 / 12, engine air filter 30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at
  100,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the first coolant change
  isn't used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing, carrying
  heavy loads or idling for long periods. Under ordinary driving the guide doesn't change it.
- The inspections, drive belts (first at 60,000, then every 15,000) and the vacuum pump included, are
  in the table only.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2020 is a list of its own ([toyota-camry-2020-2.5](toyota-camry-2020-2.5.md)), since an
  all-wheel-drive Camry was sold that year with a guide of its own; 2021–2024 are another
  ([toyota-camry-2021-2024-2.5](toyota-camry-2021-2024-2.5.md)), since their log adds all-wheel-drive
  lines. 2017 and earlier, and 2025 on, are other generations, whose guides weren't read.
