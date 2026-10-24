# Toyota Sequoia, 2016 (flex-fuel)

How `app/src/main/assets/known-schedules/toyota-sequoia-2016-ffv.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Issue #202.

Source: Toyota's Warranty and Maintenance Guide for the 2016 Sequoia Flexible Fuel Vehicle (FFV),
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-16SequoiaFFV/pdf/T-MMS-16SequoiaFFV.pdf`,
found through Toyota's owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/sequoia/2016`).
Downloaded on 2026-10-22 and read on 2026-10-23.

- The guide's maintenance log (pages 39 to 54: 48 boxes, 2,500 miles or 3 months apart, to 120,000
  miles) was compared by a script, box by box, with the 2016 gasoline guide's log
  (`toyota-sequoia-2016-gas`) and beside the 2018 FFV log (`toyota-sequoia-2018-ffv`), which was read in
  full. Every difference the script reported was looked at on the page (pages 46 and 54 by eye: line
  breaks only). Nothing says the log repeats.
- **Apart from the oil, the log is the same as the 2016 gasoline log**: the same jobs at the same
  5,000s, in the same words. So it differs from 2018 as that list does: "Lubricate propeller shaft
  (4WD)" is an ordinary job at every 15,000, and at every other 5,000 on dusty (or salted) roads or when
  towing; there is no transmission fluid cooler hose inspection (2017 has one); and the guide says
  "steering gear box" ("Inspect steering gear" here).
- **Oil.** The FFV log draws an oil change in every box, so every 2,500 miles or 3 months, and has no
  oil change among its special operating conditions (it has two, dusty roads and towing, where the
  gasoline log has four). The boxes at the odd 2,500s hold only that oil change and the floor mat,
  fluid level and wiper checks, which are in the table as printed. So the oil change is the ordinary
  job (`oil`) in every box, and its reminder is every 2,500 / 3, with no severe interval.
- **Left out: the E85 note** (the owner's ruling of 2026-10-23, #33). The guide's introduction says that
  the change every 2,500 / 3 is for a truck run on E85 more than half the time, and that otherwise the
  normal schedule of every 5,000 / 6 applies. Only that note says so; the log itself draws the change
  in every box. The note is left out, as the gasoline guide's note on replacing the oil at 5,000 miles
  if 0W-20 wasn't used is, and the table and the reminder stay as the log prints them. An owner who
  runs on E85 less than half the time can change the reminder.
- **Engine and powertrain.** The Sequoia had one engine, the 5.7L V8. This list's `engine` block is
  5.7L with `"powertrain": "flex-fuel"`, so it is matched only for a Sequoia on record as flex-fuel
  (vPIC's E85 fuel, or the owner's pick); the gasoline truck's list is `toyota-sequoia-2016-gas`. A
  truck whose powertrain isn't on record is asked which engine it has ("5.7L V8 flex-fuel (FFV)" or
  "5.7L V8 gasoline"); a blank is never taken for either.
- Drivetrain is marked on the guide's own lines: the propeller shaft lubrication, front differential and
  transfer case jobs say `(4WD)` and are left out for a truck known to be 2WD. Drive shaft boots are not
  marked 4WD in this guide, so they are kept for every truck.
- Also left out, because they aren't jobs at a milestone: the note on the cabin air filter in heavy
  traffic or dust, the daily checks for off-road driving, and the explanations of each item.
- Reminders: engine oil as above, tire rotation 5,000 / 6, propeller shaft lubrication (4WD) 15,000 / 18
  (5,000 / 6 for severe use: dust, salted roads or towing), cabin and engine air filters 30,000 / 36,
  spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every 50,000 / 60. The
  guide's "or 120 months" for the first coolant change isn't used: it counts from the day the vehicle
  went into service, which Glovebox doesn't know.
- Only for severe use: front differential oil (4WD) every 15,000 / 18, rear differential oil every
  30,000 / 36 and automatic transmission fluid every 60,000 / 72 when towing or carrying heavy loads;
  transfer case oil (4WD) every 30,000 / 36 on dusty roads or when towing.
- Inspections that are reminders too, as on the gasoline list: front differential oil (4WD) and rear
  differential oil every 15,000 / 18, transfer case oil (4WD) every 30,000 / 36. Logging a change of one
  counts as its check. This is the owner's ruling of 2026-10-23 (#33): any vehicle with a differential
  needs it inspected. The other inspections (brakes, drive belts) are in the table only, with their
  service line where there is one. Drive belts are first inspected at 60,000 and then at every 15,000.
- Re-torquing the propeller shaft bolt and tightening the chassis and body nuts and bolts have no
  service line and are in the table only.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered by this list: the gasoline Sequoia (`toyota-sequoia-2016-gas`); 2017 and 2018, whose FFV
  logs differ as their gasoline logs do (`toyota-sequoia-2017-ffv`, `toyota-sequoia-2018-ffv`). Years
  before 2016 are outside this issue and were not looked up.
