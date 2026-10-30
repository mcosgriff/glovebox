# Volkswagen Tiguan, 2016 (1st generation)

How `app/src/main/assets/known-schedules/volkswagen-tiguan-2016.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Tiguan card read, and what isn't covered, is on the
[2013–2015 page](volkswagen-tiguan-2013-2015.md).

| Model year | What was read | Document |
|---|---|---|
| 2016 | The Tiguan 2.0L TSI card (revised August 2015), in full, as page images; the list was written from it | `2015/MC-10123063-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The card is the [2016 CC 3.6L](volkswagen-cc-2016-3.6.md)'s grid** row for row: the same rows with the
  same marks in the same columns (a script's comparison of the two lists' tables, then the pages). So the list
  is that one's, in its wording, but for the Haldex row.
- **It differs from the 2013–2015 cards** as every 2016 card does: the yearly airbag check, tire rotation,
  door checks, the rear window washer nozzle and the service sticker are gone, and the dust and pollen filter
  is "Every 2 years or 40,000 miles" (footnote 1), drawn at 40,000, 80,000 and 120,000.
- **"Haldex Clutch: Change oil (if applicable)"**, at 30,000, 60,000, 90,000 and 120,000 with footnote 4, "Every
  3 years, regardless of mileage": "Change Haldex clutch oil, every 3 years (AWD)", in `allWheelDriveOnly`, a
  reminder every 36 months by time alone with no `service`, as on the [2013–2015 list](volkswagen-tiguan-2013-2015.md)
  and the CC 3.6L, by the owner's ruling on [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part A, confirmed for every list in
  [the second round](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511795038)). **Drive:** vPIC decodes `WVGAV7AX*G` (2016) as "Tiguan", trims "S, R-Line, SE, SEL",
  and `WVGBV7AX*G` as "Tiguan", trims "4Motion S, R-Line, SE, SEL", neither with a drive type (read 2026-10-29), so
  the 2016 Tiguan was sold with front-wheel drive and with 4Motion; see the 2013–2015 page for the years vPIC gives
  the drive. A Tiguan known to drive two wheels loses the job; one with no drive type on record keeps it.
- **No gearbox row**: the card prints none.
- Spark plugs and the air filter at 60,000 and 120,000 (footnotes 6 and 7, "every 6 years").
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; spark plugs and air filter
  60,000 / 72 mo; Haldex clutch oil every 36 months by time alone (AWD); brake fluid every 24 months by time alone
  (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No tire rotation, and no special operating conditions: the card has neither.
