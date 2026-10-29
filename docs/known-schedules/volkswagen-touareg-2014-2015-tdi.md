# Volkswagen Touareg TDI, 2014–2015 (2nd generation)

How `app/src/main/assets/known-schedules/volkswagen-touareg-2014-2015-tdi.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md). Every Touareg card read, and what isn't covered, is on the
[2014–2015 3.6L page](volkswagen-touareg-2014-2015-3.6.md).

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Touareg 3.0L TDI card (posted June 2013), in full, as page images; the list was written from it | `2013/MC-10242478-9999` |
| 2015 | The Touareg 3.0L TDI card (June 2014), in full: the same rows, marks and footnotes. Filed twice, byte for byte the same | `2014/MC-10240830-9999`, `2014/MC-10120130-9999` |
| 2014, 2015 | Volkswagen's 2014 and 2015 reissues, for the oil change's time limit only (not read again here) | reissues `USA5R505521`, `USA5R505821` |

- **A grid of 12 columns, 10,000 to 120,000**, no months printed, not repeated (rule 9). Unlike the Passat and
  Golf TDI cards there is no 130,000 column and no timing belt row, so nothing waits on
  [#434's question 2](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244).
- **The rows are the [2014–2015 Touareg 3.6L](volkswagen-touareg-2014-2015-3.6.md)'s** in that list's wording,
  with the spark plugs left out (the card has none) and the diesel's rows, read as on the
  [2014–2015 Passat TDI](volkswagen-passat-2014-2015-tdi.md):
  - "AdBlue® Fluid: Check fluid level and add if necessary (3.0L TDI only)", in every column: an inspection,
    table-only. Footnote 6 at 40,000, 80,000 and 120,000, "At the 4 year mark, if the mileage is under 10,000
    miles: AdBlue®: Change fluid", is a change on a condition the format can't hold; it is recorded here, not
    put in the list.
  - "Fuel filter: Replace" at every 20,000: the `fuel-filter` service, a reminder every 20,000 miles.
  - "Diesel Particulate Filter" at 120,000 (after the first 120,000 miles, then every 20,000 until it is
    replaced): an inspection, so table-only; the "every 20,000" after the grid's end is in its name.
- **No gearbox row** and no all-wheel-drive row: the cards print neither.
- `"engine": { "label": "3.0L TDI", "displacementL": 3.0, "fuel": "Diesel" }`: the fuel keeps it apart from the
  3.0L Hybrid.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; fuel filter
  20,000; air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years first,
  then every 2).
- **Oil 12 months**, from the reissues (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.
