# Schedule and reminders

Rules for setup, maker's schedules, drive and gearbox variants, Severe and E85, statuses, snoozes. Read before changing any of it; review holds a diff to every line here.

- Setup starts from the typical list (`ScheduleTemplates.generic`, labelled typical, not the maker's) **unless a
  bundled maker's schedule covers the vehicle; then that is the schedule** (the user's decision, 2026-10-13).
  Its items become the user's own (`knownList` set) and its table the vehicle's milestones.
- **Never send a vehicle that already has items back through setup**: it inserts every item again.
- Known maintenance schedules (#134, `KnownSchedules`, `docs/known-schedules.md`, which has the rules, and
  `docs/known-schedules/<id>.md`, what was read for each list). **Glovebox has no schedule of its own for any
  model and works none out; a bundled schedule is a maker's published guide, named.** The one exception is an
  oil change's months, below. Only facts are taken, in
  Glovebox's words, for only the years whose guides were read and agree. **An interval has to be the table's
  own** (`KnownSchedules.parse` refuses one that isn't), except a job the guide prints only apart from its table
  (`additional`, never placed on it). Of the inspections, only the differential and transfer
  case oil checks are reminders, wherever a guide prints them (the user's ask). A special-condition job has no
  `service`, and nor has an "(if applicable)" row, but a row joining a job every vehicle has to an "(if
  applicable)" one is split (#33).
- "(4WD)" and "(AWD)" jobs (`fourWheelDriveOnly`, `allWheelDriveOnly`) are left out only for a vehicle **known**
  to be two-wheel drive, `manualExcluded` jobs only for one known to be manual, and `manualOnly` jobs only for
  one known to shift itself. **4WD and AWD are different systems** (`DriveKind`): a guide's "(4WD)" on a model
  sold as AWD goes in `allWheelDriveOnly`, named "(AWD)".
  An engine that isn't on record matches no per-engine list; only setup asks which engine. A list's
  `engine.powertrain` (#33's rulings) tells a hybrid or flex-fuel model from its petrol twin of one size;
  `vehicles.powertrain` is set from vPIC only where it says so plainly, **never "gas" from a blank**. A list's
  `body` (#185) tells a sedan from a hatchback of one engine where their guides differ and NHTSA names both the
  same; `vehicles.bodyStyle` is set from vPIC's `BodyClass` only where it is plainly one of them, or picked on
  "Is this your vehicle?", and a blank matches no list that names a body: setup asks.
- **Taking up a schedule is one transaction** (#429): setup's save and "Use the maker's schedule" go through
  `ScheduleWriter.adopt` (`:data`), which writes the items, stops or removes the replaced doubles, fills a blank
  engine and powertrain, and adds the maker's table (read from the vehicle as it is) unless the vehicle has one.
  The caller builds the item rows from the owner's picks. A process that dies part-way must leave the vehicle as
  it was, never a maker's schedule without its table.
- A vehicle set up earlier is offered "Use the maker's schedule"; there is no replace of the user's own items.
  **A replaced typical item is deleted** (the user found an unticked twin confusing) unless a service was logged
  against it, when it stays untracked. That is the one place a typical item is deleted.
- **A list's source is said once per screen, never under every item** (the user found that too busy).
- `firstMiles` ("first at 100,000, then every 50,000") is distance really covered; **pass the vehicle's swap
  wherever statuses are worked out** (`TrackedItem.of`, through `toTracked(rule, swap)`).
- Severe-only jobs (#136): `TrackedItem.of` returns null for them while `useSevere` is off. **One Normal / Severe
  switch, not one per condition** (the user's choice). The editor can't make one.
- E85 (#33): a flex-fuel list's `e85` intervals are counted by while **"Runs mostly on E85"** is on, a separate
  switch shown only for items that have one, off by default; Normal / Severe is not folded into it (the
  owner's choice). **Take an item's interval through `ActiveInterval`**, never from `useSevere` alone; a reminder
  counts by `ActiveInterval.counted` (`countedInterval(unit)`, `toTracked(rule, swap, unit)`). Import and export
  files don't carry it.
- **Oil months (#517, [ADR-0015](../adr/0015-oil-months-worked-out.md)): the one time limit Glovebox works out.**
  An oil change (the typical `oil` item, or the owner's set to the `oil` line) whose interval in force has miles
  and no months, after the Severe and E85 choice, counts by `OilMonths`: miles ÷ 833, rounded, never more than 12
  (10,000 → 12, 7,500 → 9, 5,000 → 6; severe miles the same, no rate of their own). A kilometre figure is read
  as the schedule figure it is (16,000 km as 10,000 mi). **Worked out when reminders are read, never stored,**
  and never over months the item has. **Wherever a reminder's interval is shown, say the months are
  Glovebox's** (`TrackedItem.monthsWorkedOut`, `TrackedItem.every`, `interval_months_ours`). No other job gets it
  (the owner's decision, 2026-10-28).
- **Use `ItemStatus.counting`, not `lastDone != null`, to decide whether a reminder has something to count
  from.** A job the vehicle hasn't reached counts down to its first mileage, by distance only; it is not "Not
  logged yet".
- Which reminders a logged line counts for is **derived, never stored** (`ServiceLinks`): the item it was saved
  against plus every item answering to its `catalogId` (a typical item through `templateKey`, the user's through
  `service`). **Never read `scheduleItemId` on its own:** go through `toEntry(swap, items.answering())`, or
  `HistoryImport.itemIds` for an unsaved record. `templateKey` means "Glovebox's own", which keeps imports off
  those items. `CatalogItem.covers` is one way (a change covers its check); `ServiceLinks.own`, without
  `covers`, decides what a Log service line stands for.
- Never-logged reminders are listed apart as "Not logged yet", above Overdue and Coming up (the user's choice,
  2026-10-13), and left out of overdue counts. They are never given a guessed next-due point; a distance job says
  in amber (never red) where the schedule last put it. **"Up to date" is said only when every tracked job has
  something to count from;** otherwise "N to log". **Every reminder row is one tap from Log service with that
  job ticked** (the user's request).
- A snooze only holds a due reminder back and writes no record. A stale snooze on a reminder that isn't due is
  ignored, so nothing clears it on logging. **Pass `lead` to every `VehicleReminders.statuses` call.**
- The rule preview uses only a real logged service, never sample data, and names no maker.
