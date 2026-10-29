# Volkswagen Golf 1.4T, 2020–2021 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-2020-2021-1.4t.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md). Every Mk7 card read, and what isn't covered, is on the
[2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).

Source: Volkswagen's all-model "Maintenance Schedule" cards, filed with NHTSA as manufacturer communications.
Read on 2026-10-29, in full, as page images, by eye. These are cards from the time.

| Model year | What was read | Document |
|---|---|---|
| 2020 | "2020 VW Maintenance Schedule - All Models except e-Golf", revised 11/26/2019, the newest revision | `2019/MC-10168643-0001` |
| 2021 | "2021 VW Maintenance Schedule - All Model Gasoline Vehicles", 7/9/2020, the only one; the list's source | `2020/MC-10177239-0001` |

The earlier 2020 revisions (`2019/MC-10163843`, `2019/MC-10164263`) weren't read; the
[2020–2021 GTI's page](volkswagen-gti-2020-2021.md) says what they print differently.

- **The two cards agree, row for row, for the Golf 1.4L.** They differ only in the 40,000-mile spark plug
  row's wording ("Except: Golf 1.4L, GTI, …" on the 2020 card, "Only: Arteon, Passat, Golf R" on the 2021
  card); the 80,000-mile or 6-year row is "Only: Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI, Tiguan, Atlas, Atlas
  Cross Sport" on both. So one list covers both years.
- **The cards are read as the [2020–2021 Passat's page](volkswagen-passat-2020-2021.md#how-the-20202022-all-model-cards-are-read)
  says.** The rows are the [2020–2021 Passat](volkswagen-passat-2020-2021.md) list's, in its wording, but for
  the spark plugs:
  - **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months), by the "Golf 1.4L" mark. The mark
    names the engine, so the list has `"engine": { "label": "1.4T", "displacementL": 1.4 }`. On the 2021
    card a Golf of another engine would be on neither row, so no other list is made.
  - **No gearbox row.** The 7-speed DSG is "Only: Golf GTI, Jetta GLI, Golf R"; the non-DSG automatic's row
    names no model or engine, so it is left out
    ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514), asked
    again on #445).
- The AWD clutch fluid is left out: the 2020–2021 Golf was sold with front-wheel drive only. The trailer hitch
  is "Tiguan and Atlas only".
- **Model name.** vPIC's model list for Volkswagen names it "Golf" for 2020 (beside "Golf SportWagen" and
  "Golf Alltrack") and 2021 (`GetModelsForMakeYear`, read 2026-10-29).
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24
  months by time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24
  mo; front axle differential lock fluid every 3 years "(if fitted)"; spark plugs 80,000 / 72 mo; sunroof
  drains, front and rear, every 20,000 / 24 mo "(if fitted)".
