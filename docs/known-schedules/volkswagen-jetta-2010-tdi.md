# Volkswagen Jetta TDI, 2010 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2010-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

The documents read are those of the [2010 Jetta 2.5L](volkswagen-jetta-2010-2.5.md): the 2010 card for all
models except the Routan (`2012/MC-10156104-9999`, the same as `2012/MC-10157646-9999`), in full, from which
the list was written, and Volkswagen's 2010 reissue (`USA5R503621`, printed 11/24/2025) for the oil change's
time limit only. Read on 2026-10-28, as page images, by eye.

- **The card is read as the 2.5L's page says**, which follows the
  [Passat's](volkswagen-passat-2010-2.0t.md#how-the-2010-all-model-card-is-read). The TDI rows are those of the
  [2010 Golf TDI](volkswagen-golf-2010-tdi.md), from the same card.
- **One list per engine** (the owner's ruling of 2026-10-23). This list takes the rows marked for the
  2.0L TDI and leaves out the rows marked for other engines only:
  - "Fuel filter: Replace (2.0L TDI only)" at every 20,000: the `fuel-filter` service, a reminder every
    20,000 miles.
  - "Timing belt: Replace (2.0L TDI only)" at 120,000: the `timing-belt` service, a reminder every 120,000
    miles (the grid's only multiple of it). The row "Timing belt: Replace (if applicable, except 2.0L TDI)"
    at 110,000 is left out by its own mark.
  - "Diesel particulate filter … (2.0L TDI and 3.0L TDI only)" at 120,000: an inspection, so table-only
    (rule 7). Its "every 10,000 miles thereafter until replacement" falls past the grid's end, so it is in
    the job's name, shortened to fit the 80 characters a job's name may have.
  - Left out: both spark plug rows (2.5L and 3.2L; 2.0T, 3.6L and 4.2L), AdBlue and both fuel filter rows
    marked 3.0L TDI.
  It has `"engine": { "label": "2.0L TDI", "displacementL": 2.0, "fuel": "Diesel" }`: the
  [2.0T's list](volkswagen-jetta-2010-2.0t.md) is 2.0L too, so the two are told apart by fuel, as vPIC
  spells it. A Jetta with no engine or no fuel on record is asked which it has.
- **"(if applicable)" rows are in the table only**, as on the 2.5L's list: the tire pressure sensors. The
  Haldex row is read as on the 2.5L's list: "Change Haldex clutch oil, every 3 years (AWD)", in
  `allWheelDriveOnly`, every 36 months by time alone (#554).
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; fuel
  filter 20,000; air filter 60,000 / 72 mo; timing belt 120,000; Haldex clutch oil every 36 months by time
  alone (AWD); brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Left out, asked on #439: the DSG fluid change**, for the reason on the
  [2.0T's page](volkswagen-jetta-2010-2.0t.md): the automatic row is marked for the 2.5L Jetta only and the
  DSG row names no model, but the reissue's gearbox table names the Jetta under conventional automatics as
  well as the DSG, by model, and nothing on record says which a TDI has.
- **The reissue differs** and isn't used apart from the oil's months (a card from the time wins): it gives
  the Jetta's diesel timing belt every 130,000 ("Jetta/Variant (from 2010)"), the diesel fuel filter every
  60,000, and the particulate filter check at 100,000 then every 20,000.
- Always left out: the dealer's service sticker, the New Beetle's brake fluid row, both convertible roof
  rows, and the grey "Carefree Maintenance" shading.
- **Not covered:** as on the [2.5L's page](volkswagen-jetta-2010-2.5.md).
