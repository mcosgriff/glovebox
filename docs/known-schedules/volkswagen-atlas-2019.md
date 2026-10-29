# Volkswagen Atlas, 2019

How `app/src/main/assets/known-schedules/volkswagen-atlas-2019.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Atlas card read, and what isn't covered, is on the
[2018 2.0T's page](volkswagen-atlas-2018-2.0t.md#every-atlas-card-read).

Source: Volkswagen's 2019 "Maintenance Schedule" card for all its models except the e-Golf, filed with NHTSA as a
manufacturer communication: the newest revision, revised 11/12/2020, `2020/MC-10183295-0001`, read in full on
2026-10-29, as page images, by eye. The earlier revisions weren't read again; the
[2019 GTI's page](volkswagen-gti-2019.md) says what they print differently.

- **The list is the [2019 Tiguan](volkswagen-tiguan-2019.md)'s, row for row**, with the model's name: the trailer
  hitch row names "Tiguan, Tiguan Limited, and Atlas"; the hood latch row doesn't apply to "Passat, Golf, Beetle, and
  Jetta", so the Atlas has it; nothing else names either.
- **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months): "Spark plugs: Replace (Only Atlas, Golf
  SportWagen 1.4L, Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI, Tiguan)"; the 40,000-mile row is "Except" the same models.
  The row names the Atlas and no engine, so the 2.0T and the 3.6L share it and the list names no engine (the 2018
  card's 60,000-mile row for the 3.6L is gone).
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years": "Change AWD clutch fluid
  (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`, as on the 2018 lists.
- **Both gearbox rows are left out.** "Transmission, automatic (non-DSG automatic transmission): Change fluid" (every
  80,000) names no model
  ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)); the 6-speed DSG
  row names none either, and the 7-speed DSG is "Golf GTI, Jetta GLI, Golf R only".
- Left out by their own words: the convertible top (Beetle Convertible only) and the sunroof water drains (Passat
  only).
- vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`, read 2026-10-29), names it "Atlas".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36
  months by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name);
  dust and pollen filter 40,000 / 24 mo; front axle differential lock fluid every 3 years "(if fitted)"; panoramic
  sunroof rails every 40,000 or 2 years, first at 40,000; the tire filler bottle's date every 2 years "(if fitted)";
  spark plugs 80,000 / 72 mo.
