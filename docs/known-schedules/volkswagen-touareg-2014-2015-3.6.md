# Volkswagen Touareg 3.6L, 2014–2015 (2nd generation)

How `app/src/main/assets/known-schedules/volkswagen-touareg-2014-2015-3.6.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md). This page also lists every card read for the Touareg
(2010–2017) and says what wasn't covered and why.

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

## Every Touareg card read

| Model year | What was read | Document | List |
|---|---|---|---|
| 2010 | The card for all models except the Routan (revised October 2012), as the [2010 Passat](volkswagen-passat-2010-2.0t.md#how-the-2010-all-model-card-is-read) reads it | `2012/MC-10156104-9999` | [2010 3.6L](volkswagen-touareg-2010-3.6.md) |
| 2010 | The "Touareg 3.0L TDI Engine Only" card (revised October 2012), in full. NHTSA files it again as `MC-10157691` (not fetched) | `2012/MC-10156105-9999` | [2010 TDI](volkswagen-touareg-2010-tdi.md) |
| 2010 | Volkswagen's 2010 reissue (printed 11/24/2025), as read for the [2010 Passat](volkswagen-passat-2010-2.0t.md), for the oil change's time limit; its rows name the first-generation Touareg as `7L` | reissue `USA5R503621` | 2010 |
| 2014 | The Touareg 3.6L FSI Gasoline card (posted June 2013), in full; **this list was written from it**. NHTSA files it twice (`MC-10242479`, under 2014); the two files are byte for byte the same. `MC-10125856` was not fetched | `2013/MC-10125855-9999`, `2014/MC-10242479-9999` | this one |
| 2014 | The Touareg 3.0L TDI card (posted June 2013), in full. NHTSA's index heads its row "3.0L Hybrid Engine"; the file is the TDI's | `2013/MC-10242478-9999` | [2014–2015 TDI](volkswagen-touareg-2014-2015-tdi.md) |
| 2014 | The Touareg 3.0L Hybrid card (posted June 2013), in full. NHTSA files it twice; the two files are byte for byte the same | `2013/MC-10125857-9999`, `2013/MC-10242477-9999` | [2014 Hybrid](volkswagen-touareg-2014-hybrid.md) |
| 2015 | The Touareg 3.6L FSI Gasoline card (June 2014), in full. Filed twice, byte for byte the same | `2014/MC-10240831-9999`, `2014/MC-10120131-9999` | this one |
| 2015 | The Touareg 3.0L TDI card (June 2014), in full. Filed twice, byte for byte the same | `2014/MC-10240830-9999`, `2014/MC-10120130-9999` | [2014–2015 TDI](volkswagen-touareg-2014-2015-tdi.md) |
| 2015 | The Touareg 3.0L Hybrid card (June 2014), in full. Filed twice, byte for byte the same | `2014/MC-10240829-9999`, `2014/MC-10120129-9999` | none (below) |
| 2015 | A second card headed "2015 … Touareg … 3.0L Hybrid Engine", posted August 2015, in the 2016 cards' layout | `2015/MC-10123062-9999` | none (below) |
| 2016 | The Touareg 3.6L FSI Gasoline card (posted August 2015), in full | `2015/MC-10123060-9999` | [2016 3.6L](volkswagen-touareg-2016-3.6.md) |
| 2016 | The Touareg 3.0L TDI card (posted August 2015), in full | `2015/MC-10123061-9999` | [2016 TDI](volkswagen-touareg-2016-tdi.md) |
| 2014–2016 | Volkswagen's reissues for 2014 (`USA5R505521`), 2015 (`USA5R505821`) and 2016 (`USA5R506521`), printed November 2025, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md), [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) and [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | owners literature | 2014–2016 |

NHTSA's index also files `2014/MC-10240828-9999` ("Maintenance Schedule For USA", June 2014) under the 2015
Touareg. It was fetched and is headed "2015 Volkswagen Tiguan … 2.0L TSI Engine": a Tiguan card filed under
the wrong model, so it isn't read for the Touareg.

A script's reading of every grid was compared row by row between the cards (as a pointer only; each difference
was looked at on the page). **For each engine the 2014 and 2015 cards print the same rows with the same marks in
the same columns** and the same footnotes; only the grey "Carefree Maintenance" shading differs, and it is left
out. The 2016 cards are the 2016 layout of the other models' cards and have lists of their own.

**One list per engine.** Each card is headed with its engine. The 3.6L list has
`"engine": { "label": "3.6L", "displacementL": 3.6 }`. The 3.0L TDI and the 3.0L Hybrid share a size, so the TDI
lists add `"fuel": "Diesel"` and the Hybrid list `"powertrain": "hybrid"`: a 3.0L that isn't on record as a
hybrid never gets the Hybrid list, and one whose fuel isn't on record as diesel never gets a TDI list (setup
asks which engine it has). **The model.** NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`, read
2026-10-28) names it "Touareg" in 2010 and in every year from 2013 to 2018, with no separate hybrid model, so
every list is for "Touareg".

