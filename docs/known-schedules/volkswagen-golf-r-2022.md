# Volkswagen Golf R, 2022 (Mk8)

How `app/src/main/assets/known-schedules/volkswagen-golf-r-2022.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 and Mk8 Golf R card read, and what isn't covered, is on the
[2015 Golf R's page](volkswagen-golf-r-2015.md).

Source: Volkswagen's "2022 VW Maintenance Schedule - All Model Gasoline Vehicles", USA, filed with NHTSA as a
manufacturer communication: the newest revision, revised 7/21/2021, `2021/MC-10199495-0001`, read in full on
2026-10-29, as page images, by eye.

| Document | What was read |
|---|---|
| `2021/MC-10199495-0001` (revised 7/21/2021) | In full; the list was written from it |
| Earlier revisions (`MC-10190513`, `MC-10190515`, `MC-10190996`, April 2021) | Not read again. The [2022 GTI's page](volkswagen-gti-2022.md) compared them: they move the Golf and then the GTI between the spark plug rows and drop a duplicate suspension row. Neither change touches a Golf R row |
| Volkswagen's reissue of the 2022 card, `USA5R507921`, printed 12/03/2025 | Not used: the card from the time wins (the owner's ruling of 2026-10-23). The 2022 GTI's page says how it differs; among other rows it adds the Golf R's torque-vectoring rear differential (see the [2023–2025 list](volkswagen-golf-r-2023-2025.md)), which the 2022 card doesn't have |

- **The card is read as the [2022 GTI's list](volkswagen-gti-2022.md) reads it**, and the rows are that list's, in
  its wording: the three service levels name no model, so the Golf R's table is the GTI's. The torque splitter oil
  check, "(if equipped)", stays "(if fitted)", table-only, as there. The differences are all additional items:
  - **Spark plugs every 40,000 miles or 4 years** (40,000 / 48 months): "Only: Arteon, Golf R, Passat".
  - **The 7-speed DSG**, "Every 80,000 miles. Only: Golf R, Golf GTI, Jetta GLI": "Change DSG transmission fluid
    (DSG only)", 80,000, in `manualExcluded`, as on the GTI's list. "Transmission, automatic: Change fluid
    (excluding Canada) (only non-DSG Automatic Transmission)" names no model and is left out.
  - **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years". The GTI's list leaves it out
    as "(if equipped)" on a front-drive car; for the Golf R, sold with 4Motion only, it is read as on the 2018 and
    2019 lists and the [2019 Golf Alltrack](volkswagen-golf-alltrack-2019.md) (whose card prints the same row):
    "Change AWD clutch fluid (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`, never on
    the grid (PR #537).
- Left out, as on the 2022 GTI's list: the front axle differential lock fluid ("(if equipped)", no table to stand
  in), and the panorama sunroof noise and function test and the sunroof drains check (inspections, rule 7).
- **No months on the columns**: the footnotes count each service from the last. The oil reminder is 10,000 or 12
  months, from the minor service's "or 1 year" (#517).
- **Model name.** vPIC's model list for Volkswagen, 2022 (`GetModelsForMakeYear`), names it "Golf R". One engine,
  the 2.0T, and no row names an engine, so the list names none.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36
  months by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name);
  dust and pollen filter 40,000 / 24 mo; spark plugs 40,000 / 48 mo; 7-speed DSG fluid 80,000 (not for a manual).
