# Toyota Highlander 2.5L hybrid, 2020–2023

How `app/src/main/assets/known-schedules/toyota-highlander-2020-2023-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the Highlander Hybrid, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>HighlanderHV/pdf/T-MMS-<yy>HighlanderHV.pdf`
(`20` to `23`). Toyota's owners site lists no guide for the hybrid; these were found only at that address.
Downloaded on 2026-10-22 and read on 2026-10-23.

- The 2020 guide's maintenance log (printed pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart,
  to 120,000 miles) was read in full and the list written from it.
- 2021, 2022 and 2023 were compared page by page with a script, and every page it flagged was looked at.
  The jobs and where they fall are the same in all four. What differs is wording: "radiator, condenser
  and/or intercooler" for "or intercooler" (2021 on), "all cooling systems" in the fluid-level footnote
  (2023), and how lines wrap. The 2024 guide changes the oil change under special conditions and is
  [a list of its own](toyota-highlander-2024-hybrid.md).
- **The engine.** The guides don't name it. Toyota's owner's manuals for the Highlander Hybrid
  (`OM0E037U`, printed November 2019, and `OM0E056U`, printed July 2022, at
  `https://assets.sipb.toyota.com/publications/en/om-s/<number>/pdf/<number>.pdf`; neither is listed on
  Toyota's owners site and neither prints its model year) give one engine, the A25A-FXS 2.5-litre (2487 cm³).
  Only that size is taken from them. The list is labelled "2.5L hybrid" (`displacementL` 2.5). The petrol
  Highlander of these years has a guide of its own and other engines (a 3.5-litre V6 to 2022, a 2.4-litre
  turbo from 2023), so no two Highlander lists share a size and no `powertrain` is needed. vPIC lists only
  "Highlander" for these years (`GetModelsForMakeYear`, checked 2026-10-23), not a separate hybrid model.
- **"(4WD)" read as "(AWD)".** The guide marks the rear differential oil "(4WD)". Toyota sells the
  Highlander as front-wheel or all-wheel drive, so those jobs are in `allWheelDriveOnly` and named "(AWD)"
  (the owner's ruling of 2026-10-23, #33). A vehicle known to be front-wheel drive doesn't get them. The
  drive shaft boots carry no mark and are for every Highlander Hybrid.
- **Engine and inverter coolant.** The log's line at 100,000 says "Replace engine/inverter coolant", and its
  footnote gives the engine coolant first at 100,000 miles / 120 months, then every 50,000 / 60, and the
  inverter coolant first at 150,000 / 180, then every 50,000 / 60. In the table the line is the engine
  coolant change at 100,000 (`coolant`). **The inverter coolant has no reminder:** its interval is printed
  only in that footnote and its first change falls past the log's end (120,000), so the table never shows
  it (coordinator's rule of 2026-10-23, following the owner's ruling in #33). The engine coolant's first "or
  120 months" isn't used either: it counts from the day the vehicle went into service, which Glovebox
  doesn't know.
- The HV battery cooling intake filter is inspected at every milestone and cleaned every 20,000 miles in
  place of the inspection (`hybrid-battery-filter`).
- In the table only: the EFI tank additive, which the log lists at every milestone for Hawaii and Puerto
  Rico only, named with those places; the chassis and body nuts and bolts, a torque job; and every
  inspection.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles
  if 0W-16 wasn't used at the last change, the note on the cabin air filter in heavy traffic or dust, and
  the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval under any
  of its four special operating conditions), tire rotation 5,000 / 6, HV battery cooling intake filter
  cleaned every 20,000 / 24, cabin and engine air filters 30,000 / 36, spark plugs 120,000 / 144, engine
  coolant first at 100,000 and then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid and rear differential oil (AWD) every 60,000 / 72
  when towing or carrying heavy loads. Under ordinary driving the guide only inspects them.
- Inspections are in the table only, with a check line where Glovebox has one (fluid levels, wiper blades,
  engine and inverter coolant, rear differential oil). The filter inspection has no line.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2024 to 2026 (lists of their own: [2024](toyota-highlander-2024-hybrid.md),
  [2025](toyota-highlander-2025-hybrid.md), [2026](toyota-highlander-2026-hybrid.md)); 2027 (no guide found
  at that address on 2026-10-22); 2019 and before (an earlier generation; not read).
