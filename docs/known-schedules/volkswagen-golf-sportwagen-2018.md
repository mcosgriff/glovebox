# Volkswagen Golf SportWagen, 2018 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-sportwagen-2018.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2018 all-model "Maintenance Schedule" card, the newest revision, revised 11/26/2019,
`2019/MC-10168646-0001`, filed with NHTSA as a manufacturer communication (its index files it under the
Golf SportWagen too), read in full on 2026-10-29, as page images, by eye.

- **The list is the [2018 Golf](volkswagen-golf-2018.md)'s, row for row**, with the model's name; that page
  says how the card was read. Every Mk7 card read, and what isn't covered, is on the
  [2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md). The card names the Golf SportWagen on no row. Two rows are read
  differently from the Golf's, and both are left out and asked on #436:
  - **The hood latch.** "Hood latch: Lubricate (Does not apply to: Passat, Golf, Beetle, and Jetta)". Whether
    "Golf" takes in the Golf SportWagen isn't said; it is left out, as on the [2018 GTI](volkswagen-gti-2018.md#needs-a-decision-left-out).
  - **The AWD clutch fluid.** "AWD clutch: Change fluid (if equipped)", "Every 3 years (AWD only vehicles)",
    an additional item. The 2018 Golf SportWagen was sold with front-wheel drive and with 4Motion all-wheel drive, so by
    [the 2018 card's reading](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read) (point 8) it is an
    `additional` reminder by time alone, every 36 months, in `allWheelDriveOnly`, named "(AWD)". **It isn't
    in the list:** `KnownSchedulesTest` holds `allWheelDriveOnly` to the "(AWD)" names of the table and the
    intervals only, so an `additional` job named "(AWD)" fails the build, and changing the test is a code
    change this list doesn't make. A 4Motion owner should know the job is on the card.
- **Both gearbox rows are left out**, as on the Golf's list.
- **Model name.** vPIC's model list for Volkswagen, 2018 (`GetModelsForMakeYear`, read 2026-10-29), names it
  "Golf SportWagen", apart from "Golf", so it has a list of its own. One row of spark plugs covers its engine, so the list
  names none.
- Reminders: as the [2018 Golf's](volkswagen-golf-2018.md).
