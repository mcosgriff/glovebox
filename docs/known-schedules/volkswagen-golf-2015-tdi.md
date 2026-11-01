# Volkswagen Golf TDI, 2015 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-2015-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). The [2015 Golf SportWagen TDI](volkswagen-golf-sportwagen-2015-tdi.md)
is made from the same card. Every Mk7 card read, and what isn't covered, is on the
[2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, filed with NHTSA as manufacturer communications.
Read on 2026-10-29, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2015 | "Golf and Golf SportWagen", 2.0L TDI (posted December 2014), in full; the list was written from it | `2014/MC-10242448-9999` |
| 2015 | "Golf", 2.0L TDI (posted June 2014). Apart from the title and the posting date it is the same card, pixel for pixel | `2014/MC-10120119-9999` |
| 2015 | Volkswagen's 2015 reissue, for the oil change's time limit only, as read for the [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) (not read again here) | reissue `USA5R505821`, printed 11/24/2025 |

- **The card is the [2014 Golf TDI](volkswagen-golf-2014-tdi.md)'s grid with an AdBlue row**: the same
  rows, worded the same, with the same marks in the same columns, read off the page and checked row by row,
  and "AdBlue® Fluid: Check fluid level and add if necessary" in every column. Its footnotes are numbered
  one higher from the AdBlue note on. So the list is that one's, in that list's wording, with the AdBlue
  check added, and is read as that page says:
  - **The 130,000 column is left out, and the timing belt is an `additional` reminder every 130,000 miles**
    (the owner's ruling of 2026-10-29 on [#551](https://github.com/mcosgriff/glovebox/issues/551), part A,
    option (a)). That column holds only "AdBlue: Check fluid level and add if necessary" and "Timing Belt:
    Replace"; every other job, the oil change included, stops at 120,000. The ruling says the AdBlue check
    there goes with the column: it is an inspection, so table-only.
  - The AdBlue check is in the table at every column from 10,000 to 120,000, as "Check AdBlue level and top
    up", with no reminder (an inspection). Footnote 6 at 40,000, 80,000 and 120,000, "At the 4 year mark, if
    the mileage is under 10,000 miles: AdBlue®: Change fluid", is a change on a condition the format can't
    hold; it is recorded here, not put in the list, as on the [Passat TDI's page](volkswagen-passat-2014-2015-tdi.md).
  - Fuel filter every 20,000 (`fuel-filter`, a reminder); the particulate filter check at 120,000
    (table-only); the DSG row "(if applicable)" at 40,000, 80,000 and 120,000, table-only with no `service`.
- `"engine": { "label": "2.0L TDI", "displacementL": 2.0, "fuel": "Diesel" }`. vPIC's 2015 model list has no
  "Golf R", and a 2015 Golf R's VIN decodes to a 2.0L petrol engine with no model (see the
  [2015 Golf R's page](volkswagen-golf-r-2015.md)); one that is on record as "Golf" with that engine must not
  be given the diesel's list, so the list names its fuel. A 2.0L Golf with no fuel on record is asked which
  engine it has; a Golf with no engine on record is asked "1.8T", "2.0L TDI" or "Neither".
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter and fuel filter 20,000
  (the dust filter or 24 mo); air filter 60,000 / 72 mo; timing belt 130,000 (apart from the table); brake
  fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2015 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first column.
  No special operating conditions are given on the card.
