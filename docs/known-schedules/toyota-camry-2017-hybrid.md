# Toyota Camry Hybrid, 2017

How `app/src/main/assets/known-schedules/toyota-camry-2017-hybrid.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2017 Camry Hybrid Warranty and Maintenance Guide, https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-17CamryHV/pdf/T-MMS-17CamryHV.pdf. It isn't in Toyota's owners-site
list of guides (which lists only the petrol Camry's); it was found by its file name. Read on 2026-10-23.

- The guide's maintenance log (24 milestones, 5,000 miles or 6 months apart, to 120,000 miles or 144 months)
  was read off the pages, by eye, and the list written from it. Every milestone has the same four special
  operating conditions: dirt or dusty roads; towing, a car-top carrier or heavy loads; repeated short trips
  below 32°F; long idling or low-speed driving. Each job under them is in the table, named with its
  condition.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles
  if 0W-20 wasn't used at the last change, the oil-reminder reset footnote, the dealer's service stamp and
  the explanations of each item.
- No line is marked for a drivetrain or a gearbox: every Camry of these years is front-wheel drive with an
  automatic.
- Inspections are in the table only (rule 7). Those with a check line carry it: fluid levels
  (`fluid-check`), wiper blades (`wipers-check`), engine and inverter coolant (`cooling-check`), brakes
  (`brake-inspect`).
- The guide's "or 120 months" for the first coolant change isn't used: it counts from the day the vehicle
  went into service, which Glovebox doesn't know.
- Brake fluid has no replacement in the log, so it has no reminder from this list.
- The guide prints no statement that the log repeats after 120,000 miles, so the table doesn't (rule 9).
- **Told apart from the petrol Camry by `"powertrain": "hybrid"`.** NHTSA's decoder gives a Camry Hybrid
  the same model (`Camry`), a 2.5L engine and Gasoline as its primary fuel, so its size alone matches the
  2.5L four-cylinder's list ([`toyota-camry-2017-4-cylinder`](toyota-camry-2017-4-cylinder.md), marked
  `gas`). A hybrid is matched only where its powertrain is on record; otherwise setup asks which engine it
  has.
- Compared with the 2017 petrol Camry's guide, page by page (a script's count of each job, every
  difference looked at): the same jobs at the same milestones, cabin air filter every 10,000 / 12 and no
  front differential lines, except:
  - there is no drive belt inspection;
  - the coolant inspection is "Engine/Inverter coolant", written here "Inspect engine and inverter coolant";
  - the 100,000-mile line is "Replace engine/inverter coolant";
  - the radiator line adds "and/or intercooler"; it is "Inspect radiator and condenser" here, as on the
    petrol list, since the Camry Hybrid has no intercooler line of its own.
- **The 100,000-mile coolant line is "Replace engine coolant" (`coolant`).** The guide's footnote says the
  engine coolant is first replaced at 100,000 miles and the inverter coolant first at 150,000 miles (180
  months), both then every 50,000 / 60. So at 100,000 only the engine coolant is due.
- **The inverter coolant is an `additional` job.** Its interval is given only in that footnote, and its
  first time (150,000) is past the table's end, so it can't be the table's own interval (rule 7) and is
  never placed on the table. Under the owner's ruling of 2026-10-23 (#33) it is an `additional` item,
  "Replace inverter coolant" (`inverter-coolant`), first at 150,000 miles and then every 50,000 / 60. The
  footnote's "180 months" for the first change isn't used, for the same reason as the coolant's 120.
- There is no hybrid battery cooling filter job in this log, so none is in the list.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the guide's interval under any of its
  four conditions), tire rotation 5,000 / 6, cabin air filter 10,000 / 12, engine air filter 30,000 / 36,
  spark plugs 120,000 / 144, engine coolant first at 100,000 miles and then every 50,000 / 60, and
  (`additional`) inverter coolant first at 150,000 miles and then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or carrying heavy
  loads.
- NHTSA's model list for Toyota (`GetModelsForMakeYear`, 2017, checked 2026-10-23) has only `Camry`, with
  no hybrid spelling, so the list has no `modelAliases`.
- Not covered: the 2016 Camry Hybrid. No guide was found: Toyota's owners-site list for 2016 has only the
  petrol guide, the `camryhybrid`, `camry-hybrid` and `camryhv` lists are empty, and `T-MMS-16CamryHV` and
  the other file names tried return 403. 2018 on is a new generation, with lists of its own.
