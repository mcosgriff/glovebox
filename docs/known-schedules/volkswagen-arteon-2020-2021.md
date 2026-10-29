# Volkswagen Arteon, 2020–2021

How `app/src/main/assets/known-schedules/volkswagen-arteon-2020-2021.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Arteon card read, and what isn't covered, is on the
[2019 Arteon's page](volkswagen-arteon-2019.md#every-arteon-card-read).

Source: Volkswagen's all-model "Maintenance Schedule" cards, filed with NHTSA as manufacturer communications, read on
2026-10-29, in full, as page images, by eye: 2020, "2020 VW Maintenance Schedule - All Models except e-Golf", revised
11/26/2019, the newest revision, `2019/MC-10168643-0001`; 2021, "2021 VW Maintenance Schedule - All Model Gasoline
Vehicles", 7/9/2020, the only one, `2020/MC-10177239-0001` (the list's source). Cards from the time; no reissue used.

- **The cards are read as the [2020–2021 Passat's page](volkswagen-passat-2020-2021.md#how-the-20202022-all-model-cards-are-read)
  says**, and the two agree for the Arteon row for row, as they do for the Passat: the only row that names it is the
  40,000-mile spark plugs, "Except: Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI, Tiguan, Atlas, Atlas Cross Sport" (2020)
  and "Only: Arteon, Passat, Golf R" (2021). So one list covers both years.
- **The list is the [2020–2021 Passat](volkswagen-passat-2020-2021.md)'s, row for row**, with the model's name and
  the AWD clutch fluid; equally, the [2020–2021 Tiguan](volkswagen-tiguan-2020-2021.md)'s without its trailer hitch
  row ("Tiguan and Atlas only if equipped") and with spark plugs every 40,000 miles or 4 years (40,000 / 48 months).
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years": "Change AWD clutch fluid
  (AWD)", every 36 months by time alone, no service line, in `allWheelDriveOnly`, never on the grid, as on the
  2020–2021 Tiguan. vPIC decodes `WVWBR7AN*LE` (2020, "4Motion SE") as "AWD/All-Wheel Drive" (read 2026-10-29); the
  front-drive Arteon of 2019 is on NHTSA's safety-rating list, so it shows only on a car whose drive is AWD.
- **Both gearbox rows are left out.** "Transmission, 7-Speed DSG Automatic Transmission: Change fluid" is "Only: Golf
  GTI, Jetta GLI, Golf R"; "Transmission, automatic: Change fluid (excluding Canada) (only non-DSG Automatic
  Transmission)", every 80,000 miles, names no model
  ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)).
- Columns carry 12 months per 10,000 miles, as on the Passat's and GTI's 2020–2021 lists.
- vPIC's model lists for Volkswagen, 2020 and 2021 (`GetModelsForMakeYear`, read 2026-10-29), name it "Arteon". One
  engine, the 2.0T, so the list names none.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; AWD clutch fluid every 36 months
  by time alone; brake fluid every 24 months by time alone (3 years after registration first, in its name); dust and
  pollen filter 40,000 / 24 mo; front axle differential lock fluid every 3 years "(if fitted)"; spark plugs 40,000 /
  48 mo; sunroof drains, front and rear, every 20,000 / 24 mo "(if fitted)".
