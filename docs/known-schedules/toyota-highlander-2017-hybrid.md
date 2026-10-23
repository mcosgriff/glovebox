# Toyota Highlander Hybrid, 2017

How `app/src/main/assets/known-schedules/toyota-highlander-2017-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2017 Highlander Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-17HighlanderHV/pdf/T-MMS-17HighlanderHV.pdf`.
Read on 2026-10-23.

- The 2018 guide's log was read in full for the [2018–2019 list](toyota-highlander-2018-2019-hybrid.md),
  and the 2017 log (PDF pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles)
  compared with it page by page, by script, with every difference looked at on the page (the 5,000/10,000,
  15,000, 30,000, 60,000, 95,000/100,000 and 120,000 pages read as images). What differs:
  - the cabin air filter is replaced every 30,000 miles, not every 10,000, and it is inspected at 15,000,
    45,000, 75,000 and 105,000, beside a note that dusty or heavy-traffic driving may shorten its life;
  - there is no HV battery cooling intake filter line;
  - there is no EFI tank additive line;
  - footnote wording.
- **Hybrid, not gas.** The hybrid's engine is a 3.5L like the gasoline V6
  ([2017 3.5L V6](toyota-highlander-2017-3.5.md)), and vPIC names both "Highlander", so this list is marked
  `"powertrain": "hybrid"`. A Highlander whose powertrain isn't on record is asked which engine it has.
- **No drivetrain marks.** The hybrid guide marks no line "(4WD)" or "(AWD)"; nothing on this list is held
  back from any drivetrain.
- **Engine and inverter coolant.** The 100,000-mile line is "Replace engine/inverter coolant"; its footnote
  puts the inverter coolant's first change at 150,000 miles or 180 months, then every 50,000 / 60. The line
  is named "Replace engine coolant" (`coolant`). The inverter coolant's first change falls after the log's
  end and is given only in the footnote, so it is a reminder apart from the table (`additional`, by the
  owner's ruling of 2026-10-23, #33): "Replace inverter coolant" (`inverter-coolant`), first at 150,000
  miles and then every 50,000 / 60, never placed on the table. The "180 months" isn't used, for the same
  reason as the engine coolant's "120 months" below.
- Left out, because they aren't jobs at a milestone: the 0W-20 oil note, the cabin filter note, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000 / 6, cabin and
  engine air filters 30,000 / 36, spark plugs 60,000 / 72, engine coolant first at 100,000 and then every
  50,000 / 60 (the guide's "or 120 months" counts from the in-service day, which Glovebox doesn't know).
  Apart from the table: inverter coolant first at 150,000 and then every 50,000 / 60.
- Inspections that are reminders too (the owner's ruling of 2026-10-23, #33: any vehicle with a differential
  needs it inspected, as asked for the 4Runner on 2026-10-13): rear differential oil every 30,000 / 36.
  Logging a change of it counts as its check.
- Only for severe use: rear differential oil and automatic transmission fluid every 60,000 / 72 when towing
  or carrying heavy loads.
- Table only: the drive shaft bolt re-torque, every other inspection and drive belts (first at 60,000, then
  every 15,000).
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
