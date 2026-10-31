# Volkswagen Passat 1.8T, 2014–2015 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2014-2015-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.
Every Passat NMS card read, and what isn't covered, is on the [2014 2.5L's page](volkswagen-passat-2014-2.5.md).

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Passat 1.8L TSI card (posted June 2013), in full. It differs from the 2015 card in its gearbox row only (below) | `2013/MC-10125862-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only, as read for the [2014 Golf 2.5L](volkswagen-golf-2014-2.5.md) (not read again here) | reissue `USA5R505521`, printed 11/24/2025 |
| 2015 | The Passat 1.8L TSI card (posted June 2014), in full; the list was written from it, and is its source link | `2014/MC-10120125-9999` |
| 2015 | Volkswagen's 2015 reissue, for the oil change's time limit only, as read for the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) (not read again here) | reissue `USA5R505821`, printed 11/24/2025 |

- **The 2015 card is the [2015 Jetta 1.8T](volkswagen-jetta-2014-2015-1.8t.md)'s** with the model's name: the same rows
  with the same marks in the same columns, footnotes included. So the list is that one's, in its wording.
- **2014 takes the 2015 card's gearbox row** (the owner's ruling of 2026-10-29 on #553,
  [part B](https://github.com/mcosgriff/glovebox/issues/553#issuecomment-6511733565), #444's question 1). The June 2013 card has "Transmission: DSG (non Automatic): Change fluid
  and filter (if applicable)" at 40,000, 80,000 and 120,000 and no automatic's row; the 2015 card has
  "Transmission: Automatic (non DSG): Change fluid" at 50,000 and 100,000 instead. It is the June 2013 Jetta
  1.8T card's row, which Volkswagen's July 2013 Jetta card replaced with the automatic's (see the
  [2014–2015 Jetta 1.8T](volkswagen-jetta-2014-2015-1.8t.md)); the 2014 Passat card is read as misprinted the
  same way. Every other row, mark and footnote of the two cards is the same, so the two years are one list.
  Until 2026-10-29 2014 had a list of its own without the gearbox row.
- A grid of 12 columns, 10,000 to 120,000, no months printed, not repeated (rule 9).
- "Transmission: Automatic (non DSG): Change fluid" at 50,000 and 100,000: the `trans-fluid` service, a
  reminder every 50,000 miles, in `manualExcluded`. The 2015 card has no DSG row.
- `"engine": { "label": "1.8T", "displacementL": 1.8 }`.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo (footnote 6, "every 6 years"); automatic gearbox fluid 50,000 (not for a
  manual); brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 and 2015 reissues (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first column.
