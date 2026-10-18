# Known maintenance schedules

Glovebox has no maintenance schedule of its own for any model and works none out. What it can ship is
a **manufacturer's published schedule, named**: the table from the maker's maintenance guide and the
reminder intervals that follow from that table. This file is the rules for such a list and the record
of how each one was made. The code is `KnownSchedules` in `:domain`; the lists are the files in
`app/src/main/assets/known-schedules/`. Torque values have the same arrangement in
[known-specs.md](known-specs.md).

## Rules

1. **Facts only, in Glovebox's words.** A list takes the mileages, the months and which jobs fall at
   each. Jobs are named in Glovebox's own wording. No text, footnote or layout is copied from the guide.
2. **Every list names its source and says why it may be shipped.** `source.name`, `source.url`, the day
   it was read, a sentence shown to the user, and `source.basis`. "It was on a website" is not a basis.
   The source is credited on the About screen.
3. **A list covers only the model years whose guides were read and found to agree.** Not a generation,
   not "probably the same". A year whose guide wasn't found gets the typical starter list, as before.
4. **Where there is a list, it is the schedule.** Setting up a vehicle whose make, model and year a
   list covers starts from the maker's schedule in place of the typical list, and its table becomes
   the vehicle's milestones. The screen says whose guide it is. Everything can still be unticked or
   edited. A vehicle set up before the list existed is offered it ("Use the maker's schedule" on the
   Maintenance schedule, "Use the maker's table" on Milestones); nothing changes until that is tapped.
5. **What comes from a list says so, once per screen**, with the advice to check it against the
   owner's own guide: an info box on the Maintenance schedule and the introduction on Milestones,
   never a line under every item. `schedule_items.knownList` and `vehicles.milestonesKnownList`
   record it.
6. **The table is the whole guide**: every line at every milestone, inspections included. Jobs that
   apply only under the guide's special operating conditions are in it too, each named with its
   condition (`Replace rear differential oil · towing or heavy loads`). Such a job has a `service` only
   when no ordinary job in the table has the same one, so it is never taken for the ordinary job: the
   oil change under special conditions has none, the differential oil changes do. A job for one
   drivetrain says so in its name (`(4WD)`).
7. **A reminder interval has to be the table's own.** `KnownSchedules.parse` refuses an interval unless
   its job is in the table at exactly the multiples of that interval, with the table's months, or, for
   a job with a first time of its own (`firstAtMiles`: "first at 100,000, then every 50,000"), at that
   mileage and every interval after it. A job that fits neither stays in the table only. Reminders are given for work that gets
   logged (replace, rotate, lubricate), and for an inspection only where the owner asked for it and
   there is a service line to log it with; a list doesn't turn every inspection into a reminder.
   **A job the guide lists only under special operating conditions is a reminder that is only for
   severe use** (`severeOnly`): it is in the schedule with the guide's interval, named with its
   condition, and reminds only while "How do you drive it?" is on Severe. On normal use it stays on the
   list, switched off. There is one switch, not one per condition: on Severe every such job is on, and
   the owner unticks the ones for conditions that aren't theirs. Where the guide gives an ordinary job
   a shorter interval under those conditions, that is the item's `severe` interval.
8. **Nothing of the user's is written over.** The table is added only while the vehicle has none.
   A reminder is added as the user's own item unless they already have one of that name. Typical items
   are never edited; on a vehicle that has them, the ones that would double a new reminder are listed,
   ticked, and are removed when the schedule is taken, so each job is on the list once. One that has a
   service logged against it is kept, untracked, so that record keeps its link.
9. **A table doesn't repeat unless the guide says it does.**

## The file

```json
{
  "id": "toyota-4runner-2017-2024",
  "title": "2017–2024 4Runner",
  "make": "Toyota", "model": "4Runner", "years": { "from": 2017, "to": 2024 },
  "source": { "name": "…", "url": "https://…", "retrieved": "YYYY-MM-DD", "note": "…", "basis": "…" },
  "milestones": [ … ],
  "intervals": [ … ]
}
```

`milestones` is written exactly as the `items` of a `milestone-schedule` import file and `intervals`
as those of an `interval-schedule` one ([import/README.md](import/README.md)); both are read by the
same strict readers, so errors are reported under `items[…]`. A list in miles is given to a kilometre
vehicle as schedule figures (5,000 mi is 8,000 km). `KnownSchedulesTest` reads every file in the folder.

