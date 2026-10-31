# Volkswagen Jetta SportWagen 2.5L, 2010 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-jetta-sportwagen-2010-2.5.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`), and Volkswagen's reissue of the 2010
all-model card from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=2010`, document type
"Maintenance Card", `en-US`). Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2010 | The card for all models except the Routan (revised October 2012), in full; the list was written from it. NHTSA files it under the Jetta SportWagen, twice ("11 models", "10 models"); the two files are byte for byte the same | `2012/MC-10156104-9999`, `2012/MC-10157646-9999` |
| 2010 | Volkswagen's 2010 reissue, for the oil change's time limit and the engines | reissue `USA5R503621`, printed 11/24/2025, `…/v1/document/14e66a09-d164-4ae6-96a7-98780d69a265` |
| 2014 | The Jetta SportWagen 2.5L card (posted September 2013) and the 2.0L TDI card (posted June 2013), as the model's own cards for the "(if applicable)" rows (below). The 2.5L card has [a list of its own](volkswagen-jetta-sportwagen-2014-2.5.md) | `2013/MC-10155822-9999`, `2013/MC-10125864-9999` |

The Jetta SportWagen's other engines have lists of their own from the same card: the
[2.0T](volkswagen-jetta-sportwagen-2010-2.0t.md) and the [2.0L TDI](volkswagen-jetta-sportwagen-2010-tdi.md).
The sedan has [its own lists](volkswagen-jetta-2010-2.5.md).

- **The card is read as the [Passat's page](volkswagen-passat-2010-2.0t.md#how-the-2010-all-model-card-is-read)
  says**, and this list is the [2010 Golf 2.5L](volkswagen-golf-2010-2.5.md)'s reading for the Jetta
  SportWagen: the same rows in the same columns, in the same wording. The card marks no row for the Golf
  and not the Jetta or the other way round, apart from the automatic gearbox row, which names both ("2.5L
  Jetta models … and Golf") and is left out of both (below); brake fluid's footnote 6 names "Jetta Models"
  and the Golf alike.
- **The model.** NHTSA's vPIC model list for Volkswagen, 2010 (`GetModelsForMakeYear`), names "Jetta
  SportWagen" apart from "Jetta", so this list is for "Jetta SportWagen". It is not an alias of the sedan's
  list: vPIC gives the two their own names, and the sedan's list differs (below).
- **One list per engine** (the owner's ruling of 2026-10-23): spark plugs "(2.5L and 3.2L only)" at 40,000,
  80,000 and 120,000 (footnote 8 at 120,000, every 6 years), a reminder every 40,000 miles or 72 months; the
  rows marked for other engines are left out. It has `"engine": { "label": "2.5L", "displacementL": 2.5 }`; a
  Jetta SportWagen with no engine on record is asked which it has.
- **"(if applicable)" rows that the model's own cards don't have are left out**, as the Golf's and the GTI's
  lists do: Haldex clutch oil, the tire pressure sensors and the timing belt "(if applicable, except 2.0L
  TDI)". Neither 2014 Jetta SportWagen card (2.5L, 2.0L TDI) has any of them. This is where it differs from
  the [sedan's list](volkswagen-jetta-2010-2.5.md), which has no card of its own for these years and keeps
  them in the table, as the Passat's does.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs 40,000 / 72 mo; air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says
  at 3 years first, then every 2).
- **Left out, asked on #439: both gearbox rows**, as on the Golf
  ([#434](https://github.com/mcosgriff/glovebox/issues/434#issuecomment-6488760244), question 1): the
  automatic's fluid change is marked "2.5L Jetta models", the DSG's names every model, and the format can't
  tell the two gearboxes apart. (The 2014 2.5L card has only the conventional automatic's row; that is a
  later year, so it isn't taken for 2010.)
- Always left out: the dealer's service sticker, the New Beetle's brake fluid row, both convertible roof
  rows, and the grey "Carefree Maintenance" shading.
- **Not covered:**
  - **2011–2013.** NHTSA's index files no Jetta SportWagen card for these years. One 2.5L card filed under
    2014 (`2013/MC-10125863-9999`, posted June 2013) is headed "2013 Volkswagen Jetta SportWagen" on every
    page; whether it is a 2013 card is asked on #439 (see the
    [2014 list's page](volkswagen-jetta-sportwagen-2014-2.5.md)). Volkswagen's 2011–2013 reissues weren't
    read for this list; whether to build years from reissues alone is the Golf's question 3 on #434.
  - **2014** has [its own list](volkswagen-jetta-sportwagen-2014-2.5.md).
  - Years before 2010 (NHTSA's index has no Volkswagen card before 2010), and the Golf SportWagen from 2015,
    which has an issue of its own (#147).
