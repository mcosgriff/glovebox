# Toyota Highlander 2.4L turbo, 2024

How `app/src/main/assets/known-schedules/toyota-highlander-2024-turbo.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2024 Warranty and Maintenance Guide for the Highlander,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-24Highlander/pdf/T-MMS-24Highlander.pdf`,
which Toyota's owners site lists for the 2024 Highlander. Downloaded on 2026-10-22 and read on 2026-10-23.

- The guide's maintenance log (printed pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was compared page by page with the 2023 log, which was read in full for
  [the 2023 list](toyota-highlander-2023-turbo.md), and every page that differed was read.
- **What differs from 2023:**
  - spark plugs at 40,000, 80,000 and 120,000 (2023 also has 60,000), so here they are a reminder every
    40,000 / 48;
  - no engine oil cooler hose inspection;
  - the oil change under special conditions (dusty roads) is at 45,000 too;
  - the EFI tank additive is for Hawaii, Puerto Rico, Saipan and American Samoa (2023: Hawaii only), and
    the oil note says "recommended oil" for "0W-20".
- **The engine.** Toyota's 2024 Highlander owner's manual (`OM0E194U`, at
  `https://assets.sipb.toyota.com/publications/en/om-s/OM0E194U/pdf/OM0E194U.pdf`) gives one engine, the
  T24A-FTS 2.4-litre turbo four (2393 cm³): `displacementL` 2.4, "2.4L turbo 4-cylinder". The 2.5-litre
  Highlander Hybrid has a guide and a list of its own.
- **"(4WD)" read as "(AWD)"**, as on the 2023 list: rear differential oil, transfer case oil and the
  propeller shaft bolt re-torque are in `allWheelDriveOnly` and named "(AWD)" (the owner's ruling of
  2026-10-23, #33).
- **Special operating conditions.** The log replaces the oil under dirt or dusty roads only; the guide's
  explanation of the oil change still speaks of any listed condition, and the list follows the log.
- **Intercooler coolant**, as on the 2023 list: first at 100,000, then every 50,000 / 60
  (`intercooler-coolant`).
- In the table only: the EFI tank additive, the propeller shaft bolt re-torque and the chassis nuts and
  bolts, and every inspection but the rear differential and transfer case oil (below), including the brake
  booster vacuum pump at 120,000.
- Left out: the oil note, the cabin air filter note, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000 / 6, cabin and
  engine air filters 30,000 / 36, spark plugs 40,000 / 48, engine coolant and intercooler coolant each first
  at 100,000 and then every 50,000 / 60, rear differential oil (AWD) and transfer case oil (AWD)
  30,000 / 36 (15,000 / 18 when towing or carrying heavy loads). The coolants' first "or 120 months" isn't
  used.
- Inspections that are reminders too, as on the 2023 list (the owner's ruling of 2026-10-23, #33): rear
  differential oil (AWD) and transfer case oil (AWD), first at 15,000 miles and then every 30,000 / 36.
  Logging a change of one counts as its check.
- Only for severe use (#136): automatic transmission fluid at 60,000 / 72 when towing, carrying heavy loads
  or idling long.
- The other inspections are in the table only, with a check line where Glovebox has one (fluid levels,
  wiper blades, engine coolant, drive belts).
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2025 on, which print a chart in place of the log ([2025](toyota-highlander-2025-turbo.md),
  [2026](toyota-highlander-2026-turbo.md)).
