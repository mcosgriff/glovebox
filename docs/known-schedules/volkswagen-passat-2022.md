# Volkswagen Passat, 2022 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2022.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). How the 2020–2022 cards are read is on the
[2020–2021 list's page](volkswagen-passat-2020-2021.md#how-the-20202022-all-model-cards-are-read). Every
Passat NMS card read, and what isn't covered, is on the [2014 2.5L's page](volkswagen-passat-2014-2.5.md).

Source: Volkswagen's "2022 VW Maintenance Schedule - All Model Gasoline Vehicles", USA, filed with NHTSA as a
manufacturer communication: the newest revision, revised 7/21/2021, `2021/MC-10199495-0001`, read in full on
2026-10-29, as page images, by eye.

| Document | What was read |
|---|---|
| `2021/MC-10199495-0001` (revised 7/21/2021) | In full; the list was written from it |
| `2021/MC-10190513-0001` (4/9/2021), `MC-10190515-0001` (4/13/2021), `MC-10190996-0001` (4/21/2021) | Earlier revisions, not read again. The [2022 GTI's page](volkswagen-gti-2022.md) compared them: they differ in the spark plug rows' model names (the Golf and then the GTI moved to 80,000 / 6 years) and a duplicate suspension row in the Standard service. The change notes in NHTSA's index, as far as they go, name no Passat row |
| Volkswagen's reissue of the 2022 card, `USA5R507921`, dated 12/03/2025 | Not used: the card from the time wins (the owner's ruling of 2026-10-23). The 2022 GTI's page says how it differs (its grid ends at 100,000; belt starter-generator belt, toothed belt and tire mobility kit items) |

- **The card is read as the [2022 GTI's list](volkswagen-gti-2022.md) reads it**, and the rows are that list's,
  in its wording, with these differences:
  - **Spark plugs every 40,000 miles or 4 years** (40,000 / 48 months): "Only: Arteon, Golf R, Passat".
  - **No DSG row, and both gearbox rows are left out.** The 7-speed DSG row is "Only: Golf R, Golf GTI, Jetta
    GLI"; "Transmission, automatic: Change fluid (excluding Canada) (only non-DSG Automatic Transmission)",
    every 80,000 miles, names no model or engine
    ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514), asked
    again on #445).
- **No months on the columns**, as on the 2022 GTI's list: the footnotes count each service from the last.
  The oil reminder is 10,000 or 12 months, from the minor service's "or 1 year" (#517).
- Kept for every model, as on the GTI's list: the trailer hitch check and the torque splitter oil check, both
  "(if equipped)" with no model named, "(if fitted)", table-only.
- Left out, as on the 2022 GTI's list: the AWD clutch fluid and the front axle differential lock fluid ("(if
  equipped)", no table to stand in; the 2022 Passat was sold with front-wheel drive only), and the panorama
  sunroof noise and function test and the sunroof drains check (inspections, rule 7).
- **Model name.** vPIC's model list for Volkswagen, 2022 (`GetModelsForMakeYear`, read 2026-10-29), names it
  "Passat". One engine, the 2.0T, and no row names an engine, so the list names none.
- **Why not one list with 2020–2021**: the 2022 card adds the belt starter-generator check and the torque
  splitter check, the brake fluid level check "depending on brake wear", the trailer hitch for every model and
  the panorama sunroof test, and the lists follow different GTI readings (months on the columns, the drains
  and differential lock rows).
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24
  months by time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24
  mo; spark plugs 40,000 / 48 mo.
