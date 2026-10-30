# Volkswagen Golf Alltrack, 2020 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-alltrack-2020.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2020 all-model card, revised 11/26/2019, `2019/MC-10168643-0001`, read in full on
2026-10-29, as page images, by eye, as on the [2020 SportWagen 1.4T's page](volkswagen-golf-sportwagen-2020-1.4t.md).

- **The card names no Alltrack**; the owner's ruling ([#555](https://github.com/mcosgriff/glovebox/issues/555#issuecomment-6511795335), part A) gives it the card's rows.
  **The list is the [2020 Golf SportWagen 1.8T](volkswagen-golf-sportwagen-2020-1.8t.md)'s, row for row**, with
  the model's name and no engine.
- **Spark plugs** 40,000 / 48 mo: the Alltrack isn't among the "Except: Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI,
  Tiguan, Atlas, Atlas Cross Sport" of the 40,000-mile row. vPIC decodes `3VWH17AU*LM` (model year 2020, read
  2026-10-29) as "Golf Alltrack", "4Motion S, SE, SEL", 1.8L, so the list names no engine (one engine).
- **The AWD clutch fluid.** By the same decode, "AWD/All-Wheel Drive": "Change AWD clutch fluid (AWD)", every 36
  months by time alone, no service line, in `allWheelDriveOnly`, as on the [2019 Alltrack](volkswagen-golf-alltrack-2019.md).
- Both gearbox rows are left out, as on the Golf's list.
- **Years.** 2020 only: the 2021 card's 40,000-mile row is "Only: Arteon, Passat, Golf R" and its 80,000-mile row
  doesn't name the Alltrack either, and vPIC's 2021 model list has no "Golf Alltrack".
- vPIC's model list for Volkswagen, 2020 (`GetModelsForMakeYear`), names it "Golf Alltrack".
- Reminders: as the [2020 SportWagen 1.8T's](volkswagen-golf-sportwagen-2020-1.8t.md).
