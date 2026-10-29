# Volkswagen Golf R, 2023–2025 (Mk8)

How `app/src/main/assets/known-schedules/volkswagen-golf-r-2023-2025.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 and Mk8 Golf R card read, and what isn't covered, is on the
[2015 Golf R's page](volkswagen-golf-r-2015.md).

Source: **Volkswagen's reissued maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/document/<id>`), the ones the
[2023–2026 GTI's page](volkswagen-gti-2023-2026.md) lists. No card from the time was found for these years (NHTSA's
index has none), so the reissues are the only source; the owner's ruling of 2026-10-23 allows that, cited as
reissues with their print dates. Read on 2026-10-29, as page images, by eye (2023), and compared by script, every
difference looked at on the page (2024, 2025).

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2023 | `USA5R507521`, `…/v1/document/7f3ce02f-ee20-4e97-8747-fb18f14b9b66` | 12/03/2025 | In full; the list was written from it |
| 2024 | `USA5R505721`, `…/v1/document/2721b3eb-0228-4be4-94d6-0b8e24dbf74c` | 12/03/2025 | Compared |
| 2025 | `USA5R701521`, `…/v1/document/760f5f9a-0290-4dfe-adf0-30d03981a9ad` | 12/03/2025 | Compared |
| 2026 | `USA5R506821`, `…/v1/document/d063309f-f793-4d23-8799-a4ff5ffb3850` | 02/2026 | Compared; it differs for the Golf R, so it has [a list of its own](volkswagen-golf-r-2026.md) |

- **The cards are read as the 2023–2026 GTI's list reads them**, and the rows are that list's, in its wording: the
  three service levels mark every row "All Vehicles", "All Applicable Vehicles" or a plug-in, hybrid or electric
  model, so the Golf R's table is the GTI's (10 columns to 100,000, not repeated). The torque splitter oil check,
  "(if equipped)", "All Applicable Vehicles", stays "(if fitted)", table-only.
- **What differs between 2023, 2024 and 2025** touches only other models and the plug-in and hybrid rows, as on
  the GTI's page. Every row that names the Golf R reads the same in all three.
- **Additional items for the Golf R** (reminders in `additional`, never placed on the grid):
  - **Spark plugs** every 40,000 miles or 4 years, "Arteon, Golf R" (2025: "Golf R"): 40,000 / 48 mo.
  - **"Rear Axle Differential with Torque Vectoring - Change fluid"**, every 40,000 miles or 4 years, "Golf R":
    "Change rear differential fluid", `rear-diff`, 40,000 / 48 mo. It is the Golf R's own row, so it isn't in
    `allWheelDriveOnly`.
  - **The DSG.** "Transmission, Automatic - Change Fluid & filter (if applicable)", every 80,000 miles, whose
    applicability includes "0GC (DSG): … GTI, Golf R …": "Change DSG transmission fluid and filter (DSG only)",
    80,000, in `manualExcluded`, as the GTI's list reads the row.
  - **"AWD Clutch - Change fluid"**, every 3 years, "All Applicable Vehicles". The GTI's list leaves it out as a row
    for no named model; the Golf R was sold with 4Motion only, so it is read as on the
    [2022 Golf R](volkswagen-golf-r-2022.md): "Change AWD clutch fluid (AWD)", every 36 months by time alone, no
    service line, in `allWheelDriveOnly`.
  - Air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (USA: 3 years after registration first);
    dust and pollen filter 40,000 / 24 mo.
- **Left out of the additional items**, as on the GTI's list: the front axle differential lock fluid, the belt
  starter-generator ribbed belt (110,000, past the grid's end) and the tire mobility kit bottle ("All Applicable"),
  and the inspections: the sunroof drains ("GTI / Golf R", 20,000 / 2 years), the camshaft toothed-belt tensioning
  roller and the coolant pump toothed belt (180,000 or 15 years). The Canada-only tire rotation is left out of the
  levels.
- **No months on the columns.** The oil reminder is 10,000 or 12 months, from the minor service's "or 1 year"
  (#517).
- **Model name.** vPIC's model lists for Volkswagen (`GetModelsForMakeYear`, read 2026-10-29) name it "Golf R" for
  2023, 2024 and 2025. One engine, the 2.0T, so the list names none.
- Reminders: engine oil 10,000 / 12 mo; the additional items above.
