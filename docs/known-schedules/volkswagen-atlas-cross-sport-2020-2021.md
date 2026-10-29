# Volkswagen Atlas Cross Sport, 2020–2021

How `app/src/main/assets/known-schedules/volkswagen-atlas-cross-sport-2020-2021.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md). Every Atlas Cross Sport card read, and what isn't covered, is
[below](#every-atlas-cross-sport-card-read).

Source: Volkswagen's all-model "Maintenance Schedule" cards, filed with NHTSA as manufacturer communications, read on
2026-10-29, in full, as page images, by eye: 2020 `2019/MC-10168643-0001` (revised 11/26/2019, the newest
revision) and 2021 `2020/MC-10177239-0001` (7/9/2020, the only one; the list's source).

- **The model.** vPIC's model lists for Volkswagen (`GetModelsForMakeYear`, read 2026-10-29) name "Atlas Cross Sport"
  apart from "Atlas" from 2020 to 2026, and the cards name the two apart too. So this list is for "Atlas Cross
  Sport", with no alias, and the Atlas's lists are for "Atlas": Glovebox matches the model name whole, so neither is
  offered for the other's car.
- **The list is the [2020–2021 Atlas](volkswagen-atlas-2020-2021.md)'s without the trailer hitch check.** The two
  cards agree for it, read as that page says:
  - **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months): "Only: Golf 1.4L, GTI, Jetta 1.4L, Jetta
    GLI, Tiguan, Atlas, Atlas Cross Sport" on both cards. No engine is named, so the list names none.
  - **The trailer hitch row is left out, by its own words**: "Removable trailer hitch: Check function and inspect for
    damage (Tiguan and Atlas only if equipped)". The same cards name "Atlas Cross Sport" apart from "Atlas" on the
    spark plug rows, so the hitch row isn't read as the Cross Sport's. (The 2022 card's hitch row names no model, so
    the [2022 list](volkswagen-atlas-cross-sport-2022.md) has it.) Asked on #453.
  - **The AWD clutch fluid** "(if equipped)", "Every 3 years": "Change AWD clutch fluid (AWD)", every 36 months by
    time alone, no service line, in `allWheelDriveOnly`. The Atlas Cross Sport was sold with front-wheel drive and
    with 4Motion: NHTSA's safety-rating list (`api.nhtsa.gov/SafetyRatings`, 2020, read 2026-10-29) has "2020
    Volkswagen Atlas Cross Sport SUV AWD" and "… FWD".
  - **No gearbox row**: the non-DSG automatic row names no model
    ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)).
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name); dust and
  pollen filter 40,000 / 24 mo; front axle differential lock fluid every 3 years "(if fitted)"; spark plugs 80,000 /
  72 mo; sunroof drains, front and rear, every 20,000 / 24 mo "(if fitted)".

## Every Atlas Cross Sport card read

The Atlas Cross Sport was sold in the USA from 2020. The cards are the all-model ones the
[Atlas's page](volkswagen-atlas-2018-2.0t.md#every-atlas-card-read) lists, read on 2026-10-29, as page images, by eye.

| Model year | Document | Names "Atlas Cross Sport" on | List |
|---|---|---|---|
| 2020 | `2019/MC-10168643-0001`, revised 11/26/2019 | both spark plug rows | [2020–2021](volkswagen-atlas-cross-sport-2020-2021.md) |
| 2021 | `2020/MC-10177239-0001`, 7/9/2020 | the 80,000-mile spark plug row | [2020–2021](volkswagen-atlas-cross-sport-2020-2021.md) |
| 2022 | `2021/MC-10199495-0001`, revised 7/21/2021 | the 80,000-mile spark plug row | [2022](volkswagen-atlas-cross-sport-2022.md) |
| 2023 | reissue `USA5R507521`, printed 12/03/2025 | nothing ("Atlas Sport" on the 80,000-mile spark plug row) | none |
| 2024 | reissue `USA5R505721`, printed 12/03/2025 | nothing | none |
| 2025 | reissue `USA5R701521`, printed 12/03/2025 | the 80,000-mile spark plug row | [2025](volkswagen-atlas-cross-sport-2025.md) |
| 2026 | reissue `USA5R506821`, printed 02/2026 | the spark plug row and "09H: Atlas, Atlas Cross Sport, Tiguan (RM1)" | [2026](volkswagen-atlas-cross-sport-2026.md) |

## What isn't covered

- **2023 and 2024.** The only sources are Volkswagen's reissues, and neither names the Atlas Cross Sport: the 2023
  card's spark plug row says "Atlas, Atlas Sport" and its automatic row "09P: Atlas, Tiguan"; the 2024 card's say
  "Atlas" and "09P: Atlas, Tiguan". Whether those rows (and so the card) are the Cross Sport's would be a guess, so no
  list is made and a 2023 or 2024 Atlas Cross Sport is offered none. Asked on #453.
- **The 2020–2021 trailer hitch check** (above), asked on #453.
- **The non-DSG automatic on 2020–2022**: [#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514).
- **The 2025 automatic**: the 2025 card's gearbox line names "09H: Atlas, Tiguan (RM1)", not the Cross Sport, so the
  [2025 list](volkswagen-atlas-cross-sport-2025.md) has no gearbox reminder; asked on #453.
- **The AWD clutch fluid on 2022, 2025 and 2026** ("(if equipped)", "All Applicable Vehicles"): read as an AWD job,
  waiting on [#437's question 5](https://github.com/mcosgriff/glovebox/issues/437#issuecomment-6491507764).
