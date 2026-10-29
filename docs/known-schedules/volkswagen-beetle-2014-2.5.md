# Volkswagen Beetle 2.5L, 2014 (A5)

How `app/src/main/assets/known-schedules/volkswagen-beetle-2014-2.5.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). This page also lists every card read for the Beetle and the
Beetle Convertible (2012–2019) and says what wasn't covered and why.

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

## The Beetle cards

Volkswagen prints one card per model and engine: a "Beetle" card and a "Beetle Convertible" card for each.

| Model year | Engine | Beetle card | Beetle Convertible card | List |
|---|---|---|---|---|
| 2014 | 2.5L | Posted June 2013, `2013/MC-10125790-9999`; the list was written from it | Posted June 2013, `2013/MC-10125866-9999` | this one |
| 2014 | 1.8T | Posted February 2014, `2014/MC-10122260-9999` | Posted February 2014, `2014/MC-10122259-9999` | [2014 1.8T](volkswagen-beetle-2014-1.8t.md) |
| 2014 | 2.0L TSI | Posted June 2013, `2013/MC-10125789-9999` | Posted June 2013, `2013/MC-10125792-9999` | [2014 2.0T](volkswagen-beetle-2014-2.0t.md) |
| 2014 | 2.0L TDI | Posted June 2013, `2013/MC-10125788-9999` | Posted June 2013, `2013/MC-10125791-9999` | none (below) |
| 2015 | 1.8T | Posted June 2014, `2014/MC-10120108-9999`, and its revision posted November 2017, `2017/MC-10133454-9999` | Posted June 2014, `2014/MC-10120111-9999` | none (below) |
| 2015 | 2.0L TSI | Posted June 2014, `2014/MC-10120110-9999`, and a second card headed 2015, posted August 2015, `2015/MC-10123057-9999` | Posted June 2014, `2014/MC-10120113-9999` | none (below) |
| 2015 | 2.0L TDI | Posted June 2014, `2014/MC-10120109-9999` | Posted June 2014, `2014/MC-10120112-9999` | none (below) |
| 2016 | 1.8T | Posted August 2015, `2015/MC-10123056-9999` | Posted August 2015, `2015/MC-10123081-9999` | none (below) |
| 2016 | 2.0L TSI | None headed 2016 (see 2015) | Posted August 2015, `2015/MC-10123059-9999` | none (below) |
| 2016 | 2.0L TDI | Posted August 2015, `2015/MC-10128137-9999` | Posted August 2015, `2015/MC-10123058-9999` | none (below) |
| 2018 | all models | The newest revision of the all-model card, `2019/MC-10168646-0001` | the same card | [2018](volkswagen-beetle-2018.md) |
| 2019 | all models | The newest revision of the all-model card, `2020/MC-10183295-0001` | the same card | [2019](volkswagen-beetle-2019.md) |
| 2014 | all models | Volkswagen's 2014 reissue (`USA5R505521`, printed 11/24/2025), for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here). Its model codes on Volkswagen's owners-literature index include the Beetle (`5C1`) and the Beetle Cabrio (`5C7`) | the same | every 2014 list |

NHTSA's index files the 2014 and 2015 cards again in 2023 (`MC-10242446`, `MC-10242447`, `MC-10242453` to
`MC-10242458`, `MC-10242410` to `MC-10242413`, `MC-10242483`, `MC-10242484`, `MC-10243072` to `MC-10243075`);
those weren't fetched.

A script's reading of every grid (two readings, which had to agree) was compared row by row between the cards,
as a pointer only; each difference was looked at on the page.

**One list for both bodies.** NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`, read 2026-10-29)
names the car "Beetle" in every year from 2013 to 2019 and has no "Beetle Convertible", so a convertible is
matched as a "Beetle". Glovebox can't tell the two bodies apart (`body` is only sedan or hatchback). So a list
is made only where the Beetle card and the Beetle Convertible card of a year and engine agree. On the 2014
2.5L, 1.8T and 2.0L TSI cards they do, but for two rows that only the convertible's card has:

