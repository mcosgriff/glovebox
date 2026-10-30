# Volkswagen Golf SportWagen, 2017 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-sportwagen-2017.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissued all-model maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`, document type
"Maintenance Card", `en-US`). NHTSA's index has no card from the time for this year, so the reissue is the only source. The owner ruled on 2026-10-29 that such years are built from the reissues alone, per engine where their
marks allow, cited with their print dates ([#552, answer (a)](https://github.com/mcosgriff/glovebox/issues/552#issuecomment-6511733274)), even where they differ from the cards
from the time either side. Read on 2026-10-29, as page images, by eye; the years were compared by script and
every difference looked at on the page.

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2017 | `USA5R506721`, `…/v1/document/67a3ec7c-748c-4e26-92ea-1c7dcc6732dd` | 11/25/2025 | In full, off the page; the list was written from it |

## How the card is read

- **The layout** is the one read for the [2017 GTI](volkswagen-gti-2017.md) from the same reissue: a grid of
  service levels for every gas, diesel and hybrid model. Minor at 10,000, 30,000 … 90,000; Standard at 20,000,
  40,000 … 100,000; Extended at 40,000 and 80,000. Each milestone is the jobs of the levels marked in its
  column, each named once; the table uses that list's wording. The grid runs to 100,000; nothing says it
  repeats. The tire rotation is "Canada Only" and is left out.
- The 2017 reissue read here is the copy filed under importer `999`; the [2017 GTI's page](volkswagen-gti-2017.md)
  cites the importer `444` copy, `…/v1/document/79833ead-258d-4acf-ba6a-5be91876f4d2`. Both are `USA5R506721`,
  printed 11/25/2025.
- **No months per column.** The footnotes give the first minor service at 10,000 or 1 year, then every 20,000
  or 2 years after the last, and standard services every 20,000 or 2 years. The columns carry no months, so
  the milestones have none. The oil reminder is 10,000 or 12 months, from the minor service's "or 1 year"
  (the owner's ruling of 2026-10-28, #517).
- **The table's other jobs** are inspections: table-only, with a `-check` service where one exists, worded as on
  the 2017 GTI's list. Rows marked "All Applicable Vehicles" or "(if equipped)" say "(if fitted)". Rows for
  plug-in, electric and (unless this list is one) hybrid cars, and rows for other models, are left out.

## Additional Maintenance Items

They are reminders in `additional` with the card's figures, never placed on the grid (the owner's ruling of
2026-10-23, #33).

- **Taken:** air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (USA: 3 years after registration first, then every 2); dust and pollen filter 40,000 / 24 mo; spark plugs 40,000 / 48 mo; AWD clutch fluid every 36 months by time alone, in `allWheelDriveOnly`.
- **Spark plugs:** "Golf (AU1) / Golf variant (BX5)", 40,000 or 4 years.
- **AWD clutch.** "AWD Clutch - Change fluid", every 3 years, "All Applicable Vehicles": the 2017 Golf SportWagen was sold
  with 4Motion as well as front-wheel drive; read as on the [2018 Golf SportWagen](volkswagen-golf-sportwagen-2018.md) and the Atlas, "Change AWD clutch fluid (AWD)", no
  service line, in `allWheelDriveOnly`, so only a car known to drive two wheels loses it (the brief's rule for an
  AWD clutch row, and the owner's Haldex ruling, [#554 A](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511795038)).
- **Left out, as on the 2017 GTI's list:** rows for other models or engines by the card's own marks; rows marked
  "All Applicable Vehicles" or "All applicable engines" other than the AWD clutch (front axle differential lock
  fluid, the belt starter-generator belt, the tire filler bottle), as with "(if applicable)"
  rows, with no table to stand in; the Canada-only snow screen; and the inspections (sunroof drains, the
  camshaft and coolant pump toothed-belt checks, the diesel particulate filter check), which aren't reminders
  (rule 7).
- **No gearbox reminder.** The wagon is on "09G: … Golf Variant (BX5)" at 80,000 and on "0D9 (DSG): … Golf Variant" and "0GC (DSG):
  Golf (AU1), Golf Variant (BX5)" at 40,000. It waits for #549.

## The vehicle

- `"engine": { "label": "1.8T", "displacementL": 1.8 }`. vPIC's 2017 model list names it "Golf SportWagen".

## Not covered

- 2016 and 2018 have lists from the cards of the time; the Alltrack is its own model.
