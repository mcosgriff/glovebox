# Volkswagen Golf SportWagen 1.4T, 2019 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-sportwagen-2019-1.4t.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2019 all-model "Maintenance Schedule" card, the newest revision, revised 11/12/2020,
`2020/MC-10183295-0001`, filed with NHTSA as a manufacturer communication, read in full on 2026-10-29, as page
images, by eye.

- **The list is the [2019 Golf 1.4T](volkswagen-golf-2019-1.4t.md)'s, row for row**, with the model's name;
  that page says how the card was read. Every Mk7 card read, and what isn't covered, is on the
  [2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).
- **One list per engine.** The spark plug rows name "Golf SportWagen 1.4L" on the 80,000-mile or 6-year row
  ("Only …") and leave it out of the 40,000-mile or 4-year row ("Except …"). Every other row is the same for
  either engine. So this list has `"engine": { "label": "1.4T", "displacementL": 1.4 }` and spark plugs
  80,000 / 72 mo, and the [1.8T](volkswagen-golf-sportwagen-2019-1.8t.md) has a list of its own. A SportWagen
  with no engine on record is asked which it has.
- **Left out and asked on #436**, as on the [2018 SportWagen](volkswagen-golf-sportwagen-2018.md): the hood
  latch ("Does not apply to: Passat, Golf, Beetle, and Jetta"; whether "Golf" takes in the SportWagen isn't
  said) and the AWD clutch fluid ("AWD clutch: Change fluid (if equipped)", every 3 years; an `additional`
  job can't be named in `allWheelDriveOnly` without a change to `KnownSchedulesTest`).
- Both gearbox rows are left out, as on the Golf's list.
- vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`), names it "Golf SportWagen".
- Reminders: as the [2019 Golf 1.4T's](volkswagen-golf-2019-1.4t.md).
