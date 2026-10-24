# Toyota RAV4 Prime, 2021–2024

How `app/src/main/assets/known-schedules/toyota-rav4-prime-2021-2024.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the RAV4 Prime, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>RAV4Prime/pdf/T-MMS-<yy>RAV4Prime.pdf`
(`21` to `24`). Read on 2026-10-23.

- The 2022 guide's maintenance log (pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2021, 2023 and 2024 were compared page by page with a script, and every page it flagged was looked at.
  The jobs and where they fall are the same in all four. What differs is wording: "all cooling systems" in
  the fluid-level footnote (2023, 2024) and "recommended oil" for "0W-16" (2024).
- **The model is matched by its name.** vPIC's model list for Toyota (`GetModelsForMakeYear`, checked
  2026-10-23) gives "RAV4 Prime (PHEV)" for every year from 2021 to 2025, apart from "RAV4"; that is the
  list's `model`, and it needs no powertrain. An owner who typed "RAV4 Prime" by hand gets no list, not a
  wrong one.
- The guide marks no drivetrain on any line. The rear differential oil is inspected and changed with no
  mark, so it is kept for every Prime.
- **The DC/DC converter air intake filter** is inspected at every milestone and cleaned every 20,000 miles
  in place of the inspection. It is in the table only, with no `service` and no reminder: Glovebox has no
  line for that filter.
- **Air conditioner refrigerant** is filled at 80,000, in the table with the air conditioning line. The
  guide's footnote fills it again every 80,000 miles / 96 months after that only in states that have
  adopted the ZEV regulation; that repeat is regional, so it is left out and there is no reminder.
- **Engine and inverter coolant.** The log's line at 100,000 says "Replace engine/inverter coolant", and
  its footnote gives the engine coolant first at 100,000 miles / 120 months, then every 50,000 / 60, and
  the inverter coolant first at 150,000 / 180, then every 50,000 / 60. In the table the line is the engine
  coolant change at 100,000 (`coolant`), as the footnote has it. The inverter coolant's interval is
  printed only in that footnote and its first change falls past the log's end (120,000), so it is an
  `additional` job ("Replace inverter coolant", `inverter-coolant`, first at 150,000 then every
  50,000 / 60), never placed on the table (the owner's ruling of 2026-10-23, #33). The first changes'
  "or 120 months" and "or 180 months" aren't used: they count from the day the vehicle went into service,
  which Glovebox doesn't know.
- Left out: the EFI tank additive (Hawaii only), the oil note, the cabin air filter note, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, under any of the guide's four
  conditions), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark plugs
  120,000 / 144, engine coolant first at 100,000 and then every 50,000 / 60, rear differential oil
  inspection 30,000 / 36. Apart from the table: inverter coolant first at 150,000 then every 50,000 / 60.
- Only for severe use: automatic transmission fluid and rear differential oil every 60,000 / 72 when
  towing.
- **The rear differential oil inspection is a reminder**, at the table's own 30,000 / 36, with the
  `rear-diff-check` line; logging a change counts as its check. By the owner's ruling of 2026-10-23 (#33)
  any vehicle with a differential needs it inspected, so the 4Runner's rule applies wherever a guide prints
  the inspection.
- The other inspections are in the table only, with a check line where Glovebox has one (fluid levels,
  wiper blades, engine and inverter coolant).
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2025, which has a chart in place of the log and is
  [its own list](toyota-rav4-prime-2025.md).
