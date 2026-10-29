# Volkswagen Tiguan, 2020–2021 (2nd generation)

How `app/src/main/assets/known-schedules/volkswagen-tiguan-2020-2021.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every second-generation Tiguan card read, and what isn't covered, is on
the [2018 Tiguan's page](volkswagen-tiguan-2018.md#every-second-generation-tiguan-card-read).

Source: Volkswagen's all-model "Maintenance Schedule" cards, filed with NHTSA as manufacturer communications. Read on
2026-10-29, in full, as page images, by eye. These are cards from the time.

| Model year | What was read | Document |
|---|---|---|
| 2020 | "2020 VW Maintenance Schedule - All Models except e-Golf", revised 11/26/2019, the newest revision | `2019/MC-10168643-0001` |
| 2021 | "2021 VW Maintenance Schedule - All Model Gasoline Vehicles", 7/9/2020, the only one; the list's source | `2020/MC-10177239-0001` |

- **The two cards agree, row for row, for the Tiguan.** The 80,000-mile or 6-year spark plug row is "Only: Golf 1.4L,
  GTI, Jetta 1.4L, Jetta GLI, Tiguan, Atlas, Atlas Cross Sport" on both; the 40,000-mile row is "Except" those models
  (2020) or "Only: Arteon, Passat, Golf R" (2021). So one list covers both years.
- **The cards are read as the [2020–2021 Passat's page](volkswagen-passat-2020-2021.md#how-the-20202022-all-model-cards-are-read)
  says.** The list is the [2020–2021 Golf 1.4T](volkswagen-golf-2020-2021-1.4t.md)'s (the Passat's rows with the
  80,000-mile plugs), with the model's name and no engine, and two more rows, by the cards' own words:
  - **"Removable trailer hitch: Check function and inspect for damage (Tiguan and Atlas only if equipped)"**, in the
    Standard service: "Check removable trailer hitch (if fitted)", at 20,000 and every 20,000 to 120,000, after the
    horn. Table-only.
  - **"AWD clutch: Change fluid (if equipped)"**, "Every 3 years", read as point 10 there says (the 2018 card's point
    8): "Change AWD clutch fluid (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`. The
    Tiguan was sold with front-wheel drive and with 4Motion (the vPIC decodes on the
    [2018 page](volkswagen-tiguan-2018.md)).
- **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months), by the Tiguan's name on the row. One engine, the
  2.0T, so the list names none.
- **No gearbox row.** The 7-speed DSG is "Only: Golf GTI, Jetta GLI, Golf R". The Tiguan's row would be
  "Transmission, automatic: Change fluid (excluding Canada) (only non-DSG Automatic Transmission)", every 80,000
  miles, which names no model, so it is left out
  ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)).
- No hood latch row on these cards.
- **Model name.** vPIC's model lists for Volkswagen, 2020 and 2021 (`GetModelsForMakeYear`, read 2026-10-29), name it
  "Tiguan".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name); dust and
  pollen filter 40,000 / 24 mo; front axle differential lock fluid every 3 years "(if fitted)"; spark plugs 80,000 /
  72 mo; sunroof drains, front and rear, every 20,000 / 24 mo "(if fitted)".
