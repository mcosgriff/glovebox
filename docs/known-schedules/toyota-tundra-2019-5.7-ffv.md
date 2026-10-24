# Toyota Tundra 5.7L V8 flex-fuel (FFV), 2019

How `app/src/main/assets/known-schedules/toyota-tundra-2019-5.7-ffv.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Issue #189.

Source: Toyota's Warranty and Maintenance Guide for flex-fuel (FFV) trucks, as listed on Toyota's owners
site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/tundra/2019`), at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-19TundraFFV/pdf/T-MMS-19TundraFFV.pdf`.
Downloaded on 2026-10-22 and read on 2026-10-23.

- The 2019 FFV guide's maintenance log (PDF pages 39 to 54: 48 boxes, 2,500 miles or 3 months apart, to
  120,000 miles) was compared with the 2018 FFV log, which was read in full, and with the 2019 gasoline log,
  by a script; every difference it reported was looked at on the page (PDF pages 37, 39, 40, 42 and 53 by
  eye; every difference was a missing "(4WD)" or noise).
- Apart from the oil, the jobs and where they fall are those of the 2019 gasoline log, without its "(4WD)"
  marks.
- **Oil.** The FFV log draws an oil change in every box, so every 2,500 miles or 3 months, and has no oil
  change among its special operating conditions. So the table has the ordinary job (`oil`) in every box, as
  printed.
- **E85: the "Runs mostly on E85" switch** (the owner's ruling of 2026-10-23, #33). The guide's introduction
  and its explanation of the oil say that the change every 2,500 / 3 is for a truck run on E85 more than half
  the time, and that otherwise the normal schedule of every 5,000 / 6 applies. So the oil reminder is every
  5,000 / 6, and the list's `e85` gives it every 2,500 / 3, which is what it counts by while the owner has
  switched "Runs mostly on E85" on (off until they do; nothing is assumed). The E85 interval is held to the
  log as any interval is; the 5,000 / 6 falls on it, at every second box with its months. Neither has a
  severe interval: Normal / Severe still governs only the special-condition jobs below.
- The boxes at the odd 2,500s also hold the floor mat, fluid level and wiper checks; they are in the table
  as printed, with no condition, since the guide gives them none.
- **Engine.** The FFV Tundra had the 5.7 L V8 only (fueleconomy.gov's model lists, checked 2026-10-23; the
  2018 guide names 3UR-FBE), so the list is for 5.7 L with `"powertrain": "flex-fuel"`. The gasoline trucks'
  lists for this year are `toyota-tundra-2018-2019-4.6` and `toyota-tundra-2018-2019-5.7`. A truck whose
  powertrain isn't on record is asked which it has; a blank is never taken for either.
- **The 2019 FFV guide prints no "(4WD)" on any line**, where the 2019 gasoline guide and the 2018 FFV guide
  mark the drive shaft boots, front differential oil and transfer case oil "(4WD)". The list keeps the
  guide's lines as printed, so it has no `fourWheelDriveOnly` and these jobs are named without the mark.
  fueleconomy.gov lists the 2019 FFV Tundra as 4WD only. This is why 2018 and 2019 are two lists: the same
  jobs, but not the same lines.
- Also left out, because they aren't jobs at a milestone: the note on the cabin air filter in heavy traffic or
  dust, the daily checks for off-road driving, and the explanations of each item.
- Reminders: engine oil as above, tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark
  plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every 50,000 / 60. The guide's "or
  120 months" for the first coolant change isn't used: it counts from the day the vehicle went into service,
  which Glovebox doesn't know.
- Inspections that are reminders too, as on the 4Runner (asked for by the owner, 2026-10-13): front and rear
  differential oil every 15,000 / 18, transfer case oil every 30,000 / 36. Logging a change of one counts as
  its check. The other inspections are in the table only (the drive belts are first inspected at 60,000 and
  then every 15,000).
- Table only, with no catalog line: re-torquing the propeller shaft bolt (every 15,000, and at the
  milestones between on dusty or salted roads or when towing) and tightening the chassis and body nuts and
  bolts (dusty roads or towing), as on the 4Runner.
- Only for severe use (#136): front differential oil every 15,000 / 18, rear differential oil every 30,000 /
  36 and automatic transmission fluid every 60,000 / 72 when towing or carrying heavy loads; transfer case
  oil every 30,000 / 36 on dusty roads or when towing. Under ordinary driving the guide only inspects these.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- No guide was seen to say what follows 120,000 miles, so the table doesn't repeat.
- Not covered by this list: other years (each FFV year is its own list); gasoline trucks (their own lists).
  Toyota's owners site lists no FFV guide for 2020 or 2021.
