# Toyota 4Runner V6, 2010–2012

How `app/src/main/assets/known-schedules/toyota-4runner-2010-2012-v6.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year. Read on 2026-10-23.

- 2010: `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-104Runner/pdf/2010_4Runner_WMG_0W20_lr.pdf`
- 2011: `https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-114Runner/pdf/T-MMS-114Runner.pdf`
- 2012: `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-124Runner/pdf/2012_Toyota_%204Runner_WMG.pdf`

What was read:

- The 2011 guide's Schedule B, "6 Cylinder" (PDF pages 56 to 71: 24 milestones, 5,000 miles or 6 months
  apart, to 120,000 miles), was read in full and the list written from it.
- The 2010 guide's Schedule B and the 2012 guide's log (PDF pages 40 to 55) were compared with it page by page.
  The jobs and where they fall are the same in all three. What differs is wording: the 2010 oil-reminder footnote
  says "after engine oil replacement" where 2011 says "every 5,000 miles", and one 2010 towing heading (25,000
  miles) leaves out "car-top carrier, or heavy vehicle loading". The 2012 log has no schedule letter or cylinder
  count, since by then the 4Runner had only the V6.
- **The 2010 guide online is a 0W-20 printing** (its file name ends `_0W20`; it gives the oil change every
  10,000 miles with 0W-20, as 2011 does). The guide that came with an early 2010 truck may have been an earlier
  printing; only this one was found.
- **The 2012 guide online is a reissue**: the PDF was made in January 2016. Whether it matches the booklet
  printed for 2012 can't be told from it.

Engine, drivetrain and gearbox:

- **One list per engine.** The 2010 and 2011 guides have two schedules, A for the 4-cylinder and B for the
  6-cylinder ([2010](toyota-4runner-2010-4cyl.md) and [2011](toyota-4runner-2011-4cyl.md) 4-cylinder lists).
  Schedule B is the same in 2010 and 2011 and the same as the 2012 log, so one list covers the V6 for the three
  years. The schedules print no size; the guides' warranty pages name the engines 2TR-FE and 1GR-FE, which are
  2.7 L and 4.0 L, so this list's `engine` is 4.0 L. A 2012 truck with no engine on record is asked which engine
  it has, and offered only this one (and "Neither, or I'm not sure").
- Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the list; the 4WD-only jobs say `(4WD)` and
  are left out for a 2WD truck. The guide has no "AWD" lines.
- No line is for one gearbox only.

Against the 2017–2024 list ([toyota-4runner-2017-2024.md](toyota-4runner-2017-2024.md)) the jobs that get
logged, the reminders and the special-condition jobs are the same. The table differs in three inspections:

- "Clean cabin air filter" is a ticked job at 15,000, 45,000, 75,000 and 105,000, where 2017 inspects it. It has
  no `service`: the catalog's cabin-air line is a replacement, and cleaning it isn't one.
- The automatic transmission's inspection is of its fluid, not for leaks.
- The guide has no wiper-blade inspection.

The rest, as on the 2017–2024 page:

- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles if
  0W-20 wasn't used at the last change, the note on the cabin filter in dusty or heavy traffic, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the guide's interval under any of its four
  special operating conditions), tire rotation 5,000 / 6, propeller shaft lubrication 15,000 / 18 (5,000 / 6 on
  dusty or salted roads or towing), cabin and engine air filters 30,000 / 36, spark plugs 120,000 / 144, and
  engine coolant first at 100,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the first
  coolant change isn't used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Inspections that are reminders too: front and rear differential oil every 15,000 / 18, transfer case oil every
  30,000 / 36. The other inspections, drive belts (first at 60,000, then every 15,000) among them, are in the
  table only.
- Only for severe use: front and rear differential oil every 15,000 / 18 and automatic transmission fluid every
  60,000 / 72 when towing or carrying heavy loads; transfer case oil every 30,000 / 36 on dusty roads or when
  towing.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.

Not covered: 2013 on, whose logs differ (the [2013](toyota-4runner-2013.md), [2014](toyota-4runner-2014.md),
[2015–2016](toyota-4runner-2015-2016.md) and [2017–2024](toyota-4runner-2017-2024.md) lists).
