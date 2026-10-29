# Volkswagen Tiguan Limited, 2018 (1st generation)

How `app/src/main/assets/known-schedules/volkswagen-tiguan-limited-2018.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every first-generation Tiguan card read, and what isn't
covered, is on the [2013–2015 Tiguan's page](volkswagen-tiguan-2013-2015.md).

Source: Volkswagen's 2018 all-model "Maintenance Schedule" card, the newest revision, revised 11/26/2019,
`2019/MC-10168646-0001`, filed with NHTSA as a manufacturer communication (its index files it under the
Tiguan too), read in full on 2026-10-29, as page images, by eye. It is read as
[the 2018 Jetta's page](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read) says.

- **The model.** In 2018 Volkswagen sold the first-generation Tiguan beside the new, second one as the "Tiguan
  Limited". vPIC's model list for Volkswagen, 2018 (`GetModelsForMakeYear`, read 2026-10-29), names both
  "Tiguan" and "Tiguan Limited", and vPIC decodes the patterns `WVGAV7AX*J` and `WVGBV7AX*J` (model year 2018,
  read 2026-10-29) as "Tiguan Limited". So this list is for "Tiguan Limited", with no alias; a 2018 "Tiguan" is
  the second generation (issue #450). One spark plug row covers its 2.0L engine, so the list names none.
- **The rows are the [2018 Golf Alltrack](volkswagen-golf-alltrack-2018.md)'s**, in that list's wording, with
  the model's name, and two more, which the card gives the Tiguan Limited by its own words:
  - **"Removable trailer hitch: Check function and inspect for damage (Tiguan, Tiguan Limited, and Atlas only
    if equipped)"**, in the Standard service: "Check removable trailer hitch (if fitted)", at 20,000 and every
    20,000 to 120,000, after the horn, as the card lists it. An inspection, so table-only.
  - **"Hood latch: Lubricate (Does not apply to: Passat, Golf, Beetle, and Jetta)"**, in the Extended service:
    the Tiguan Limited is none of the four, so the row is its: "Lubricate hood latch", at 40,000, 80,000 and
    120,000. Table-only, as the door checks are on the earlier cards: no catalog line names it. (The GTI,
    Golf R and Golf Alltrack lists leave it out only because "Golf" may take them in.)
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years (AWD only vehicles)", an
  additional item. The Tiguan Limited was sold with front-wheel drive and with 4Motion: vPIC decodes
  `WVGAV7AX*J` (trim "Base") as "FWD/Front-Wheel Drive" and `WVGBV7AX*J` (trim "4Motion Base") as
  "AWD/All-Wheel Drive". So by the card's reading (point 8) it is "Change AWD clutch fluid (AWD)", an
  `additional` reminder by time alone, every 36 months, with no service line, in `allWheelDriveOnly`, never
  placed on the grid. A Tiguan Limited known to drive two wheels loses it.
- **Both gearbox rows are left out.** The Tiguan Limited was sold with a conventional six-speed automatic only
  (vPIC decodes both patterns as "Automatic"), so the card's only row that could be its gearbox's is
  "Transmission, Automatic (non-DSG): Change fluid", every 80,000 miles. The card names no model on it, and the
  row is left out as on the Passat's lists ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514),
  [#445's question 1](https://github.com/mcosgriff/glovebox/issues/445#issuecomment-6489825579)); asked again
  for the Tiguan Limited on [#449's question 2](https://github.com/mcosgriff/glovebox/issues/449#issuecomment-6492074401). The 6-speed DSG row (no
  DSG on this car) and the 7-speed DSG ("Golf R only") are not its.
- Left out as not the Tiguan Limited's, by their own words: the convertible top (Beetle Convertible only), the
  sunroof water drains (Passat only), the 1.4L Jetta's belt checks, and the 3.6L's spark plugs.
- Reminders: engine oil 10,000 / 12 mo; air filter 60,000 / 72 mo; brake fluid every 24 months by time alone
  (its first time, 3 years, in its name); dust and pollen filter 40,000 / 24 mo; spark plugs 40,000 / 48 mo; the
  panoramic sunroof, the front differential lock and the tire filler bottle "(if fitted)" and the AWD clutch
  fluid, as on the Golf Alltrack's list.
