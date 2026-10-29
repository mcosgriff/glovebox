# Volkswagen Jetta 1.8T, 2015 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2015-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2015 | The Jetta Sedan 1.8L TSI card (posted June 2014), in full; the list was written from it | `2014/MC-10120123-9999` |
| 2015 | Volkswagen's 2015 reissue, for the oil change's time limit only, as read for the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) (not read again here) | reissue `USA5R505821`, printed 11/24/2025 |

- **The card is the [2014 Jetta SportWagen 2.5L](volkswagen-jetta-sportwagen-2014-2.5.md)'s grid** with the
  model's and engine's name: the same rows with the same marks in the same columns, except the spark plugs.
  So the list is that one's, in that list's wording, with these differences:
  - spark plugs at 60,000 and 120,000, each with footnote 6, "every 6 years regardless of mileage": a
    reminder every 60,000 miles or 72 months (the SportWagen 2.5L's are at 40,000, 80,000 and 120,000);
  - the tire check names no tire age and the washer check adds the headlight washers, so those two rows use
    the [Jetta Hybrid](volkswagen-jetta-2015-hybrid.md)'s wording (the card prints them as the SportWagen's
    card does).
- A grid of 12 columns, 10,000 to 120,000, no months printed, not repeated (rule 9).
- "Transmission: Automatic (non DSG): Change fluid" at 50,000 and 100,000: the `trans-fluid` service, a
  reminder every 50,000 miles, in `manualExcluded`. The card has no DSG row.
- **The engine.** The card is for the 1.8L TSI, so the list has
  `"engine": { "label": "1.8T", "displacementL": 1.8 }`. No other 2015 Jetta list is 1.8L. A Jetta with no
  engine on record is asked which it has. NHTSA's vPIC model list for Volkswagen, 2015
  (`GetModelsForMakeYear`), has "Jetta" and no "GLI" or "Jetta Sedan".
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo; automatic gearbox fluid 50,000 (not for a manual); brake fluid every
  24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2015 reissue, as on the Jetta Hybrid's page (the owner's ruling of
  2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first column.
- **Not covered for 2015** (see the [2014 1.8T's page](volkswagen-jetta-2014-1.8t.md#what-isnt-covered)):
  the 2.0L MPI and the GLI 2.0L TSI, and the 2.0L TDI.
