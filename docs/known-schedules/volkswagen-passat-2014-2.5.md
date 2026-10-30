# Volkswagen Passat 2.5L, 2014 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2014-2.5.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). This page also lists every card read for the Passat NMS
(2012–2022) and says what wasn't covered and why.

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

## The Passat NMS cards

| Model year | Engine | What was read | Document | List |
|---|---|---|---|---|
| 2014 | 2.5L | Posted July 2013, in full; NHTSA files it again as `MC-10242452` (not fetched) | `2013/MC-10125771-9999` | this one |
| 2014 | 1.8L TSI | Posted June 2013, in full; filed again as `MC-10242472` (not fetched) | `2013/MC-10125862-9999` | [2014–2015 1.8T](volkswagen-passat-2014-2015-1.8t.md) |
| 2014 | 3.6L | Posted June 2013, in full; filed again as `MC-10242474` (not fetched) | `2013/MC-10125860-9999` | [2014–2015 3.6L](volkswagen-passat-2014-2015-3.6.md) |
| 2014 | 2.0L TDI | Posted June 2013, in full; filed again as `MC-10242473` (not fetched) | `2013/MC-10125861-9999` | [2014–2015 TDI](volkswagen-passat-2014-2015-tdi.md) |
| 2015 | 1.8L TSI | Posted June 2014, in full | `2014/MC-10120125-9999` | [2014–2015 1.8T](volkswagen-passat-2014-2015-1.8t.md) |
| 2015 | 3.6L | Posted June 2014, in full | `2014/MC-10120127-9999` | [2014–2015 3.6L](volkswagen-passat-2014-2015-3.6.md) |
| 2015 | 2.0L TDI | Posted June 2014, in full | `2014/MC-10120126-9999` | [2014–2015 TDI](volkswagen-passat-2014-2015-tdi.md) |
| 2016 | 1.8L TSI | Posted August 2015, in full | `2015/MC-10123065-9999` | [2016 1.8T](volkswagen-passat-2016-1.8t.md) |
| 2016 | 2.0L TDI | Posted August 2015, in full | `2015/MC-10123064-9999` | [2016 TDI](volkswagen-passat-2016-tdi.md) |
| 2016 | 3.6L | Headed "2015", posted August 2015, in full; read as the 2016 card (#553) | `2015/MC-10123080-9999` | [2016 3.6L](volkswagen-passat-2016-3.6.md) |
| 2018 | all models | Revised 11/26/2019, the newest revision | `2019/MC-10168646-0001` | [2018 2.0T](volkswagen-passat-2018-2.0t.md), [2018 3.6L](volkswagen-passat-2018-3.6.md) |
| 2019 | all models | Revised 11/12/2020, the newest revision | `2020/MC-10183295-0001` | [2019](volkswagen-passat-2019.md) |
| 2020 | all models | Revised 11/26/2019, the newest revision | `2019/MC-10168643-0001` | [2020–2021](volkswagen-passat-2020-2021.md) |
| 2021 | all gasoline models | 7/9/2020, the only one | `2020/MC-10177239-0001` | [2020–2021](volkswagen-passat-2020-2021.md) |
| 2022 | all gasoline models | Revised 7/21/2021, the newest revision | `2021/MC-10199495-0001` | [2022](volkswagen-passat-2022.md) |

NHTSA's index files `MC-10240824`, `MC-10240825` and `MC-10240826` ("Maintenance Schedule For USA", filed in
2023) under the 2015 Passat; they weren't fetched. The 2014 and 2015 per-engine cards are read off the page the
way the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) and [2015 Jetta 1.8T](volkswagen-jetta-2014-2015-1.8t.md) cards
were, and the 2016 ones as the [2016 Jetta 1.8T](volkswagen-jetta-2016-1.8t.md) card was. A script's reading
of every grid was compared row by row between the cards (as a pointer only; each difference was looked at on
the page).

**One list per engine** (the owner's ruling of 2026-10-23): each card is for one engine, named in its heading.
NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`) names the car "Passat", apart from "CC", in
2013–2019 (read 2026-10-28 and 2026-10-29), so every list is for "Passat" with an `engine`. No 2014–2019 Passat had two
engines of one size, so no list needs `fuel`. A Passat with no engine on record is asked which it has.

**The gearbox rows.** The per-engine cards print one self-shifting gearbox row each, and name no other:
"Transmission: Automatic (non DSG): Change fluid" or "Transmission: DSG (non Automatic): Change fluid and
filter (if applicable)". A card with only the automatic's row gives its `trans-fluid` reminder in
`manualExcluded`; a card with only the DSG's gives the DSG's, read as the [2010–2014 GTI](volkswagen-gti-2010-2014.md)
list reads the same row on the 2014 GTI card (`manualExcluded`, "(DSG only)", every 40,000). The one exception
is the 2014 1.8T, which takes the 2015 card's automatic row ([its page](volkswagen-passat-2014-2015-1.8t.md)).

## This list

- **The card is the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md)'s grid**: the same rows with the same marks
  in the same columns (only the brake fluid footnote's wording differs: "At the 3 year mark, then every 2
  years"). The wording is that list's, except the tire check (no tire age) and the washer check (the
  headlight washers too), which the card prints as the 2015 Jetta 1.8T's card does, so those two rows use the
  [2015 Jetta 1.8T](volkswagen-jetta-2014-2015-1.8t.md)'s wording.
- `"engine": { "label": "2.5L", "displacementL": 2.5 }`.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs 40,000 / 72 mo (drawn at 40,000, 80,000 and 120,000, with footnote 6, "every 6 years"); automatic
  gearbox fluid 50,000 (not for a manual); air filter 60,000 / 72 mo; brake fluid every 24 months by time
  alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 reissue (`USA5R505521`, printed 11/24/2025), as read for the
  [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here; the owner's ruling of 2026-10-28,
  #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.

## What isn't covered

- **2012 and 2013.** NHTSA's index has no Passat card for either year. Volkswagen's 2025 reissues would be the
  only source; they weren't read for the Passat, and for the Golf they differ from the cards from the time
  ([#434's question 3](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244)).
- The 2014 1.8T's gearbox row and the two 3.6L cards headed "2015" were settled by the owner on 2026-10-29
  (#553, [part B and C](https://github.com/mcosgriff/glovebox/issues/553#issuecomment-6511733565)): see the [2014–2015 1.8T](volkswagen-passat-2014-2015-1.8t.md),
  [2014–2015 3.6L](volkswagen-passat-2014-2015-3.6.md) and [2016 3.6L](volkswagen-passat-2016-3.6.md) pages.
- **2017.** NHTSA's index has two "New Publication" filings under the 2017 Passat (`MC-10107219`,
  `MC-10107220`); `2016/MC-10107219-9999` and `2017/MC-10107219-9999` are not there (404), as for the Jetta's
  2017 filings. Volkswagen's 2017 reissue would be the only source; left out, as for #434's question 3.
- **The 2018 and 2019 gearbox rows**: see the [2018 2.0T's page](volkswagen-passat-2018-2.0t.md). The 2020–2022
  automatic row: see the [2020–2021 page](volkswagen-passat-2020-2021.md); asked on #445.
- The 2010 B6 has [its own list](volkswagen-passat-2010-2.0t.md); the 2020–2022 Passat's lists are
  [2020–2021](volkswagen-passat-2020-2021.md) and [2022](volkswagen-passat-2022.md).
