# Volkswagen Passat, 2020–2021 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2020-2021.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). The [2022 list](volkswagen-passat-2022.md) is made from the
next year's card the same way. Every Passat NMS card read, and what isn't covered, is on the
[2014 2.5L's page](volkswagen-passat-2014-2.5.md).

Source: Volkswagen's all-model "Maintenance Schedule" cards, filed with NHTSA as manufacturer communications.
Read on 2026-10-29, in full, as page images, by eye. These are cards from the time; no reissue was needed.

| Model year | What was read | Document |
|---|---|---|
| 2020 | "2020 VW Maintenance Schedule - All Models except e-Golf", revised 11/26/2019, the newest revision | `2019/MC-10168643-0001` |
| 2021 | "2021 VW Maintenance Schedule - All Model Gasoline Vehicles", 7/9/2020, the only one; the list's source | `2020/MC-10177239-0001` |

The earlier 2020 revisions (`2019/MC-10163843-0002`, 8/7/2019; `2019/MC-10164263-0001`, 8/26/2019) weren't
read again; the [2020–2021 GTI's page](volkswagen-gti-2020-2021.md) says what they print differently (brake
fluid "every 2 years regardless of mileage" in the first; the spark plug rows' wording). The Passat is on the
40,000-mile spark plug row by any of them.

- **The two cards agree, row for row, for the Passat.** The only difference between them is the 40,000-mile
  spark plug row's wording: "Except: Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI, Tiguan, Atlas, Atlas Cross Sport"
  on the 2020 card, "Only: Arteon, Passat, Golf R" on the 2021 card. The Passat is at 40,000 miles or 4 years
  by both. So one list covers both years.
- **The cards are read as the [2020–2021 GTI's list](volkswagen-gti-2020-2021.md) reads them** (below). The
  rows are that list's, in its wording, with these differences:
  - **Spark plugs every 40,000 miles or 4 years** (40,000 / 48 months), not the GTI's 80,000 / 6 years.
  - **No DSG row, and both gearbox rows are left out.** "Transmission, 7-Speed DSG Automatic Transmission:
    Change fluid", every 80,000 miles, is "Only: Golf GTI, Jetta GLI, Golf R". "Transmission, automatic:
    Change fluid (excluding Canada) (only non-DSG Automatic Transmission)", every 80,000 miles, names no model
    or engine, so it is left out as on the 2018 and 2019 Passat lists
    ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)). Unlike
    2018 and 2019, these cards have no 6-speed DSG row, so asked again on #445.
- Left out by their own words: the removable trailer hitch ("Tiguan and Atlas only if equipped"). The AWD
  clutch fluid is left out: the 2020–2021 Passat was sold with front-wheel drive only.
- The Passat-only "Sunroof water drains" row of the 2019 card's services is gone; these cards have "Sunroof
  drains: Front and rear" as an additional item for every model, kept as on the GTI's list.
- **Model name.** vPIC's model list for Volkswagen, 2020 (`GetModelsForMakeYear`, read 2026-10-29), names it
  "Passat". One engine, the 2.0T, and no row names an engine, so the list names none.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24
  months by time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24
  mo; front axle differential lock fluid every 3 years "(if fitted)"; spark plugs 40,000 / 48 mo; sunroof
  drains, front and rear, every 20,000 / 24 mo "(if fitted)".

## How the 2020–2022 all-model cards are read

For later lists from the 2020, 2021 and 2022 cards. Each year's card is read as the GTI list made from it
first read it ([2020–2021](volkswagen-gti-2020-2021.md), [2022](volkswagen-gti-2022.md)), which is the
[2018 card's reading](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read) with these points of its
own:

1. **The newest revision of each year wins** (the owner's ruling, #143): 2020 `2019/MC-10168643-0001`
   (revised 11/26/2019); 2021 `2020/MC-10177239-0001` (7/9/2020, the only one); 2022 `2021/MC-10199495-0001`
   (revised 7/21/2021).
2. **The grid** is the 2018 card's: Minor at 10,000 and every 20,000 after, Standard at 20,000 and every
   20,000, Extended at 40,000, 80,000 and 120,000; 12 columns, not repeated.
3. **Months on the columns differ by the GTI list they follow.** The 2020–2021 lists give each column 12
   months per 10,000 miles, from the footnotes, as the 2019 lists do; the 2022 lists give the columns no
   months, as the [2022 GTI's page](volkswagen-gti-2022.md) says. The oil change is a reminder every 10,000
   miles or 12 months either way (#517).
4. **No hood latch, convertible top, tire filler bottle or panoramic-sunroof rails item** on these cards; the
   panoramic sunroof is back on the 2022 card only (below).
5. **2020 and 2021 additional items**, kept for every model they don't name otherwise: air filter, brake fluid,
   dust and pollen filter, front axle differential lock fluid "(if fitted)", sunroof drains every 20,000 or 2
   years "(if fitted)" (a reminder with no service line, as the GTI's list has it). The trailer hitch row in
   the services is "Tiguan and Atlas only".
6. **2022 additional items**: the AWD clutch fluid and the front axle differential lock fluid, both "(if
   equipped)" with no table to stand in, are not reminders; nor are the panorama sunroof noise and function
   test (every 10,000 or 1 year) and the sunroof drains check (every 20,000 or 2 years), which are inspections
   (rule 7). This is the 2022 GTI list's reading; it differs from the 2020–2021 reading of the same drains and
   differential lock rows, and neither list was changed to match the other here.
7. **2022 services** add "Ribbed Belt and Tensioning Roller for Belt Starter Generator - Check condition" and
   "Torque Splitter: Check oil … (if equipped)" to the Standard service, and the trailer hitch row names no
   model ("(if equipped)"), so it stays for every model, "(if fitted)".
8. **Spark plugs** by the model names on the two rows: 40,000 / 48 mo for the models on the "Except …" row
   (2020) or the "Only: Arteon, Passat, Golf R" row (2021, 2022); 80,000 / 72 mo for those named on the other.
9. **Gearbox rows.** The 7-speed DSG row names its models (Golf GTI, Jetta GLI, Golf R). The non-DSG automatic
   row names none, so a model not on the 7-speed row gets no gearbox reminder
   ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514), asked again
   on #445 for these cards).
10. **AWD clutch fluid** "(if equipped)", every 3 years: on the 2020–2021 cards, read as the 2018 card's point
    8 (`allWheelDriveOnly` for a model sold with AWD); on the 2022 card, point 6.

## Not covered

- 2019 has [its own list](volkswagen-passat-2019.md): its card has the Passat-only sunroof water drains in
  the services, the panoramic sunroof rails and the tire filler bottle, and a 6-speed DSG row.
- 2022: [its own list](volkswagen-passat-2022.md).
