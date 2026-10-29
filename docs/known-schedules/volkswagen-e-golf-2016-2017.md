# Volkswagen e-Golf, 2016–2017

How `app/src/main/assets/known-schedules/volkswagen-e-golf-2016-2017.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids
are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`), and Volkswagen's reissued all-model
cards from its owners-literature site. Read on 2026-10-28, as page images, by eye.

| Model year | What was read | Document |
|---|---|---|
| 2015 | The e-Golf card (posted October 2014), for comparison; it has [a list of its own](volkswagen-e-golf-2015.md) | `2014/MC-10121578-9999` |
| 2016 | The e-Golf card (posted August 2015), in full; the list was written from it | `2015/MC-10123077-9999` |
| 2017 | The e-Golf card in two printings, both posted November 2017 (filed 17 and 30 November; the second is a scan of the same card). They agree with 2016 | `2017/MC-10133455-9999`, `2017/MC-10133456-9999` |
| 2017 | NHTSA's index lists an earlier 2017 card (`10107702`, May 2016); its file wasn't found at `2016/MC-10107702-9999` | — |
| 2017–2019 | Volkswagen's reissues, for comparison (below) | `USA5R506721` (2017), `USA5R507221` (2018), `USA5R507421` (2019), printed 11/25/2025; `…/v1/document/67a3ec7c-748c-4e26-92ea-1c7dcc6732dd`, `…/ceacbe76-21f0-44a9-977e-74ce3a9ff0b3`, `…/71c329f1-dfb1-459e-938f-7e71662f05db` |
| 2020 | The 2020 e-Golf card (revised 8/26/2019), to see what the later cards became (below) | `2019/MC-10164262-0001` |

- **What differs from 2015.** The 2016 card is the 2015 card's grid without five rows: tire rotation, the
  yearly airbag check, the door check lubrication, the rear window washer nozzle check and the service
  sticker. Its dust and pollen filter is every 40,000 miles or 2 years, drawn at 40,000, 80,000 and 120,000
  (2015: every 20,000, and every 2 years). The first column is no longer shaded. Every other row is in the
  same columns, so the remaining jobs keep the 2015 list's wording. These are the same changes as between
  the 2015 and 2016 Jetta Hybrid and GTI cards ([volkswagen-jetta-2016-hybrid](volkswagen-jetta-2016-hybrid.md)).
- **2017 is 2016's card.** Row for row and column for column the same; the 2017 printings call the snow
  filter a "snow sieve" and correct "rear are" to "rear area" in the high-voltage row, which is what the
  November 2017 revisions were filed for.
- **No tire rotation.** The card has no rotation row and nothing says when to rotate; the list has none.
  That is the card as printed, not checked against a booklet.
- The card is a grid: 12 columns, 10,000 to 120,000 miles, with no months printed for a column. Nothing says
  it repeats. Jobs "regardless of mileage" (brake fluid, the tire mobility kit, the air filter every 6 years)
  are drawn in the columns Volkswagen chose and named with their time here. The 12-volt battery and
  high-voltage checks are inspections, so table-only (rule 7).
- Reminders: dust and pollen filter 40,000 / 24 mo; brake fluid every 24 months by time alone (the card says
  at 3 years first, then every 2).
- **The air filter is in the table only**, as on [the 2015 list](volkswagen-e-golf-2015.md): Glovebox's line
  for it is the engine air filter, which an electric car hasn't got. Asked on #438.
- **The model.** vPIC names the car "e-Golf" in 2015, 2016 and 2017 (`GetModelsForMakeYear`), so the list is
  for that model, with no `engine`.
- No drivetrain or gearbox marks, and no special operating conditions, on the card.
- **The reissues differ** and aren't used for 2016–2017: Volkswagen's 2017 reissue has the later layout's
  battery-electric section (minor, standard and extended services, 10 columns to 100,000), with no air
  filter and the tire filler bottle as a 2-year check only. The card from the time wins (the owner's ruling of 2026-10-23).
- **Not covered:**
  - **2018 and 2019.** No e-Golf card from the time was found: NHTSA's index has none for those years, and
    its 2018 and 2019 all-model cards are for every model except the e-Golf. The only sources are Volkswagen's
    2018 and 2019 reissues (printed 11/25/2025), whose battery-electric section is a different schedule
    from the 2016–2017 card: the later minor / standard / extended layout to 100,000, no air filter, the tire
    filler bottle as a 2-year check only, rotation for Canada only, and a ribbed belt check for "All Vehicles".
    The 2018 and 2019 reissues differ from each other only in wording, a Canada brake fluid line and the grid
    (not compared mark by mark). The 2020 card, the first from the
    time in the new layout, has no air filter either, so the schedule changed somewhere after 2017. Whether
    the reissues are enough for 2018–2019, and how their layout's levels become a table, is the owner's to
    decide (asked on #438).
  - 2015 has [its own list](volkswagen-e-golf-2015.md).
