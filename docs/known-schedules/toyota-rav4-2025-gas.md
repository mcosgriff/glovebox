# Toyota RAV4 (gas), 2025

How `app/src/main/assets/known-schedules/toyota-rav4-2025-gas.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2025 Warranty and Maintenance Guide for the RAV4,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-25RAV4/pdf/T-MMS-25RAV4.pdf`. Read on 2026-10-23.

- **A chart, not a log.** The 2025 guide gives the schedule as a chart (pages 38 to 43): jobs as rows,
  columns every 5,000 miles from 5,000 to 150,000, each cell I (inspect), P (perform), R (replace) or
  T (torque), with a "Special Operating Conditions" chart beside each standard one that marks only cells
  the standard chart doesn't already have. All six pages were read in full and the list written from them.
  The guide's other years print a log and are [a list of their own](toyota-rav4-2019-2024-gas.md).
- **No months.** The chart's columns are in miles only, so the milestones have no months, and so do the
  reminders: the guide prints months for no job but the coolant, whose row has no `service` (below).
  Nothing says the chart repeats after 150,000, so the table doesn't.
- **Rows marked "(if applicable)"** are in the table with no `service`, so they never drive a reminder
  (the owner's ruling of 2026-10-23, #33): the propeller shaft bolts, the radiator, condenser and
  intercooler, the rear differential oil, the transfer case oil, and the engine and intercooler coolant
  (replaced at 100,000 and 150,000). The same rows under special conditions are named with their
  condition and have no `service` either. The chart marks nothing "(4WD)" or "(AWD)", so this list has
  no drivetrain field: every 2025 RAV4 gets those rows, as the guide prints them.
- The engine is the 2.5L four-cylinder in a gas and a hybrid version with different guides. This list
  is marked `powertrain: gas` and labelled "2.5L gas", so a RAV4 is only matched to it once its owner, or
  vPIC's decode, says it isn't a hybrid; otherwise setup asks.
- The brake booster vacuum pump at 120,000 is in the table only, with no `service`: the chart inspects it
  and its footnote says to replace the vane and caps, once, and Glovebox has no line for that part.
- Left out: the EFI tank additive ("Island use only"), and the notes on oil selection and the cabin
  filter.
- Reminders: engine oil every 10,000 miles (5,000 for severe use: dust, towing, idling or short trips in
  freezing weather), tire rotation every 5,000, cabin and engine air filters every 30,000, and spark
  plugs every 120,000.
- Only for severe use: automatic transmission fluid every 60,000 when towing or idling for long periods.
- Inspections are in the table only, with a check line where Glovebox has one (fluid levels, wiper
  blades, drive belts). Drive belts are first inspected at 60,000, then every 15,000.
- The brake linings row is I at every column; its footnote says visually every 5,000 and thickness and
  runout every 30,000. The table keeps the chart's one row.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2026 on (`26RAV4` wasn't found at Toyota's address on 2026-10-22).
