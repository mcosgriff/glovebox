# Volkswagen Passat 1.8T, 2015 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2015-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.
Every Passat NMS card read, and what isn't covered, is on the [2014 2.5L's page](volkswagen-passat-2014-2.5.md).

| Model year | What was read | Document |
|---|---|---|
| 2015 | The Passat 1.8L TSI card (posted June 2014), in full; the list was written from it | `2014/MC-10120125-9999` |
| 2015 | Volkswagen's 2015 reissue, for the oil change's time limit only, as read for the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) (not read again here) | reissue `USA5R505821`, printed 11/24/2025 |

- **The card is the [2015 Jetta 1.8T](volkswagen-jetta-2015-1.8t.md)'s** with the model's name: the same rows
  with the same marks in the same columns, footnotes included. So the list is that one's, in its wording.
- A grid of 12 columns, 10,000 to 120,000, no months printed, not repeated (rule 9).
- "Transmission: Automatic (non DSG): Change fluid" at 50,000 and 100,000: the `trans-fluid` service, a
  reminder every 50,000 miles, in `manualExcluded`. The card has no DSG row.
- `"engine": { "label": "1.8T", "displacementL": 1.8 }`.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo (footnote 6, "every 6 years"); automatic gearbox fluid 50,000 (not for a
  manual); brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2015 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first column.
