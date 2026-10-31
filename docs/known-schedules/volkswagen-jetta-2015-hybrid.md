# Volkswagen Jetta Hybrid, 2015

How `app/src/main/assets/known-schedules/volkswagen-jetta-2015-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids
are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`), and Volkswagen's reissued all-model
cards from its owners-literature site (`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`,
document type "Maintenance Card", `en-US`). Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2013 | The Jetta Hybrid 1.4L card in two printings: posted November 2012 (filed twice) and posted January 2013. The January 2013 one is row for row this list's card; 2013 has [a list of its own](volkswagen-jetta-2013-hybrid.md) | `2012/MC-10156148-9999`, `2013/MC-10242481-9999` (Nov. 2012); `2013/MC-10242444-9999` (Jan. 2013) |
| 2014 | **No card for the Jetta Hybrid was found.** NHTSA's index files `2013/MC-10125796-9999` under the 2014 Jetta Hybrid; it is the 2014 GLI 2.0L TSI's card. Volkswagen's 2014 reissue was read (below) | reissue `USA5R505521`, printed 11/24/2025, `…/v1/document/033ff2d5-58e3-4663-97cf-1bbdb864258d` |
| 2015 | The Jetta Hybrid 1.4L card (posted June 2014), in full; the list was written from it | `2014/MC-10120122-9999` |
| 2015 | Volkswagen's 2015 reissue, for the oil change's time limit (below) | reissue `USA5R505821`, printed 11/24/2025, `…/v1/document/cd0baffd-4470-48db-b81c-b2b59ce5e527` |
| 2016 | The Jetta Hybrid 1.4L card (posted August 2015). It differs from 2015, so it has [a list of its own](volkswagen-jetta-2016-hybrid.md) | `2015/MC-10123070-9999` |

- **The card is a grid**: 15 columns, 10,000 to 150,000 miles, with no months printed for a column. Nothing
  says it repeats, so the table doesn't (rule 9). Jobs "regardless of mileage" (airbag check yearly, the
  dust and pollen filter every 2 years, brake fluid, the tire mobility kit, spark plugs every 4 years and the
  air filter every 6) are drawn in the columns Volkswagen chose and named with their time here where the
  time is the only thing that says when.
- **The rows are the 2015 GTI card's** ([volkswagen-gti-2015](volkswagen-gti-2015.md)) where they are the
  same job, and use that list's wording, except:
  - the grid runs to 150,000, not 120,000;
  - spark plugs are every 40,000 miles or 4 years (drawn at 40,000, 80,000 and 120,000), not 60,000 / 6;
  - there is no DSG fluid change;
  - a high-voltage hybrid components check at every column;
  - a timing belt check and a coolant pump timing belt check at 150,000, each "then every 20,000 until
    replacement is necessary". They are inspections, so table-only (rule 7); the "then every 20,000" is in
    their names, since it falls past the grid's end;
  - the tire check names no tire age, and the washer check adds the headlight washers;
  - the ribbed belt check is "(if applicable)" on this card; it is named "(if fitted)".
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs 40,000 / 48 mo; air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says
  at 3 years first, then every 2).
- **Oil 12 months.** The card prints no months. Volkswagen's reissue for 2015 (`USA5R505821`, and the 2013
  and 2014 ones alike) gives the first minor service at 10,000 miles or 1 year after delivery, then minor
  services every 20,000 or 2 years, and standard services (which include the minor) every 20,000 or 2
  years: an oil change every 10,000 or about once a year. The table keeps no months; the oil reminder takes
  12 (the owner's ruling of 2026-10-28, #517), as the [2010–2014 GTI](volkswagen-gti-2010-2014.md) does.
  The reissue is a later layout (10 columns to 100,000, minor/standard/extended), so nothing else is taken
  from it.
- **The engine.** The card is for the 1.4L hybrid. NHTSA's vPIC model list for Volkswagen
  (`GetModelsForMakeYear`, 2013, 2014 and 2016 checked) has no "Jetta Hybrid", only "Jetta", so the list is
  for the Jetta with `"engine": { "label": "1.4L hybrid", "displacementL": 1.4, "powertrain": "hybrid" }`.
  A Jetta whose engine and powertrain aren't on record is asked which engine it has. The powertrain is set
  so that a petrol Jetta with a 1.4L engine is never given the hybrid's list.
- No drivetrain or gearbox marks: no row on the card names one.
- No special operating conditions are given on the card.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first column.
- **Not covered:**
  - **2013** has [its own list](volkswagen-jetta-2013-hybrid.md), the same as this one: the January 2013
    printing wins over the November 2012 one (the owner's ruling of 2026-10-29 on #553,
    [part A](https://github.com/mcosgriff/glovebox/issues/553#issuecomment-6511794772)). It is a list apart,
    not 2013–2015, because 2014 isn't covered by these cards (below).
  - **2014.** No card from the time was found. The 2013 (January 2013) and 2015 cards agree row for row,
    and Volkswagen's 2014 reissue agrees with every reminder except the dust and pollen filter (40,000 or
    2 years, as in every reissue of these years; see the GTI's page). Whether that is enough to cover 2014
    is the owner's to decide (asked on #441).
  - **2016** has [its own list](volkswagen-jetta-2016-hybrid.md).
