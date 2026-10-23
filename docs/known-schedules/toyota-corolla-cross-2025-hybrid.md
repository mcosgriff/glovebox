# Toyota Corolla Cross Hybrid, 2025

How `app/src/main/assets/known-schedules/toyota-corolla-cross-2025-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2025 Corolla Cross Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-25CorollaCrossHV/pdf/T-MMS-25CorollaCrossHV.pdf`.
Read on 2026-10-23. (Toyota's owners site lists no guide for the hybrid; this address is the issue's.)

- **A chart, not a log.** From 2025 the guide gives the schedule as a maintenance chart: jobs are rows and
  mileages are columns, every 5,000 from 5,000 to 150,000, on three pages, each with a page for special
  operating conditions beside it. The six chart pages (PDF pages 40 to 45) were read in full and the
  list written from them. The list has 30 milestones, to 150,000 miles.
- **One year only.** The 2026 hybrid chart has the same cells, but its engine oil under special
  conditions is for dusty roads or repeated short trips below freezing, where 2025's is for dusty roads
  only. 2026 is a list of its own ([toyota-corolla-cross-2026-hybrid](toyota-corolla-cross-2026-hybrid.md)).
- **No months at the milestones.** The chart's columns carry only mileages, so the milestones have no
  months. A reminder keeps months only where the guide prints them for that job: the engine oil's
  footnote gives 12 months (6 under severe use), and the two coolants' give 60 months for every change
  after the first (below). The other reminders are by distance only.
- **Hybrid, not gas.** The gas Corolla Cross has a 2.0L engine too and a guide of its own, and NHTSA gives
  both as "Corolla Cross" (`GetModelsForMakeYear` for Toyota, 2025, checked 2026-10-23: no separate
  hybrid model). So this list has `engine.powertrain` `hybrid` and the gas list has `gas`; a 2.0L
  Corolla Cross whose powertrain isn't on record is asked which it is. The chart names no engine and
  marks no drivetrain.
- **The hybrid battery's cooling filter.** The chart inspects it at every column and cleans it ("C") at
  every 20,000; its footnote says the same. The clean is a reminder every 20,000, logged with the hybrid
  battery cooling filter line; the inspection is in the table only.
- **Rows marked "(if applicable)"** are in the table as printed, with "(if applicable)" in their names
  and no service line, so none of them drives a reminder (the owner's ruling of 2026-10-23):
  - inspect the radiator, condenser and intercooler every 15,000;
  - the intercooler's part of the two coolant rows (below).
- **The coolant rows are split.** The chart prints "replace the engine coolant and intercooler coolant" at
  100,000 and 150,000, and "replace the inverter coolant and intercooler coolant" at 150,000 (footnotes:
  first at 100,000 / 120 months and at 150,000 / 180 months, then every 50,000 / 60), each one row marked
  "(if applicable)". The mark belongs to the intercooler: every one of these vehicles has engine coolant
  and an inverter loop. So, under the owner's ruling of 2026-10-23 (#33), each row is two jobs in the
  table: "Replace engine coolant" (`coolant`) or "Replace inverter coolant" (`inverter-coolant`), and
  "Replace intercooler coolant (if applicable)", in the table only, once per milestone. The engine
  coolant is a reminder first at 100,000 miles and the inverter coolant first at 150,000, both then every
  50,000 miles / 60 months, as the footnotes give them. The first changes' "120 months" and "180 months"
  count from the day the vehicle went into service, which Glovebox doesn't know, so the first changes are
  by mileage only.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (a row "for island use only") and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval on
  dusty roads), tire rotation 5,000, cabin and engine air filters 30,000, the hybrid battery's cooling
  filter 20,000, engine coolant first at 100,000 and then every 50,000 / 60, inverter coolant first at
  150,000 and then every 50,000 / 60, spark plugs 120,000, and the rear differential oil inspection 30,000.
- Only for severe use (#136): automatic transmission fluid and rear differential oil every 60,000 when
  towing or carrying heavy loads.
- **The rear differential inspection is a reminder**, at the table's own 30,000 (`rear-diff-check`).
  Under the owner's ruling of 2026-10-23 (#33), any vehicle with a differential needs it inspected, so
  every list whose guide prints the inspection makes it one; logging a change counts as its check.
- The other inspections are in the table only. The drive shaft nuts and bolts re-torque (every 15,000, and
  at every other milestone on dusty roads) has no service line and is in the table only. The chart's
  "brake linings" row is one inspection at every column; its footnote's thickness and runout check every
  30,000 isn't drawn as a job of its own.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2027 (no Corolla Cross Hybrid guide found: `T-MMS-27CorollaCrossHV` answered 403, and
  Toyota's owners site lists only a warranty guide with no schedule). 2023 and 2024 are lists of their
  own ([2023](toyota-corolla-cross-2023-hybrid.md), [2024](toyota-corolla-cross-2024-hybrid.md)).
