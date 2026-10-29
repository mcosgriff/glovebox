# Volkswagen Routan 3.8L, 2010

How `app/src/main/assets/known-schedules/volkswagen-routan-2010-3.8.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Routan card read, and what isn't covered, is on the
[2013–2014 page](volkswagen-routan-2013-2014.md).

| Model year | What was read | Document |
|---|---|---|
| 2010 | "2010 USA Volkswagen Maintenance Schedule by Miles - Routan Only" (revised November 2013), in full, as page images; the list was written from it. `MC-10242441` and `MC-10212706` are the same card | `2013/MC-10156035-9999` |

The card is a grid of 20 columns, 6,000 to 120,000, with no oil change indicator block.

**One list per engine.** Two rows name an engine: "Replace the ignition cables (3.8L engine)" and "Replace the
timing belt (4.0L engine)", both at 102,000. Every other row names none and is in both lists. This list has
`"engine": { "label": "3.8L", "displacementL": 3.8 }` and the ignition cables (as `plug-wires`); the
[4.0L list](volkswagen-routan-2010-4.0.md) has the timing belt instead. A Routan whose engine isn't on record
gets neither until setup asks which engine it has.

- Oil and filter, and tire rotation, in every column: reminders every 6,000. **The card prints no oil change
  indicator wording and no months**, so the grid's 6,000 is the card's own interval and the reminders are
  shipped; the 2013–2014 card puts both under the indicator, and Volkswagen's bulletin `MC-10124405` (2014)
  speaks of the Routan's "change oil" indicator for 2009–2014 ([the open question on #452](https://github.com/mcosgriff/glovebox/issues/452#issuecomment-6492943764)). No months (#517:
  none recorded).
- Engine air filter at 30,000, 60,000, 90,000 and 120,000: reminder every 30,000.
- "If using your vehicle for dusty or off-road conditions: inspect the engine air cleaner filter" every 12,000:
  table-only, named with its condition (an inspection).
- "Replace the air conditioning filter (if applicable)" every 12,000: table-only with no `service`, as every
  "(if applicable)" row is (#33).
- Brake linings (`brake-inspect`), and front suspension, tie rod ends and boot seals, every 12,000; CV joints
  (footnote 1, "first inspection at 12,000 miles or 12 months") and the exhaust system at 12,000, 24,000,
  48,000, 72,000, 96,000 and 120,000. All table-only. Footnote 2 gives the exhaust system the same first
  inspection; the grid doesn't mark it.
- Automatic transmission fluid and filter at 120,000: reminder every 120,000; under Severe every 60,000, from
  the "police, taxi, fleet or frequent trailer towing" row drawn at 60,000 (that row stays in the table with its
  condition and no `service`, as on the [2013–2014 list](volkswagen-routan-2013-2014.md)).
- PCV valve inspected at 90,000 (table-only). Spark plugs and ignition cables at 102,000: reminders every
  102,000. Accessory drive belts at 120,000: reminder every 120,000.
- **Engine coolant is table-only**, at 60,000 and 102,000 as drawn, each cell marked "4": "flush and replace
  the engine coolant if not completed at 60 months". Footnote 3, "at 60 months if not completed at 102,000
  miles", is marked on no cell. 60,000 and 102,000 are no one interval, so no reminder is the table's own ([question 4 on
  #452](https://github.com/mcosgriff/glovebox/issues/452#issuecomment-6492943764) asks whether the 60,000 cell stands for the 60 months).
- Left out: the "Carefree Maintenance Program Coverage" shading and the "**" / "***" notes on coverage and the
  emissions warranty.
