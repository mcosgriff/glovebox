# Toyota Highlander Hybrid, 2016

How `app/src/main/assets/known-schedules/toyota-highlander-2016-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2016 Highlander Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-16HighlanderHV/pdf/T-MMS-16HighlanderHV.pdf`.
Read on 2026-10-23.

- The 2016 log (PDF pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles) was
  compared page by page, by script, with the 2017 and 2018 logs (see the
  [2017 list](toyota-highlander-2017-hybrid.md)), and every difference was looked at on the page (the
  5,000/10,000, 15,000, 30,000, 60,000, 95,000/100,000 and 120,000 pages read as images). What differs from
  2017:
  - front differential oil is inspected at every 30,000 and replaced at 60,000 and 120,000 when towing or
    carrying heavy loads;
  - spark plugs are replaced only at 120,000;
  - the cabin air filter isn't in the inspection list (the 15,000-mile pages keep the note about it);
  - "Steering gear box" for "Steering gear", and footnote wording.
  Like 2017, the cabin air filter is replaced every 30,000, and there is no HV battery cooling intake filter
  or EFI tank additive line.
- **Hybrid, not gas.** The hybrid's engine is a 3.5L like the gasoline V6
  ([2016 3.5L V6](toyota-highlander-2016-3.5.md)), and vPIC names both "Highlander", so this list is marked
  `"powertrain": "hybrid"`. A Highlander whose powertrain isn't on record is asked which engine it has.
- **No drivetrain marks.** The hybrid guide marks no line "(4WD)" or "(AWD)", the front differential lines
  included; nothing on this list is held back from any drivetrain.
- **Engine and inverter coolant.** The 100,000-mile line is "Replace engine/inverter coolant". Its footnote
  puts the engine coolant's first change at 100,000 miles or 120 months and the inverter coolant's at
  150,000 miles or "230 months" (the 2017 to 2019 guides print 180, so 230 looks like a misprint); nothing on
  this list uses that figure. The line is named "Replace engine coolant" (`coolant`). The inverter coolant's
  first change falls after the log's end and is given only in the footnote (then every 50,000 miles / 60
  months), so it is a reminder apart from the table (`additional`, by the owner's ruling of 2026-10-23,
  #33): "Replace inverter coolant" (`inverter-coolant`), first at 150,000 miles and then every 50,000 / 60,
  never placed on the table. Its months for the first change aren't used, as with the engine coolant's.
- Left out, because they aren't jobs at a milestone: the 0W-20 oil note, the cabin filter note, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000 / 6, cabin and
  engine air filters 30,000 / 36, spark plugs 120,000 / 144, engine coolant first at 100,000 and then every
  50,000 / 60 (the guide's "or 120 months" counts from the in-service day, which Glovebox doesn't know).
  Apart from the table: inverter coolant first at 150,000 and then every 50,000 / 60.
- Inspections that are reminders too (the owner's ruling of 2026-10-23, #33: any vehicle with a differential
  needs it inspected, as asked for the 4Runner on 2026-10-13): front and rear differential oil every
  30,000 / 36. Logging a change of one counts as its check.
- Only for severe use: front and rear differential oil and automatic transmission fluid every 60,000 / 72,
  all when towing or carrying heavy loads.
- Table only: the drive shaft bolt re-torque, every other inspection and drive belts (first at 60,000, then
  every 15,000).
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
