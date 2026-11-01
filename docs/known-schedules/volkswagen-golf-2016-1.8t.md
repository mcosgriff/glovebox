# Volkswagen Golf 1.8T, 2016 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-2016-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). The [2016 Golf SportWagen 1.8T](volkswagen-golf-sportwagen-2016-1.8t.md)
is made from the same card. Every Mk7 card read, and what isn't covered, is on the
[2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, filed with NHTSA as manufacturer communications.
Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2016 | "Golf and Golf SportWagen", 1.8T (posted August 2015), in full; the list was written from it | `2015/MC-10123075-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The card is the [2016 Jetta 1.8T](volkswagen-jetta-2016-1.8t.md)'s** (`2015/MC-10123068-9999`) with the
  model's name: the same rows, worded the same, with the same marks in the same columns. Read off the page,
  then checked row by row against the Jetta's card. So the list is that one's, in that list's wording.
- **What differs from 2015** is what differs on the Jetta's cards: no tire rotation, yearly airbag check, door
  check lubrication, rear window washer nozzle check or service sticker; the dust and pollen filter every
  40,000 miles or 2 years, drawn at 40,000, 80,000 and 120,000; "Transmission: Automatic (non DSG): Change
  fluid" at 80,000 only.
- Spark plugs at 60,000 and 120,000, footnote 5, "every 6 years regardless of mileage": 60,000 / 72 mo.
- **The engine.** `"engine": { "label": "1.8T", "displacementL": 1.8 }`. The [2016 Golf TDI](volkswagen-golf-2016-tdi.md) has a list
  too, so a Golf with no engine on record is asked, with "1.8T", "2.0L TDI" or "Neither". vPIC's model list for Volkswagen,
  2016 (`GetModelsForMakeYear`), has "Golf", "Golf SportWagen", "Golf GTI", "Golf R" and "e-Golf" apart.
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; spark plugs and air filter
  60,000 / 72 mo; automatic gearbox fluid 80,000 (not for a manual); brake fluid every 24 months by time
  alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No special operating conditions are given on the card.
