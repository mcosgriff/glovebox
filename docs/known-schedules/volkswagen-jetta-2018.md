# Volkswagen Jetta, 2018 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2018.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2018 "Maintenance Schedule" card for all its models except the e-Golf, which Volkswagen
files with NHTSA as a manufacturer communication (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`;
the ids are in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images,
by eye.

| Revision | What was read | Document |
|---|---|---|
| Revised 11/26/2019 (the newest) | In full, off the page; **the list was written from it** | `2019/MC-10168646-0001` |
| 4/17/2017 and 7/18/2017 | Not read again; the [2018 GTI's page](volkswagen-gti-2018.md#what-the-earlier-revisions-print-differently) says what they print differently | `2017/MC-10125867-9999`, `2017/MC-10127897-9999` |

NHTSA's index files three more revision notices under the 2018 Jetta (`MC-10127874`, "boots" added to the
check from below; `MC-10127898`, the 110,000 column added; `MC-10133461`, "(1.4L Jetta only)" added to the two
belt checks). Each is a change the newest revision has. They weren't fetched.

## How the 2018 all-model card is read

This is how the newest revision is read for every model on it, as the [2018 GTI's list](volkswagen-gti-2018.md)
first read it; a later list from this card should read it the same way, or say where and why it doesn't.

1. **The newest revision wins** (the owner's ruling, #143): `2019/MC-10168646-0001`, revised 11/26/2019.
2. **The grid.** Minor at 10,000 and every 20,000 after; Standard at 20,000 and every 20,000 (it includes
   every Minor job); Extended at 40,000, 80,000 and 120,000, with a Standard. Each milestone is the jobs of
   its services, each named once; 12 columns, 10,000 to 120,000, not repeated (rule 9).
3. **Months.** The footnotes give the Minor service at 10,000 miles or 1 year first, the Standard at 20,000
   or 2 years, then each every 20,000 miles or 2 years. The milestones carry no months; the oil change, in
   every service, is a reminder every 10,000 miles or 12 months (#517).
4. **The services' jobs are inspections** apart from the oil change: table-only, with a `-check` service
   where one exists. "(if applicable)" and "(if equipped)" rows are named "(if fitted)".
5. **Additional Maintenance Items** are reminders in `additional` with the card's own figures, never placed on
   the grid (the owner's ruling of 2026-10-23, #33): air filter 60,000 / 72 mo; brake fluid every 24 months by
   time alone, its first time ("3 years after initial registration") in its name; dust and pollen filter
   40,000 / 24 mo; front axle differential lock fluid "(if equipped)", every 3 years by time alone, "(if
   fitted)" with no service line, since no model is named; the panoramic sunroof, every 40,000 miles or 2
   years, first at 40,000 (`firstAtMiles`; the card's "or 3 years" for the first time is recorded, not put
   into months); the tire filler bottle's date every 2 years by time alone, "(if fitted)".
6. **Spark plugs** by the engine marks on the two rows: 40,000 / 48 mo for the 4-cylinder "1.4L, 1.8L, and
   2.0L", 60,000 / 72 mo for the 3.6L. A model whose engines are all on one row needs no `engine`.
7. **Rows naming models** ("Passat only", "Golf R only", "Does not apply to: Passat, Golf, Beetle, and
   Jetta", "Tiguan, Tiguan Limited, and Atlas only", "Beetle Convertible only", "*1.4L Jetta only") are kept
   only for the models they name, and left out for the others, by their own words. Where a model's name is
   doubtful against a row (the GTI and "Golf"), the row is left out and asked.
8. **"AWD clutch: Change fluid (if equipped)", "AWD only vehicles"**, every 3 years: for a model sold with
   all-wheel drive it is an `additional` reminder by time alone in `allWheelDriveOnly`, named "(AWD)". The GTI
   and the Jetta were sold with front-wheel drive only, so it is left out of their lists.
9. **The gearbox rows.** "Transmission, 6-Speed DSG: Change fluid and filter" (every 40,000) and
   "Transmission, Automatic (non-DSG): Change fluid" (every 80,000) name no model; the 7-speed DSG is "Golf R
   only". Where a model was sold with only one kind of self-shifting gearbox the card names, its row is a
   `manualExcluded` reminder (the GTI's 6-speed DSG). Where one model, as Glovebox matches it, was sold with
   both a DSG and a conventional automatic, the format can't tell them apart, so both rows are left out
   ([#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333)).
10. **Severe conditions**: one general sentence (oil and the air filter more often in extreme cold or dust)
    with no jobs or figures, so a list has nothing for them. No tire rotation: the card has none.

## This list

- **One list for every engine.** The 2018 Jetta was the 1.4T, the 1.8T and the GLI's 2.0T, and vPIC's
  model list for Volkswagen, 2018 (`GetModelsForMakeYear`, read 2026-10-29), names them all "Jetta" (no
  "GLI"). The card's only rows that differ by Jetta engine are the 1.4L's belt checks (below), and the spark
  plug row covers all three, so the list names no engine.
- **The rows are the [2018 GTI's](volkswagen-gti-2018.md)**, in that list's wording, but for two:
  - **the hood latch** is left out by its own words, "Does not apply to: … Jetta" (on the GTI's list it is
    left out and asked);
  - **both gearbox rows are left out** (point 9): the GLI's 6-speed DSG and the 1.4T's and 1.8T's
    automatic are all "Jetta" to vPIC, and the card doesn't say which engine has which; asked on #440, as on
    #443.
- **Left out, asked on #440: the 1.4L Jetta's belt checks.** "Coolant pump toothed belt: Check" and "Timing
  belt: Check", "At 150,000 miles, then every 20,000 miles (*1.4L Jetta only)". They are additional items, so
  they belong in no column, but they are inspections, which the rules don't make reminders (rule 7), and both
  start past the grid's end. Nothing in the format holds a check that is neither on the table nor a reminder.
  A 1.4T owner should know they are on the card.
- Left out as not the Jetta's: the AWD clutch fluid (point 8), the 3.6L's spark plugs, the 7-speed DSG, the
  trailer hitch, the convertible top, and the sunroof water drains (Passat only).
- Reminders: engine oil 10,000 / 12 mo; additional items as in point 5; spark plugs 40,000 / 48 mo.
- **Not covered:** 2017 and the other Mk6 years: see the [2014 1.8T's page](volkswagen-jetta-2014-1.8t.md#what-isnt-covered).
  The Mk7 Jetta (2019 on) is #442's.
