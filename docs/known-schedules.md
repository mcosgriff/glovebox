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
   condition (`Replace rear differential oil · towing or heavy loads`) and with no `service`, so it is
   never taken for the ordinary job. A job for one drivetrain says so in its name (`(4WD)`).
7. **A reminder interval has to be the table's own.** `KnownSchedules.parse` refuses an interval unless
   its job is in the table at exactly the multiples of that interval, with the table's months, or, for
   a job with a first time of its own (`firstAtMiles`: "first at 100,000, then every 50,000"), at that
   mileage and every interval after it. A job that fits neither stays in the table only. Reminders are given only for work that gets
   logged (replace, rotate, lubricate), not for inspections.
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

A model whose guide differs by engine gets one list per engine; that needs a field to match on, which
is added with the first list that needs it.

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
  list and the 4WD-only jobs say `(4WD)`.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000
  miles if 0W-20 wasn't used at the last change, the daily checks for off-road driving, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any special operating condition), tire rotation 5,000 / 6, propeller shaft lubrication
  15,000 / 18, cabin and engine air filters 30,000 / 36, spark plugs 120,000 / 144, and engine coolant
  first at 100,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the first coolant
  change isn't used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Not reminders, because under ordinary driving the guide only inspects them: brake fluid, transmission
  fluid, differential and transfer case oil. Their replacement under towing or dust is in the table.
- Not covered: 2010 and 2012–2016 (no guide found at that address), 2011 (found; it has separate
  schedules for the 4-cylinder and the V6), 2025 on (a new generation, with a chart in place of the log).
