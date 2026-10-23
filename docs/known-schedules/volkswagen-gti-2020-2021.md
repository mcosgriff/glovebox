# Volkswagen GTI, 2020–2021 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-gti-2020-2021.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule" cards for all its models, which Volkswagen files with NHTSA
as manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the
ids are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-23. These are
cards from the time; no reissue was needed.

| Model year | What was read | Document |
|---|---|---|
| 2018 | Three revisions of the card for all models except the e-Golf (4/17/2017, 7/18/2017, revised 11/26/2019). Not covered (below) | `2017/MC-10125867-9999`, `2017/MC-10127897-9999`, `2019/MC-10168646-0001` |
| 2019 | Three revisions of the card for all models except the e-Golf (8/7/2019, revised 11/26/2019, revised 11/12/2020). Not covered (below) | `2019/MC-10163818-0001`, `2019/MC-10168645-0001`, `2020/MC-10183295-0001` |
| 2020 | Three revisions of the card for all models except the e-Golf (8/7/2019, revised 8/26/2019, revised 11/26/2019). Every GTI row agrees with 2021 | `2019/MC-10163843-0002`, `2019/MC-10164263-0001`, `2019/MC-10168643-0001` |
| 2021 | The card for all gasoline models (7/9/2020), in full; the list was written from it | `2020/MC-10177239-0001` |

- **The layout.** A grid of three kinds of service against 12 columns, 10,000 to 120,000 miles: Minor
  at 10,000 and every 20,000 after; Standard at 20,000 and every 20,000 (it includes every Minor job);
  Extended at 40,000, 80,000 and 120,000 (it includes every Standard job). Each kind's jobs are listed
  under it. The table here is that grid written out: each column holds the jobs of its services.
- **Months.** The card gives them under the grid: the first Minor service at 10,000 miles or 1 year,
  the first Standard at 20,000 or 2 years, then each every 20,000 miles or 2 years after the last one.
  So each column is 12 months per 10,000 miles (10,000 at 12, 20,000 at 24 … 120,000 at 144), and the
  oil change, which is in every service, is every 10,000 miles or 12 months.
- **The table doesn't repeat**: the card stops at 120,000 and says nothing about after it.
- **Additional Maintenance Items.** The card gives these as intervals apart from the grid; they are
  reminders and are never placed on the table (the owner's ruling of 2026-10-23, #33):
  - Air filter: every 60,000 miles or 6 years.
  - Brake fluid: 3 years after registration, then every 2 years: a 24-month reminder by time alone,
    with the first time in its name.
  - Dust and pollen filter: every 40,000 miles or 2 years.
  - Front axle differential lock fluid "(if equipped)": every 3 years, by time alone. The card names no
    model for it, so it is kept, marked "(if fitted)". It has no service line: Glovebox's front
    differential oil change isn't plainly the same job, so it is logged by its name.
  - Spark plugs: every 80,000 miles or 6 years; the GTI is named on that row.
  - Sunroof drains, front and rear: check and clean every 20,000 miles or 2 years "(if equipped)". It has
    no service line, so it is logged by its name.
  - 7-speed DSG fluid: every 80,000 miles; the row names the Golf GTI. Not for a manual car
    (`manualExcluded`). These cards have no 6-speed DSG row.
  None of these is also a row of the grid. The grid's sunroof job is the guide rails' lubrication; the
  additional one is the drains.
- No tire rotation: the card has none, for any model.
- Left out as not the GTI's: the removable trailer hitch check (Tiguan and Atlas only), the AWD clutch
  fluid (the GTI is front-wheel drive only, as the Mk6 list left out the Haldex clutch), the 40,000-mile
  spark plug row (Arteon, Passat, Golf R) and the non-DSG automatic's fluid (the GTI has a manual or a
  DSG).
- Severe conditions: the card has one general sentence (some work, chiefly oil and the air filter, more
  often in extreme cold or dust) with no jobs or figures, so the list has nothing for them.
- One engine: the GTI's 2.0T. The cards name the GTI where a row differs by model, so the list names
  no engine.
- **What differed between the 2020 revisions:** the first (8/7/2019) gives brake fluid as every 2 years
  regardless of mileage; the later two, and the 2021 card, give 3 years after registration and then
  every 2. The reminder is every 24 months either way; its name follows the later cards. The spark plug
  rows are worded "Except … GTI" and "Only … GTI" in 2020 and "Only" in 2021; both put the GTI at
  80,000 miles or 6 years. Nothing else that applies to the GTI differs.
- **Model name.** NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`) has "Golf GTI" for
  2016 to 2021 and no "GTI"; 2014 and 2015 have "GTI". The list is for "GTI", like the others, with
  `modelAliases` "Golf GTI".
- **Not covered:**
  - **2018.** Its cards print the panoramic sunroof both ways: the Extended service checks its function,
    and the additional items give it a service of its own (clean and grease the rails, or for colorless
    grease only a function check) after 40,000 miles or 3 years, then every 40,000 or 2 years. A list
    can't hold one job in the grid and apart from it, and the card doesn't settle whether they are one
    job. Its revisions also differ on the sunroof rows. The 2018 card also has spark plugs at 40,000
    miles or 4 years for the 2.0L and a hood latch row that "does not apply to … Golf", so it would be a
    list of its own in any case.
  - **2019.** The same panoramic sunroof rows as 2018. And the DSG rows disagree between revisions: the
    first (8/7/2019) gives the 7-speed DSG to the Golf R only, which leaves a DSG GTI with the 6-speed
    row (40,000 miles); the later two name the Golf GTI on the 7-speed row (80,000) and still keep a
    6-speed row that names no model. The cards don't say which gearbox a 2019 GTI has.
  - **2017.** No card was found, and the 2016 and 2018 cards differ, so it isn't inferred from either.
  - **2022 on** is the next generation (Mk8), not part of this issue.
