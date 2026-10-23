# Toyota Tacoma 2.7L 4-cylinder, 2018–2023

How `app/src/main/assets/known-schedules/toyota-tacoma-2018-2023-4cyl.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>Tacoma/pdf/T-MMS-<yy>Tacoma.pdf`
(`18` to `23`; Toyota's owners site lists each at
`https://www.toyota.com/service/tcom/cs/manuals/downloads/tacoma/<year>`).
Read on 2026-10-23. The guides for 2016 to 2023 were all found, and all eight were compared.

## One guide, two lists

Toyota's guide has one maintenance log for both engines. The V6's own lines say "2GR-FKS engine only"; the
2.7L four-cylinder (2TR-FE) is never named, so every line without that mark is for both. Under the owner's
ruling of 2026-10-23 (#33: a log is split per engine only where each line's engine is printed plainly), the
log is split: the 3.5L V6 list is every line, the 2.7L list every line not marked for the V6. Every line was
checked for its mark; none was unclear.

In these years’ guides the V6-only lines are spark plugs at 60,000 (both engines have them at 120,000); the
brake booster vacuum pump replacement at 120,000; from 2020, the engine oil cooler hose inspection at every
15,000.
They are left out of this list. The V6 lists for these years: [3.5L V6, 2018](toyota-tacoma-2018-v6.md), [3.5L
V6, 2019](toyota-tacoma-2019-v6.md), [3.5L V6, 2020–2023](toyota-tacoma-2020-2023-v6.md).

## What was read

- The 2020 guide's maintenance log (pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to 120,000
  miles or 144 months) was read in full, by eye, and the lists were written from it.
- The other seven years (pages 39 to 54) were compared with it by a script's count of each job at each
  milestone, and every difference it reported was looked at on the page. The months and milestones are the
  same in all eight.
- The runs of years that agree, by engine:
  - 3.5L V6: 2016, 2017, 2018, 2019, and 2020–2023, each its own list.
  - 2.7L four-cylinder: 2016, 2017, and 2018–2023. From 2018 the years differ only in V6 lines, so the
    four-cylinder's lines are the same from 2018 to 2023.

What these years say, against the others:

- 2018: As 2017, and the automatic transmission fluid cooler hoses and connections are inspected again at
  every 30,000.
- 2018: **The brake booster vacuum pump is replaced at 120,000**, marked "2GR-FKS engine only; except for
  off-road package models": a V6 line, named "Replace brake booster vacuum pump (except off-road package)".
- 2019: As 2018, except that the vacuum pump replacement is marked "2GR-FKS engine only; if equipped" in place
  of excluding the off-road package: named "Replace brake booster vacuum pump (if equipped)".
- 2020–2023: As 2019, and the **engine oil cooler hoses and connections** are inspected at every 15,000,
  marked "2GR-FKS engine only".
- 2020–2023: 2021 and 2022 are the same as 2020. 2023 says "Radiator, condenser and/or intercooler" where the
  others say "Radiator and condenser", and its fluid-level footnote says "cooling systems": wording only.
- None of those differences touches a line without the V6 mark, so 2018 to 2023 make one four-cylinder list.

## How the lines were taken

- The Tacoma is a four-wheel-drive truck, with two-wheel-drive versions; the guide marks its 4WD lines "(4WD)"
  and they are in `fourWheelDriveOnly`, left out for a truck known to be 2WD. Propeller shaft lubrication is
  marked "(4WD)"; the re-torque of the propeller shaft bolt is not, so a 2WD truck keeps it.
- **Gearbox.** Under towing the guide replaces "manual transmission oil" at 30,000 and 90,000, and has one
  line, "transmission fluid or oil", at 60,000 and 120,000: a single line on the page for both gearboxes.
  So a manual's oil is changed every 30,000 under towing and an automatic's fluid every 60,000.
  - "Replace transmission fluid or oil · towing or heavy loads" is the `trans-fluid` reminder, every
    60,000 / 72 months, for severe use only, for either gearbox.
  - "Replace manual transmission oil · towing or heavy loads" (30,000 and 90,000) is in `manualOnly` and in
    the table only, with no `service`. With `trans-fluid` it would be one job with the 60,000 line, at every
    30,000, and the automatic's 60,000 reminder would no longer be the table's own (rule 7). So a manual's
    30,000 change is shown on the table but not reminded of.
  - "Replace automatic transmission fluid · long idling or low-speed driving" (60,000 and 120,000) is in
    `manualExcluded`, in the table only, with no `service`: a milestone can hold a service once, and the
    towing line at the same milestones has it. Its reminder is the towing line's, which falls at the same
    mileages.
  - The inspection of the automatic transmission fluid cooler hoses is in `manualExcluded`, by its name.
- Jobs for the guide's four special operating conditions (dirt or dusty roads; towing, a car-top carrier or
  heavy loads; repeated short trips below freezing; long idling or low-speed driving) are named with their
  condition. Where the guide repeats a line under two conditions it is one job here, named with both.
- Inspections are in the table only, except the differential and transfer case oils, which are reminders
  too (below). Those with a catalog line carry it: fluid levels (`fluid-check`), wiper blades
  (`wipers-check`), brakes (`brake-inspect`), drive belts (`belt-check`, first at 60,000, then every
  15,000), engine coolant (`cooling-check`), and the differential and transfer case oils
  (`front-diff-check`, `rear-diff-check`, `transfer-case-check`).
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles if
  0W-20 wasn't used at the last change, the note on the cabin filter in heavy traffic or dust, the daily
  checks for off-road driving, and the explanations of each item.

## Reminders

- Engine oil 10,000 / 12 (5,000 / 6 for severe use, the guide's interval under any of the four conditions),
  tire rotation 5,000 / 6, propeller shaft lubrication (4WD) 15,000 / 18 (5,000 / 6 for severe use: dust,
  salted roads or towing), cabin air filter 20,000 / 24, engine air filter 30,000 / 36, engine coolant first
  at 100,000 and then every 50,000 / 60, and spark plugs every 120,000 / 144 months. The guide's "or 120
  months" for the first
  coolant change isn't used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Inspections that are reminders too: front differential oil (4WD) and rear differential oil every
  15,000 / 18, transfer case oil (4WD) every 30,000 / 36. Logging a change of one counts as its check.
  This is the owner's ruling of 2026-10-23 (#33): any vehicle with a differential needs it inspected, so
  the 4Runner's rule applies to every list whose guide prints the inspection.
- Only for severe use (#136): front (4WD) and rear differential oil every 15,000 / 18 under towing, transfer
  case oil (4WD) every 30,000 / 36 on dusty roads or when towing, and transmission fluid or oil every
  60,000 / 72 under towing.
- In the table only: the re-torque of the propeller shaft bolt; the tightening of chassis and body nuts and
  bolts.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.

## Not covered

- The other engine in these years (see above), and other years of this engine: [2.7L 4-cylinder,
  2016](toyota-tacoma-2016-4cyl.md), [2.7L 4-cylinder, 2017](toyota-tacoma-2017-4cyl.md).
- 2015 and earlier (the previous generation, whose guides weren't read) and 2024 on (a new generation).
