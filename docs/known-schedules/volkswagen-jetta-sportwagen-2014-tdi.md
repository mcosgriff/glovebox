# Volkswagen Jetta SportWagen TDI, 2014 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-jetta-sportwagen-2014-tdi.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, filed with NHTSA as manufacturer communications.
Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Jetta SportWagen 2.0L TDI card (posted June 2013), in full; the list was written from it. NHTSA's index also files `MC-10242469` and `MC-10242470`, "2.0L TDI Engine", under it; not fetched | `2013/MC-10125864-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here) | reissue `USA5R505521`, printed 11/24/2025 |

- **The card is the [2014 Golf TDI](volkswagen-golf-2014-tdi.md)'s** with the model's name: the same rows,
  worded the same, with the same marks in the same columns, read off the page and checked row by row against
  the Golf's card. Only the brake fluid footnote's wording differs ("At the 3 year mark, then every 2
  years"), not its meaning. So the list is that one's, in that list's wording, and that page says how it is
  read. In short: the 130,000 column, which holds only "Timing Belt: Replace", is left out and the timing
  belt is an `additional` reminder every 130,000 miles (the owner's ruling of 2026-10-29 on
  [#551](https://github.com/mcosgriff/glovebox/issues/551), part A); the DSG row is "(if applicable)", so
  table-only.
- `"engine": { "label": "2.0L TDI", "displacementL": 2.0 }`. No `fuel`: the 2014 Jetta SportWagen's other
  card is the [2.5L's](volkswagen-jetta-sportwagen-2014-2.5.md). vPIC names "Jetta" and "Jetta SportWagen"
  apart in 2014, as on the [2010 TDI's page](volkswagen-jetta-sportwagen-2010-tdi.md).
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter and fuel filter 20,000
  (the dust filter or 24 mo); air filter 60,000 / 72 mo; timing belt 130,000 (apart from the table); brake
  fluid every 24 months by time alone.
- **Oil 12 months**, from Volkswagen's 2014 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first two columns.
