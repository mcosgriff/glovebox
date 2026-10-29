# Volkswagen Golf Alltrack, 2018 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-alltrack-2018.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2018 all-model "Maintenance Schedule" card, the newest revision, revised 11/26/2019,
`2019/MC-10168646-0001`, filed with NHTSA as a manufacturer communication (its index files it under the
Golf Alltrack too), read in full on 2026-10-29, as page images, by eye.

- **The list is the [2018 Golf](volkswagen-golf-2018.md)'s, row for row**, with the model's name; that page
  says how the card was read. Every Mk7 card read, and what isn't covered, is on the
  [2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md). The card names the Golf Alltrack on no row. One row is read
  differently from the Golf's, and left out and asked on #436:
  - **The hood latch.** "Hood latch: Lubricate (Does not apply to: Passat, Golf, Beetle, and Jetta)". Whether
    "Golf" takes in the Golf Alltrack isn't said; it is left out, as on the [2018 GTI](volkswagen-gti-2018.md#needs-a-decision-left-out).
- **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years (AWD only vehicles)",
  an additional item. The 2018 Golf Alltrack was sold with 4Motion all-wheel drive, so by
  [the 2018 card's reading](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read) (point 8) it is
  "Change AWD clutch fluid (AWD)", an `additional` reminder by time alone, every 36 months, with no service line
  (none is for an AWD coupling), in `allWheelDriveOnly`, never placed on the grid. vPIC decodes the pattern
  `3VWH17AU*JM` (2018 Golf Alltrack, S, SE, SEL, read 2026-10-29) as "AWD/All-Wheel Drive", so a 4Motion car decoded
  by VIN keeps it, as does one with no drive on record; only one known to drive two wheels loses it. (The
  first list left it out only because `KnownSchedulesTest`'s AWD check didn't count `additional` jobs, #436
  question 1; that check now counts them.)
- **Both gearbox rows are left out**, as on the Golf's list.
- **Model name.** vPIC's model list for Volkswagen, 2018 (`GetModelsForMakeYear`, read 2026-10-29), names it
  "Golf Alltrack", apart from "Golf", so it has a list of its own. One row of spark plugs covers its engine, so the list
  names none.
- Reminders: as the [2018 Golf's](volkswagen-golf-2018.md).
