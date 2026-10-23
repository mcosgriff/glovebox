# Toyota Tundra, 2020–2021

How `app/src/main/assets/known-schedules/toyota-tundra-2020-2021.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, as listed on Toyota's owners site
(`https://www.toyota.com/service/tcom/cs/manuals/downloads/tundra/<year>`), at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>Tundra/pdf/T-MMS-<yy>Tundra.pdf`
(`20` and `21`). Downloaded on 2026-10-22 and read on 2026-10-23.

- The 2020 guide's maintenance log (PDF pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2021 was compared page by page. The jobs and where they fall are the same. What differs is wording
  in the notes (the cabin air filter note, spacing in a footnote) and the order of lines within a box.
- The log has no per-engine lines, so the list has no `engine` and covers every engine of these years.
  Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the list and the 4WD-only jobs say
  `(4WD)` and are left out for a truck known to be 2WD. The Tundra is a four-wheel-drive truck, not an
  AWD model, so these stay in `fourWheelDriveOnly`. The guide has no "AWD" lines.
- The layout is the 2017–2024 4Runner's, and the list follows that one's wording. Differences from the
  4Runner: the Tundra's log has no propeller shaft lubrication, only the bolt re-torque (every 15,000,
  and under dust, salted roads or towing at every other milestone); the rear differential oil under
  towing is every 30,000, not 15,000.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000
  miles if 0W-20 wasn't used at the last change, the daily checks for off-road driving, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any special operating condition), tire rotation 5,000 / 6, cabin and engine air filters
  30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts from the
  day the vehicle went into service, which Glovebox doesn't know.
- Inspections that are reminders too, as on the 4Runner (asked for by the owner, 2026-10-13): front and
  rear differential oil every 15,000 / 18, transfer case oil every 30,000 / 36. Logging a change of one
  counts as its check. The other inspections are in the table only.
- The propeller shaft bolt re-torque and the chassis and body nuts and bolts have no service line, so
  they are in the table only and remind of nothing, as on the 4Runner.
- Only for severe use (#136): front differential oil every 15,000 / 18, rear differential oil every
  30,000 / 36 and automatic transmission fluid every 60,000 / 72 when towing or carrying heavy loads;
  transfer case oil every 30,000 / 36 on dusty roads or when towing. Under ordinary driving the guide
  only inspects these.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Other years: 2016–2019 gasoline trucks have lists of their own, one per engine with `"powertrain":
  "gas"` (e.g. [toyota-tundra-2018-2019-5.7](toyota-tundra-2018-2019-5.7.md)), since flex-fuel (FFV) trucks
  of those years had a guide of their own; no FFV list is shipped yet. Toyota's owners site lists no FFV
  guide for 2020 or 2021, so this list has no engine block. 2022 on has lists of its own.
