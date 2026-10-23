# Toyota Sienna, 2023–2024

How `app/src/main/assets/known-schedules/toyota-sienna-2023-2024.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-23SiennaHV/pdf/T-MMS-23SiennaHV.pdf` and
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-24Sienna/pdf/T-MMS-24Sienna.pdf` (the 2024
guide drops "HV" from its name). Read on 2026-10-23.

- The 2021 guide's log was read in full ([2021](toyota-sienna-2021.md)); the 2023 and 2024 logs (PDF pages
  40 to 55, the same 24 milestones to 120,000 miles) were compared with it and with each other: a script's
  count of every line, then each difference looked at on the page.
- 2023 and 2024 agree on every job and where it falls. What differs is wording: the 2024 oil note says
  "recommended oil" where 2023 says "0W-16", and the footnotes are reworded.
- Against 2021 and 2022: the special-condition oil change at the odd 5,000s is printed under dirt or dusty
  roads only; the towing, short-trip and idling oil changes are gone. (The 2023 guide's explanation pages
  still speak of a 5,000-mile oil change under special conditions in general; the list follows the log.)
  The 2022 auxiliary battery line (Autonomous Driving Kit) is gone too. Every other job is the same.
- Every Sienna of these years is a hybrid with one engine, so the list has no `engine` block and no
  powertrain. NHTSA's model list gives the name as "Sienna" only.
- Drivetrain: the guide prints "(AWD)" on the rear differential oil inspection (every 30,000) and its
  replacement when towing (60,000 and 120,000). They are in `allWheelDriveOnly`, named "(AWD)" as printed,
  and are left out for a Sienna known to be front-wheel drive.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected at every milestone and cleaned every
  20,000 miles; the cleaning is a reminder (`hybrid-battery-filter`), the inspection is in the table only.
- Coolant: the 100,000-mile line names the engine and inverter coolant together; its footnote gives the
  engine coolant first at 100,000 miles, then every 50,000 / 60 months, and the inverter coolant first at
  150,000, then every 50,000 / 60. The line is written as "Replace engine coolant" (`coolant`). **The
  inverter coolant has no reminder**: its first change is past the end of the log and given only in that
  footnote. The first change's "or 120 months" isn't used (it counts from the day the vehicle went into
  service, which Glovebox doesn't know).
- Left out: "Add Toyota EFI Tank Additive" (Hawaii, Puerto Rico and Saipan only), the note to change the oil
  again if the recommended oil wasn't used, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the log's dusty-roads oil change),
  tire rotation 5,000 / 6, hybrid battery cooling filter cleaning 20,000 / 24, cabin and engine air filters
  30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every
  50,000 / 60.
- Only for severe use (#136): automatic transmission fluid and rear differential oil (AWD) every
  60,000 / 72 when towing or carrying heavy loads.
- Inspections are in the table only, with a check line where the catalog has one. Brake fluid has no
  replacement in the log, so it has no reminder from this list.
- Not covered here: 2021 and 2022 (lists of their own, which differ in the oil changes above), and 2025 on
  (a chart in place of the log, with lists of their own).
