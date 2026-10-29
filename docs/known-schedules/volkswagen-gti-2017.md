# Volkswagen GTI, 2017 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-gti-2017.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissued maintenance card**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=2017&model-code=5G1`,
document type "Maintenance Card", importer `444`, `en-US`). NHTSA's index has no 2017 card for the GTI
(only the e-Golf's), so the reissue is the only source; the owner's rulings (#33, #143) allow that, cited
as a reissue with its print date. Read on 2026-10-26.

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2016 | `USA5R506521`, `…/v1/document/ceab8882-749e-4dda-ba29-a7af0f418d43` | 11/25/2025 | For comparison with the 2016 card from the time (below); the 2016 list stays on that card |
| 2017 | `USA5R506721`, `…/v1/document/79833ead-258d-4acf-ba6a-5be91876f4d2` | 11/25/2025 | In full, off the page; the list was written from it |

- **The layout** is the one of the 2023–2026 reissues ([that list's page](volkswagen-gti-2023-2026.md)):
  a grid of service levels for every gas, diesel and hybrid model. Minor at 10,000, 30,000 … 90,000;
  Standard at 20,000, 40,000 … 100,000 ("Perform Minor Maintenance" is its first job); Extended at 40,000
  and 80,000, "only in combination with Standard". Each milestone here is the jobs of the levels marked in
  its column, each named once. The grid runs to 100,000; nothing says it repeats.
- **The GTI's rows are the 2023 reissue's, row for row**, so the table uses that list's wording. The
  exhaust check reads "leaks, attachment and damage" here, "leaks, damage and secure fittings" there; it is
  one job.
- **No months per column.** The footnotes give the first minor service at 10,000 or 1 year, then every
  20,000 or 2 years after the last, and standard services every 20,000 or 2 years. The columns carry no
  months, so, as on the 2023–2026 list, the milestones have none. The oil reminder is 10,000 or 12 months,
  from the minor service's "or 1 year" (the owner's ruling of 2026-10-28, #517).
- **Additional Maintenance Items** are reminders in `additional` with the card's figures, never placed on
  the grid (the owner's ruling of 2026-10-23, #33): air filter 60,000 / 72 mo; brake fluid every 24 months
  by time alone (USA: 3 years after registration first, then every 2); dust and pollen filter 40,000 /
  24 mo.
- **No spark plugs and no DSG fluid** (see "Needs a decision" below). Nothing is guessed for them.
- **Left out of the additional items**, as on the 2023–2026 list:
  - Not the GTI's, by the card's own naming: diesel fuel filter, diesel particulate filter, AdBlue, the
    Golf R's torque-vectoring rear differential, the snow screen (Canada; Passat, Tiguan), the Touareg's
    A/C ribbed belt, the diesel toothed belts, the coolant pump toothed belt (its rows name other models
    and the 1.4L Jetta), and the 09G / 09M / 09P / 09S / 0C8 / 01M automatics.
  - Marked "All Applicable Vehicles" or "All applicable engines": AWD clutch fluid and front axle
    differential lock fluid (every 3 years), the belt starter-generator ribbed belt and tensioner
    (110,000) and the tire mobility kit filler bottle (every 4 years). As with "(if applicable)" rows,
    these are never a reminder, and there is no table for them to stand in. The 110,000-mile belt would
    also fall past the grid's end.
  - Inspections, which aren't reminders (rule 7) and have no table to stand in: the sunroof drains check
    (20,000 / 2 years, "Golf/GTI/GolfR"), and the camshaft toothed-belt tensioning roller check (180,000
    or 15 years; "Golf, e-Golf, GTI, Golf R, Passat"), which also falls past the grid's end.
- **Left out of the levels:** the Canada-only tire rotation, and the rows for plug-in, hybrid, diesel and
  electric cars, the Touareg (air suspension, power steering fluid) and the convertibles, the Golf
  Cabriolet and the CC (convertible top, door arrester), and the hood hook (CC, Golf Convertible, Tiguan,
  Touareg).
- **The table's jobs** are all inspections apart from the oil change: they stay table-only, with a
  `-check` service where one exists. Rows marked "All Applicable Vehicles" or "(if equipped)" say "(if
  fitted)" (tire pressure monitoring calibration, tire repair kit, trailer hitch, torque splitter oil,
  sunroof).
- Severe conditions: one general sentence (oil, drivetrain fluid and the air filter more often in extreme
  cold or dust) with no jobs or figures, so the list has nothing for them.
- One engine: the GTI's 2.0T; the list names none.
- **Model name.** NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`, read 2026-10-26) has
  "Golf GTI" for 2017 and no "GTI". The list is for "GTI", like the other Mk7 lists, with `modelAliases`
  "Golf GTI".

## How far the reissue matches a card from the time

There is no 2017 card from the time to check against, but the **2016 reissue** (printed the same day) can
be checked against the **2016 card from the time** (`2015/MC-10123072-9999`, the source of the
[2016 list](volkswagen-gti-2016.md)). They differ:

- The 2016 card is a grid of jobs for the GTI, 10,000 to 120,000; the reissue is the all-model level
  layout above, to 100,000.
- DSG fluid: the 2016 card gives every 40,000 miles; the 2016 reissue names the GTI only on the
  "0GC (DSG)" line at 80,000 miles, as the 2017 reissue does.
- Spark plugs: the 2016 card gives 60,000 or 6 years; the 2016 reissue names no GTI on any spark plug
  line (the "Golf (AU1)" line is 40,000 or 4 years), as the 2017 reissue.
- The 2016 reissue keeps the GTI's camshaft toothed-belt check (180,000 or 15 years); the card from the
  time stops at 120,000.

So the reissues are not copies of the cards from the time. For 2017 the reissue is still the only source;
the rows where it is doubtful are left out below, not guessed.

## Needs a decision (left out)

- **DSG fluid.** The only transmission row naming the GTI is "Transmission, Automatic – Change Fluid &
  filter (if applicable) »for non-DSG – USA Only«", whose 80,000-mile part lists "0GC (DSG): Beetle, Golf,
  GTI, Golf R". The row is headed non-DSG yet lists DSG gearboxes; its 40,000-mile part lists "02E (DSG)"
  and "0D9 (DSG)" for Golf models without naming the GTI. The 2016 card from the time gives the GTI's DSG
  40,000; the 2018 card gives the 6-speed DSG 40,000 and the 7-speed 80,000 "Golf R only". Taking 80,000
  from the reissue as printed, or 40,000 from the years either side, is a call for the owner.
- **Spark plugs.** No spark plug line names the GTI. "Golf (AU1) / Golf variant (BX5)" is on the 40,000
  or 4 years line, "Golf (2.5l)" and "Golf R" too. Whether "Golf (AU1)" covers the GTI isn't said. The
  2016 card gives the GTI 60,000 or 6 years; the 2018 card gives the 2.0L 40,000 or 4 years.

## Not covered

- 2016 and earlier, and 2018 on: each has a list of its own, from a card from the time where one exists.
