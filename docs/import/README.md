# Importing data into Glovebox

Glovebox can read a JSON file you write (or someone shares with you) and add its contents to one of
your vehicles: **Settings → Import**. This folder is the specification of those files.

- `schema/` — one [JSON Schema](https://json-schema.org/) per kind of import. The schema is the rule
  book: if a file passes it, its shape is right.
- `examples/` — a valid file of each kind to copy from. All values are made up.
- `examples/invalid/<kind>/` — files that must be refused, one mistake each. They are what the app's
  tests run; they are also a quick way to see what a rule means.
- `examples/beyond-schema/<kind>/` — the few rules a schema can't state, which the app still enforces.

## What can be imported

| `kind` | What it is | Schema | In the app |
|---|---|---|---|
| `torque-specs` | Fasteners with socket sizes and torque values | [`torque-specs.schema.json`](schema/torque-specs.schema.json) | Yes |
| `interval-schedule` | A schedule with one repeating interval per job | [`interval-schedule.schema.json`](schema/interval-schedule.schema.json) | Yes |
| `service-history` | Maintenance done before you used Glovebox | [`service-history.schema.json`](schema/service-history.schema.json) | Yes |
| `milestone-schedule` | A schedule by mileage point, as an owner's manual prints it | [`milestone-schedule.schema.json`](schema/milestone-schedule.schema.json) | Yes |

## The shape of every file

```json
{
  "glovebox": "import",
  "version": 1,
  "kind": "torque-specs",
  "items": [ ... ]
}
```

- `glovebox` is always `"import"`.
- `version` is the format version, a whole number. This document describes version `1`. A file with
  a higher version than the app knows is refused as made for a newer version. A version-1 file will
  always stay readable.
- `kind` says what `items` holds. One kind per file.
- These are the only fields beside `items`, except where a kind adds one of its own
  (`milestone-schedule` has `repeatsEveryMiles`). A kind's own field in another kind's file is an
  unknown field.
- `"$schema"` may be added so an editor checks the file as you type (see below). Glovebox ignores it.
- **The file never names a vehicle.** You pick the vehicle in the app, so a file can be passed
  between owners of the same model, and it carries no VIN.

The format is deliberately strict:

- A field Glovebox doesn't know refuses the whole file. A misspelled `"torqe"` is an error, not a
  value quietly dropped.
- Nothing is repaired. Text must not start or end with a space or contain line breaks, numbers are
  written without quotes, and a field you don't have is left out, not set to `null` or `""`.
- Units are always written out. Nothing is assumed from context. **A distance is in miles or in
  kilometres, and its field name says which:** `everyMiles` or `everyKm`, `atMiles` or `atKm`,
  `repeatsEveryMiles` or `repeatsEveryKm`, `odometer` (miles) or `odometerKm`. Give one or the other
  for a figure, never both, and **use one unit for the whole file**: a file with miles in one place
  and kilometres in another is refused.
- **The file's unit needn't be the vehicle's.** Each vehicle counts in miles or kilometres. A file in
  the vehicle's own unit is saved exactly as written. A file in the other unit is converted: odometer
  readings exactly, intervals and milestones the way a manual printed for both markets gives them
  (5,000 mi is 8,000 km), and the Import page shows the converted figures before anything is saved.
  A file saved from the app is written in that vehicle's unit.
- Every list and every piece of text has a maximum, given in the schema.

## Checking a file before you import it

You don't have to: the app checks every file itself and lists what is wrong with it. But a file is
easier to fix at a keyboard than on a phone.

**From a terminal**, with [uv](https://docs.astral.sh/uv/) installed:

```sh
uvx check-jsonschema --schemafile docs/import/schema/torque-specs.schema.json my-specs.json
```

It prints `ok -- validation done`, or each problem and where it is. Without a copy of this
repository, give the schema's address instead:

```sh
uvx check-jsonschema \
  --schemafile https://raw.githubusercontent.com/mcosgriff/glovebox/main/docs/import/schema/torque-specs.schema.json \
  my-specs.json
```

**In an editor** that understands JSON Schema (VS Code and the JetBrains editors do out of the box),
make this the first line inside the file's braces and mistakes are underlined as you type:

```json
"$schema": "https://raw.githubusercontent.com/mcosgriff/glovebox/main/docs/import/schema/torque-specs.schema.json",
```

## What happens to data you already have

Nothing you have is changed unless you say so.

After a file passes, the app shows what it would do before it does anything:

- **New** — nothing on the vehicle matches. These are added.
- **Fills in** — matches a row of yours that is only a name, with nothing entered yet. These are
  filled in, since nothing is lost. For service history, the same goes for a bare "last done" you
  entered, when the file's record for that visit resets the same reminders.
- **Already the same** — matches something of yours that says the same. Left alone.
- **Different from yours** — matches something of yours that says something else. Listed with your
  value beside the file's.

You then choose:

- **Add new only** (the default) — rows that differ are left exactly as they are.
- **Replace matching** — rows that differ are replaced with the file's version, after you confirm a
  list of what will change.

Either way:

- An import **never deletes** anything the file doesn't mention.
- It is **all or nothing**. If any part can't be saved, none of it is.
- What it adds is **your own data**, the same as if you had typed it. It never changes anything
  Glovebox itself supplies.
- Torque specs and schedules go into a vehicle in your garage. Service history can also go into a
  vehicle you have sold.
- There is no Undo. The result screen says exactly what was added, filled in and replaced.

## Privacy

An import reads the one file you pick, on your phone. Nothing is sent anywhere. An import file is
plain text and not encrypted: treat it like any other document. To move all of your data between
phones, use the encrypted backup instead (Settings → Backup).

## Saving a file from the app

Three screens can write what they hold as a file of the matching kind, with "Save as an import
file" at the bottom: the Maintenance schedule (`interval-schedule`), Torque specs (`torque-specs`)
and Milestones (`milestone-schedule`, when the table is your own). It is a copy to keep, or to give
to someone with the same model. Service history has no such button: that is the vehicle's own
record, and the PDF / Markdown export and the backup are for it.

- **What goes in a schedule file:** every item you have ticked, the typical ones Glovebox suggested
  as well as your own, with both intervals as you have set them. A typical item is written with the
  `service` that resets it. Not which interval is in use, not a snooze, and not when anything was
  last done. Imported into a vehicle that tracks the same typical items, the file's items are new
  items of that owner's, and the Import page offers to stop tracking the typical ones they double.
- **What goes in a torque file:** every row, filled in or not, with values in the unit you have
  chosen in Settings (to two decimals). They are the values you entered. Whoever gets the file
  should check each against their own service manual, as the Import page tells them.
- **Nothing about the vehicle** is in any of them: no VIN, no history, no dates. Only the suggested
  file name says which model it came from. The file isn't encrypted.
- **A saved file is always one the app accepts.** The screens are looser than the format in a few
  places: an interval can be longer than 500,000 miles or 600 months, a name can run past the limit,
  and two torque rows can have the same name. Anything a file can't hold is listed, with the reason,
  before the file is saved, and you choose whether to save the rest.

## `torque-specs`

[`schema/torque-specs.schema.json`](schema/torque-specs.schema.json) ·
[`examples/torque-specs.json`](examples/torque-specs.json) · up to 500 items

```json
{
  "name": "Lug nuts",
  "category": "wheels",
  "socket": "21 mm",
  "torque": { "value": 100, "unit": "ft-lb" },
  "source": "factory-manual",
  "note": "Star pattern, re-check after 50 mi"
}
```

| Field | Required | Rules |
|---|---|---|
| `name` | yes | 1 to 60 characters |
| `category` | yes | `engine`, `wheels`, `driveline`, `suspension` or `other` |
| `socket` | no | Socket or tool size as you'd say it, up to 30 characters |
| `torque` | no | `value` (a number above 0) and `unit`: `Nm`, `ft-lb` or `in-lb` |
| `source` | no | `own` (the default) or `factory-manual`, which needs a `torque` |
| `note` | no | Up to 140 characters |

- Torque is stored in N·m and shown in the unit you chose in Settings, so write each value in
  whatever unit your source uses. The most accepted is 1000 Nm, 737 ft-lb or 8850 in-lb.
- `factory-manual` is your statement that you copied the value from the factory service manual. It
  is shown as such; Glovebox has no way to check it.
- **Matching:** a row in the file matches a row on the vehicle with the same category and the same
  name, ignoring capitals. The same name can't appear twice under one category in a file (this is
  the rule the schema can't state).
- **Same or different:** socket, torque (compared as a wrench setting, whichever units were used),
  note and, where there is a torque, source.

## `service-history`

[`schema/service-history.schema.json`](schema/service-history.schema.json) ·
[`examples/service-history.json`](examples/service-history.json) · in kilometres: [`examples/service-history-km.json`](examples/service-history-km.json) · up to 1000 records

Maintenance that was done before you kept it in Glovebox: a dealer printout typed up, a spreadsheet,
another app's export. One record per visit.

```json
{
  "date": "2024-03-01",
  "odometer": 41000,
  "work": [
    { "label": "Oil & filter change", "service": "oil" },
    { "label": "Replaced left tail lamp" }
  ],
  "doneBy": { "type": "shop", "name": "Corner Garage" },
  "cost": "62.50",
  "notes": "0W-20, 6.2 qt"
}
```

| Field | Required | Rules |
|---|---|---|
| `date` | yes | `YYYY-MM-DD`, a real date, not in the future |
| `odometer` | this or `odometerKm` | Whole miles, 1 to 9999999: what the dash read that day |
| `odometerKm` | this or `odometer` | The same in whole kilometres, 1 to 9999999 |
| `work` | yes | 1 to 30 lines, each with a `label` (up to 80 characters) and optionally a `service` |
| `doneBy` | yes | `{ "type": "diy" }` or `{ "type": "shop" }`, the shop optionally with a `name` (up to 60 characters) |
| `cost` | no | Dollars as text in quotes, at most two decimals: `"62.50"`. Text, so nothing is rounded |
| `notes` | no | Up to 1000 characters; line breaks are allowed |

`service` says which of Glovebox's own service lines a line of work is, and it is what makes a line
count for a reminder. The values, by the category History files them under:

| Category | `service` values |
|---|---|
| Oil & engine | `oil`, `engine-air`, `spark-plugs`, `plug-wires`, `ignition-coils`, `glow-plugs`, `belt`, `belt-check`, `timing-belt`, `timing-chain`, `pcv`, `engine-sensors`, `turbo`, `valve-cover-gasket`, `head-gasket`, `intake-gasket`, `oil-pan-gasket`, `rear-main-seal`, `engine-mounts` |
| Tires & wheels | `rotation`, `balance`, `alignment`, `new-tires`, `flat`, `tire-check`, `tpms` |
| Brakes | `front-pads`, `rear-pads`, `brake-pads`, `rotors`, `brake-shoes`, `brake-drums`, `brake-fluid`, `brake-fluid-check`, `brake-inspect`, `brake-service`, `calipers`, `wheel-cylinders`, `master-cylinder`, `brake-booster`, `brake-lines`, `parking-brake`, `abs-sensor` |
| Fluids | `trans-fluid`, `power-steering`, `washer`, `fluid-check` |
| Cooling | `coolant`, `cooling-check`, `water-pump`, `thermostat`, `radiator`, `radiator-cap`, `coolant-reservoir`, `coolant-hoses`, `cooling-fan`, `fan-clutch`, `heater-core`, `inverter-coolant`, `intercooler-coolant`, `hybrid-battery-filter` |
| Fuel | `fuel-filter`, `fuel-system`, `throttle-body`, `fuel-injectors`, `fuel-pump`, `evap-system` |
| Exhaust | `oxygen-sensor`, `egr-valve`, `secondary-air-pump`, `catalytic-converter`, `muffler`, `exhaust` |
| Driveline | `diff-fluid`, `front-diff-check`, `front-diff`, `rear-diff-check`, `rear-diff`, `transfer-case-check`, `transfer-case`, `driveshaft`, `cv-axle`, `axle-seals`, `u-joints`, `clutch`, `clutch-hydraulics`, `flywheel`, `trans-pan-gasket`, `shift-cable` |
| Suspension & steering | `shocks`, `springs`, `control-arms`, `ball-joints`, `bushings`, `sway-bar-links`, `wheel-bearing`, `tie-rods`, `steering-rack`, `power-steering-pump`, `steering-parts`, `suspension-check` |
| Cabin & electrical | `cabin-air`, `wipers`, `wipers-check`, `battery`, `battery-check`, `key-fob-battery`, `bulbs`, `headlight-assembly`, `lights-check`, `windows-check`, `window-regulator`, `door-lock-actuator`, `air-conditioning`, `ac-flush`, `ac-compressor`, `ac-condenser`, `blower-motor`, `starter`, `alternator`, `fuses`, `horn` |
| Inspection | `multi-point`, `scheduled-service`, `emissions`, `safety-inspection`, `recall` |
| Other | `accessory`, `windshield`, `mirror`, `door-handle`, `body`, `detail` |

- **Reminders:** a line with a `service` resets the reminder that service resets, exactly as ticking
  it when you log a service does: `oil`, `engine-air`, `spark-plugs`, `rotation`, `brake-fluid`,
  `brake-inspect`, `coolant`, `trans-fluid`, `diff-fluid` and `cabin-air` each have one. A change
  also counts as the check of the same thing: `front-diff`, `rear-diff` and `transfer-case` reset a
  reminder set to `front-diff-check`, `rear-diff-check` or `transfer-case-check`, and `diff-fluid`
  resets all three; likewise `brake-fluid` for `brake-fluid-check`, `belt` for `belt-check`, `battery` for `battery-check`,
  `wipers` for `wipers-check` and `coolant` for `cooling-check`. `ac-flush` also counts as `air-conditioning`, since a
  flush ends with the system refilled. A check never counts as a change. `brake-pads` is pads on an axle the
  record doesn't name, and `scheduled-service` ("the 30,000 mile service") resets nothing, since it
  doesn't say which jobs were done. A line
  *without* a `service` resets one of your own schedule items if its `label` is that item's name
  (ignoring capitals), or, failing that, is filed under the line one of your category rules gives its whole
  wording (rules are kept when you sort Custom lines on History; a `service` in the file always wins). Any other
  line is history only. The preview lists every reminder that would
  move before you import.
- An older record never moves a reminder that a newer service has already set: the last time a job
  was done is always the most recent one in the log.
- **Matching:** a record in the file matches a record on the vehicle with the same date and the same
  odometer. Two records in one file can't share both (one visit is one record; this, and the date
  not being in the future, are the rules the schema can't state).
- **Same or different:** the work lines (in any order, ignoring capitals), who did it, the cost and
  the notes.
- **Replace matching** replaces those four things. Photos on your record stay, and so does a recall
  it was the work for.
- **Readings that don't fit:** an odometer only goes up, so a record whose reading breaks the run
  of the others (lower than earlier ones, or higher than later ones) is pointed out in the preview.
  It doesn't stop the import: check the date and the odometer, in the file or on the vehicle.
- **Marked as imported:** these records say "imported" in History, on the record's screen and in
  exports, so they are never taken for something logged in the app at the time.
- **Mileage:** imported readings count towards the vehicle's odometer and driving pace like any
  other. If the instrument cluster was replaced and you have set an odometer offset, write each
  reading as the dash showed it on that day; Glovebox converts.
- No photos or receipts in version 1.

## `interval-schedule`

[`schema/interval-schedule.schema.json`](schema/interval-schedule.schema.json) ·
[`examples/interval-schedule.json`](examples/interval-schedule.json) · in kilometres: [`examples/interval-schedule-km.json`](examples/interval-schedule-km.json) · up to 200 items

One repeating interval per job: oil every 7,500 miles, tires rotated every 5,000, brake fluid every
3 years.

```json
{
  "name": "Oil change",
  "everyMiles": 7500,
  "everyMonths": 12,
  "severe": { "everyMiles": 5000, "everyMonths": 6 },
  "rule": "whichever-first",
  "service": "oil"
}
```

| Field | Required | Rules |
|---|---|---|
| `name` | yes | 1 to 60 characters, unique in the file ignoring capitals |
| `everyMiles` | a distance, months, or both | Whole miles, 1 to 500000 |
| `everyKm` | in place of `everyMiles` | Whole kilometres, 1 to 800000 |
| `everyMonths` | a distance, months, or both | Whole months, 1 to 600. Write years as months: 3 years is `36` |
| `firstAtMiles` | no | For a job whose first time isn't one interval in: the odometer reading it is first due at, 1 to 500000. "First at 100,000 miles, then every 50,000" is `"firstAtMiles": 100000, "everyMiles": 50000` |
| `firstAtKm` | in place of `firstAtMiles` | The same in kilometres, 1 to 800000. In the same unit as the item |
| `severe` | no | The interval for hard use, if it differs: `everyMiles` or `everyKm`, `everyMonths`, or a distance and months. In the same unit as the item |
| `severeOnly` | no | `true` for a job that exists only under hard use, such as changing the differential oil when towing. Its interval is then the one for hard use; with the schedule set to normal use it stays on the list, switched off, and is never due. It can't also have `severe` |
| `rule` | no | `whichever-first`, `whichever-last`, `mileage-only` or `time-only`. Left out, the item follows the rule you chose in Settings |
| `service` | no | Which of Glovebox's own service lines resets this item. The values are the same as for [`service-history`](#service-history) |

- **Where they go:** each item becomes one of *your own* schedule items, tracked from the start, the
  same as "+ Add your own item". The typical items Glovebox suggested are never changed or removed
  by an import.
- **Matching:** an item in the file matches one of your own items with the same name, ignoring
  capitals. It never matches a typical item, even one with the same name.
- **Same or different:** the interval, the first-time figure, the severe interval, whether it is only for severe use, the rule and the service.
- **The first time:** with `firstAtMiles`, a job that has never been logged is due when the odometer
  reaches that reading, and from then on every interval after the last time it was logged. It is by
  distance only: a manual's "or 10 years" for the first time counts from the day the vehicle went
  into service, which Glovebox doesn't know. Once the odometer is past it with nothing logged, the
  job is listed as not logged yet, like any other.
- **Replace matching** changes only those six things. Whether the item is tracked, a snooze, which
  of its intervals is in use and every service logged against it stay as they are.
- **Two reminders for one job:** if a typical item you are tracking has the same name as an item in
  the file, or is the one the file's `service` resets, the preview says so and offers to stop
  tracking the typical one. That is your choice; left alone, both stay, and logging that service
  resets both. A stopped item keeps its history and can be switched back on.
- **When it was last done isn't part of this file.** New items start as "Not logged yet"; after the
  import the app offers "When were these last done?", or import a `service-history` file.
- **Logging work against them:** an item with a `service` is reset by ticking that built-in line
  when you log a service ("Oil & filter change" for `oil`), and work already in your history under
  that line counts for it straight away. An item without one is listed under "Your items" and is
  logged there. You can set or change this later on the item, in the Maintenance schedule.
- The same job can't appear twice in a file under names that differ only in capitals (this is the
  rule the schema can't state).

## `milestone-schedule`

[`schema/milestone-schedule.schema.json`](schema/milestone-schedule.schema.json) ·
[`examples/milestone-schedule.json`](examples/milestone-schedule.json) · in kilometres: [`examples/milestone-schedule-km.json`](examples/milestone-schedule-km.json) · up to 200 milestones

A schedule the way an owner's manual prints it: mileage points, each with the work due there.

```json
{
  "atMiles": 30000,
  "orMonths": 36,
  "work": [
    { "name": "Replace engine air filter", "service": "engine-air" },
    { "name": "Rotate tires", "service": "rotation" },
    { "name": "Inspect drive belts" }
  ]
}
```

| Field | Required | Rules |
|---|---|---|
| `atMiles` | this or `atKm` | Whole miles the vehicle has covered, 1 to 1000000. No two milestones in a file may share one |
| `atKm` | this or `atMiles` | The same in whole kilometres, 1 to 1600000 |
| `orMonths` | no | The vehicle's age in whole months at which the same work is due if the mileage isn't reached, 1 to 600. Shown for information |
| `work` | yes | 1 to 40 lines, each with a `name` (up to 80 characters) and optionally a `service` |

One optional field goes beside `items`, not inside a milestone, because it is about the table as a
whole:

```json
{
  "glovebox": "import",
  "version": 1,
  "kind": "milestone-schedule",
  "repeatsEveryMiles": 120000,
  "items": [ ... ]
}
```

| Field | Required | Rules |
|---|---|---|
| `repeatsEveryMiles` | no | The length of the whole cycle in whole miles, 1 to 1000000, and at least the highest `atMiles` in the file. Leave it out for a table that ends |
| `repeatsEveryKm` | no | The same for a table in kilometres (`atKm`), 1 to 1600000, and at least the highest `atKm` |

- **It is information, not reminders.** The table is shown on the Milestones screen (from a
  vehicle's Maintenance schedule) in place of the milestones Glovebox builds from your schedule. It
  never creates, changes or silences a reminder: those still come from your schedule items and your
  service log. To get reminders for a job, add it to the schedule (or import an `interval-schedule`).
- **What counts as done:** the screen shows a job as completed when your log has that work. A job
  with a `service` is completed by logged work for that service (the values are the same as for
  [`service-history`](#service-history)). A job without one is completed by logged work whose label
  is the job's `name`, ignoring capitals. A job nothing in the log matches is simply listed.
- **Which milestone a service lands on:** the first milestone listing that job at or after the
  mileage it was done at. Oil changed at 48,000 shows under 50,000. After a job's last milestone,
  later work still shows under that last one; in a table that repeats there is no last one, so it
  shows under the job's milestone in the next round.
- **The same job at several milestones** should have the same `service`, or exactly the same
  `name`, at each: that is how Glovebox knows it is one job repeating.
- **Order doesn't matter** in the file; milestones are shown by mileage.
- **The table ends where your file ends, unless it says it repeats.** Many manuals end with "after
  120,000 miles, repeat from the beginning". `repeatsEveryMiles` says that: every milestone at
  mileage *a* is due again at *a* + 120,000, *a* + 240,000 and so on, with the same work, and the
  Milestones screen keeps going ("Show further ahead"). The number is the length of the whole
  cycle, not the gap after the last row. For a table that runs 10,000 to 120,000 and then starts
  again at 130,000 it is 120000; if the table stops at 100,000 and still starts again at 130,000 it
  is 120000 too. It can't be less than the highest `atMiles`.
- **`orMonths` is not repeated.** It is the vehicle's age when a milestone first comes due, and the
  file doesn't say how old the vehicle will be the next time round, so a repeat shows the mileage
  and the work only.
- **Repeating is all or nothing for a vehicle's table.** The first file imported into a vehicle
  decides whether its table repeats and how often. A later file for the same vehicle has to say the
  same (the same `repeatsEveryMiles`, or none if the table doesn't repeat); one that says otherwise
  is refused before anything is offered, whichever of "Add new only" and "Replace matching" you
  would have picked. To change how a table repeats, remove it and import the corrected file.
- **Mileage is miles the vehicle has really covered,** as a manual means it. If the instrument
  cluster was replaced and you have set an odometer offset, the screen also shows what the dash will
  read at each milestone.
- **Matching:** a milestone in the file matches one already on the vehicle at the same `atMiles`.
  Same or different is the months and the work lines (in any order, ignoring capitals).
  **Replace matching** replaces those and nothing else.
- **Removing it:** "Remove this table" on the Milestones screen deletes the whole table and brings
  back the milestones built from your schedule.
- **Changing it in the app:** an imported table can be edited on the Milestones screen afterwards
  (add, edit or delete a milestone, change whether it repeats), and a table can be typed in there
  from nothing. The app holds a typed table to the same rules as this file. Jobs are kept in the
  order the file lists them.
- **Getting it back out:** "Save as an import file" on the Milestones screen writes the vehicle's
  table as a file of this kind, with `repeatsEveryMiles` if it repeats, to keep or to give to
  someone with the same model. The file holds the table only: nothing about the vehicle it came
  from or the work done on it. It isn't encrypted.
- Within one milestone a `name`, and a `service`, can each appear only once; no two milestones
  can have the same `atMiles`; and `repeatsEveryMiles` can't be less than the highest `atMiles`
  (these are the rules the schema can't state).

**Someone else's file is someone else's numbers.** Check intervals against your owner's manual and
every torque value against your service manual before you rely on them.
