# Recalls

Rules for NHTSA checks, marking recalls done. Read before changing any of it; review holds a diff to every line here.

- `RecallRepository` re-checks a week-old list; a failed check leaves the stored list alone. "No open recalls"
  only once NHTSA has answered. NHTSA answers 400 with an empty list for no recalls, so the body is read, not the
  status. `Settings.recallCadence` governs the in-app checks too.
- `completedOn` (the user's mark) survives refreshes; a dropped campaign is deleted only while open. Archived
  (`archivedOn`) means "doesn't apply" and isn't open. A mark-done record resets no reminder.
