# Toyota Camry, 2016 (2.5L 4-cylinder)

How `app/src/main/assets/known-schedules/toyota-camry-2016-4-cylinder.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2016 Camry Warranty and Maintenance Guide, https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-16Camry/pdf/T-MMS-16Camry.pdf, found through Toyota's owners-site
list of guides for the 2016 Camry. Read on 2026-10-23.

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
  (`fluid-check`), wiper blades (`wipers-check`), engine coolant (`cooling-check`), brakes
  (`brake-inspect`), drive belts (`belt-check`), front differential oil (`front-diff-check`).
- The guide's "or 120 months" for the first coolant change isn't used: it counts from the day the vehicle
  went into service, which Glovebox doesn't know.
- Brake fluid has no replacement in the log, so it has no reminder from this list.
- The guide prints no statement that the log repeats after 120,000 miles, so the table doesn't (rule 9).
- **One log for both petrol engines, written as two lists.** The guide has a single maintenance log for the
  Camry; no line in it is marked for an engine (`2GR-FE`, the V6, is named only on the warranty pages). So
  every line is the 2.5L four-cylinder's and the 3.5L V6's alike, and the 2016 log is written twice,
  identically: `toyota-camry-2016-4-cylinder` and `toyota-camry-2016-v6`. They are separate lists only so
  that the four-cylinder one can be marked `"powertrain": "gas"`: the Camry Hybrid also has a 2.5L engine,
  NHTSA's decoder gives it the same model (`Camry`) and Gasoline as its primary fuel, and a list for every
  engine would be offered to it. No 2016 Camry Hybrid guide was found (see below), so the 2016 four-cylinder list is marked `gas` to keep it off a 2016 hybrid: a hybrid gets the typical list, and a 2.5L Camry with no powertrain on record is asked which engine it has.
- The 2017 guide (https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-17Camry/pdf/T-MMS-17Camry.pdf) was compared with it: a script's count of each job on each page, and
  every difference looked at on the page. It can't share this list: 2017 replaces the cabin air filter every
  10,000 / 12 (2016: every 30,000 / 36), and has no front differential oil lines. 2016 prints "Steering gear
  box" where 2017 prints "Steering gear"; both are "Inspect steering gear" here.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the guide's interval under any of
  its four conditions), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark plugs
  120,000 / 144, and engine coolant first at 100,000 miles and then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid and front differential oil every 60,000 / 72
  when towing or carrying heavy loads. Under ordinary driving the guide only inspects the differential oil
  (every 30,000), and the transmission only for leaks.
- **The front differential oil lines are as printed.** The 2016 log inspects "Front differential oil" at
  30,000, 60,000, 90,000 and 120,000, and replaces it when towing at 60,000 and 120,000, on a front-wheel-drive
  car, with no drivetrain mark; the 2017 log has neither line. They are kept, unmarked, as the guide prints
  them (`front-diff-check`, `front-diff`), and left for the owner to check against their own guide.
- Also left out: a note on the 15,000-mile pages that heavy traffic, dusty roads or desert areas may
  shorten the cabin air filter's life (advice, not a job at a milestone).
- NHTSA's model list for Toyota (`GetModelsForMakeYear`, 2016 and 2017, checked 2026-10-23) has only
  `Camry`, with no separate hybrid spelling, so the list has no `modelAliases`.
- Not covered: 2012–2015 (Toyota lists guides for them, not read), the 2016 Camry Hybrid (no guide found: Toyota's owners-site list for 2016 has only the petrol guide, and the hybrid guide's file names that were tried return 403), and 2018 on (a new generation, with lists of its own).
