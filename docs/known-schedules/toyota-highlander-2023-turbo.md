# Toyota Highlander 2.4L turbo, 2023

How `app/src/main/assets/known-schedules/toyota-highlander-2023-turbo.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2023 Warranty and Maintenance Guide for the Highlander,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-23Highlander/pdf/T-MMS-23Highlander.pdf`,
which Toyota's owners site lists for the 2023 Highlander. Downloaded on 2026-10-22 and read on 2026-10-23.

- The guide's maintenance log (printed pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it. It stands alone: the 2024 log differs
  (below), and the 2022 log is for the V6.
- **The engine.** The guide doesn't name it. Toyota's 2023 Highlander owner's manual (`OM0E055U`, at
  `https://assets.sipb.toyota.com/publications/en/om-s/OM0E055U/pdf/OM0E055U.pdf`) gives one engine, the
  T24A-FTS 2.4-litre turbo four (2393 cm³). The list is for that size (`displacementL` 2.4, "2.4L turbo
  4-cylinder"), so it is never given to the 2.5-litre Highlander Hybrid, which has a guide of its own.
- **"(4WD)" read as "(AWD)".** The guide marks rear differential oil, transfer case oil and the propeller
  shaft bolt re-torque "(4WD)". Toyota sells the Highlander as front-wheel or all-wheel drive, so these jobs
  are in `allWheelDriveOnly` and named "(AWD)" (the owner's ruling of 2026-10-23, #33). A vehicle known to
  be front-wheel drive doesn't get them. The drive shaft boots carry no mark and are for every Highlander.
- **Spark plugs have no reminder.** The log replaces them at 40,000, 60,000, 80,000 and 120,000 miles, which
  is no single interval (`KnownSchedules.parse` would refuse any), so they are in the table only, with their
  service line so that a logged change still counts there. The 2020–2022 V6 log has them at 60,000 and
  120,000 and the 2024 log at 40,000, 80,000 and 120,000; nothing is worked out from either.
- **Severe-use oil at 45,000.** The log lists the oil change under special conditions (dusty roads) at every
  5,000-mile milestone between the ordinary changes except 45,000, where the dusty-road heading has only the
  air filter inspection and the nuts and bolts. The table follows the log and leaves it out there; the
  severe-use reminder below is the log's interval at every other such milestone. The 2024 log has it at
  45,000.
- **Special operating conditions.** The log replaces the oil under dirt or dusty roads only, not when towing,
  on short trips or idling as the V6 log did. The guide's explanation of the oil change at the back still
  speaks of any of the listed conditions; the list follows the log.
- **Intercooler coolant.** The log inspects it at the 15,000-mile milestones and replaces it at 100,000; its
  footnote says first at 100,000 miles / 120 months, then every 50,000 / 60. That is a reminder
  (`intercooler-coolant`), like the engine coolant beside it. The first change's "or 120 months" isn't used:
  it counts from the day the vehicle went into service, which Glovebox doesn't know.
- In the table only: the EFI tank additive, which the log lists at every milestone for Hawaii only, named
  so; the propeller shaft bolt re-torque and the chassis nuts and bolts, which are torque jobs; and every
  inspection but the rear differential and transfer case oil (below), including the brake booster vacuum
  pump at 120,000 (its footnote says what to replace in it).
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles
  if 0W-20 wasn't used at the last change, the note on the cabin air filter in heavy traffic or dust, and
  the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000 / 6, cabin and
  engine air filters 30,000 / 36, engine coolant and intercooler coolant each first at 100,000 and then
  every 50,000 / 60, rear differential oil (AWD) and transfer case oil (AWD) 30,000 / 36 (15,000 / 18 when
  towing or carrying heavy loads).
- Inspections that are reminders too (the owner's ruling of 2026-10-23, #33: any vehicle with a differential
  needs it inspected, as asked for the 4Runner on 2026-10-13): rear differential oil (AWD) and transfer case
  oil (AWD) are inspected at 15,000, 45,000, 75,000 and 105,000 and replaced at the 30,000s, so each
  inspection is a reminder first at 15,000 miles and then every 30,000 / 36. Logging a change of one counts
  as its check.
- Only for severe use (#136): automatic transmission fluid at 60,000 / 72 when towing, carrying heavy loads
  or idling long. Under ordinary driving the guide only inspects the transmission.
- The other inspections are in the table only, with a check line where Glovebox has one (fluid levels,
  wiper blades, engine coolant, drive belts).
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2020–2022 (the 3.5-litre V6, a list of its own), 2024 ([its own list](toyota-highlander-2024-turbo.md)),
  2025 and 2026 (charts, [2025](toyota-highlander-2025-turbo.md) and [2026](toyota-highlander-2026-turbo.md)),
  2027 (no guide found on 2026-10-22).
