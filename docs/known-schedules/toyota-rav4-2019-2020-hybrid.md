# Toyota RAV4 Hybrid, 2019–2020

How `app/src/main/assets/known-schedules/toyota-rav4-2019-2020-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the RAV4 Hybrid, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>RAV4HV/pdf/T-MMS-<yy>RAV4HV.pdf`
(`19`, `20`). Read on 2026-10-23.

- The 2020 guide's maintenance log (pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2019 was compared page by page with a script: the same jobs at the same milestones; only "air flow"
  for "airflow" in the cabin filter note differs.
- **Why 2019–2020 is a list apart from 2021–2024:** in these two guides the HV battery cooling intake
  filter is cleaned only under the extended-idling condition, at every milestone. From 2021 it is
  inspected every 5,000 and cleaned every 20,000 as ordinary jobs ([2021–2024](toyota-rav4-2021-2024-hybrid.md)).
- The gas RAV4 of the same years has the same 2.5L engine and a different guide. This list is marked
  `powertrain: hybrid` and labelled "2.5L hybrid": a RAV4 that vPIC decodes as a strong hybrid, or that
  its owner says is one, gets it; one with nothing on record is asked.
- The guide marks no drivetrain on any line. The rear differential oil is inspected and changed with no
  mark, so it is kept for every RAV4 Hybrid.
- **Engine and inverter coolant.** The log's line at 100,000 says "Replace engine/inverter coolant", and
  its footnote gives the engine coolant first at 100,000 miles / 120 months, then every 50,000 / 60, and
  the inverter coolant first at 150,000 / 180, then every 50,000 / 60. In the table the line is the engine
  coolant change at 100,000 (`coolant`), as the footnote has it. The inverter coolant's interval is
  printed only in that footnote and its first change falls past the log's end (120,000), so it is an
  `additional` job ("Replace inverter coolant", `inverter-coolant`, first at 150,000 then every
  50,000 / 60), never placed on the table (the owner's ruling of 2026-10-23, #33). The first changes'
  "or 120 months" and "or 180 months" aren't used: they count from the day the vehicle went into service,
  which Glovebox doesn't know.
- Left out: the EFI tank additive (Hawaii, Puerto Rico, Saipan and American Samoa only), the 0W-16 oil
  note, the cabin air filter note, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, under any of the guide's four
  conditions), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark plugs
  120,000 / 144, engine coolant first at 100,000 and then every 50,000 / 60, rear differential oil
  inspection 30,000 / 36. Apart from the table: inverter coolant first at 150,000 then every 50,000 / 60.
- Only for severe use: clean the HV battery cooling intake filter every 5,000 / 6 when idling for long
  periods; automatic transmission fluid and rear differential oil every 60,000 / 72 when towing.
- **The rear differential oil inspection is a reminder**, at the table's own 30,000 / 36, with the
  `rear-diff-check` line; logging a change counts as its check. By the owner's ruling of 2026-10-23 (#33)
  any vehicle with a differential needs it inspected, so the 4Runner's rule applies wherever a guide prints
  the inspection.
- The other inspections are in the table only, with a check line where Glovebox has one (fluid levels,
  wiper blades, engine and inverter coolant).
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2018 and earlier (not read), 2021 on (the lists linked above, and
  [2025](toyota-rav4-2025-hybrid.md)).
