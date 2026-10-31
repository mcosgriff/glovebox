# Volkswagen Jetta Hybrid, 2013

How `app/src/main/assets/known-schedules/volkswagen-jetta-2013-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids
are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`), and Volkswagen's reissued all-model
cards from its owners-literature site. Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2013 | The Jetta Hybrid 1.4L card posted January 2013, the newest printing, in full; the list follows it | `2013/MC-10242444-9999` |
| 2013 | The same card posted November 2012 (filed twice), in full. It differs from the January 2013 printing in two rows (below) | `2012/MC-10156148-9999`, `2013/MC-10242481-9999` |
| 2013 | Volkswagen's 2013 reissue, for the oil change's time limit only (see the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md)'s page) | reissue `USA5R505321`, printed 11/24/2025 |

- **The list is the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md)'s.** The January 2013 card and the
  2015 card (`2014/MC-10120122-9999`) print the same rows with the same marks in the same columns, so the jobs,
  wording, engine (`1.4L hybrid`, `"powertrain": "hybrid"`) and reminders are as on that page.
- **The newest printing wins** (the owner's ruling of 2026-10-29 on #553,
  [part A](https://github.com/mcosgriff/glovebox/issues/553#issuecomment-6511794772), which takes #143's
  "newest revision wins" to the 2013–2016 per-model cards). The November 2012 printing draws "Brake fluid and
  clutch unit: Change fluid" at 30,000, 50,000, 70,000, 90,000 and 110,000 only, and "Tire mobility kit: Check
  and observe expiration date on the tire filler bottle" at 20,000 to 120,000 only; the January 2013 printing
  draws them to 150,000 and 140,000, as the 2015 card does. Neither changes a reminder: brake fluid is by time
  alone, and the tire-kit check is table-only.
- **A list of its own, not one 2013–2015 list.** The ruling joins 2013 to the 2015 list, but a list covers one
  run of years, and 2014 between them has no card from the time: only Volkswagen's 2014 reissue
  (`USA5R505521`), which gives the dust and pollen filter every 40,000 or 2 years where these cards give 20,000
  (#552, [#441's question 2](https://github.com/mcosgriff/glovebox/issues/441#issuecomment-6488238918)). So
  2013 is this list, with the same content as the 2015 one; if 2014 is ever read as these cards, the three years
  become one 2013–2015 list.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs 40,000 / 48 mo; air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says
  at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2013 reissue, which gives the first service at 10,000 miles or 1 year,
  as read for the 2015 list (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first column.
