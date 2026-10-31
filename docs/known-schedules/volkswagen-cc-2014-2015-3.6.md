# Volkswagen CC 3.6L, 2014–2015

How `app/src/main/assets/known-schedules/volkswagen-cc-2014-2015-3.6.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every CC card read, and what isn't covered, is on the
[2014–2015 2.0T's page](volkswagen-cc-2014-2015-2.0t.md).

| Model year | What was read | Document |
|---|---|---|
| 2014 | The CC 3.6L FSI card (posted June 2013), in full; the list was written from it | `2013/MC-10125794-9999` |
| 2015 | The CC 3.6L FSI card (posted June 2014), in full: the same rows, marks and footnotes | `2014/MC-10120115-9999` |
| 2014–2015 | Volkswagen's 2014 and 2015 reissues, for the oil change's time limit only (see the 2.0T's page) | `USA5R505521`, `USA5R505821` |

- **The card is the [2014–2015 CC 2.0T](volkswagen-cc-2014-2015-2.0t.md)'s grid without the DSG row and with a
  Haldex clutch row**; every other row and mark is the same (the footnotes are numbered one on from 5). So the
  list uses that list's wording.
- **"Haldex Clutch: Change oil (if applicable)"**, drawn at 30,000, 60,000, 90,000 and 120,000 with footnote 5,
  "Every 3 years, regardless of mileage: Haldex clutch: Change oil (if applicable)". The Haldex clutch is the
  4Motion all-wheel drive's coupling, and the row is only on the 3.6L card, not the front-wheel-drive 2.0T's;
  so it is read as a job for all-wheel drive (issue #446) and not as a table-only "(if applicable)" row: named
  "Change Haldex clutch oil, every 3 years (AWD)", in `allWheelDriveOnly`, and a reminder every 36 months by
  time alone (no catalog line names the job, so it has no `service`). A CC known to drive two wheels loses it.
  The 2010 all-model card's row (a 2010 CC gets the [2010 Passat](volkswagen-passat-2010-2.0t.md)'s list) is read the
  same way, by the owner's ruling on [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part A, confirmed for every list in [the second round](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511795038)).
- **No gearbox row**: the card prints none, so the list has no transmission fluid job.
- Spark plugs at 60,000 and 120,000 with footnote 7, "every 6 years": every 60,000 miles or 72 months.
- `"engine": { "label": "3.6L", "displacementL": 3.6 }`.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo; Haldex clutch oil every 36 months by time alone (AWD); brake fluid every
  24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's reissues (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.
