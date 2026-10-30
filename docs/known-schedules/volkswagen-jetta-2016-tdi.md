# Volkswagen Jetta TDI, 2016 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2016-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Mk6 Jetta card read, and what isn't covered, is on the
[2014 1.8T's page](volkswagen-jetta-2014-2015-1.8t.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, filed with NHTSA as manufacturer communications.
Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2016 | The Jetta Sedan 2.0L TDI card (posted August 2015), in full; the list was written from it | `2015/MC-10123066-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The 130,000 column is a service of its own, so it is kept**, as on the
  [2015 Jetta TDI](volkswagen-jetta-2015-tdi.md) (which says why #551 part A's ruling isn't needed here). At
  130,000 the card draws the AdBlue check, the brake pad check, the oil change, the service display reset
  and the timing belt. The timing belt is a reminder every 130,000 miles (`timing-belt`).
- **The other rows are the [2016 Golf TDI](volkswagen-golf-2016-tdi.md)'s**, in that list's wording: the same
  rows with the same marks in the same columns from 10,000 to 120,000, checked row by row on the page (the
  [2016 Jetta 1.8T](volkswagen-jetta-2016-1.8t.md)'s rows without the spark plugs and automatic gearbox
  fluid, with the diesel's). A grid of 13 columns, 10,000 to 130,000, no months printed, not repeated (rule 9).
  The AdBlue check is table-only (footnote 5, the change at 4 years under 10,000 miles, recorded, not listed);
  the fuel filter every 20,000 is a reminder; the particulate filter check at 120,000 and the DSG row "(if
  applicable)" at 40,000, 80,000 and 120,000 are table-only.
- `"engine": { "label": "2.0L TDI", "displacementL": 2.0, "fuel": "Diesel" }`, as on the 2015 list: the 2016
  Jetta's 2.0L MPI is 2.0L too, and vPIC may name the GLI "Jetta".
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; fuel filter 20,000; air
  filter 60,000 / 72 mo; timing belt 130,000; brake fluid every 24 months by time alone (the card says at 3
  years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No special operating conditions are given on the card.
