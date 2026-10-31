# Volkswagen Jetta TDI, 2014 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2014-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every 2014 Jetta card read, and what isn't covered, is on the
[2014 1.8T's page](volkswagen-jetta-2014-2015-1.8t.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, filed with NHTSA as manufacturer communications.
Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Jetta Sedan 2.0L TDI card (posted June 2013), in full; the list was written from it | `2013/MC-10125799-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here) | reissue `USA5R505521`, printed 11/24/2025 |

- **The card is the [2014 Golf TDI](volkswagen-golf-2014-tdi.md)'s** with the model's name: the same rows,
  worded the same, with the same marks in the same columns, read off the page and checked row by row against
  the Golf's card. It has no grey "Carefree Maintenance" heading, and its brake fluid footnote reads "At the
  3 year mark, then every 2 years", to the same effect. So the list is that one's, in that list's wording,
  and that page says how it is read. In short: the 130,000 column, which holds only "Timing Belt: Replace",
  is left out and the timing belt is an `additional` reminder every 130,000 miles (the owner's ruling of
  2026-10-29 on [#551](https://github.com/mcosgriff/glovebox/issues/551), part A); the DSG row is "(if
  applicable)", so table-only.
- **Not the 2015 and 2016 cards.** Those cards' 130,000 column is a service of its own, so their lists
  ([2015](volkswagen-jetta-2015-tdi.md), [2016](volkswagen-jetta-2016-tdi.md)) keep it.
- `"engine": { "label": "2.0L TDI", "displacementL": 2.0, "fuel": "Diesel" }`: the 2014 Jetta's 2.0L MPI and
  GLI 2.0L TSI are 2.0L too (they have no list; see the 1.8T's page), so the TDI is told apart by fuel, as on
  the [2010 Jetta TDI](volkswagen-jetta-2010-tdi.md). A 2.0L Jetta with no fuel on record is asked which
  engine it has.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter and fuel filter 20,000
  (the dust filter or 24 mo); air filter 60,000 / 72 mo; timing belt 130,000 (apart from the table); brake
  fluid every 24 months by time alone.
- **Oil 12 months**, from Volkswagen's 2014 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey shading of the first two columns.