**All-wheel drive.** The Touareg was sold in the USA with 4Motion only. None of the 2014–2016 cards prints an
all-wheel-drive row (no Haldex, transfer case or differential job), so these lists have no `allWheelDriveOnly`.
The 2010 cards' "Haldex Clutch: Change oil (if applicable)" row is an all-wheel-drive reminder (#554; see the 2010 pages).

## This list

- **The cards are the [2014–2015 CC 3.6L](volkswagen-cc-2014-2015-3.6.md)'s grid** with two differences:
  there is no Haldex row, and "Sunroof: Open sunroof and check front water drains" is drawn at every 20,000
  (the CC's at 40,000, 80,000 and 120,000). So the list is that one's, in its wording, with those changes.
- **No gearbox row**: the cards print none, so the list has no transmission fluid job.
- Spark plugs and the air filter at 60,000 and 120,000 with footnotes 6 and 7, "every 6 years": every 60,000
  miles or 72 months. The "Air filter element and snow filter: Replace element and clean screen and housing"
  row is named "Replace air filter element and clean housing", as on the CC list.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years
  first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 and 2015 reissues (the owner's ruling of 2026-10-28, #517). The
  table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.

## What isn't covered

- **2011, 2012, 2013 and 2017.** NHTSA's index has no Touareg card for those years. Volkswagen's 2025 reissues
  would be the only source; they weren't read for the Touareg, and for the Golf they differ from the cards from
  the time ([#434's question 3](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244)). The
  2010 and 2014 cards aren't stretched over them.
- **The 2015 and 2016 Hybrid.** Two different cards are headed "2015 … 3.0L Hybrid": the June 2014 card
  (`MC-10240829`, row for row the 2014 Hybrid card) and the August 2015 card (`MC-10123062`, in the 2016
  layout: no yearly airbag check, tire rotation, door checks, rear nozzle or sticker, and the dust and pollen
  filter every 40,000 or 2 years). No card is headed "2016 … Hybrid". This is the Passat 3.6L's case,
  [#444's question 2](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514), and is left
  out the same way until the owner decides.
- **A 2010 Touareg with the 4.2L V8.** The all-model card's spark plug row names "2.0T, 3.6L and 4.2L"; whether
  a 2010 Touareg was sold with the 4.2L wasn't checked, so the 2010 petrol list is for the 3.6L only.
- **2018 on.** Outside #451. vPIC's 2018 model list still names "Touareg", but no Touareg row is in the 2018
  all-model card's index entry, and none was looked for.
- The open questions for these lists are on [#451](https://github.com/mcosgriff/glovebox/issues/451#issuecomment-6492650721): the 2010 TDI dust filter months (question 1) and AdBlue checks past 120,000 (question 2), the supercharger belt (question 3), and the 2010 4.2L (question 4).
