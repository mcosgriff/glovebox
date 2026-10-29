# Volkswagen Arteon, 2023

How `app/src/main/assets/known-schedules/volkswagen-arteon-2023.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Arteon card read, and what isn't covered, is on the
[2019 Arteon's page](volkswagen-arteon-2019.md#every-arteon-card-read).

Source: **Volkswagen's reissued 2023 maintenance card**, `USA5R507521`, printed 12/03/2025, from its owners-literature
site (`https://ownersliterature.vw.com/owners-literature-service/v1/document/7f3ce02f-ee20-4e97-8747-fb18f14b9b66`),
the one the [2023–2026 GTI's page](volkswagen-gti-2023-2026.md) lists. No card from the time was found (NHTSA's index
has none), so the reissue is the only source; the owner's ruling of 2026-10-23 allows that, cited with its print
date. Read on 2026-10-29, as page images, by eye.

- **The card is read as the [2023–2025 Golf R's list](volkswagen-golf-r-2023-2025.md) reads it**, and the list is
  that one's without the Golf R's own rear differential ("Rear Axle Differential with Torque Vectoring", "Golf R"):
  10 columns to 100,000, not repeated, no months on the columns; the oil reminder is 10,000 or 12 months (#517). The
  levels mark every row "All Vehicles", "All Applicable Vehicles" or a plug-in, hybrid or electric model, so the
  Arteon's table is the GTI's and Golf R's.
- **Additional items that name the Arteon:**
  - **Spark plugs** every 40,000 miles or 4 years, "Arteon, Golf R": 40,000 / 48 mo.
  - **The DSG.** "Transmission, Automatic - Change Fluid & filter (if applicable)", every 80,000 miles, applicability
    "0GC (DSG): Arteon, GTI, Golf R, Jetta, Taos" (the other lines are "09P: Atlas, Tiguan" and "09S: Jetta, Taos").
    The Arteon is named on the DSG line only, and vPIC decodes `WVWTR7AN*PE` (2023) as a 7-speed "Automatic", so the
    row is its: "Change DSG transmission fluid and filter (DSG only)", 80,000, `trans-fluid`, in `manualExcluded`, as
    on the Golf R's and GTI's lists.
  - Not reminders, as on the Golf R's list: the sunroof drains check ("Arteon, Atlas", 20,000 or 2 years) and the
    camshaft toothed-belt tensioning roller check ("Arteon, GTI, Golf R, Jetta, Taos, Tiguan", 180,000 or 15 years),
    both inspections (rule 7).
- **"AWD Clutch - Change fluid"**, every 3 years, "All Applicable Vehicles": read as on the Golf R's and the
  [2023–2024 Tiguan](volkswagen-tiguan-2023-2024.md)'s lists, "Change AWD clutch fluid (AWD)", every 36 months by
  time alone, no service line, in `allWheelDriveOnly`. vPIC decodes `WVWTR7AN*PE` as "AWD/All-Wheel Drive"; that
  reading waits on [#437's question 5](https://github.com/mcosgriff/glovebox/issues/437#issuecomment-6491507764).
- **Left out of the additional items**, as on the Golf R's list: the front axle differential lock fluid, the belt
  starter-generator ribbed belt (110,000, past the grid's end) and the tire mobility kit bottle ("All Applicable"),
  and the coolant pump toothed belt check. The Canada-only tire rotation is left out of the levels.
- **Model name.** vPIC's model list for Volkswagen, 2023 (`GetModelsForMakeYear`, read 2026-10-29), names it
  "Arteon". One engine, the 2.0T, so the list names none.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (USA: 3 years after registration first); dust and pollen
  filter 40,000 / 24 mo; spark plugs 40,000 / 48 mo; DSG fluid and filter 80,000 (not for a manual car).
