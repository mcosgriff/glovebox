# Volkswagen Golf R, 2012 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-golf-r-2012.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissued all-model maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`, document type
"Maintenance Card", `en-US`). NHTSA's index has no card from the time for this year, so the reissue is the only source. The owner ruled on 2026-10-29 that such years are built from the reissues alone, per engine where their
marks allow, cited with their print dates ([#552, answer (a)](https://github.com/mcosgriff/glovebox/issues/552#issuecomment-6511733274)), even where they differ from the cards
from the time either side. Read on 2026-10-29, as page images, by eye; the years were compared by script and
every difference looked at on the page.

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2012 | `USA5R505021`, `…/v1/document/903e84e8-1ae4-4437-9640-90a1a8a31586` | 11/24/2025 | In full, off the page; the list was written from it |

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
- **This model's own rows in the table:** the hood hook, Extended, marked "CC, Golf Convertible, EOS, Golf(5K), Tiguan(5N), Touareg (7P)". All are table-only.

## Additional Maintenance Items

They are reminders in `additional` with the card's figures, never placed on the grid (the owner's ruling of
2026-10-23, #33).

- **Taken:** air filter 40,000 / 48 mo ("Golf R (5K)"; "All Vehicles, except Golf R (5K)" is 60,000 or 6 years); brake fluid every 24 months by time alone (USA: 3 years after registration first, then every 2); dust and pollen filter 40,000 / 24 mo; spark plugs 40,000 / 48 mo; rear differential fluid 40,000 / 48 mo ("Rear Axle Differential with Torque Vectoring", "Golf R"); AWD clutch fluid every 36 months by time alone, in `allWheelDriveOnly`.
- **Spark plugs and air filter:** "Golf R" is on the 40,000 or 4 years line of both rows. The spark plug row also
  has "Golf / GTI (5K-2.0T)" on its 60,000 or 6 years line; the Golf R's own mark is taken over the family's,
  as the air filter row ("except Golf R (5K)") reads the Golf R apart. The 2013 card from the time gives 60,000
  or 6 years for both; the reissue is followed, as ruled.
- **Rear differential:** "Rear Axle Differential with Torque Vectoring - Change fluid", 40,000 or 4 years,
  "Golf R". The 2013 card from the time has no such job; the issue (#552) named this row when the owner ruled,
  so it is taken as the 2023–2025 Golf R's list takes it, `rear-diff`.
- **AWD clutch.** "AWD Clutch - Change fluid", every 3 years, "All Applicable Vehicles": read as on the
  [2018 Golf SportWagen](volkswagen-golf-sportwagen-2018.md) and the Atlas, "Change AWD clutch fluid (AWD)", no
  service line, in `allWheelDriveOnly`, so only a car known to drive two wheels loses it (the brief's rule for an
  AWD clutch row, and the owner's Haldex ruling, [#554 A](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511795038)).
- **Left out, as on the 2017 GTI's list:** rows for other models or engines by the card's own marks; rows marked
  "All Applicable Vehicles" or "All applicable engines" other than the AWD clutch (front axle differential lock
  fluid, the panoramic sunroof service, the belt starter-generator belt, the tire filler bottle), as with "(if applicable)"
  rows, with no table to stand in; the Canada-only snow screen; and the inspections (sunroof drains, the
  camshaft and coolant pump toothed-belt checks, the diesel particulate filter check), which aren't reminders
  (rule 7).
- **No gearbox reminder.** The Golf R is on two DSG lines, "0GC (DSG)" at 80,000 and "0D9 (DSG)" at 40,000, and the card doesn't
  say which it has, so the row is left out, as the owner ruled for gearbox rows that don't settle it (#549).

## The vehicle

- One engine; the list names none. vPIC's 2012 model list has no "Golf R" (its 2013 list has); the list is for
  "Golf R", as the [2013 list](volkswagen-golf-r-2013.md), which a decoded Golf R carries.
- **The hood hook** is marked "Golf(5K)"; "Golf" on a card covers the whole Golf family
  ([#555, round 2](https://github.com/mcosgriff/glovebox/issues/555#issuecomment-6511795335)).

## Not covered

- 2013 has [its own list](volkswagen-golf-r-2013.md), from the card of the time.
