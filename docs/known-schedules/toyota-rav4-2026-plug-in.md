# Toyota RAV4 Plug-in Hybrid, 2026

How `app/src/main/assets/known-schedules/toyota-rav4-2026-plug-in.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). The hybrid of the same year has its own list,
[toyota-rav4-2026-hybrid](toyota-rav4-2026-hybrid.md), and most of what is said there holds here too.

Source: Toyota's Warranty and Maintenance Guide for the 2026 RAV4 Plug-in Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-26RAV4PHV/pdf/T-MMS-26RAV4PHV.pdf` (52 pages,
dated January 2026). Read on 2026-10-23.

- The guide's maintenance chart (PDF pages 40 to 45) was read in full, by eye, and the list written from it.
  A script that reads each cell's position on the page gave the same grid. Its cells were also compared
  with the hybrid's guide: they are the same except for the rows below.
- **The layout is a chart**: every 5,000 miles from 5,000 to 150,000 (30 milestones), each page followed by
  a "Special Operating Conditions" page. **No months on the columns**, so the milestones have none; the
  chart stops at 150,000 and doesn't repeat. Months on a reminder only where the guide prints them for that
  job: oil under special conditions, 5,000 miles or six months, and the coolants, 60 months for every change
  after the first (their footnotes). The first change's months count from the in-service date, which
  Glovebox doesn't know, so the first change is by mileage only.
- **Engine, powertrain and model name.** The guide names no engine size. Toyota's newsroom
  (`https://pressroom.toyota.com/vehicle/2026-toyota-rav4-plug-in-hybrid/`) gives it the same 2.5-litre
  four-cylinder as the hybrid, so both lists carry `powertrain` (`plug-in` here). vPIC's model list for
  Toyota 2026 (`GetModelsForMakeYear`, read 2026-10-23) has `RAV4` and `RAV4 Prime (PHEV)` (as do 2025 and
  2027). Toyota now calls the car the RAV4 Plug-in Hybrid, and whether a 2026 VIN decodes to `RAV4` or to
  `RAV4 Prime (PHEV)` wasn't checked (no VIN was sent). So the list is for `RAV4` with the `plug-in`
  powertrain, and names `RAV4 Prime (PHEV)` in `modelAliases`, which is in vPIC's 2026 list and can only be
  this car in that year.
- **Drivetrain.** Toyota sells the plug-in with all-wheel drive only, and the chart has no drive marks and
  no "(if applicable)" on the rear differential oil (the hybrid's has). So the rear differential oil rows are
  ordinary: inspected every 30,000, a reminder with the `rear-diff-check` line (logging a change counts as
  its check), and replaced at 60,000 and 120,000 under towing or heavy loads, which is a reminder only for
  severe use (`severeOnly`, every 60,000). By the owner's ruling of 2026-10-23 (#33) any vehicle with a
  differential needs it inspected, so the 4Runner's rule applies wherever a guide prints the inspection.
- **Traction battery coolant:** inspected every 15,000 and replaced at 120,000 (a footnote: first at
  120,000, then every 50,000). Glovebox has no catalog line for it, and the next change after 120,000 falls
  past the chart's end, so **the replacement is table-only, with no `service` and no reminder**; the
  inspections are table-only too.
- **"Add A/C refrigerant (ZEV states only)"** at 80,000 (a footnote: every 80,000 miles or 96 months, in
  states that have adopted the ZEV regulation). Table-only, with no `service` and no reminder: it applies
  only in some states and Glovebox doesn't know which state a car is in.
- **Coolant rows are split** (the owner's ruling of 2026-10-23, #33). The "(if applicable)" mark belongs to
  the intercooler: the plug-in has an engine coolant and an inverter coolant.
  - "Engine Coolant/Intercooler Coolant (if applicable)", replaced at 100,000 and 150,000, is two table jobs:
    "Replace engine coolant" (`coolant`) and "Replace intercooler coolant (if applicable)" (table only). The
    footnote gives the engine coolant first at 100,000 or 120 months, then every 50,000 or 60, so it is a
    reminder first at 100,000 then every 50,000 miles / 60 months.
  - "Inverter Coolant/Intercooler Coolant (if applicable)", replaced at 150,000, is "Replace inverter
    coolant" (`inverter-coolant`) with the same intercooler row (one intercooler row per milestone). It is a
    reminder first at 150,000 then every 50,000 miles / 60 months, as the guide's footnote gives it.
- **"(if applicable)" row, table-only, no reminder** (the owner's ruling of 2026-10-23, #33):
  "Radiator/Condenser/Intercooler (if applicable)", an inspection.
- The plug-in has no hybrid battery cooling intake filter row.
- **Reminders:** engine oil 10,000 (5,000 / 6 mo under special conditions), tire rotation 5,000, cabin and
  engine air filters 30,000, spark plugs 120,000, engine coolant first at 100,000 then every 50,000 / 60,
  inverter coolant first at 150,000 then every 50,000 / 60, rear differential oil inspection 30,000; for severe
  use only, rear differential oil 60,000.
- **Special operating conditions** are in the table named with their condition, as on the hybrid's list;
  where a special page only repeats a standard cell, the job is listed once.
- "Add EFI tank additive (island use only)", every 5,000: table-only, no `service`.
- The other inspections are in the table only, with a check line where one exists. Brake fluid has no
  replacement in the chart, so no reminder.
- **Not covered:** 2027 (no guide at `T-MMS-27RAV4PHV`, 403 on 2026-10-23). The 2025 and earlier RAV4 Prime
  is an earlier generation with guides of its own, and isn't part of this list.
