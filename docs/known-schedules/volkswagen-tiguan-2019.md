# Volkswagen Tiguan, 2019 (2nd generation)

How `app/src/main/assets/known-schedules/volkswagen-tiguan-2019.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every second-generation Tiguan card read, and what isn't covered, is on
the [2018 Tiguan's page](volkswagen-tiguan-2018.md#every-second-generation-tiguan-card-read).

Source: Volkswagen's 2019 "Maintenance Schedule" card for all its models except the e-Golf, filed with NHTSA as a
manufacturer communication: the newest revision, revised 11/12/2020, `2020/MC-10183295-0001`, read in full on
2026-10-29, as page images, by eye. The earlier revisions (`2019/MC-10163818-0001`, `2019/MC-10168645-0001`)
weren't read again; the [2019 GTI's page](volkswagen-gti-2019.md) says what they print differently.

- **The list is the [2019 Golf Alltrack](volkswagen-golf-alltrack-2019.md)'s, row for row** (the
  [2019 Passat](volkswagen-passat-2019.md)'s rows without its Passat-only sunroof water drains), with the model's
  name, and these differences, each by the card's own words:
  - **"Removable trailer hitch: Check function and inspect for damage (Tiguan, Tiguan Limited, and Atlas only if
    equipped)"**, in the Standard service: "Check removable trailer hitch (if fitted)", at 20,000 and every 20,000
    to 120,000, after the horn. Table-only.
  - **"Hood latch: Lubricate (Does not apply to: Passat, Golf, Beetle, and Jetta)"**, in the Extended service: the
    Tiguan is none of the four, so "Lubricate hood latch", at 40,000, 80,000 and 120,000, table-only, as on the
    [2018 Tiguan](volkswagen-tiguan-2018.md).
  - **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months): "Spark plugs: Replace (Only Atlas, Golf
    SportWagen 1.4L, Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI, Tiguan)"; the 40,000-mile row is "Except" the same
    models. The Tiguan had one engine, so the list names none.
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years": "Change AWD clutch fluid
  (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`, as on the 2018 Tiguan and the 2019
  Golf Alltrack. The second-generation Tiguan was sold with front-wheel drive and with 4Motion (the 2018 and 2024
  vPIC decodes on the [2018 page](volkswagen-tiguan-2018.md)).
- **Both gearbox rows are left out.** "Transmission: 6-Speed DSG automatic transmission: Change fluid and filter"
  (every 40,000) and "Transmission, automatic (non-DSG automatic transmission): Change fluid" (every 80,000) name no
  model; the Tiguan had a conventional 8-speed automatic only, and its row is left out as on the 2019 Passat's list
  ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)). The 7-speed DSG
  is "Golf GTI, Jetta GLI, Golf R only".
- Left out by their own words: the convertible top (Beetle Convertible only) and the sunroof water drains (Passat
  only).
- vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`, read 2026-10-29), names it "Tiguan".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36
  months by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name);
  dust and pollen filter 40,000 / 24 mo; front axle differential lock fluid every 3 years "(if fitted)"; panoramic
  sunroof rails every 40,000 or 2 years, first at 40,000 (the card's "or 3 years" for the first time is recorded,
  not put into months); the tire filler bottle's date every 2 years "(if fitted)"; spark plugs 80,000 / 72 mo.
