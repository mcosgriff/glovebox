# Volkswagen Golf SportWagen, 2018 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-sportwagen-2018.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2018 all-model "Maintenance Schedule" card, the newest revision, revised 11/26/2019,
`2019/MC-10168646-0001`, filed with NHTSA as a manufacturer communication (its index files it under the
Golf SportWagen too), read in full on 2026-10-29, as page images, by eye.

- **The list is the [2018 Golf](volkswagen-golf-2018.md)'s, row for row**, with the model's name; that page
  says how the card was read. Every Mk7 card read, and what isn't covered, is on the
  [2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md). The card names the Golf SportWagen on no row. One row needed a
  ruling:
  - **The hood latch.** "Hood latch: Lubricate (Does not apply to: Passat, Golf, Beetle, and Jetta)". It is left
    out, by the owner's ruling on [#555](https://github.com/mcosgriff/glovebox/issues/555#issuecomment-6511795335): "Golf" covers the whole Golf family (SportWagen, Alltrack, Golf R).
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years (AWD only vehicles)",
  an additional item. The 2018 Golf SportWagen was sold with front-wheel drive and with 4Motion all-wheel drive, so by
  [the 2018 card's reading](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read) (point 8) it is
  "Change AWD clutch fluid (AWD)", an `additional` reminder by time alone, every 36 months, with no service line
  (none is for an AWD coupling), in `allWheelDriveOnly`, never placed on the grid. vPIC decodes the pattern
  `3VW117AU*JM` (2018 Golf SportWagen, 4Motion S, read 2026-10-29) as "AWD/All-Wheel Drive", so a 4Motion car decoded
  by VIN keeps it, as does one with no drive on record; only one known to drive two wheels loses it. (The
  first list left it out only because `KnownSchedulesTest`'s AWD check didn't count `additional` jobs, #436
  question 1; that check now counts them.)
- **Both gearbox rows are left out**, as on the Golf's list.
- **Model name.** vPIC's model list for Volkswagen, 2018 (`GetModelsForMakeYear`, read 2026-10-29), names it
  "Golf SportWagen", apart from "Golf", so it has a list of its own. One row of spark plugs covers its engine, so the list
  names none.
- Reminders: as the [2018 Golf's](volkswagen-golf-2018.md).
