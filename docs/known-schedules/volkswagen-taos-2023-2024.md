# Volkswagen Taos, 2023–2024

How `app/src/main/assets/known-schedules/volkswagen-taos-2023-2024.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Taos card read, and what isn't covered, is on the
[2022 Taos's page](volkswagen-taos-2022.md#every-taos-card-read).

Source: **Volkswagen's reissued maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/document/<id>`), the ones the
[2023–2026 GTI's page](volkswagen-gti-2023-2026.md) lists. No card from the time was found for these years (NHTSA's
index has none), so the reissues are the only source; the owner's ruling of 2026-10-23 allows that, cited with their
print dates. Read on 2026-10-29, as page images, by eye, and compared with each other.

| Model year | Document | Printed |
|---|---|---|
| 2023 | `USA5R507521`, `…/v1/document/7f3ce02f-ee20-4e97-8747-fb18f14b9b66`; the list's source | 12/03/2025 |
| 2024 | `USA5R505721`, `…/v1/document/2721b3eb-0228-4be4-94d6-0b8e24dbf74c` | 12/03/2025 |

- **The two cards agree for the Taos**, as they do for the [2023–2024 Tiguan](volkswagen-tiguan-2023-2024.md): 2024
  drops the plug-in and hybrid rows, words the automatic row's heading "» USA Only«" where 2023 has "»for non-DSG -
  USA Only«", and adds a severe-use row for the 09U gearbox ("09U: Jetta"), which doesn't name the Taos.
- **The list is the 2023–2024 Tiguan's, row for row**, with the model's name (10 columns to 100,000, not repeated, no
  months on the columns; the oil reminder is 10,000 or 12 months, #517). The levels mark every row "All Vehicles",
  "All Applicable Vehicles" or a plug-in, hybrid or electric model. The additional items that name the Taos:
  - **Spark plugs** every 80,000 miles or 6 years, "Atlas, Atlas Sport, Jetta, GLI, Taos, GTI, Tiguan" (2024:
    "Atlas, Jetta, GLI, Taos, GTI, Tiguan"): 80,000 / 72 mo.
  - **The automatic.** "Transmission, Automatic - Change Fluid & filter (if applicable)", every 80,000 miles,
    applicability "09P: Atlas, Tiguan", "09S: Jetta, Taos", "0GC (DSG): Arteon, GTI, Golf R, Jetta ,Taos". The Taos
    is on both the 09S and the DSG line of the one row, with the one interval, so a Taos has the job whichever of the
    two it has (front-wheel drive with the 8-speed automatic, or 4Motion with the 7-speed DSG): read as on the
    [2023 Jetta's list](volkswagen-jetta-2023.md), "Change transmission fluid and filter (automatic only)", 80,000,
    `trans-fluid`, in `manualExcluded`. Nothing else on the card gives the Taos a gearbox job.
  - Not reminders (inspections, rule 7): the sunroof drains check ("Taos", 20,000 or 2 years) and the camshaft
    toothed-belt tensioning roller check ("Arteon, GTI, Golf R, Jetta, Taos, Tiguan", 180,000 or 15 years).
- **"AWD Clutch - Change fluid"**, every 3 years, "All Applicable Vehicles": "Change AWD clutch fluid (AWD)", every 36
  months by time alone, no service line, in `allWheelDriveOnly`, as on the Tiguan's list. NHTSA's 2024 safety-rating
  list has "Taos SUV AWD" and "… FWD". That reading waits on
  [#437's question 5](https://github.com/mcosgriff/glovebox/issues/437#issuecomment-6491507764).
- **Left out of the additional items**, as on the Tiguan's list: the front axle differential lock fluid ("All
  Applicable Vehicles"), the belt starter-generator ribbed belt (110,000, past the grid's end), the tire mobility kit
  bottle and the coolant pump toothed belt check. Not the Taos's: the 40,000-mile spark plugs (Arteon, Golf R) and
  the Golf R's rear differential. The Canada-only tire rotation is left out of the levels.
- **Model name.** vPIC's model lists for Volkswagen, 2023 and 2024 (`GetModelsForMakeYear`, read 2026-10-29), name it
  "Taos".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (USA: 3 years after registration first); dust and pollen
  filter 40,000 / 24 mo; spark plugs 80,000 / 72 mo; transmission fluid and filter 80,000 (not for a manual car).
