# Toyota Camry V6, 2018–2020

How `app/src/main/assets/known-schedules/toyota-camry-2018-2020-v6.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>Camry/pdf/T-MMS-<yy>Camry.pdf`
(`18` to `20`). Read on 2026-10-23.

- The 2020 guide's maintenance log (pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2018 and 2019 were compared with it by a script's count of each job on each log page, and the
  pages that carry the engine lines were looked at (2018's 60,000 and 120,000, 2019's 120,000). The jobs and where they fall
  are the same in all three. What differs is wording: the 2018 guide is laid out in one column, says
  "Hawaii, Puerto Rico, and American Samoa", and its oil note names no engine ("0W-16/0W-20").
- **One log, split by engine** (the owner's ruling of 2026-10-23). The guide prints one log for the
  2.5L 4-cylinder (A25A-FKS) and the 3.5L V6 (2GR-FKS). Three lines name an engine, and every other
  line names none and so is for both:
  - Replace spark plugs at 60,000, "(2GR-FKS engine only)": in this list. The 120,000 change is
    unmarked, so it is here too, which makes the V6's plugs every 60,000.
  - Replace the brake booster's vacuum pump at 120,000, "(2GR-FKS engine only)": in this list.
  - Inspect the brake booster's vacuum pump at 120,000, "(A25A-FKS only)": left out.
  Every line's engine is printed plainly, so the split could be made. This list has `engine`
  3.5 and never matches a 2.5L Camry, gas or hybrid; those are not covered by it.
- These guides have no line marked for a drivetrain; the 2018–2020 Camry guide is for front-wheel
  drive. The 2020 all-wheel-drive Camry has a guide of its own (`T-MMS-20CamryAWD`), which names no
  engine. It wasn't used for this list.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's
  EFI tank additive (printed at every milestone for Hawaii, Puerto Rico and American Samoa only), the
  note that the oil is to be replaced at 5,000 miles if 0W-20 (the V6's oil) wasn't used at the last
  change, and the explanations of each item.
- The brake booster vacuum pump has no service line in Glovebox (`brake-booster` is the booster
  itself), and it is replaced once in the log, so it is in the table only, with no reminder.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any of its four special operating conditions), tire rotation 5,000 / 6, cabin air filter
  10,000 / 12, engine air filter 30,000 / 36, spark plugs 60,000 / 72, and engine coolant first at
  100,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the first coolant change
  isn't used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing, carrying
  heavy loads or idling for long periods. Under ordinary driving the guide doesn't change it.
- The inspections, drive belts included (first at 60,000, then every 15,000), are in the table only.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2021–2024 are a list of their own
  ([toyota-camry-2021-2024-v6](toyota-camry-2021-2024-v6.md)), since their log adds all-wheel-drive
  lines these years don't have. The 2.5L gas and hybrid Camry are not in this list: they share an
  engine size and need a way to tell them apart that Glovebox doesn't have yet.
