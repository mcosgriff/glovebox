# Volkswagen Taos, 2025–2026

How `app/src/main/assets/known-schedules/volkswagen-taos-2025-2026.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Taos card read, and what isn't covered, is on the
[2022 Taos's page](volkswagen-taos-2022.md#every-taos-card-read).

Source: **Volkswagen's reissued maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/document/<id>`), the ones the
[2023–2026 GTI's page](volkswagen-gti-2023-2026.md) lists. No card from the time was found for these years (NHTSA's
index has none), so the reissues are the only source; the owner's ruling of 2026-10-23 allows that, cited with their
print dates. Read on 2026-10-29, as page images, by eye, and compared with each other and with 2023 and 2024.

| Model year | Document | Printed |
|---|---|---|
| 2025 | `USA5R701521`, `…/v1/document/760f5f9a-0290-4dfe-adf0-30d03981a9ad`; the list's source | 12/03/2025 |
| 2026 | `USA5R506821`, `…/v1/document/d063309f-f793-4d23-8799-a4ff5ffb3850` | 02/2026 |

- **The list is the [2023–2024 Taos](volkswagen-taos-2023-2024.md)'s without its gearbox reminder.** The levels
  ("Gas Vehicles") read as in 2023 and 2024, as the [2023–2026 Atlas's page](volkswagen-atlas-2023-2026.md) found, and
  every additional item that names the Taos reads the same, but for the automatic:
  - **Spark plugs** every 80,000 miles or 6 years, "Atlas, Atlas Cross Sport, Jetta, GLI, GTI, Taos, Tiguan (RM1)"
    (2026 lists the same models): 80,000 / 72 mo.
  - **"AWD Clutch - Change fluid"**, every 3 years, "All Applicable Vehicles": "Change AWD clutch fluid (AWD)", every
    36 months by time alone, in `allWheelDriveOnly`. NHTSA's 2025 safety-rating list has "Taos SUV AWD" and "… FWD".
    That reading waits on [#437's question 5](https://github.com/mcosgriff/glovebox/issues/437#issuecomment-6491507764).
  - Not reminders, as before: the sunroof drains check ("Taos") and the camshaft toothed-belt tensioning roller check
    ("GTI, Golf R, Jetta, Taos, Tiguan (RM1)").
- **The automatic is left out**, asked on [#455](https://github.com/mcosgriff/glovebox/issues/455) (questions 1 and 2):
  - 2025: "Transmission, Automatic - Change Fluid & filter (if applicable) » USA Only«", every 80,000 miles, "09H:
    Atlas, Tiguan (RM1)" and "0GC (DSG): GTI, Golf R, Jetta ,Taos"; and "Every 60,000 miles (90K km) - If driven
    under Severe Operation (i.e. taxi/ towing, etc.)", "09U: Jetta, Taos, Tiguan (RM1)". The Taos is on two rows
    that differ, and its gearbox code isn't on record, so a reminder from either could be the wrong one for the car
    (the shape of [#442's question 1](https://github.com/mcosgriff/glovebox/issues/442#issuecomment-6491794904) and
    [#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333)).
  - 2026: the DSG line is "0GC (DSG): GTI, Golf R, Jetta", without the Taos, which is left only on the 09U row, every
    60,000 miles "If driven under Severe Operation". A severe-use-only gearbox job among the additional items has no
    ruling, so it isn't made a reminder.
  With the gearbox row out, 2025 and 2026 agree for the Taos, so they are one list.
- **Left out of the additional items**, as before: the front axle differential lock fluid, the belt starter-generator
  ribbed belt (110,000, past the grid's end), the tire mobility kit bottle and the coolant pump toothed belt check.
  Not the Taos's: the Golf R's spark plugs and rear differential. The Canada-only tire rotation is left out.
- **Model name.** vPIC's model lists for Volkswagen, 2025 and 2026 (`GetModelsForMakeYear`, read 2026-10-29), name it
  "Taos".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (USA: 3 years after registration first); dust and pollen
  filter 40,000 / 24 mo; spark plugs 80,000 / 72 mo.
