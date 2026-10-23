# Toyota Sienna, 2021

How `app/src/main/assets/known-schedules/toyota-sienna-2021.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2021 Sienna Warranty and Maintenance Guide,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-21SiennaHV/pdf/T-MMS-21SiennaHV.pdf`. Read on
2026-10-23.

- The maintenance log (PDF pages 40 to 55, printed 38 to 53: 24 milestones, 5,000 miles or 6 months apart,
  to 120,000 miles) was read in full, as page images, and the list written from it. It is also the guide the
  2022 and 2023–2024 lists were compared with.
- Why 2021 is a list of its own: the 2022 guide adds a line the 2021 one doesn't have (an auxiliary battery
  for the Autonomous Driving Kit, see [2022](toyota-sienna-2022.md)), and from 2023 the special-condition oil
  change is for dusty roads only (see [2023–2024](toyota-sienna-2023-2024.md)).
- Every Sienna of these years is a hybrid with one engine, so the list has no `engine` block and no
  powertrain. NHTSA's model list gives the name as "Sienna" only (checked for 2021 and 2025 on 2026-10-23).
- Drivetrain: the guide prints "(AWD)" on two lines, the rear differential oil inspection (every 30,000) and
  its replacement when towing (60,000 and 120,000). They are in `allWheelDriveOnly`, named "(AWD)" as
  printed, and are left out for a Sienna known to be front-wheel drive.
- Hybrid jobs: the guide inspects the hybrid battery's cooling intake filter at every milestone and cleans
  it every 20,000 miles; the cleaning is a reminder on the `hybrid-battery-filter` line, the inspection is
  in the table only.
- Coolant: the log's 100,000-mile line names the engine and inverter coolant together. Its footnote gives
  the engine coolant first at 100,000 miles, then every 50,000 / 60 months, and the inverter coolant first
  at 150,000, then every 50,000 / 60. The line is written as "Replace engine coolant" (`coolant`). The
  inverter coolant's first change is past the end of the log and given only in that footnote, so it is an
  `additional` item, "Replace inverter coolant" (`inverter-coolant`), every 50,000 / 60 and first at
  150,000, never placed on the table (the owner's ruling of 2026-10-23, #33). The footnote's "or 120
  months" for the first engine coolant change and "or 180 months" for the first inverter coolant change
  aren't used: they count from the day the vehicle went into service, which Glovebox doesn't know.
- Left out: "Add Toyota EFI Tank Additive" at every milestone, which is for Hawaii, Puerto Rico and Saipan
  only; the note that the oil is to be changed again if 0W-16 wasn't used at the last change (a condition,
  not a job at a milestone); and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the log's oil change under any of its
  special conditions), tire rotation 5,000 / 6, hybrid battery cooling filter cleaning 20,000 / 24, cabin
  and engine air filters 30,000 / 36, spark plugs 120,000 / 144, engine coolant first at 100,000 miles
  and then every 50,000 / 60, and inverter coolant (the `additional` item) first at 150,000 and then every
  50,000 / 60.
- Inspection that is a reminder too: rear differential oil (AWD) every 30,000 / 36. Logging a change
  counts as its check. This is the owner's ruling of 2026-10-23 (#33): any vehicle with a differential
  needs it inspected, so the 4Runner's rule applies to every list whose guide prints the inspection.
- Only for severe use (#136): automatic transmission fluid and rear differential oil (AWD) every
  60,000 / 72 when towing or carrying heavy loads.
- The other inspections are in the table only, with a check line where the catalog has one (fluid levels,
  wiper blades, brakes, engine and inverter coolant). The table names the radiator,
  condenser and intercooler as the guide does.
- The special-condition oil change at the odd 5,000s is printed under four conditions in this year (dirt or
  dusty roads, towing, short trips below freezing, long idling); the table names them on one line.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered here: 2020 and earlier (another generation), and 2022 on, which have lists of their own.
