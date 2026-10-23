# Toyota 4Runner, 2008, 4.7L V8

How `app/src/main/assets/known-schedules/toyota-4runner-2008-v8.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2008 4Runner Scheduled Maintenance Guide, a guide for the 4Runner alone, at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-084runner/pdf/T-MMS-084runner.pdf`. Toyota's owners site lists it (its downloads endpoint for the 4Runner and the year); no VIN
was sent. Read on 2026-10-23.

- The maintenance log (printed pages 06 to 21, the same layout as 2009's: 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles) was compared page by page with the 2009 log, which was read in full; each kind of difference a script's count of each job per page reported was looked at on the page (the 5,000, 30,000, 60,000, 100,000 and 105,000-mile pages, as images).
- **One log, split by engine** (the owner's ruling of 2026-10-23, #33). The guide has one log for both
  engines, and every line that is for one engine says which, plainly: the spark plugs at 30,000, 60,000 and 90,000 miles have a footnote that says "1GR-FE engine", and the timing belt at 90,000 miles one that says "2UZ-FE engine". Every other line names
  no engine and is on both lists. So this list is the log with the 4.7L V8's lines, and
  [the 4.0L V6's list](toyota-4runner-2008-v6.md) the same log with its own; this one leaves out
  the V6's spark plugs every 30,000 miles. The guide names the engines by code: the 2UZ-FE is the 4.7L V8, the 1GR-FE the 4.0L V6, the two
  engines the 4Runner had in these years. The list is matched by the engine size on record, and setup asks
  which engine when none is.
- Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the list and the jobs marked "(4WD)"
  are left out for a two-wheel-drive one. The guide marks the propeller shaft lubrication "(4WD)" but not the
  re-torque of its bolt, and both are kept as printed. It has no "AWD" lines.
- Left out: the charcoal canister inspection at 60,000 and 120,000 miles, which is only for vehicles registered
  in California, Massachusetts and New York (Glovebox has no way to say a job is for some states); the daily
  checks for off-road driving; the notes on the cabin air filter and the oil reminder light; the explanations
  of each item.
- Reminders: engine oil 5,000 mi / 6 mo (the guide's interval for all driving), tire rotation 5,000 / 6,
  propeller shaft lubrication (4WD) 15,000 / 18, cabin and engine air filters 30,000 / 36, spark plugs at 120,000 miles / 144 months only (the one plug change that names no engine) and the timing belt at 90,000 miles / 108 months ("replace every 90,000 miles or 108 months" in the guide's explanation of maintenance items), and engine coolant has no reminder from this list (see "What differed").
- Inspections that are reminders too, as on the 2017–2024 list (asked for by the owner, 2026-10-13): front and
  rear differential oil every 15,000 / 18, transfer case oil every 30,000 / 36. Logging a change of one counts
  as its check. The other inspections are in the table only, among them the drive belts (first at 60,000
  miles, then every 15,000) and the valve clearance at 60,000 and 120,000.
- Only for severe use (#136), all when towing: front and rear differential oil every 15,000 / 18, transfer
  case oil every 30,000 / 36, automatic transmission fluid every 60,000 / 72. Under ordinary driving the
  guide only inspects these.
- Shorter under severe use: propeller shaft lubrication 5,000 / 6 (dusty or salted roads, or towing).
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.

## What differed

- The jobs and where they fall are the same as in 2009. **What differs is the coolant:** the 2008 guide's "Replace engine coolant" at 100,000 miles carries footnote mark 5, and no footnote 5 is printed on that page or anywhere in the guide; the explanation of maintenance items gives no interval either. The 2009 and 2006 guides print "initial replacement at 100,000 miles/120 months, every 50,000 miles/60 months thereafter" there. Nothing is carried over from another year, so in this list the coolant is in the table at 100,000 miles and has no reminder.
- Layout and wording only: the 30,000, 60,000, 90,000 and 120,000-mile brake inspection (thickness and runout) is among the work here and among the inspections in 2009; the plugs' engine is in a footnote here and on the line in 2009; the charcoal canister's footnote here names the 2UZ-FE engine as well as the states, and 2009's only the states (left out either way, below); "chassis" where 2009 says "chassis and body"; the cabin air filter note sits on other pages.

## Not covered

- Other years of this engine: [2006](toyota-4runner-2006-v8.md), [2009](toyota-4runner-2009-v8.md).
- 2007: lists of its own, [V6](toyota-4runner-2007-v6.md) and [V8](toyota-4runner-2007-v8.md), with the manual
  transmission oil under towing in `manualOnly`.
- 2005 and earlier: Toyota lists no guide for the 4Runner alone, so there is no 4Runner log to read. 2004 and
  2005 have one booklet for every SUV and truck, with lines marked by model; 2002 and 2003 one for every model,
  with two schedules side by side; 1998 to 2001 booklets for every model, or every SUV and truck (not read).
  1996 and 1997 have no maintenance guide listed, and 1991 to 1995 nothing at all.
- 2010 on: [#140](https://github.com/mcosgriff/glovebox/issues/140) and the
  [2017–2024 list](toyota-4runner-2017-2024.md).
