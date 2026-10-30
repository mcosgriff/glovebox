# Volkswagen Beetle 1.8T, 2016 (A5)

How `app/src/main/assets/known-schedules/volkswagen-beetle-2016-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Beetle card read, how the Beetle and Beetle Convertible
cards are put into one list, and what isn't covered are on the [2014 2.5L's page](volkswagen-beetle-2014-2.5.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2016 | The Beetle 1.8T card (posted August 2015), in full; the list was written from it | `2015/MC-10123056-9999` |
| 2016 | The Beetle Convertible 1.8T card (posted August 2015), in full: the same rows, marks and footnotes, with the two convertible roof rows, and no 20,000 sunroof drain mark | `2015/MC-10123081-9999` |
| 2016 | The Passat 1.8L TSI card (posted August 2015), for comparison | `2015/MC-10123065-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The card is the [2016 Passat 1.8T](volkswagen-passat-2016-1.8t.md)'s grid** (both read side by side): the
  same rows with the same marks in the same columns, footnotes included, the automatic gearbox's row at 80,000
  included, but for the sunroof drain mark below. So the list is that one's, in its wording, with the
  convertible's two roof rows.
- **The 20,000 sunroof drain mark is read as the same error as on the 2015 card** (the owner's ruling of
  2026-10-29 on #553, [part A](https://github.com/mcosgriff/glovebox/issues/553#issuecomment-6511794772)). The Beetle card draws "Sunroof: Open sunroof and check front water drains
  and clean if necessary (if applicable)" at 20,000, 40,000, 80,000 and 120,000; the convertible's card draws it
  at 40,000, 80,000 and 120,000, as the Passat's does. Volkswagen took the same 20,000 mark off the 2015 Beetle
  1.8T card in November 2017 ([2014–2015 1.8T](volkswagen-beetle-2014-2015-1.8t.md)); no revision of the 2016
  card was found. Without it the Beetle and convertible cards agree, and the row is table-only either way.
- The convertible roof rows, "Convertible roof, CSC roof, and Retractable hard top: Check for function and
  damage, clean and lubricate seals (if applicable)" (footnote 1, the latch every 2 years) and "Convertible roof
  water drains: Check", at 40,000, 80,000 and 120,000, are table-only and named for the Beetle Convertible, as on
  the 2014 lists (see the 2.5L's page).
- "Transmission: Automatic (non DSG): Change fluid" at 80,000 only, and no DSG row: a reminder every 80,000
  miles, in `manualExcluded`, as on the 2016 Passat 1.8T.
- `"engine": { "label": "1.8T", "displacementL": 1.8 }`.
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; spark plugs and air filter
  60,000 / 72 mo; automatic gearbox fluid 80,000 (not for a manual); brake fluid every 24 months by time alone
  (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- As on the 2016 cards, there is no yearly airbag check, tire rotation, door check lubrication, rear washer
  nozzle check or service sticker, and no special operating conditions.
