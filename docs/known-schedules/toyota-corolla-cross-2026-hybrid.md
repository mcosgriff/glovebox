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
- Everything else is as on the 2025 list's page: no months at the milestones (the oil's 12 / 6 months and the
  coolants' 60 are from their footnotes); `engine.powertrain` `hybrid` (NHTSA gives the model only as "Corolla
  Cross" for 2026, checked 2026-10-23 with `GetModelsForMakeYear`); no drivetrain marks; the hybrid battery's
  cooling filter cleaned every 20,000 as a reminder; the rows marked "(if applicable)" in the table only, with
  no service (the radiator, condenser and intercooler inspection, and the intercooler coolant); and the two
  coolant rows split as below.
- **The coolant rows are split.** The chart prints "replace the engine coolant and intercooler coolant" at
  100,000 and 150,000, and "replace the inverter coolant and intercooler coolant" at 150,000 (footnotes:
  first at 100,000 / 120 months and at 150,000 / 180 months, then every 50,000 / 60), each one row marked
  "(if applicable)". The mark belongs to the intercooler: every one of these vehicles has engine coolant
  and an inverter loop. So, under the owner's ruling of 2026-10-23 (#33), each row is two jobs in the
  table: "Replace engine coolant" (`coolant`) or "Replace inverter coolant" (`inverter-coolant`), and
  "Replace intercooler coolant (if applicable)", in the table only, once per milestone. The engine
  coolant is a reminder first at 100,000 miles and the inverter coolant first at 150,000, both then every
  50,000 miles / 60 months, as the footnotes give them. The first changes' "120 months" and "180 months"
  count from the day the vehicle went into service, which Glovebox doesn't know, so the first changes are
  by mileage only.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (a row for Hawaii only) and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use: dusty roads or short trips below
  freezing), tire rotation 5,000, cabin and engine air filters 30,000, the hybrid battery's cooling filter
  20,000, engine coolant first at 100,000 and then every 50,000 / 60, inverter coolant first at 150,000
  and then every 50,000 / 60, spark plugs 120,000, and the rear differential oil inspection 30,000.
- Only for severe use (#136): automatic transmission fluid and rear differential oil every 60,000 when
  towing or carrying heavy loads.
- **The rear differential inspection is a reminder**, at the table's own 30,000 (`rear-diff-check`).
  Under the owner's ruling of 2026-10-23 (#33), any vehicle with a differential needs it inspected, so
  every list whose guide prints the inspection makes it one; logging a change counts as its check.
- The other inspections are in the table only. The drive shaft nuts and bolts re-torque has no service
  line and is in the table only. Brake fluid has no replacement in the chart, so it has no reminder from
  this list.
- Not covered: 2027 (no Corolla Cross Hybrid guide found: `T-MMS-27CorollaCrossHV` answered 403, and
  Toyota's owners site lists only a warranty guide with no schedule).
