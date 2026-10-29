# Volkswagen Passat 1.8T, 2016 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2016-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.
Every Passat NMS card read, and what isn't covered, is on the [2014 2.5L's page](volkswagen-passat-2014-2.5.md).

| Model year | What was read | Document |
|---|---|---|
| 2016 | The Passat 1.8L TSI card (posted August 2015), in full; the list was written from it | `2015/MC-10123065-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The card is the [2016 Jetta 1.8T](volkswagen-jetta-2016-1.8t.md)'s** with the model's name: the same rows
  with the same marks in the same columns, footnotes included. So the list is that one's, in its wording.
- "Transmission: Automatic (non DSG): Change fluid" at 80,000 only: a reminder every 80,000 miles, in
  `manualExcluded`.
- `"engine": { "label": "1.8T", "displacementL": 1.8 }`.
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; spark plugs and air filter
  60,000 / 72 mo; automatic gearbox fluid 80,000 (not for a manual); brake fluid every 24 months by time alone
  (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No tire rotation, and no special operating conditions: the card has neither.
