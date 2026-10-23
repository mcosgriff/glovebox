# Toyota 4Runner, 2009, 4.7L V8

How `app/src/main/assets/known-schedules/toyota-4runner-2009-v8.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2009 4Runner Scheduled Maintenance Guide, a guide for the 4Runner alone, at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-094Runner/pdf/09%204Runner%20SMG%20127072.pdf`. Toyota's owners site lists it (its downloads endpoint for the 4Runner and the year); no VIN
was sent. Read on 2026-10-23.

- The maintenance log (printed pages 06 to 21: 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles) was read in full off the pages, and the list written from it. The 2008 and 2006 lists were then compared with it.
- **One log, split by engine** (the owner's ruling of 2026-10-23, #33). The guide has one log for both
  engines, and every line that is for one engine says which, plainly: the spark plugs at 30,000, 60,000 and 90,000 miles say "1GR-FE engine only" on the line, and the timing belt at 90,000 miles has a footnote that says "2UZ-FE engine". Every other line names
  no engine and is on both lists. So this list is the log with the 4.7L V8's lines, and
  [the 4.0L V6's list](toyota-4runner-2009-v6.md) the same log with its own; this one leaves out
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
  propeller shaft lubrication (4WD) 15,000 / 18, cabin and engine air filters 30,000 / 36, spark plugs at 120,000 miles / 144 months only (the one plug change that names no engine) and the timing belt at 90,000 miles / 108 months ("replace every 90,000 miles or 108 months" in the guide's explanation of maintenance items), and engine coolant first at 100,000 miles and then every 50,000 / 60. The guide's "or 120 months" for the first change isn't used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
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

- 2008 has the same jobs at the same milestones, but its 100,000-mile coolant replacement carries a footnote mark with no footnote printed for it, on that page or anywhere in the guide, so the 2008 guide never says the coolant is replaced again every 50,000 miles. That is why 2008 is a list of its own ([2008, this engine](toyota-4runner-2008-v8.md)). The other differences are layout and wording: 2008 puts the 30,000-mile brake inspection (thickness and runout) among the work and 2009 among the inspections, 2008 gives the plugs' engine in a footnote and 2009 on the line, and 2008 says "chassis" where 2009 says "chassis and body".
- 2006 differs in its jobs (a cabin air filter cleaning at 5,000 miles, the cabin air filter and a limited-slip differential oil change only "if equipped", salted roads as a condition of its own); see [its page](toyota-4runner-2006-v8.md).

## Not covered

- Other years of this engine: [2006](toyota-4runner-2006-v8.md), [2008](toyota-4runner-2008-v8.md).
- 2007: the guide (`https://assets.sipb.toyota.com/publications/en/omms-s/07ToyMS_MS0016/pdf/07ToyMS_MS0016.pdf`) was read, and its towing list at 30,000 and 90,000 miles has "Replace
  manual transmission oil". A job only for a manual gearbox can't be said in a list yet (`manualExcluded` is the
  other way round), so no 2007 list was made. 2007 also adds an "if equipped" active height control inspection.
- 2005 and earlier: Toyota lists no guide for the 4Runner alone, so there is no 4Runner log to read. 2004 and
  2005 have one booklet for every SUV and truck, with lines marked by model; 2002 and 2003 one for every model,
  with two schedules side by side; 1998 to 2001 booklets for every model, or every SUV and truck (not read).
  1996 and 1997 have no maintenance guide listed, and 1991 to 1995 nothing at all.
- 2010 on: [#140](https://github.com/mcosgriff/glovebox/issues/140) and the
  [2017–2024 list](toyota-4runner-2017-2024.md).
