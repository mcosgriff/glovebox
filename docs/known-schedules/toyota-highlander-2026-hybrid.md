# Toyota Highlander 2.5L hybrid, 2026

How `app/src/main/assets/known-schedules/toyota-highlander-2026-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2026 Warranty and Maintenance Guide for the Highlander Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-26HighlanderHV/pdf/T-MMS-26HighlanderHV.pdf`.
Toyota's owners site lists no guide for the hybrid; it was found only at that address. Downloaded on
2026-10-22 and read on 2026-10-23.

- **A chart**, laid out as the 2025 one (printed pages 38 to 43). It was compared cell by cell with the 2025
  chart, which was read in full for [the 2025 list](toyota-highlander-2025-hybrid.md), and the
  5,000–50,000 pages were read here as well.
- **What differs from 2025:**
  - the oil change under special conditions: its footnote now says dirt or dusty roads **or repeated short
    trips below freezing** (2025: dirt or dusty roads only), so the job is named for both;
  - the EFI tank additive is for Hawaii and Puerto Rico (2025: "island use only");
  - "HEV battery" for "HV battery";
  - the special charts repeat cells the standard chart already has (the oil change at every column, the air
    filter replacement at the 30,000s, the ball joints, boots and steering linkage at the 15,000s, the
    transmission fluid and rear differential oil inspections at 30,000, 90,000 and 150,000). Such a job is in the table
    once, as the standard chart's; the table is the same as if they weren't repeated, and nothing else in
    the special charts differs from 2025.
- **The engine:** 2.5-litre hybrid, as on [the 2020–2023 page](toyota-highlander-2020-2023-hybrid.md),
  labelled "2.5L hybrid" (`displacementL` 2.5).
- Everything else is as on [the 2025 page](toyota-highlander-2025-hybrid.md): no months on the milestones;
  the rear differential oil and intercooler "(if applicable)" rows in the table with no `service` (the
  owner's ruling of 2026-10-23, #33), so no rear differential reminder; the two combined coolant rows
  ("Engine Coolant / Intercooler Coolant (if applicable)" at 100,000 and 150,000, "Inverter Coolant /
  Intercooler Coolant (if applicable)" at 150,000) split, by the same ruling, into "Replace engine coolant"
  (`coolant`) or "Replace inverter coolant" (`inverter-coolant`) and "Replace intercooler coolant (if
  applicable)" (table only), since the mark belongs to the intercooler and every Highlander Hybrid has
  engine coolant and an inverter loop; the battery filter cleaned every 20,000.
- In the table only: the EFI tank additive (Hawaii and Puerto Rico only), the chassis nuts and bolts, and
  every inspection.
- Reminders: engine oil every 10,000 miles / 12 months (5,000 / 6 for severe use: dirt or dusty roads, or
  short trips below freezing), tire rotation every 5,000, hybrid battery cooling intake filter cleaned every
  20,000, cabin and engine air filters every 30,000, spark plugs every 120,000, engine coolant first at
  100,000 and inverter coolant first at 150,000, each then every 50,000 / 60 (their footnotes' months
  after the first change; the first change is by mileage only, as on the 2025 page).
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or carrying heavy loads.
- Inspections are in the table only, with a check line where Glovebox has one (fluid levels, wiper blades).
- Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2027 on (`27HighlanderHV` refused at that address on 2026-10-22).
