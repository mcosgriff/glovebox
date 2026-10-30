# Volkswagen Golf R, 2018 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-r-2018.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 and Mk8 Golf R card read, and what isn't covered, is on the
[2015 Golf R's page](volkswagen-golf-r-2015.md).

Source: Volkswagen's 2018 all-model "Maintenance Schedule" card, the newest revision, revised 11/26/2019,
`2019/MC-10168646-0001`, filed with NHTSA as a manufacturer communication (its index files it under the Golf R
too), read in full on 2026-10-29, as page images, by eye. It is read as
[the 2018 card's reading](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read) says.

- **The list is the [2018 Golf Alltrack](volkswagen-golf-alltrack-2018.md)'s, row for row** (which is the
  [2018 Golf](volkswagen-golf-2018.md)'s with the AWD clutch fluid), with the model's name and one additional item
  more. Spark plugs 40,000 / 48 mo: the 4-cylinder "1.4L, 1.8L, and 2.0L" row covers the Golf R's 2.0L, so the list
  names no engine.
- **The 7-speed DSG.** "Transmission, 7-Speed DSG: Change fluid (Golf R only)", "Every 80,000 miles": an additional
  item naming the Golf R, so "Change DSG transmission fluid (DSG only)", `trans-fluid`, every 80,000 miles, in
  `manualExcluded`, as the [2019 GTI](volkswagen-gti-2019.md) names its 7-speed row. Never placed on the grid.
- **The other two gearbox rows are left out**: "Transmission, 6-Speed DSG: Change fluid and filter" (every 40,000)
  and "Transmission, Automatic (non-DSG): Change fluid" (every 80,000) name no model. The card gives the Golf R a
  self-shifting gearbox row of its own, and taking an unnamed row as well would give a Golf R with the 7-speed two
  gearbox jobs (point 9 of the reading; [asked on #437](https://github.com/mcosgriff/glovebox/issues/437#issuecomment-6491507764), question 2).
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years (AWD only vehicles)": "Change
  AWD clutch fluid (AWD)", an `additional` reminder by time alone every 36 months, no service line, in
  `allWheelDriveOnly`, never on the grid (point 8; PR #537). The Golf R was sold with 4Motion all-wheel drive only;
  vPIC decodes the pattern `WVWVF7AU*JW` (model year 2018, read 2026-10-29) as "Golf R", 2.0L, 292 HP,
  "AWD/All-Wheel Drive". Only a car known to drive two wheels would lose it.
- **Left out: the hood latch**, "Hood latch: Lubricate (Does not apply to: Passat, Golf, Beetle, and Jetta)", by
  the owner's ruling on [#555](https://github.com/mcosgriff/glovebox/issues/555#issuecomment-6511795335): "Golf" covers the whole Golf family (SportWagen, Alltrack, Golf R).
- Left out by their own words: the Passat-only sunroof water drains, the trailer hitch, the convertible top, the
  1.4L Jetta's belt checks and the 3.6L's spark plugs.
- **Model name.** vPIC's model list for Volkswagen, 2018 (`GetModelsForMakeYear`), has "Golf R" apart from "Golf".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36
  months by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name);
  dust and pollen filter 40,000 / 24 mo; front axle differential lock fluid every 3 years "(if fitted)"; panoramic
  sunroof rails every 40,000 or 2 years, first at 40,000; the tire filler bottle's date every 2 years "(if fitted)";
  spark plugs 40,000 / 48 mo; 7-speed DSG fluid 80,000 (not for a manual).
