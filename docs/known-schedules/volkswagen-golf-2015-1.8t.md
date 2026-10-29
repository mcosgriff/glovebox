# Volkswagen Golf 1.8T, 2015 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-2015-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). The [2015 Golf SportWagen 1.8T](volkswagen-golf-sportwagen-2015-1.8t.md)
is made from the same card. This page also lists every Mk7 Golf, Golf SportWagen and Golf Alltrack card read
(below) and what isn't covered.

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2015 | "Golf and Golf SportWagen", 1.8T (posted December 2014), in full; the list was written from it | `2014/MC-10242450-9999` |
| 2015 | "Golf", 1.8T (posted June 2014), in full. Apart from the title and the posting date it is the same card, pixel for pixel; NHTSA files it twice | `2014/MC-10120118-9999`, `2014/MC-10240818-9999` |
| 2015 | Volkswagen's 2015 reissue, for the oil change's time limit only, as read for the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) (not read again here) | reissue `USA5R505821`, printed 11/24/2025 |

- **The card is the [2015 Jetta 1.8T](volkswagen-jetta-2015-1.8t.md)'s** (`2014/MC-10120123-9999`) with the
  model's name: the same rows, worded the same, with the same marks in the same columns. Read off the page,
  then checked row by row against the Jetta's card. So the list is that one's, in that list's wording:
  12 columns, 10,000 to 120,000, no months printed, not repeated (rule 9).
- Spark plugs at 60,000 and 120,000 with footnote 6, "every 6 years regardless of mileage": 60,000 / 72 mo.
- "Transmission: Automatic (non DSG): Change fluid" at 50,000 and 100,000: the `trans-fluid` service, a
  reminder every 50,000 miles, in `manualExcluded`. The card has no DSG row.
- **The engine.** The card is for the 1.8T, so the list has `"engine": { "label": "1.8T", "displacementL": 1.8 }`.
  The 2015 Golf TDI has no list (below), so a Golf with no engine on record is asked, with "1.8T" or
  "Neither". NHTSA's vPIC model list for Volkswagen, 2015 (`GetModelsForMakeYear`), has "Golf", "Golf
  SportWagen", "GTI" and "e-Golf" apart, and no "Golf R".
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo; automatic gearbox fluid 50,000 (not for a manual); brake fluid every
  24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2015 reissue, as on the Jetta Hybrid's page (the owner's ruling of
  2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first column.
- No special operating conditions are given on the card.

## Every Mk7 card read

| Model year | Card | Document | List |
|---|---|---|---|
| 2015 | Golf, 1.8T (June 2014) and Golf and Golf SportWagen, 1.8T (December 2014) | `2014/MC-10120118-9999`, `2014/MC-10242450-9999` | this list, [Golf SportWagen 2015](volkswagen-golf-sportwagen-2015-1.8t.md) |
| 2015 | Golf, 2.0L TDI (June 2014) and Golf and Golf SportWagen, 2.0L TDI (December 2014); the same card, pixel for pixel, but the title | `2014/MC-10120119-9999`, `2014/MC-10242448-9999` | none (below) |
| 2016 | Golf and Golf SportWagen, 1.8T (August 2015) | `2015/MC-10123075-9999` | [Golf 2016](volkswagen-golf-2016-1.8t.md), [Golf SportWagen 2016](volkswagen-golf-sportwagen-2016-1.8t.md) |
| 2016 | Golf and Golf SportWagen, 2.0L TDI (August 2015) | `2015/MC-10123074-9999` | none (below) |
| 2018 | All models except e-Golf, revised 11/26/2019 | `2019/MC-10168646-0001` | [Golf](volkswagen-golf-2018.md), [Golf SportWagen](volkswagen-golf-sportwagen-2018.md), [Golf Alltrack](volkswagen-golf-alltrack-2018.md) |
| 2019 | All models except e-Golf, revised 11/12/2020 | `2020/MC-10183295-0001` | [Golf 1.4T](volkswagen-golf-2019-1.4t.md), [Golf SportWagen 1.4T](volkswagen-golf-sportwagen-2019-1.4t.md) and [1.8T](volkswagen-golf-sportwagen-2019-1.8t.md), [Golf Alltrack](volkswagen-golf-alltrack-2019.md) |
| 2020 | All models except e-Golf, revised 11/26/2019 | `2019/MC-10168643-0001` | [Golf 1.4T, 2020–2021](volkswagen-golf-2020-2021-1.4t.md) |
| 2021 | All model gasoline vehicles, 7/9/2020 | `2020/MC-10177239-0001` | [Golf 1.4T, 2020–2021](volkswagen-golf-2020-2021-1.4t.md) |

NHTSA's index also files `2014/MC-10242449-9999`, "2.0TSI Engine (292 HP)", under the 2015 Golf and Golf
SportWagen. That is the Golf R's engine, which is #437's; it wasn't read. No 2015 or 2016 card is for a
1.4T, and no card before 2018 names the Golf Alltrack.

## What isn't covered

- **The 2.0L TDI, 2015 and 2016.** Both cards run 13 columns to 130,000, and the 130,000 column holds only
  "AdBlue: Check fluid level and add if necessary" and "Timing Belt: Replace"; every other job, the oil
  change included, stops at 120,000. That is the 2014 Golf TDI's column
  ([#434's question 2](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244)), with the
  AdBlue check as well, so no list is made until it is answered. Their DSG row is "(if applicable)" and
  would be table-only.
- **2017.** NHTSA's index has no 2017 card for the Golf, Golf SportWagen or Golf Alltrack. Volkswagen's 2017
  reissue would be the only source, and its 2016 reissue differs from the 2016 card from the time (spark
  plugs "Golf (AU1)" every 40,000 or 4 years, where the card says 60,000 or 6; the gearboxes by code), as
  the [2017 GTI's page](volkswagen-gti-2017.md#how-far-the-reissue-matches-a-card-from-the-time) found. So
  2017 waits on [#434's question 3](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244).
- **The gearbox rows of 2018–2021** are left out of every list made from the all-model cards: see the
  [2018 Golf's page](volkswagen-golf-2018.md).
- **The Golf SportWagen and Golf Alltrack, 2020 and 2021.** vPIC's model list has both for 2020 and neither
  for 2021. The newest 2020 card names neither: its spark plug rows name "Golf 1.4L", and the 2019 card names
  "Golf 1.4L" and "Golf SportWagen 1.4L" apart, so which row a 2020 SportWagen is on isn't said. NHTSA's index
  files only the earlier 2020 revisions (`2019/MC-10163843`, `2019/MC-10164263`) under the SportWagen. No list;
  asked on #436.
- The GTI and the Golf R have lists of their own (#143, #437); the e-Golf too (#438).
