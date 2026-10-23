# Toyota Highlander 3.5L V6, 2020–2022

How `app/src/main/assets/known-schedules/toyota-highlander-2020-2022-v6.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>Highlander/pdf/T-MMS-<yy>Highlander.pdf`
(`20` to `22`). Downloaded on 2026-10-22 and read on 2026-10-23.

- The 2020 guide's maintenance log (printed pages 38 to 53: 24 milestones, 5,000 miles or 6 months
  apart, to 120,000 miles) was read in full and the list written from it.
- 2021 and 2022 were compared page by page. The jobs and where they fall are the same in all three.
  What differs is only where footnote marks sit and how a page's lines are ordered or wrapped.
- **One engine.** These guides are for the gasoline Highlander; the Highlander Hybrid has a guide of its
  own (`T-MMS-<yy>HighlanderHV`). The guides don't name the engine, so the list's engine comes from
  Toyota's owner's manuals for the same years, whose specifications give one engine, the 2GR-FKS
  3.5-litre V6 (3456 cm³): `OM0E036U` (2020), `OM0E096U` and `OM0E142U` (2021, before and from March
  2021 production) and `OM0E137U` (2022), at
  `https://assets.sipb.toyota.com/publications/en/om-s/<number>/pdf/<number>.pdf`. The list is for that
  engine (`displacementL` 3.5), so it is never given to a 2.5-litre hybrid.
- **"(4WD)" read as "(AWD)".** The guide marks rear differential oil, transfer case oil and the propeller
  shaft bolt re-torque "(4WD)". Toyota sells the Highlander as front-wheel or all-wheel drive, and the
  owner's manuals call it AWD, so these jobs are in `allWheelDriveOnly` and named "(AWD)" (the owner's
  ruling of 2026-10-23). A vehicle known to be front-wheel drive doesn't get them. The drive shaft boots
  carry no mark in the guide and are for every Highlander.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000
  miles if 0W-20 wasn't used at the last change, the note on the cabin air filter in heavy traffic or
  dust, and the explanations of each item.
- Table only, with no reminder:
  - the EFI tank additive, which the guide lists at every milestone for Hawaii, Puerto Rico and Saipan
    only, named with those places;
  - the brake booster vacuum pump, replaced once at 120,000 miles. Glovebox has no service line for it
    ("Brake booster" is a different part), and the log never repeats it;
  - the propeller shaft bolt re-torque and the chassis nuts and bolts, which are torque jobs, as on the
    4Runner;
  - every inspection. Rear differential and transfer case oil are inspected at 15,000, 45,000, 75,000
    and 105,000 and replaced at the 30,000s, so the inspection fits no interval of its own; logging a
    change counts as its check.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any of its four special operating conditions), tire rotation 5,000 / 6, cabin and engine air
  filters 30,000 / 36, spark plugs 60,000 / 72, engine coolant first at 100,000 miles and then every
  50,000 / 60, rear differential oil (AWD) and transfer case oil (AWD) 30,000 / 36 (15,000 / 18 when
  towing or carrying heavy loads). The guide's "or 120 months" for the first coolant change isn't used:
  it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): automatic transmission fluid at 60,000 / 72 when towing, carrying heavy
  loads or idling long. Under ordinary driving the guide only inspects the transmission.
- Shorter under severe use: engine oil, rear differential oil and transfer case oil, as above. The
  propeller shaft bolt re-torque goes from every 15,000 to every 5,000 on dusty or salted roads or when
  towing; it is in the table only.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered:
  - the Highlander Hybrid (2.5L) and, from 2023, the 2.4-litre turbo, which have lists of their own
    ([toyota-highlander-2020-2023-hybrid](toyota-highlander-2020-2023-hybrid.md) and the others in the index);
  - 2019 and before (an earlier generation; not read).
