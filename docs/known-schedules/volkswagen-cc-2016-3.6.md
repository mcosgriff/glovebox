# Volkswagen CC 3.6L, 2016

How `app/src/main/assets/known-schedules/volkswagen-cc-2016-3.6.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every CC card read, and what isn't covered, is on the
[2014–2015 2.0T's page](volkswagen-cc-2014-2015-2.0t.md).

| Model year | What was read | Document |
|---|---|---|
| 2016 | The CC 3.6L FSI card (posted August 2015), in full; the list was written from it | `2015/MC-10123078-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only, as read for the [2016 Jetta Hybrid](volkswagen-jetta-2016-hybrid.md) (not read again here) | reissue `USA5R506521`, printed 11/25/2025 |

- **The card is the [2016 CC 2.0T](volkswagen-cc-2016-2.0t.md)'s grid without the DSG row and with the Haldex
  clutch row** (30,000, 60,000, 90,000 and 120,000; footnote 4, "Every 3 years, regardless of mileage"); every
  other row and mark is the same. The Haldex job is read as on the [2014–2015 3.6L](volkswagen-cc-2014-2015-3.6.md):
  "Change Haldex clutch oil, every 3 years (AWD)", in `allWheelDriveOnly`, a reminder every 36 months by time alone.
- **No gearbox row**: the card prints none.
- `"engine": { "label": "3.6L", "displacementL": 3.6 }`.
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; spark plugs and air filter
  60,000 / 72 mo; Haldex clutch oil every 36 months by time alone (AWD); brake fluid every 24 months by time
  alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- No tire rotation, and no special operating conditions: the card has neither.
