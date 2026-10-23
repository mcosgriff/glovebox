# Toyota Corolla 2.0L, 2023–2024

How `app/src/main/assets/known-schedules/toyota-corolla-2023-2024-2.0.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guides, read on 2026-10-23: the sedan's,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>Corolla/pdf/T-MMS-<yy>Corolla.pdf`, and
the hatchback's,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>CorollaHB/pdf/T-MMS-<yy>CorollaHB.pdf`
(`23` and `24` for each).

- The 2023 sedan guide's maintenance log (pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- The 2024 sedan and the 2023 and 2024 hatchback were compared with it by a script that reads each
  milestone's words, and by a script that names each job it finds at each milestone and holds them to this
  list. The jobs and where they fall are the same in all four. What differs is wording: the hatchback's EFI
  additive line says "Hawaii and Puerto Rico only" where the sedan's adds Saipan.
- **Sedan and hatchback are one list.** NHTSA gives both as "Corolla" (`GetModelsForMakeYear` for Toyota,
  2023 and 2024, checked 2026-10-23: no "Corolla Hatchback"), and their guides agree. The gas cars of these
  years have the 2.0L only and the log names no engine; the Corolla Hybrid's 1.8L has a list of its own
  ([toyota-corolla-2023-2024-hybrid](toyota-corolla-2023-2024-hybrid.md)), so this list has `engine` 2.0L
  and needs no powertrain.
- **Not the 2019–2022 list.** From 2023 the manual transmission lines are gone, the transmission inspected
  for leaks and the fluid changed under towing or idling are "automatic transmission", the radiator line
  reads "radiator, condenser and/or intercooler", and the brake booster's vacuum pump is inspected with no
  engine named ([toyota-corolla-2019-2022-2.0](toyota-corolla-2019-2022-2.0.md)).
- **Automatic transmission fluid.** At 60,000 and 120,000 the log replaces it under towing or heavy loads,
  and again under long idling. That is one job under either condition, so it is one line,
  "Replace automatic transmission fluid · towing or long idling", as on the other Toyota lists.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (printed at every milestone for Hawaii, Puerto Rico and Saipan only), the note that the oil
  is to be replaced at 5,000 miles if the recommended oil wasn't used at the last change, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any of its four special operating conditions), tire rotation 5,000 / 6, cabin and engine air
  filters 30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts from the day
  the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or idling for long
  periods.
- The inspections are in the table only, with the check line where Glovebox has one (fluid levels, wiper
  blades, brakes, engine coolant, drive belts). The drive belts are inspected first at 60,000 and then
  every 15,000; the brake booster's vacuum pump at 120,000.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2019–2022 and 2025 are lists of their own. **2026 is not covered**: the 2026 sedan's chart
  agrees with 2025's, but the 2026 hatchback's special operating conditions differ from both (seen on its
  first such page, PDF page 40: the dusty-road inspections at every 5,000, the engine oil replaced at every
  5,000, the engine air filter replaced at 30,000, and an automatic transmission fluid inspection at
  30,000, none of which the sedan has), and NHTSA gives both bodies as "Corolla" with the same 2.0L engine,
  so a list can't tell which one a car is. No 2026 gas list is made.
