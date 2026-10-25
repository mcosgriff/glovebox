# Recalls

Rules for NHTSA checks, marking recalls done. Read before changing any of it; review holds a diff to every line here.

- `RecallRepository` re-checks a week-old list; a failed check leaves the stored list alone. "No open recalls"
  only once NHTSA has answered. NHTSA answers 400 with an empty list for no recalls, so the body is read, not the
  status. `Settings.recallCadence` governs the in-app checks too.
- `completedOn` (the user's mark) survives refreshes; a dropped campaign is deleted only while open. Archived
  (`archivedOn`) means "doesn't apply" and isn't open. A mark-done record resets no reminder.
- NHTSA matches the model name exactly and files some recalls under names vPIC doesn't give ("F-250 SD" for an
  "F-250", #460). The owner ticks the other names that are theirs (`recall_names`, from `NhtsaClient.recallModels`,
  which sends only year and make; `RecallNames.offered` lists those beginning with the model). **None is ever
  ticked for them.** A check asks under the model and each ticked name, and **counts as failed unless every name
  answers**, since a list missing one name's recalls would withdraw them.
- NHTSA's undocumented `parkIt`, `parkOutSide` and `overTheAirUpdate` (#461) are kept on each recall and follow
  NHTSA when it changes them. **Only a plain `true` counts; missing or odd is false, and nothing ever says a
  vehicle is safe to drive.** "Do not drive" and "Park outside" show, in red, only on an open recall (the card,
  the vehicle's recall banner) and lead a new recall's notification. `Notes` isn't kept: the ones seen were
  boilerplate.
