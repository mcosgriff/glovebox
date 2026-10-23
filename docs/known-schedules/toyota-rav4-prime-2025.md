# Toyota RAV4 Plug-in Hybrid (RAV4 Prime), 2025

How `app/src/main/assets/known-schedules/toyota-rav4-prime-2025.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2025 Warranty and Maintenance Guide for the RAV4 Plug-in Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-25RAV4PHV/pdf/T-MMS-25RAV4PHV.pdf`
(`25RAV4Prime` isn't at that address). Read on 2026-10-23.

- **The model is matched by its name.** Toyota calls the 2025 car the "RAV4 Plug-in Hybrid"; vPIC's model
  list for Toyota (`GetModelsForMakeYear`, checked 2026-10-23) still gives "RAV4 Prime (PHEV)" for 2025, as
  for 2021 to 2024, and has no other spelling. That is the list's `model`, and it needs no powertrain. An
  owner who typed "RAV4 Plug-in Hybrid" by hand gets no list, not a wrong one.
- **A chart, not a log.** The 2025 guide gives the schedule as a chart (pages 38 to 43): jobs as rows,
  columns every 5,000 miles from 5,000 to 150,000, each cell I (inspect), P (perform), R (replace) or
  T (torque), with a "Special Operating Conditions" chart beside each standard one that marks only cells
  the standard chart doesn't already have. All six pages were read in full and the list written from them.
  2021 to 2024 print a log and are [a list of their own](toyota-rav4-prime-2021-2024.md).
- **No months.** The chart's columns are in miles only, so the milestones have no months, and so do the
  reminders: the guide prints months only for the two coolants, whose rows have no `service` (below).
  Nothing says the chart repeats after 150,000, so the table doesn't.
- **Rows marked "(if applicable)"** are in the table with no `service`, so they never drive a reminder
  (the owner's ruling of 2026-10-23, #33): the radiator, condenser and intercooler, the engine and
  intercooler coolant (replaced at 100,000 and 150,000) and the inverter and intercooler coolant (replaced
  at 150,000). So this list, unlike the 2021–2024 one, has no coolant reminder.
- What differs from the 2021–2024 log besides the layout: the chart has no DC/DC converter air intake
  filter row, and air conditioner refrigerant is added at 80,000 with no repeat and no regional footnote.
  It is in the table with the air conditioning line, and isn't a reminder.
- The chart marks no drivetrain on any row; the rear differential oil has no mark, so it is kept for
  every car.
- Left out: the EFI tank additive ("Island use only"), and the notes on oil selection and the cabin
  filter.
- Reminders: engine oil every 10,000 miles (5,000 for severe use: dust, towing, idling or short trips in
  freezing weather), tire rotation every 5,000, cabin and engine air filters every 30,000, and spark
  plugs every 120,000.
- Only for severe use: automatic transmission fluid and rear differential oil every 60,000 when towing.
- Inspections are in the table only, with a check line where Glovebox has one (fluid levels, wiper
  blades, rear differential oil).
- The brake linings row is I at every column; its footnote says visually every 5,000 and thickness and
  runout every 30,000. The table keeps the chart's one row.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2026 on (not read).
