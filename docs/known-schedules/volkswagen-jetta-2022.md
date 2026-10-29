# Volkswagen Jetta, 2022 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2022.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 Jetta card read, the model names and what isn't covered are on
the [2019 Jetta 1.4T's page](volkswagen-jetta-2019-1.4t.md#every-mk7-jetta-card-read).

Source: Volkswagen's "2022 VW Maintenance Schedule - All Model Gasoline Vehicles", USA, filed with NHTSA as a
manufacturer communication: the newest revision, revised 7/21/2021, `2021/MC-10199495-0001`, read in full on
2026-10-29, as page images, by eye. The earlier revisions (April 2021) weren't read again; the
[2022 GTI's page](volkswagen-gti-2022.md) says how they differ (the Golf and then the GTI move between the spark plug
rows). Volkswagen's reissue of the card (`USA5R507921`, 12/03/2025) isn't used: the card from the time wins.

- **The card is read as the [2022 GTI's list](volkswagen-gti-2022.md) reads it**, and the rows are that list's, in its
  wording (the [2022 Passat's](volkswagen-passat-2022.md) list is the same table), with these differences:
  - **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months): "Only: Atlas, Atlas Cross Sport, Golf GTI,
    Jetta, Jetta GLI, Taos, Tiguan".
  - **No gearbox row.** The 7-speed DSG row is "Only: Golf R, Golf GTI, Jetta GLI"; "Transmission, automatic: Change
    fluid (excluding Canada) (only non-DSG Automatic Transmission)", every 80,000 miles, names no model or engine
    ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514), asked again on
    #445).
- **No months on the columns**, as on the 2022 GTI's list. The oil reminder is 10,000 or 12 months, from the minor
  service's "or 1 year" (#517).
- Kept for every model, as on the GTI's list: the trailer hitch check and the torque splitter oil check, "(if
  fitted)", table-only. Left out, as there: the AWD clutch fluid and the front axle differential lock fluid ("(if
  equipped)", no table to stand in; the Mk7 Jetta was sold with front-wheel drive only), and the panorama sunroof
  test and the sunroof drains check (inspections, rule 7).
- **Model name.** vPIC's model list for Volkswagen, 2022 (`GetModelsForMakeYear`, read 2026-10-29), has "Jetta" and
  "Jetta GLI" apart (the [GLI's list](volkswagen-jetta-gli-2022.md)). The card names the Jetta with no engine, so the
  list names none.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24 months by
  time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24 mo; spark plugs
  80,000 / 72 mo.
