# Volkswagen Passat TDI, 2014–2015 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2014-2015-tdi.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.
Every Passat NMS card read, and what isn't covered, is on the [2014 2.5L's page](volkswagen-passat-2014-2.5.md).

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Passat 2.0L TDI card (posted June 2013), in full; the list was written from it | `2013/MC-10125861-9999` |
| 2015 | The Passat 2.0L TDI card (posted June 2014), in full: the same rows, marks and footnotes | `2014/MC-10120126-9999` |
| 2014, 2015 | Volkswagen's 2014 and 2015 reissues, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) and the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) (not read again here) | reissues `USA5R505521` and `USA5R505821` |

- **A grid of 13 columns, 10,000 to 130,000**, no months printed, not repeated (rule 9). **Unlike the 2014
  Golf and Jetta TDI cards** ([#434's question 2](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244)),
  the 130,000 column here is a whole service: the yearly airbag check, the oil change, tire rotation, brake pad check, brake fluid,
  AdBlue check, rear washer nozzle, service display and washer check are all drawn in it, with the timing
  belt. So every reminder falls on the table at every multiple of its interval, and nothing is past the grid.
- **The rows are the [2014 3.6L](volkswagen-passat-2014-3.6.md)'s** in that list's wording, with the
  spark plugs left out (the card has none), every 10,000-mile row carried on to 130,000, and the diesel's rows:
  - "AdBlue® Fluid: Check fluid level and add if necessary", in every column: an inspection, table-only.
    Footnote 6 at 40,000, 80,000 and 120,000, "At the 4 year mark, if the mileage is under 10,000 miles:
    AdBlue®: Change fluid", is a change on a condition the format can't hold; it is recorded here, not put in
    the list.
  - "Fuel filter: Replace" at every 20,000: the `fuel-filter` service, a reminder every 20,000 miles.
  - "Diesel Particulate Filter" at 120,000 (after the first 120,000 miles, then every 20,000 until it is
    replaced): an inspection, so table-only; the "every 20,000" after the grid's end is in its name.
  - "Timing Belt: Replace" at 130,000 only: the `timing-belt` service, a reminder every 130,000 miles (the
    grid's only multiple of it).
- The DSG's row, "(if applicable)", at 40,000, 80,000 and 120,000, with no automatic's row: a reminder every
  40,000 miles, "(DSG only)", in `manualExcluded`, as on the 3.6L's page.
- `"engine": { "label": "2.0L TDI", "displacementL": 2.0 }`. No `fuel`: no 2014–2015 Passat had a petrol 2.0L.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter and fuel filter 20,000
  (the dust filter or 24 mo); DSG fluid and filter 40,000 (not for a manual); air filter 60,000 / 72 mo;
  timing belt 130,000; brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from the reissues (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.
