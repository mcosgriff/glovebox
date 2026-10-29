# Volkswagen Touareg Hybrid, 2014 (2nd generation)

How `app/src/main/assets/known-schedules/volkswagen-touareg-2014-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md). Every Touareg card read, and what isn't covered, is on the
[2014–2015 3.6L page](volkswagen-touareg-2014-2015-3.6.md).

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Touareg 3.0L Hybrid card (posted June 2013), in full, as page images; the list was written from it. Filed twice, byte for byte the same | `2013/MC-10125857-9999`, `2013/MC-10242477-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only (not read again here) | reissue `USA5R505521` |

- **The rows are the [2014–2015 Touareg 3.6L](volkswagen-touareg-2014-2015-3.6.md)'s**, with the same marks in
  the same columns and the same footnotes, in that list's wording, and two rows of its own:
  - "Hybrid components: Perform a visual inspection of high voltage components and the lines in the engine
    compartment and the rear of the vehicle for damage", in every column: an inspection, table-only, named
    "Inspect high-voltage hybrid components and their lines for damage" as on the
    [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md).
  - "Ribbed belt for supercharger: Replace", at 50,000 and 100,000: **table-only**, named "Replace supercharger
    ribbed belt", with no `service` and no reminder. The catalog's `belt` line is the serpentine belt, which
    this car's ordinary "Ribbed belt: Check condition" row is about; logging the supercharger's belt under it
    would read as that belt replaced. There is no line for a second belt, and none is added here. Every
    50,000 would fall on the table (50,000 and 100,000), so if the owner gives it a line it can become a
    reminder every 50,000 miles.
- **No gearbox row** and no all-wheel-drive row: the card prints neither.
- `"engine": { "label": "3.0L hybrid", "displacementL": 3.0, "powertrain": "hybrid" }`: the 3.0L TDI has the
  same size, and a 3.0L that isn't on record as a hybrid never gets this list.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs and air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years
  first, then every 2).
- **Oil 12 months**, from Volkswagen's 2014 reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first columns.
- **Not covered: 2015 and 2016.** Two different cards are headed "2015 … 3.0L Hybrid" (the June 2014 card is
  this one row for row; the August 2015 card is in the 2016 layout), and none is headed 2016; see the
  [3.6L page](volkswagen-touareg-2014-2015-3.6.md#what-isnt-covered). If the owner reads the June 2014 card as
  2015's, this list becomes 2014–2015.
