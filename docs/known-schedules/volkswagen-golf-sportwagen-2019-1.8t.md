# Volkswagen Golf SportWagen 1.8T, 2019 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-sportwagen-2019-1.8t.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2019 all-model "Maintenance Schedule" card, the newest revision, revised 11/12/2020,
`2020/MC-10183295-0001`, filed with NHTSA as a manufacturer communication, read in full on 2026-10-29, as page
images, by eye.

- **The list is the [2019 SportWagen 1.4T](volkswagen-golf-sportwagen-2019-1.4t.md)'s, row for row, but for
  the spark plugs**: the 40,000-mile or 4-year row is "Except Atlas, Golf SportWagen 1.4L, Golf 1.4L, GTI,
  Jetta 1.4L, Jetta GLI, Tiguan", which a SportWagen 1.8L isn't, and the 80,000-mile row is "Only" those. So
  spark plugs 40,000 / 48 mo, and `"engine": { "label": "1.8T", "displacementL": 1.8 }`. How the card was
  read is on the [2019 Golf 1.4T's page](volkswagen-golf-2019-1.4t.md); every Mk7 card read, and what isn't
  covered, on the [2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).
- **Left out: the hood latch**, by the owner's ruling on [#555](https://github.com/mcosgriff/glovebox/issues/555#issuecomment-6511795335): "Golf" covers the whole Golf family (SportWagen, Alltrack, Golf R).
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years", an additional item. The
  2019 Golf SportWagen's 4Motion came with the 1.8T, so it is read as on the [2018 SportWagen](volkswagen-golf-sportwagen-2018.md):
  "Change AWD clutch fluid (AWD)", an `additional` reminder by time alone, every 36 months, no service line, in
  `allWheelDriveOnly`, never placed on the grid. vPIC decodes `3VW117AU*KM` (2019 Golf SportWagen, 4Motion S, 1.8L, read
  2026-10-29) as "AWD/All-Wheel Drive".
- Both gearbox rows are left out, as on the Golf's list.
- Reminders: engine oil 10,000 / 12 mo; additional items as on the 1.4T's list, with spark plugs 40,000 / 48 mo, and the AWD clutch fluid.
