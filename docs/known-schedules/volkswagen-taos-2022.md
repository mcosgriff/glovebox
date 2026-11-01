# Volkswagen Taos, 2022

How `app/src/main/assets/known-schedules/volkswagen-taos-2022.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). The later Taos lists are made the same way from each year's card. Every
Taos card read, and what isn't covered, is [below](#every-taos-card-read).

Source: Volkswagen's "2022 VW Maintenance Schedule - All Model Gasoline Vehicles", USA, filed with NHTSA as a
manufacturer communication: the newest revision, revised 7/21/2021, `2021/MC-10199495-0001`, read in full on
2026-10-29, as page images, by eye. Volkswagen's reissue of the card (`USA5R507921`, 12/03/2025) isn't used: the card
from the time wins (the owner's ruling of 2026-10-23).

- **The card is read as the [2022 GTI's list](volkswagen-gti-2022.md) reads it**, and its only row that names the
  Taos is the **spark plugs every 80,000 miles or 6 years** (80,000 / 72 months), "Only: Atlas, Atlas Cross Sport,
  Golf GTI, Jetta, Jetta GLI, Taos, Tiguan". No engine is named, so the list names none.
- **The list is the [2022 Tiguan](volkswagen-tiguan-2022.md)'s, row for row**, with the model's name: no row of the
  card names the Tiguan alone, so the two read the same.
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years", read as on the 2022 Tiguan and
  Arteon: "Change AWD clutch fluid (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`.
  The Taos was sold with front-wheel drive and with 4Motion (below). That reading is confirmed by the owner on
  [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part B, 2026-10-29).
- **No gearbox row.** "Transmission, 7-Speed DSG Automatic Transmission: Change fluid", every 80,000 miles, is
  "Only: Golf R, Golf GTI, Jetta GLI", which leaves the Taos out; "Transmission, automatic: Change fluid (excluding
  Canada) (only non-DSG Automatic Transmission)", every 80,000 miles, names no model
  ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)). Neither is put on
  the list; see [#455's question 3](https://github.com/mcosgriff/glovebox/issues/455#issuecomment-6493694589).
- **No months on the columns**, as on the 2022 GTI's list. The oil reminder is 10,000 or 12 months, from the minor
  service's "or 1 year" (#517).
- Kept for every model, as there: the trailer hitch check ("(if equipped)", no model named on this card) and the
  torque splitter oil check, "(if fitted)", table-only. Left out, as there: the front axle differential lock fluid,
  the panorama sunroof test and the sunroof drains check.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name); dust and
  pollen filter 40,000 / 24 mo; spark plugs 80,000 / 72 mo.

## Every Taos card read

The Taos is sold in the USA from model year 2022. Each year's card is an all-model card; none is the Taos's own. Read
on 2026-10-29, as page images, by eye.

| Model year | Document | List |
|---|---|---|
| 2022 | `2021/MC-10199495-0001`, revised 7/21/2021, the newest revision | [2022](volkswagen-taos-2022.md) |
| 2023 | Volkswagen's reissue `USA5R507521`, printed 12/03/2025 (owners-literature site) | [2023–2024](volkswagen-taos-2023-2024.md) |
| 2024 | Volkswagen's reissue `USA5R505721`, printed 12/03/2025 | [2023–2024](volkswagen-taos-2023-2024.md) |
| 2025 | Volkswagen's reissue `USA5R701521`, printed 12/03/2025 | [2025–2026](volkswagen-taos-2025-2026.md) |
| 2026 | Volkswagen's reissue `USA5R506821`, printed 02/2026 | [2025–2026](volkswagen-taos-2025-2026.md) |

These are the cards the [Tiguan's](volkswagen-tiguan-2018.md#every-second-generation-tiguan-card-read),
[Atlas's](volkswagen-atlas-2023-2026.md) and [Jetta's](volkswagen-jetta-2023.md) lists were made from, and each is
read as those lists read it.

**The 2021 card filed as "Tarek".** NHTSA's index files the 2021 all-model card (`2020/MC-10177239-0001`) under
"Tarek", the Taos's name before launch, as well as the other models. The card itself names neither (its text was
searched on 2026-10-29), and vPIC's model list for Volkswagen, 2021 (`GetModelsForMakeYear`), has no Taos or Tarek:
the first Taos is a 2022. So no 2021 list.

**Why three lists.** 2022 to 2023: the card from the time gives way to the reissue, whose grid ends at 100,000 and whose
gearbox row names the Taos. 2023 and 2024 agree for the Taos. 2024 to 2025: the Taos moves onto two gearbox rows that
differ, and that row is left out.

**vPIC and drive.** vPIC's model lists for Volkswagen (`GetModelsForMakeYear`, read 2026-10-29) name "Taos" for every
year from 2022 to 2026 (and "Tarek" for none). NHTSA's safety-rating lists (`api.nhtsa.gov/SafetyRatings`, read
2026-10-29) have "Taos SUV AWD" and "Taos SUV FWD" for 2022, 2024 and 2025 (2026: none yet), so every list carries the
AWD clutch fluid in `allWheelDriveOnly` and a car on record as front-wheel drive doesn't get it. One engine, the 1.5T,
so no list names one.

## What isn't covered

- **The gearbox on 2022** (above), and **on 2025 and 2026**
  ([2025–2026's page](volkswagen-taos-2025-2026.md)); asked on
  [#455](https://github.com/mcosgriff/glovebox/issues/455#issuecomment-6493694589).
- **The AWD clutch fluid on 2022 on**, printed "(if equipped)" (2022) and "All Applicable Vehicles" (2023 on): read as
  an AWD job, as the Tiguan's, Atlas's and Arteon's lists do; that reading is confirmed by the owner on
  [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part B, 2026-10-29).
