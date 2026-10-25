# "Last done" is derived from the service log, and nothing is assumed

"Last done" for a reminder is never stored. It is worked out from the service records (`ServiceLog.lastDone`), so editing or deleting a record rolls the reminder back by itself. When the user tells the app when something was last done without logging a visit, that is saved as a service record flagged `enteredAsLastDone` (#78).

Nothing is assumed or estimated for the user: there is no "assume everything was done on schedule". An item nobody has logged stays "Not logged yet".

## Considered Options

From #78:
- **A starting point kept apart from the service log.** History stays truthful, but it is a second source of "last done" that every reminder calculation would have to learn about.
- **Assumed service records, made at setup.** Rejected: History would hold visits that never happened, and they would flow into exports and the PDF report.
- **A cached last-done column.** It would drift from the records it summarises.
