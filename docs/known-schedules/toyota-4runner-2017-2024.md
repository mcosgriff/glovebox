# Toyota 4Runner, 2017–2024

How `app/src/main/assets/known-schedules/toyota-4runner-2017-2024.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>4Runner/pdf/T-MMS-<yy>4Runner.pdf`
(`17` to `24`). Read on 2026-10-13.

- The 2020 guide's maintenance log (pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2017, 2018, 2019, 2021, 2022, 2023 and 2024 were compared page by page. The jobs and where they fall
  are the same in all eight. What differs is wording: the 2023 and 2024 guides say "Radiator, condenser
  and/or intercooler"; the 2024 guide's oil note says "recommended oil" where the others say "0W-20";
  2017 has no footnote on fluid levels.
- These years have one engine. Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the
  list and the 4WD-only jobs say `(4WD)` and are left out for a 2WD truck. The guide has no "AWD" lines:
  Toyota calls the Limited's system full-time 4WD, and NHTSA's decoder gives a Limited as
  `4WD/4-Wheel Drive/4x4` or `4x2` (checked 2026-10-13 with the 2022 patterns `JTEKU5JR*N` and
  `JTEDU5JR*N`), never as AWD.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000
  miles if 0W-20 wasn't used at the last change, the daily checks for off-road driving, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any special operating condition), tire rotation 5,000 / 6, propeller shaft lubrication
  15,000 / 18, cabin and engine air filters 30,000 / 36, spark plugs 120,000 / 144, and engine coolant
  first at 100,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the first coolant
  change isn't used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Inspections that are reminders too (asked for by the owner, 2026-10-13): front and rear differential
  oil every 15,000 / 18, transfer case oil every 30,000 / 36. Logging a change of one counts as its
  check. The other inspections are in the table only.
- Only for severe use (#136): front and rear differential oil every 15,000 / 18 and automatic
  transmission fluid every 60,000 / 72 when towing or carrying heavy loads; transfer case oil every
  30,000 / 36 on dusty roads or when towing. Under ordinary driving the guide only inspects these.
- Shorter under severe use: engine oil 5,000 / 6 (any of the guide's four conditions), propeller shaft
  lubrication 5,000 / 6 (dust, salted roads or towing).
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2010 to 2016, whose guides are on `assets.sipb.toyota.com` under their own file names (2011's
  is at the address above) and whose logs differ from this one; they have lists of their own
  ([2015–2016](toyota-4runner-2015-2016.md), which lacks only this list's cabin-filter inspection at 15,000,
  45,000, 75,000 and 105,000; [2014](toyota-4runner-2014.md); [2013](toyota-4runner-2013.md);
  [2010–2012 V6](toyota-4runner-2010-2012-v6.md); [2010](toyota-4runner-2010-4cyl.md) and
  [2011](toyota-4runner-2011-4cyl.md) 4-cylinder). 2025 on: a new generation, with a chart in place of the log.
