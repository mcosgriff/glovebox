# Volkswagen Beetle 2.0T, 2014 (A5)

How `app/src/main/assets/known-schedules/volkswagen-beetle-2014-2.0t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Beetle card read, how the Beetle and Beetle Convertible
cards are put into one list, and what isn't covered are on the [2014 2.5L's page](volkswagen-beetle-2014-2.5.md).

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Beetle 2.0L TSI card (posted June 2013), in full; the list was written from it | `2013/MC-10125789-9999` |
| 2014 | The Beetle Convertible 2.0TSI card (posted June 2013), in full: the same rows, marks and footnotes, with the two convertible roof rows | `2013/MC-10125792-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only (see the 2.5L's page) | reissue `USA5R505521` |

- **The card is the [2014–2015 CC 2.0T](volkswagen-cc-2014-2015-2.0t.md)'s grid**: the same rows with the same
  marks in the same columns, DSG row included (a script's comparison of the two lists' tables, then the pages),
  with the convertible's two roof rows. So the list is that one's, in its wording, with the roof rows (table-only,
  named for the Beetle Convertible).
- "Transmission: DSG (non Automatic): Change fluid and filter (if applicable)" at 40,000, 80,000 and 120,000,
  and no automatic's row: `trans-fluid`, every 40,000 miles, "(DSG only)", in `manualExcluded`, as on the CC's
  list.
- `"engine": { "label": "2.0T", "displacementL": 2.0, "fuel": "Gasoline" }`: the 2014 Beetle's 2.0L TDI is the
  same size, so the two are told apart by fuel, as vPIC spells it. A Beetle with no engine or fuel on record is
  asked which it has.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; DSG fluid
  and filter 40,000 (not for a manual); spark plugs and air filter 60,000 / 72 mo (footnotes, "every 6 years");
  brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, the empty timing belt row, and the grey "Carefree Maintenance" shading.
- **Not covered:** 2015, which has two different Beetle 2.0L TSI cards headed 2015, and 2016, which has none
  headed 2016 (see the 2.5L's page; asked on #448). The June 2014 card headed 2015 and the 2015 convertible
  card are row for row this one.
