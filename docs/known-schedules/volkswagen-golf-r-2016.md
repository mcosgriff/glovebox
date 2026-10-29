# Volkswagen Golf R, 2016 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-r-2016.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 and Mk8 Golf R card read, and what isn't covered, is on the
[2015 Golf R's page](volkswagen-golf-r-2015.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, filed with NHTSA as manufacturer communications. Read on
2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2016 | "2016 Volkswagen Golf R Maintenance Schedule For USA, 2.0TSI Engine (292 HP)" (posted August 2015), in full; the list was written from it | `2015/MC-10123073-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) and the [2016 Golf](volkswagen-golf-2016-1.8t.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The card is the [2016 GTI](volkswagen-gti-2016.md)'s with one row more**: the same rows, worded the same, with
  the same marks in the same columns, read off the page and then checked row by row against that list. So the list is
  that one's, in its wording, with the Haldex row.
- **What differs from 2015** is what differs on the GTI's cards: no tire rotation, yearly airbag check, door check
  lubrication, rear window washer nozzle check or service sticker; the dust and pollen filter every 40,000 miles or
  2 years, drawn at 40,000, 80,000 and 120,000.
- **"Haldex Clutch: Change oil (if applicable)"** at 30,000, 60,000, 90,000 and 120,000, footnote 4, "Every 3 years,
  regardless of mileage": table-only, with no `service`, as on the [2015](volkswagen-golf-r-2015.md) and
  [2013](volkswagen-golf-r-2013.md) lists.
- "Transmission: DSG (non Automatic): Change fluid and filter" at 40,000, 80,000 and 120,000: "Change DSG
  transmission fluid and filter (DSG only)", a reminder every 40,000 miles in `manualExcluded`. No row for a
  conventional automatic.
- Spark plugs and the air filter at 60,000 and 120,000, "every 6 years" (footnotes 6 and 7): 60,000 / 72 mo.
- No engine block: the card is for the 2.0TSI, the Golf R's only engine.
- **Model name.** vPIC's model list for Volkswagen, 2016 (`GetModelsForMakeYear`), has "Golf R" apart from "Golf",
  "Golf SportWagen", "Golf GTI" and "e-Golf".
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; DSG fluid and filter 40,000 (not for
  a manual); air filter and spark plugs 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at
  3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No special operating conditions are given on the card.
