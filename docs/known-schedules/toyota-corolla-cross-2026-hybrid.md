# Toyota Corolla Cross Hybrid, 2026

How `app/src/main/assets/known-schedules/toyota-corolla-cross-2026-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2026 Corolla Cross Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-26CorollaCrossHV/pdf/T-MMS-26CorollaCrossHV.pdf`.
Read on 2026-10-23. (Toyota's owners site lists no guide for the hybrid; this address is the issue's.)

- The guide's six maintenance chart pages (PDF pages 40 to 45: every 5,000 from 5,000 to 150,000, with a
  page of special operating conditions beside each) were read in full and the list written from them.
  A script that reads each chart's cells found them the same as the 2025 hybrid chart's.
- **One year only.** The difference from 2025 is in the engine oil under special conditions: this guide
  changes it every 5,000 miles on dusty roads **or for repeated trips under five miles below freezing**,
  where 2025's is for dusty roads only. The other differences are wording: "HEV" for "HV", the EFI
  additive "for Hawaii only" where 2025 says "for island use only", and footnote numbering. 2025 is a list
  of its own ([toyota-corolla-cross-2025-hybrid](toyota-corolla-cross-2025-hybrid.md)).
- Everything else is as on the 2025 list's page: no months at the milestones (the oil's 12 / 6 months are
  from its footnote); `engine.powertrain` `hybrid` (NHTSA gives the model only as "Corolla Cross" for
  2026, checked 2026-10-23 with `GetModelsForMakeYear`); no drivetrain marks; the hybrid battery's
  cooling filter cleaned every 20,000 as a reminder; and the rows marked "(if applicable)" in the table
  only, with no service: the radiator, condenser and intercooler inspection, the engine and intercooler
  coolant at 100,000 and 150,000, and the inverter and intercooler coolant at 150,000. So **neither the
  engine coolant nor the inverter coolant has a reminder from this list**; each is one chart row marked
  "(if applicable)", kept whole as the coordinator's rule for such rows says.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (a row for Hawaii only) and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use: dusty roads or short trips below
  freezing), tire rotation 5,000, cabin and engine air filters 30,000, the hybrid battery's cooling filter
  20,000, and spark plugs 120,000.
- Only for severe use (#136): automatic transmission fluid and rear differential oil every 60,000 when
  towing or carrying heavy loads.
- The inspections are in the table only; the rear differential oil inspection (every 30,000) carries the
  rear differential check line. The drive shaft nuts and bolts re-torque has no service line and is in the
  table only. Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered: 2027 (no Corolla Cross Hybrid guide found: `T-MMS-27CorollaCrossHV` answered 403, and
  Toyota's owners site lists only a warranty guide with no schedule).
