# Toyota 4Runner, 2007, 4.0L V6

How `app/src/main/assets/known-schedules/toyota-4runner-2007-v6.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's 2007 4Runner Scheduled Maintenance Guide, a guide for the 4Runner alone (24 pages, the
maintenance log only), at `https://assets.sipb.toyota.com/publications/en/omms-s/07ToyMS_MS0016/pdf/07ToyMS_MS0016.pdf`. Toyota's owners site lists it (its downloads endpoint for the 4Runner and the
year); no VIN was sent. Read on 2026-10-23.

- The maintenance log (one page per milestone: 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles)
  was read off the pages, as images, for 5,000, 15,000, 30,000, 60,000, 90,000, 100,000 and 120,000 miles, one
  of each kind, and the rest checked against them by text; a script's count of the jobs on each page matched the
  list's. It has the same layout as the 2006 guide and was compared with it line by line (see "What differed").
- **One log, split by engine** (the owner's ruling of 2026-10-23, #33). The guide has one log for both
  engines, and every line that is for one engine says which, plainly: the spark plugs at 30,000, 60,000 and
  90,000 miles have a footnote that says "Applies to 4Runner with 1GR-FE", and the timing belt at 90,000 miles
  one that says "Applies to 4Runner with 2UZ-FE". Every other line names no engine and is on both lists. So this
  list is the log with the 4.0L V6's lines, and [the 4.7L V8's list](toyota-4runner-2007-v8.md) the same
  log with its own; this one leaves out the V8's spark plugs at 120,000 miles only and its timing belt at 90,000 miles. The guide names the engines by code: the 1GR-FE is the
  4.0L V6, the 2UZ-FE the 4.7L V8, the two engines the 4Runner had in these years. The list is matched by the
  engine size on record, and setup asks which engine when none is.
- Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the list and the jobs marked "(4WD)"
  are left out for a two-wheel-drive one. The guide marks the propeller shaft lubrication "(4WD)" but not the
  re-torque of its bolt, and both are kept as printed. It has no "AWD" lines.
- **Gearbox.** The towing list at 30,000 and 90,000 miles has "Replace manual transmission oil", and at 60,000
  and 120,000 miles "Replace transmission fluid or oil" (whichever the gearbox has). The manual oil change is in
  `manualOnly`, so a vehicle known to shift itself doesn't get it; the 60,000-mile line is for both. The manual
  oil change has no service line and no reminder: with the transmission fluid line it would be one job in the
  table at 30,000, 60,000, 90,000 and 120,000 miles, which isn't the 60,000-mile interval of the line for both,
  so one of the two reminders would have to go; the one kept is the guide's line for every gearbox. A manual's
  oil change at 30,000 and 90,000 miles when towing is in the table only.
- "If equipped" lines, as with the later guides' "(if applicable)" rows, are in the table with no service line
  and no reminder: the cabin air filter replacement at 30,000, 60,000, 90,000 and 120,000 miles, and the active
  height control gas chamber inspection (first at 30,000 miles, then every 15,000).
- Left out: the charcoal canister inspection at 60,000 and 120,000 miles, which the guide gives for the 2UZ-FE
  only and only for vehicles registered in California, Massachusetts and New York (Glovebox has no way to say a
  job is for some states); the notes on the cabin air filter and the oil reminder light. The guide's footnotes
  point to an "Explanation of Maintenance Items" section that this PDF doesn't have.
- From 90,000 miles the guide says "radiator, condenser and/or intercooler" where it said "radiator core and
  condenser"; Glovebox names the inspection the same throughout.
- Reminders: engine oil 5,000 mi / 6 mo (the guide's interval for all driving), tire rotation 5,000 / 6,
  propeller shaft lubrication (4WD) 15,000 / 18, engine air filter 30,000 / 36 (the cabin air filter is "if
  equipped", so it has none), spark plugs every 30,000 miles / 36 months (the 120,000-mile plug change names no engine, so it is on both lists, and here it is the fourth of the V6's), and engine coolant first at 100,000 miles and then every 50,000 / 60 (the
  100,000-mile page's footnote). The footnote's "or 120 months" for the first change isn't used: it counts from
  the day the vehicle went into service, which Glovebox doesn't know.
- Inspections that are reminders too, as on the 2017–2024 list (asked for by the owner, 2026-10-13): front and
  rear differential oil every 15,000 / 18, transfer case oil every 30,000 / 36. Logging a change of one counts
  as its check. The other inspections are in the table only, among them the drive belts (first at 60,000
  miles, then every 15,000), the valve clearance at 60,000 and 120,000, and the transmission fluid or oil.
- Only for severe use (#136), all when towing: front and rear differential oil (the rear including a
  limited-slip differential) every 15,000 / 18, transfer case oil every 30,000 / 36, transmission fluid or oil
  every 60,000 / 72. Under ordinary driving the guide only inspects these.
- Shorter under severe use: propeller shaft lubrication 5,000 / 6 (dusty or salted roads, or towing; the guide
  gives "dirt roads or dusty roads", towing and salted roads as three headings).
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.

## What differed

The 2007 guide was read for its own lists; nothing was carried over from another year. Against the 2006 guide,
which has the same layout:

- 2007 has no cabin air filter cleaning at 5,000 miles and no limited-slip differential oil change at 30,000
  miles.
- 2007 adds the active height control gas chamber inspection ("if equipped") and the manual transmission oil
  change when towing (above).
- 2007 says "transmission fluid or oil" where 2006 says "automatic transmission fluid", in the inspection and in
  the towing change at 60,000 and 120,000 miles.
- 2007's conditions are "dirt roads or dusty roads", towing and salted roads, as in 2008 and 2009, where 2006
  has "desert or on dirt roads"; the propeller shaft jobs come under all three.
- Against 2008 and 2009 (a different layout), 2007 also differs in the cabin air filter ("if equipped" here),
  the limited-slip wording of the rear differential change, and the lines above.

## Not covered

- Other years of this engine: [2006](toyota-4runner-2006-v6.md), [2008](toyota-4runner-2008-v6.md),
  [2009](toyota-4runner-2009-v6.md).
- 2005 and earlier: Toyota lists no guide for the 4Runner alone, so there is no 4Runner log to read. 2004 and
  2005 have one booklet for every SUV and truck, with lines marked by model; 2002 and 2003 one for every model,
  with two schedules side by side; 1998 to 2001 booklets for every model, or every SUV and truck (not read).
  1996 and 1997 have no maintenance guide listed, and 1991 to 1995 nothing at all.
- 2010 on: [#140](https://github.com/mcosgriff/glovebox/issues/140) and the
  [2017–2024 list](toyota-4runner-2017-2024.md).
