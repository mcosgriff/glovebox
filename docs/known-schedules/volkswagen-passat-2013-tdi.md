# Volkswagen Passat TDI, 2013 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2013-tdi.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissued all-model maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`, document type
"Maintenance Card", `en-US`). NHTSA's index has no card from the time for this year, so the reissue is the only source. The owner ruled on 2026-10-29 that such years are built from the reissues alone, per engine where their
marks allow, cited with their print dates ([#552, answer (a)](https://github.com/mcosgriff/glovebox/issues/552#issuecomment-6511733274)), even where they differ from the cards
from the time either side. Read on 2026-10-29, as page images, by eye; the years were compared by script and
every difference looked at on the page.

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2013 | `USA5R505321`, `…/v1/document/8db4252c-5991-4f79-a654-5c725d09bdf8` | 11/24/2025 | In full, off the page; the list was written from it |

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
- **Fuel filter:** every 60,000 miles, "Bettle, Jetta, Golf, Passat(A3*)" (2012's reissue gives 40,000).
- **Timing belt:** "Every 130,000 miles", "Passat(A3*)".
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
- **No gearbox reminder.** The transmission row names this model under more than one gearbox, at different intervals, and none says which car has which. As the owner ruled for such rows, they wait for a gearbox choice on the vehicle ([#549](https://github.com/mcosgriff/glovebox/issues/549), [#550](https://github.com/mcosgriff/glovebox/issues/550)).

## The vehicle

- `"engine": { "label": "2.0L TDI", "displacementL": 2.0 }`.

## Not covered

- 2012 has [its own list](volkswagen-passat-2012-tdi.md).
