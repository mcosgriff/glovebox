# Known maintenance schedules

Glovebox has no maintenance schedule of its own for any model and works none out. What it can ship is
a **manufacturer's published schedule, named**: the table from the maker's maintenance guide and the
reminder intervals that follow from that table. This file is the rules for such a list and an index
of the lists; how each one was made is recorded on its own page, `known-schedules/<id>.md` (see
[The lists](#the-lists)). The code is `KnownSchedules` in `:domain`; the lists are the files in
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
   mileage and every interval after it. A job that fits neither stays in the table only. Where the table
   prints no months but the guide's own footnotes give the oil change a time limit ("10,000 or 1 year"),
   the oil reminder takes those months (the owner's ruling of 2026-10-28, #517); the page says where.
   An oil change a list leaves in miles alone gets Glovebox's worked-out months when reminders are read
   ([ADR-0015](adr/0015-oil-months-worked-out.md)), never in the list.
   Reminders are given for work that gets
   logged (replace, rotate, lubricate). Of the inspections, only the differential and transfer case oil
   checks are reminders, on every list whose guide prints them (the owner asked for them on the 4Runner on
   2026-10-13, and for every vehicle with a differential on 2026-10-23, #33); logging a change of one counts
   as its check. A list doesn't turn other inspections into reminders.
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
and the code keeps them apart** (`DriveKind`), so nothing marked for one is taken to be for the other.

`allWheelDriveOnly` names the jobs for all-wheel drive only, with "(AWD)" in their names, by the same
rule: only a vehicle **known** to drive two wheels loses them. Toyota's guides print "(4WD)" on some
models it sells as AWD (RAV4, Highlander, Sienna); those jobs go here, named "(AWD)", and the list's page
says the guide prints "(4WD)". `KnownSchedulesTest` holds each field to the mark in the names.

`manualExcluded` names the jobs that aren't for a manual gearbox, such as a dual-clutch transmission's
fluid change. The rule is the same shape: only a vehicle **known** to have a manual (vPIC's
`Manual/Standard`) loses them; an automatic, a dual-clutch or nothing on record keeps them.

`manualOnly` is the other way round: the jobs for a manual gearbox only, such as its oil change. Only a
vehicle **known** to shift itself (an automatic, a CVT, a dual-clutch or an automated manual) loses them;
a manual or nothing on record keeps them. A job can't be in both.

A job a guide gives by time alone ("every 2 years regardless of mileage") is a reminder with months and
no distance, marked `time-only`. The table shows it wherever the maker drew it, and the check doesn't hold
its mileages against it. Where the first time differs ("at 3 years, then every 2"), the reminder is the
repeating part and the job's name says the rest: Glovebox doesn't know the day a car went into service.

**A job the guide gives only as an interval, apart from its table** (Volkswagen's "Additional
Maintenance Items", a "Time-Dependent Maintenance Items" box) goes in `additional`, written as the
`items` of an `interval-schedule` file, with the guide's own figures. It is a reminder and **is never
placed on the table**, so rule 7 has nothing to hold it to; the owner's ruling of 2026-10-23 (#33). A job
that is also in the table, or among `intervals`, by its line or its name, is refused: a guide that prints
one job both ways has to be settled on the list's page first. The drive and gearbox fields name these
jobs as they do any other.

Toyota's footnotes give some jobs' intervals past the end of the table, such as the inverter coolant of a
2016–2024 hybrid (first at 150,000 miles, then every 50,000, on a table that ends at 120,000). Such a job is
in `additional` with the footnote's figures (the owner's ruling of 2026-10-23, #33).

**A row marked "(if applicable)"** names no drivetrain or engine, so it is in the table with no `service`
and drives no reminder (#33). Where the chart joins a job every such vehicle has to one that is only
"if applicable", as the 2025-on "Engine coolant / Intercooler coolant (if applicable)", the row is split:
the engine (or inverter) coolant change is a job of its own with its service and reminder, and the
intercooler's stays "(if applicable)" (the owner's ruling of 2026-10-23).

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

`modelAliases` lists other names NHTSA gives the same car in those years, such as `"Golf GTI"` beside
`"GTI"`: vPIC's model list for a make and year changes its spelling between years, and a list is matched
by the model as decoded or picked. **An alias has to be seen in vPIC's own model list** (`GetModelsForMakeYear`,
which is sent only the make and year) for a year the list covers, and the list's page says where. Never a
near miss or a related model ("Golf" is not the GTI).

Models are matched ignoring case, spaces and a trailing `(PHEV)` or `(HEV)`: vPIC tags some plug-ins and
hybrids that way (`Prius Prime (PHEV)`) and an owner typing the name leaves it off (#475). Any other bracket is
part of the name (Honda's `VT750 (Shadow Aero 750)`). A spelling that differs only by the tag needs no alias.

**A hybrid, plug-in or flex-fuel model and its petrol twin of the same size** are told apart by
`"powertrain"` in the `engine` block: `gas`, `hybrid`, `plug-in`, `flex-fuel` or `electric`. Set it only
where two lists of one size differ by it; a list without it is for every powertrain. A vehicle's powertrain
comes from vPIC only where the decode says it plainly (a strong hybrid, a plug-in, a battery-electric, an
E85 fuel), or from the owner on "Is this your vehicle?". **A blank is never taken for petrol**, so a list
marked `gas` is only matched for a vehicle the owner said is petrol; otherwise setup asks "Which engine
does it have?" with each list's `label` ("2.5L hybrid"), and the pick is written to the vehicle if it had
no powertrain on record. A flex-fuel guide's shorter oil change for a truck run mostly on E85 is the list's
`e85` (below), not left out (the owner's ruling of 2026-10-23, #33); the note on changing oil early after the
wrong grade still is.

**A sedan and a hatchback of one engine whose guides differ** are told apart by `"body"`, at the top level of
the list: `sedan` or `hatchback` (#185, the owner's ruling of 2026-10-25). Set it only where two lists of one
model, years and engine differ by it and NHTSA gives both the same model name, as for the 2026 Corolla; a list
without it is for every body. A vehicle's body style comes from vPIC's `BodyClass` only where it is plainly one
of the two ("Sedan/Saloon", "Hatchback/Liftback/Notchback"), or from the owner on "Is this your vehicle?"
(`vehicles.bodyStyle`). **A blank body matches no list that names one and is never guessed**: setup then asks
"Which one is it?", offering each list's engine `label` with its body ("2.0L 4-cylinder, Hatchback"), and the
pick is written to the vehicle if it had no body style on record.

**`e85`** gives a flex-fuel guide's intervals for a vehicle run on E85 more than half the time, which Toyota's
FFV guides print as an oil change every 2,500 miles / 3 months against the normal 5,000 / 6:

```json
"e85": [ { "name": "Replace engine oil and filter", "everyMiles": 2500, "everyMonths": 3 } ]
```

Each names one of the list's `intervals`, once, and never one that is `severeOnly`; it takes `everyMiles` (or
`everyKm`) and `everyMonths` like an interval, and only a list whose `engine.powertrain` is `flex-fuel` may
have it. The guide's log is drawn for E85 use, so rule 7 holds each E85 interval to the table exactly as it
holds an interval; the job's ordinary interval then only has to fall on the table (the job at every multiple
of it, with the table's months there). An item with one counts by it while **"Runs mostly on E85"** is on: a
switch of its own on setup and the Maintenance schedule, shown only when an item has an E85 interval, off
until the owner turns it on. It is apart from Normal / Severe, which keeps governing the special-condition
jobs (the owner's choice). An import or export file never carries an E85 interval.

## Making a list

1. Get the maker's guide for every model year the list is to cover.
2. Read one year's schedule off the pages, by eye, and write the list from that.
3. Compare the other years with it. Text extraction of these guides is unreliable (two milestones share
   a page, footnote marks run into words, some years have letter-spaced text), so a script's count of
   each job per page is only a pointer: every difference it reports is looked at on the page.
4. Record on the list's own page, `docs/known-schedules/<id>.md` (the `id` of its JSON file), which
   documents were read, what differed, and which years are not covered and why. Add it to the index below.
5. Bundle its bulletins: add the model's NHTSA spellings to `scripts/bulletins.py` and refresh them
   ([docs/bulletins.md](bulletins.md#adding-a-vehicle)). `BulletinsTest` fails until the model has a file.

## The lists

One page per list, named by its `id`:

- [Toyota 4Runner hybrid, 2025–2026](known-schedules/toyota-4runner-2025-2026-hybrid.md) (`toyota-4runner-2025-2026-hybrid`)
- [Toyota 4Runner, 2006, 4.0L V6](known-schedules/toyota-4runner-2006-v6.md) (`toyota-4runner-2006-v6`)
- [Toyota 4Runner, 2006, 4.7L V8](known-schedules/toyota-4runner-2006-v8.md) (`toyota-4runner-2006-v8`)
- [Toyota 4Runner, 2007, 4.0L V6](known-schedules/toyota-4runner-2007-v6.md) (`toyota-4runner-2007-v6`)
- [Toyota 4Runner, 2007, 4.7L V8](known-schedules/toyota-4runner-2007-v8.md) (`toyota-4runner-2007-v8`)
- [Toyota 4Runner, 2008, 4.0L V6](known-schedules/toyota-4runner-2008-v6.md) (`toyota-4runner-2008-v6`)
- [Toyota 4Runner, 2008, 4.7L V8](known-schedules/toyota-4runner-2008-v8.md) (`toyota-4runner-2008-v8`)
- [Toyota 4Runner, 2009, 4.0L V6](known-schedules/toyota-4runner-2009-v6.md) (`toyota-4runner-2009-v6`)
- [Toyota 4Runner, 2009, 4.7L V8](known-schedules/toyota-4runner-2009-v8.md) (`toyota-4runner-2009-v8`)
- [Toyota 4Runner, 2010, 4-cylinder](known-schedules/toyota-4runner-2010-4cyl.md) (`toyota-4runner-2010-4cyl`)
- [Toyota 4Runner, 2010–2012, V6](known-schedules/toyota-4runner-2010-2012-v6.md) (`toyota-4runner-2010-2012-v6`)
- [Toyota 4Runner, 2011, 4-cylinder](known-schedules/toyota-4runner-2011-4cyl.md) (`toyota-4runner-2011-4cyl`)
- [Toyota 4Runner, 2013](known-schedules/toyota-4runner-2013.md) (`toyota-4runner-2013`)
- [Toyota 4Runner, 2014](known-schedules/toyota-4runner-2014.md) (`toyota-4runner-2014`)
- [Toyota 4Runner, 2015–2016](known-schedules/toyota-4runner-2015-2016.md) (`toyota-4runner-2015-2016`)
- [Toyota 4Runner, 2017–2024](known-schedules/toyota-4runner-2017-2024.md) (`toyota-4runner-2017-2024`)
- [Toyota 4Runner, 2025–2026, 2.4L turbo](known-schedules/toyota-4runner-2025-2026-gas.md) (`toyota-4runner-2025-2026-gas`)
- [Toyota Camry 2.5L, 2018–2019](known-schedules/toyota-camry-2018-2019-2.5.md) (`toyota-camry-2018-2019-2.5`)
- [Toyota Camry 2.5L, 2020](known-schedules/toyota-camry-2020-2.5.md) (`toyota-camry-2020-2.5`)
- [Toyota Camry 2.5L, 2021–2024](known-schedules/toyota-camry-2021-2024-2.5.md) (`toyota-camry-2021-2024-2.5`)
- [Toyota Camry Hybrid, 2017](known-schedules/toyota-camry-2017-hybrid.md) (`toyota-camry-2017-hybrid`)
- [Toyota Camry Hybrid, 2018–2020](known-schedules/toyota-camry-2018-2020-hybrid.md) (`toyota-camry-2018-2020-hybrid`)
- [Toyota Camry Hybrid, 2021–2024](known-schedules/toyota-camry-2021-2024-hybrid.md) (`toyota-camry-2021-2024-hybrid`)
- [Toyota Camry V6, 2018–2020](known-schedules/toyota-camry-2018-2020-v6.md) (`toyota-camry-2018-2020-v6`)
- [Toyota Camry V6, 2021–2024](known-schedules/toyota-camry-2021-2024-v6.md) (`toyota-camry-2021-2024-v6`)
- [Toyota Camry, 2016 (2.5L 4-cylinder)](known-schedules/toyota-camry-2016-4-cylinder.md) (`toyota-camry-2016-4-cylinder`)
- [Toyota Camry, 2016 (3.5L V6)](known-schedules/toyota-camry-2016-v6.md) (`toyota-camry-2016-v6`)
- [Toyota Camry, 2017 (2.5L 4-cylinder)](known-schedules/toyota-camry-2017-4-cylinder.md) (`toyota-camry-2017-4-cylinder`)
- [Toyota Camry, 2017 (3.5L V6)](known-schedules/toyota-camry-2017-v6.md) (`toyota-camry-2017-v6`)
- [Toyota Camry, 2025](known-schedules/toyota-camry-2025.md) (`toyota-camry-2025`)
- [Toyota Camry, 2026](known-schedules/toyota-camry-2026.md) (`toyota-camry-2026`)
- [Toyota Camry, 2027](known-schedules/toyota-camry-2027.md) (`toyota-camry-2027`)
- [Toyota Corolla 1.8L, 2016](known-schedules/toyota-corolla-2016.md) (`toyota-corolla-2016`)
- [Toyota Corolla 1.8L, 2017–2019](known-schedules/toyota-corolla-2017-2019.md) (`toyota-corolla-2017-2019`)
- [Toyota Corolla 1.8L (gas), 2020–2022](known-schedules/toyota-corolla-2020-2022-1.8.md) (`toyota-corolla-2020-2022-1.8`)
- [Toyota Corolla 2.0L, 2019–2022](known-schedules/toyota-corolla-2019-2022-2.0.md) (`toyota-corolla-2019-2022-2.0`)
- [Toyota Corolla 2.0L, 2023–2024](known-schedules/toyota-corolla-2023-2024-2.0.md) (`toyota-corolla-2023-2024-2.0`)
- [Toyota Corolla 2.0L, 2025](known-schedules/toyota-corolla-2025-2.0.md) (`toyota-corolla-2025-2.0`)
- [Toyota Corolla Cross (gas), 2023–2024](known-schedules/toyota-corolla-cross-2023-2024-gas.md) (`toyota-corolla-cross-2023-2024-gas`)
- [Toyota Corolla Cross (gas), 2025–2026](known-schedules/toyota-corolla-cross-2025-2026-gas.md) (`toyota-corolla-cross-2025-2026-gas`)
- [Toyota Corolla Cross Hybrid, 2023](known-schedules/toyota-corolla-cross-2023-hybrid.md) (`toyota-corolla-cross-2023-hybrid`)
- [Toyota Corolla Cross Hybrid, 2024](known-schedules/toyota-corolla-cross-2024-hybrid.md) (`toyota-corolla-cross-2024-hybrid`)
- [Toyota Corolla Cross Hybrid, 2025](known-schedules/toyota-corolla-cross-2025-hybrid.md) (`toyota-corolla-cross-2025-hybrid`)
- [Toyota Corolla Cross Hybrid, 2026](known-schedules/toyota-corolla-cross-2026-hybrid.md) (`toyota-corolla-cross-2026-hybrid`)
- [Toyota Corolla Cross, 2022](known-schedules/toyota-corolla-cross-2022.md) (`toyota-corolla-cross-2022`)
- [Toyota Corolla hatchback, 2026](known-schedules/toyota-corolla-2026-hatchback.md) (`toyota-corolla-2026-hatchback`)
- [Toyota Corolla Hybrid, 2020](known-schedules/toyota-corolla-2020-hybrid.md) (`toyota-corolla-2020-hybrid`)
- [Toyota Corolla Hybrid, 2021–2022](known-schedules/toyota-corolla-2021-2022-hybrid.md) (`toyota-corolla-2021-2022-hybrid`)
- [Toyota Corolla Hybrid, 2023–2024](known-schedules/toyota-corolla-2023-2024-hybrid.md) (`toyota-corolla-2023-2024-hybrid`)
- [Toyota Corolla Hybrid, 2025–2026](known-schedules/toyota-corolla-2025-2026-hybrid.md) (`toyota-corolla-2025-2026-hybrid`)
- [Toyota Corolla sedan (gas), 2026](known-schedules/toyota-corolla-2026-sedan.md) (`toyota-corolla-2026-sedan`)
- [Toyota Grand Highlander 2.4L, 2024](known-schedules/toyota-grand-highlander-2024-2.4.md) (`toyota-grand-highlander-2024-2.4`)
- [Toyota Grand Highlander 2.4L, 2025](known-schedules/toyota-grand-highlander-2025-2.4.md) (`toyota-grand-highlander-2025-2.4`)
- [Toyota Grand Highlander 2.4L, 2026](known-schedules/toyota-grand-highlander-2026-2.4.md) (`toyota-grand-highlander-2026-2.4`)
- [Toyota Grand Highlander Hybrid MAX, 2024](known-schedules/toyota-grand-highlander-2024-2.4-hybrid.md) (`toyota-grand-highlander-2024-2.4-hybrid`)
- [Toyota Grand Highlander Hybrid MAX, 2025](known-schedules/toyota-grand-highlander-2025-2.4-hybrid.md) (`toyota-grand-highlander-2025-2.4-hybrid`)
- [Toyota Grand Highlander Hybrid MAX, 2026](known-schedules/toyota-grand-highlander-2026-2.4-hybrid.md) (`toyota-grand-highlander-2026-2.4-hybrid`)
- [Toyota Grand Highlander Hybrid, 2024](known-schedules/toyota-grand-highlander-2024-2.5-hybrid.md) (`toyota-grand-highlander-2024-2.5-hybrid`)
- [Toyota Grand Highlander Hybrid, 2025](known-schedules/toyota-grand-highlander-2025-2.5-hybrid.md) (`toyota-grand-highlander-2025-2.5-hybrid`)
- [Toyota Grand Highlander Hybrid, 2026](known-schedules/toyota-grand-highlander-2026-2.5-hybrid.md) (`toyota-grand-highlander-2026-2.5-hybrid`)
- [Toyota Highlander 2.4L turbo, 2023](known-schedules/toyota-highlander-2023-turbo.md) (`toyota-highlander-2023-turbo`)
- [Toyota Highlander 2.4L turbo, 2024](known-schedules/toyota-highlander-2024-turbo.md) (`toyota-highlander-2024-turbo`)
- [Toyota Highlander 2.4L turbo, 2025](known-schedules/toyota-highlander-2025-turbo.md) (`toyota-highlander-2025-turbo`)
- [Toyota Highlander 2.4L turbo, 2026](known-schedules/toyota-highlander-2026-turbo.md) (`toyota-highlander-2026-turbo`)
- [Toyota Highlander 2.5L hybrid, 2020–2023](known-schedules/toyota-highlander-2020-2023-hybrid.md) (`toyota-highlander-2020-2023-hybrid`)
- [Toyota Highlander 2.5L hybrid, 2024](known-schedules/toyota-highlander-2024-hybrid.md) (`toyota-highlander-2024-hybrid`)
- [Toyota Highlander 2.5L hybrid, 2025](known-schedules/toyota-highlander-2025-hybrid.md) (`toyota-highlander-2025-hybrid`)
- [Toyota Highlander 2.5L hybrid, 2026](known-schedules/toyota-highlander-2026-hybrid.md) (`toyota-highlander-2026-hybrid`)
- [Toyota Highlander 2.7L, 2016](known-schedules/toyota-highlander-2016-2.7.md) (`toyota-highlander-2016-2.7`)
- [Toyota Highlander 2.7L, 2017](known-schedules/toyota-highlander-2017-2.7.md) (`toyota-highlander-2017-2.7`)
- [Toyota Highlander 2.7L, 2018–2019](known-schedules/toyota-highlander-2018-2019-2.7.md) (`toyota-highlander-2018-2019-2.7`)
- [Toyota Highlander 3.5L V6, 2016](known-schedules/toyota-highlander-2016-3.5.md) (`toyota-highlander-2016-3.5`)
- [Toyota Highlander 3.5L V6, 2017](known-schedules/toyota-highlander-2017-3.5.md) (`toyota-highlander-2017-3.5`)
- [Toyota Highlander 3.5L V6, 2018–2019](known-schedules/toyota-highlander-2018-2019-3.5.md) (`toyota-highlander-2018-2019-3.5`)
- [Toyota Highlander 3.5L V6, 2020–2022](known-schedules/toyota-highlander-2020-2022-v6.md) (`toyota-highlander-2020-2022-v6`)
- [Toyota Highlander Hybrid, 2016](known-schedules/toyota-highlander-2016-hybrid.md) (`toyota-highlander-2016-hybrid`)
- [Toyota Highlander Hybrid, 2017](known-schedules/toyota-highlander-2017-hybrid.md) (`toyota-highlander-2017-hybrid`)
- [Toyota Highlander Hybrid, 2018–2019](known-schedules/toyota-highlander-2018-2019-hybrid.md) (`toyota-highlander-2018-2019-hybrid`)
- [Toyota Prius Prime, 2017](known-schedules/toyota-prius-prime-2017.md) (`toyota-prius-prime-2017`)
- [Toyota Prius Prime, 2018–2020](known-schedules/toyota-prius-prime-2018-2020.md) (`toyota-prius-prime-2018-2020`)
- [Toyota Prius Prime, 2021–2022](known-schedules/toyota-prius-prime-2021-2022.md) (`toyota-prius-prime-2021-2022`)
- [Toyota Prius Prime, 2023–2024](known-schedules/toyota-prius-prime-2023-2024.md) (`toyota-prius-prime-2023-2024`)
- [Toyota Prius Prime, 2025–2026](known-schedules/toyota-prius-prime-2025-2026.md) (`toyota-prius-prime-2025-2026`)
- [Toyota Prius Prime, 2027](known-schedules/toyota-prius-prime-2027.md) (`toyota-prius-prime-2027`)
- [Toyota Prius, 2016](known-schedules/toyota-prius-2016.md) (`toyota-prius-2016`)
- [Toyota Prius, 2017](known-schedules/toyota-prius-2017.md) (`toyota-prius-2017`)
- [Toyota Prius, 2018](known-schedules/toyota-prius-2018.md) (`toyota-prius-2018`)
- [Toyota Prius, 2019–2020](known-schedules/toyota-prius-2019-2020.md) (`toyota-prius-2019-2020`)
- [Toyota Prius, 2021–2022](known-schedules/toyota-prius-2021-2022.md) (`toyota-prius-2021-2022`)
- [Toyota Prius, 2023–2024](known-schedules/toyota-prius-2023-2024.md) (`toyota-prius-2023-2024`)
- [Toyota Prius, 2025–2026](known-schedules/toyota-prius-2025-2026.md) (`toyota-prius-2025-2026`)
- [Toyota Prius, 2027](known-schedules/toyota-prius-2027.md) (`toyota-prius-2027`)
- [Toyota RAV4 (gas), 2019–2024](known-schedules/toyota-rav4-2019-2024-gas.md) (`toyota-rav4-2019-2024-gas`)
- [Toyota RAV4 (gas), 2025](known-schedules/toyota-rav4-2025-gas.md) (`toyota-rav4-2025-gas`)
- [Toyota RAV4 Hybrid, 2016](known-schedules/toyota-rav4-2016-hybrid.md) (`toyota-rav4-2016-hybrid`)
- [Toyota RAV4 Hybrid, 2017](known-schedules/toyota-rav4-2017-hybrid.md) (`toyota-rav4-2017-hybrid`)
- [Toyota RAV4 Hybrid, 2018](known-schedules/toyota-rav4-2018-hybrid.md) (`toyota-rav4-2018-hybrid`)
- [Toyota RAV4 Hybrid, 2019–2020](known-schedules/toyota-rav4-2019-2020-hybrid.md) (`toyota-rav4-2019-2020-hybrid`)
- [Toyota RAV4 Hybrid, 2021–2024](known-schedules/toyota-rav4-2021-2024-hybrid.md) (`toyota-rav4-2021-2024-hybrid`)
- [Toyota RAV4 Hybrid, 2025](known-schedules/toyota-rav4-2025-hybrid.md) (`toyota-rav4-2025-hybrid`)
- [Toyota RAV4 Hybrid, 2026](known-schedules/toyota-rav4-2026-hybrid.md) (`toyota-rav4-2026-hybrid`)
- [Toyota RAV4 Plug-in Hybrid (Prime), 2025](known-schedules/toyota-rav4-prime-2025.md) (`toyota-rav4-prime-2025`)
- [Toyota RAV4 Plug-in Hybrid, 2026](known-schedules/toyota-rav4-2026-plug-in.md) (`toyota-rav4-2026-plug-in`)
- [Toyota RAV4 Prime, 2021–2024](known-schedules/toyota-rav4-prime-2021-2024.md) (`toyota-rav4-prime-2021-2024`)
- [Toyota RAV4, 2016 (gasoline)](known-schedules/toyota-rav4-2016-gas.md) (`toyota-rav4-2016-gas`)
- [Toyota RAV4, 2017–2018 (gasoline)](known-schedules/toyota-rav4-2017-2018-gas.md) (`toyota-rav4-2017-2018-gas`)
- [Toyota Sequoia, 2016 (flex-fuel)](known-schedules/toyota-sequoia-2016-ffv.md) (`toyota-sequoia-2016-ffv`)
- [Toyota Sequoia, 2016 (gasoline)](known-schedules/toyota-sequoia-2016-gas.md) (`toyota-sequoia-2016-gas`)
- [Toyota Sequoia, 2017 (flex-fuel)](known-schedules/toyota-sequoia-2017-ffv.md) (`toyota-sequoia-2017-ffv`)
- [Toyota Sequoia, 2017 (gasoline)](known-schedules/toyota-sequoia-2017-gas.md) (`toyota-sequoia-2017-gas`)
- [Toyota Sequoia, 2018 (flex-fuel)](known-schedules/toyota-sequoia-2018-ffv.md) (`toyota-sequoia-2018-ffv`)
- [Toyota Sequoia, 2018 (gasoline)](known-schedules/toyota-sequoia-2018-gas.md) (`toyota-sequoia-2018-gas`)
- [Toyota Sequoia, 2019–2022](known-schedules/toyota-sequoia-2019-2022.md) (`toyota-sequoia-2019-2022`)
- [Toyota Sequoia, 2023–2024](known-schedules/toyota-sequoia-2023-2024.md) (`toyota-sequoia-2023-2024`)
- [Toyota Sequoia, 2025–2026](known-schedules/toyota-sequoia-2025-2026.md) (`toyota-sequoia-2025-2026`)
- [Toyota Sienna, 2017](known-schedules/toyota-sienna-2017.md) (`toyota-sienna-2017`)
- [Toyota Sienna, 2018–2020](known-schedules/toyota-sienna-2018-2020.md) (`toyota-sienna-2018-2020`)
- [Toyota Sienna, 2021](known-schedules/toyota-sienna-2021.md) (`toyota-sienna-2021`)
- [Toyota Sienna, 2022](known-schedules/toyota-sienna-2022.md) (`toyota-sienna-2022`)
- [Toyota Sienna, 2023–2024](known-schedules/toyota-sienna-2023-2024.md) (`toyota-sienna-2023-2024`)
- [Toyota Sienna, 2025](known-schedules/toyota-sienna-2025.md) (`toyota-sienna-2025`)
- [Toyota Sienna, 2026](known-schedules/toyota-sienna-2026.md) (`toyota-sienna-2026`)
- [Toyota Tacoma 2.7L 4-cylinder, 2016](known-schedules/toyota-tacoma-2016-4cyl.md) (`toyota-tacoma-2016-4cyl`)
- [Toyota Tacoma 2.7L 4-cylinder, 2017](known-schedules/toyota-tacoma-2017-4cyl.md) (`toyota-tacoma-2017-4cyl`)
- [Toyota Tacoma 2.7L 4-cylinder, 2018–2023](known-schedules/toyota-tacoma-2018-2023-4cyl.md) (`toyota-tacoma-2018-2023-4cyl`)
- [Toyota Tacoma 3.5L V6, 2016](known-schedules/toyota-tacoma-2016-v6.md) (`toyota-tacoma-2016-v6`)
- [Toyota Tacoma 3.5L V6, 2017](known-schedules/toyota-tacoma-2017-v6.md) (`toyota-tacoma-2017-v6`)
- [Toyota Tacoma 3.5L V6, 2018](known-schedules/toyota-tacoma-2018-v6.md) (`toyota-tacoma-2018-v6`)
- [Toyota Tacoma 3.5L V6, 2019](known-schedules/toyota-tacoma-2019-v6.md) (`toyota-tacoma-2019-v6`)
- [Toyota Tacoma 3.5L V6, 2020–2023](known-schedules/toyota-tacoma-2020-2023-v6.md) (`toyota-tacoma-2020-2023-v6`)
- [Toyota Tacoma, 2024 (gas)](known-schedules/toyota-tacoma-2024-gas.md) (`toyota-tacoma-2024-gas`)
- [Toyota Tacoma, 2024 (hybrid)](known-schedules/toyota-tacoma-2024-hybrid.md) (`toyota-tacoma-2024-hybrid`)
- [Toyota Tacoma, 2025–2026 (gas)](known-schedules/toyota-tacoma-2025-2026-gas.md) (`toyota-tacoma-2025-2026-gas`)
- [Toyota Tacoma, 2025–2026 (hybrid)](known-schedules/toyota-tacoma-2025-2026-hybrid.md) (`toyota-tacoma-2025-2026-hybrid`)
- [Toyota Tundra 4.6L V8, 2016](known-schedules/toyota-tundra-2016-4.6.md) (`toyota-tundra-2016-4.6`)
- [Toyota Tundra 4.6L V8, 2017](known-schedules/toyota-tundra-2017-4.6.md) (`toyota-tundra-2017-4.6`)
- [Toyota Tundra 4.6L V8, 2018–2019](known-schedules/toyota-tundra-2018-2019-4.6.md) (`toyota-tundra-2018-2019-4.6`)
- [Toyota Tundra 5.7L V8 flex-fuel (FFV), 2016](known-schedules/toyota-tundra-2016-5.7-ffv.md) (`toyota-tundra-2016-5.7-ffv`)
- [Toyota Tundra 5.7L V8 flex-fuel (FFV), 2017](known-schedules/toyota-tundra-2017-5.7-ffv.md) (`toyota-tundra-2017-5.7-ffv`)
- [Toyota Tundra 5.7L V8 flex-fuel (FFV), 2018](known-schedules/toyota-tundra-2018-5.7-ffv.md) (`toyota-tundra-2018-5.7-ffv`)
- [Toyota Tundra 5.7L V8 flex-fuel (FFV), 2019](known-schedules/toyota-tundra-2019-5.7-ffv.md) (`toyota-tundra-2019-5.7-ffv`)
- [Toyota Tundra 5.7L V8, 2016](known-schedules/toyota-tundra-2016-5.7.md) (`toyota-tundra-2016-5.7`)
- [Toyota Tundra 5.7L V8, 2017](known-schedules/toyota-tundra-2017-5.7.md) (`toyota-tundra-2017-5.7`)
- [Toyota Tundra 5.7L V8, 2018–2019](known-schedules/toyota-tundra-2018-2019-5.7.md) (`toyota-tundra-2018-2019-5.7`)
- [Toyota Tundra, 2020–2021](known-schedules/toyota-tundra-2020-2021.md) (`toyota-tundra-2020-2021`)
- [Toyota Tundra, 2022 (gas)](known-schedules/toyota-tundra-2022-gas.md) (`toyota-tundra-2022-gas`)
- [Toyota Tundra, 2022 (hybrid)](known-schedules/toyota-tundra-2022-hybrid.md) (`toyota-tundra-2022-hybrid`)
- [Toyota Tundra, 2023 (hybrid)](known-schedules/toyota-tundra-2023-hybrid.md) (`toyota-tundra-2023-hybrid`)
- [Toyota Tundra, 2023–2024 (gas)](known-schedules/toyota-tundra-2023-2024-gas.md) (`toyota-tundra-2023-2024-gas`)
- [Toyota Tundra, 2024 (hybrid)](known-schedules/toyota-tundra-2024-hybrid.md) (`toyota-tundra-2024-hybrid`)
- [Toyota Tundra, 2025–2026 (gas)](known-schedules/toyota-tundra-2025-2026-gas.md) (`toyota-tundra-2025-2026-gas`)
- [Toyota Tundra, 2025–2026 (hybrid)](known-schedules/toyota-tundra-2025-2026-hybrid.md) (`toyota-tundra-2025-2026-hybrid`)
- [Volkswagen Atlas 2.0T, 2018](known-schedules/volkswagen-atlas-2018-2.0t.md) (`volkswagen-atlas-2018-2.0t`)
- [Volkswagen Atlas 3.6L, 2018](known-schedules/volkswagen-atlas-2018-3.6.md) (`volkswagen-atlas-2018-3.6`)
- [Volkswagen Atlas Cross Sport, 2020–2021](known-schedules/volkswagen-atlas-cross-sport-2020-2021.md) (`volkswagen-atlas-cross-sport-2020-2021`)
- [Volkswagen Atlas Cross Sport, 2022](known-schedules/volkswagen-atlas-cross-sport-2022.md) (`volkswagen-atlas-cross-sport-2022`)
- [Volkswagen Atlas Cross Sport, 2025](known-schedules/volkswagen-atlas-cross-sport-2025.md) (`volkswagen-atlas-cross-sport-2025`)
- [Volkswagen Atlas Cross Sport, 2026](known-schedules/volkswagen-atlas-cross-sport-2026.md) (`volkswagen-atlas-cross-sport-2026`)
- [Volkswagen Atlas, 2019](known-schedules/volkswagen-atlas-2019.md) (`volkswagen-atlas-2019`)
- [Volkswagen Atlas, 2020–2021](known-schedules/volkswagen-atlas-2020-2021.md) (`volkswagen-atlas-2020-2021`)
- [Volkswagen Atlas, 2022](known-schedules/volkswagen-atlas-2022.md) (`volkswagen-atlas-2022`)
- [Volkswagen Atlas, 2023–2026](known-schedules/volkswagen-atlas-2023-2026.md) (`volkswagen-atlas-2023-2026`)
- [Volkswagen Beetle 1.8T, 2014 (A5)](known-schedules/volkswagen-beetle-2014-1.8t.md) (`volkswagen-beetle-2014-1.8t`)
- [Volkswagen Beetle 2.0T, 2014 (A5)](known-schedules/volkswagen-beetle-2014-2.0t.md) (`volkswagen-beetle-2014-2.0t`)
- [Volkswagen Beetle 2.5L, 2014 (A5)](known-schedules/volkswagen-beetle-2014-2.5.md) (`volkswagen-beetle-2014-2.5`)
- [Volkswagen Beetle, 2018 (A5)](known-schedules/volkswagen-beetle-2018.md) (`volkswagen-beetle-2018`)
- [Volkswagen Beetle, 2019 (A5)](known-schedules/volkswagen-beetle-2019.md) (`volkswagen-beetle-2019`)
- [Volkswagen CC 2.0T, 2014–2015](known-schedules/volkswagen-cc-2014-2015-2.0t.md) (`volkswagen-cc-2014-2015-2.0t`)
- [Volkswagen CC 2.0T, 2016](known-schedules/volkswagen-cc-2016-2.0t.md) (`volkswagen-cc-2016-2.0t`)
- [Volkswagen CC 3.6L, 2014–2015](known-schedules/volkswagen-cc-2014-2015-3.6.md) (`volkswagen-cc-2014-2015-3.6`)
- [Volkswagen CC 3.6L, 2016](known-schedules/volkswagen-cc-2016-3.6.md) (`volkswagen-cc-2016-3.6`)
- [Volkswagen e-Golf, 2015](known-schedules/volkswagen-e-golf-2015.md) (`volkswagen-e-golf-2015`)
- [Volkswagen e-Golf, 2016–2017](known-schedules/volkswagen-e-golf-2016-2017.md) (`volkswagen-e-golf-2016-2017`)
- [Volkswagen Eos, 2010](known-schedules/volkswagen-eos-2010.md) (`volkswagen-eos-2010`)
- [Volkswagen Eos, 2014–2015](known-schedules/volkswagen-eos-2014-2015.md) (`volkswagen-eos-2014-2015`)
- [Volkswagen Eos, 2016](known-schedules/volkswagen-eos-2016.md) (`volkswagen-eos-2016`)
- [Volkswagen Golf 1.4T, 2019 (Mk7)](known-schedules/volkswagen-golf-2019-1.4t.md) (`volkswagen-golf-2019-1.4t`)
- [Volkswagen Golf 1.4T, 2020–2021 (Mk7)](known-schedules/volkswagen-golf-2020-2021-1.4t.md) (`volkswagen-golf-2020-2021-1.4t`)
- [Volkswagen Golf 1.8T, 2015 (Mk7)](known-schedules/volkswagen-golf-2015-1.8t.md) (`volkswagen-golf-2015-1.8t`)
- [Volkswagen Golf 1.8T, 2016 (Mk7)](known-schedules/volkswagen-golf-2016-1.8t.md) (`volkswagen-golf-2016-1.8t`)
- [Volkswagen Golf 2.5L, 2010 (Mk6)](known-schedules/volkswagen-golf-2010-2.5.md) (`volkswagen-golf-2010-2.5`)
- [Volkswagen Golf 2.5L, 2014 (Mk6)](known-schedules/volkswagen-golf-2014-2.5.md) (`volkswagen-golf-2014-2.5`)
- [Volkswagen Golf Alltrack, 2018 (Mk7)](known-schedules/volkswagen-golf-alltrack-2018.md) (`volkswagen-golf-alltrack-2018`)
- [Volkswagen Golf Alltrack, 2019 (Mk7)](known-schedules/volkswagen-golf-alltrack-2019.md) (`volkswagen-golf-alltrack-2019`)
- [Volkswagen Golf R, 2013 (Mk6)](known-schedules/volkswagen-golf-r-2013.md) (`volkswagen-golf-r-2013`)
- [Volkswagen Golf R, 2015 (Mk7)](known-schedules/volkswagen-golf-r-2015.md) (`volkswagen-golf-r-2015`)
- [Volkswagen Golf R, 2016 (Mk7)](known-schedules/volkswagen-golf-r-2016.md) (`volkswagen-golf-r-2016`)
- [Volkswagen Golf R, 2018 (Mk7)](known-schedules/volkswagen-golf-r-2018.md) (`volkswagen-golf-r-2018`)
- [Volkswagen Golf R, 2019 (Mk7)](known-schedules/volkswagen-golf-r-2019.md) (`volkswagen-golf-r-2019`)
- [Volkswagen Golf R, 2022 (Mk8)](known-schedules/volkswagen-golf-r-2022.md) (`volkswagen-golf-r-2022`)
- [Volkswagen Golf R, 2023–2025 (Mk8)](known-schedules/volkswagen-golf-r-2023-2025.md) (`volkswagen-golf-r-2023-2025`)
- [Volkswagen Golf R, 2026 (Mk8)](known-schedules/volkswagen-golf-r-2026.md) (`volkswagen-golf-r-2026`)
- [Volkswagen Golf SportWagen 1.4T, 2019 (Mk7)](known-schedules/volkswagen-golf-sportwagen-2019-1.4t.md) (`volkswagen-golf-sportwagen-2019-1.4t`)
- [Volkswagen Golf SportWagen 1.8T, 2015 (Mk7)](known-schedules/volkswagen-golf-sportwagen-2015-1.8t.md) (`volkswagen-golf-sportwagen-2015-1.8t`)
- [Volkswagen Golf SportWagen 1.8T, 2016 (Mk7)](known-schedules/volkswagen-golf-sportwagen-2016-1.8t.md) (`volkswagen-golf-sportwagen-2016-1.8t`)
- [Volkswagen Golf SportWagen 1.8T, 2019 (Mk7)](known-schedules/volkswagen-golf-sportwagen-2019-1.8t.md) (`volkswagen-golf-sportwagen-2019-1.8t`)
- [Volkswagen Golf SportWagen, 2018 (Mk7)](known-schedules/volkswagen-golf-sportwagen-2018.md) (`volkswagen-golf-sportwagen-2018`)
- [Volkswagen Golf TDI, 2010 (Mk6)](known-schedules/volkswagen-golf-2010-tdi.md) (`volkswagen-golf-2010-tdi`)
- [Volkswagen Golf, 2018 (Mk7)](known-schedules/volkswagen-golf-2018.md) (`volkswagen-golf-2018`)
- [Volkswagen GTI, 2006 (Mk5)](known-schedules/volkswagen-gti-2006.md) (`volkswagen-gti-2006`)
- [Volkswagen GTI, 2007 (Mk5)](known-schedules/volkswagen-gti-2007.md) (`volkswagen-gti-2007`)
- [Volkswagen GTI, 2008 (Mk5)](known-schedules/volkswagen-gti-2008.md) (`volkswagen-gti-2008`)
- [Volkswagen GTI, 2009 (Mk5)](known-schedules/volkswagen-gti-2009.md) (`volkswagen-gti-2009`)
- [Volkswagen GTI, 2010–2014 (Mk6)](known-schedules/volkswagen-gti-2010-2014.md) (`volkswagen-gti-2010-2014`)
- [Volkswagen GTI, 2015 (Mk7)](known-schedules/volkswagen-gti-2015.md) (`volkswagen-gti-2015`)
- [Volkswagen GTI, 2016 (Mk7)](known-schedules/volkswagen-gti-2016.md) (`volkswagen-gti-2016`)
- [Volkswagen GTI, 2017 (Mk7)](known-schedules/volkswagen-gti-2017.md) (`volkswagen-gti-2017`)
- [Volkswagen GTI, 2018 (Mk7)](known-schedules/volkswagen-gti-2018.md) (`volkswagen-gti-2018`)
- [Volkswagen GTI, 2019 (Mk7)](known-schedules/volkswagen-gti-2019.md) (`volkswagen-gti-2019`)
- [Volkswagen GTI, 2020–2021 (Mk7)](known-schedules/volkswagen-gti-2020-2021.md) (`volkswagen-gti-2020-2021`)
- [Volkswagen GTI, 2022 (Mk8)](known-schedules/volkswagen-gti-2022.md) (`volkswagen-gti-2022`)
- [Volkswagen GTI, 2023–2026 (Mk8)](known-schedules/volkswagen-gti-2023-2026.md) (`volkswagen-gti-2023-2026`)
- [Volkswagen Jetta 1.4T, 2016 (Mk6)](known-schedules/volkswagen-jetta-2016-1.4t.md) (`volkswagen-jetta-2016-1.4t`)
- [Volkswagen Jetta 1.4T, 2019 (Mk7)](known-schedules/volkswagen-jetta-2019-1.4t.md) (`volkswagen-jetta-2019-1.4t`)
- [Volkswagen Jetta 1.4T, 2020–2021 (Mk7)](known-schedules/volkswagen-jetta-2020-2021-1.4t.md) (`volkswagen-jetta-2020-2021-1.4t`)
- [Volkswagen Jetta 1.8T, 2014 (Mk6)](known-schedules/volkswagen-jetta-2014-1.8t.md) (`volkswagen-jetta-2014-1.8t`)
- [Volkswagen Jetta 1.8T, 2015 (Mk6)](known-schedules/volkswagen-jetta-2015-1.8t.md) (`volkswagen-jetta-2015-1.8t`)
- [Volkswagen Jetta 1.8T, 2016 (Mk6)](known-schedules/volkswagen-jetta-2016-1.8t.md) (`volkswagen-jetta-2016-1.8t`)
- [Volkswagen Jetta 2.0T, 2010 (Mk5)](known-schedules/volkswagen-jetta-2010-2.0t.md) (`volkswagen-jetta-2010-2.0t`)
- [Volkswagen Jetta 2.5L, 2010 (Mk5)](known-schedules/volkswagen-jetta-2010-2.5.md) (`volkswagen-jetta-2010-2.5`)
- [Volkswagen Jetta TDI, 2010 (Mk5)](known-schedules/volkswagen-jetta-2010-tdi.md) (`volkswagen-jetta-2010-tdi`)
- [Volkswagen Jetta, 2018 (Mk6)](known-schedules/volkswagen-jetta-2018.md) (`volkswagen-jetta-2018`)
- [Volkswagen Jetta, 2022 (Mk7)](known-schedules/volkswagen-jetta-2022.md) (`volkswagen-jetta-2022`)
- [Volkswagen Jetta, 2023 (Mk7)](known-schedules/volkswagen-jetta-2023.md) (`volkswagen-jetta-2023`)
- [Volkswagen Jetta, 2024–2026 (Mk7)](known-schedules/volkswagen-jetta-2024-2026.md) (`volkswagen-jetta-2024-2026`)
- [Volkswagen Jetta GLI, 2019 (Mk7)](known-schedules/volkswagen-jetta-gli-2019.md) (`volkswagen-jetta-gli-2019`)
- [Volkswagen Jetta GLI, 2020–2021 (Mk7)](known-schedules/volkswagen-jetta-gli-2020-2021.md) (`volkswagen-jetta-gli-2020-2021`)
- [Volkswagen Jetta GLI, 2022 (Mk7)](known-schedules/volkswagen-jetta-gli-2022.md) (`volkswagen-jetta-gli-2022`)
- [Volkswagen Jetta GLI, 2023–2026 (Mk7)](known-schedules/volkswagen-jetta-gli-2023-2026.md) (`volkswagen-jetta-gli-2023-2026`)
- [Volkswagen Jetta Hybrid, 2015](known-schedules/volkswagen-jetta-2015-hybrid.md) (`volkswagen-jetta-2015-hybrid`)
- [Volkswagen Jetta Hybrid, 2016](known-schedules/volkswagen-jetta-2016-hybrid.md) (`volkswagen-jetta-2016-hybrid`)
- [Volkswagen Jetta SportWagen 2.0T, 2010 (Mk5)](known-schedules/volkswagen-jetta-sportwagen-2010-2.0t.md) (`volkswagen-jetta-sportwagen-2010-2.0t`)
- [Volkswagen Jetta SportWagen 2.5L, 2010 (Mk5)](known-schedules/volkswagen-jetta-sportwagen-2010-2.5.md) (`volkswagen-jetta-sportwagen-2010-2.5`)
- [Volkswagen Jetta SportWagen 2.5L, 2014 (Mk5)](known-schedules/volkswagen-jetta-sportwagen-2014-2.5.md) (`volkswagen-jetta-sportwagen-2014-2.5`)
- [Volkswagen Jetta SportWagen TDI, 2010 (Mk5)](known-schedules/volkswagen-jetta-sportwagen-2010-tdi.md) (`volkswagen-jetta-sportwagen-2010-tdi`)
- [Volkswagen Passat 1.8T, 2014 (NMS)](known-schedules/volkswagen-passat-2014-1.8t.md) (`volkswagen-passat-2014-1.8t`)
- [Volkswagen Passat 1.8T, 2015 (NMS)](known-schedules/volkswagen-passat-2015-1.8t.md) (`volkswagen-passat-2015-1.8t`)
- [Volkswagen Passat 1.8T, 2016 (NMS)](known-schedules/volkswagen-passat-2016-1.8t.md) (`volkswagen-passat-2016-1.8t`)
- [Volkswagen Passat 2.0T, 2018 (NMS)](known-schedules/volkswagen-passat-2018-2.0t.md) (`volkswagen-passat-2018-2.0t`)
- [Volkswagen Passat 2.5L, 2014 (NMS)](known-schedules/volkswagen-passat-2014-2.5.md) (`volkswagen-passat-2014-2.5`)
- [Volkswagen Passat 3.6L, 2014 (NMS)](known-schedules/volkswagen-passat-2014-3.6.md) (`volkswagen-passat-2014-3.6`)
- [Volkswagen Passat 3.6L, 2018 (NMS)](known-schedules/volkswagen-passat-2018-3.6.md) (`volkswagen-passat-2018-3.6`)
- [Volkswagen Passat TDI, 2014–2015 (NMS)](known-schedules/volkswagen-passat-2014-2015-tdi.md) (`volkswagen-passat-2014-2015-tdi`)
- [Volkswagen Passat TDI, 2016 (NMS)](known-schedules/volkswagen-passat-2016-tdi.md) (`volkswagen-passat-2016-tdi`)
- [Volkswagen Passat, 2010 (B6)](known-schedules/volkswagen-passat-2010.md) (`volkswagen-passat-2010`)
- [Volkswagen Passat, 2019 (NMS)](known-schedules/volkswagen-passat-2019.md) (`volkswagen-passat-2019`)
- [Volkswagen Passat, 2020–2021 (NMS)](known-schedules/volkswagen-passat-2020-2021.md) (`volkswagen-passat-2020-2021`)
- [Volkswagen Passat, 2022 (NMS)](known-schedules/volkswagen-passat-2022.md) (`volkswagen-passat-2022`)
- [Volkswagen Routan 3.8L, 2010](known-schedules/volkswagen-routan-2010-3.8.md) (`volkswagen-routan-2010-3.8`)
- [Volkswagen Routan 4.0L, 2010](known-schedules/volkswagen-routan-2010-4.0.md) (`volkswagen-routan-2010-4.0`)
- [Volkswagen Routan, 2011](known-schedules/volkswagen-routan-2011.md) (`volkswagen-routan-2011`)
- [Volkswagen Routan, 2012](known-schedules/volkswagen-routan-2012.md) (`volkswagen-routan-2012`)
- [Volkswagen Routan, 2013–2014](known-schedules/volkswagen-routan-2013-2014.md) (`volkswagen-routan-2013-2014`)
- [Volkswagen Tiguan Limited, 2018 (1st generation)](known-schedules/volkswagen-tiguan-limited-2018.md) (`volkswagen-tiguan-limited-2018`)
- [Volkswagen Tiguan, 2010 (1st generation)](known-schedules/volkswagen-tiguan-2010.md) (`volkswagen-tiguan-2010`)
- [Volkswagen Tiguan, 2013–2015 (1st generation)](known-schedules/volkswagen-tiguan-2013-2015.md) (`volkswagen-tiguan-2013-2015`)
- [Volkswagen Tiguan, 2016 (1st generation)](known-schedules/volkswagen-tiguan-2016.md) (`volkswagen-tiguan-2016`)
- [Volkswagen Tiguan, 2018 (2nd generation)](known-schedules/volkswagen-tiguan-2018.md) (`volkswagen-tiguan-2018`)
- [Volkswagen Tiguan, 2019 (2nd generation)](known-schedules/volkswagen-tiguan-2019.md) (`volkswagen-tiguan-2019`)
- [Volkswagen Tiguan, 2020–2021 (2nd generation)](known-schedules/volkswagen-tiguan-2020-2021.md) (`volkswagen-tiguan-2020-2021`)
- [Volkswagen Tiguan, 2022 (2nd generation)](known-schedules/volkswagen-tiguan-2022.md) (`volkswagen-tiguan-2022`)
- [Volkswagen Tiguan, 2023–2024 (2nd generation)](known-schedules/volkswagen-tiguan-2023-2024.md) (`volkswagen-tiguan-2023-2024`)
- [Volkswagen Touareg 3.6L, 2010 (1st generation)](known-schedules/volkswagen-touareg-2010-3.6.md) (`volkswagen-touareg-2010-3.6`)
- [Volkswagen Touareg 3.6L, 2014–2015 (2nd generation)](known-schedules/volkswagen-touareg-2014-2015-3.6.md) (`volkswagen-touareg-2014-2015-3.6`)
- [Volkswagen Touareg 3.6L, 2016 (2nd generation)](known-schedules/volkswagen-touareg-2016-3.6.md) (`volkswagen-touareg-2016-3.6`)
- [Volkswagen Touareg Hybrid, 2014 (2nd generation)](known-schedules/volkswagen-touareg-2014-hybrid.md) (`volkswagen-touareg-2014-hybrid`)
- [Volkswagen Touareg TDI, 2010 (1st generation)](known-schedules/volkswagen-touareg-2010-tdi.md) (`volkswagen-touareg-2010-tdi`)
- [Volkswagen Touareg TDI, 2014–2015 (2nd generation)](known-schedules/volkswagen-touareg-2014-2015-tdi.md) (`volkswagen-touareg-2014-2015-tdi`)
- [Volkswagen Touareg TDI, 2016 (2nd generation)](known-schedules/volkswagen-touareg-2016-tdi.md) (`volkswagen-touareg-2016-tdi`)
