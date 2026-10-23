# Toyota RAV4, 2017–2018 (gasoline)

How `app/src/main/assets/known-schedules/toyota-rav4-2017-2018-gas.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, as listed by Toyota's owners site
(`https://www.toyota.com/service/tcom/cs/manuals/downloads/rav4/<year>`):
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-17RAV4/pdf/T-MMS-17RAV4.pdf` and
`…/T-MMS-18RAV4/pdf/T-MMS-18RAV4.pdf`. Read on 2026-10-23.

- The 2017 guide's maintenance log was read in full and the list written from it: printed pages 38 to 53,
  24 milestones, 5,000 miles or 6 months apart, to 120,000 miles or 144 months. Nothing says it repeats.
- The 2018 guide was compared milestone by milestone (a script's reading of each box, which found no
  difference). The jobs and where they fall are the same. The only change is a footnote on "inspect and adjust
  all fluid levels" about checking sealed units for leaks, which isn't a job.
- **Gasoline and hybrid.** Toyota prints a separate guide for the RAV4 Hybrid, with a different schedule. vPIC
  names both "RAV4" in every year from 2013 to 2018 (`GetModelsForMakeYear`, checked 2026-10-23), and both have
  a 2.5-litre engine, so this list is `"powertrain": "gas"` and the hybrid's lists are `"hybrid"`. A RAV4 with
  no powertrain on record is asked which it is.
- **"(4WD)" is printed, "(AWD)" is written.** The guide marks the propeller shaft bolt re-torque, the rear
  differential oil and the transfer case oil "(4WD)". Toyota sells this RAV4 as all-wheel drive, so by the
  owner's ruling of 2026-10-23 (#33) those jobs are named "(AWD)" and listed in `allWheelDriveOnly`: a RAV4
  known to be front-wheel drive doesn't get them.
- Left out, because they aren't jobs at a milestone: the note to replace the oil at 5,000 miles if 0W-20 wasn't
  used last time, the note on the cabin air filter in heavy traffic or dust, and the footnotes.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 under any of the guide's four special conditions), tire
  rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark plugs 120,000 / 144, and engine coolant
  first at 100,000 miles and then every 50,000 / 60, rear differential oil and transfer case oil inspections
  (AWD) 15,000 / 18. The guide's "or 120 months" for the first coolant change isn't used: it counts from the
  day the vehicle went into service, which Glovebox doesn't know.
- **The rear differential and transfer case oil inspections (AWD) are reminders**, at the table's own
  15,000 / 18, with the `rear-diff-check` and `transfer-case-check` lines; logging a change counts as its
  check. By the owner's ruling of 2026-10-23 (#33) any vehicle with a differential needs it inspected, so the
  4Runner's rule applies wherever a guide prints the inspection.
- Only for severe use (#136): rear differential oil (AWD) and transfer case oil (AWD) every 15,000 / 18 when
  towing or carrying heavy loads; automatic transmission fluid every 60,000 / 72 when towing or with long
  idling.
- In the table only: the re-torque of the propeller shaft bolt (every 15,000 miles, and every 5,000 on dusty or
  salted roads or when towing; there is no catalog line for it, as on the 4Runner list), the drive belt
  inspection (first at 60,000, then every 15,000) and the other inspections. Inspections with a catalog line
  (brakes, drive belts) carry it, so logging that work completes them on Milestones; none of them is a
  reminder.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2016 (its own list, [toyota-rav4-2016-gas](toyota-rav4-2016-gas.md)), 2013–2015 (see that
  page), 2019 on (a new generation).
