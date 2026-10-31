# Volkswagen CC 2.0T, 2014–2015

How `app/src/main/assets/known-schedules/volkswagen-cc-2014-2015-2.0t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). This page also lists every card read for the CC (2010–2017)
and says what wasn't covered and why.

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

## The CC cards

| Model year | Engine | What was read | Document | List |
|---|---|---|---|---|
| 2014 | 2.0L TSI | Posted June 2013, in full; the list was written from it. NHTSA files it again as `MC-10242459` (not fetched) | `2013/MC-10125793-9999` | this one |
| 2015 | 2.0L TSI | Posted June 2014, in full; filed again as `MC-10242414` and `MC-10243076` (not fetched) | `2014/MC-10120114-9999` | this one |
| 2014 | 3.6L FSI | Posted June 2013, in full; filed again as `MC-10242460` (not fetched) | `2013/MC-10125794-9999` | [2014–2015 3.6L](volkswagen-cc-2014-2015-3.6.md) |
| 2015 | 3.6L FSI | Posted June 2014, in full; filed again as `MC-10242415` and `MC-10243077` (not fetched) | `2014/MC-10120115-9999` | [2014–2015 3.6L](volkswagen-cc-2014-2015-3.6.md) |
| 2016 | 2.0L TSI | Posted August 2015, in full | `2015/MC-10123079-9999` | [2016 2.0T](volkswagen-cc-2016-2.0t.md) |
| 2016 | 3.6L FSI | Posted August 2015, in full | `2015/MC-10123078-9999` | [2016 3.6L](volkswagen-cc-2016-3.6.md) |
| 2014–2016 | all models | Volkswagen's reissues for 2014 (`USA5R505521`), 2015 (`USA5R505821`) and 2016 (`USA5R506521`), printed November 2025, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md), [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) and [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here). Each one's model codes on Volkswagen's owners-literature index include the "Passat CC" (2016: `358`) | owners literature | every list |

A script's reading of every grid was compared row by row between the cards (as a pointer only; each
difference was looked at on the page). The 2014 and 2015 cards of each engine print the same rows with the
same marks in the same columns and the same footnotes; only the grey "Carefree Maintenance" shading differs
(20,000 is shaded on the 2014 cards, not on the 2015 ones), and it is left out. So each engine has one
2014–2015 list. The 2016 cards are the 2016 Passat cards' layout (below).

**One list per engine** (the owner's ruling of 2026-10-23): each card is for one engine, named in its heading.
NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`) names the car "CC" in 2014–2018 (read
2026-10-28), so every list is for "CC" with an `engine`. The CC had no two engines of one size, so no list
needs `fuel`. A CC with no engine on record is asked which it has.

**The gearbox rows.** The 2.0L TSI cards print one gearbox row, "Transmission: DSG (non Automatic): Change
fluid and filter (if applicable)", and no automatic's row. It is read as the [2014 Passat 3.6L](volkswagen-passat-2014-2015-3.6.md)
and the [2010–2014 GTI](volkswagen-gti-2010-2014.md) read the same row: `trans-fluid`, a reminder every 40,000
miles, named "(DSG only)", in `manualExcluded`. The 3.6L cards print no gearbox row at all, so those lists have none.

**The Haldex clutch** (the 3.6L cards only): see the [2014–2015 3.6L's page](volkswagen-cc-2014-2015-3.6.md).

## This list

- **The card is the [2014 Passat 3.6L](volkswagen-passat-2014-2015-3.6.md)'s grid**: the same rows with the same
  marks in the same columns, DSG row included (a script's comparison of the two lists' tables, then the pages).
  So the list is that one's, in its wording.
- `"engine": { "label": "2.0T", "displacementL": 2.0 }`.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; DSG fluid
  and filter 40,000 (not for a manual); spark plugs and air filter 60,000 / 72 mo (footnotes 6 and 7, "every 6
  years"); brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 and 2015 reissues (the owner's ruling of 2026-10-28, #517). The
  table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.

## What isn't covered

- **2010.** The 2010 card is Volkswagen's card for all models except the Routan, read for the
  [2010 Passat](volkswagen-passat-2010.md). vPIC's 2010 model list for Volkswagen has no "CC", and a vPIC
  decode of the CC's VIN pattern for 2010 (`WVWML7AN*AE`, read 2026-10-29) names the model "Passat" (trim
  "Sport"). A 2010 CC as NHTSA decodes it is therefore matched by the 2010 Passat list, which was read from the
  same card: the card names the CC only in its brake fluid row (with the Passat), and has no row for the CC
  alone or against it. No list named "CC" is made for 2010: no vPIC name for the year would ever match it.
  That list's open question about the DSG row ([#443](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333))
  applies to the 2010 CC as well (a DSG 2.0T and a conventional-automatic 3.6L).
- **2011, 2012, 2013 and 2017.** NHTSA's index has no CC card for those years. Volkswagen's 2025 reissues
  would be the only source; they weren't read for the CC, and for the Golf they differ from the cards from the
  time ([#434's question 3](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244)). vPIC's
  2013 model list has no "CC" either, so a 2011–2013 CC may decode as "Passat", which has no list for those years.
