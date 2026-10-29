# Volkswagen Jetta, 2023 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2023.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 Jetta card read, the model names and what isn't covered are on
the [2019 Jetta 1.4T's page](volkswagen-jetta-2019-1.4t.md#every-mk7-jetta-card-read).

Source: **Volkswagen's reissued 2023 maintenance card**, `USA5R507521`, printed 12/03/2025, from its
owners-literature site (`https://ownersliterature.vw.com/owners-literature-service/v1/document/7f3ce02f-ee20-4e97-8747-fb18f14b9b66`),
the one the [2023–2026 GTI's page](volkswagen-gti-2023-2026.md) lists. No card from the time was found (NHTSA's index
has none), so the reissue is the only source; the owner's ruling of 2026-10-23 allows that, cited with its print date.
Read on 2026-10-29, as page images, by eye.

- **The card is read as the [2023–2026 GTI's list](volkswagen-gti-2023-2026.md) reads it**, and the table is that
  list's, in its wording: the three service levels mark every row "All Vehicles", "All Applicable Vehicles" or a
  plug-in, hybrid or electric model (10 columns to 100,000, not repeated, no months on the columns; the oil reminder
  is 10,000 or 12 months, #517).
- **Additional items for the Jetta** (reminders in `additional`, never placed on the grid):
  - **Spark plugs** every 80,000 miles or 6 years, "Atlas, Atlas Sport, Jetta, GLI, Taos, GTI, Tiguan": 80,000 / 72 mo.
  - **The automatic.** "Transmission, Automatic - Change Fluid & filter (if applicable) »for non-DSG - USA Only«",
    every 80,000 miles, applicability "09P: Atlas, Tiguan", "09S: Jetta, Taos", "0GC (DSG): Arteon, GTI, Golf R,
    Jetta, Taos". The Jetta is on both the 09S and the 0GC line of the one row, with the one interval, so any Jetta
    that shifts itself has the job whichever gearbox it has: "Change transmission fluid and filter (automatic only)",
    80,000, `trans-fluid`, in `manualExcluded`. This is why 2023 has a list of its own: from 2024 the Jetta is also
    on a 09U row with other figures ([2024–2026](volkswagen-jetta-2024-2026.md)).
  - Air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (USA: 3 years after registration first);
    dust and pollen filter 40,000 / 24 mo.
- **Left out of the additional items**, as on the GTI's list: the AWD clutch fluid and the front axle differential
  lock fluid ("All Applicable Vehicles"; the Mk7 Jetta was sold with front-wheel drive only), the belt
  starter-generator ribbed belt (110,000, past the grid's end, "All applicable engines") and the tire mobility kit
  bottle, and the inspections: the sunroof drains (20,000 / 2 years, "Jetta"), the camshaft toothed-belt tensioning
  roller ("… Jetta …") and the coolant pump toothed belt (180,000 or 15 years). Not the Jetta's: the 40,000-mile spark
  plugs (Arteon, Golf R) and the Golf R's rear differential. The Canada-only tire rotation is left out of the levels.
- **Model name.** vPIC's model list for Volkswagen, 2023 (`GetModelsForMakeYear`, read 2026-10-29), has "Jetta" and
  "Jetta GLI" apart ([the GLI's list](volkswagen-jetta-gli-2023-2026.md)). The card names the Jetta with no engine,
  so the list names none.
- Reminders: engine oil 10,000 / 12 mo; the additional items above.
