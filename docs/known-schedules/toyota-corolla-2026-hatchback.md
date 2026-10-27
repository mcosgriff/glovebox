# Toyota Corolla hatchback, 2026

How `app/src/main/assets/known-schedules/toyota-corolla-2026-hatchback.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2026 Corolla Hatchback Warranty and Maintenance Guide (the printing online has a back cover
dated 5/26),
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-26CorollaHB/pdf/T-MMS-26CorollaHB.pdf`. Read on
2026-10-26.

- **Why a list per body (#185).** NHTSA gives the hatchback and the sedan both as "Corolla" with the same 2.0L
  engine, and in 2026 their special-operating-conditions charts differ (below; the sedan's is on
  [its own page](toyota-corolla-2026-sedan.md)). So this list has `"body": "hatchback"`, as the owner ruled on
  2026-10-25, and a 2026 gas Corolla matches it only when its body style is on record as a hatchback: from
  NHTSA's "Hatchback/Liftback/Notchback", or the owner's pick. With none on record, setup asks.
- **A chart.** Jobs are rows and mileages are columns, every 5,000 from 5,000 to 150,000, on three pages, each
  with a special-operating-conditions page beside it (PDF pages 39 to 44, printed 38 to 43). **All six pages
  were read in full, as page images**, and the list written from them: 30 milestones, 5,000 miles apart. Nothing
  says the chart repeats after 150,000.
- **The standard chart** is the same, cell for cell, as the 2025 hatchback's and the 2026 sedan's (held to them
  by a script that compares each row's cells, besides the reading): only footnote numbers, the EFI additive row's
  wording ("Hawaii and Puerto Rico only") and line breaks differ.
- **The special-operating-conditions chart differs from the sedan's and from 2025's.** It marks every column the
  work is done at, standard cells included (as the 2026 Camry's does):
  - on dirt or dusty roads, the ball joints and dust covers, drive shaft boots and steering linkage are inspected
    at every column (the sedan: only where the standard chart has no inspection);
  - on dirt or dusty roads, the engine air filter is replaced at every 30,000 and inspected at the other columns
    (the sedan: inspected only, and not at the 30,000s);
  - under any of the four conditions, the engine oil and filter are changed at every column (the sedan: at the
    odd 5,000s only);
  - when towing or idling for long periods, the automatic transmission fluid is inspected at 30,000, 90,000 and
    150,000 and replaced at 60,000 and 120,000 (the sedan: replaced at 60,000 and 120,000 only);
  - the chassis and body nuts and bolts are tightened at every column on dirt or dusty roads or when towing, as on
    the sedan.

  The table keeps those cells as drawn, each named with its condition and with no service line, except the
  transmission fluid replacement, which has its reminder (below). The dusty-roads engine air filter replacement
  falls on the standard one and has no reminder of its own.
- **The engine oil's months.** The chart has no footnote on the oil row (2025's gave the ordinary change 12
  months). The "Explanation of Maintenance Items" says the oil and filter are changed at 5,000 miles or six months
  under any of the special operating conditions, so the severe-use interval is 5,000 / 6. **The ordinary change
  has no months:** nothing in the guide gives one for it, and the general sentence that the car be seen "every
  5,000 miles or six months" is about visits, not this job. That is a gap, left as it is.
- The gas hatchback has the 2.0L only and the chart names no engine; the list has `engine` 2.0L so that a 2026
  Corolla Hybrid (1.8L, [its own list](toyota-corolla-2025-2026-hybrid.md)) never matches it.
- **No months at the milestones.** The columns carry mileages only. A reminder keeps months only where the guide
  prints them for that job: the special-conditions oil change (6) and the engine coolant after its first change
  (60, footnote).
- **Rows marked "(if applicable)"** are in the table with no service line (the owner's ruling of 2026-10-23): the
  radiator, condenser and intercooler inspection, and the intercooler's part of the coolant row.
- **The coolant row is split.** "Engine Coolant / Intercooler Coolant (if applicable)" at 100,000 and 150,000
  (footnote: first at 100,000 miles / 120 months, then every 50,000 / 60) is "Replace engine coolant"
  (`coolant`), a reminder first at 100,000 miles and then every 50,000 / 60 months, and "Replace intercooler
  coolant (if applicable)", in the table only. The first change's "120 months" counts from the day the car went
  into service, which Glovebox doesn't know, so the first change is by mileage only.
- Left out: adding Toyota's EFI tank additive (a row for Hawaii and Puerto Rico only), the notes on oil
  selection, and the explanations of each item.
- Reminders: engine oil 10,000 mi (5,000 / 6 for severe use), tire rotation 5,000, cabin and engine air filters
  30,000, engine coolant first at 100,000 and then every 50,000 / 60, and spark plugs 120,000.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or idling for long periods.
- The inspections are in the table only, with the check line where Glovebox has one (fluid levels, wiper blades,
  brakes, drive belts). The drive belts are inspected at 60,000 and then every 15,000; the brake booster's
  vacuum pump at 120,000. Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Not covered here: the 2026 sedan ([its own list](toyota-corolla-2026-sedan.md)), and 2019–2025 (lists of their
  own; [2025](toyota-corolla-2025-2.0.md) is one list for both bodies, whose charts agree that year).
