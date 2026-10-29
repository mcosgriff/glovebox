# Volkswagen Atlas, 2020–2021

How `app/src/main/assets/known-schedules/volkswagen-atlas-2020-2021.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Atlas card read, and what isn't covered, is on the
[2018 2.0T's page](volkswagen-atlas-2018-2.0t.md#every-atlas-card-read).

Source: Volkswagen's all-model "Maintenance Schedule" cards, filed with NHTSA as manufacturer communications, read on
2026-10-29, in full, as page images, by eye: 2020 `2019/MC-10168643-0001` (revised 11/26/2019, the newest
revision) and 2021 `2020/MC-10177239-0001` (7/9/2020, the only one; the list's source).

- **The two cards agree, row for row, for the Atlas.** The 80,000-mile or 6-year spark plug row is "Only: Golf 1.4L,
  GTI, Jetta 1.4L, Jetta GLI, Tiguan, Atlas, Atlas Cross Sport" on both; the 40,000-mile row is "Except" those models
  (2020) or "Only: Arteon, Passat, Golf R" (2021).
- **The list is the [2020–2021 Tiguan](volkswagen-tiguan-2020-2021.md)'s, row for row**, with the model's name: the
  cards are read as the [2020–2021 Passat's page](volkswagen-passat-2020-2021.md#how-the-20202022-all-model-cards-are-read)
  says, and the only rows that name the Tiguan name the Atlas too:
  - **"Removable trailer hitch: Check function and inspect for damage (Tiguan and Atlas only if equipped)"**, in the
    Standard service: "Check removable trailer hitch (if fitted)", at 20,000 and every 20,000 to 120,000. Table-only.
  - **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months). The row names the Atlas and no engine, so the
    list names none.
- **The AWD clutch fluid** "(if equipped)", "Every 3 years": "Change AWD clutch fluid (AWD)", every 36 months by time
  alone, no service line, in `allWheelDriveOnly` (point 10 of that reading; the Atlas was sold with 4Motion, the
  vPIC decodes on the [2018 page](volkswagen-atlas-2018-2.0t.md)).
- **No gearbox row.** The 7-speed DSG is "Only: Golf GTI, Jetta GLI, Golf R". "Transmission, automatic: Change fluid
  (excluding Canada) (only non-DSG Automatic Transmission)", every 80,000 miles, names no model, so it is left out
  ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)).
- No hood latch row on these cards.
- vPIC's model lists for Volkswagen, 2020 and 2021 (`GetModelsForMakeYear`, read 2026-10-29), name it "Atlas".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name); dust and
  pollen filter 40,000 / 24 mo; front axle differential lock fluid every 3 years "(if fitted)"; spark plugs 80,000 /
  72 mo; sunroof drains, front and rear, every 20,000 / 24 mo "(if fitted)".
