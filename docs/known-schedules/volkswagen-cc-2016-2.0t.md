# Volkswagen CC 2.0T, 2016

How `app/src/main/assets/known-schedules/volkswagen-cc-2016-2.0t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every CC card read, and what isn't covered, is on the
[2014–2015 2.0T's page](volkswagen-cc-2014-2015-2.0t.md).

| Model year | What was read | Document |
|---|---|---|
| 2016 | The CC 2.0L TSI card (posted August 2015), in full; the list was written from it | `2015/MC-10123079-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The card is the [2016 Passat 1.8T](volkswagen-passat-2016-1.8t.md)'s grid with the DSG's row in place of
  the automatic's**: every other row has the same marks in the same columns, so the list uses that list's wording. Like
  that card it has no yearly airbag check, tire rotation, door check lubrication, rear washer nozzle check or
  service sticker, and gives the dust and pollen filter every 40,000 miles or 2 years.
- "Transmission: DSG (non Automatic): Change fluid and filter (if applicable)" at 40,000, 80,000 and 120,000,
  and no automatic's row: `trans-fluid`, every 40,000 miles, "(DSG only)", in `manualExcluded`, as on the
  [2014–2015 2.0T](volkswagen-cc-2014-2015-2.0t.md).
- `"engine": { "label": "2.0T", "displacementL": 2.0 }`.
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; DSG fluid and filter 40,000
  (not for a manual); spark plugs and air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the
  card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No tire rotation, and no special operating conditions: the card has neither.
