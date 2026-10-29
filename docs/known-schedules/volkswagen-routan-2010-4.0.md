# Volkswagen Routan 4.0L, 2010

How `app/src/main/assets/known-schedules/volkswagen-routan-2010-4.0.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Routan card read, and what isn't covered, is on the
[2013–2014 page](volkswagen-routan-2013-2014.md).

| Model year | What was read | Document |
|---|---|---|
| 2010 | "2010 USA Volkswagen Maintenance Schedule by Miles - Routan Only" (revised November 2013), in full, as page images; the list was written from it. `MC-10242441` and `MC-10212706` are the same card | `2013/MC-10156035-9999` |

The card and how it is read are on the [3.8L page](volkswagen-routan-2010-3.8.md). This list is that one's with
the one engine-marked row swapped: **"Replace the timing belt (4.0L engine)" at 102,000** (`timing-belt`,
reminder every 102,000) in place of the 3.8L's ignition cables. It has
`"engine": { "label": "4.0L", "displacementL": 4.0 }`.
