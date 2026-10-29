# Volkswagen Eos, 2016

How `app/src/main/assets/known-schedules/volkswagen-eos-2016.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Eos card read, and what isn't covered, is on the
[2014–2015 page](volkswagen-eos-2014-2015.md).

| Model year | What was read | Document |
|---|---|---|
| 2016 | The Eos 2.0L TSI card (posted August 2015), in full; the list was written from it | `2015/MC-10123076-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The card is the [2016 CC 2.0T](volkswagen-cc-2016-2.0t.md)'s grid with two rows added**: every other row
  has the same marks in the same columns, so the list uses that list's wording. Like that card it has no yearly
  airbag check, tire rotation, rear washer nozzle check or service sticker.
- The two added rows, both at 40,000, 80,000 and 120,000 and both table-only:
  - "Convertible roof, CSC roof, and Retractable hard top: Check for function and damage, clean and lubricate
    seals (if applicable)", read as on the [2014–2015 list](volkswagen-eos-2014-2015.md#the-eos-cards).
  - "Door checks and mounting pins: Lubricate (EOS only)": the Eos's own row, named "Lubricate door checks and
    mounting pins" as on the 2014–2015 list.
- **The dust and pollen filter**: the row is headed "(Every 2 years or 40,000 miles)" and drawn at 40,000,
  80,000 and 120,000; its footnote 1 says "Every 2 years, regardless of mileage: Dust and pollen filter: Replace
  (if applicable)" (the CC's footnote says "Every 2 years or 40,000 miles"). Both give 2 years, and the columns
  give 40,000, so the reminder is 40,000 miles or 24 months, as on the 2016 CC 2.0T.
- "Transmission: DSG (non Automatic): Change fluid and filter (if applicable)" at 40,000, 80,000 and 120,000:
  `trans-fluid`, every 40,000 miles, "(DSG only)", in `manualExcluded`, as on the 2014–2015 list.
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; DSG fluid and filter 40,000
  (not for a manual); spark plugs and air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the
  card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No tire rotation, and no special operating conditions: the card has neither.
