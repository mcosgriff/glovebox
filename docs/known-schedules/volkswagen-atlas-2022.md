# Volkswagen Atlas, 2022

How `app/src/main/assets/known-schedules/volkswagen-atlas-2022.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Atlas card read, and what isn't covered, is on the
[2018 2.0T's page](volkswagen-atlas-2018-2.0t.md#every-atlas-card-read).

Source: Volkswagen's "2022 VW Maintenance Schedule - All Model Gasoline Vehicles", USA, filed with NHTSA as a
manufacturer communication: the newest revision, revised 7/21/2021, `2021/MC-10199495-0001`, read in full on
2026-10-29, as page images, by eye. Volkswagen's reissue of the card isn't used: the card from the time wins.

- **The list is the [2022 Tiguan](volkswagen-tiguan-2022.md)'s, row for row**, with the model's name: the card is
  read as the [2022 GTI's list](volkswagen-gti-2022.md) reads it, and its only row that names the Tiguan names the
  Atlas too: **spark plugs every 80,000 miles or 6 years** (80,000 / 72 months), "Only: Atlas, Atlas Cross Sport,
  Golf GTI, Jetta, Jetta GLI, Taos, Tiguan". No engine is named, so the list names none.
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years", read as on the 2022 Tiguan:
  "Change AWD clutch fluid (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`. That
  reading is confirmed by the owner on [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part B, 2026-10-29).
- **No gearbox row.** The 7-speed DSG row is "Only: Golf R, Golf GTI, Jetta GLI"; the non-DSG automatic row, every
  80,000 miles, names no model
  ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)).
- **No months on the columns**, as on the 2022 GTI's list. The oil reminder is 10,000 or 12 months (#517).
- Kept for every model, as there: the trailer hitch check ("(if equipped)", no model named on this card) and the
  torque splitter oil check, "(if fitted)", table-only. Left out, as there: the front axle differential lock fluid,
  the panorama sunroof test and the sunroof drains check.
- vPIC's model list for Volkswagen, 2022 (`GetModelsForMakeYear`, read 2026-10-29), names it "Atlas".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name); dust and
  pollen filter 40,000 / 24 mo; spark plugs 80,000 / 72 mo.
