# Volkswagen Jetta 1.8T, 2014–2015 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2014-2015-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). This page also says what wasn't covered for the Mk6 Jetta and
GLI (2011–2018) and why.

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Jetta Sedan 1.8L TSI card posted July 2013, in full; the list's source link. NHTSA files it again as `MC-10242451`, byte for byte the same. Row for row the 2015 card | `2013/MC-10125772-9999`, `2014/MC-10242451-9999` |
| 2014 | The Jetta Sedan 1.8L TSI card posted June 2013, in full. It differs from the July card in its gearbox row only (below) | `2013/MC-10125797-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here) | reissue `USA5R505521`, printed 11/24/2025 |
| 2015 | The Jetta Sedan 1.8L TSI card (posted June 2014), in full; the list was written from it | `2014/MC-10120123-9999` |
| 2015 | Volkswagen's 2015 reissue, for the oil change's time limit only, as read for the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) (not read again here) | reissue `USA5R505821`, printed 11/24/2025 |

NHTSA's index also files `MC-10125798` and `MC-10125799` under the 2014 Jetta (posted June 2013, both read):
the 2.0L MPI card, below, and the 2.0L TDI card, which has [a list of its own](volkswagen-jetta-2014-tdi.md). The 2014 GLI's card, `MC-10125796`, is filed under the Jetta Hybrid
(below). `MC-10242462`, `MC-10242467` and `MC-10242468`, "2.0L TSI Engine", dated June 2013 and filed in 2023,
weren't fetched.

- **The 2015 card is the [2014 Jetta SportWagen 2.5L](volkswagen-jetta-sportwagen-2014-2.5.md)'s grid** with the
  model's and engine's name: the same rows with the same marks in the same columns, except the spark plugs.
  So the list is that one's, in that list's wording, with these differences:
  - spark plugs at 60,000 and 120,000, each with footnote 6, "every 6 years regardless of mileage": a
    reminder every 60,000 miles or 72 months (the SportWagen 2.5L's are at 40,000, 80,000 and 120,000);
  - the tire check names no tire age and the washer check adds the headlight washers, so those two rows use
    the [Jetta Hybrid](volkswagen-jetta-2015-hybrid.md)'s wording (the card prints them as the SportWagen's
    card does).
- **2014's gearbox row: the July 2013 card** (the owner's ruling of 2026-10-29 on #553, [part A](https://github.com/mcosgriff/glovebox/issues/553#issuecomment-6511794772):
  the newest printing wins for the 2013–2016 per-model cards too). The two 2014 cards differ in one row:
  - June 2013 (`MC-10125797`): "Transmission: DSG (non Automatic): Change fluid and filter (if applicable)"
    at 40,000, 80,000 and 120,000;
  - July 2013 (`MC-10125772`, filed again in 2014 as `MC-10242451`): "Transmission: Automatic (non DSG):
    Change fluid" at 50,000 and 100,000, as on the 2015 card.

  Taking the July card, the 2014 and 2015 cards print the same rows with the same marks (only the year in the
  heading and the copyright differ), so the two years are one list. Until 2026-10-29 2014 had a list of its own
  without the gearbox row (asked on #440's question 1).
- A grid of 12 columns, 10,000 to 120,000, no months printed, not repeated (rule 9).
- "Transmission: Automatic (non DSG): Change fluid" at 50,000 and 100,000: the `trans-fluid` service, a
  reminder every 50,000 miles, in `manualExcluded`. Neither the 2015 card nor the July 2013 card has a DSG row.
- **The engine.** The card is for the 1.8L TSI, so the list has
  `"engine": { "label": "1.8T", "displacementL": 1.8 }`. No other 2014 or 2015 Jetta list is 1.8L. A Jetta with no
  engine on record is asked which it has. NHTSA's vPIC model list for Volkswagen, 2015
  (`GetModelsForMakeYear`), has "Jetta" and no "GLI" or "Jetta Sedan".
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo; automatic gearbox fluid 50,000 (not for a manual); brake fluid every
  24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 and 2015 reissues, as on the Golf's and the Jetta Hybrid's pages (the owner's ruling of 2026-10-28,
  #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.

## What isn't covered

- **2011–2013.** NHTSA's index has no Jetta sedan card for these years (only the Jetta Hybrid's, from 2013).
  Volkswagen's 2025 reissues would be the only source; they differ from the cards from the time (see the
  Golf's [question 3 on #434](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244)).
- **The 2.0L gasoline engines, 2014–2016: the 2.0L MPI and the GLI's 2.0L TSI.** Each has its own card:
  - 2.0L MPI: `2013/MC-10125798-9999` (2014), `2014/MC-10120124-9999` (2015), `2015/MC-10123067-9999` (2016);
  - GLI 2.0L TSI: `2013/MC-10125796-9999` (2014, filed under the Jetta Hybrid, headed "2014 Volkswagen GLI
    Jetta"), `2014/MC-10120121-9999` (2015, headed "2015 Volkswagen GLI Jetta"; NHTSA files it again under the
    Jetta Hybrid as `2014/MC-10240821-9999`, byte for byte the same) and `2015/MC-10123071-9999` (2016).

  They differ: the MPI's spark plugs are every 4 years (the GLI's every 6), it has "Timing Belt: Check and
  replace if necessary (2.0L MPI only)" with its own check intervals, and it has the automatic's fluid row
  where the GLI has "Transmission: DSG (non Automatic): Change fluid and filter (if applicable)" at 40,000,
  80,000 and 120,000. vPIC names both "Jetta" (it has no "GLI" model in these years), and both are 2.0L
  gasoline, so neither the engine's size nor its fuel tells them apart, and the format has nothing else that
  would. A list for either would be found for both, so neither is made; asked on #440.
- **The 2.0L TDI, 2014–2016** has a list per year: [2014](volkswagen-jetta-2014-tdi.md) (`2013/MC-10125799-9999`),
  [2015](volkswagen-jetta-2015-tdi.md) (`2014/MC-10121579-9999`, fetched on 2026-10-29) and
  [2016](volkswagen-jetta-2016-tdi.md) (`2015/MC-10123066-9999`). The 2014 card's 130,000 column has only
  "Timing Belt: Replace", so it is left out and the belt is a reminder apart from the table (the owner's ruling of 2026-10-29 on [#551](https://github.com/mcosgriff/glovebox/issues/551), part A).
  The 2015 and 2016 cards' 130,000 column is a service of its own (oil, AdBlue, brake pads and more), so
  those lists keep it, as the Passat TDI's does.
- **2017.** NHTSA's index has no Jetta card for 2017 (two "New Publication" filings under the 2017 Jetta,
  `MC-10107217` and `MC-10107218`, aren't at the usual address). Volkswagen's 2017 reissue (`USA5R506721`) would
  be the only source, and it differs from the cards either side: it gives "Jetta (163)" spark plugs every
  60,000 or 6 years, where the 2016 1.4T card gives 4 years and the 2018 card 40,000 or 4 years for every
  Jetta engine, and it names the Jetta under the `09G`, `09S`, `01M` and `02E (DSG)` gearboxes. Left out, as
  for #434's question 3.
- **2016 1.4T and 1.8T** have lists of their own ([1.4T](volkswagen-jetta-2016-1.4t.md),
  [1.8T](volkswagen-jetta-2016-1.8t.md)), and **2018** one for every engine
  ([2018](volkswagen-jetta-2018.md)).
