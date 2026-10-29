# Volkswagen Atlas 3.6L, 2018

How `app/src/main/assets/known-schedules/volkswagen-atlas-2018-3.6.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Atlas card read, and what isn't covered, is on the
[2018 2.0T's page](volkswagen-atlas-2018-2.0t.md#every-atlas-card-read).

Source: the newest revision of Volkswagen's 2018 all-model card, `2019/MC-10168646-0001` (revised 11/26/2019),
read on 2026-10-29, as page images, by eye.

- **The list is the [2018 2.0T](volkswagen-atlas-2018-2.0t.md)'s**, read the same way, with the spark plugs of the
  card's "*6-cyl Gasoline: 3.6L engines only" row: every 60,000 miles or 6 years (60,000 / 72 months), as on the
  [2018 Passat 3.6L](volkswagen-passat-2018-3.6.md).
- `"engine": { "label": "3.6L", "displacementL": 3.6 }`. vPIC decodes `1V2BR2CA*JC`, `1V2DR2CA*JC` and
  `1V2LR2CA*JC` (2018) as "Atlas", 3.6 L, 6 cylinders, "AWD/All-Wheel Drive", 8-speed "Automatic" (read
  2026-10-29); the AWD clutch fluid is in `allWheelDriveOnly`.
- **Left out, as on the 2.0T's page: both gearbox rows**
  ([#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)).
