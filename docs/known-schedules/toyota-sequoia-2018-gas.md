# Toyota Sequoia, 2018 (gasoline)

How `app/src/main/assets/known-schedules/toyota-sequoia-2018-gas.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2018 Sequoia,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-18Sequoia/pdf/T-MMS-18Sequoia.pdf`, found
through Toyota's owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/sequoia/2018`).
Read on 2026-10-23.

- The guide's maintenance log (pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it. Nothing says the log repeats.
- Its jobs and where they fall are the same as in the 2019–2022 guides (`toyota-sequoia-2019-2022`);
  only line breaks and footnote numbers differ. It is a list of its own, not part of that one, because
  2018 also had a flexible-fuel Sequoia with its own guide (below), so this list is for the gasoline
  truck only.
- **Engine and powertrain.** The Sequoia had one engine, the 5.7L V8. For 2018 Toyota also published a
  separate guide for the Flexible Fuel Vehicle (`T-MMS-18SequoiaFFV`), with a different oil schedule.
  This list's `engine` block is 5.7L with `"powertrain": "gas"`, so it is matched only for a Sequoia the
  owner said is gasoline; otherwise setup asks which engine it has ("5.7L V8 gasoline").
- Drivetrain is marked on the guide's own lines: the front differential and transfer case jobs say
  `(4WD)` and are left out for a truck known to be 2WD. Drive shaft boots are not marked 4WD in this
  guide, so they are kept for every truck.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000
  miles if 0W-20 wasn't used at the last change, the note on the cabin air filter in heavy traffic or
  dust, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the guide's interval under any of
  its four special operating conditions), tire rotation 5,000 / 6, cabin and engine air filters
  30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts from the
  day the vehicle went into service, which Glovebox doesn't know.
- Only for severe use: front differential oil (4WD) every 15,000 / 18, rear differential oil every
  30,000 / 36 and automatic transmission fluid every 60,000 / 72 when towing or carrying heavy loads;
  transfer case oil (4WD) every 30,000 / 36 on dusty roads or when towing.
- Inspections that are reminders too: front differential oil (4WD) and rear differential oil every
  15,000 / 18, transfer case oil (4WD) every 30,000 / 36. Logging a change of one counts as its check.
  This is the owner's ruling of 2026-10-23 (#33): any vehicle with a differential needs it inspected,
  so the 4Runner's rule applies to every list whose guide prints the inspection. The other inspections
  are in the table only, with their service line where there is one (brakes, drive belts). Drive belts
  are first inspected at 60,000 and then at every 15,000.
- Re-torquing the propeller shaft bolt and tightening the chassis and body nuts and bolts have no
  service line and are in the table only.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: the 2018 flexible-fuel Sequoia. Its guide's log is 48 boxes 2,500 miles apart: the oil
  is changed at every 5,000 as an ordinary job, and the boxes at the odd 2,500s also change it; the
  guide's notes say that is for a truck run on E85 more than half of the time. That condition isn't
  one of the guide's special operating conditions and can't be stated as a reminder the table holds
  (the extra change is printed only at the odd 2,500s), so no flexible-fuel list was written.
  2016 and 2017 have lists of their own (`toyota-sequoia-2016-gas`, `toyota-sequoia-2017-gas`), which
  differ from this one.
