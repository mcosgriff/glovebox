# Volkswagen Atlas 2.0T, 2018

How `app/src/main/assets/known-schedules/volkswagen-atlas-2018-2.0t.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). The [2018 3.6L](volkswagen-atlas-2018-3.6.md) list is made the same
way from the same card. Every Atlas card read, and what isn't covered, is [below](#every-atlas-card-read); the
Atlas Cross Sport's are on the [2020–2021 Atlas Cross Sport's page](volkswagen-atlas-cross-sport-2020-2021.md).

Source: Volkswagen's 2018 all-model "Maintenance Schedule" card, the newest revision, revised 11/26/2019,
`2019/MC-10168646-0001`, filed with NHTSA as a manufacturer communication, read in full on 2026-10-29, as page
images, by eye. It is read as [the 2018 Jetta's page](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read)
says.

- **The list is the [2018 Tiguan](volkswagen-tiguan-2018.md)'s, row for row**, with the model's name and an engine:
  the card names no row for the Tiguan and not the Atlas. That is the
  [2018 Golf Alltrack](volkswagen-golf-alltrack-2018.md)'s rows plus:
  - **"Removable trailer hitch: Check function and inspect for damage (Tiguan, Tiguan Limited, and Atlas only if
    equipped)"**, in the Standard service: "Check removable trailer hitch (if fitted)", at 20,000 and every 20,000
    to 120,000. Table-only.
  - **"Hood latch: Lubricate (Does not apply to: Passat, Golf, Beetle, and Jetta)"**, in the Extended service: the
    Atlas is none of the four, so "Lubricate hood latch", at 40,000, 80,000 and 120,000. Table-only.
- **One list per engine** (the owner's ruling of 2026-10-23), as the [2018 Passat](volkswagen-passat-2018-2.0t.md)'s
  are split: the only rows that name engines are the two spark plug rows, "*4-cyl Gasoline: 1.4L, 1.8L, and 2.0L"
  every 40,000 miles or 4 years, and "*6-cyl Gasoline: 3.6L engines only" every 60,000 miles or 6 years. This list
  has `"engine": { "label": "2.0T", "displacementL": 2.0 }` and spark plugs 40,000 / 48 months; the 3.6L's has
  60,000 / 72. Every other row is the same in both. vPIC decodes `1V2AP2CA*JC` (2018, trim "S") as "Atlas", 2.0 L,
  4 cylinders, and `1V2BR2CA*JC`, `1V2DR2CA*JC`, `1V2LR2CA*JC` as "Atlas", 3.6 L, 6 cylinders (read 2026-10-29).
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years (AWD only vehicles)": "Change AWD
  clutch fluid (AWD)", an `additional` reminder by time alone, every 36 months, no service line, in
  `allWheelDriveOnly`, never on the grid. vPIC decodes every 2018 pattern above, the 2.0 L "S" included, as
  "AWD/All-Wheel Drive" (8-speed "Automatic"), so it is on both engines' lists; it shows only on a car whose drive is
  AWD.
- **Both gearbox rows are left out.** The 7-speed DSG is "Golf R only"; the Atlas's row would be "Transmission,
  Automatic (non-DSG): Change fluid", every 80,000 miles, which names no model, and it is left out as on the
  Tiguan's lists ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514),
  [#449's question 2](https://github.com/mcosgriff/glovebox/issues/449#issuecomment-6492074401)).
- Left out as not the Atlas's, by their own words: the convertible top, the sunroof water drains (Passat only) and
  the 1.4L Jetta's belt checks.
- Reminders: engine oil 10,000 / 12 mo; air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (its
  first time, 3 years, in its name); dust and pollen filter 40,000 / 24 mo; spark plugs 40,000 / 48 mo; the
  panoramic sunroof rails, the front differential lock and the tire filler bottle "(if fitted)" and the AWD clutch
  fluid, as on the 2018 Tiguan's list.

## Every Atlas card read

The Atlas was sold in the USA from 2018; the Atlas Cross Sport from 2020. Each year's card is an all-model card;
none is the Atlas's own. Read on 2026-10-29, as page images, by eye.

| Model year | Document | List |
|---|---|---|
| 2018 | `2019/MC-10168646-0001`, revised 11/26/2019, the newest revision | [2.0T](volkswagen-atlas-2018-2.0t.md), [3.6L](volkswagen-atlas-2018-3.6.md) |
| 2019 | `2020/MC-10183295-0001`, revised 11/12/2020, the newest revision | [2019](volkswagen-atlas-2019.md) |
| 2020 | `2019/MC-10168643-0001`, revised 11/26/2019, the newest revision | [2020–2021](volkswagen-atlas-2020-2021.md) |
| 2021 | `2020/MC-10177239-0001`, 7/9/2020, the only one | [2020–2021](volkswagen-atlas-2020-2021.md) |
| 2022 | `2021/MC-10199495-0001`, revised 7/21/2021, the newest revision | [2022](volkswagen-atlas-2022.md) |
| 2023 | Volkswagen's reissue `USA5R507521`, printed 12/03/2025 (owners-literature site) | [2023–2026](volkswagen-atlas-2023-2026.md) |
| 2024 | Volkswagen's reissue `USA5R505721`, printed 12/03/2025 | [2023–2026](volkswagen-atlas-2023-2026.md) |
| 2025 | Volkswagen's reissue `USA5R701521`, printed 12/03/2025 | [2023–2026](volkswagen-atlas-2023-2026.md) |
| 2026 | Volkswagen's reissue `USA5R506821`, printed 02/2026 | [2023–2026](volkswagen-atlas-2023-2026.md) |

These are the cards the second-generation Tiguan's lists were made from, and each is read as those lists read it
(the [2018 Tiguan's page](volkswagen-tiguan-2018.md#every-second-generation-tiguan-card-read)); on every card the
Atlas's rows are the Tiguan's apart from the spark plugs on 2018 and the gearbox line on 2025–2026, so each Atlas
list is that year's Tiguan list with the Atlas's name.

**Why six lists.** 2018 is split by engine (the spark plugs). 2018 to 2019: the 2019 card puts the Atlas on the
80,000-mile spark plug row, whatever its engine. 2019 to 2020: the hood latch, the panoramic sunroof rails and the
tire filler bottle go, and the sunroof drains come in. 2021 to 2022: the columns lose their months and the belt
starter-generator and torque splitter checks come in. 2022 to 2023: the reissue's grid ends at 100,000 and names the
Atlas's gearbox. 2020 and 2021 agree for the Atlas; so do 2023 to 2026 (the gearbox line is "09P: Atlas, Tiguan" on
2023–2024 and "09H: Atlas, …" on 2025–2026, both every 80,000 miles).

**vPIC.** The model lists for Volkswagen (`GetModelsForMakeYear`, read 2026-10-29) name "Atlas" for every year from
2018 to 2026, and "Atlas Cross Sport" apart from it from 2020. Glovebox matches the model name whole, so the Atlas's
lists are never offered for an Atlas Cross Sport, which has [its own](volkswagen-atlas-cross-sport-2020-2021.md).
NHTSA's safety-rating list (`api.nhtsa.gov/SafetyRatings`, 2025, read 2026-10-29) has "2025 Volkswagen Atlas SUV
AWD" and "… FWD", so 4Motion was still sold; every list carries the AWD clutch fluid in `allWheelDriveOnly`.

## What isn't covered

- **The non-DSG automatic on 2018–2022**: left out (above),
  [#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514).
- **The AWD clutch fluid on 2022–2026**, printed "(if equipped)" (2022) and "All Applicable Vehicles" (2023 on): read
  as an AWD job, as the Tiguan's and the Golf R's lists do; that reading is confirmed by the owner on
  [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part B, 2026-10-29).
- **2019 on is not split by engine.** From 2019 the spark plug rows name the Atlas, not an engine, so the 2.0T and
  the 3.6L share each list.
- 2027 on: no maintenance card on the owners-literature site.
