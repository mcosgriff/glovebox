# Volkswagen Jetta 1.8T, 2014 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2014-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). This page also says what wasn't covered for the Mk6 Jetta and
GLI (2011–2018) and why.

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Jetta Sedan 1.8L TSI card posted July 2013, in full; the list was written from it. NHTSA files it again as `MC-10242451`, byte for byte the same | `2013/MC-10125772-9999`, `2014/MC-10242451-9999` |
| 2014 | The Jetta Sedan 1.8L TSI card posted June 2013, in full. It differs from the July card in its gearbox row only (below) | `2013/MC-10125797-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here) | reissue `USA5R505521`, printed 11/24/2025 |

NHTSA's index also files `MC-10125798` and `MC-10125799` under the 2014 Jetta (posted June 2013, both read):
the 2.0L MPI and 2.0L TDI cards, below. The 2014 GLI's card, `MC-10125796`, is filed under the Jetta Hybrid
(below). `MC-10242462`, `MC-10242467` and `MC-10242468`, "2.0L TSI Engine", dated June 2013 and filed in 2023,
weren't fetched.

- **The list is the [2015 1.8T](volkswagen-jetta-2015-1.8t.md)'s, without the gearbox row.** The July 2013
  card and the 2015 card print the same rows with the same marks; only the year in the heading and the
  copyright differ. The jobs, wording, engine and reminders are as on that page.
- **Left out, asked on #440: the gearbox row.** The two 2014 cards differ in one row:
  - June 2013 (`MC-10125797`): "Transmission: DSG (non Automatic): Change fluid and filter (if applicable)"
    at 40,000, 80,000 and 120,000;
  - July 2013 (`MC-10125772`, filed again in 2014 as `MC-10242451`): "Transmission: Automatic (non DSG):
    Change fluid" at 50,000 and 100,000, as on the 2015 card.

  The July card reads as a correction of the June one, but the owner's ruling that the newest revision wins
  is for the 2018-on all-model cards (#143), so nothing is chosen here. If the July card is taken, this list
  and the 2015 one are the same and become one 2014–2015 list.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years
  first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 reissue, as on the Golf's page (the owner's ruling of
  2026-10-28, #517). The table has none.
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
- **The 2.0L TDI, 2014–2016** (`2013/MC-10125799-9999` for 2014 and `2015/MC-10123066-9999` for 2016, both
  read; the 2015 card, `MC-10121579`, wasn't fetched, and NHTSA's index says it runs to 130,000 too). The grid has 13 columns, to 130,000, and the 130,000
  column has only "Timing Belt: Replace", as on the 2014 Golf TDI's card: this waits on
  [#434's question 2](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244).
- **2017.** NHTSA's index has no Jetta card for 2017 (two "New Publication" filings under the 2017 Jetta,
  `MC-10107217` and `MC-10107218`, aren't at the usual address). Volkswagen's 2017 reissue (`USA5R506721`) would
  be the only source, and it differs from the cards either side: it gives "Jetta (163)" spark plugs every
  60,000 or 6 years, where the 2016 1.4T card gives 4 years and the 2018 card 40,000 or 4 years for every
  Jetta engine, and it names the Jetta under the `09G`, `09S`, `01M` and `02E (DSG)` gearboxes. Left out, as
  for #434's question 3.
- **2016 1.4T and 1.8T** have lists of their own ([1.4T](volkswagen-jetta-2016-1.4t.md),
  [1.8T](volkswagen-jetta-2016-1.8t.md)), and **2018** one for every engine
  ([2018](volkswagen-jetta-2018.md)).
