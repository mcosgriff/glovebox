# Volkswagen Passat 3.6L, 2016 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2016-3.6.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.
Every Passat NMS card read, and what isn't covered, is on the [2014 2.5L's page](volkswagen-passat-2014-2.5.md).

| Model year | What was read | Document |
|---|---|---|
| 2016 | The Passat 3.6L card headed "2015" and posted August 2015, in full; the list was written from it | `2015/MC-10123080-9999` |
| 2016 | The Passat 1.8L TSI card (posted August 2015), in full, for comparison | `2015/MC-10123065-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **A card headed "2015", read as the 2016 card.** Every page of `MC-10123080` is headed "2015 Volkswagen
  Passat Maintenance Schedule For USA / 3.6L Engine", and NHTSA files it under the 2015 Passat, but it was posted
  in August 2015 with the other 2016 cards and is in their layout, and another card, posted June 2014, is the 2015
  3.6L's ([2014–2015 3.6L](volkswagen-passat-2014-2015-3.6.md)). No card is headed "2016 … 3.6L". The owner's
  ruling of 2026-10-29 on #553 ([part C](https://github.com/mcosgriff/glovebox/issues/553#issuecomment-6511733565), #444's question 2) reads it as the 2016 card printed with the
  wrong year.
- **The card is the [2016 1.8T](volkswagen-passat-2016-1.8t.md)'s grid with the DSG's row in place of the
  automatic's**: every other row, mark and footnote is the same (both cards read side by side), so the list is
  that one's, in its wording. As on the 2016 cards, there is no yearly airbag check, tire rotation, door check
  lubrication, rear washer nozzle check or service sticker, and the dust and pollen filter is every 40,000 miles
  or 2 years.
- **"Transmission: DSG (non Automatic): Change fluid and filter (if applicable)"** at 40,000, 80,000 and
  120,000, and no automatic's row: read as on the [2014–2015 3.6L](volkswagen-passat-2014-2015-3.6.md) list,
  `trans-fluid`, a reminder every 40,000 miles, named "(DSG only)", in `manualExcluded`.
- Spark plugs at 60,000 and 120,000 with footnote 5, "every 6 years": every 60,000 miles or 72 months.
- `"engine": { "label": "3.6L", "displacementL": 3.6 }`. No other 2016 Passat list is 3.6L.
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; DSG fluid and filter 40,000
  (not for a manual); spark plugs and air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the
  card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No tire rotation, and no special operating conditions: the card has neither.
