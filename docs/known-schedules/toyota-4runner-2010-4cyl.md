# Toyota 4Runner 4-cylinder, 2010

How `app/src/main/assets/known-schedules/toyota-4runner-2010-4cyl.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2010 4Runner,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-104Runner/pdf/2010_4Runner_WMG_0W20_lr.pdf`.
Read on 2026-10-23.

- The guide's Schedule A, "4 Cylinder" (PDF pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles), was read in full and the list written from it.
- **The 2010 guide online is a 0W-20 printing** (its file name ends `_0W20`). The guide that came with an early
  2010 truck may have been an earlier printing; only this one was found. Its Schedule A changes the oil every
  5,000 miles either way.
- The 2011 guide's Schedule A was compared page by page. It has none of this schedule's 4WD lines, so 2011 has a
  list of its own ([toyota-4runner-2011-4cyl.md](toyota-4runner-2011-4cyl.md)); the 2011 guide doesn't say why.
  Otherwise the two are the same, apart from the wording of the oil-reminder footnote.

Engine, drivetrain and gearbox:

- **One list per engine.** The guide has two schedules, A for the 4-cylinder and B for the 6-cylinder (the
  [2010–2012 V6 list](toyota-4runner-2010-2012-v6.md)). The schedules print no size; the guide's warranty pages
  name the engines 2TR-FE and 1GR-FE, which are 2.7 L and 4.0 L, so this list's `engine` is 2.7 L. A truck with
  no engine on record is asked which it has.
- Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the list; the 4WD-only jobs say `(4WD)` and
  are left out for a 2WD truck. The guide has no "AWD" lines.
- No line is for one gearbox only.

How Schedule A differs from Schedule B, the V6's:

- The oil and filter are replaced every 5,000 miles / 6 months, not 10,000 / 12, and there is no shorter interval
  for severe use.
- Its special operating conditions are only two, dusty or dirt roads and towing or heavy loads. The V6's short
  trips below freezing and long idling, both for the oil, aren't in it.
- Everything else is the same, line for line: the cabin filter is cleaned at 15,000, 45,000, 75,000 and
  105,000 (no `service`: the catalog's cabin-air line is a replacement), the automatic transmission's fluid is
  inspected, and there is no wiper-blade inspection.

The rest:

- Left out, because they aren't jobs at a milestone: the note on the cabin filter in dusty or heavy traffic and
  the explanations of each item.
- Reminders: engine oil 5,000 mi / 6 mo, tire rotation 5,000 / 6, propeller shaft lubrication 15,000 / 18 (5,000
  / 6 on dusty or salted roads or towing), cabin and engine air filters 30,000 / 36, spark plugs 120,000 / 144, and
  engine coolant first at 100,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the first
  coolant change isn't used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Inspections that are reminders too: front and rear differential oil every 15,000 / 18, transfer case oil every
  30,000 / 36. The other inspections, drive belts (first at 60,000, then every 15,000) among them, are in the
  table only.
- Only for severe use: front and rear differential oil every 15,000 / 18 and automatic transmission fluid every
  60,000 / 72 when towing or carrying heavy loads; transfer case oil every 30,000 / 36 on dusty roads or when
  towing.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.

Not covered: other years' 4-cylinder (2011 has its own list; the 2012 to 2016 guides have no 4-cylinder schedule).
