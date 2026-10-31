# Volkswagen Tiguan, 2018 (2nd generation)

How `app/src/main/assets/known-schedules/volkswagen-tiguan-2018.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every second-generation Tiguan card read, and what isn't covered, is
[below](#every-second-generation-tiguan-card-read).

Source: Volkswagen's 2018 all-model "Maintenance Schedule" card, the newest revision, revised 11/26/2019,
`2019/MC-10168646-0001`, filed with NHTSA as a manufacturer communication, read in full on 2026-10-29, as page
images, by eye. It is read as [the 2018 Jetta's page](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read)
says.

- **The model.** In 2018 Volkswagen sold the new, second-generation Tiguan beside the first one, which it called
  the "Tiguan Limited". vPIC's model list for Volkswagen, 2018 (`GetModelsForMakeYear`, read 2026-10-29), names
  both, and vPIC decodes `3VV1B7AX*JM`, `3VV0B7AX*JM` and `3VV2B7AX*JM` (model year 2018, read 2026-10-29) as
  "Tiguan". So this list is for "Tiguan", with no alias, and the
  [2018 Tiguan Limited's list](volkswagen-tiguan-limited-2018.md) is for "Tiguan Limited": Glovebox matches the
  model name whole, so neither list can be offered for the other's car. (An owner who types "Tiguan" by hand for a
  Tiguan Limited gets this list; the two read the same card and differ only in their name.)
- **The list is the 2018 Tiguan Limited's, row for row**, with the model's name: the card names no row for one
  Tiguan and not the other. That is the [2018 Golf Alltrack](volkswagen-golf-alltrack-2018.md)'s rows plus:
  - **"Removable trailer hitch: Check function and inspect for damage (Tiguan, Tiguan Limited, and Atlas only if
    equipped)"**, in the Standard service: "Check removable trailer hitch (if fitted)", at 20,000 and every 20,000
    to 120,000, after the horn. Table-only.
  - **"Hood latch: Lubricate (Does not apply to: Passat, Golf, Beetle, and Jetta)"**, in the Extended service:
    "Lubricate hood latch", at 40,000, 80,000 and 120,000. Table-only.
- **Spark plugs 40,000 miles or 4 years** (40,000 / 48 months), by the row's engine mark "*4-cyl Gasoline: 1.4L,
  1.8L, and 2.0L". The Tiguan had one engine, the 2.0T (vPIC: 2.0 L, gasoline), so the list names none. The 2019
  card moves the Tiguan to 80,000 miles or 6 years ([2019](volkswagen-tiguan-2019.md)); the 2018 card is kept as
  printed.
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years (AWD only vehicles)". The
  Tiguan was sold with front-wheel drive and with 4Motion: vPIC decodes `3VV1B7AX*JM` (trim "S") as
  "FWD/Front-Wheel Drive" and `3VV0B7AX*JM` ("4Motion S") and `3VV2B7AX*JM` ("SE, 4Motion SEL") as "AWD/All-Wheel
  Drive". So, by the card's reading (point 8), "Change AWD clutch fluid (AWD)", an `additional` reminder by time
  alone, every 36 months, no service line, in `allWheelDriveOnly`, never on the grid.
- **Both gearbox rows are left out.** vPIC decodes every pattern above as an 8-speed "Automatic"; no DSG row could
  be the Tiguan's (the 7-speed DSG is "Golf R only"). Its row would be "Transmission, Automatic (non-DSG): Change
  fluid", every 80,000 miles, which names no model, and it is left out as on the Passat's and the Tiguan Limited's
  lists ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514),
  [#449's question 2](https://github.com/mcosgriff/glovebox/issues/449#issuecomment-6492074401)).
- Left out as not the Tiguan's, by their own words: the convertible top, the sunroof water drains (Passat only), the
  1.4L Jetta's belt checks and the 3.6L's spark plugs.
- Reminders: engine oil 10,000 / 12 mo; air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (its
  first time, 3 years, in its name); dust and pollen filter 40,000 / 24 mo; spark plugs 40,000 / 48 mo; the
  panoramic sunroof, the front differential lock and the tire filler bottle "(if fitted)" and the AWD clutch fluid,
  as on the Tiguan Limited's list.

## Every second-generation Tiguan card read

The second-generation Tiguan was sold in the USA from 2018 to 2024; the third generation is 2025 on. Each year's
card is an all-model card; none is the Tiguan's own. Read on 2026-10-29, as page images, by eye.

| Model year | Document | List |
|---|---|---|
| 2018 | `2019/MC-10168646-0001`, revised 11/26/2019, the newest revision | [2018](volkswagen-tiguan-2018.md) |
| 2019 | `2020/MC-10183295-0001`, revised 11/12/2020, the newest revision | [2019](volkswagen-tiguan-2019.md) |
| 2020 | `2019/MC-10168643-0001`, revised 11/26/2019, the newest revision | [2020–2021](volkswagen-tiguan-2020-2021.md) |
| 2021 | `2020/MC-10177239-0001`, 7/9/2020, the only one | [2020–2021](volkswagen-tiguan-2020-2021.md) |
| 2022 | `2021/MC-10199495-0001`, revised 7/21/2021, the newest revision | [2022](volkswagen-tiguan-2022.md) |
| 2023 | Volkswagen's reissue `USA5R507521`, printed 12/03/2025 (owners-literature site) | [2023–2024](volkswagen-tiguan-2023-2024.md) |
| 2024 | Volkswagen's reissue `USA5R505721`, printed 12/03/2025 (owners-literature site) | [2023–2024](volkswagen-tiguan-2023-2024.md) |

Each card is read as the earlier lists from it read it: 2018 as [the 2018 Jetta's page](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read)
says, 2019 as the [2019 Passat](volkswagen-passat-2019.md) and [Golf Alltrack](volkswagen-golf-alltrack-2019.md),
2020–2022 as [the 2020–2021 Passat's page](volkswagen-passat-2020-2021.md#how-the-20202022-all-model-cards-are-read)
says, and 2023–2024 as the [2023–2026 GTI](volkswagen-gti-2023-2026.md) and [2023 Jetta](volkswagen-jetta-2023.md)
lists. The earlier revisions of the 2018, 2019, 2020 and 2022 cards weren't read again; the GTI pages say how they
differ, and the newest revision wins (the owner's ruling, #143).

**Why five lists.** Each pair of neighbouring years differs for the Tiguan: 2018 to 2019 the spark plugs (40,000 / 4
years to 80,000 / 6 years) and the 2019 card's wording; 2019 to 2020 the hood latch, the panoramic sunroof rails and
tire filler bottle rows go, and the sunroof drains come in; 2021 to 2022 the columns lose their months and the
belt starter-generator and torque splitter checks come in; 2022 to 2023 the reissue's grid ends at 100,000 and
names the Tiguan's gearbox. 2020 and 2021 agree for the Tiguan, as do 2023 and 2024.

**vPIC.** The model lists for Volkswagen (`GetModelsForMakeYear`, read 2026-10-29) name "Tiguan" for every year from
2018 to 2024 ("Tiguan Limited" too in 2018). vPIC decodes `3VV0B7AX*RM` and `3VV2B7AX*RM` (2024) as "Tiguan",
2.0 L, "Automatic", and `3VV2B7AX*RM` as "AWD/All-Wheel Drive" (trim "4Motion SE Wolfsburg Edition"), so 4Motion was
still sold at the end; every list carries the AWD clutch fluid in `allWheelDriveOnly`.

## What isn't covered

- **2025 on**: the third generation (vPIC still says "Tiguan"; the 2025 and 2026 reissues name it "Tiguan (RM1)").
  It isn't this issue's, and no list is made, so a 2025 Tiguan is offered none.
- **The non-DSG automatic on 2018–2022** (above, and on each page): left out,
  [#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514), asked again for the
  Tiguan on [#450](https://github.com/mcosgriff/glovebox/issues/450).
- **The AWD clutch fluid on 2022–2024**, printed "(if equipped)" (2022) and "All Applicable Vehicles" (2023–2024):
  read as an AWD job, as the Golf R's lists do; that reading is confirmed by the owner on
  [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part B, 2026-10-29).
- The first-generation Tiguan's years are on the [2013–2015 Tiguan's page](volkswagen-tiguan-2013-2015.md#what-isnt-covered).
