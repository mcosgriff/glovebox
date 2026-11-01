# Shop recommendations are kept apart from the schedule, and closed only when the owner says so

Reading an invoice (#484) put the shop's recommended work in the record's notes as one block of text, which the owner couldn't act on or come back to. #572 keeps each item the owner chooses on the vehicle until the work is logged.

## The rule

- **A table of its own, `shop_recommendations`** (database version 40). A row is one kept item: the shop's wording, its price, the inspection item it was found under, what was found, the mark's colour (red or yellow, read by hue) and the catalog line it comes off with.
- **Its shop and date are the record it was kept from** (`recordId`, `ON DELETE CASCADE`), never stored again: an edit to that record moves them, and deleting it (Undo of a new record included) takes what it kept. `vehicleId` cascades too, so Delete vehicle and Delete all data need nothing more. Backup and restore carry it as every table.
- **Closed three ways, none of them stored as "done":** by the owner (`closedOn`), by a later record of the work (`doneByRecordId`, `ON DELETE SET NULL`), or by a newer recommendation of the same work from the same shop (`replacedById`, `ON DELETE SET NULL`). Deleting the record that closed or replaced one opens it again by the foreign keys; Undo of that delete puts the links back (`RecordRecommendations`).
- **Never a reminder.** An open item makes nothing overdue and moves nothing; closing one logs nothing. "Last done" stays derived from records alone ([ADR-0004](0004-last-done-is-derived.md)).
- **Same shop, same item replaces:** the shop as the record names it (a saved shop's spelling, #484) and the wording, case and spacing aside. Different shops never replace each other; work done by the owner has no shop and replaces nothing.
- **Matching the work is by catalog line, whoever did it;** an item kept with no line matches on wording (`LineGuess.similar`, the same typo-tolerant comparison guesses use). Only records dated on or after the recommendation. **The form asks** ("done with these?", ticked) and closes only on save; nothing closes on its own.

## Considered Options

- **Keep them in the notes.** What #484 did. Turned down by the owner: nothing to act on, and no way to tell later what is still open.
- **Make them reminders (schedule items).** Turned down: a recommendation is the shop's opinion, often for work already on the schedule, and a reminder would make it overdue and move with the owner's rules.
- **Store the shop and date on the row.** Turned down: they are the record's, and two copies drift when the record is edited.
- **Close matching items on save without asking.** Turned down: a guess at what the work was is only ever offered (#312), and closing the wrong item would hide work still to do.
- **Match on wording alone.** Turned down: the same work is worded differently by every shop and by the owner ("Coolant System Service", "Antifreeze exchange", "Coolant flushed"); the catalog line is what they share.

## Consequences

- A kept item's line is the owner's rule for its wording, otherwise a guess the form labels as one and lets the owner change; a guessed line is stored as the line once kept.
- Records only close items when logged new on Log service; an edit, an import or a recall's record never does.
- The export and the PDF report don't list open recommendations yet (open question on #572).