- "Convertible roof, CSC roof, and Retractable hard top: Check for function and damage, clean and lubricate
  seals (if applicable)", at 40,000, 80,000 and 120,000, with footnote 2, "Every 2 years, regardless of
  mileage: Check latch for convertible roof": "Check convertible roof and seals; latch every 2 years (Beetle
  Convertible)";
- "Convertible roof water drains: Check", at 40,000, 80,000 and 120,000: "Check convertible roof water drains
  (Beetle Convertible)".

Both are kept, named for the Beetle Convertible, and are table-only (issue #448): no catalog line names them,
and the format has no field for a convertible. A Beetle coupe owner unticks them. Footnote numbers shift by one
on the convertible's cards; the footnotes say the same.

**One list per engine** (the owner's ruling of 2026-10-23): each card is for one engine, named in its heading.
The 2.0L TSI and the 2.0L TDI are the same size, so the 2.0T list has `"fuel": "Gasoline"`, as the
[2010 Jetta 2.0T](volkswagen-jetta-2010-2.0t.md) does. A Beetle with no engine on record is asked which it has.

## This list

- **The card is the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md)'s grid**: the same rows with the same marks
  in the same columns, gearbox row included (a script's comparison of the two lists' tables, then the pages),
  with the convertible's two roof rows. The wording follows the [2014–2015 CC 2.0T](volkswagen-cc-2014-2015-2.0t.md),
  whose card prints the same text where the Golf's differs (the Beetle card's washer row names the headlight
  washers, and its tire row has no "age").
- "Transmission: Automatic (non DSG): Change fluid" at 50,000 and 100,000, and no DSG row: the `trans-fluid`
  service, a reminder every 50,000 miles, in `manualExcluded`, as on the Golf's list.
- `"engine": { "label": "2.5L", "displacementL": 2.5 }`.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs 40,000 / 72 mo (footnote, "every 6 years"); automatic gearbox fluid 50,000 (not for a manual); air
  filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, the empty "Timing Belt: Replace" row, and the grey "Carefree
  Maintenance" shading of the first columns.

## What isn't covered

Asked on [#448](https://github.com/mcosgriff/glovebox/issues/448); nothing below is in a list until the owner decides.

- **The 2.0L TDI, 2014–2016.** The Beetle's and the Beetle Convertible's cards differ in the 130,000 column,
  the grid's last. On the Beetle's cards it is a whole service (as on the [2014–2015 Passat TDI](volkswagen-passat-2014-2015-tdi.md)):
  oil, rotation, brake pads, brake fluid and the rest, with the timing belt. On the convertible's 2014 card it
  holds only the timing belt ([#434's question 2](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244));
  on its 2015 card it lacks the yearly airbag check and the brake fluid; on its 2016 card, the brake fluid. One
  "Beetle" list can't be both.
- **The 1.8T, 2015 and 2016.** The Beetle's June 2014 and August 2015 cards draw "Sunroof: Open sunroof and
  check front water drains" at 20,000 as well as at 40,000, 80,000 and 120,000; the convertible's cards don't.
  Volkswagen's November 2017 revision of the 2015 Beetle 1.8T card (`2017/MC-10133454-9999`, "Removed
  maintenance check at 20K for 'Sunroof: Open sunroof and check water drains…'") takes the 20,000 mark out,
  and is then row for row the 2014 1.8T card. No revision of the 2016 card was found.
- **The 2.0L TSI, 2015 and 2016.** Two Beetle cards are headed "2015 … 2.0L TSI": the June 2014 one, row for row
  the 2014 card, and one posted August 2015 in the 2016 layout (no yearly airbag check, rotation, door checks,
  rear nozzle or sticker; the dust and pollen filter every 40,000 or 2 years), which is row for row the 2016
  Beetle Convertible 2.0T card without the roof rows. No Beetle card is headed 2016 for the 2.0L TSI. The same
  case as the Passat 3.6L ([#444's question 2](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)).
- **2012, 2013 and 2017.** NHTSA's index has no Beetle card for those years. Volkswagen's 2025 reissues would be
  the only source; they weren't read for the Beetle, and for the Golf they differ from the cards from the time
  ([#434's question 3](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244)).
- **The 2010 New Beetle** is on the 2010 card for all models except the Routan; it is the older generation and
  outside #448.
