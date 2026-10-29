# Volkswagen Jetta Hybrid, 2016

How `app/src/main/assets/known-schedules/volkswagen-jetta-2016-hybrid.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids
are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`), and Volkswagen's reissued all-model
card from its owners-literature site. Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2015 | The Jetta Hybrid 1.4L card (posted June 2014), for comparison; it has [a list of its own](volkswagen-jetta-2015-hybrid.md) | `2014/MC-10120122-9999` |
| 2016 | The Jetta Hybrid 1.4L card (posted August 2015), in full; the list was written from it | `2015/MC-10123070-9999` |
| 2016 | Volkswagen's 2016 reissue, for the oil change's time limit (below) | reissue `USA5R506521`, printed 11/25/2025, `…/v1/document/ceab8882-749e-4dda-ba29-a7af0f418d43` |

- **What differs from 2015.** The 2016 card is the 2015 card's grid without five rows: tire rotation, the
  yearly airbag check, the door check lubrication, the rear window washer nozzle check and the service
  sticker. Its dust and pollen filter is every 40,000 miles or 2 years, drawn at 40,000, 80,000 and 120,000
  (2015: every 20,000, and every 2 years). Every other row is in the same columns, so the remaining jobs keep
  the 2015 list's wording. These are the same changes as between the 2015 and 2016 GTI cards
  ([volkswagen-gti-2016](volkswagen-gti-2016.md)).
- **No tire rotation.** The card has no rotation row and nothing says when to rotate; the list has none.
  That is the card as printed, not checked against a booklet.
- The card is a grid: 15 columns, 10,000 to 150,000 miles, with no months printed for a column. Nothing says
  it repeats. Jobs "regardless of mileage" (brake fluid, the tire mobility kit, spark plugs every 4 years and
  the air filter every 6) are drawn in the columns Volkswagen chose and named with their time here where the
  time is the only thing that says when. The timing belt and coolant pump timing belt checks at 150,000
  ("then every 20,000 until replacement is necessary") and the high-voltage hybrid components check are
  inspections, so table-only (rule 7).
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; spark plugs 40,000 / 48 mo;
  air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years first, then
  every 2).
- **Oil 12 months.** The card prints no months. Volkswagen's reissue for 2016 (`USA5R506521`, also read for
  the [2017 GTI](volkswagen-gti-2017.md)) gives the first minor service at 10,000 miles or 1 year after
  delivery, then every 20,000 or 2 years, with standard services (which include the minor) every 20,000 or
  2 years. The table keeps no months; the oil reminder takes 12 (the owner's ruling of 2026-10-28, #517).
  Nothing else is taken from the reissue, a later layout.
- **The engine.** As for 2015: vPIC names the car "Jetta" (2016 checked), so the list is for the Jetta with
  the 1.4L hybrid engine and `"powertrain": "hybrid"`. The powertrain is set so that a petrol Jetta of
  1.4L (the 2016 Jetta had a 1.4L turbo too, from general knowledge, not a document) never gets this list.
- No drivetrain or gearbox marks, and no special operating conditions, on the card.
- **Not covered:** 2013 and 2014 (see [the 2015 list's page](volkswagen-jetta-2015-hybrid.md)). NHTSA's index
  has no Jetta Hybrid card after 2016.
