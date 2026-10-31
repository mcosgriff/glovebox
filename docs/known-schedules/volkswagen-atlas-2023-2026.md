# Volkswagen Atlas, 2023–2026

How `app/src/main/assets/known-schedules/volkswagen-atlas-2023-2026.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Atlas card read, and what isn't covered, is on the
[2018 2.0T's page](volkswagen-atlas-2018-2.0t.md#every-atlas-card-read).

Source: **Volkswagen's reissued maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/document/<id>`), the ones the
[2023–2026 GTI's page](volkswagen-gti-2023-2026.md) lists. No card from the time was found for these years (NHTSA's
index has none), so the reissues are the only source; the owner's ruling of 2026-10-23 allows that, cited with their
print dates. Read on 2026-10-29, as page images, by eye, and compared with each other (the levels by script, every
difference looked at on the page).

| Model year | Document | Printed |
|---|---|---|
| 2023 | `USA5R507521`, `…/v1/document/7f3ce02f-ee20-4e97-8747-fb18f14b9b66`; the list's source | 12/03/2025 |
| 2024 | `USA5R505721`, `…/v1/document/2721b3eb-0228-4be4-94d6-0b8e24dbf74c` | 12/03/2025 |
| 2025 | `USA5R701521`, `…/v1/document/760f5f9a-0290-4dfe-adf0-30d03981a9ad` | 12/03/2025 |
| 2026 | `USA5R506821`, `…/v1/document/d063309f-f793-4d23-8799-a4ff5ffb3850` | 02/2026 |

- **The four cards agree for the Atlas.** The levels' jobs are the same in all four (2024 drops the plug-in and hybrid
  rows; 2025 and 2026 are "Gas Vehicles" and move the Canada-only tire rotation), as the GTI's page found. Of the
  additional items that name models, the Atlas is on:
  - **Spark plugs every 80,000 miles or 6 years**: "Atlas, Atlas Sport, Jetta, GLI, Taos, GTI, Tiguan" (2023),
    "Atlas, Jetta, GLI, Taos, GTI, Tiguan" (2024), "Atlas, Atlas Cross Sport, Jetta, GLI, GTI, Taos, Tiguan (RM1)"
    (2025 and 2026): 80,000 / 72 mo. No engine is named, so the list names none.
  - **The automatic.** "Transmission, Automatic - Change Fluid & filter (if applicable)", every 80,000 miles, "09P:
    Atlas, Tiguan" (2023, 2024), "09H: Atlas, Tiguan (RM1)" (2025), "09H: Atlas, Atlas Cross Sport, Tiguan (RM1)"
    (2026). The Atlas is named on one gearbox line in each year, at the same mileage, so the row is its:
    "Change transmission fluid and filter (automatic only)", 80,000, `trans-fluid`, in `manualExcluded`, as on the
    [2023–2024 Tiguan](volkswagen-tiguan-2023-2024.md) (vPIC decodes the 2018 Atlas as an 8-speed "Automatic").
    No severe-use row (09U) names the Atlas.
  - The sunroof drains check (20,000 / 2 years, "Arteon, Atlas" or "Atlas"): an inspection, left out as on the
    Tiguan's list (rule 7).
- **So the list is the 2023–2024 Tiguan's, row for row**, with the model's name and the years 2023 to 2026: 10
  columns to 100,000, not repeated, no months on the columns; the oil reminder is 10,000 or 12 months (#517).
- **"AWD Clutch - Change fluid"**, every 3 years, "All Applicable Vehicles": read as on the Tiguan's list, "Change AWD
  clutch fluid (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`. The Atlas was sold with
  front-wheel drive and with 4Motion (NHTSA's 2025 safety-rating list: "Atlas SUV AWD" and "… FWD"). That reading
  is confirmed by the owner on [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part B, 2026-10-29).
- **Left out of the additional items**, as on the Tiguan's list: the front axle differential lock fluid ("All
  Applicable Vehicles"), the belt starter-generator ribbed belt (110,000, past the grid's end), the tire mobility kit
  bottle and the toothed-belt checks. The Canada-only tire rotation is left out of the levels.
- **Model name.** vPIC's model lists for Volkswagen, 2023 to 2026 (`GetModelsForMakeYear`, read 2026-10-29), name it
  "Atlas".
- Reminders: engine oil 10,000 / 12 mo; air filter 60,000 / 72 mo; AWD clutch fluid every 36 months by time alone;
  brake fluid every 24 months by time alone (USA: 3 years after registration first); dust and pollen filter
  40,000 / 24 mo; spark plugs 80,000 / 72 mo; automatic transmission fluid and filter 80,000 (not for a manual).
