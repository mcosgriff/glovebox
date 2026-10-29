# Volkswagen Jetta SportWagen 2.5L, 2014 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-jetta-sportwagen-2014-2.5.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Jetta SportWagen 2.5L card (posted September 2013), in full; the list was written from it. NHTSA files it again as `MC-10157419` (not fetched) and `MC-10242471`, which is byte for byte the same | `2013/MC-10155822-9999`, `2014/MC-10242471-9999` |
| 2014? | A Jetta SportWagen 2.5L card posted June 2013, filed by NHTSA under 2014 but headed "2013 Volkswagen Jetta SportWagen" on every page (below). Same jobs in the same columns. NHTSA's index also files `MC-10242470`, "2.5L Engine", dated June 2013 (not fetched) | `2013/MC-10125863-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here) | reissue `USA5R505521`, printed 11/24/2025 |

- **The card is the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md)'s with the model's name**: the same rows,
  in the same columns, with the same marks; only the line breaks and footnote 4's wording ("At the 3 year
  mark, then every 2 years regardless of miles driven") differ. So this list is the Golf's, for "Jetta
  SportWagen", in that list's wording:
  - a grid of 12 columns, 10,000 to 120,000, no months printed, not repeated (rule 9);
  - spark plugs at 40,000, 80,000 and 120,000, each with footnote 6, every 6 years: 40,000 / 72 mo;
  - "Transmission: Automatic (non DSG): Change fluid" at 50,000 and 100,000: the `trans-fluid` service, a
    reminder every 50,000 miles, in `manualExcluded`. The card has no DSG row.
- **The model and engine.** NHTSA's vPIC model list for Volkswagen, 2014 (`GetModelsForMakeYear`), names
  "Jetta SportWagen" apart from "Jetta". The card is for the 2.5L, so the list has
  `"engine": { "label": "2.5L", "displacementL": 2.5 }`; a Jetta SportWagen with no engine on record is asked
  which it has.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs 40,000 / 72 mo; automatic gearbox fluid 50,000 (not for a manual); air filter 60,000 / 72 mo; brake
  fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 reissue, as on the Golf's page (the owner's ruling of 2026-10-28,
  #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first two
  columns.
- **Not 2013 yet, asked on #439.** `MC-10125863` (posted June 2013) is headed "2013" on all three pages;
  NHTSA's index files it under model year 2014, and its description begins "2013 Volkswagen Jetta
  SportWagen". Its jobs and columns are the 2014 card's. It may be the 2013 card or a 2014 card misheaded
  and posted again, corrected, in September 2013; nothing on it says which, so the list stays 2014.
- **Not covered:** the 2.0L TDI for 2014 (see the [2010 TDI's page](volkswagen-jetta-sportwagen-2010-tdi.md);
  it waits on #434's question 2), 2011–2013 (see the
  [2010 2.5L's page](volkswagen-jetta-sportwagen-2010-2.5.md)), and the Golf SportWagen from 2015.
