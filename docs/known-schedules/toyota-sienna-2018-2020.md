# Toyota Sienna, 2018–2020

How `app/src/main/assets/known-schedules/toyota-sienna-2018-2020.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>Sienna/pdf/T-MMS-<yy>Sienna.pdf`
(`18`, `19` and `20`). Read on 2026-10-23.

- The 2018 guide's maintenance log (PDF pages 38 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles / 144 months) was read in full and the list written from it.
- 2019 (PDF pages 38 to 54) and 2020 (PDF pages 39 to 55) were compared with it page by page, by a count of
  the words on each page, and every difference was looked at on the page. 2019 is the same as 2018; the
  one difference is the wording of one oil line ("engine oil and filter" for "engine oil and oil filter"
  at 25,000 under towing). 2020 has the same jobs in the same places; it differs in two words, both
  wording: its drivetrain lines are marked "(AWD)" where 2018 and 2019 print "(4WD)", and the drive
  shaft re-torque is of a "nut" where 2018 and 2019 say "bolt". That job is named "Re-torque drive shaft
  bolt or nut (AWD)" so it reads true for all three years.
- These years have one engine (the guides name none), and an automatic gearbox only, so the list has no
  `engine` field and no manual marking. There is no hybrid.
- Drivetrain: the Sienna is sold with front-wheel or all-wheel drive, and the guide marks the jobs for
  the all-wheel-drive van on their own lines. **The 2018 and 2019 guides print those lines "(4WD)"**
  (and the 2020 guide "(AWD)"). Following the owner's ruling of 2026-10-23, they are in
  `allWheelDriveOnly` and named "(AWD)": re-torque the drive shaft and propeller shaft fasteners,
  inspect the rear differential and transfer case oil, and, under special conditions, the same re-torques
  and the rear differential and transfer case oil changes. A van known to be front-wheel drive doesn't
  get them. "Inspect drive shaft boots" is not marked in the guide (a front-wheel-drive van has drive
  shafts too), so it is for every van.
- Left out, because it is for a region only: "Add Toyota EFI Tank Additive or equivalent", printed at
  every milestone for Hawaii, Puerto Rico, Saipan and American Samoa only. Glovebox has no way to say a
  job is for one region, and putting it in every owner's table would be wrong for nearly all of them.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000
  miles if 0W-20 wasn't used at the last change, the note on the cabin air filter in heavy traffic or
  dust, the footnote on inspecting sealed transmissions, transfer cases and differentials for leaks
  (it changes no job), and the explanations of each item.
- Table only, with no `service`: "Replace brake booster vacuum pump", once, at 120,000. No catalog line
  logs it (`brake-booster` is the booster, not its pump), and it happens once in the table.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any special operating condition), tire rotation 5,000 / 6, cabin and engine air filters
  30,000 / 36, spark plugs 60,000 / 72, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts from the day
  the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): rear differential oil (AWD) and transfer case oil (AWD) every 15,000 / 18
  when towing or carrying heavy loads, and automatic transmission fluid every 60,000 / 72 when towing,
  carrying heavy loads or idling for long periods or driving long distances at low speed (named "towing or
  long idling", as a name may be at most 60 characters). Under ordinary driving the guide only inspects these.
- Inspections are in the table only. The differential and transfer case checks carry their service lines
  (`rear-diff-check`, `transfer-case-check`) so a logged change counts as the check, but none is made a
  reminder, since the owner hasn't asked for that on this model.
- The drive belts are inspected first at 60,000 / 72 and then every 15,000 / 18; they are in the table
  only.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered here: 2017, which has its own list ([toyota-sienna-2017](toyota-sienna-2017.md)) because its
  guide replaces the automatic transmission fluid for towing only; 2016 and earlier (outside #197; the
  2016 guide was not read for this list); 2021 on (outside #197).
