# Volkswagen Tiguan, 2022 (2nd generation)

How `app/src/main/assets/known-schedules/volkswagen-tiguan-2022.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every second-generation Tiguan card read, and what isn't covered, is on
the [2018 Tiguan's page](volkswagen-tiguan-2018.md#every-second-generation-tiguan-card-read).

Source: Volkswagen's "2022 VW Maintenance Schedule - All Model Gasoline Vehicles", USA, filed with NHTSA as a
manufacturer communication: the newest revision, revised 7/21/2021, `2021/MC-10199495-0001`, read in full on
2026-10-29, as page images, by eye. The earlier revisions (April 2021) weren't read again; the
[2022 GTI's page](volkswagen-gti-2022.md) says how they differ (the Golf and then the GTI move between the spark plug
rows). Volkswagen's reissue of the card (`USA5R507921`, 12/03/2025) isn't used: the card from the time wins.

- **The card is read as the [2022 GTI's list](volkswagen-gti-2022.md) reads it**, and the list is the
  [2022 Jetta](volkswagen-jetta-2022.md)'s (that table with the 80,000-mile plugs), with the model's name, and:
  - **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months): "Only: Atlas, Atlas Cross Sport, Golf GTI,
    Jetta, Jetta GLI, Taos, Tiguan".
  - **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years". The 2022 GTI's and Jetta's
    lists leave it out as "(if equipped)" on front-drive cars; the Tiguan was sold with front-wheel drive and with
    4Motion, so it is read as on the [2022 Golf R](volkswagen-golf-r-2022.md) and the 2018–2021 Tiguan lists:
    "Change AWD clutch fluid (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`. That
    reading of the 2022 row waits on
    [#437's question 5](https://github.com/mcosgriff/glovebox/issues/437#issuecomment-6491507764).
- **No gearbox row.** The 7-speed DSG row is "Only: Golf R, Golf GTI, Jetta GLI"; "Transmission, automatic: Change
  fluid (excluding Canada) (only non-DSG Automatic Transmission)", every 80,000 miles, names no model
  ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)).
- **No months on the columns**, as on the 2022 GTI's list. The oil reminder is 10,000 or 12 months, from the minor
  service's "or 1 year" (#517).
- Kept for every model, as on the GTI's list: the trailer hitch check ("(if equipped)", no model named on this card)
  and the torque splitter oil check, "(if fitted)", table-only. Left out, as there: the front axle differential lock
  fluid ("(if equipped)", no table to stand in), and the panorama sunroof test and the sunroof drains check
  (inspections, rule 7).
- **Model name.** vPIC's model list for Volkswagen, 2022 (`GetModelsForMakeYear`, read 2026-10-29), names it
  "Tiguan". One engine, the 2.0T, so the list names none.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name); dust and
  pollen filter 40,000 / 24 mo; spark plugs 80,000 / 72 mo.
