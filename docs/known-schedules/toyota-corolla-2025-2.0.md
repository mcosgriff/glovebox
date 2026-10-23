# Toyota Corolla 2.0L, 2025

How `app/src/main/assets/known-schedules/toyota-corolla-2025-2.0.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guides for 2025, read on 2026-10-23: the sedan's,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-25Corolla/pdf/T-MMS-25Corolla.pdf`, and the
hatchback's,
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-25CorollaHB/pdf/T-MMS-25CorollaHB.pdf`.

- **A chart, not a log.** From 2025 the guide gives the schedule as a maintenance chart: jobs are rows and
  mileages are columns, every 5,000 from 5,000 to 150,000, on three pages, each with a page for special
  operating conditions beside it. The sedan guide's six chart pages (PDF pages 39 to 44) were read in full
  and the list written from them. The list has 30 milestones, to 150,000 miles.
- The hatchback's six chart pages (PDF pages 39 to 44) were read on the page as well, and a script that
  reads each chart's cells held both guides to this list. The cells are the same; what differs is wording
  (the EFI additive row is "Hawaii and Puerto Rico only" on the hatchback) and footnote numbering.
- **Sedan and hatchback are one list.** NHTSA gives both as "Corolla" (`GetModelsForMakeYear` for Toyota,
  2025, checked 2026-10-23: no "Corolla Hatchback"), and their charts agree. The gas cars have the 2.0L
  only and the chart names no engine; the Corolla Hybrid's 1.8L has a list of its own
  ([toyota-corolla-2025-2026-hybrid](toyota-corolla-2025-2026-hybrid.md)), so this list has `engine` 2.0L
  and needs no powertrain.
- **No months at the milestones.** The chart's columns carry only mileages, so the milestones have no
  months. A reminder keeps months only where the guide prints them for that job: the engine oil's
  footnote gives 12 months (6 under severe use), and the engine coolant's gives 60 months for every
  change after the first (below). The other reminders are by distance only.
- **Rows marked "(if applicable)"** are in the table as printed, with "(if applicable)" in their names and
  no service line, so none of them drives a reminder (the owner's ruling of 2026-10-23):
  - inspect the radiator, condenser and intercooler every 15,000;
  - the intercooler's part of the coolant row (below).
- **The coolant row is split.** The chart prints "replace the engine coolant and intercooler coolant" as
  one row marked "(if applicable)", at 100,000 and 150,000 (its footnote: first at 100,000 miles /
  120 months, then every 50,000 / 60). The mark belongs to the intercooler: every one of these cars has
  engine coolant. So, under the owner's ruling of 2026-10-23 (#33), the row is two jobs in the table:
  "Replace engine coolant" (`coolant`), a reminder first at 100,000 miles and then every 50,000 miles /
  60 months, and "Replace intercooler coolant (if applicable)", in the table only. The first change's "120
  months" counts from the day the car went into service, which Glovebox doesn't know, so the first change
  is by mileage only.
- **Automatic transmission fluid** is replaced at 60,000 and 120,000 when towing or idling for long periods
  (one cell, with both footnotes), so it is one line, "· towing or long idling".
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (a row for Hawaii, Puerto Rico and Saipan only) and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval under
  any of its four special operating conditions), tire rotation 5,000, cabin and engine air filters 30,000,
  engine coolant first at 100,000 and then every 50,000 / 60, and spark plugs 120,000.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or idling for long
  periods.
- The inspections are in the table only, with the check line where Glovebox has one (fluid levels, wiper
  blades, brakes, drive belts). The drive belts are inspected at 60,000 and then every 15,000; the brake
  booster's vacuum pump at 120,000. The chart's "brake linings" row is one inspection at every column; its
  footnote's thickness and runout check every 30,000 isn't drawn as a job of its own.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- **Not covered: 2026.** The 2026 sedan's chart agrees with 2025's (by the words of its pages, and on its
  PDF page 40, seen on the page), but the 2026 hatchback's
  special operating conditions differ (seen on its PDF page 40: the dusty-road inspections at every 5,000,
  the engine oil replaced at every 5,000, the engine air filter replaced at 30,000, and an automatic
  transmission fluid inspection at 30,000, none of which the sedan has). NHTSA gives both as "Corolla" with
  the same 2.0L engine, so no list can tell a 2026 sedan from a 2026 hatchback, and none is made for 2026.
  2019–2024 are lists of their own ([2019–2022](toyota-corolla-2019-2022-2.0.md),
  [2023–2024](toyota-corolla-2023-2024-2.0.md)).
