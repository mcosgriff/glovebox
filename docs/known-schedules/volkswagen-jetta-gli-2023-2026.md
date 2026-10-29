# Volkswagen Jetta GLI, 2023–2026 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-jetta-gli-2023-2026.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 Jetta card read, the model names and what isn't covered are on
the [2019 Jetta 1.4T's page](volkswagen-jetta-2019-1.4t.md#every-mk7-jetta-card-read).

Source: **Volkswagen's reissued maintenance cards** for 2023 (`USA5R507521`, 12/03/2025, the list's source), 2024
(`USA5R505721`, 12/03/2025), 2025 (`USA5R701521`, 12/03/2025) and 2026 (`USA5R506821`, 02/2026), from its
owners-literature site, the ones the [2023–2026 GTI's page](volkswagen-gti-2023-2026.md) lists. No card from the time
was found, so the reissues are the only source (the owner's ruling of 2026-10-23), cited with their print dates. Read
on 2026-10-29, as page images, by eye.

- **The cards are read as the 2023–2026 GTI's list reads them**, and the table is that list's, in its wording (10
  columns to 100,000, no months on them; oil 10,000 or 12 months, #517).
- **Spark plugs** every 80,000 miles or 6 years, "GLI" on all four cards ("Jetta, GLI, Taos" in 2023 and 2024; "Jetta",
  "GLI, GTI" in 2025; "GLI" on its own line in 2026): 80,000 / 72 mo.
- **The DSG is left out**, asked on [#442](https://github.com/mcosgriff/glovebox/issues/442#issuecomment-6491794904) (question 2). The cards' automatic row ("Transmission, Automatic - Change Fluid & filter
  (if applicable)", every 80,000 miles) gives the DSG as "0GC (DSG): Arteon, GTI, Golf R, Jetta, Taos" (2023, 2024),
  "GTI, Golf R, Jetta, Taos" (2025), "GTI, Golf R, Jetta" (2026). It names the "Jetta", not the "GLI", while the spark
  plug row on the same cards names the two apart, and vPIC has "Jetta" and "Jetta GLI" apart for these years. The
  2019–2022 cards name the "Jetta GLI" on their DSG row. Whether the reissues' "Jetta" there is the GLI isn't on the
  card, so it isn't taken.
- Left out, as on the GTI's list: the AWD clutch fluid and front axle differential lock fluid ("All Applicable
  Vehicles"; the GLI is front-wheel drive), the belt starter-generator belt (110,000, past the grid's end), the tire
  mobility kit bottle, and the inspections (sunroof drains; camshaft and coolant pump toothed belts, 180,000 or 15
  years). Not the GLI's: the 09P, 09S, 09H and 09U automatics, the 40,000-mile spark plugs and the Golf R's rear
  differential.
- **What differs between the years** touches only other models, the Jetta's automatic rows and the hybrid and
  plug-in rows (as on the GTI's page); every row that names the GLI reads the same.
- **Model name.** vPIC's model lists for Volkswagen (`GetModelsForMakeYear`, read 2026-10-29) have "Jetta GLI" apart
  from "Jetta" for 2023 to 2026. One engine, the 2.0T, so the list names none.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24 months by
  time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24 mo; spark plugs
  80,000 / 72 mo.
