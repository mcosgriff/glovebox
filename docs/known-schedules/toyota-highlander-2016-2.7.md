# Toyota Highlander 2.7L, 2016

How `app/src/main/assets/known-schedules/toyota-highlander-2016-2.7.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for 2016,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-16Highlander/pdf/T-MMS-16Highlander.pdf`.
Read on 2026-10-23.

- The 2016 log (PDF pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles) was
  compared page by page, by script, with the 2017 log (see the [2017 list](toyota-highlander-2017-2.7.md)),
  and every difference was looked at on the page. What differs from 2017:
  - front differential oil is inspected at every 30,000 and replaced at 60,000 and 120,000 when towing or
    carrying heavy loads. These lines carry no drivetrain mark, so they are for every Highlander;
  - spark plugs are replaced only at 120,000 (2017 adds 60,000 for the V6);
  - there is no brake booster vacuum pump line;
  - automatic transmission fluid is replaced at 60,000 and 120,000 only when towing, not also under
    extensive idling;
  - the cabin air filter isn't in the inspection list (the 15,000-mile pages keep the note about it);
  - "Steering gear box" for "Steering gear", and spelling.
- **A list for the 2.7L only.** The guide covers the 2.7L four-cylinder (1AR-FE) and the 3.5L V6 (2GR-FE)
  in one log, and no line in it names an engine, so every line is the 2.7L's. It is written as the 2.7L's
  list, not as one for every engine, because the Highlander Hybrid is a 3.5L as well, with a guide of its
  own: a list for every engine would be offered to a hybrid. The same lines serve the 3.5L V6, in
  [toyota-highlander-2016-3.5](toyota-highlander-2016-3.5.md), told apart from
  [toyota-highlander-2016-hybrid](toyota-highlander-2016-hybrid.md) by powertrain.
- **"(AWD)"**: the guide prints "(4WD)" on the propeller shaft bolt, rear differential oil and transfer
  case oil lines. Toyota sells the Highlander as AWD, so they are named "(AWD)" and kept in
  `allWheelDriveOnly` (the owner's ruling); a Highlander known to be front-wheel drive doesn't get them.
- Left out, because they aren't jobs at a milestone: the 0W-20 oil note, the cabin filter note, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000 / 6, cabin and
  engine air filters 30,000 / 36, spark plugs 120,000 / 144, engine coolant first at 100,000 and then every
  50,000 / 60 (the guide's "or 120 months" counts from the in-service day, which Glovebox doesn't know).
- Only for severe use: rear differential oil (AWD) and transfer case oil (AWD) every 15,000 / 18, and front
  differential oil and automatic transmission fluid every 60,000 / 72, all when towing or carrying heavy
  loads.
- Table only: the propeller shaft bolt re-torque, every inspection (the differential and transfer case
  ones carry service lines but aren't reminders) and drive belts (first at 60,000, then every 15,000).
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
