# Toyota Highlander 2.5L hybrid, 2024

How `app/src/main/assets/known-schedules/toyota-highlander-2024-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2024 Warranty and Maintenance Guide for the Highlander Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-24HighlanderHV/pdf/T-MMS-24HighlanderHV.pdf`.
Toyota's owners site lists no guide for the hybrid; it was found only at that address. Downloaded on
2026-10-22 and read on 2026-10-23.

- The guide's maintenance log (printed pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was compared page by page with the 2020 log, which was read in full for
  [the 2020–2023 list](toyota-highlander-2020-2023-hybrid.md), and every page that differed was read.
- **One difference in the jobs.** Under special operating conditions the 2024 log replaces the engine oil
  only on dirt or dusty roads; the 2020–2023 logs also list it when towing, on short trips below freezing
  and when idling long, and the 2024 log drops those last two headings. The guide's explanation of the oil
  change at the back still speaks of any of the listed conditions; the list follows the log. Otherwise the
  jobs and where they fall are the same as 2020–2023. The wording differs too: "recommended oil" for
  "0W-16" in the oil note.
- **The engine:** 2.5-litre hybrid, as on [the 2020–2023 page](toyota-highlander-2020-2023-hybrid.md),
  labelled "2.5L hybrid" (`displacementL` 2.5). The 2024 petrol Highlander is a 2.4-litre turbo, so no
  `powertrain` is needed.
- **"(4WD)" read as "(AWD)"**, as on the 2020–2023 list: the guide prints "(4WD)" on the rear differential
  oil, and these jobs are in `allWheelDriveOnly`, named "(AWD)" (the owner's ruling of 2026-10-23, #33).
- **Engine and inverter coolant** as on the 2020–2023 list: the 100,000 line is the engine coolant
  (`coolant`); the inverter coolant, first at 150,000 by footnote only, past the log's end, has no
  reminder.
- In the table only: the EFI tank additive (Hawaii and Puerto Rico only), the chassis and body nuts and
  bolts, and every inspection. Left out: the oil note, the cabin air filter note, and the explanations.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the log's interval on dusty roads),
  tire rotation 5,000 / 6, HV battery cooling intake filter cleaned every 20,000 / 24, cabin and engine air
  filters 30,000 / 36, spark plugs 120,000 / 144, engine coolant first at 100,000 and then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid and rear differential oil (AWD) every 60,000 / 72
  when towing or carrying heavy loads.
- Inspections are in the table only, with a check line where Glovebox has one (fluid levels, wiper blades,
  engine and inverter coolant, rear differential oil).
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2025 on, which print a chart in place of the log ([2025](toyota-highlander-2025-hybrid.md),
  [2026](toyota-highlander-2026-hybrid.md)).
