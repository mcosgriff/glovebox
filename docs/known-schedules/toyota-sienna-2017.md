# Toyota Sienna, 2017

How `app/src/main/assets/known-schedules/toyota-sienna-2017.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2017 model year,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-17Sienna/pdf/T-MMS-17Sienna.pdf`. Read on
2026-10-23.

- The guide's maintenance log (PDF pages 38 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles / 144 months) was read in full and the list written from it. It was also compared, by a
  count of the words on each page, with the 2018 guide, and every difference was looked at on the page.
- 2017 has a list of its own because its log differs from 2018–2020
  ([toyota-sienna-2018-2020](toyota-sienna-2018-2020.md)) in one job: the automatic transmission fluid
  is replaced at 60,000 and 120,000 only when towing or carrying heavy loads, where 2018 on also replaces
  it for long idling or long low-speed driving. The other differences change no job: 2017 has no EFI
  tank additive line (see below for why 2018–2020 leave it out anyway) and no footnote on inspecting
  sealed transmissions, transfer cases and differentials for leaks. The jobs are named as in the
  2018–2020 list, except the drive shaft re-torque, which the 2017 guide gives only as a bolt.
- This year has one engine (the guide names none), and an automatic gearbox only, so the list has no
  `engine` field and no manual marking. There is no hybrid.
- Drivetrain: the Sienna is sold with front-wheel or all-wheel drive, and the guide marks the jobs for
  the all-wheel-drive van on their own lines. **The 2017 guide prints those lines "(4WD)".** Following
  the owner's ruling of 2026-10-23, they are in `allWheelDriveOnly` and named "(AWD)": re-torque the
  drive shaft and propeller shaft bolts, inspect the rear differential and transfer case oil, and, under
  special conditions, the same re-torques and the rear differential and transfer case oil changes. A van
  known to be front-wheel drive doesn't get them. "Inspect drive shaft boots" is not marked in the guide
  (a front-wheel-drive van has drive shafts too), so it is for every van.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000
  miles if 0W-20 wasn't used at the last change, the note on the cabin air filter in heavy traffic or
  dust, and the explanations of each item.
- Table only, with no `service`: "Replace brake booster vacuum pump", once, at 120,000. No catalog line
  logs it (`brake-booster` is the booster, not its pump), and it happens once in the table.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any special operating condition), tire rotation 5,000 / 6, cabin and engine air filters
  30,000 / 36, spark plugs 60,000 / 72, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts from the day
  the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): rear differential oil (AWD) and transfer case oil (AWD) every 15,000 / 18,
  and automatic transmission fluid every 60,000 / 72, all when towing or carrying heavy loads. Under
  ordinary driving the guide only inspects these.
- Inspections are in the table only. The differential and transfer case checks carry their service lines
  (`rear-diff-check`, `transfer-case-check`) so a logged change counts as the check, but none is made a
  reminder, since the owner hasn't asked for that on this model.
- The drive belts are inspected first at 60,000 / 72 and then every 15,000 / 18; they are in the table
  only.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered here: 2018–2020, which have their own list; 2016 and earlier (outside #197; the 2016 guide
  was not read for this list).
