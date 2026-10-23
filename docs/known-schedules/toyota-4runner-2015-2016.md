# Toyota 4Runner, 2015–2016

How `app/src/main/assets/known-schedules/toyota-4runner-2015-2016.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year. Read on 2026-10-23.

- 2015: `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-154Runner/pdf/2015_Toyota_4Runner_WMG.pdf`
- 2016: `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-164Runner/pdf/2016_Toyota%204Runner_WMG_lr.pdf`

What was read:

- The 2015 guide's maintenance log (PDF pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to 120,000
  miles) was read in full and the list written from it.
- The 2016 log (PDF pages 40 to 55) was compared page by page and is the same, word for word.
- Against 2014 ([2014](toyota-4runner-2014.md)), the automatic transmission is inspected "for signs of leakage" at
  30,000, 60,000, 90,000 and 120,000, where 2014 inspects its fluid. Nothing else changes.
- **Against 2017–2024** ([toyota-4runner-2017-2024.md](toyota-4runner-2017-2024.md)), 2015 and 2016 have no
  cabin-filter line at 15,000, 45,000, 75,000 and 105,000, where 2017 inspects it. That is the only difference in
  the jobs (the rest is wording: "Steering gear box" for "Steering gear", and the footnotes), but a list covers
  only years whose guides agree, so these two years are a list of their own and the 2017–2024 list is unchanged.

Engine, drivetrain and gearbox:

- The guide has one log, with no schedule letter or cylinder count; the 4Runner had only the V6. So the list has
  no `engine` and is for every 2015 and 2016 4Runner.
- Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the list; the 4WD-only jobs say `(4WD)` and
  are left out for a 2WD truck. The guide has no "AWD" lines.
- No line is for one gearbox only.

The rest, as on the 2017–2024 page:

- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles if
  0W-20 wasn't used at the last change, the note on the cabin filter in dusty or heavy traffic, the towing
  footnote, and the explanations of each item.
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

Not covered: every other year has its own list.
