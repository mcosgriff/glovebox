# Volkswagen Jetta GLI, 2020–2021 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-jetta-gli-2020-2021.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 Jetta card read, the model names and what isn't covered are on
the [2019 Jetta 1.4T's page](volkswagen-jetta-2019-1.4t.md#every-mk7-jetta-card-read).

Source: the 2020 card, revised 11/26/2019 (`2019/MC-10168643-0001`, the newest revision), and the 2021 card, 7/9/2020
(`2020/MC-10177239-0001`, the only one; the list's source), Volkswagen's all-model "Maintenance Schedule" cards filed
with NHTSA. Read on 2026-10-29, in full, as page images, by eye.

- **The two cards agree, row for row, for the GLI**: "Jetta GLI" is on the 80,000-mile spark plug row ("Only" on
  both; "Except" on the 2020 card's 40,000-mile row) and the 7-speed DSG row on both.
- **The cards are read as the [2020–2021 GTI's list](volkswagen-gti-2020-2021.md) reads them**, and the rows are that
  list's, in its wording:
  - **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months).
  - **The 7-speed DSG**, "Every 80,000 miles. Only: Golf GTI, Jetta GLI, Golf R": "Change DSG transmission fluid (DSG
    only)", 80,000, in `manualExcluded`. The non-DSG automatic's row names no model and is left out (the GLI has a
    manual or the DSG).
  - Sunroof drains, front and rear, every 20,000 / 24 mo "(if fitted)", and the front axle differential lock fluid
    "(if fitted)", as on the GTI's list.
- Left out: the AWD clutch fluid (the Mk7 Jetta was sold with front-wheel drive only; vPIC decodes the 2020 and 2021
  GLI's VIN patterns as "FWD/Front-Wheel Drive"), the 40,000-mile spark plugs (Arteon, Passat, Golf R) and the trailer
  hitch (Tiguan and Atlas only).
- **Model and engine.** vPIC's model list has "Jetta" alone for 2020 and "Jetta" and "Jetta GLI" for 2021; it decodes
  the GLI's VIN pattern as "Jetta", trim "GLI (S, Autobahn)", 2.0 litres for 2020 (`3VW6T7BU*LM`) and as "Jetta GLI",
  2.0 litres for 2021 (`3VW6T7BU*MM`). So the list is for "Jetta GLI" with `modelAliases` "Jetta", and with
  `"engine": { "label": "2.0T (GLI)", "displacementL": 2.0 }` so that a "Jetta" is matched to it only by its 2.0
  engine, beside the [1.4T's list](volkswagen-jetta-2020-2021-1.4t.md).
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24 months by
  time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24 mo; front axle
  differential lock fluid every 3 years "(if fitted)"; spark plugs 80,000 / 72 mo; sunroof drains every 20,000 / 24
  mo "(if fitted)"; 7-speed DSG fluid 80,000 (not for a manual).
