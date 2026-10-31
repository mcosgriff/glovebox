# Volkswagen Golf R, 2015 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-r-2015.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). This page also lists every Mk7 and Mk8 Golf R card read (below) and
what isn't covered.

Source: Volkswagen's "Maintenance Schedule For USA" card for the Golf R, which Volkswagen files with NHTSA as a
manufacturer communication (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are in
NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2015 | "2015 Volkswagen Golf R Maintenance Schedule For USA, 2.0TSI Engine (292 HP)" (posted December 2014), in full; the list was written from it. NHTSA's index files it under the Golf and Golf SportWagen | `2014/MC-10242449-9999` |
| 2015 | Volkswagen's 2015 reissue, for the oil change's time limit only, as read for the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) and the [2015 Golf](volkswagen-golf-2015-1.8t.md) (not read again here) | reissue `USA5R505821`, printed 11/24/2025 |

## The card

- **It is the [2013 Golf R](volkswagen-golf-r-2013.md)'s card with one row more**: the same rows, worded the same,
  with the same marks in the same columns, read off the page and then checked row by row against that list. So the
  list is that one's, in its wording: 12 columns, 10,000 to 120,000, no months printed, not repeated (rule 9). It is
  equally the [2015 GTI](volkswagen-gti-2015.md)'s list with the Haldex row.
- **The row more is the gearbox**: "Transmission: DSG (non Automatic): Change fluid and filter" at 40,000, 80,000
  and 120,000, with no "(if applicable)". Named as on the GTI list, "Change DSG transmission fluid and filter (DSG
  only)", `trans-fluid`, a reminder every 40,000 miles in `manualExcluded` (only a car known to be a manual loses it).
  The card has no row for a conventional automatic.
- **"Haldex Clutch: Change oil (if applicable)"** at 30,000, 60,000, 90,000 and 120,000, footnote 5, "Every 3 years,
  regardless of mileage", is
  read as the [CC 3.6L](volkswagen-cc-2014-2015-3.6.md) reads it, by the owner's ruling on [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part A, confirmed for every list in [the second round](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511795038)): named "Change Haldex clutch oil, every 3 years (AWD)", in `allWheelDriveOnly`,
  and a reminder every 36 months by time alone, with no `service` (no catalog line names the job). The Golf R was
  sold only with 4Motion, so every Golf R keeps it unless one is on record as two-wheel drive.
- Spark plugs at 60,000 and 120,000 with footnote 7, "every 6 years": 60,000 / 72 mo. The air filter the same
  (footnote 8).
- No engine block: the card is for the 2.0TSI, the Golf R's only engine.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; DSG fluid and
  filter 40,000 (not for a manual); air filter and spark plugs 60,000 / 72 mo; Haldex clutch oil every 36 months by
  time alone (AWD); brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2015 reissue, as on the 2015 Golf's page (the owner's ruling of 2026-10-28,
  #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first column.
- No special operating conditions are given on the card.
- **Model name.** vPIC's model list for Volkswagen, 2015 (`GetModelsForMakeYear`, read 2026-10-29), has no "Golf R"
  (it has "Golf", "Golf SportWagen", "GTI" and "e-Golf"), and a decode of the pattern `WVWVF7AU*FW` (model year 2015)
  gives the 292 HP 2.0L but no model. The list is for "Golf R", as the card and vPIC's 2016 and later lists name it,
  with no alias ("Golf" is not the Golf R). A 2015 Golf R that vPIC decodes as "Golf" finds no list (the 2015 Golf
  list is for the 1.8T); one named "Golf R" by the owner finds this one.

## Every Mk7 and Mk8 Golf R card read

| Model year | Card | Document | List |
|---|---|---|---|
| 2015 | Golf R, 2.0TSI (292 HP), December 2014 | `2014/MC-10242449-9999` | this list |
| 2016 | Golf R, 2.0TSI (292 HP), August 2015 | `2015/MC-10123073-9999` | [2016](volkswagen-golf-r-2016.md) |
| 2018 | All models except e-Golf, revised 11/26/2019 | `2019/MC-10168646-0001` | [2018](volkswagen-golf-r-2018.md) |
| 2019 | All models except e-Golf, revised 11/12/2020 | `2020/MC-10183295-0001` | [2019](volkswagen-golf-r-2019.md) |
| 2020 | All models except e-Golf, revised 11/26/2019 | `2019/MC-10168643-0001` | [2020](volkswagen-golf-r-2020.md) |
| 2022 | All model gasoline vehicles, revised 7/21/2021 | `2021/MC-10199495-0001` | [2022](volkswagen-golf-r-2022.md) |
| 2023–2025 | Volkswagen's reissues `USA5R507521`, `USA5R505721`, `USA5R701521`, printed 12/03/2025 | owners' literature | [2023–2025](volkswagen-golf-r-2023-2025.md) |
| 2026 | Volkswagen's reissue `USA5R506821`, printed 02/2026 | owners' literature | [2026](volkswagen-golf-r-2026.md) |

## What isn't covered

- **2017.** NHTSA's index has no 2017 card for the Golf R (its only 2017 cards are the e-Golf's). Volkswagen's 2017
  reissue would be the only source, and the reissues differ from the cards from the time (the
  [2013 Golf R's page](volkswagen-golf-r-2013.md#the-2013-reissue) and the
  [2017 GTI's page](volkswagen-gti-2017.md#how-far-the-reissue-matches-a-card-from-the-time) say how), so 2017 waits on
  [#434's question 3](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244). vPIC's model list
  has "Golf R" for 2017.
- **2021.** vPIC's model list for Volkswagen has no "Golf R" for 2021, so no list is made. (2020 has a
  [list](volkswagen-golf-r-2020.md), by the owner's ruling on [#556](https://github.com/mcosgriff/glovebox/issues/556#issuecomment-6511734459), question 2.)
- The Mk6 Golf R (2012–2013) is [#435's](volkswagen-golf-r-2013.md).
