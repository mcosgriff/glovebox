# Volkswagen Golf Alltrack, 2019 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-alltrack-2019.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2019 all-model "Maintenance Schedule" card, the newest revision, revised 11/12/2020,
`2020/MC-10183295-0001`, filed with NHTSA as a manufacturer communication (its index files it under the Golf
Alltrack too), read in full on 2026-10-29, as page images, by eye.

- **The list is the [2019 Golf SportWagen 1.8T](volkswagen-golf-sportwagen-2019-1.8t.md)'s, row for row**,
  with the model's name. How the card was read is on the [2019 Golf 1.4T's page](volkswagen-golf-2019-1.4t.md);
  every Mk7 card read, and what isn't covered, on the [2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).
- **Spark plugs** 40,000 / 48 mo: the 40,000-mile or 4-year row is "Except" models among which the Alltrack
  isn't, and the 80,000-mile row is "Only" those. No row names an Alltrack engine, so the list names none.
- **Left out and asked on #436**, as on the [2018 Alltrack](volkswagen-golf-alltrack-2018.md): the hood latch
  ("Does not apply to: Passat, Golf, Beetle, and Jetta") and the AWD clutch fluid ("AWD clutch: Change fluid
  (if equipped)", every 3 years; an `additional` job can't be named in `allWheelDriveOnly` without a change
  to `KnownSchedulesTest`). The Alltrack was sold with 4Motion all-wheel drive.
- Both gearbox rows are left out, as on the Golf's list.
- vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`), names it "Golf Alltrack".
- Reminders: engine oil 10,000 / 12 mo; additional items as on the SportWagen 1.8T's list.
