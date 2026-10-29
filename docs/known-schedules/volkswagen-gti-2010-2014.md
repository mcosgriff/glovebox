# Volkswagen GTI, 2010–2014 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-gti-2010-2014.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "Maintenance Schedule For USA" cards, which Volkswagen files with NHTSA as
manufacturer communications (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids
are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-13.

| Model year | What was read | Document |
|---|---|---|
| 2010 | The card for all models except the Routan. Every row that names the GTI, its 2.0T engine or all models agrees with the 2012 GTI card | `2012/MC-10156104-9999` |
| 2011 | **No card from the time was found.** Volkswagen's model-year 2011 sheet of transmission and spark plug intervals gives the GTI 2.0T: DSG fluid at 40,000, spark plugs at 60,000, as the cards either side do. Volkswagen's 2025 reissue of the 2011 card agrees with every reminder (see below) | `2012/MC-10071284-7690`; reissue `USA5R504821` |
| 2012 | The GTI card, in full; the list was written from it | `2012/MC-10072614-5448` |
| 2013 | **No card from the time was found.** The model-year 2013 sheet gives the same two figures, and Volkswagen's 2025 reissue of the 2013 card agrees with every reminder (see below) | `2012/MC-10071272-7690`; reissue `USA5R505321` |
| 2014 | The GTI card, in full: the same jobs in the same columns as 2012 | `2013/MC-10155950-9999` |

- **2011 and 2013 rest on the cards either side, Volkswagen's sheet for the year, and its 2025
  reissues.** No card from the time was found for either year.
  - **The reissues:** Volkswagen's owners-literature site serves all-model cards for 2010 to 2013
    (`USA5R503621`, `USA5R504821`, `USA5R505021`, `USA5R505321`, each dated 11/24/2025). They were read
    on 2026-10-18. They are laid out afresh: minor, standard and extended services, then a table of
    additional items.
  - **What agrees, for 2011 and 2013 alike:**
    - Engine oil and tire rotation every 10,000.
    - DSG fluid every 40,000 (`02E (DSG)`, Golf).
    - Air filter every 60,000 or 6 years.
    - Spark plugs every 60,000 or 6 years (`Golf / GTI (5K-2.0T)`).
    - Brake fluid 3 years after registration, then every 2.
  - **What differs:** the dust and pollen filter is every 40,000 miles or 2 years in all four reissues.
    The cards from the time say every 20,000 or every 2 years: the 2012 and 2014 GTI cards, and the
    2010 card. So this is Volkswagen revising the interval for every year, not a 2011 or 2013
    difference. The list keeps the cards of the time.
  - **Oil:** the reissues give it a time limit too. Both the minor and the standard service change
    the oil (standard includes "Perform Minor Maintenance").
    - Minor services fall at 10,000 (or 1 year from delivery), then every 20,000 or 2 years: 10,000,
      30,000, 50,000 and so on.
    - Standard services fall every 20,000 or 2 years: 20,000, 40,000 and so on.
    - So the oil is still changed every 10,000, or about once a year.
    The old cards print no months, so the table has none, but the oil reminder is 10,000 or 12 months from
    these (the owner's ruling of 2026-10-28, #517).
  - **The table** is the old cards' 12 columns. The reissues' minor/standard/extended layout isn't the
    same table.
  - The owner of a 2011 or 2013 should still check the list against the booklet in the car.
- The card is a grid: 12 columns, 10,000 to 120,000 miles, with no months printed for a column. Jobs
  "regardless of mileage" (airbag check yearly, brake fluid, the tire mobility kit) are drawn in the
  columns Volkswagen chose and named with their time here.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; DSG fluid and
  filter 40,000 (not for a manual car); air filter and spark plugs 60,000 / 72 mo; brake fluid every
  24 months by time alone (the card says at 3 years first, then every 2).
- Left out of the 2010 card's rows as not the GTI's: AdBlue, the diesel particulate and fuel filters
  (TDI), the New Beetle's brake fluid row, the convertible roof, the 2.5L / 3.2L spark plug row and the
  2.5L automatic's fluid. Its three "if applicable" rows that the 2012 and 2014 GTI cards don't have
  (Haldex clutch oil, timing belt, tire pressure sensors) are left out too, on the evidence of those cards.
- Left out of every year: the dealer's service sticker.
- Not covered: the Mk5 (2006–2009, a list per year from Volkswagen's reissues; see
  [2009](volkswagen-gti-2009.md#the-mk5-lists)) and the Mk7 (2015 on), which have cards of their own.
