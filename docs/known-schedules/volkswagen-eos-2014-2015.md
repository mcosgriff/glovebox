# Volkswagen Eos, 2014–2015

How `app/src/main/assets/known-schedules/volkswagen-eos-2014-2015.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). This page also lists every card read for the Eos (2010–2016)
and says what wasn't covered and why.

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

## The Eos cards

| Model year | What was read | Document | List |
|---|---|---|---|
| 2010 | The card for all models except the Routan (revised October 2012), as read for the [2010 Passat](volkswagen-passat-2010-2.0t.md), and Volkswagen's 2010 reissue (`USA5R503621`, printed 11/24/2025), whose model codes include the Eos (`1F7`) | `2012/MC-10156104-9999` | [2010](volkswagen-eos-2010.md) |
| 2014 | The Eos 2.0L TSI card, posted June 2013, in full; the list was written from it. NHTSA's index also lists `MC-10242461` for the 2014 Eos ("2.0L TSI Engine", dated 2013-06-01, filed in 2023; not fetched) | `2013/MC-10125795-9999` | this one |
| 2015 | The Eos 2.0L TSI card, posted June 2014, in full; NHTSA's index also lists `MC-10240817` for the 2015 Eos ("Maintenance Schedule For USA", dated 2014-06-01, filed in 2023; not fetched) | `2014/MC-10120117-9999` | this one |
| 2016 | The Eos 2.0L TSI card, posted August 2015, in full | `2015/MC-10123076-9999` | [2016](volkswagen-eos-2016.md) |
| 2014–2016 | Volkswagen's reissues for 2014 (`USA5R505521`), 2015 (`USA5R505821`) and 2016 (`USA5R506521`), printed November 2025, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md), [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) and [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here). Each one's model codes on Volkswagen's owners-literature index include the Eos (`1F8`) | owners literature | 2014–2015, 2016 |

A script's reading of every grid was compared row by row between the cards (as a pointer only; each
difference was looked at on the page). The 2014 and 2015 cards print the same rows with the same marks in the
same columns and the same footnotes; only the grey "Carefree Maintenance" shading differs (20,000 is shaded on
the 2014 card, not on the 2015 one), and it is left out. So 2014 and 2015 have one list. The 2016 card is the
later layout (see its page).

**The model.** NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`) names the car "Eos" in every
year from 2010 to 2016 (read 2026-10-28), so every list is for "Eos". Every Eos card from 2014 to 2016 is for
the 2.0L TSI, the only engine they name, so the lists have no `engine`.

**The gearbox row.** Each card prints one gearbox row, "Transmission: DSG (non Automatic): Change fluid and
filter (if applicable)", and no automatic's row. It is read as the [2014–2015 CC 2.0T](volkswagen-cc-2014-2015-2.0t.md)
and the [2010–2014 GTI](volkswagen-gti-2010-2014.md) read the same row: `trans-fluid`, a reminder every 40,000
miles, named "(DSG only)", in `manualExcluded`. Volkswagen's 2010 reissue names the Eos's automatic only as
`02E (DSG)`.

**The convertible roof** (issue #447): "Convertible roof, CSC roof, and Retractable hard top: Check for function
and damage, clean and lubricate seals (if applicable)", at 40,000, 80,000 and 120,000. The Eos's roof is its
own (a retractable hard top), so the row is kept, named "Check convertible roof for function and damage; clean
and lubricate seals", and it is table-only: no catalog line names it, and it is an "(if applicable)" row.

## This list

- **The card is the [2014–2015 CC 2.0T](volkswagen-cc-2014-2015-2.0t.md)'s grid with the convertible roof
  row added**: every other row has the same marks in the same columns and the same footnotes (a script's
  comparison of the two cards, then the pages). So the list is that one's, in its wording, with the roof row.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; DSG fluid
  and filter 40,000 (not for a manual); spark plugs and air filter 60,000 / 72 mo (footnotes 6 and 7, "every 6
  years"); brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 and 2015 reissues (the owner's ruling of 2026-10-28, #517). The
  table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.

## What isn't covered

- **2011, 2012 and 2013.** NHTSA's index has no Eos card for those years; they have
  [a list from Volkswagen's reissues alone](volkswagen-eos-2011-2013.md) ([#552](https://github.com/mcosgriff/glovebox/issues/552#issuecomment-6511733274)).
- **2007–2009** (outside #447): NHTSA's index has no Volkswagen card before 2010.
