# Volkswagen Jetta 1.8T, 2016 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2016-1.8t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2016 | The Jetta Sedan 1.8L TSI card (posted August 2015), in full; the list was written from it | `2015/MC-10123068-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

NHTSA's index files four more 2016 Jetta cards, all posted August 2015 and all read: the 1.4T
(`MC-10123069`, [its own list](volkswagen-jetta-2016-1.4t.md)), the 2.0L MPI (`MC-10123067`), the 2.0L TDI
(`MC-10123066`) and, under "Jetta GLI", the GLI 2.0L TSI (`MC-10123071`). For why the last three have no list,
see the [2014 1.8T's page](volkswagen-jetta-2014-2015-1.8t.md#what-isnt-covered).

- **What differs from 2015.** The 2016 card is the [2015 1.8T](volkswagen-jetta-2014-2015-1.8t.md)'s grid without
  five rows: tire rotation, the yearly airbag check, the door check lubrication, the rear window washer nozzle
  check and the service sticker. Its dust and pollen filter is every 40,000 miles or 2 years, drawn at 40,000,
  80,000 and 120,000 (2015: every 20,000, and every 2 years), and "Transmission: Automatic (non DSG): Change
  fluid" is drawn at 80,000 only (2015: 50,000 and 100,000). Every other row is in the same columns, so the
  jobs keep the 2015 list's wording. This is the change the [2016 GTI](volkswagen-gti-2016.md)'s card made too.
- **No tire rotation.** The card has no rotation row and nothing says when to rotate; the list has none.
- Spark plugs at 60,000 and 120,000, footnote 5, "every 6 years regardless of mileage": 60,000 / 72 mo.
- The automatic's fluid: a reminder every 80,000 miles (the grid's only multiple of it), in `manualExcluded`.
- **The engine.** `"engine": { "label": "1.8T", "displacementL": 1.8 }`; no other 2016 Jetta list is 1.8L.
  NHTSA's vPIC model list for Volkswagen, 2016 (`GetModelsForMakeYear`), has "Jetta" and no "Jetta GLI".
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; spark plugs and air filter
  60,000 / 72 mo; automatic gearbox fluid 80,000 (not for a manual); brake fluid every 24 months by time alone
  (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue, as on the Jetta Hybrid's page (the owner's ruling of
  2026-10-28, #517). The table has none.
- No special operating conditions are given on the card.
