# Volkswagen Golf R, 2013 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-golf-r-2013.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" card for the Golf R, which Volkswagen files with NHTSA as a
manufacturer communication (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are in
NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29.

| Model year | What was read | Document |
|---|---|---|
| 2012 | **No card from the time was found.** NHTSA's index has no 2012 Golf R card; see [Not covered](#not-covered) | – |
| 2013 | The Golf R card ("Applicable engines: 2.0T", revised October 2012), in full; the list was written from it. NHTSA files it twice; the two files are byte for byte the same | `2012/MC-10156106-9999`, `2012/MC-10157647-9999` |
| 2013 | Volkswagen's 2013 reissue, for the oil change's time limit only (see below) | reissue `USA5R505321`, printed 11/24/2025 |

## The card

- A grid of 12 columns, 10,000 to 120,000 miles, with no months printed for a column: the layout of the
  [2010–2014 GTI](volkswagen-gti-2010-2014.md)'s cards. Jobs "regardless of mileage" (airbag check yearly, brake
  fluid, the Haldex clutch, the tire mobility kit, spark plugs and air filter at 6 years) are drawn in the columns
  Volkswagen chose and named with their time here.
- Every row the GTI's card also has is named as on the GTI list. Two differ:
  - "Brake fluid and clutch unit: Change fluid (Every 3 years regardless of mileage and every 2 years after)", at
    30,000, 50,000, 70,000, 90,000 and 110,000: named "Change brake fluid (at 3 years, then every 2 years)", as
    on the GTI list (the clutch is worked by the same fluid, and a job's name is held to 60 characters),
    `brake-fluid`, a reminder every 24 months by time alone.
  - "Haldex Clutch: Change oil (if applicable)", at 30,000, 60,000, 90,000 and 120,000, with footnote 4, "Every 3
    years, regardless of mileage: Haldex clutch: Change oil (if applicable)". It is
    read as the [CC 3.6L](volkswagen-cc-2014-2015-3.6.md) reads it, by the owner's ruling on [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part A, confirmed for every list in [the second round](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511795038)): named "Change Haldex clutch oil, every 3 years (AWD)", in `allWheelDriveOnly`,
    and a reminder every 36 months by time alone, with no `service` (no catalog line names the job). The Golf R was
    sold only with 4Motion, so every Golf R keeps it unless one is on record as two-wheel drive.
- The "Air filter element and snow filter" row is named "Replace air filter element and clean housing", as on the
  GTI list.
- **No gearbox row**: the card has none. The Mk6 Golf R was sold in the USA with a 6-speed manual only.
- No engine block: the card is for the 2.0T, the Golf R's only engine.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; air filter
  and spark plugs 60,000 / 72 mo; Haldex clutch oil every 36 months by time alone (AWD); brake fluid every 24
  months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2013 reissue (the owner's ruling of 2026-10-28, #517): its first minor
  service, which changes the oil, falls "at 10,000 miles … or 1 year after delivery", and the following minor and
  standard services keep the oil at every 10,000 or about once a year, as read for the
  [GTI](volkswagen-gti-2010-2014.md). The table has no months.
- Left out: the dealer's service sticker.

## The 2013 reissue

Volkswagen's owners-literature site (`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=2013`,
document `8db4252c-5991-4f79-a654-5c725d09bdf8`) serves an all-model 2013 card printed 11/24/2025, which names
the Golf R in its rows. It differs from the card from the time on the Golf R's jobs, so only its oil time limit is
taken; the card from the time wins (the owner's ruling of 2026-10-23):

- Air filter and spark plugs every 40,000 miles or 4 years for the Golf R (the card: 60,000, and 6 years).
- Dust and pollen filter every 40,000 miles or 2 years (the card: 20,000, or 2 years), as for every year's
  reissue (see the GTI page).
- A torque-vectoring rear differential every 40,000 miles or 4 years, a torque splitter check and DSG gearbox rows
  (`0GC`, `0D9`) naming the Golf R. None of these is on the 2013 card, and the Mk6 Golf R had none of them; they
  read as the later Golf R's.
- "AWD Clutch - Change fluid: Every 3 years", as an additional item for all applicable vehicles: the same job as
  the card's Haldex row.

## Not covered

- **2012**: NHTSA's index has no Golf R card for 2012 (its only 2012 Volkswagen card is the Routan's). Volkswagen's
  2025 reissue would be the only source, and the reissues differ from the cards from the time (above); that is the
  open question on the Golf ([#434](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244),
  question 3), so 2012 waits for it. The 2013 card isn't stretched back over it.
- The Mk7 and Mk8 Golf R (2015 on), which have cards and lists of their own: see the [2015 Golf R's page](volkswagen-golf-r-2015.md).
