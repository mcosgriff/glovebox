# Volkswagen Tiguan, 2013–2015 (1st generation)

How `app/src/main/assets/known-schedules/volkswagen-tiguan-2013-2015.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). This page also lists every card read for the first-generation
Tiguan and the Tiguan Limited (2009–2018) and says what wasn't covered and why.

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

## The Tiguan cards

| Model year | What was read | Document | List |
|---|---|---|---|
| 2010 | The card for all models except the Routan (revised October 2012), in full, read again as page images | `2012/MC-10156104-9999` | [2010](volkswagen-tiguan-2010.md) |
| 2010 | Volkswagen's 2010 reissue (printed 11/24/2025), its spark plug and gearbox rows on page 5, and the oil change's time limit as read for the [2010 Passat](volkswagen-passat-2010.md) | reissue `USA5R503621` | [2010](volkswagen-tiguan-2010.md) |
| 2013 | The Tiguan 2.0TFSI card (revised October 2012), in full; **the list was written from it**. NHTSA files it again as `MC-10157692` (not fetched) | `2012/MC-10156107-9999` | this one |
| 2014 | The Tiguan 2.0L TSI card (revised June 2013), in full. NHTSA files it twice; the two files are byte for byte the same | `2013/MC-10125858-9999`, `2013/MC-10242476-9999` | this one |
| 2015 | The Tiguan 2.0L TSI card (revised June 2014), in full | `2014/MC-10120128-9999` | this one |
| 2016 | The Tiguan 2.0L TSI card (revised August 2015), in full | `2015/MC-10123063-9999` | [2016](volkswagen-tiguan-2016.md) |
| 2013–2016 | Volkswagen's reissues for 2013 (`USA5R505321`), 2014 (`USA5R505521`), 2015 (`USA5R505821`) and 2016 (`USA5R506521`), printed November 2025, for the oil change's time limit only, as read for the [2013 Golf R](volkswagen-golf-r-2013.md), [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md), [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) and [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | owners literature | 2013–2015, 2016 |
| 2018 (Tiguan Limited) | The 2018 all-model card, the newest revision (revised 11/26/2019), in full | `2019/MC-10168646-0001` | [2018 Tiguan Limited](volkswagen-tiguan-limited-2018.md) |

NHTSA's index also files `2014/MC-10240827-9999` ("Maintenance Schedule For USA", June 2014) under the 2015
Tiguan. It was fetched and is headed "2015 Volkswagen Passat … 3.6L Engine": a Passat card filed under the
wrong model, so it isn't read for the Tiguan.

A script's reading of every grid was compared row by row between the cards (as a pointer only; each
difference was looked at on the page). **The 2013, 2014 and 2015 cards print the same rows with the same marks
in the same columns** and the same footnotes; only the grey "Carefree Maintenance" shading differs (10,000 to
30,000 on the 2013 card, 10,000 and 20,000 on the 2014, 10,000 on the 2015), and it is left out, as is the
rewording of the brake fluid footnote ("Every 3 years, regardless of mileage and every 2 years after" in 2013,
"At the 3 year mark, then every 2 years" from 2014), which says the same. So 2013–2015 is one list. The 2016
card is the 2016 layout of the other models' cards and has a list of its own.

**One list for every engine.** Every card is headed with the 2.0T (2.0TFSI, 2.0L TSI), the first-generation
Tiguan's only engine in the USA, so the lists name no `engine`. **The model.** NHTSA's vPIC model list for
Volkswagen (`GetModelsForMakeYear`, read 2026-10-28) names it "Tiguan" in every year from 2010 to 2017, so the
lists are for "Tiguan". For 2018 the list has both "Tiguan" (the second generation, issue #450) and "Tiguan
Limited" (this one, carried on; see its page).

## This list

- **The cards are the [2014–2015 CC 3.6L](volkswagen-cc-2014-2015-3.6.md)'s grid** row for row: the same rows
  with the same marks in the same columns (a script's comparison of the two lists' tables, then the pages). So
  the list is that one's, in its wording, but for the Haldex row (below).
- **"Haldex Clutch: Change oil (if applicable)"**, drawn at 30,000, 60,000, 90,000 and 120,000 with footnote 5,
  "Every 3 years, regardless of mileage: Haldex clutch: Change oil (if applicable)". **Table-only**, named
  "Change Haldex clutch oil (if applicable)", with no `service` and no reminder, by the "(if applicable)" rule,
  as on the [2010 Passat](volkswagen-passat-2010.md) and the [2013 Golf R](volkswagen-golf-r-2013.md). The
  Tiguan was sold with front-wheel drive and with 4Motion, so the row is a 4Motion Tiguan's job; the CC's
  3.6L lists read the same row as an all-wheel-drive reminder instead, and which reading should hold is the
  open question on the CC ([#446](https://github.com/mcosgriff/glovebox/issues/446#issuecomment-6490065640)).
  If the owner takes the CC's reading, this row becomes "Change Haldex clutch oil, every 3 years (AWD)" in
  `allWheelDriveOnly`, every 36 months by time alone.
- **No gearbox row**: the cards print none, so the list has no transmission fluid job.
- Spark plugs and the air filter at 60,000 and 120,000 with footnotes 7 and 8, "every 6 years": every 60,000
  miles or 72 months. The "Air filter element and snow filter" row is named "Replace air filter element and
  clean housing", as on the GTI list.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years
  first, then every 2).
- **Oil 12 months**, from Volkswagen's 2013, 2014 and 2015 reissues (the owner's ruling of 2026-10-28, #517).
  The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.

## What isn't covered

- **2009.** NHTSA's index has no Volkswagen card before 2010.
- **2011, 2012 and 2017.** NHTSA's index has no Tiguan card for those years. Volkswagen's 2025 reissues would be
  the only source; they weren't read for the Tiguan, and for the Golf they differ from the cards from the time
  ([#434's question 3](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244)). The 2010
  and 2013 cards aren't stretched over them.
- **The 2018 Tiguan** (second generation, "Tiguan" in vPIC's 2018 list) is issue #450's, not this one's.
- The open questions for these lists are on [#449](https://github.com/mcosgriff/glovebox/issues/449).
