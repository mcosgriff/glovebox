# Volkswagen Golf SportWagen 1.4T, 2020 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-sportwagen-2020-1.4t.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md). Every Mk7 card read, and what isn't covered, is on the
[2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).

Source: Volkswagen's "2020 VW Maintenance Schedule - All Models except e-Golf", revised 11/26/2019, the newest
2020 revision, `2019/MC-10168643-0001`, filed with NHTSA as a manufacturer communication, read in full on
2026-10-29, as page images, by eye. NHTSA's index files only the earlier 2020 revisions (`2019/MC-10163843`,
`2019/MC-10164263`) under the Golf SportWagen; the newest revision wins (#143), and the
[2020–2021 GTI's page](volkswagen-gti-2020-2021.md) says what the earlier ones print differently.

- **The card names no SportWagen.** Its spark plug rows are "Every 40,000 miles or 4 years … Except: Golf 1.4L,
  GTI, Jetta 1.4L, Jetta GLI, Tiguan, Atlas, Atlas Cross Sport" and "Every 80,000 miles or 6 years … Only: Golf
  1.4L, …". The owner's ruling ([#555](https://github.com/mcosgriff/glovebox/issues/555#issuecomment-6511795335), part A) reads the SportWagen 1.4L as the
  "Golf 1.4L", as the 2019 card names "Golf SportWagen 1.4L" beside "Golf 1.4L" on the same rows. So spark
  plugs 80,000 / 72 mo, and `"engine": { "label": "1.4T", "displacementL": 1.4 }`; any other engine has the
  [1.8T's list](volkswagen-golf-sportwagen-2020-1.8t.md).
- **The list is the [2020–2021 Golf 1.4T](volkswagen-golf-2020-2021-1.4t.md)'s, row for row**, with the model's
  name, the year 2020 and the AWD clutch fluid; that page and the
  [2020–2021 Passat's](volkswagen-passat-2020-2021.md#how-the-20202022-all-model-cards-are-read) say how the card
  is read.
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years": "Change AWD clutch fluid
  (AWD)", an `additional` reminder by time alone, every 36 months, no service line, in `allWheelDriveOnly`,
  never placed on the grid. vPIC decodes `3VW157AU*LM` (model year 2020, read 2026-10-29) as "Golf
  SportWagen", "4Motion S", 1.4L, "AWD/All-Wheel Drive". Unlike 2019 (where vPIC gives the 4Motion the 1.8L
  only, so the [2019 1.4T](volkswagen-golf-sportwagen-2019-1.4t.md) has no AWD job), vPIC records a 2020 1.4L
  with 4Motion; a SportWagen on record as front-wheel drive doesn't get the job.
- **The hood latch** isn't on the 2020 card. Both gearbox rows are left out, as on the Golf's list (the 7-speed
  DSG is "Only: Golf GTI, Jetta GLI, Golf R"; the non-DSG automatic's row names no model). The trailer hitch is
  "Tiguan and Atlas only".
- vPIC's model list for Volkswagen, 2020 (`GetModelsForMakeYear`), names it "Golf SportWagen".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36
  months by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its
  name); dust and pollen filter 40,000 / 24 mo; front axle differential lock fluid every 3 years "(if fitted)";
  spark plugs 80,000 / 72 mo; sunroof drains, front and rear, every 20,000 / 24 mo "(if fitted)".
