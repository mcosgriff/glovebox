# Volkswagen Tiguan, 2023–2024 (2nd generation)

How `app/src/main/assets/known-schedules/volkswagen-tiguan-2023-2024.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every second-generation Tiguan card read, and what isn't covered, is on
the [2018 Tiguan's page](volkswagen-tiguan-2018.md#every-second-generation-tiguan-card-read).

Source: **Volkswagen's reissued maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/document/<id>`), the ones the
[2023–2026 GTI's page](volkswagen-gti-2023-2026.md) lists. No card from the time was found for these years (NHTSA's
index has none), so the reissues are the only source; the owner's ruling of 2026-10-23 allows that, cited with their
print dates. Read on 2026-10-29, as page images, by eye, and compared with each other.

| Model year | Document | Printed |
|---|---|---|
| 2023 | `USA5R507521`, `…/v1/document/7f3ce02f-ee20-4e97-8747-fb18f14b9b66`; the list's source | 12/03/2025 |
| 2024 | `USA5R505721`, `…/v1/document/2721b3eb-0228-4be4-94d6-0b8e24dbf74c` | 12/03/2025 |

- **The two cards agree for the Tiguan.** 2024 drops the plug-in and hybrid rows from the levels, words the
  automatic row's heading "» USA Only«" where 2023 has "»for non-DSG - USA Only«", and adds a severe-use row for the
  09U gearbox ("09U: Jetta"); none of that touches the Tiguan. Every row that names the Tiguan reads the same in both.
- **The cards are read as the [2023–2026 GTI's list](volkswagen-gti-2023-2026.md) reads them**, and the list is the
  [2023 Jetta](volkswagen-jetta-2023.md)'s, with the model's name and one more additional item (10 columns to
  100,000, not repeated, no months on the columns; the oil reminder is 10,000 or 12 months, #517):
  - **Spark plugs** every 80,000 miles or 6 years, "Atlas, Atlas Sport, Jetta, GLI, Taos, GTI, Tiguan" (2024:
    "Atlas, Jetta, GLI, Taos, GTI, Tiguan"): 80,000 / 72 mo.
  - **The automatic.** "Transmission, Automatic - Change Fluid & filter (if applicable)", every 80,000 miles,
    applicability "09P: Atlas, Tiguan" (the other lines are "09S: Jetta, Taos" and "0GC (DSG): Arteon, GTI, Golf R,
    Jetta, Taos"). The Tiguan is named on one gearbox line only, and vPIC decodes it as "Automatic" (2024:
    `3VV0B7AX*RM`, `3VV2B7AX*RM`), so the row is its: "Change transmission fluid and filter (automatic only)",
    80,000, `trans-fluid`, in `manualExcluded`, as on the 2023 Jetta's list. The 2024 card's 09U severe-use row
    names only the Jetta.
  - **"AWD Clutch - Change fluid"**, every 3 years, "All Applicable Vehicles". The GTI's and Jetta's lists leave it
    out on front-drive cars; the Tiguan was sold with front-wheel drive and with 4Motion (vPIC decodes
    `3VV2B7AX*RM`, 2024, as "AWD/All-Wheel Drive"), so it is read as on the
    [2023–2025 Golf R](volkswagen-golf-r-2023-2025.md): "Change AWD clutch fluid (AWD)", every 36 months by time
    alone, no service line, in `allWheelDriveOnly`. That reading is confirmed by the owner on
    [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part B, 2026-10-29).
  - Air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (USA: 3 years after registration first);
    dust and pollen filter 40,000 / 24 mo.
- **Left out of the additional items**, as on the GTI's and Jetta's lists: the front axle differential lock fluid
  ("All Applicable Vehicles"), the belt starter-generator ribbed belt (110,000, past the grid's end, "All applicable
  engines") and the tire mobility kit bottle, and the inspections: the sunroof drains (20,000 / 2 years, "Tiguan"),
  the camshaft toothed-belt tensioning roller ("… Tiguan", 180,000 or 15 years) and the coolant pump toothed belt.
  Not the Tiguan's: the 40,000-mile spark plugs (Arteon, Golf R) and the Golf R's rear differential. The Canada-only
  tire rotation is left out of the levels.
- **Model name.** vPIC's model lists for Volkswagen, 2023 and 2024 (`GetModelsForMakeYear`, read 2026-10-29), name it
  "Tiguan". One engine, the 2.0T, so the list names none.
- **Not 2025 on**: the third generation, "Tiguan (RM1)" on the 2025 and 2026 reissues, whose gearbox rows differ
  (09H, and a 09U severe-use row every 60,000). It isn't this issue's.
- Reminders: engine oil 10,000 / 12 mo; the additional items above.
