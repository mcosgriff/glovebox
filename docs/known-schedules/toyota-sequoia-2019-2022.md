# Toyota Sequoia, 2019–2022

How `app/src/main/assets/known-schedules/toyota-sequoia-2019-2022.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Issue #202.

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>Sequoia/pdf/T-MMS-<yy>Sequoia.pdf`
(`19` to `22`), as listed by Toyota's owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/sequoia/<year>`).
Read on 2026-10-23.

- The 2020 guide's maintenance log (PDF pages 40 to 55, printed 38 to 53: 24 milestones, 5,000 miles or
  6 months apart, to 120,000 miles / 144 months) was read in full and the list written from it.
- 2019, 2021 and 2022 were compared with it page by page (log on PDF pages 39 to 54 in each), and every
  difference a script found was looked at on the page. The jobs and where they fall are the same in all
  four. What differs is only layout: the cabin air filter note on the 15,000s wraps differently in 2021
  and 2022, and 2022 sets the dealer-verification lines in small capitals.
- The log is the 4Runner's, with these differences: there is **no propeller shaft lubrication** in these
  years; the drive shaft boots are **not** marked "(4WD)"; and when towing, the rear differential oil is
  replaced only at the 30,000s (the front at every 15,000).
- One engine, the 5.7 L V8, and one schedule, so the list has no `engine`. Automatic only; nothing is
  marked for a gearbox. No hybrid.
- Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the list. The Sequoia is a
  4WD / 2WD truck, so the guide's "(4WD)" lines stay `fourWheelDriveOnly`, named "(4WD)": front
  differential oil (inspect, and replace when towing) and transfer case oil (inspect, and replace on
  dusty roads or when towing). They are left out for a truck known to be two-wheel drive. The guide has
  no AWD lines.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced if 0W-20
  wasn't used at the last change, the note on the cabin air filter in heavy traffic or dust, the daily
  checks for off-road driving, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any special operating condition), tire rotation 5,000 / 6, cabin and engine air filters
  30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts from the
  day the vehicle went into service, which Glovebox doesn't know.
- Inspections that are reminders too, as on the 4Runner: front and rear differential oil every
  15,000 / 18, transfer case oil every 30,000 / 36. Logging a change of one counts as its check. The
  other inspections are in the table only (the drive belts are first inspected at 60,000 and then every
  15,000).
- Only for severe use (#136): front differential oil every 15,000 / 18, rear differential oil every
  30,000 / 36 and automatic transmission fluid every 60,000 / 72 when towing or carrying heavy loads;
  transfer case oil every 30,000 / 36 on dusty roads or when towing. Under ordinary driving the guide
  only inspects these.
- Table only, with no catalog line: re-torquing the propeller shaft bolt (every 15,000, and at the
  milestones between on dusty or salted roads or when towing) and tightening the chassis and body nuts
  and bolts (dusty roads or towing), as on the 4Runner.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered:
  - **2016, 2017 and 2018** (guides found, not written). Each of those years has a second guide for
    the flexible-fuel (FFV) Sequoia, with a different log: 2,500 miles apart, oil every 5,000, and
    oil at the odd 2,500s for a truck run mostly on E85. Both versions have the 5.7 L engine, and
    Glovebox can't yet tell an FFV truck from a gasoline one, so a list for those years would be
    offered to an FFV truck with the wrong oil interval. They wait until it can. (The gasoline guides
    also differ from 2019–2022, by a count of job lines: 2016 and 2017 have a propeller shaft
    lubrication, and 2017 inspects the transmission cooler hoses. 2018's gasoline guide matched by that
    count but was not compared page by page.)
  - 2019–2022 FFV: Toyota's owners site lists no FFV guide for these years; that is all that was
    checked.
  - 2023 on: a new generation, not looked at here.
