# Toyota Sequoia, 2016 (gasoline)

How `app/src/main/assets/known-schedules/toyota-sequoia-2016-gas.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2016 Sequoia,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-16Sequoia/pdf/T-MMS-16Sequoia.pdf`, found
through Toyota's owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/sequoia/2016`).
Read on 2026-10-23.

- The guide's maintenance log (pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read and the list written from it, beside the 2018 guide
  (`toyota-sequoia-2018-gas`), which was read in full, and the 2017 guide (`toyota-sequoia-2017-gas`).
  Every difference a count of each job per page reported was looked at on the page. Nothing says the
  log repeats.
- What differs from 2018, and makes this a list of its own:
  - **Propeller shaft lubrication (4WD).** "Lubricate propeller shaft (4WD)" is an ordinary job at every
    15,000, and at every other 5,000 on dusty (or salted) roads or when towing, the same as the
    re-torque of the propeller shaft bolt. 2017 has it too.
  - Unlike 2017, the 2016 guide doesn't inspect the transmission fluid cooler hoses; that is the only
    difference in jobs between the two years.
  - Wording only: the guide says "steering gear box" where 2017 and 2018 say "steering gear" (the same
    job, "Inspect steering gear" here), and the fluid-levels line has no footnote.
- **Engine and powertrain.** The Sequoia had one engine, the 5.7L V8. For 2016 Toyota also published a
  separate guide for the Flexible Fuel Vehicle (`T-MMS-16SequoiaFFV`), with a different oil schedule.
  This list's `engine` block is 5.7L with `"powertrain": "gas"`, so it is matched only for a Sequoia the
  owner said is gasoline; otherwise setup asks which engine it has ("5.7L V8 gasoline").
- Drivetrain is marked on the guide's own lines: the propeller shaft lubrication, front differential and
  transfer case jobs say `(4WD)` and are left out for a truck known to be 2WD. Drive shaft boots are not
  marked 4WD in this guide, so they are kept for every truck.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000
  miles if 0W-20 wasn't used at the last change, the note on the cabin air filter in heavy traffic or
  dust, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the guide's interval under any of
  its four special operating conditions), tire rotation 5,000 / 6, propeller shaft lubrication (4WD)
  15,000 / 18 (5,000 / 6 for severe use: dust, salted roads or towing), cabin and engine air filters
  30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts from the
  day the vehicle went into service, which Glovebox doesn't know.
- Only for severe use: front differential oil (4WD) every 15,000 / 18, rear differential oil every
  30,000 / 36 and automatic transmission fluid every 60,000 / 72 when towing or carrying heavy loads;
  transfer case oil (4WD) every 30,000 / 36 on dusty roads or when towing.
- Inspections are in the table only, with their service line where there is one (brakes, front and
  rear differential oil, transfer case oil, drive belts). Drive belts are first inspected at 60,000
  and then at every 15,000.
- Re-torquing the propeller shaft bolt and tightening the chassis and body nuts and bolts have no
  service line and are in the table only.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: the 2016 flexible-fuel Sequoia. Its guide's log is 48 boxes 2,500 miles apart: the oil
  is changed at every 5,000 as an ordinary job, and the boxes at the odd 2,500s also change it; the
  guide's notes say that is for a truck run on E85 more than half of the time. That condition isn't
  one of the guide's special operating conditions and can't be stated as a reminder the table holds
  (the extra change is printed only at the odd 2,500s), so no flexible-fuel list was written. Years
  before 2016 are outside this issue and were not looked up.
