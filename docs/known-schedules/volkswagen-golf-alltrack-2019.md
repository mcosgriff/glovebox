# Volkswagen Golf Alltrack, 2019 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-alltrack-2019.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2019 all-model "Maintenance Schedule" card, the newest revision, revised 11/12/2020,
`2020/MC-10183295-0001`, filed with NHTSA as a manufacturer communication (its index files it under the Golf
Alltrack too), read in full on 2026-10-29, as page images, by eye.

- **The list is the [2019 Golf SportWagen 1.8T](volkswagen-golf-sportwagen-2019-1.8t.md)'s, row for row**,
  with the model's name. How the card was read is on the [2019 Golf 1.4T's page](volkswagen-golf-2019-1.4t.md);
  every Mk7 card read, and what isn't covered, on the [2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).
- **Spark plugs** 40,000 / 48 mo: the 40,000-mile or 4-year row is "Except" models among which the Alltrack
  isn't, and the 80,000-mile row is "Only" those. No row names an Alltrack engine, so the list names none.
- **Left out: the hood latch** ("Does not apply to: Passat, Golf, Beetle, and Jetta"), by the owner's ruling on [#555](https://github.com/mcosgriff/glovebox/issues/555#issuecomment-6511795335): "Golf" covers the whole Golf family (SportWagen, Alltrack, Golf R).
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years", an additional item. The
  2019 Golf Alltrack was sold with 4Motion all-wheel drive, so it is read as on the [2018 Alltrack](volkswagen-golf-alltrack-2018.md):
  "Change AWD clutch fluid (AWD)", an `additional` reminder by time alone, every 36 months, no service line, in
  `allWheelDriveOnly`, never placed on the grid. vPIC decodes `3VWH17AU*KM` (2019 Golf Alltrack, 4Motion S, SE, SEL, 1.8L, read
  2026-10-29) as "AWD/All-Wheel Drive".
- Both gearbox rows are left out, as on the Golf's list.
- vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`), names it "Golf Alltrack".
- Reminders: engine oil 10,000 / 12 mo; additional items as on the SportWagen 1.8T's list.
