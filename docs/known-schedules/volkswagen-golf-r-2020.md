# Volkswagen Golf R, 2020 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-r-2020.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 and Mk8 Golf R card read, and what isn't covered, is on the
[2015 Golf R's page](volkswagen-golf-r-2015.md).

Source: Volkswagen's 2020 all-model card, revised 11/26/2019, `2019/MC-10168643-0001`, read in full on
2026-10-29, as page images, by eye, as on the [2020 SportWagen 1.4T's page](volkswagen-golf-sportwagen-2020-1.4t.md).
Made by the owner's ruling on [#556](https://github.com/mcosgriff/glovebox/issues/556#issuecomment-6511734459) (question 2).

- **The list is the [2020 Golf Alltrack](volkswagen-golf-alltrack-2020.md)'s, row for row**, with the model's name
  and the DSG fluid.
- **Spark plugs** 40,000 / 48 mo: the Golf R isn't among the "Except: Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI,
  Tiguan, Atlas, Atlas Cross Sport" of the 40,000-mile row. One engine, the 2.0T, so the list names none.
- **The 7-speed DSG.** "Transmission, 7-Speed DSG Automatic Transmission: Change fluid", "Every 80,000 miles. Only:
  Golf GTI, Jetta GLI, Golf R": "Change DSG transmission fluid (DSG only)", `trans-fluid`, every 80,000 miles, in
  `manualExcluded`, as on the [2020–2021 GTI](volkswagen-gti-2020-2021.md). The non-DSG automatic's row is left
  out.
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years": "Change AWD clutch fluid
  (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`, as on the
  [2019 Golf R](volkswagen-golf-r-2019.md). vPIC decodes `WVWVA7AU*LW` (model year 2020, read 2026-10-29) as
  "Golf R", 2.0L, a 7-speed automatic, with no drive type; the Golf R was sold with 4Motion only, and a car with no
  drive on record keeps the job.
- **2021 isn't here**: the 2021 card names the Golf R, but vPIC's model list for Volkswagen, 2021, has no "Golf R"
  (read 2026-10-29), so no 2021 Golf R can be decoded or picked.
- vPIC's model list for Volkswagen, 2020 (`GetModelsForMakeYear`), names it "Golf R".
- Reminders: engine oil 10,000 / 12 mo; additional items as on the Alltrack's list, and the DSG fluid 80,000 (not
  for a manual).
