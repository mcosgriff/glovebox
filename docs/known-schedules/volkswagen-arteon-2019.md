# Volkswagen Arteon, 2019

How `app/src/main/assets/known-schedules/volkswagen-arteon-2019.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). The later Arteon lists are made the same way from each year's card. Every
Arteon card read, and what isn't covered, is [below](#every-arteon-card-read).

Source: Volkswagen's 2019 "Maintenance Schedule" card for all its models except the e-Golf, filed with NHTSA as a
manufacturer communication: the newest revision, revised 11/12/2020, `2020/MC-10183295-0001`, read in full on
2026-10-29, as page images, by eye. The earlier revisions weren't read again; the
[2019 GTI's page](volkswagen-gti-2019.md) says what they print differently.

- **The card is read as the [2019 Passat's](volkswagen-passat-2019.md) and [2019 Tiguan's](volkswagen-tiguan-2019.md)
  lists read it.** No row names the Arteon, so its rows are the ones that name no model, plus those whose "Except" or
  "Does not apply to" leaves it in. The list is the 2019 Tiguan's without its trailer hitch row, with the Passat's
  spark plugs; set against the 2019 Passat's list:
  - **No sunroof water drains**: "Sunroof water drains: Check for clearance and clean if necessary (Passat only)".
  - **"Hood latch: Lubricate (Does not apply to: Passat, Golf, Beetle, and Jetta)"**, in the Extended service: the
    Arteon is none of the four, so "Lubricate hood latch", at 40,000, 80,000 and 120,000. Table-only.
  - **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years": "Change AWD clutch fluid
    (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`, never on the grid, as on the 2019
    Tiguan. The Arteon was sold with front-wheel drive and with 4Motion: NHTSA's safety-rating list
    (`api.nhtsa.gov/SafetyRatings`, 2019, read 2026-10-29) has "2019 Volkswagen Arteon 4 DR FWD" and "… 4 DR AWD", so
    it shows only on a car whose drive is AWD.
- **Spark plugs every 40,000 miles or 4 years** (40,000 / 48 months): the row "(Except Atlas, Golf SportWagen 1.4L,
  Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI, Tiguan)" doesn't except the Arteon, and the 80,000 / 6-year row is "(Only …)"
  those models. No row names an engine, so the list names none.
- **Both gearbox rows are left out.** "Transmission: 6-Speed DSG automatic transmission: Change fluid and filter"
  (every 40,000) and "Transmission, automatic (non-DSG automatic transmission): Change fluid" (every 80,000) name no
  model, and the card doesn't say which an Arteon has
  ([#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333),
  [#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)). The 7-speed DSG is
  "Golf GTI, Jetta GLI, Golf R only".
- Left out by their own words: the removable trailer hitch ("Tiguan, Tiguan Limited, and Atlas only if equipped"),
  the convertible top (Beetle Convertible only) and the sunroof water drains (Passat only).
- vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`, read 2026-10-29), names it "Arteon".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name); dust and
  pollen filter 40,000 / 24 mo; front axle differential lock fluid every 3 years "(if fitted)"; panoramic sunroof
  rails every 40,000 or 2 years, first at 40,000 (the card's "or 3 years" for the first time is recorded, not put
  into months); the tire filler bottle's date every 2 years "(if fitted)"; spark plugs 40,000 / 48 mo.

## Every Arteon card read

The Arteon was sold in the USA for 2019 to 2023. Each year's card is an all-model card; none is the Arteon's own
(NHTSA's index files none under the Arteon). Read on 2026-10-29, as page images, by eye.

| Model year | Document | List |
|---|---|---|
| 2019 | `2020/MC-10183295-0001`, revised 11/12/2020, the newest revision | [2019](volkswagen-arteon-2019.md) |
| 2020 | `2019/MC-10168643-0001`, revised 11/26/2019, the newest revision | [2020–2021](volkswagen-arteon-2020-2021.md) |
| 2021 | `2020/MC-10177239-0001`, 7/9/2020, the only one | [2020–2021](volkswagen-arteon-2020-2021.md) |
| 2022 | `2021/MC-10199495-0001`, revised 7/21/2021, the newest revision | [2022](volkswagen-arteon-2022.md) |
| 2023 | Volkswagen's reissue `USA5R507521`, printed 12/03/2025 (owners-literature site) | [2023](volkswagen-arteon-2023.md) |

These are the cards the [Passat's](volkswagen-passat-2020-2021.md#how-the-20202022-all-model-cards-are-read),
second-generation [Tiguan's](volkswagen-tiguan-2018.md#every-second-generation-tiguan-card-read) and
[Golf R's](volkswagen-golf-r-2023-2025.md) lists were made from, and each is read as those lists read it. On
2019–2022 the only row that names the Arteon is the 40,000-mile spark plugs (by "Except" in 2019 and 2020, by
"Only: Arteon, …" in 2021 and 2022); on the 2023 reissue it is named on the sunroof drains, the 40,000-mile spark
plugs, the DSG line ("0GC (DSG): Arteon, GTI, Golf R, Jetta, Taos") and the camshaft toothed-belt check.

**Why four lists.** 2019 to 2020: the hood latch, the panoramic sunroof rails and the tire filler bottle go, and the
sunroof drains come in. 2020 and 2021 agree for the Arteon. 2021 to 2022: the columns lose their months and the belt
starter-generator and torque splitter checks come in. 2022 to 2023: the reissue's grid ends at 100,000 and names the
Arteon's gearbox.

**vPIC.** The model lists for Volkswagen (`GetModelsForMakeYear`, read 2026-10-29) name "Arteon" for every year from
2019 to 2023, and not 2024. vPIC decodes `WVWBR7AN*LE` (2020) as "Arteon", trim "4Motion SE", "AWD/All-Wheel Drive",
8-speed "Automatic", 2.0 L, and `WVWTR7AN*PE` (2023) as "Arteon", "4Motion SEL Premium R-Line", "AWD/All-Wheel
Drive", 7-speed "Automatic", 2.0 L (read 2026-10-29). One engine, the 2.0T, so no list names one. Every list carries
the AWD clutch fluid in `allWheelDriveOnly`.

## What isn't covered

- **The gearbox on 2019–2022**: no gearbox row on these cards names the Arteon (above;
  [#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333),
  [#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)). vPIC's 8-speed
  "Automatic" for 2020 is recorded, not used to pick a row.
- **The AWD clutch fluid on 2022 and 2023**, printed "(if equipped)" (2022) and "All Applicable Vehicles" (2023):
  read as an AWD job, as the Tiguan's, Atlas's and Golf R's lists do; that reading waits on
  [#437's question 5](https://github.com/mcosgriff/glovebox/issues/437#issuecomment-6491507764).
- **2024 on**: vPIC names no Arteon after 2023, though the 2024 reissue (`USA5R505721`) still names it on the same
  rows as 2023's.
