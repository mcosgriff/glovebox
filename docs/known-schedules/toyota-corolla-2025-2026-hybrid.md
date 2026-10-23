# Toyota Corolla Hybrid, 2025–2026

How `app/src/main/assets/known-schedules/toyota-corolla-2025-2026-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the Corolla Hybrid for each model year, which Toyota
publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>CorollaHV/pdf/T-MMS-<yy>CorollaHV.pdf`
(`25` and `26`). Read on 2026-10-23. (Toyota's owners site lists no guide for the hybrid; these addresses
answer.)

- **A chart, not a log.** From 2025 the guide gives the schedule as a maintenance chart: jobs are rows and
  mileages are columns, every 5,000 from 5,000 to 150,000, on three pages, each with a page for special
  operating conditions beside it. The 2025 guide's six chart pages (PDF pages 40 to 45) were read in full
  and the list written from them. The list has 30 milestones, to 150,000 miles.
- The 2026 guide's six chart pages (PDF pages 40 to 45) were read on the page as well, and a script that
  reads each chart's cells held both guides to this list. The cells are the same; what differs is wording
  ("HEV" for "HV"), footnote numbering, and a footnote added in 2026 to the battery filter row (inspect it
  every 5,000, clean it every 20,000), which says what the cells already show.
- **No months at the milestones.** The chart's columns carry only mileages, so the milestones have no
  months, and no footnote gives the hybrid's engine oil any months (the gas Corolla's chart has one; this
  one doesn't). So every reminder here is by distance only.
- **1.8L, no powertrain needed.** NHTSA gives the hybrid and the gas cars all as "Corolla"
  (`GetModelsForMakeYear` for Toyota, 2025 and 2026, checked 2026-10-23: no "Corolla Hybrid"), but the gas
  cars have the 2.0L, so the engine size tells them apart: this list has `engine` 1.8L and no `powertrain`.
  The chart names no engine.
- **The hybrid battery's cooling filter.** The chart inspects it at every column and cleans it ("C") at
  every 20,000. The clean is a reminder every 20,000, logged with the hybrid battery cooling filter line;
  the inspection is in the table only.
- **Rows marked "(if applicable)"** are in the table as printed, with "(if applicable)" in their names and
  no service line, so none of them drives a reminder (the owner's ruling of 2026-10-23). The chart names no
  drivetrain, where the 2023–2024 logs marked the same jobs "(AWD)", so nothing here is
  `allWheelDriveOnly`. These rows are:
  - re-torque the drive shaft nuts and bolts every 15,000, and at every other milestone on dusty roads;
  - inspect the rear differential oil every 30,000, and replace it at 60,000 and 120,000 when towing (a
    severe-use reminder on the 2023–2024 list; here it is not);
  - inspect the radiator, condenser and intercooler every 15,000;
  - **replace the engine coolant and intercooler coolant** at 100,000 and 150,000, and **replace the
    inverter coolant and intercooler coolant** at 150,000 (footnotes: first at 100,000 / 120 months and at
    150,000 / 180 months, then every 50,000 / 60). Each is one row marked "(if applicable)", kept whole
    and table-only as the coordinator's rule for such rows says, so **neither the engine coolant nor the
    inverter coolant has a reminder from this list**. The owner may decide later to split such rows.
- Left out, because they aren't jobs at a milestone: the explanations of each item. The hybrid's chart has
  no EFI additive row.
- Reminders: engine oil 10,000 mi (5,000 for severe use, which is the guide's interval under any of its
  four special operating conditions), tire rotation 5,000, cabin and engine air filters 30,000, the hybrid
  battery's cooling filter 20,000, and spark plugs 120,000.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or carrying heavy loads.
- The inspections are in the table only, with the check line where Glovebox has one (fluid levels, wiper
  blades, brakes). The chart's "brake linings" row is one inspection at every column; its footnote's
  thickness and runout check every 30,000 isn't drawn as a job of its own.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2020–2024 are lists of their own. 2027 is outside this issue; its guide wasn't looked for.
