# Volkswagen Jetta 1.4T, 2020–2021 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2020-2021-1.4t.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 Jetta card read, the model names and what isn't covered are on
the [2019 Jetta 1.4T's page](volkswagen-jetta-2019-1.4t.md#every-mk7-jetta-card-read).

Source: Volkswagen's all-model "Maintenance Schedule" cards, filed with NHTSA as manufacturer communications. Read on
2026-10-29, in full, as page images, by eye. These are cards from the time.

| Model year | What was read | Document |
|---|---|---|
| 2020 | "2020 VW Maintenance Schedule - All Models except e-Golf", revised 11/26/2019, the newest revision | `2019/MC-10168643-0001` |
| 2021 | "2021 VW Maintenance Schedule - All Model Gasoline Vehicles", 7/9/2020, the only one; the list's source | `2020/MC-10177239-0001` |

- **The two cards agree, row for row, for the Jetta 1.4L.** They differ only in the 40,000-mile spark plug row's
  wording ("Except: Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI, …" on the 2020 card, "Only: Arteon, Passat, Golf R" on
  the 2021 card); the 80,000-mile or 6-year row is "Only: Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI, Tiguan, Atlas, Atlas
  Cross Sport" on both. So one list covers both years.
- **The cards are read as the [2020–2021 Golf 1.4T's list](volkswagen-golf-2020-2021-1.4t.md) reads them**, and the
  rows are that list's, in its wording:
  - **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months), by the "Jetta 1.4L" mark, so the list has
    `"engine": { "label": "1.4T", "displacementL": 1.4 }`.
  - **No gearbox row.** The 7-speed DSG is "Only: Golf GTI, Jetta GLI, Golf R", the GLI's; "Transmission,
    automatic: Change fluid (excluding Canada) (only non-DSG Automatic Transmission)", every 80,000 miles, names no
    model or engine, so it is left out
    ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514), asked again on
    #445).
- The AWD clutch fluid is left out: the Mk7 Jetta was sold with front-wheel drive only. The trailer hitch is "Tiguan
  and Atlas only".
- **Model and engine.** vPIC names the 2020 GLI "Jetta" (VIN pattern `3VW6T7BU*LM`, 2.0 litres) and the 2021 GLI
  "Jetta GLI" (`3VW6T7BU*MM`); the [2020–2021 GLI list](volkswagen-jetta-gli-2020-2021.md) is for the 2.0, so this one
  names the 1.4.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24 months by
  time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24 mo; front axle
  differential lock fluid every 3 years "(if fitted)"; spark plugs 80,000 / 72 mo; sunroof drains, front and rear,
  every 20,000 / 24 mo "(if fitted)".
