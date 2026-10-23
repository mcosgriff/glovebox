# Toyota Highlander 2.7L, 2017

How `app/src/main/assets/known-schedules/toyota-highlander-2017-2.7.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for 2017,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-17Highlander/pdf/T-MMS-17Highlander.pdf`.
Read on 2026-10-23.

- The 2018 guide's log was read in full for the [2018–2019 list](toyota-highlander-2018-2019-2.7.md), and
  the 2017 log (PDF pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles)
  compared with it page by page, by script, with every difference looked at on the page. What differs:
  - the cabin air filter is replaced every 30,000 miles, not every 10,000, and it is inspected at 15,000,
    45,000, 75,000 and 105,000, beside a note that dusty or heavy-traffic driving may shorten its life;
  - there is no EFI tank additive line;
  - footnote wording.
  Everything else is the same as 2018, so this is a list of its own only because of the cabin filter.
- **One log, split by engine** (the owner's ruling of 2026-10-23). The guide covers the 2.7L four-cylinder
  (1AR-FE) and the 3.5L V6 (2GR-FKS) in one log. Two lines name an engine, and both say the V6: spark
  plugs at 60,000 and the brake booster's vacuum pump at 120,000, each "(2GR-FKS engine only)". Every other
  line names no engine, so it is the 2.7L's as much as the V6's. This list is the log without those two
  lines. Spark plugs are replaced at 120,000 for every engine.
- The 3.5L list isn't made here: the Highlander Hybrid is a 3.5L too, and Glovebox can't yet tell the two
  apart.
- **"(AWD)"**: the guide prints "(4WD)" on the propeller shaft bolt, rear differential oil and transfer
  case oil lines. Toyota sells the Highlander as AWD, so they are named "(AWD)" and kept in
  `allWheelDriveOnly` (the owner's ruling); a Highlander known to be front-wheel drive doesn't get them.
- Left out, because they aren't jobs at a milestone: the 0W-20 oil note, the cabin filter note, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000 / 6, cabin and
  engine air filters 30,000 / 36, spark plugs 120,000 / 144, engine coolant first at 100,000 and then every
  50,000 / 60 (the guide's "or 120 months" counts from the in-service day, which Glovebox doesn't know).
- Only for severe use: rear differential oil (AWD) and transfer case oil (AWD) every 15,000 / 18 when
  towing or carrying heavy loads; automatic transmission fluid every 60,000 / 72 when towing or under
  extensive idling.
- Table only: the propeller shaft bolt re-torque, every inspection (the differential and transfer case
  ones carry service lines but aren't reminders) and drive belts (first at 60,000, then every 15,000).
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
