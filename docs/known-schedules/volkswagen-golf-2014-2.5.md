# Volkswagen Golf 2.5L, 2014 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-golf-2014-2.5.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`), and Volkswagen's reissued all-model card
for 2014 from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=2014`, document type
"Maintenance Card", `en-US`). Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2014 | The Golf 2.5L card (posted October 2013), in full; the list was written from it. NHTSA files it three times; the files are byte for byte the same | `2013/MC-10155949-9999`, `2013/MC-10157540-9999`, `2013/MC-10242464-9999` |
| 2014 | Volkswagen's 2014 reissue, for the oil change's time limit only | reissue `USA5R505521`, printed 11/24/2025, `…/v1/document/033ff2d5-58e3-4663-97cf-1bbdb864258d` |

NHTSA's index files seven documents under the 2014 Golf. The other four: `2013/MC-10155943-9999`,
`2013/MC-10157534-9999` and `2013/MC-10242463-9999` are the Golf 2.0L TDI card ([its own list](volkswagen-golf-2014-tdi.md)), and `2013/MC-10242465-9999` is
the 2014 GTI 2.0TFSI card.

- **The card is a grid**: 12 columns, 10,000 to 120,000 miles, with no months printed for a column.
  Nothing says it repeats, so the table doesn't (rule 9). Jobs "regardless of mileage" (airbag check
  yearly, the dust and pollen filter and the tire mobility kit check every 2 years, brake fluid, the tire
  filler bottle every 4 years, spark plugs and the air filter every 6) are drawn in the columns Volkswagen
  chose. The pages after the third are blank.
- **The rows are the [2010–2014 GTI](volkswagen-gti-2010-2014.md)'s** where they are the same job, and use
  that list's wording, except:
  - spark plugs are drawn at 40,000, 80,000 and 120,000, each with footnote 6, "every 6 years regardless of
    mileage": a reminder every 40,000 miles or 72 months;
  - "Transmission: Automatic (non DSG): Change fluid" at 50,000 and 100,000, in place of the DSG's fluid
    change: the `trans-fluid` service, a reminder every 50,000 miles, in `manualExcluded`, so a Golf on
    record as a manual doesn't get it. The card has no DSG row.
  - The card's brake fluid row is "Brake fluid and clutch unit"; it is named as on the GTI's list.
- **The engine.** The card is for the 2.5L, so the list has `"engine": { "label": "2.5L",
  "displacementL": 2.5 }`; a Golf with no engine on record is asked which it has. NHTSA's vPIC model list
  for Volkswagen, 2014 (`GetModelsForMakeYear`), names it "Golf", apart from "GTI".
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; spark
  plugs 40,000 / 72 mo; automatic gearbox fluid 50,000 (not for a manual); air filter 60,000 / 72 mo; brake
  fluid every 24 months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months.** The card prints no months. Volkswagen's 2014 reissue gives the first minor service at
  10,000 miles or 1 year after delivery, then minor services every 20,000 or 2 years, and standard services
  (which include the minor) every 20,000 or 2 years, both changing the oil: so oil every 10,000 or about
  once a year. The oil reminder takes 12 months (the owner's ruling of 2026-10-28, #517); the table has none.
  Nothing else is taken from the reissue, which is a later layout.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first two
  columns.
- **Not covered:** 2011–2013 (see the
  [2010 2.5L's page](volkswagen-golf-2010-2.5.md)), and the Mk7 from 2015.
