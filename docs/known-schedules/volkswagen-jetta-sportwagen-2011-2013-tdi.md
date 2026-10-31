# Volkswagen Jetta SportWagen TDI, 2011–2013 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-jetta-sportwagen-2011-2013-tdi.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissued all-model maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`, document type
"Maintenance Card", `en-US`). NHTSA's index has no card from the time for these years, so the reissues are the only source. The owner ruled on 2026-10-29 that such years are built from the reissues alone, per engine where their
marks allow, cited with their print dates ([#552, answer (a)](https://github.com/mcosgriff/glovebox/issues/552#issuecomment-6511733274)), even where they differ from the cards
from the time either side. Read on 2026-10-29, as page images, by eye; the years were compared by script and
every difference looked at on the page.

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2011 | `USA5R504821`, `…/v1/document/ce283b19-b48b-41b2-baab-a1f04c81af4f` | 11/24/2025 | In full, off the page; the list was written from it |
| 2012 | `USA5R505021`, `…/v1/document/903e84e8-1ae4-4437-9640-90a1a8a31586` | 11/24/2025 | Compared with the first, differences looked at on the page |
| 2013 | `USA5R505321`, `…/v1/document/8db4252c-5991-4f79-a654-5c725d09bdf8` | 11/24/2025 | Compared with the first, differences looked at on the page |

## How the card is read

- **The layout** is the later one of the [2017 GTI](volkswagen-gti-2017.md)'s reissue: a grid of service
  levels for every model but the Routan. Minor at 10,000, 30,000 … 90,000; Standard at 20,000, 40,000 …
  100,000 ("Perform Minor Maintenance" is its first job); Extended at 40,000 and 80,000, "only in
  combination with Standard". Each milestone is the jobs of the levels marked in its column, each named once.
  The grid runs to 100,000; nothing says it repeats. The levels' jobs are the same in each of the
  2011, 2012, 2013 and 2014 reissues, and the same as the 2017 reissue's but for
  the tire rotation (below) and the sunroof test, which here has no cleaning and lubricating.
- **Tire rotation** is a Minor job for "All Vehicles" (the 2017 and later reissues make it Canada only), so it
  is in every column and a reminder every 10,000 miles.
- **No months per column.** The footnotes give the first minor service at 10,000 or 1 year, then every 20,000
  or 2 years after the last, and standard services every 20,000 or 2 years. The columns carry no months, so
  the milestones have none. The oil reminder is 10,000 or 12 months, from the minor service's "or 1 year"
  (the owner's ruling of 2026-10-28, #517).
- **The table's other jobs** are inspections: table-only, with a `-check` service where one exists, worded as on
  the 2017 GTI's list. Rows marked "All Applicable Vehicles" or "(if equipped)" say "(if fitted)". Rows for
  plug-in, electric and (unless this list is one) hybrid cars, and rows for other models, are left out.
- **This model's own rows in the table:** "Reducing Agent (AdBlue), Filling; Only at Customer Request", Minor, marked "Diesel". All are table-only.

## Additional Maintenance Items

They are reminders in `additional` with the card's figures, never placed on the grid (the owner's ruling of
2026-10-23, #33).

- **Taken:** air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (USA: 3 years after registration first, then every 2); dust and pollen filter 40,000 / 24 mo; diesel fuel filter 60,000; timing belt (diesel) 130,000.
- **Fuel filter:** every 60,000 miles. The line names "Jetta, Golf" (2011, 2012) and "Bettle, Jetta, Golf,
  Passat(A3*)" (2013), not the wagon by any of its own names. Whether it is read as a Jetta or as a Golf (the
  wagon is the Golf Variant), the figure is the same, and no other line could be its.
- **Timing belt:** "Every 130,000 miles", "Jetta/Variant (from 2010)" (2011, 2013) and "Jetta/golf Variant
  (from 2010)" (2012).
- **Not taken: AdBlue.** "Reducing Agent (AdBlue), Filling, Every 10,000 miles, TDI engines". The mark takes in
  every TDI, but it is a top-up, not a part replaced; the 2010 cards from the time mark their AdBlue job for
  the 3.0L TDI only, and the 2014–2015 Touareg TDI cards make it a check in every column. Whether it becomes
  a reminder, and for which TDIs, is one of the open questions on [#552](https://github.com/mcosgriff/glovebox/issues/552).
  The Minor level's "AdBlue, only at customer request" row ("Diesel") is in the table, table-only.
- **Left out, as on the 2017 GTI's list:** rows for other models or engines by the card's own marks; rows marked
  "All Applicable Vehicles" or "All applicable engines" (the AWD clutch fluid, for a model sold here with
  front-wheel drive only, front axle differential lock
  fluid, the panoramic sunroof service, the belt starter-generator belt, the tire filler bottle), as with "(if applicable)"
  rows, with no table to stand in; the Canada-only snow screen; and the inspections (sunroof drains, the
  camshaft and coolant pump toothed-belt checks, the diesel particulate filter check), which aren't reminders
  (rule 7).
- **No gearbox reminder.** As on the [2.5L's page](volkswagen-jetta-sportwagen-2011-2012-2.5.md): it waits for #549.

## The vehicle

- `"engine": { "label": "2.0L TDI", "displacementL": 2.0, "fuel": "Diesel" }`, as the
  [2010 TDI list](volkswagen-jetta-sportwagen-2010-tdi.md).

## Not covered

- NHTSA's index has no TDI SportWagen card for 2013 (the card headed "2013" is the 2.5L's), so 2013 is here.
