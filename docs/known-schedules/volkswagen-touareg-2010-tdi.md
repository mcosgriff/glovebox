# Volkswagen Touareg TDI, 2010 (1st generation)

How `app/src/main/assets/known-schedules/volkswagen-touareg-2010-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Touareg card read, and what isn't covered, is on the
[2014–2015 3.6L page](volkswagen-touareg-2014-2015-3.6.md).

| Model year | What was read | Document |
|---|---|---|
| 2010 | The "Touareg 3.0L TDI Engine Only" card (revised October 2012), in full, as page images; the list was written from it | `2012/MC-10156105-9999` |
| 2010 | The card for all models except the Routan (revised October 2012), for comparison (below) | `2012/MC-10156104-9999` |
| 2010 | Volkswagen's 2010 reissue, as read for the [2010 Passat](volkswagen-passat-2010.md), for the oil change's time limit; it names the Touareg (`7L`) | reissue `USA5R503621`, printed 11/24/2025 |

- **A grid of 12 columns, 10,000 to 120,000**, no months printed for a column, not repeated (rule 9). No
  130,000 column and no timing belt row.
- **The AdBlue check between the columns.** "AdBlue® Fluid: Check fluid level and add if necessary" is drawn in
  every column, and the card's notes add it "At 5K, 15K, 25K, 35K" and "At 45K, 55K, 65K, 75K, 85K, 95K, 105K,
  115K, 125K and every 5K after". The table has it at every 5,000 from 5,000 to 120,000: the points between the
  columns are milestones of their own with only that check. It is an inspection, so table-only. **125,000 and
  "every 5,000 after" are past the grid's end** and aren't in the table (rule 9); asked on
  [#451](https://github.com/mcosgriff/glovebox/issues/451).
- Rows and names, in the wording of the [2010 Jetta TDI](volkswagen-jetta-2010-tdi.md) list where it is the same
  job:
  - "Dust and pollen filter: Replace" at every 20,000, **with no time limit**: this card has no "every 2 years"
    note for it (the all-model card's row has one; below). So the reminder is every 20,000 miles with no months.
  - "Fuel filter: Replace" at 40,000, 80,000 and 120,000: the `fuel-filter` service, every 40,000 miles.
  - "Fuel filter: Drain water" at 20,000, 60,000 and 100,000: table-only, "Drain water from diesel fuel filter".
    It isn't a filter change, so it doesn't take the `fuel-filter` line, and there is no line for it.
  - "Tire pressure sensors: Replace (Every 6 years regardless of mileage)" at 60,000 and 120,000 (footnote 6),
    printed for this car with no "(if applicable)": the `tpms` service, every 60,000 miles or 72 months, read as
    the air filter's row with the same note is (60,000 / 72).
  - "Diesel particle filter", at 120,000 (after 120,000 miles, then every 10,000 until it is replaced): table-only,
    the "every 10,000" in its name.
  - "Haldex Clutch: Change oil (if applicable)" at 40,000, 80,000 and 120,000: table-only, "Change Haldex clutch
    oil (if applicable)", as on the 2010 Passat list; the "(if applicable)" rule holds although every Touareg
    has 4Motion (the open question on [#446](https://github.com/mcosgriff/glovebox/issues/446#issuecomment-6490065640)
    is whether such a row should be an all-wheel-drive reminder).
  - "Second battery: Check (if applicable)" and "Transmission: Automatic: Check fluid" (40,000, 80,000, 120,000):
    inspections, table-only.
- **The all-model card differs** on the Touareg TDI's rows and isn't used for this list (the Touareg TDI's own
  card is the more particular one): its dust and pollen filter row is "(if applicable)" with footnote 4, "every
  2 years"; it has a sunroof water drain check at 40,000, 80,000 and 120,000, which this card hasn't; and its
  tire pressure sensor and timing belt rows are "(if applicable)" (this card has the sensors for every
  Touareg TDI and no timing belt). Its 3.0L TDI rows (AdBlue, fuel filter drain and change, the particulate
  filter) agree with this card. Whether the dust and pollen filter should take the all-model card's 24 months
  is asked on [#451](https://github.com/mcosgriff/glovebox/issues/451).
- `"engine": { "label": "3.0L TDI", "displacementL": 3.0, "fuel": "Diesel" }`. No 2010 Touareg had a 3.0L
  hybrid, but the fuel is set as on the later TDI lists.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000; fuel filter 40,000;
  air filter and tire pressure sensors 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says
  at 3 years first, then every 2).
- **Oil 12 months**, from the 2010 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first three columns.
