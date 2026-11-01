# Volkswagen Jetta 2.0T, 2010 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2010-2.0t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

The documents read are those of the [2010 Jetta 2.5L](volkswagen-jetta-2010-2.5.md): the 2010 card for all
models except the Routan (`2012/MC-10156104-9999`, the same as `2012/MC-10157646-9999`), in full, from which
the list was written, and Volkswagen's 2010 reissue (`USA5R503621`, printed 11/24/2025) for the oil change's
time limit and the engines. Read on 2026-10-28, as page images, by eye.

- **The card is read as the 2.5L's page says**, which follows the
  [Passat's](volkswagen-passat-2010-2.0t.md#how-the-2010-all-model-card-is-read). The Passat's list is the card's
  2.0T reading, so this list has the same jobs in the same columns.
- **The engine.** The card marks rows by engine, not by which model has it. The reissue's spark plug rows
  put "Jetta (1k2 - CCTA/CBFA/BPY)" and "Jetta Wag. (AJ5 - CCTA/CBFA)" with "Golf / GTI (5K-2.0T)" at every
  60,000 or 6 years, the card's "(2.0T, 3.6L and 4.2L only)" figure, and the rest of the Jetta at 40,000,
  the 2.5L's; so the 2010 Jetta had a 2.0T. vPIC's 2010 model list has no "GLI" apart from "Jetta".
- **One list per engine** (the owner's ruling of 2026-10-23). This list takes "Spark plugs: Replace (2.0T,
  3.6L and 4.2L only)" at 60,000 and 120,000 (footnote 8, every 6 years): a reminder every 60,000 miles or
  72 months. It leaves out the 2.5L / 3.2L spark plugs and every row marked 2.0L or 3.0L TDI.
  It has `"engine": { "label": "2.0T", "displacementL": 2.0, "fuel": "Gasoline" }`: the
  [TDI's list](volkswagen-jetta-2010-tdi.md) is 2.0L too, so the two are told apart by fuel, as vPIC spells
  it. A Jetta with no engine or no fuel on record is asked which it has.
- **"(if applicable)" rows are in the table only**, as on the 2.5L's list: tire pressure sensors, and the
  timing belt "(if applicable, except 2.0L TDI)" at 110,000. The Haldex row is read as on the 2.5L's list:
  "Change Haldex clutch oil, every 3 years (AWD)", in `allWheelDriveOnly`, every 36 months by time alone (#554).
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; air
  filter and spark plugs 60,000 / 72 mo; Haldex clutch oil every 36 months by time alone (AWD); brake fluid
  every 24 months by time alone (the card says at 3 years first, then every 2).
- **Left out, asked on #439: the DSG fluid change.** The automatic row is marked "2.5L Jetta models, NB and
  NBC, and Golf", so it isn't this engine's. The row "Transmission: DSG (non automatic): Change fluid and
  filter", at 40,000, 80,000 and 120,000, names no model, so read as printed it is the 2.0T Jetta's, and the
  [GTI's list](volkswagen-gti-2010-2014.md) makes the same row a `manualExcluded` reminder. But the reissue's
  gearbox table names the Jetta, by model and not engine, under `01M` and `09G` (conventional automatics) as
  well as `02E (DSG)`, and nothing on record says which a 2.0T Jetta has, so the row waits for the owner
  ([#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333)).
- Always left out: the dealer's service sticker, the New Beetle's brake fluid row, both convertible roof
  rows, and the grey "Carefree Maintenance" shading.
- **Not covered:** as on the [2.5L's page](volkswagen-jetta-2010-2.5.md).
