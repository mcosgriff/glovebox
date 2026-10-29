# Volkswagen Touareg TDI, 2016 (2nd generation)

How `app/src/main/assets/known-schedules/volkswagen-touareg-2016-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Touareg card read, and what isn't covered, is on the
[2014–2015 3.6L page](volkswagen-touareg-2014-2015-3.6.md).

| Model year | What was read | Document |
|---|---|---|
| 2016 | The Touareg 3.0L TDI card (posted August 2015), in full, as page images; the list was written from it | `2015/MC-10123061-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit only (not read again here) | reissue `USA5R506521` |

- **The rows are the [2016 Touareg 3.6L](volkswagen-touareg-2016-3.6.md)'s**, in that list's wording, with the
  spark plugs left out (the card has none) and the diesel's rows read as on the
  [2014–2015 Touareg TDI](volkswagen-touareg-2014-2015-tdi.md): the AdBlue check in every column (footnote 5,
  the change at 4 years under 10,000 miles, recorded, not in the list), the fuel filter every 20,000, and the
  diesel particulate filter check at 120,000 ("then every 20,000"). 12 columns to 120,000; no timing belt.
- **No gearbox row** and no all-wheel-drive row: the card prints neither.
- `"engine": { "label": "3.0L TDI", "displacementL": 3.0, "fuel": "Diesel" }`.
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; fuel filter 20,000; air filter
  60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from Volkswagen's 2016 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
