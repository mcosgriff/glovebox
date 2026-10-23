# Toyota Camry Hybrid, 2018–2020

How `app/src/main/assets/known-schedules/toyota-camry-2018-2020-hybrid.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the Camry Hybrid for each model year, which Toyota
publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>CamryHV/pdf/T-MMS-<yy>CamryHV.pdf`
(`18` to `20`). Read on 2026-10-23.

- The 2020 guide's maintenance log (pages 42 to 57: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it. The 2018 guide, laid out differently,
  was read page by page as well.
- 2019 was compared with 2020 by a script's count of each line on each log page, sorted by the special
  operating condition it is printed under, and matched exactly. 2018 has the same jobs in the same
  places; its EFI tank additive line names American Samoa too.
- **Its own guide, not the Camry's.** The hybrid's log has no drive belts, no automatic transmission
  fluid cooler hoses and no brake booster vacuum pump; its spark plugs are changed at 120,000 only; it
  inspects the "engine/inverter coolant"; and its transmission fluid is replaced (at 60,000 and 120,000)
  only when towing or carrying heavy loads, not for long idling. It names no engine and has no line for
  a drivetrain. This list has `engine.powertrain` `hybrid`, and the 2.5L gas lists have `gas`; a 2.5L
  Camry whose powertrain isn't on record is asked which it is.
- **The hybrid battery's cooling filter.** These years print "clean the HV battery cooling intake
  filter" at every milestone, and only under extensive idling or low-speed driving. So it is a job for
  severe use only: in the table at every milestone, named with its condition, and a reminder every
  5,000 / 6 only while "How do you drive it?" is on Severe. It is logged with the hybrid battery
  cooling filter line.
- **Engine and inverter coolant at 100,000.** The 100,000 box prints "Replace engine/inverter coolant",
  and the guide's footnote to that line settles it: the engine coolant is first replaced at
  100,000 miles / 120 months, the inverter coolant first at 150,000 miles / 180 months, and both every
  50,000 / 60 after that. So the 100,000 job is the engine coolant's, and that is how this list names
  it. The inverter coolant's first change is past the end of the log (120,000), so it has no place in
  the table and can't be the table's own interval (rule 7). Under the owner's ruling of 2026-10-23
  (#33) it is an `additional` item, never on the table: "Replace inverter coolant"
  (`inverter-coolant`), first at 150,000 miles and then every 50,000 / 60. The "180 months" isn't used,
  for the same reason as the coolant's 120 (below).
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's
  EFI tank additive (printed at every milestone for Hawaii and Puerto Rico only), the note that the oil
  is to be replaced at 5,000 miles if 0W-16 wasn't used at the last change, and the explanations of
  each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any of its four special operating conditions), tire rotation 5,000 / 6, cabin air filter
  10,000 / 12, engine air filter 30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at
  100,000 miles and then every 50,000 / 60, and (`additional`) inverter coolant first at 150,000 miles
  and then every 50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it
  counts from the day the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): the hybrid battery's cooling filter every 5,000 / 6 for long idling or
  low-speed driving, and automatic transmission fluid every 60,000 / 72 when towing or carrying heavy
  loads.
- The inspections are in the table only.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2021–2024 are a list of their own
  ([toyota-camry-2021-2024-hybrid](toyota-camry-2021-2024-hybrid.md)), since from 2021 the guide checks
  the battery filter at every milestone and cleans it every 20,000 for everyone. 2017 and earlier, and
  2025 on, are other generations, whose guides weren't read.
