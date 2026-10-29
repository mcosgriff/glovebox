# Volkswagen e-Golf, 2015

How `app/src/main/assets/known-schedules/volkswagen-e-golf-2015.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids
are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`), and Volkswagen's reissued all-model
card from its owners-literature site (`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`,
document type "Maintenance Card", `en-US`). Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2015 | The e-Golf card posted June 2014, filed twice (the two files are the same) | `2014/MC-10120116-9999`, `2014/MC-10240816-9999` |
| 2015 | The e-Golf card posted October 2014, filed twice (the same), in full; the list was written from it | `2014/MC-10121578-9999`, `2014/MC-10242482-9999` |
| 2015 | Volkswagen's 2015 reissue, for comparison (below) | reissue `USA5R505821`, printed 11/24/2025, `…/v1/document/cd0baffd-4470-48db-b81c-b2b59ce5e527` |
| 2016 | The e-Golf card (posted August 2015). It differs from 2015, so it is in [a list of its own](volkswagen-e-golf-2016-2017.md) | `2015/MC-10123077-9999` |

- **The two 2015 printings agree** row for row and column for column. The October 2014 one only corrects
  spelling ("12 Volt Batter", "underbofy", "kickdown").
- **The card is a grid**: 12 columns, 10,000 to 120,000 miles, with no months printed for a column. Nothing
  says it repeats, so the table doesn't (rule 9). Jobs "regardless of mileage" (airbag check yearly, the
  dust and pollen filter every 2 years, brake fluid, the tire mobility kit, the air filter every 6 years) are
  drawn in the columns Volkswagen chose and named with their time here where the time is the only thing that
  says when.
- **Electric**: no engine oil, spark plugs or exhaust. The card's own rows are a 12-volt battery test and
  high-voltage battery charge, charging cable, charging socket and high-voltage components checks at every
  column from 20,000, and electric motor bay checks from above and below at 40,000, 80,000 and 120,000. They
  are inspections, so table-only (rule 7). Rows shared with the petrol cards use the wording of the
  [2015 Jetta Hybrid](volkswagen-jetta-2015-hybrid.md) and [2015 GTI](volkswagen-gti-2015.md) lists.
- Reminders: tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; brake fluid every 24 months by
  time alone (the card says at 3 years first, then every 2).
- **The air filter is in the table only.** The card draws "air filter element and snow filter: replace" at
  60,000 and 120,000, every 6 years, and its footnote marks it "(if applicable)". Glovebox's line for that
  job is the engine air filter, which an electric car hasn't got (the typical list leaves it off an electric
  vehicle), and no other line fits, so it drives no reminder here. Asked on #438.
- **The model.** NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`, 2015, 2016 and 2017
  checked) names the car "e-Golf", so the list is for that model, with no `engine`: the e-Golf had one
  powertrain.
- No drivetrain or gearbox marks, and no special operating conditions, on the card. The "clutch unit" in the
  brake fluid row is the card's wording; the job is the brake fluid change.
- Left out: the dealer's service sticker, and the grey "Carefree Maintenance" shading of the first column.
- **The reissue differs** and isn't used: Volkswagen's 2015 reissue has a battery-electric section in its
  later layout (minor, standard and extended services, 10 columns to 100,000), with no air filter, and the
  dust and pollen filter every 20,000 or 2 years. The card from the time wins (the owner's ruling of
  2026-10-23).
- **Not covered:** 2016 and 2017 have [their own list](volkswagen-e-golf-2016-2017.md); 2018 and 2019 are on
  that page.
