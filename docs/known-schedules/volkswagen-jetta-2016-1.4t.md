# Volkswagen Jetta 1.4T, 2016 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2016-1.4t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2016 | The Jetta Sedan 1.4T card (posted August 2015), in full; the list was written from it | `2015/MC-10123069-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The card is the [2016 1.8T](volkswagen-jetta-2016-1.8t.md)'s** with the engine's name: the same rows in
  the same columns, except the spark plugs, drawn at 40,000, 80,000 and 120,000 with footnote 5, "every 4 years
  regardless of mileage": a reminder every 40,000 miles or 48 months. Everything else, and the wording, is as on
  the 1.8T's page.
- **The engine.** `"engine": { "label": "1.4T", "displacementL": 1.4, "powertrain": "gas" }`. The
  [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) is 1.4L too and vPIC names it "Jetta" as well, so the two
  are told apart by powertrain. A blank powertrain is never taken for petrol, so a 1.4L Jetta whose
  powertrain isn't on record is asked which engine it has ("1.4T" or "1.4L hybrid").
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter and spark plugs 40,000 (24 and 48 mo); air
  filter 60,000 / 72 mo; automatic gearbox fluid 80,000 (not for a manual); brake fluid every 24 months by time
  alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No tire rotation, and no special operating conditions: the card has neither.
- **Not covered:** see the [2014 1.8T's page](volkswagen-jetta-2014-2015-1.8t.md#what-isnt-covered).
