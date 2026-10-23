# Toyota RAV4 Hybrid, 2021–2024

How `app/src/main/assets/known-schedules/toyota-rav4-2021-2024-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the RAV4 Hybrid, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>RAV4HV/pdf/T-MMS-<yy>RAV4HV.pdf`
(`21` to `24`). Read on 2026-10-23.

- The 2022 guide's maintenance log (pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2021, 2023 and 2024 were compared page by page with a script, and every page it flagged was looked at.
  The jobs and where they fall are the same in all four. What differs is wording: "all cooling systems" in
  the fluid-level footnote (2023, 2024), "recommended oil" for "0W-16" (2024), and the islands named for
  the EFI tank additive (2024).
- The HV battery cooling intake filter is inspected at every milestone and cleaned every 20,000 miles in
  place of the inspection. In 2019 and 2020 it was cleaned only when idling for long periods, so those
  years are [a list of their own](toyota-rav4-2019-2020-hybrid.md).
- The gas RAV4 of the same years has the same 2.5L engine and a different guide. This list is marked
  `powertrain: hybrid` and labelled "2.5L hybrid": a RAV4 that vPIC decodes as a strong hybrid, or that
  its owner says is one, gets it; one with nothing on record is asked.
- The guide marks no drivetrain on any line. The rear differential oil is inspected and changed with no
  mark, so it is kept for every RAV4 Hybrid.
- **Engine and inverter coolant.** The log's line at 100,000 says "Replace engine/inverter coolant", and
  its footnote gives the engine coolant first at 100,000 miles / 120 months, then every 50,000 / 60, and
  the inverter coolant first at 150,000 / 180, then every 50,000 / 60. In the table the line is the engine
  coolant change at 100,000 (`coolant`), as the footnote has it. **The inverter coolant has no reminder.**
  Its interval is printed only in that footnote and its first change falls past the log's end
  (120,000), so the table never shows it; an `additional` reminder is only for a job the guide prints
  apart from its table (coordinator's rule of 2026-10-23, following the owner's ruling in #33). The
  engine coolant's first "or 120 months" isn't used either: it counts from the day the vehicle went
  into service, which Glovebox doesn't know.
- Left out: the EFI tank additive (island use only), the oil note, the cabin air filter note, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, under any of the guide's four
  conditions), tire rotation 5,000 / 6, HV battery cooling intake filter cleaned every 20,000 / 24, cabin
  and engine air filters 30,000 / 36, spark plugs 120,000 / 144, engine coolant first at 100,000 and then
  every 50,000 / 60.
- Only for severe use: automatic transmission fluid and rear differential oil every 60,000 / 72 when
  towing.
- Inspections are in the table only, with a check line where Glovebox has one (fluid levels, wiper
  blades, engine and inverter coolant, rear differential oil). The filter inspection has no line.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2025, which has a chart in place of the log and is [its own list](toyota-rav4-2025-hybrid.md).
