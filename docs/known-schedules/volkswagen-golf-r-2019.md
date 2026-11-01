# Volkswagen Golf R, 2019 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-r-2019.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 and Mk8 Golf R card read, and what isn't covered, is on the
[2015 Golf R's page](volkswagen-golf-r-2015.md).

Source: Volkswagen's 2019 all-model "Maintenance Schedule" card, the newest revision, revised 11/12/2020,
`2020/MC-10183295-0001`, filed with NHTSA as a manufacturer communication (its index files it under the Golf R
too), read in full on 2026-10-29, as page images, by eye. The earlier revisions weren't read again; the
[2019 GTI's page](volkswagen-gti-2019.md) says what they print differently.

- **The list is the [2019 Golf Alltrack](volkswagen-golf-alltrack-2019.md)'s, row for row**, with the model's name
  and one additional item more; that page and the [2019 Golf 1.4T's](volkswagen-golf-2019-1.4t.md) say how the card
  was read.
- **Spark plugs** 40,000 / 48 mo: the 40,000-mile or 4-year row is "Except Atlas, Golf SportWagen 1.4L, Golf 1.4L,
  GTI, Jetta 1.4L, Jetta GLI, Tiguan", among which the Golf R isn't, and the 80,000-mile row is "Only" those. No row
  names a Golf R engine, so the list names none.
- **The 7-speed DSG.** "Transmission: 7-Speed DSG automatic transmission: Change fluid (Golf GTI, Jetta GLI, Golf R
  only)", "Every 80,000 miles": "Change DSG transmission fluid (DSG only)", `trans-fluid`, every 80,000 miles, in
  `manualExcluded`, as on the 2019 GTI's list. The 6-speed DSG and non-DSG automatic rows name no model and are left
  out, as on the [2018 Golf R](volkswagen-golf-r-2018.md).
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years": "Change AWD clutch fluid
  (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`, as on the 2018 Golf R. vPIC decodes
  the pattern `WVWVA7AU*KW` (model year 2019, read 2026-10-29) as "Golf R", 2.0L, with no drive type; the Golf R was
  sold with 4Motion only, and a car with no drive on record keeps the job.
- **Left out: the hood latch** ("Does not apply to: Passat, Golf, Beetle, and Jetta"), by the owner's ruling on [#555](https://github.com/mcosgriff/glovebox/issues/555#issuecomment-6511795335): "Golf" covers the whole Golf family (SportWagen, Alltrack, Golf R).
- vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`), names it "Golf R".
- Reminders: engine oil 10,000 / 12 mo; additional items as on the 2018 Golf R's list.
