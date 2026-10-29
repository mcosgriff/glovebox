# Volkswagen GTI, 2023–2026 (Mk8)

How `app/src/main/assets/known-schedules/volkswagen-gti-2023-2026.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissued maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`, document type
"Maintenance Card", importer `444`, `en-US`). No card from the time was found for these years (NHTSA's
index has none), so the reissues are the only source; the owner's ruling of 2026-10-23 (#33) allows that,
cited as reissues with their print dates. Read on 2026-10-23.

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2023 | `USA5R507521`, `…/v1/document/7f3ce02f-ee20-4e97-8747-fb18f14b9b66` | 12/03/2025 | In full, off the page; the list was written from it |
| 2024 | `USA5R505721`, `…/v1/document/2721b3eb-0228-4be4-94d6-0b8e24dbf74c` | 12/03/2025 | Compared by script, differences looked at on the page |
| 2025 | `USA5R701521`, `…/v1/document/760f5f9a-0290-4dfe-adf0-30d03981a9ad` | 12/03/2025 | The same |
| 2026 | `USA5R506821`, `…/v1/document/d063309f-f793-4d23-8799-a4ff5ffb3850` | 02/2026 | The same |

- **What differs between the years** touches only other models and the hybrid, plug-in and electric
  lines: 2024 drops the plug-in and hybrid rows and adds a severe-use automatic fluid row for the 09U
  gearbox (Jetta); 2025 and 2026 are headed "Gas Vehicles" and re-sort the applicability of the spark
  plug, transmission and sunroof drain rows (Arteon gone, Tiguan "(RM1)"); 2026 gives the Golf R's plugs
  6 years. For the GTI the
  grid, every level's jobs and every additional item it is named in are the same in all four: spark plugs
  80,000 or 6 years ("GTI"), DSG fluid 80,000 ("0GC (DSG): GTI"), sunroof drains 20,000 or 2 years ("GTI /
  Golf R").
- **Model name.** vPIC's model lists (`GetModelsForMakeYear`, read 2026-10-23) call the car "Golf GTI" for
  2023, 2024 and 2026 and "GTI" for 2025, so the list is for "Golf GTI" with `modelAliases` "GTI".
- **Not 2022.** Volkswagen's card from the time exists for 2022 and wins over its reissue; it has a grid to
  120,000 and other rows, so 2022 has [its own list](volkswagen-gti-2022.md).
- **The cards cover every gas model** (2023 and 2024: gas and hybrid). Each is a grid of service levels,
  not of jobs: Minor at 10,000, 30,000 … 90,000; Standard at 20,000, 40,000 … 100,000; Extended at 40,000
  and 80,000, "only in combination with Standard". Standard includes "Perform Minor Maintenance". Each
  milestone here is the jobs of the levels marked in its column, each named once. The grid runs to
  100,000 (with kilometres under the miles); nothing says it repeats.
- **No months per column.** The footnotes give the first minor service at 10,000 or 1 year, then every
  20,000 or 2 years after the last, and standard services every 20,000 or 2 years. The columns carry no
  months, so the milestones have none. The oil reminder is 10,000 or 12 months, from the minor service's
  "or 1 year" (the owner's ruling of 2026-10-28, #517).
- **"Additional Maintenance Items"** are a separate table of job, interval and applicability, with no
  column marked. They are reminders in `additional` with the card's figures, never placed on the grid
  (the owner's ruling of 2026-10-23, #33): air filter 60,000 / 72 mo; brake fluid every 24 months by time
  alone (USA: 3 years after registration first, then every 2); dust and pollen filter 40,000 / 24 mo;
  spark plugs 80,000 / 72 mo; DSG fluid and filter 80,000 (not for a manual car, `manualExcluded`).
- **Left out of the additional items:**
  - Not the GTI's, by the card's own naming: the Golf R's torque-vectoring rear differential, the
    40,000-mile spark plugs (Arteon, Golf R), the 09P / 09S / 09H / 09U automatics' fluid.
  - Marked "All Applicable Vehicles" or "All applicable engines": AWD clutch fluid and front axle
    differential lock fluid (every 3 years), the belt starter-generator ribbed belt and tensioner
    (110,000) and the tire mobility kit filler bottle (every 4 years). As with "(if applicable)" rows,
    these are never a reminder, and there is no table for them to stand in. The 110,000-mile belt would
    also fall past the grid's end.
  - Inspections, which aren't reminders (rule 7) and have no table to stand in: the sunroof drains check
    (20,000 / 2 years, "GTI / Golf R"), and the camshaft toothed-belt tensioning roller and coolant pump
    toothed-belt checks (180,000 or 15 years), which also fall past the grid's end.
- **Left out of the levels:** the Canada-only tire rotation, and the rows for plug-in, hybrid and
  electric cars (high-voltage battery and parts, charging cable and socket, warning label).
- **The table's jobs** are all inspections apart from the oil change: they stay table-only, with a
  `-check` service where one exists. Two lines that would share one service in a milestone are kept
  apart and only one has it (the brake pad check is `brake-inspect`; the brake system and shock absorber
  check has none). Rows marked "All Applicable Vehicles" or "(if equipped)" say "(if fitted)" (tire
  pressure monitoring calibration, tire repair kit, trailer hitch, torque splitter oil, sunroof).
- Reminders: engine oil 10,000 (from the table) / 12 mo (the footnotes); the five additional items above.
- Not covered: 2022 ([its own list](volkswagen-gti-2022.md)); 2027, whose maintenance card isn't on the
  owners-literature site.
