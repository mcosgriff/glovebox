# Volkswagen GTI, 2019 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-gti-2019.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule" cards for all its models except the e-Golf, which Volkswagen
files with NHTSA as manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`;
the ids are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-26. These are
cards from the time; no reissue was needed.

| Revision | What was read | Document |
|---|---|---|
| Revised 08/07/2019 | In full, compared with the newest | `2019/MC-10163818-0001` |
| Revised 11/26/2019 | In full, compared with the newest | `2019/MC-10168645-0001` |
| Revised 11/12/2020 | In full, off the page; **the list was written from it** | `2020/MC-10183295-0001` |

**The newest revision wins** (the owner's ruling, #143: for 2018 on, the newest revision of each year's
card, the 2019 DSG row included). What the earlier ones print differently is below.

- **The layout** is the 2018 card's ([its page](volkswagen-gti-2018.md)): Minor at 10,000 and every 20,000
  after, Standard at 20,000 and every 20,000 (it includes every Minor job), Extended at 40,000, 80,000 and
  120,000; the table is that grid written out, 10,000 to 120,000. It doesn't repeat.
- **Months**: the footnotes are the 2018 card's, so each column is 12 months per 10,000 miles and the oil
  change is every 10,000 miles or 12 months.
- **The GTI's jobs in the services are the 2018 list's**; only the exhaust check reads "leaks, damage, and
  secure fittings", and the interior lights check adds the overhead lights (one job either way).
- **The panoramic sunroof is printed twice; both are kept** (the owner's ruling, #143), as on the 2018
  list: the Extended service's function check "(if equipped)" is a table row with no `service`, "(if
  fitted)"; the additional item (for gray grease, clean and grease the rails and clean the wind deflector;
  for colorless grease, only a function check) is an `additional` reminder, never on the grid: every
  40,000 miles or 2 years, first at 40,000 (`firstAtMiles`). **The card's first time is "after 40,000
  miles or 3 years"**; the format's first time is a distance only, so the 3 years is recorded here and
  not put into months. The non-panoramic sunroof's guide rails are a table row of their own, "(if fitted)".
- **Additional Maintenance Items** are reminders in `additional` with the card's figures, never placed on
  the table (the owner's ruling of 2026-10-23, #33):
  - Air filter: every 60,000 miles or 6 years.
  - Brake fluid: 3 years after registration, then every 2 years: a 24-month reminder by time alone, with
    the first time in its name.
  - Dust and pollen filter: every 40,000 miles or 2 years.
  - Front axle differential lock fluid "(if equipped)": every 3 years, by time alone; no model named, so
    kept "(if fitted)", with no service line, as on the 2018 and 2020–2021 lists.
  - Panoramic sunroof: above.
  - Spark plugs: every 80,000 miles or 6 years; the GTI is named on that row ("Only … GTI …"), and
    excepted from the 40,000-mile row ("Except … GTI …").
  - Tire filler bottle "(if equipped)": check its expiry date every 2 years (no more than 4 years old),
    by time alone, "(if fitted)", with no service line.
  - DSG fluid: every 80,000 miles, from the 7-speed row, which names the "Golf GTI, Jetta GLI, Golf R
    only". Not for a manual car (`manualExcluded`). The 6-speed row (fluid and filter, every 40,000) names
    no model; it is left out, since the newest card puts the GTI on the 7-speed row.
- **Left out as not the GTI's:** the AWD clutch fluid (the GTI is front-wheel drive), the 40,000-mile
  spark plugs (the GTI is excepted), the 6-speed DSG (above), the non-DSG automatic's fluid, the removable
  trailer hitch (Tiguan, Tiguan Limited and Atlas only), the convertible top (Beetle Convertible only) and
  the sunroof water drains (Passat only).
- No tire rotation: the card has none.
- Severe conditions: one general sentence with no jobs or figures, so the list has nothing for them.
- One engine: the GTI's 2.0T; the rows name the GTI where they differ by model, so the list names none.
- **Model name.** NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`, read 2026-10-26) has
  "Golf GTI" for 2019 and no "GTI". The list is for "GTI", like the other Mk7 lists, with `modelAliases`
  "Golf GTI".

## What the earlier revisions print differently

- **Revised 08/07/2019:** the 7-speed DSG row is "Golf R only", which would leave a DSG GTI with the
  6-speed row (fluid and filter, every 40,000 miles). Brake fluid is "every 2 years regardless of
  mileage". The non-DSG automatic's row is "fluid and filter".
- **Revised 11/26/2019:** the 7-speed row names the Golf GTI, Jetta GLI and Golf R; the non-DSG
  automatic's row is "fluid" only. Brake fluid is still "every 2 years".
- **Revised 11/12/2020 (the newest):** brake fluid at 3 years after registration, then every 2. Nothing
  else that applies to the GTI differs from 11/26/2019.

The brake fluid reminder is every 24 months by any revision; its name follows the newest. The DSG
reminder follows the newest: 80,000 miles.

## Needs a decision (left out)

- **Hood latch.** "Hood latch: Lubricate (Does not apply to: Passat, Golf, Beetle, and Jetta)", in the
  Extended service of all three revisions. As for 2018, the card doesn't say whether the GTI counts as a
  Golf here (its spark plug rows name "Golf 1.4L" and "GTI" apart). Left out until the owner rules.

## Not covered

- 2018: [its own list](volkswagen-gti-2018.md); its spark plugs (40,000 or 4 years) and DSG row (6-speed,
  40,000) differ. 2020–2021: [their list](volkswagen-gti-2020-2021.md).
