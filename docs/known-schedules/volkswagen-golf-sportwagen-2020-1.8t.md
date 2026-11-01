# Volkswagen Golf SportWagen 1.8T, 2020 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-sportwagen-2020-1.8t.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2020 all-model card, revised 11/26/2019, `2019/MC-10168643-0001`, read in full on
2026-10-29, as page images, by eye, as on the [2020 SportWagen 1.4T's page](volkswagen-golf-sportwagen-2020-1.4t.md).

- **The list is the [2020 SportWagen 1.4T](volkswagen-golf-sportwagen-2020-1.4t.md)'s, row for row, but for the
  spark plugs**: by the owner's ruling ([#555](https://github.com/mcosgriff/glovebox/issues/555#issuecomment-6511795335), part A) any SportWagen engine but the 1.4L
  is on the 40,000-mile or 4-year row ("Except: Golf 1.4L, …"). So spark plugs 40,000 / 48 mo.
- **The engine.** vPIC decodes `3VW117AU*LM` and `3VW217AU*LM` (model year 2020, read 2026-10-29) as "Golf
  SportWagen", "4Motion S", 1.8L, "AWD/All-Wheel Drive" (an automatic and a "Manual/Standard"). So the list has
  `"engine": { "label": "1.8T", "displacementL": 1.8 }`. A SportWagen with no engine on record is asked which it
  has.
- **The AWD clutch fluid**, as on the 1.4T's list: "Change AWD clutch fluid (AWD)", every 36 months by time alone,
  in `allWheelDriveOnly`, by the decodes above.
- Reminders: as the 1.4T's, with spark plugs 40,000 / 48 mo.
