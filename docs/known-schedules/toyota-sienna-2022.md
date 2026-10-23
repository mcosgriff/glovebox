# Toyota Sienna, 2022

How `app/src/main/assets/known-schedules/toyota-sienna-2022.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2022 Sienna Warranty and Maintenance Guide,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-22SiennaHV/pdf/T-MMS-22SiennaHV.pdf`. Read on
2026-10-23.

- The 2021 guide's log was read in full ([2021](toyota-sienna-2021.md)) and the 2022 log (PDF pages 40 to 55,
  the same 24 milestones to 120,000 miles) compared with it: a script's count of every line, then each
  difference looked at on the page.
- The one difference: 2022 adds "Replace auxiliary battery" at every 10,000 miles, footnoted as for vehicles
  with the Autonomous Driving Kit only. It is in the table as "Replace auxiliary battery · Autonomous Driving
  Kit only", with no `service` and no reminder, since it is equipment most Siennas don't have. Because the
  2021 guide doesn't print it, 2021 and 2022 are separate lists. Everything else is the 2021 list.
- Every Sienna of these years is a hybrid with one engine, so the list has no `engine` block and no
  powertrain. NHTSA's model list gives the name as "Sienna" only.
- Drivetrain: the guide prints "(AWD)" on the rear differential oil inspection (every 30,000) and its
  replacement when towing (60,000 and 120,000). They are in `allWheelDriveOnly`, named "(AWD)" as printed,
  and are left out for a Sienna known to be front-wheel drive.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected at every milestone and cleaned every
  20,000 miles; the cleaning is a reminder (`hybrid-battery-filter`), the inspection is in the table only.
- Coolant: the 100,000-mile line names the engine and inverter coolant together; its footnote gives the
  engine coolant first at 100,000 miles, then every 50,000 / 60 months, and the inverter coolant first at
  150,000, then every 50,000 / 60. The line is written as "Replace engine coolant" (`coolant`). The
  inverter coolant's first change is past the end of the log and given only in that footnote, so it is an
  `additional` item, "Replace inverter coolant" (`inverter-coolant`), every 50,000 / 60 and first at
  150,000, never placed on the table (the owner's ruling of 2026-10-23, #33). The first changes' "or 120
  months" (engine coolant) and "or 180 months" (inverter coolant) aren't used: they count from the day the
  vehicle went into service, which Glovebox doesn't know.
- Left out: "Add Toyota EFI Tank Additive" (Hawaii, Puerto Rico and Saipan only), the note to change the oil
  again if 0W-16 wasn't used, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000 / 6, hybrid
  battery cooling filter cleaning 20,000 / 24, cabin and engine air filters 30,000 / 36, spark plugs
  120,000 / 144, engine coolant first at 100,000 miles and then every 50,000 / 60, and inverter coolant
  (the `additional` item) first at 150,000 and then every 50,000 / 60.
- Inspection that is a reminder too: rear differential oil (AWD) every 30,000 / 36. Logging a change
  counts as its check. This is the owner's ruling of 2026-10-23 (#33): any vehicle with a differential
  needs it inspected, so the 4Runner's rule applies to every list whose guide prints the inspection.
- Only for severe use (#136): automatic transmission fluid and rear differential oil (AWD) every
  60,000 / 72 when towing or carrying heavy loads.
- The special-condition oil change at the odd 5,000s is printed under four conditions in this year (dirt or
  dusty roads, towing, short trips below freezing, long idling); the table names them on one line.
- The other inspections are in the table only, with a check line where the catalog has one. Brake fluid
  has no replacement in the log, so it has no reminder from this list.
- Not covered here: 2021 and 2023 on, which have lists of their own.
