# Volkswagen Passat, 2010 (B6)

How `app/src/main/assets/known-schedules/volkswagen-passat-2010.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" card for all models except the Routan, which Volkswagen
files with NHTSA as a manufacturer communication (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`;
the ids are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`), and Volkswagen's reissue of the
2010 all-model card from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=2010`, document type
"Maintenance Card", `en-US`). Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2010 | The card for all models except the Routan (revised October 2012), in full; the list was written from it. NHTSA's index lists it twice, as "11 models" and "10 models"; the two files are byte for byte the same | `2012/MC-10156104-9999`, `2012/MC-10157646-9999` |
| 2010 | Volkswagen's 2010 reissue, for the oil change's time limit and for comparison (below). Its model codes include the Passat (`3C2`) and Passat Variant (`3C5`) | reissue `USA5R503621`, printed 11/24/2025, `…/v1/document/14e66a09-d164-4ae6-96a7-98780d69a265` |

## How the 2010 all-model card is read

The same card covers the 2010 Beetle, CC, Eos, Golf, GTI, Jetta, Jetta SportWagen, Passat, Tiguan and
Touareg. The [2010–2014 GTI](volkswagen-gti-2010-2014.md) list read it first; this list reads it the same
way, and a later list from this card should too:

- **A grid of 12 columns**, 10,000 to 120,000 miles, no months printed for a column. Nothing says it
  repeats, so the table doesn't (rule 9). Jobs "regardless of mileage" are drawn in the columns Volkswagen
  chose and named with their time. The grey "Carefree Maintenance" shading of the first three columns is
  left out.
- **A row is the model's when** it names the model, an engine the model has, or no model at all. A row
  marked for other models or engines only ("New Beetle only", "3.0L TDI only", "2.5L Jetta models, NB and
  NBC, and Golf") is left out.
- **"Air filter element and snow filter: Replace and clean housing (if applicable)"**, footnote 9, every
  6 years: read as the engine air filter, which every petrol car has, with "(if applicable)" for the snow
  filter. So it has the `engine-air` service, 60,000 / 72 months, as on the GTI list.
- **"Dust and pollen filter: Replace (if applicable)"**, footnote 4, every 2 years: the `cabin-air`
  service, 20,000 / 24 months, as on the GTI list.
- **Brake fluid**, footnote 6 ("Every 3 years, regardless of mileage and every 2 years after", for the
  Touareg, Eos, Passat models, Jetta models, GTI, Golf, Tiguan and CC): a time-only reminder every
  24 months, the first 3 years in its name, drawn at 30,000, 50,000, 70,000, 90,000 and 110,000.
- **Oil 12 months.** The card prints no months. The reissue's first minor service is at 10,000 miles or
  1 year after delivery, then every 20,000 or 2 years, and its standard service every 20,000 or 2 years,
  both changing the oil: so oil every 10,000 or about once a year. The oil reminder takes 12 months (the
  owner's ruling of 2026-10-28, #517); the table has none. Nothing else is taken from the reissue.
- **"(if applicable)" rows that name no drivetrain or engine are table-only** (no `service`, no
  reminder), unless a card for the model itself shows they aren't its rows. The GTI list left out Haldex
  clutch oil, the timing belt and the tire pressure sensors on the evidence of its 2012 and 2014 cards; the
  Passat has no such card, so here they are in the table, named "(if applicable)". The rear washer nozzle,
  power steering, the sunroof rows and the tire mobility kit are named "(if fitted)" and are inspections
  or table-only anyway.
- **"Transmission: DSG (non automatic): Change fluid and filter"** at 40,000, 80,000 and 120,000 is a
  `manualExcluded` reminder only where the model has no conventional automatic beside its DSG (the GTI).
  Where it has both, the format can't tell them apart (below).
- Always left out: the dealer's service sticker, and the convertible roof rows on a model without one.

## This list

- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; air
  filter and spark plugs 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3
  years first, then every 2).
- Spark plugs: the row "(2.0T, 3.6L and 4.2L only)", 60,000 and 120,000, footnote 8 every 6 years. The
  "(2.5L and 3.2L only)" row is left out.
- Table-only "(if applicable)" rows: Haldex clutch oil at 40,000, 80,000 and 120,000; tire pressure sensors
  at 60,000 and 120,000 (every 6 years); the timing belt "(if applicable, except 2.0L TDI)" at 110,000.
- **The model.** NHTSA's vPIC model list for Volkswagen, 2010 (`GetModelsForMakeYear`), names it "Passat",
  so the list is for that model, with no `engine`. The list has no "CC" for 2010.
  A vPIC decode of the CC's VIN pattern for 2010 names it "Passat" too, so a 2010 CC gets this list (see the
  [CC's page](volkswagen-cc-2014-2015-2.0t.md#what-isnt-covered)).
- Left out as not the Passat's, by the card's own marks: AdBlue (3.0L TDI), the diesel particulate filter
  (2.0L and 3.0L TDI), the three fuel filter rows (2.0L / 3.0L TDI), the 2.0L TDI timing belt, the New
  Beetle's brake fluid, the 2.5L / 3.2L spark plugs, the automatic gearbox fluid (2.5L Jetta, New Beetle and
  convertible, Golf), and both convertible roof rows. The card names no Passat TDI; if one were on record it
  would still get this list, without those rows (asked on #443).
- **Left out, asked on #443: the DSG fluid change.** The card's row "Transmission: DSG (non automatic):
  Change fluid and filter" (page 3) is drawn at 40,000, 80,000 and 120,000 for every model. The reissue's
  gearbox rows (page 5) give the Passat two automatic gearboxes: `09M` every 80,000 and `02E (DSG)` every
  40,000. `manualExcluded` would give the DSG job to a Passat with the conventional `09M` automatic too,
  and nothing on record tells the two apart, so the row is not in the list until the owner decides.
- **The reissue differs** and isn't used apart from the oil's months (a card from the time wins, the owner's
  ruling of 2026-10-23). For the Passat it gives spark plugs every 40,000 or 4 years, the `09M` automatic's
  fluid every 80,000, the dust and pollen filter every 40,000 or 2 years, the AWD clutch fluid every 3
  years (for every vehicle that has one), the panoramic sunroof every 40,000 or 3 years, and a timing belt
  check at 60,000 then every 20,000. It is a later layout (10 columns to 100,000; minor, standard and
  extended services; a table of additional items).
- **Not covered:** the B6's earlier years, 2006–2009 (outside #443; NHTSA's index has no Volkswagen card
  before 2010), and the Passat NMS from 2012, which has cards of its own (#444; see the [2014 2.5L's page](volkswagen-passat-2014-2.5.md)).
