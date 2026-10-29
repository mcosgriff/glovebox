# Volkswagen Passat 3.6L, 2014 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2014-3.6.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.
Every Passat NMS card read, and what isn't covered, is on the [2014 2.5L's page](volkswagen-passat-2014-2.5.md).

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Passat 3.6L card (posted June 2013), in full; the list was written from it | `2013/MC-10125860-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here) | reissue `USA5R505521`, printed 11/24/2025 |

- **The card is the [2015 1.8T](volkswagen-passat-2015-1.8t.md)'s grid with the DSG's row in place of the
  automatic's**: every other row, mark and footnote is the same, so the list uses that list's wording.
- **"Transmission: DSG (non Automatic): Change fluid and filter (if applicable)"** at 40,000, 80,000 and
  120,000, and no automatic's row: read as the 2014 GTI card's same row is read on the
  [2010–2014 GTI](volkswagen-gti-2010-2014.md) list, `trans-fluid`, a reminder every 40,000 miles, named
  "(DSG only)", in `manualExcluded`.
- Spark plugs at 60,000 and 120,000 with footnote 6, "every 6 years": every 60,000 miles or 72 months.
- `"engine": { "label": "3.6L", "displacementL": 3.6 }`.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; DSG fluid
  and filter 40,000 (not for a manual); spark plugs and air filter 60,000 / 72 mo; brake fluid every 24 months
  by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.
- **Not covered: 2015 and 2016.** Two different cards are headed "2015 … 3.6L"; see the
  [2014 2.5L's page](volkswagen-passat-2014-2.5.md#what-isnt-covered). If the June 2014 one is taken, it is
  this card row for row and this list becomes 2014–2015.
