# Milestones

Rules for the milestones view and the user's own table. Read before changing any of it; review holds a diff to every line here.

- **Milestones are a view, never a second source of what is due:** nothing on that screen creates, moves or
  silences a reminder, and nothing about them is stored; "completed" is derived from the log, with no tick-box.
- The user's own table (`milestones`, `milestone_jobs`) shows **in place of** the generated view. A job is done by
  its `service` or, failing that, its name. Nothing about reminders, notifications or exports reads these tables.
- A table repeats one way or not at all (`vehicles.milestonesRepeatEveryMiles`, set by an import or the editor's
  repeat row, cleared with the table by `forgetTable`); a file with a different repeat is refused whole. A repeat
  has no `orMonths`.
- **A table from a maker's list that doesn't repeat is continued past its last column** (#568,
  `MilestoneContinuation`, `docs/known-schedules.md` rule 9): each job the table marks at least twice, evenly
  and on to its end, keeps that spacing; a job the guide starts past the end (`firstAtMiles`) comes in from its
  first time; any other job isn't continued and is named as such. The continued milestones are
  `Milestone.continued`, carry no `orMonths`, and reach the vehicle's mileage plus the table's length. One note
  per screen, between the maker's last milestone and the first continued one, says they are Glovebox's and to
  check the service manual; each continued card says "continued by Glovebox". It is still only a view: nothing
  of it is stored and no reminder moves. A repeat, where the table has one, wins; a table the user typed (no
  maker's list) ends where it ends.
- **A table typed in the app keeps exactly the rules of one read from a file, from the same code**
  (`MilestoneImport`'s limits and checks, called by `MilestoneEdits`). A table starts **empty**, never a copy of
  the generated view; a job's service is **never suggested from its name**; deleting is a confirmation with no
  Undo; Close and Back ask before discarding. Read jobs through `MilestoneWithJobs.ordered`. Where a table came
  from is not stored.
