# Volkswagen Jetta 2.5L, 2010 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2010-2.5.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" card for all models except the Routan, which Volkswagen
files with NHTSA as a manufacturer communication (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`;
the ids are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`), and Volkswagen's reissue of the
2010 all-model card from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=2010`, document type
"Maintenance Card", `en-US`). Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2010 | The card for all models except the Routan (revised October 2012), in full; the list was written from it. NHTSA files it under the Jetta and the Jetta SportWagen, twice ("11 models", "10 models"); the two files are byte for byte the same | `2012/MC-10156104-9999`, `2012/MC-10157646-9999` |
| 2010 | Volkswagen's 2010 reissue, for the oil change's time limit and the Jetta's engines (below) | reissue `USA5R503621`, printed 11/24/2025, `…/v1/document/14e66a09-d164-4ae6-96a7-98780d69a265` |

The Jetta's other two engines have lists of their own from the same card: the
[2.0T](volkswagen-jetta-2010-2.0t.md) and the [2.0L TDI](volkswagen-jetta-2010-tdi.md). The Jetta
SportWagen has [its own lists](volkswagen-jetta-sportwagen-2010-2.5.md).

- **The card is read as the [Passat's page](volkswagen-passat-2010-2.0t.md#how-the-2010-all-model-card-is-read)
  says**: 12 columns to 120,000 with no months, not repeated; a row is the Jetta 2.5L's when it names the
  Jetta ("Jetta models"), the 2.5L, or no model or engine; the air filter is `engine-air` (60,000 / 72 mo),
  the dust and pollen filter `cabin-air` (20,000 / 24 mo), brake fluid time-only every 24 months (footnote 6
  names "Jetta Models"), oil 12 months from the reissue (#517). The rows use the Passat list's wording.
- **The model.** NHTSA's vPIC model list for Volkswagen, 2010 (`GetModelsForMakeYear`), names "Jetta" and
  "Jetta SportWagen" apart (and no "GLI"), so this list is for "Jetta". The card says "Jetta models" and
  doesn't tell a sedan generation from another; the reissue names the 2010 Jetta by the code `1K2` and the
  wagon by `AJ5`. Nothing here is matched by generation: the list is for a 2010 "Jetta" with a 2.5L.
- **One list per engine** (the owner's ruling of 2026-10-23): every row that differs between the Jetta's
  engines carries an engine mark on the card. This list takes "Spark plugs: Replace (2.5L and 3.2L only)"
  and leaves out the rows marked 2.0L TDI (fuel filter, diesel particulate filter, timing belt), 3.0L TDI
  (AdBlue, fuel filters) and "2.0T, 3.6L and 4.2L" (spark plugs). It has
  `"engine": { "label": "2.5L", "displacementL": 2.5 }`; a Jetta with no engine on record is asked which it
  has.
- **Spark plugs 40,000 / 72 mo.** The row is drawn at 40,000, 80,000 and 120,000, and at 120,000 with
  footnote 8, "every 6 years regardless of mileage", as on the [2010 Golf 2.5L](volkswagen-golf-2010-2.5.md).
  (The reissue says every 40,000 or 4 years for the Jetta's other engines than `CCTA/CBFA/BPY`; the card
  from the time wins.)
- **"(if applicable)" rows are in the table only, named "(if applicable)"**, as on the Passat: no card of
  the Jetta sedan's own for these years was found to show they aren't its rows (the 2014 Jetta cards are
  for the later sedan, #440): tire pressure sensors at 60,000 and 120,000 (every 6 years); the timing belt
  "(if applicable, except 2.0L TDI)" at 110,000.
- **"Haldex Clutch: Change oil (if applicable)"**, drawn at 40,000, 80,000 and 120,000, is
  read as the [CC 3.6L](volkswagen-cc-2014-2015-3.6.md) reads it, by the owner's ruling on [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part A, confirmed for every list in [the second round](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511795038)): named "Change Haldex clutch oil, every 3 years (AWD)", in
  `allWheelDriveOnly`, and a reminder every 36 months by time alone, with no `service` (no catalog line names the
  job). This card prints no time for the row; the 3 years is the ruling's. The table keeps the card's columns. A
  vehicle known to drive two wheels loses it.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs 40,000 / 72 mo; air filter 60,000 / 72 mo; Haldex clutch oil every 36 months by time alone (AWD);
  brake fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Left out, asked on #439: both gearbox rows.** "Transmission: Automatic :Change fluid (2.5L Jetta models,
  NB and NBC, and Golf)" at 50,000 and 100,000, and "Transmission: DSG (non automatic): Change fluid and
  filter" at 40,000, 80,000 and 120,000 with no model mark (page 3). Read as printed, a 2010 Jetta 2.5L gets
  both, whichever gearbox it has, and `manualExcluded` can't tell a conventional automatic from a DSG. It is
  the Golf's question ([#434](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244),
  question 1); neither row is in the list until the owner decides.
- Always left out: the dealer's service sticker, the New Beetle's brake fluid row, both convertible roof
  rows, and the grey "Carefree Maintenance" shading.
- **Not covered:**
  - **2011 on.** The Jetta sedan from 2011 is the later car, with an issue of its own (#440).
  - Years before 2010: NHTSA's index has no Volkswagen card before 2010.
