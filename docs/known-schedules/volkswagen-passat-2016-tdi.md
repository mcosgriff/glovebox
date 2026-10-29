# Volkswagen Passat TDI, 2016 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2016-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.
Every Passat NMS card read, and what isn't covered, is on the [2014 2.5L's page](volkswagen-passat-2014-2.5.md).

| Model year | What was read | Document |
|---|---|---|
| 2016 | The Passat 2.0L TDI card (posted August 2015), in full; the list was written from it | `2015/MC-10123064-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The 2014–2015 TDI card in the 2016 layout**, as the [2016 1.8T](volkswagen-passat-2016-1.8t.md) card is
  the 2015 one's: the yearly airbag check, tire rotation, the door check lubrication, the rear washer nozzle
  check and the service sticker are gone, and the dust and pollen filter is every 40,000 miles or 2 years (at
  40,000, 80,000 and 120,000). Every other row and mark is the [2014–2015 TDI](volkswagen-passat-2014-2015-tdi.md)'s,
  in that list's wording, the 130,000 column included (the oil change, brake pad check, brake fluid, AdBlue
  check, service display and washer check, with the timing belt). The AdBlue footnote (now 5) is recorded as
  on that page.
- `"engine": { "label": "2.0L TDI", "displacementL": 2.0 }`.
- Reminders: engine oil 10,000 / 12 mo; fuel filter 20,000; dust and pollen filter 40,000 / 24 mo; DSG fluid
  and filter 40,000 (not for a manual); air filter 60,000 / 72 mo; timing belt 130,000; brake fluid every 24
  months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No tire rotation, and no special operating conditions: the card has neither.
