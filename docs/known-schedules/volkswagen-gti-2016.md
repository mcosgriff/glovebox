# Volkswagen GTI, 2016 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-gti-2016.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids
are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-23.

| Model year | What was read | Document |
|---|---|---|
| 2015 | The GTI 2.0TFSI card (posted June 2014), for comparison; it has a list of its own | `2014/MC-10120120-9999` |
| 2016 | The GTI 2.0TFSI card (posted August 2015), in full; the list was written from it | `2015/MC-10123072-9999` |

- **What differs from 2015.** The 2016 card is the 2015 card's grid without five rows: tire rotation,
  the yearly airbag check, the door check lubrication, the rear window washer nozzle check and the
  service sticker. Its dust and pollen filter is every 40,000 miles or 2 years, drawn at 40,000, 80,000
  and 120,000 (2015: every 20,000, and every 2 years). Every other row is in the same columns, so the
  remaining jobs keep the wording of the 2015 and 2010–2014 lists.
- **No tire rotation.** The card has no rotation row and nothing says when to rotate; the list has
  none. That is the card as printed, not checked against a booklet.
- The card is a grid: 12 columns, 10,000 to 120,000 miles, with no months printed for a column. Jobs
  "regardless of mileage" (brake fluid, the tire mobility kit, spark plugs and the air filter every 6
  years) are drawn in the columns Volkswagen chose and named with their time here where the time is the
  only thing that says when.
- Reminders: engine oil 10,000; dust and pollen filter 40,000 / 24 mo; DSG fluid and filter 40,000 (not
  for a manual car); air filter and spark plugs 60,000 / 72 mo; brake fluid every 24 months by time
  alone (the card says at 3 years first, then every 2).
- One engine: the card is for the 2.0TFSI, the GTI's only engine, so the list names none.
- No special operating conditions are given on the card.
- **Model name.** NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`) has "Golf GTI" for
  2016 and no "GTI"; 2014 and 2015 have "GTI". The list is for "GTI", like the others, with
  `modelAliases` "Golf GTI", so a 2016 decoded by VIN finds it.
- **Not covered:**
  - **2017.** No card was found, and the 2016 and 2018 cards differ in layout and jobs, so it isn't
    inferred from either.
  - **2018 and 2019:** see [the 2020–2021 list's page](volkswagen-gti-2020-2021.md).
