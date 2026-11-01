# Volkswagen Golf TDI, 2016 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-2016-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). The [2016 Golf SportWagen TDI](volkswagen-golf-sportwagen-2016-tdi.md)
is made from the same card. Every Mk7 card read, and what isn't covered, is on the
[2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, filed with NHTSA as manufacturer communications.
Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2016 | "Golf and Golf SportWagen", 2.0L TDI (posted August 2015), in full; the list was written from it | `2015/MC-10123074-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The rows are the [2016 Golf 1.8T](volkswagen-golf-2016-1.8t.md)'s**, in that list's wording: the same
  rows with the same marks in the same columns, checked row by row on the page. What differs from 2015 is what
  differs on the 1.8T's cards (no tire rotation, yearly airbag check, door check lubrication, rear window
  washer nozzle check or service sticker; the dust and pollen filter every 40,000 miles or 2 years, drawn at
  40,000, 80,000 and 120,000). The 1.8T's spark plugs and automatic gearbox fluid aren't on this card; the
  diesel's rows are those of the [2015 Golf TDI](volkswagen-golf-2015-tdi.md): the AdBlue check in every
  column (table-only; its footnote 5, the change at 4 years under 10,000 miles, is recorded, not listed),
  the fuel filter every 20,000 (a reminder), the particulate filter check at 120,000 (table-only), the DSG
  row "(if applicable)" at 40,000, 80,000 and 120,000 (table-only, no `service`), and the timing belt at
  130,000.
- **The 130,000 column is left out, and the timing belt is an `additional` reminder every 130,000 miles**
  (the owner's ruling of 2026-10-29 on [#551](https://github.com/mcosgriff/glovebox/issues/551), part A,
  option (a)). As on the 2015 card, that column holds only the AdBlue check and "Timing Belt: Replace"; the
  AdBlue check there goes with the column. So the table is 10,000 to 120,000, not repeated (rule 9).
- `"engine": { "label": "2.0L TDI", "displacementL": 2.0 }`. No `fuel`: vPIC's 2016 model list has "Golf",
  "Golf SportWagen", "Golf GTI", "Golf R" and "e-Golf" apart, so no other 2016 "Golf" is 2.0L. A Golf with no
  engine on record is asked "1.8T", "2.0L TDI" or "Neither".
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; fuel filter 20,000; air
  filter 60,000 / 72 mo; timing belt 130,000 (apart from the table); brake fluid every 24 months by time
  alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No special operating conditions are given on the card.
