# Volkswagen Golf 2.5L, 2010 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-golf-2010-2.5.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`), and Volkswagen's reissue of the 2010
all-model card from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=2010`, document type
"Maintenance Card", `en-US`). Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2010 | The card for all models except the Routan (revised October 2012), in full; the list was written from it. NHTSA files it twice ("11 models", "10 models"); the two files are byte for byte the same | `2012/MC-10156104-9999`, `2012/MC-10157646-9999` |
| 2010 | Volkswagen's 2010 reissue, for the oil change's time limit only | reissue `USA5R503621`, printed 11/24/2025, `…/v1/document/14e66a09-d164-4ae6-96a7-98780d69a265` |
| 2014 | The Golf 2.5L card (posted October 2013), as the model's own card for the "(if applicable)" rows (below). It has [a list of its own](volkswagen-golf-2014-2.5.md) | `2013/MC-10155949-9999` |

The Golf's 2.0L TDI has [a list of its own](volkswagen-golf-2010-tdi.md) from the same card.

- **The 2010 card is read as the [Passat's page](volkswagen-passat-2010-2.0t.md#how-the-2010-all-model-card-is-read)
  says**: 12 columns to 120,000 with no months, not repeated; a row is the Golf 2.5L's when it names the
  Golf, the 2.5L, or no model or engine; the air filter is `engine-air` (60,000 / 72 mo), the dust and
  pollen filter `cabin-air` (20,000 / 24 mo), brake fluid time-only every 24 months, oil 12 months from the
  reissue. The rows that are also on the [2010–2014 GTI](volkswagen-gti-2010-2014.md) use that list's
  wording.
- **One list per engine** (the owner's ruling of 2026-10-23): every row that differs between the Golf's
  two engines carries an engine mark on the card. This list takes "Spark plugs: Replace (2.5L and 3.2L
  only)" and leaves out the rows marked 2.0L TDI (the fuel filter, the diesel particulate filter, the
  2.0L TDI timing belt) and the other engines' rows (3.0L TDI AdBlue and fuel filters; the 2.0T, 3.6L and
  4.2L spark plugs). It has `"engine": { "label": "2.5L", "displacementL": 2.5 }`; a Golf with no engine on
  record is asked which it has. NHTSA's vPIC model list for Volkswagen, 2010 (`GetModelsForMakeYear`),
  names it "Golf", apart from "GTI".
- **Spark plugs 40,000 / 72 mo.** The row is drawn at 40,000, 80,000 and 120,000, and at 120,000 with
  footnote 8, "every 6 years regardless of mileage", as the GTI's 2.0T row is at 120,000. (Volkswagen's
  reissue says every 40,000 or 4 years for the "Golf (2.5l)"; the card from the time wins.)
- **"(if applicable)" rows that the Golf's own cards don't have are left out**, as the GTI's list does on
  the evidence of its 2012 and 2014 cards: Haldex clutch oil, the tire pressure sensors and the timing belt
  "(if applicable, except 2.0L TDI)". Neither 2014 Golf card (2.5L, 2.0L TDI) has any of them. The rear
  washer nozzle, power steering, the sunroof rows and the tire mobility kit are on the Golf's cards; they
  are named as on the GTI's list.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs 40,000 / 72 mo; air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says
  at 3 years first, then every 2).
- **Left out, asked on #434: both gearbox rows.** The card has "Transmission: Automatic: Change fluid
  (2.5L Jetta models, NB and NBC, and Golf)" at 50,000 and 100,000, and "Transmission: DSG (non
  automatic): Change fluid and filter" at 40,000, 80,000 and 120,000 for every model (page 3). Read as
  printed, a 2010 Golf gets both, whichever gearbox it has. `manualExcluded` would give both jobs to every
  Golf that isn't a manual, and nothing on record tells a conventional automatic from a DSG, so neither row
  is in the list until the owner decides. (The 2014 2.5L card has only the conventional automatic's row;
  that is a later year, so it isn't taken for 2010.)
- Always left out: the dealer's service sticker, the New Beetle's brake fluid row, both convertible roof
  rows, and the grey "Carefree Maintenance" shading.
- **Not covered:**
  - **2011–2013.** No Golf card from the time was found; NHTSA's index has none for these years, and
    Volkswagen's reissues weren't read for this list. They would be its only source (the owner's ruling of
    2026-10-23 allows that), but the 2010 reissue's gearbox table names the Golf under five gearboxes at two
    intervals, by code, and gives spark plugs and the dust and pollen filter other intervals than the
    cards. Whether to build 2011–2013 from reissues alone is asked on #434.
  - **2014** has [its own list](volkswagen-golf-2014-2.5.md).
  - Years before 2010 (NHTSA's index has no Volkswagen card before 2010) and the Mk7 (2015 on), which has
    an issue of its own (#147).
