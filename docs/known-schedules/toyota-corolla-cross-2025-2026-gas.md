# Toyota Corolla Cross (gas), 2025–2026

How `app/src/main/assets/known-schedules/toyota-corolla-cross-2025-2026-gas.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>CorollaCross/pdf/T-MMS-<yy>CorollaCross.pdf`
(`25` and `26`; found through Toyota's owners site). Read on 2026-10-23.

- **A chart, not a log.** From 2025 the guide gives the schedule as a maintenance chart: jobs are rows and
  mileages are columns, every 5,000 from 5,000 to 150,000, on three pages, each with a page for special
  operating conditions beside it. The 2025 guide's six chart pages (PDF pages 39 to 44) were read in full
  and the list written from them. The list has 30 milestones, to 150,000 miles.
- 2026 was compared with 2025 by a script that reads each chart's cells, and by the words of the pages.
  The cells are the same; what differs is footnote numbering, the EFI additive's footnote (gone in 2026)
  and the spacing of one row's name.
- **No months at the milestones.** The chart's columns carry only mileages, so the milestones have no
  months. A reminder keeps months only where the guide prints them for that job: the engine oil's
  footnote gives 12 months (6 under severe use), and the engine coolant's gives 60 months for every
  change after the first (below). The other reminders are by distance only.
- **Gas, not hybrid.** The Corolla Cross Hybrid has a 2.0L engine too and a guide of its own, and NHTSA
  gives both as "Corolla Cross" (`GetModelsForMakeYear` for Toyota, 2025 and 2026, checked 2026-10-23:
  no separate hybrid model). So this list has `engine.powertrain` `gas` and the hybrid lists have
  `hybrid`; a 2.0L Corolla Cross whose powertrain isn't on record is asked which it is.
- **Rows marked "(if applicable)"** are in the table as printed, with "(if applicable)" in their names
  and no service line, so none of them drives a reminder (the owner's ruling of 2026-10-23). The chart
  names no drivetrain, where the 2022–2024 logs marked the same jobs "(AWD)", so nothing here is
  `allWheelDriveOnly`. These rows are:
  - re-torque the propeller shaft bolts every 15,000, and at every other milestone on dusty or salted
    roads or when towing;
  - inspect the rear differential oil and the transfer case oil every 15,000, and replace both every
    15,000 when towing (reminders on the 2022–2024 lists, the inspections for all-wheel drive and the
    changes for severe use; here they are not, by the owner's ruling of 2026-10-23, #33, that rows
    marked "(if applicable)" have no service);
  - inspect the radiator, condenser and intercooler every 15,000;
  - the intercooler's part of the coolant row (below).
- **The coolant row is split.** The chart prints "replace the engine coolant and intercooler coolant" as
  one row marked "(if applicable)", at 100,000 and 150,000 (its footnote: first at 100,000 miles /
  120 months, then every 50,000 / 60). The mark belongs to the intercooler: every one of these vehicles
  has engine coolant. So, under the owner's ruling of 2026-10-23 (#33), the row is two jobs in the table:
  "Replace engine coolant" (`coolant`), a reminder first at 100,000 miles and then every 50,000 miles /
  60 months, and "Replace intercooler coolant (if applicable)", in the table only. The first change's "120
  months" counts from the day the vehicle went into service, which Glovebox doesn't know, so the first
  change is by mileage only.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (a row for Hawaii only) and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval on
  dusty roads or for repeated short trips below freezing), tire rotation 5,000, cabin and engine air
  filters 30,000, engine coolant first at 100,000 and then every 50,000 / 60, and spark plugs 120,000.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or idling for long
  periods.
- The brake booster's vacuum pump is replaced at 120,000; Glovebox has no line for it and it is a
  one-off within the chart, so it is in the table only. The inspections, drive belts (60,000 and then
  every 15,000) included, are in the table only. The chart's "brake linings" row is one inspection at
  every column; its footnote's thickness and runout check every 30,000 isn't drawn as a job of its own.
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2027. Toyota's owners site lists only a warranty guide for all its gas, hybrid and plug-in
  models (`T-MMS-27GHVPHV`), with no maintenance schedule, and `T-MMS-27CorollaCross` answered 403.
  2022–2024 are lists of their own ([2022](toyota-corolla-cross-2022.md),
  [2023–2024](toyota-corolla-cross-2023-2024-gas.md)).
