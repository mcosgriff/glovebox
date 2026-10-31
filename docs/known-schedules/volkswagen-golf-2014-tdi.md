# Volkswagen Golf TDI, 2014 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-golf-2014-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). The [2014 Jetta SportWagen TDI](volkswagen-jetta-sportwagen-2014-tdi.md)
and the [2014 Jetta TDI](volkswagen-jetta-2014-tdi.md) are made from the same card under their own names.

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Golf 2.0L TDI card (posted October 2013), in full; the list was written from it. NHTSA files it again as `MC-10157534` and `MC-10242463`, byte for byte the same | `2013/MC-10155943-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here) | reissue `USA5R505521`, printed 11/24/2025 |

- **The 130,000 column is left out, and the timing belt is a reminder every 130,000 miles apart from the
  table** (the owner's ruling of 2026-10-29 on
  [#551](https://github.com/mcosgriff/glovebox/issues/551), part A, option (a)). The card's grid runs 13
  columns, 10,000 to 130,000, but the 130,000 column holds only "Timing Belt: Replace"; the oil change, tire
  rotation and every other job stop at 120,000. With that column in the table, rule 7 would refuse the oil
  and rotation reminders. So the table is the card's first 12 columns, 10,000 to 120,000, no months printed,
  not repeated (rule 9), and the belt is in `additional`: "Replace timing belt" (`timing-belt`), every
  130,000 miles, never on the table, as Toyota's jobs past the end of the grid are.
- **The rows are the [2015 Golf 1.8T](volkswagen-golf-2015-1.8t.md)'s**, in that list's wording: the same
  rows with the same marks in the same columns, checked row by row on the page. The 1.8T's spark plugs and
  automatic gearbox fluid aren't on this card; the diesel's rows are:
  - "Fuel filter: Replace" at every 20,000: the `fuel-filter` service, a reminder every 20,000 miles.
  - "Diesel Particulate Filter" at 120,000 (after the first 120,000 miles, then every 20,000 until it is
    replaced): an inspection, so table-only; the "every 20,000" after the grid's end is in its name.
  - "Timing Belt: Replace" at 130,000 only: the `additional` reminder above.
  - "Transmission: DSG (non Automatic): Change fluid and filter (if applicable)" at 40,000, 80,000 and
    120,000: an "(if applicable)" row, so in the table with no `service` and no reminder, named "(if fitted)"
    (#33). The card has no automatic's row.
- `"engine": { "label": "2.0L TDI", "displacementL": 2.0 }`, as on the [2010 Golf TDI](volkswagen-golf-2010-tdi.md).
  No `fuel`: the 2014 Golf's other engine is the 2.5L, and the GTI is a model of its own in vPIC.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter and fuel filter 20,000
  (the dust filter or 24 mo); air filter 60,000 / 72 mo; timing belt 130,000 (apart from the table); brake
  fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 reissue, as on the 2.5L's page (the owner's ruling of 2026-10-28,
  #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first two
  columns. No special operating conditions are given on the card.
