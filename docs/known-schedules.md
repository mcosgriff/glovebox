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
and the code keeps them apart** (`DriveKind`), so nothing marked for one is taken to be for the other.

`allWheelDriveOnly` names the jobs for all-wheel drive only, with "(AWD)" in their names, by the same
rule: only a vehicle **known** to drive two wheels loses them. Toyota's guides print "(4WD)" on some
models it sells as AWD (RAV4, Highlander, Sienna); those jobs go here, named "(AWD)", and the list's page
says the guide prints "(4WD)". `KnownSchedulesTest` holds each field to the mark in the names.

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

**A hybrid, plug-in or flex-fuel model and its petrol twin of the same size** are told apart by
`"powertrain"` in the `engine` block: `gas`, `hybrid`, `plug-in`, `flex-fuel` or `electric`. Set it only
where two lists of one size differ by it; a list without it is for every powertrain. A vehicle's powertrain
comes from vPIC only where the decode says it plainly (a strong hybrid, a plug-in, a battery-electric, an
E85 fuel), or from the owner on "Is this your vehicle?". **A blank is never taken for petrol**, so a list
marked `gas` is only matched for a vehicle the owner said is petrol; otherwise setup asks "Which engine
does it have?" with each list's `label` ("2.5L hybrid"), and the pick is written to the vehicle if it had
no powertrain on record.

## Making a list

1. Get the maker's guide for every model year the list is to cover.
2. Read one year's schedule off the pages, by eye, and write the list from that.
3. Compare the other years with it. Text extraction of these guides is unreliable (two milestones share
   a page, footnote marks run into words, some years have letter-spaced text), so a script's count of
   each job per page is only a pointer: every difference it reports is looked at on the page.
4. Record on the list's own page, `docs/known-schedules/<id>.md` (the `id` of its JSON file), which
   documents were read, what differed, and which years are not covered and why. Add it to the index below.

## The lists

One page per list, named by its `id`:

- [Toyota 4Runner, 2017–2024](known-schedules/toyota-4runner-2017-2024.md) (`toyota-4runner-2017-2024`)
- [Volkswagen GTI, 2010–2014 (Mk6)](known-schedules/volkswagen-gti-2010-2014.md) (`volkswagen-gti-2010-2014`)
