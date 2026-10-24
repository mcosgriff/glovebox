# Toyota RAV4 Hybrid, 2017

How `app/src/main/assets/known-schedules/toyota-rav4-2017-hybrid.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2017 RAV4 Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-17RAV4HV/pdf/T-MMS-17RAV4HV.pdf`. Read on
2026-10-23. (Toyota's owners site lists no guide under `rav4hybrid`; the hybrid guides are at this address.)

- The maintenance log was read in full: printed pages 38 to 53, 24 milestones, 5,000 miles or 6 months apart,
  to 120,000 miles or 144 months. Nothing says it repeats.
- **Hybrid and gasoline.** vPIC names both "RAV4" (checked for 2013–2018 on 2026-10-23) and both are 2.5 litres,
  so this list is `"powertrain": "hybrid"` and the gasoline lists are `"gas"`. A RAV4 with no powertrain on
  record is asked which it is.
- The guide marks no job for one drive, so nothing here is left out for any drivetrain. The rear differential
  oil is inspected every 30,000 and replaced every 60,000 when towing, as printed.
- **Inverter coolant is apart from the table.** The 100,000-mile box says "replace engine/inverter coolant",
  but its footnote puts the inverter coolant's first change at 150,000 miles (or 180 months) and every 50,000 /
  60 after, past the end of the table, while the engine coolant is first at 100,000. The 100,000 job is
  "Replace engine coolant" (`coolant`). The inverter coolant is printed only in the footnote, so it is an
  `additional` job ("Replace inverter coolant", `inverter-coolant`, first at 150,000 then every 50,000 / 60),
  never placed on the table (the owner's ruling of 2026-10-23, #33). The "180 months" isn't used: it counts
  from the in-service date, which Glovebox doesn't know.
- Left out, because they aren't jobs at a milestone: the 0W-20 oil note, the cabin air filter note and the
  footnotes.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 under any of the guide's four special conditions), tire
  rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark plugs 120,000 / 144, engine coolant first
  at 100,000 and then every 50,000 / 60 (the first change's "120 months" counts from the in-service date, which
  Glovebox doesn't know), rear differential oil inspection 30,000 / 36 (`rear-diff-check`). Apart from the
  table: inverter coolant first at 150,000 then every 50,000 / 60.
- **The rear differential oil inspection is a reminder**, at the table's own 30,000 / 36; logging a change
  counts as its check. By the owner's ruling of 2026-10-23 (#33) any vehicle with a differential needs it
  inspected, so the 4Runner's rule applies wherever a guide prints the inspection.
- Only for severe use (#136): rear differential oil and automatic transmission fluid every 60,000 / 72 when
  towing or carrying heavy loads.
- In the table only: the re-torque of the drive shaft bolt (every 15,000, and every 5,000 on dusty roads; there
  is no catalog line for it), the drive belt inspection (first at 60,000, then every 15,000), the engine oil
  cooler hose inspection and the other inspections. Inspections with a catalog line carry it; apart from the
  rear differential oil, none is a reminder.
- The guide says "radiator, condenser and/or intercooler"; the job keeps all three.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2016 and 2018, which differ (their own lists, [2016](toyota-rav4-2016-hybrid.md) and
  [2018](toyota-rav4-2018-hybrid.md)); 2019 on (a new generation).
