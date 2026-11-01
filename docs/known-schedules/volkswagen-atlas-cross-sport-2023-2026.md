# Volkswagen Atlas Cross Sport, 2023–2026

How `app/src/main/assets/known-schedules/volkswagen-atlas-cross-sport-2023-2026.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md). Every Atlas Cross Sport card read, and what isn't covered, is on
the [2020–2021 page](volkswagen-atlas-cross-sport-2020-2021.md#every-atlas-cross-sport-card-read).

Source: **Volkswagen's reissued maintenance cards**, from its owners-literature site, the ones the
[2023–2026 Atlas's page](volkswagen-atlas-2023-2026.md) lists: 2023 `USA5R507521` (12/03/2025, the list's source),
2024 `USA5R505721` (12/03/2025), 2025 `USA5R701521` (12/03/2025) and 2026 `USA5R506821` (02/2026). No card from
the time was found, so the reissues are the only source (the owner's ruling of 2026-10-23), cited with their print
dates. Read on 2026-10-29, as page images, by eye.

- **The list is the [2023–2026 Atlas](volkswagen-atlas-2023-2026.md)'s, row for row**, with the model's name. The
  cards' levels name no model. Of their additional items, by year:
  - **2023 and 2024.** Neither card names the Atlas Cross Sport: the 2023 card's spark plug row says "Atlas, Atlas
    Sport", the 2024 card's "Atlas", and the automatic "09P: Atlas, Tiguan" on both. By the owner's ruling on
    [#555](https://github.com/mcosgriff/glovebox/issues/555#issuecomment-6511795335) (part A, the Atlas Cross Sport's question 1), "Atlas Sport" (2023) and "Atlas" (2024) are read as
    covering it, and the list matches the Atlas's.
  - **2025.** Spark plugs every 80,000 miles or 6 years name "Atlas, Atlas Cross Sport, …". The automatic's line,
    "09H: Atlas, Tiguan (RM1)", doesn't; by the same ruling (question 2) the 2026 card's line is taken back to 2025.
  - **2026.** Spark plugs as 2025; the automatic "09H: Atlas, Atlas Cross Sport, Tiguan (RM1)".
  So every year reads as the Atlas's: **spark plugs 80,000 / 72 months** (no engine is named, so the list names
  none) and **"Change transmission fluid and filter (automatic only)"**, every 80,000 miles, `trans-fluid`, in
  `manualExcluded`. One list covers the four years; it takes the place of the 2025 and 2026 lists. The 09U
  severe-use row doesn't name it.
- **"AWD Clutch - Change fluid"**, "All Applicable Vehicles": "Change AWD clutch fluid (AWD)", every 36 months by time
  alone, in `allWheelDriveOnly`, as on the Atlas's list. NHTSA's safety-rating list for 2025
  (`api.nhtsa.gov/SafetyRatings`, read 2026-10-29) has "2025 Volkswagen Atlas Cross Sport SUV AWD" and "… FWD". The
  reading is confirmed by the owner on [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part B, 2026-10-29).
- Left out as on the Atlas's list: the front axle differential lock fluid, the belt starter-generator belt, the tire
  mobility kit bottle, the inspections and the Canada-only tire rotation.
- vPIC's model lists for Volkswagen, 2023 to 2026 (`GetModelsForMakeYear`, read 2026-10-29), name it "Atlas Cross
  Sport" apart from "Atlas".
- Reminders: engine oil 10,000 / 12 mo; air filter 60,000 / 72 mo; AWD clutch fluid every 36 months by time alone;
  brake fluid every 24 months by time alone (USA: 3 years after registration first); dust and pollen filter
  40,000 / 24 mo; spark plugs 80,000 / 72 mo; automatic transmission fluid and filter 80,000 (not for a manual).