`fourWheelDriveOnly` names the jobs the guide marks "(4WD)". A vehicle **known** to drive two wheels
(vPIC's `4x2`, or RWD / FWD picked by hand) doesn't get them, in the table or the schedule. Every other
vehicle keeps them, with "(4WD)" in the name: a four-wheel-drive one because they are its jobs, and one
on record as all-wheel drive, or with no drive type at all, because the guide has no other line for it
and leaving a job out is the worse mistake. **Four-wheel drive and all-wheel drive are different systems
and the code keeps them apart** (`DriveKind`); a guide that marks jobs for AWD gets its own field for
them, and nothing marked for one is taken to be for the other.

`manualExcluded` names the jobs that aren't for a manual gearbox, such as a dual-clutch transmission's
fluid change. The rule is the same shape: only a vehicle **known** to have a manual (vPIC's
`Manual/Standard`) loses them; an automatic, a dual-clutch or nothing on record keeps them.

A job a guide gives by time alone ("every 2 years regardless of mileage") is a reminder with months and
no distance, marked `time-only`. The table shows it wherever the maker drew it, and the check doesn't hold
its mileages against it. Where the first time differs ("at 3 years, then every 2"), the reminder is the
repeating part and the job's name says the rest: Glovebox doesn't know the day a car went into service.

**A model whose guide has a schedule per engine gets one list per engine**, each with

```json
"engine": { "label": "2.7L 4-cylinder", "displacementL": 2.7 }
```

and `"fuel": "Diesel"` as well where two engines of one size differ by it. A list without `engine` is
for every engine the model had in its years. A vehicle is matched by the size in its engine text ("4.0L
6-cyl" as a VIN decode writes it, or what the owner typed) and, where the list names one, by its fuel.
**An engine that isn't on record matches no engine's list.** Setup then asks "Which engine does it
have?", offering each list's `label` and "Neither, or I'm not sure"; the pick is the owner's, and it is
written to the vehicle if it had no engine on record. Nothing is guessed from a trim or a model name.

## Making a list

1. Get the maker's guide for every model year the list is to cover.
2. Read one year's schedule off the pages, by eye, and write the list from that.
3. Compare the other years with it. Text extraction of these guides is unreliable (two milestones share
   a page, footnote marks run into words, some years have letter-spaced text), so a script's count of
   each job per page is only a pointer: every difference it reports is looked at on the page.
4. Record below which documents were read and what differed.

## The lists

### Toyota 4Runner, 2017–2024

Source: Toyota's Warranty and Maintenance Guide for each model year, which Toyota publishes at
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-<yy>4Runner/pdf/T-MMS-<yy>4Runner.pdf`
(`17` to `24`). Read on 2026-10-13.

- The 2020 guide's maintenance log (pages 38 to 53: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2017, 2018, 2019, 2021, 2022, 2023 and 2024 were compared page by page. The jobs and where they fall
  are the same in all eight. What differs is wording: the 2023 and 2024 guides say "Radiator, condenser
  and/or intercooler"; the 2024 guide's oil note says "recommended oil" where the others say "0W-20";
  2017 has no footnote on fluid levels.
- These years have one engine. Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the
  list and the 4WD-only jobs say `(4WD)` and are left out for a 2WD truck. The guide has no "AWD" lines:
  Toyota calls the Limited's system full-time 4WD, and NHTSA's decoder gives a Limited as
  `4WD/4-Wheel Drive/4x4` or `4x2` (checked 2026-10-13 with the 2022 patterns `JTEKU5JR*N` and
  `JTEDU5JR*N`), never as AWD.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000
  miles if 0W-20 wasn't used at the last change, the daily checks for off-road driving, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any special operating condition), tire rotation 5,000 / 6, propeller shaft lubrication
  15,000 / 18, cabin and engine air filters 30,000 / 36, spark plugs 120,000 / 144, and engine coolant
  first at 100,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the first coolant
  change isn't used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Inspections that are reminders too (asked for by the owner, 2026-10-13): front and rear differential
  oil every 15,000 / 18, transfer case oil every 30,000 / 36. Logging a change of one counts as its
  check. The other inspections are in the table only.
- Only for severe use (#136): front and rear differential oil every 15,000 / 18 and automatic
  transmission fluid every 60,000 / 72 when towing or carrying heavy loads; transfer case oil every
  30,000 / 36 on dusty roads or when towing. Under ordinary driving the guide only inspects these.
- Shorter under severe use: engine oil 5,000 / 6 (any of the guide's four conditions), propeller shaft
  lubrication 5,000 / 6 (dust, salted roads or towing).
- Brake fluid has no replacement in the guide's log at all, so it has no reminder from this list.
- Not covered: 2010 and 2012–2016 (no guide found at that address), 2011 (found; it has separate
  schedules for the 4-cylinder and the V6), 2025 on (a new generation, with a chart in place of the log).

### Volkswagen GTI, 2010–2014 (Mk6)

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
    The old cards print no months, so the list has none.
  - **The table** is the old cards' 12 columns. The reissues' minor/standard/extended layout isn't the
    same table.
  - The owner of a 2011 or 2013 should still check the list against the booklet in the car.
- The card is a grid: 12 columns, 10,000 to 120,000 miles, with no months printed for a column. Jobs
  "regardless of mileage" (airbag check yearly, brake fluid, the tire mobility kit) are drawn in the
  columns Volkswagen chose and named with their time here.
- Reminders: engine oil and tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; DSG fluid and
  filter 40,000 (not for a manual car); air filter and spark plugs 60,000 / 72 mo; brake fluid every
  24 months by time alone (the card says at 3 years first, then every 2).
- Left out of the 2010 card's rows as not the GTI's: AdBlue, the diesel particulate and fuel filters
  (TDI), the New Beetle's brake fluid row, the convertible roof, the 2.5L / 3.2L spark plug row and the
  2.5L automatic's fluid. Its three "if applicable" rows that the 2012 and 2014 GTI cards don't have
  (Haldex clutch oil, timing belt, tire pressure sensors) are left out too, on the evidence of those cards.
- Left out of every year: the dealer's service sticker.
- Not covered: the Mk5 (2009 and earlier) and the Mk7 (2015 on), which have cards of their own.
