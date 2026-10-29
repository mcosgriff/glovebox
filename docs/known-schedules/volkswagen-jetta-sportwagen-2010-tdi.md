# Volkswagen Jetta SportWagen TDI, 2010 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-jetta-sportwagen-2010-tdi.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

The documents read are those of the [2010 Jetta SportWagen 2.5L](volkswagen-jetta-sportwagen-2010-2.5.md):
the 2010 card for all models except the Routan (`2012/MC-10156104-9999`, the same as
`2012/MC-10157646-9999`), in full, from which the list was written; Volkswagen's 2010 reissue
(`USA5R503621`, printed 11/24/2025) for the oil change's time limit only; and the 2014 Jetta SportWagen
2.0L TDI card (posted June 2013, `2013/MC-10125864-9999`) as the model's own card for the "(if applicable)"
rows. Read on 2026-10-28, as page images, by eye.

- **The card is read as the 2.5L's page says.** This list is the
  [2010 Golf TDI](volkswagen-golf-2010-tdi.md)'s reading for the Jetta SportWagen: the same rows in the same
  columns, in the same wording, apart from the particulate filter's name (below).
- **One list per engine** (the owner's ruling of 2026-10-23). It takes the rows marked 2.0L TDI: the fuel
  filter every 20,000 (`fuel-filter`, a reminder every 20,000), the timing belt at 120,000 (`timing-belt`, a
  reminder every 120,000) and the diesel particulate filter check at 120,000 (an inspection, table-only;
  "every 10,000 thereafter until replacement" is in its name, shortened to fit the 80 characters a job's name
  may have). It leaves out both spark plug rows, the non-TDI timing belt row and the 3.0L TDI rows.
  It has `"engine": { "label": "2.0L TDI", "displacementL": 2.0, "fuel": "Diesel" }`: the
  [2.0T's list](volkswagen-jetta-sportwagen-2010-2.0t.md) is 2.0L too, so the two are told apart by fuel, as
  vPIC spells it. A Jetta SportWagen with no engine or no fuel on record is asked which it has.
- **"(if applicable)" rows that the model's own cards don't have are left out**: Haldex clutch oil and the
  tire pressure sensors. The 2014 TDI card has neither.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; fuel
  filter 20,000; air filter 60,000 / 72 mo; timing belt 120,000; brake fluid every 24 months by time alone
  (the card says at 3 years first, then every 2).
- **Left out, asked on #439: the DSG fluid change**, for the reason on the
  [2.0T's page](volkswagen-jetta-sportwagen-2010-2.0t.md).
- Always left out: the dealer's service sticker, the New Beetle's brake fluid row, both convertible roof
  rows, and the grey "Carefree Maintenance" shading.
- **Not covered:**
  - **2011–2013**, as on the [2.5L's page](volkswagen-jetta-sportwagen-2010-2.5.md).
  - **2014.** The Jetta SportWagen 2.0L TDI card (`2013/MC-10125864-9999`, posted June 2013; NHTSA's index also
    files `MC-10242469`, "2.0L TDI Engine", under it, not fetched) was read in full. It is the
    [2014 Golf TDI](volkswagen-golf-2010-tdi.md) card with the model's name: a grid of 13 columns to
    130,000, where the 130,000 column has only "Timing Belt: Replace" and oil, rotation and every other job
    stop at 120,000. With that column in the table rule 7 refuses the oil and rotation reminders; without
    it, the timing belt is a job past the grid's end. It waits for the Golf's answer
    ([#434](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244), question 2).
