# Toyota Highlander Hybrid, 2018–2019

How `app/src/main/assets/known-schedules/toyota-highlander-2018-2019-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the Highlander Hybrid in each model year, which Toyota
publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>HighlanderHV/pdf/T-MMS-<yy>HighlanderHV.pdf`
(`18` and `19`). Read on 2026-10-23. Toyota's owners site doesn't list these guides; the gasoline
Highlander's guide is a different one, with a list per engine
([3.5L V6](toyota-highlander-2018-2019-3.5.md)).

- The 2018 guide's maintenance log (PDF pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full, as images, and the list written from it.
- 2019 (PDF pages 40 to 55, set in a different typeface) was compared page by page with a script that
  counts each job on each page. The counts differ only on the 105,000 page, which was read as an image: the
  difference is the coolant footnote's wording, and the jobs are the same.
- **Hybrid, not gas.** The hybrid's engine is a 3.5L like the gasoline V6, and vPIC names both
  "Highlander" in every year covered (`GetModelsForMakeYear`, Toyota 2016 to 2019), so this list is marked
  `"powertrain": "hybrid"`. A Highlander whose powertrain isn't on record is asked which engine it has.
- **No drivetrain marks.** The hybrid guide marks no line "(4WD)" or "(AWD)" and has no propeller shaft or
  transfer case lines; it says "drive shaft bolt". Nothing on this list is held back from any drivetrain.
- **Engine and inverter coolant.** The 100,000-mile line is "Replace engine/inverter coolant", with a
  footnote: the engine coolant is first replaced at 100,000 miles or 120 months, the inverter coolant at
  150,000 miles or 180 months, each every 50,000 miles or 60 months after. So the 100,000-mile line is the
  engine coolant's, and is named "Replace engine coolant" (`coolant`). The inverter coolant's first change
  falls after the log's end and is given only in that footnote, not in the table, so it is a reminder
  apart from the table (`additional`, by the owner's ruling of 2026-10-23, #33): "Replace inverter coolant"
  (`inverter-coolant`), first at 150,000 miles and then every 50,000 / 60, never placed on the table. The
  "180 months" isn't used, for the same reason as the engine coolant's "120 months" below. The inspections
  are named "Inspect engine and inverter coolant", as the guide lists them.
- Left out: the EFI tank additive at every milestone, which is for Hawaii and Puerto Rico only; Glovebox
  has no way to say a job is for one region. Also left out, because they aren't jobs at a milestone: the
  note that the oil is to be replaced at 5,000 miles if 0W-20 wasn't used at the last change, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the guide's interval under any of
  its four special operating conditions), tire rotation 5,000 / 6, cabin air filter 10,000 / 12, engine
  air filter 30,000 / 36, spark plugs 60,000 / 72, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts from the day
  the vehicle went into service, which Glovebox doesn't know. Apart from the table: inverter coolant first
  at 150,000 miles and then every 50,000 / 60.
- Inspections that are reminders too (the owner's ruling of 2026-10-23, #33: any vehicle with a differential
  needs it inspected, as asked for the 4Runner on 2026-10-13): rear differential oil every 30,000 / 36.
  Logging a change of it counts as its check.
- Only for severe use: cleaning the HV battery's cooling intake filter every 5,000 / 6 under extensive
  idling or low-speed driving (`hybrid-battery-filter`); rear differential oil and automatic transmission
  fluid every 60,000 / 72 when towing or carrying heavy loads. Under ordinary driving the guide only
  inspects the rear differential, at every 30,000.
- Table only, with no reminder: the drive shaft bolt re-torque (at every 15,000, and every 5,000 on dusty
  roads; no service line fits, as with the gasoline model's propeller shaft bolt), every other inspection,
  and drive belts, which are inspected first at 60,000 and then every 15,000.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered here: 2016 and 2017 have lists of their own (their logs differ). A 2015 hybrid guide was
  found but not read, and no 2014 hybrid guide was found.
