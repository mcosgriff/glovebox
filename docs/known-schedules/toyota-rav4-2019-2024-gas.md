# Toyota RAV4 (gas), 2019–2024

How `app/src/main/assets/known-schedules/toyota-rav4-2019-2024-gas.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>RAV4/pdf/T-MMS-<yy>RAV4.pdf`
(`19` to `24`). Read on 2026-10-23. Toyota prints a separate guide for the hybrid and the plug-in; those
are their own lists ([2019–2020 hybrid](toyota-rav4-2019-2020-hybrid.md),
[2021–2024 hybrid](toyota-rav4-2021-2024-hybrid.md), [2021–2024 Prime](toyota-rav4-prime-2021-2024.md)).

- The 2022 guide's maintenance log (pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2019, 2020, 2021, 2023 and 2024 were compared page by page with a script, and every page it flagged
  was looked at. The jobs and where they fall are the same in all six. What differs is wording: "air flow"
  for "airflow" (2019, 2020); "Radiator, condenser and/or intercooler" and "all cooling systems" in the
  fluid-level footnote (2023, 2024); "recommended oil" for "0W-16" in the oil note (2024).
- **The 45,000 milestone has no months.** Every one of the six guides prints it as "45,000 miles or
  18 months", where the pattern of the log (6 months to every 5,000 miles) makes 54. By the owner's ruling
  of 2026-10-23 (#33) the milestone keeps its mileage and the misprinted months are left off; nothing is
  corrected.
- **"(AWD)" where the guide prints "(4WD)".** Toyota sells this RAV4 with front- or all-wheel drive. The
  guide marks the propeller shaft bolt, the transfer case oil and the rear differential oil "(4WD)" and has
  no "AWD" or "2WD" lines. By the owner's ruling of 2026-10-23 those jobs are named "(AWD)" here and listed
  in `allWheelDriveOnly`: a RAV4 known to be front-wheel drive doesn't get them; one on record as AWD, or
  with no drive type, does.
- One engine, the 2.5L four-cylinder, in a gas and a hybrid version with different schedules. The list is
  marked `powertrain: gas` and labelled "2.5L gas", so a RAV4 is only matched to it once its owner, or
  vPIC's decode, says it isn't a hybrid; otherwise setup asks.
- Left out: the EFI tank additive (Hawaii, Puerto Rico, Saipan and American Samoa only), the note that
  the oil is to be replaced at 5,000 miles if 0W-16 wasn't used at the last change, the cabin air filter
  note, and the explanations of each item.
- The brake booster vacuum pump at 120,000 is in the table only, with no `service` and no reminder: the
  guide inspects it and its footnote says to replace the vane and caps, once, and Glovebox has no line
  for that part.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, under any of the guide's four
  conditions), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, transfer case oil (AWD)
  30,000 / 36 (15,000 / 18 for severe use: towing), spark plugs 120,000 / 144, and engine coolant first
  at 100,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the first coolant change
  isn't used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Only for severe use: rear differential oil (AWD) every 15,000 / 18 when towing; automatic transmission
  fluid every 60,000 / 72 when towing or idling for long periods.
- Inspections are in the table only, with a check line where Glovebox has one (fluid levels, wiper
  blades, engine coolant, drive belts, rear differential and transfer case oil). Drive belts are first
  inspected at 60,000, then every 15,000. The propeller shaft bolt re-torque has no line and stays in the
  table.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2018 and earlier (the previous generation; `18RAV4` is at the same address but wasn't
  read), and 2025, which has a chart in place of the log and is [its own list](toyota-rav4-2025-gas.md).
