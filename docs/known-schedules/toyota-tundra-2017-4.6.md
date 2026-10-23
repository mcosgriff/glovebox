# Toyota Tundra 4.6L V8, 2017

How `app/src/main/assets/known-schedules/toyota-tundra-2017-4.6.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Issue #189.

Source: Toyota's Warranty and Maintenance Guide for gasoline trucks, as listed on Toyota's owners site
(`https://www.toyota.com/service/tcom/cs/manuals/downloads/tundra/<year>`), at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-17Tundra/pdf/T-MMS-17Tundra.pdf`.
Downloaded on 2026-10-22 and read on 2026-10-23.

- The 2017 guide's maintenance log (PDF pages 39 to 54, printed 38 to 53: 24 milestones, 5,000 miles or 6
  months apart, to 120,000 miles) was compared with the 2018 log, which was read in full, by a script; every
  page of the 2017 log (PDF pages 39 to 54) was then read by eye. The script's grouping of the
  special-condition lines was unreliable on this layout.
- Unlike 2018 on, this year's log lubricates the propeller shaft "(4WD)" every 15,000 miles, and at every
  milestone between under dust, salted roads or towing. That is a reminder: every 15,000 / 18, and 5,000 / 6
  for severe use (`driveshaft`, as on the 4Runner), left out for a truck known to be 2WD.
- **Engines and fuel.** The 2016–2019 Tundra was sold with a 4.6 L V8 (1UR-FE) or a 5.7 L V8 (3UR-FE), and
  the 5.7 L was also sold as a flex-fuel (FFV) truck, which has a guide of its own (T-MMS-<yy>TundraFFV)
  with a different oil schedule. The emissions-warranty pages of the guides name 1UR-FE and 3UR-FE (2018
  also 3UR-FBE), and fueleconomy.gov's model lists (checked 2026-10-23) give the 4.6 L and 5.7 L for
  gasoline trucks in every one of these years and the 5.7 L only for FFV trucks (2WD and 4WD in 2016 and
  2017, 4WD only in 2018 and 2019). The gasoline guide's log has no per-engine lines: every line is for both
  engines. It is still one list per engine size, with `"powertrain": "gas"`, because that is how Glovebox
  keeps a gasoline 5.7 L apart from an FFV one: a list without an engine would be given to an FFV truck
  without asking. **This is not a split of the log by engine** (the owner's ruling of 2026-10-23 on splits
  doesn't come into it): the 4.6 L and 5.7 L lists of a year are the same table and the same reminders.
- The list for the other engine of these years is `toyota-tundra-2017-5.7`, the same table. The FFV truck has its own list (below).
- Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the list and the 4WD-only jobs say
  `(4WD)` and are left out for a truck known to be 2WD. The Tundra is a four-wheel-drive truck, not an AWD
  model, so these stay in `fourWheelDriveOnly`. The guide has no "AWD" lines. Automatic only; nothing is
  marked for a gearbox. No hybrid.
- The layout is the 2017–2024 4Runner's, and the list follows that one's wording. Differences from the
  4Runner: the rear differential oil under towing is every 30,000, not 15,000.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles
  if 0W-20 wasn't used at the last change, the note on the cabin air filter in heavy traffic or dust, the
  daily checks for off-road driving, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval under any
  special operating condition), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark
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
- Not covered by this list: other years (their own lists); FFV trucks:
  their own guide's list, [toyota-tundra-2017-5.7-ffv](toyota-tundra-2017-5.7-ffv.md)
  (the owner's ruling of 2026-10-23, #33); 2022 on (a new
  generation).
