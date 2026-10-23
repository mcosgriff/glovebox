# Toyota Tacoma, 2024 (hybrid)

How `app/src/main/assets/known-schedules/toyota-tacoma-2024-hybrid.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2024 Tacoma hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-24TacomaHV/pdf/T-MMS-24TacomaHV.pdf`. Read on
2026-10-23. Toyota's owners site lists no hybrid guide under the Tacoma; it was found only at that address.

- The guide's maintenance log (pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to 120,000
  miles) was read in full, page by page, and the list written from it. 2024 is the only year of this
  generation with a log; it is the only year this list covers.
- **The 2024 Tacoma has a gas and a hybrid version of one size**, a 2.4L turbo four-cylinder and the
  i-FORCE MAX hybrid built on it, and Toyota prints a guide for each. This list is for the hybrid
  (`"powertrain": "hybrid"`, label "2.4L turbo hybrid"); the gas engine has
  [its own list](toyota-tacoma-2024-gas.md). vPIC names both "Tacoma" for 2024 (checked 2026-10-23).
- The hybrid's guide marks no line "(4WD)": its drive shaft boots, front differential and transfer case
  lines have no mark, so nothing is in `fourWheelDriveOnly` and no job is left out for any drivetrain. The
  gearbox is automatic only, so there are no manual-gearbox jobs.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected every 5,000 miles and cleaned every
  20,000 (`hybrid-battery-filter`).
- **Coolant**: at 100,000 miles the log prints one line, "engine/inverter coolant". Its footnote gives the
  engine coolant's first change at 100,000 miles or 120 months, then every 50,000 / 60, and the inverter
  coolant's first change at 150,000 or 180 months, then every 50,000 / 60. The line is "Replace engine
  coolant" here (`coolant`), a reminder first at 100,000. The inverter coolant's interval is given only in
  that footnote and its first change falls past the log's end, so it is an `additional` item, "Replace
  inverter coolant" (`inverter-coolant`), every 50,000 / 60 and first at 150,000, never placed on the table
  (the owner's ruling of 2026-10-23, #33). The 120 and 180 months for the first changes aren't used: they
  count from the day the vehicle went into service, which Glovebox doesn't know.
- **The vacuum pump** is inspected at 120,000 miles, and the footnote says to replace its vane and vane caps.
  It is in the table only, as one job, with no catalog line (a one-off part).
- Left out: the fuel-tank additive the log lists at every milestone for Hawaii only (a job for one region,
  which the format can't name), the note that the oil is to be replaced if the recommended oil wasn't used
  at the last change, the note on the cabin air filter in heavy traffic or dust, the daily checks for
  off-road driving, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 on dirt or dusty roads), tire rotation 5,000 / 6,
  cleaning the hybrid battery cooling intake filter 20,000 / 24, cabin and engine air filters 30,000 / 36,
  spark plugs 40,000 / 48, engine coolant first at 100,000 miles and then every 50,000 / 60, and inverter
  coolant (the `additional` item) first at 150,000 and then every 50,000 / 60.
- Inspections that are reminders too: front and rear differential oil every 15,000 / 18, transfer case oil
  every 30,000 / 36, unmarked as the guide prints them. Logging a change of one counts as its check. This is
  the owner's ruling of 2026-10-23 (#33): any vehicle with a differential needs it inspected, so the
  4Runner's rule applies to every list whose guide prints the inspection. The other inspections are in the
  table only, with a check line where there is one: fluid levels, wiper blades, brakes, drive belts (first
  at 60,000, then every 15,000) and engine and inverter coolant.
- Only for severe use (#136): front and rear differential oil every 15,000 / 18 when towing or carrying heavy
  loads; transfer case oil every 30,000 / 36 on dusty roads or when towing; automatic transmission fluid
  every 60,000 / 72 when towing or under long idling or low-speed driving (the log prints it under both).
- In the table only: re-torquing the propeller shaft bolt (every 15,000, and at the other milestones on dusty
  or salted roads or when towing) and tightening the chassis and body nuts and bolts.
- The log has no brake fluid replacement, so it has no reminder from this list.
- Not covered: 2025 on (a chart in place of the log: [the 2025–2026 list](toyota-tacoma-2025-2026-hybrid.md)).
  There was no Tacoma hybrid before 2024.
