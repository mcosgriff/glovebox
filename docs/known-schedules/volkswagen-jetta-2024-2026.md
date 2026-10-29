# Volkswagen Jetta, 2024–2026 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2024-2026.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 Jetta card read, the model names and what isn't covered are on
the [2019 Jetta 1.4T's page](volkswagen-jetta-2019-1.4t.md#every-mk7-jetta-card-read).

Source: **Volkswagen's reissued maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/document/<id>`), the ones the
[2023–2026 GTI's page](volkswagen-gti-2023-2026.md) lists. No card from the time was found for these years, so the
reissues are the only source (the owner's ruling of 2026-10-23), cited with their print dates. Read on 2026-10-29, as
page images, by eye, and compared with each other and the 2023 card.

| Model year | Document | Printed |
|---|---|---|
| 2024 | `USA5R505721`, `…/v1/document/2721b3eb-0228-4be4-94d6-0b8e24dbf74c`; the list's source | 12/03/2025 |
| 2025 | `USA5R701521`, `…/v1/document/760f5f9a-0290-4dfe-adf0-30d03981a9ad` | 12/03/2025 |
| 2026 | `USA5R506821`, `…/v1/document/d063309f-f793-4d23-8799-a4ff5ffb3850` | 02/2026 |

- **The list is the [2023 Jetta's](volkswagen-jetta-2023.md) without its gearbox reminder**: the three service levels
  and every additional item that names the Jetta read the same in all three years and in 2023 (spark plugs 80,000 or 6
  years, "Jetta"; sunroof drains and the camshaft toothed-belt roller, inspections, "Jetta"), but for the automatic.
- **The automatic is left out**, asked on #442. Each card puts the Jetta on two automatic rows that differ:
  - 2024: "Transmission, Automatic - Change Fluid & filter (if applicable) » USA Only«", every 80,000 miles, "09S:
    Jetta, Taos" and "0GC (DSG): Arteon, GTI, Golf R, Jetta, Taos"; and a second row, "Every 80,000 miles (120K km) -
    Severe driving conditions only (i.e. taxi/ towing, etc.)", "09U: Jetta".
  - 2025 and 2026: the 80,000-mile row is "09H: Atlas, Tiguan (RM1)" and "0GC (DSG): GTI, Golf R, Jetta, Taos" (2026:
    "GTI, Golf R, Jetta"), with no 09S; the other is "Every 60,000 miles (90K km) - If driven under Severe Operation
    (i.e. taxi/ towing, etc.)", "09U: Jetta, Taos, Tiguan (RM1)".
  A Jetta's gearbox code isn't on record and the format can't tell a 09U from a 09S or a DSG, so a reminder from
  either row could be the wrong one for the car (the shape of
  [#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333)).
- Left out, as on the 2023 list: the AWD clutch fluid and front axle differential lock fluid ("All Applicable
  Vehicles"; front-wheel drive only), the belt starter-generator belt (110,000, past the grid's end), the tire
  mobility kit bottle, and the inspections. 2024 also prints hybrid and plug-in rows in the levels; 2025 and 2026 are
  headed "Gas Vehicles". Neither touches the Jetta.
- **Model name.** vPIC's model lists for Volkswagen (`GetModelsForMakeYear`, read 2026-10-29) have "Jetta" and "Jetta
  GLI" apart for 2024, 2025 and 2026. No row names a Jetta engine, so the list names none.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24 months by
  time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24 mo; spark plugs
  80,000 / 72 mo.
