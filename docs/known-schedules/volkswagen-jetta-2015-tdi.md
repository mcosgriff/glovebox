# Volkswagen Jetta TDI, 2015 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2015-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Mk6 Jetta card read, and what isn't covered, is on the
[2014 1.8T's page](volkswagen-jetta-2014-2015-1.8t.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, filed with NHTSA as manufacturer communications.
Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2015 | The Jetta Sedan 2.0L TDI card (posted October 2014), in full; the list was written from it. NHTSA files it again as `MC-10242480`, byte for byte the same | `2014/MC-10121579-9999`, `2014/MC-10242480-9999` |
| 2015 | Volkswagen's 2015 reissue, for the oil change's time limit only, as read for the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) (not read again here) | reissue `USA5R505821`, printed 11/24/2025 |

- **Unlike the 2014 Jetta TDI's and the 2015 Golf TDI's cards, the 130,000 column is a service of its own**,
  so it is kept, as on the [2014–2015 Passat TDI](volkswagen-passat-2014-2015-tdi.md). #551 (and #440, from
  which the Jetta came to it) said the 2014–2016 Jetta TDI cards' 130,000 column holds only the timing belt;
  the 2015 card hadn't been fetched then, and on this one and the 2016 card it doesn't. At 130,000 the card
  draws the AdBlue check, the brake pad check, the oil change, the rear window washer nozzle check, the
  service display reset, the service sticker, the timing belt and tire rotation. So every reminder falls on
  the table at every multiple of its interval, and nothing is past the grid: the owner's ruling on #551 part
  A (a 130,000 column with only the belt) isn't needed here. The timing belt is a reminder every 130,000 miles
  (`timing-belt`, the grid's only multiple of it), as on the Passat.
- **The other rows are the [2015 Golf TDI](volkswagen-golf-2015-tdi.md)'s**, in that list's wording: the same
  rows with the same marks in the same columns from 10,000 to 120,000, checked row by row on the page. A grid
  of 13 columns, 10,000 to 130,000, no months printed, not repeated (rule 9).
  - The AdBlue check in every column, table-only; footnote 6 (the change at 4 years under 10,000 miles) is
    recorded, not listed.
  - Fuel filter every 20,000 (a reminder); the particulate filter check at 120,000 (table-only); the DSG row
    "(if applicable)" at 40,000, 80,000 and 120,000, table-only with no `service`.
  - The yearly airbag check, the washer and wiper check and the brake fluid aren't drawn at 130,000; the
    table has them where the card does.
- `"engine": { "label": "2.0L TDI", "displacementL": 2.0, "fuel": "Diesel" }`: the 2015 Jetta's 2.0L MPI and
  GLI are 2.0L too (they have no list), so the TDI is told apart by fuel, as on the
  [2014 Jetta TDI](volkswagen-jetta-2014-tdi.md).
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter and fuel filter 20,000
  (the dust filter or 24 mo); air filter 60,000 / 72 mo; timing belt 130,000; brake fluid every 24 months by
  time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2015 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first column.
  No special operating conditions are given on the card.
