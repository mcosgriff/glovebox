# Toyota Sequoia, 2023–2024

How `app/src/main/assets/known-schedules/toyota-sequoia-2023-2024.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>SequoiaHV/pdf/T-MMS-<yy>SequoiaHV.pdf`
(`23` and `24`). Read on 2026-10-23. Toyota's owners site lists no warranty guide for these years; the
guides were found only at that address (#204).

- The 2023 guide's maintenance log (pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- The 2024 guide was compared page by page. The jobs and where they fall are the same. The only
  difference is the oil note, which says "recommended oil" where 2023 says "0W-20".
- The Sequoia of these years is sold only as a hybrid, with one engine (the i-FORCE MAX), so the list
  names no engine or powertrain. The gearbox is automatic only. The guide marks some lines "(4WD)"; the
  Sequoia is a true four-wheel-drive truck, so those jobs are in `fourWheelDriveOnly`, say `(4WD)`, and are
  left out for a two-wheel-drive Sequoia.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected every 5,000 miles and cleaned every
  20,000 (`hybrid-battery-filter`). At 100,000 miles the guide prints one line for the engine and inverter
  coolant and one for the intercooler coolant; each coolant is its own line here (`coolant`,
  `inverter-coolant`, `intercooler-coolant`).
- The guide's footnote puts the first inverter coolant change at 150,000 miles, which disagrees with the
  log's own line at 100,000 and is past the log's end. The table keeps the line where the log prints it,
  and inverter coolant has no reminder from this list. The owner's ruling of 2026-10-23 (#33) gave the
  2016–2024 Toyota hybrids an inverter coolant reminder from that footnote, but not this list: here the
  table and the footnote disagree, and the conflict is left for the owner.
- Left out: the fuel-tank additive the log lists at every milestone for Hawaii only (a job for one region,
  which the format can't name), the note that the oil is to be replaced if the recommended oil wasn't used
  at the last change, the daily checks for off-road driving, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 on dirt or dusty roads; unlike the 4Runner, towing is
  not a condition for the oil), tire rotation 5,000 / 6, cleaning the hybrid battery cooling intake filter
  20,000 / 24, cabin and engine air filters 30,000 / 36, spark plugs 40,000 / 48, and engine coolant and
  intercooler coolant each first at 100,000 miles and then every 50,000 / 60. The months for the coolants
  are the footnotes'.
- Inspections that are reminders too: front differential oil (4WD) and rear differential oil every
  15,000 / 18, transfer case oil (4WD) every 30,000 / 36. Logging a change of one counts as its check. This
  is the owner's ruling of 2026-10-23 (#33): any vehicle with a differential needs it inspected, so the
  4Runner's rule applies to every list whose guide prints the inspection. The other inspections are in the
  table only, with a check line where there is one: fluid levels, wiper blades, drive belts, engine and
  inverter coolant.
- Only for severe use (#136): front differential oil every 15,000 / 18 and rear differential oil every
  30,000 / 36 when towing or carrying heavy loads (the 4Runner's rear is 15,000); transfer case oil every
  30,000 / 36 on dusty roads or when towing; automatic transmission fluid every 60,000 / 72 when towing.
- In the table only: re-torquing the propeller shaft bolt (every 15,000, and at the other milestones on
  dusty or salted roads or when towing) and tightening the chassis and body nuts and bolts.
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2022 and earlier (the previous generation, a gasoline V8 whose guides are separate), 2025 on
  (a chart in place of the log: [the 2025–2026 list](toyota-sequoia-2025-2026.md)).
