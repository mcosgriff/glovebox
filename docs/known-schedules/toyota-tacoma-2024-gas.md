# Toyota Tacoma, 2024 (gas)

How `app/src/main/assets/known-schedules/toyota-tacoma-2024-gas.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2024 Tacoma,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-24Tacoma/pdf/T-MMS-24Tacoma.pdf`, the address
Toyota's owners site gives for it (`https://www.toyota.com/service/tcom/cs/manuals/downloads/tacoma/2024`).
Read on 2026-10-23.

- The guide's maintenance log (pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to 120,000
  miles) was read in full, page by page, and the list written from it. 2024 is the only year of this
  generation with a log; it is the only year this list covers.
- **The 2024 Tacoma has a gas and a hybrid version of one size**, a 2.4L turbo four-cylinder and the
  i-FORCE MAX hybrid built on it, and Toyota prints a guide for each. This list is for the gas engine
  (`"powertrain": "gas"`, label "2.4L turbo gas"); the hybrid has
  [its own list](toyota-tacoma-2024-hybrid.md). vPIC names both "Tacoma" for 2024 (checked 2026-10-23), so
  the list needs no other spelling.
- **Drivetrain:** the guide marks the drive shaft boots, the front differential oil and the transfer case oil
  "(4WD)". The Tacoma is a true four-wheel-drive truck, so those jobs are in `fourWheelDriveOnly`, keep
  "(4WD)" in their names, and are left out for a two-wheel-drive Tacoma.
- **Gearbox:** the 2024 gas Tacoma was sold with a manual or an automatic.
  - For a manual only (`manualOnly`, left out for a vehicle known to shift itself): lubricating the clutch
    fork on dusty roads at every milestone, and replacing the manual transmission oil when towing at 30,000
    and 90,000.
  - Not for a manual (`manualExcluded`): inspecting the automatic transmission's cooler hoses and
    connections, and replacing the automatic transmission fluid under long idling or low-speed driving.
  - At 60,000 and 120,000 the towing lines say "transmission fluid or oil", one line for either gearbox. It
    is kept as one line, with no `service`, for both.
  - The manual transmission oil has no reminder. Its only catalog line would be `trans-fluid`, which the
    automatic transmission fluid uses too, and jobs are matched by their line: the two together would be in
    the table at 30,000, 60,000, 90,000 and 120,000, so the automatic's 60,000-mile reminder would be refused.
    It stays in the table only.
- **Coolants:** at 100,000 miles the log replaces the engine coolant and, on a line of its own, the
  intercooler coolant. Each footnote gives the first change at 100,000 miles or 120 months, then every
  50,000 / 60. Each is a reminder on its own line (`coolant`, `intercooler-coolant`). The 120 months for the
  first change aren't used: they count from the day the vehicle went into service, which Glovebox doesn't know.
- **The brake booster's vacuum pump** is inspected at 120,000 miles, and the footnote says to replace its
  vane and vane caps. It is in the table only, as one job, with no catalog line (a one-off part).
- Left out: the fuel-tank additive the log lists at every milestone for Hawaii only (a job for one region,
  which the format can't name), the note that the oil is to be replaced if the recommended oil wasn't used
  at the last change, the note on the cabin air filter in heavy traffic or dust, the daily checks for
  off-road driving, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 on dirt or dusty roads, the only condition the log
  gives for the oil), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark plugs
  40,000 / 48, engine coolant and intercooler coolant each first at 100,000 miles and then every 50,000 / 60.
- Inspections are in the table only (no reminders), with a check line where there is one: fluid levels,
  wiper blades, brakes, drive belts (first at 60,000, then every 15,000), engine coolant, and front and rear
  differential and transfer case oil.
- Only for severe use (#136): front (4WD) and rear differential oil every 15,000 / 18 when towing or carrying
  heavy loads; transfer case oil (4WD) every 30,000 / 36 on dusty roads or when towing; automatic
  transmission fluid every 60,000 / 72 under long idling or low-speed driving.
- In the table only: re-torquing the propeller shaft bolt (every 15,000, and at the other milestones on dusty
  or salted roads or when towing), tightening the chassis and body nuts and bolts, and the clutch fork.
- The log has no propeller shaft lubrication (the 4Runner's has) and no brake fluid replacement, so neither
  has a reminder from this list.
- Not covered: 2016–2023 (the previous generation, with guides of their own), 2025 on (a chart in place of
  the log: [the 2025–2026 list](toyota-tacoma-2025-2026-gas.md)).
