# Volkswagen Passat 1.8T, 2014 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2014-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.
Every Passat NMS card read, and what isn't covered, is on the [2014 2.5L's page](volkswagen-passat-2014-2.5.md).

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Passat 1.8L TSI card (posted June 2013), in full; the list was written from it | `2013/MC-10125862-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here) | reissue `USA5R505521`, printed 11/24/2025 |

- **The list is the [2015 1.8T](volkswagen-passat-2015-1.8t.md)'s, without the gearbox row.** The 2014 and
  2015 cards print the same rows with the same marks but for that row; the jobs, wording, engine and the
  other reminders are as on that page. The 2014 card is also, row for row, the 2014 3.6L card.
- **Left out, asked on #444: the gearbox row.** The June 2013 card has "Transmission: DSG (non Automatic):
  Change fluid and filter (if applicable)" at 40,000, 80,000 and 120,000 and no automatic's row; the 2015
  1.8T card (June 2014) has "Transmission: Automatic (non DSG): Change fluid" at 50,000 and 100,000 instead.
  This is the June 2013 Jetta 1.8T card's row, which Volkswagen's July 2013 Jetta card replaced with the
  automatic's ([#440's question 1](https://github.com/mcosgriff/glovebox/issues/440#issuecomment-6489388227)).
  No later 2014 Passat 1.8T card was found, so nothing is chosen here.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years
  first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.
