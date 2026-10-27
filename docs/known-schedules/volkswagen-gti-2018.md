# Volkswagen GTI, 2018 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-gti-2018.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule" cards for all its models except the e-Golf, which Volkswagen
files with NHTSA as manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`;
the ids are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-26. These are
cards from the time; no reissue was needed (Volkswagen's owners-literature site has no 2018 card).

| Revision | What was read | Document |
|---|---|---|
| 4/17/2017 | In full, compared with the newest | `2017/MC-10125867-9999` |
| 7/18/2017 | In full, compared with the newest | `2017/MC-10127897-9999` |
| Revised 11/26/2019 | In full, off the page; **the list was written from it** | `2019/MC-10168646-0001` |

**The newest revision wins** (the owner's ruling, #143: for 2018 on, the newest revision of each year's
card). What the earlier ones print differently is below.

- **The layout.** A grid of three kinds of service against 12 columns, 10,000 to 120,000 miles: Minor at
  10,000 and every 20,000 after; Standard at 20,000 and every 20,000 (it includes every Minor job);
  Extended at 40,000, 80,000 and 120,000, where a Standard service also falls. Each kind's jobs are listed
  under it. The table here is that grid written out: each column holds the jobs of its services.
- **Months.** The footnotes give the first Minor service at 10,000 miles or 1 year, the first Standard at
  20,000 or 2 years, then each every 20,000 miles or 2 years after the last one. So each column is 12
  months per 10,000 miles (10,000 at 12 … 120,000 at 144), and the oil change, in every service, is every
  10,000 miles or 12 months, as on the [2020–2021 list](volkswagen-gti-2020-2021.md).
- **The table doesn't repeat**: the card stops at 120,000 and says nothing about after it.
- **Unlike 2020–2021**, the brake system and the coil springs and shock absorbers are checked in the
  Extended service, not the Standard, and there are two sunroof rows (below).
- **The panoramic sunroof is printed twice; both are kept** (the owner's ruling, #143):
  - The Extended service's "Sunroof, Panoramic Sliding (if applicable): Check function" is a table row with
    no `service`, "(if fitted)".
  - The additional item "Panorama Sliding Sunroof (if equipped)" (for gray grease, clean and grease the
    guide rails and clean the wind deflector; for colorless grease, only a function check listening for
    noises) is an `additional` reminder, never placed on the grid: every 40,000 miles or 2 years, first at
    40,000 (`firstAtMiles`). **The card's first time is "after 40,000 miles or 3 years"**; the format's
    first time is a distance only, so the 3 years is recorded here and not put into months.
  - The non-panoramic sunroof's row (check, clean and lubricate the guide rails, "(if applicable)") is a
    table row of its own, "(if fitted)".
- **Additional Maintenance Items** are reminders in `additional` with the card's figures, never placed on
  the table (the owner's ruling of 2026-10-23, #33):
  - Air filter: every 60,000 miles or 6 years.
  - Brake fluid: 3 years after first registration, then every 2 years: a 24-month reminder by time alone,
    with the first time in its name.
  - Dust and pollen filter: every 40,000 miles or 2 years.
  - Front axle differential lock fluid "(if equipped)": every 3 years, by time alone. As on the
    2020–2021 list, the card names no model for it, so it is kept, marked "(if fitted)", with no service
    line.
  - Panoramic sunroof: above.
  - Spark plugs: every 40,000 miles or 4 years, on the row for 4-cylinder gasoline "1.4L, 1.8L, and 2.0L"
    engines. The GTI's 2.0T is the only engine, so the list names none.
  - Tire filler bottle "(if equipped)": check its expiry date every 2 years (it must be no more than 4
    years old), by time alone, "(if fitted)", with no service line.
  - 6-speed DSG fluid and filter: every 40,000 miles; the row names no model. The 7-speed row is "Golf R
    only", so a DSG GTI has the 6-speed row. Not for a manual car (`manualExcluded`).
- **Left out as not the GTI's:** the AWD clutch fluid ("AWD only vehicles"; the GTI is front-wheel drive,
  as the Mk6 list left out the Haldex clutch), the coolant pump toothed belt and timing belt checks ("1.4L
  Jetta only"), the 3.6L's spark plugs, the 7-speed DSG ("Golf R only"), the non-DSG automatic's fluid,
  the removable trailer hitch (Tiguan, Tiguan Limited and Atlas only), the convertible top (Beetle
  Convertible only) and the sunroof water drains (Passat only).
- No tire rotation: the card has none.
- Severe conditions: one general sentence (oil and the air filter more often in extreme cold or dust)
  with no jobs or figures, so the list has nothing for them.
- **Model name.** NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`, read 2026-10-26) has
  "Golf GTI" for 2018 and no "GTI". The list is for "GTI", like the other Mk7 lists, with `modelAliases`
  "Golf GTI".

## What the earlier revisions print differently

- **4/17/2017:** the grid has 11 columns with no 110,000: Minor is marked at 10,000 … 90,000 and at
  120,000, Standard at 20,000 … 100,000 only, Extended at 40,000, 80,000 and 120,000 (read off the page).
  The exterior lights check is in the Extended service, not the Standard. Its services have no sunroof row; its additional items have the panoramic sunroof
  item and a second one, "Sunroof: check function, clean, lubricate, and check water drains (if
  equipped)", after 40,000 or 3 years, then every 40,000 or 2 years. No footnotes give months for the
  services. Spark plugs name the "2.0T gasoline". The DSG rows are the same as the newest.
- **7/18/2017:** the 110,000 column is there and the marks fall as on the newest; the exterior lights check
  is in the Standard service; the Extended service gains one sunroof row, "Panorama
  Sunroof and Sliding / Tilting Sunroof: check, clean and lubricate; check and clean the water drains (if
  equipped)", and the additional items keep both sunroof items. No footnotes give months.
- **Revised 11/26/2019 (the newest):** adds the footnotes with the months, splits the sunroof into the
  panoramic and non-panoramic rows, drops the additional "Sunroof" item, and marks the coolant pump and
  timing belt checks "1.4L Jetta only". The brake fluid, the spark plugs, the dust and pollen filter, the
  air filter and the DSG rows read the same in all three.

## Needs a decision (left out)

- **Hood latch.** The Extended service has "Hood latch: Lubricate (Does not apply to: Passat, Golf,
  Beetle, and Jetta)". The card doesn't say whether the GTI counts as a Golf here: elsewhere it names
  "Golf" and "GTI" apart (2019's spark plug rows), while vPIC calls the car "Golf GTI". The row is left
  out until the owner rules.

## Not covered

- 2017: [its own list](volkswagen-gti-2017.md), from a reissue. 2019: [its own list](volkswagen-gti-2019.md);
  its spark plugs and DSG row differ.
