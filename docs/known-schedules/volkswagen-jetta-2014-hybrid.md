# Volkswagen Jetta Hybrid, 2014 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2014-hybrid.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissued all-model maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`, document type
"Maintenance Card", `en-US`). NHTSA's index has no card from the time for this year, so the reissue is the only source. The owner ruled on 2026-10-29 that such years are built from the reissues alone, per engine where their
marks allow, cited with their print dates ([#552, answer (a)](https://github.com/mcosgriff/glovebox/issues/552#issuecomment-6511733274)), even where they differ from the cards
from the time either side. Read on 2026-10-29, as page images, by eye; the years were compared by script and
every difference looked at on the page.

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2014 | `USA5R505521`, `…/v1/document/033ff2d5-58e3-4663-97cf-1bbdb864258d` | 11/24/2025 | In full, off the page; the list was written from it |

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
- **This model's own rows in the table:** the high-voltage parts and wiring check, Standard, marked "HEV, PHEV". All are table-only.

## Additional Maintenance Items

They are reminders in `additional` with the card's figures, never placed on the grid (the owner's ruling of
2026-10-23, #33).

- **Taken:** air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (USA: 3 years after registration first, then every 2); dust and pollen filter 40,000 / 24 mo; spark plugs 40,000 / 48 mo.
- **Spark plugs:** "Jetta (162 - Except CCTA/CBFA/BPY)", 40,000 or 4 years, as on the 2015 card.
- **The dust and pollen filter** is 40,000 / 24 mo here; the 2013 (January 2013) and 2015 cards give 20,000. The
  other reminders agree with them, as #441 found.
- **Left out, as on the 2017 GTI's list:** rows for other models or engines by the card's own marks; rows marked
  "All Applicable Vehicles" or "All applicable engines" (the AWD clutch fluid, for a model sold here with
  front-wheel drive only, front axle differential lock
  fluid, the panoramic sunroof service, the belt starter-generator belt, the tire filler bottle), as with "(if applicable)"
  rows, with no table to stand in; the Canada-only snow screen; and the inspections (sunroof drains, the
  camshaft and coolant pump toothed-belt checks, the diesel particulate filter check), which aren't reminders
  (rule 7).
- **No gearbox reminder.** "Jetta" is on four transmission lines (01M, 02E, 09G at 40,000; 09S at 80,000), none of them the Hybrid's
  dual-clutch gearbox by name; the [2015 list](volkswagen-jetta-2015-hybrid.md) has no gearbox reminder either.

## The vehicle

- `"engine": { "label": "1.4L hybrid", "displacementL": 1.4, "powertrain": "hybrid" }`, as the 2015 and 2016
  hybrid lists; the model is "Jetta".
- **Hybrid.** The table has the "HEV, PHEV" high-voltage parts check. The camshaft toothed-belt check (150,000,
  then every 20,000, "Jetta (hybrid)") and the coolant pump toothed-belt check are inspections past the grid.

## Not covered

- 2013 goes with the 2015 card (#553); 2015 and 2016 have lists from the cards of the time.
