# Volkswagen GTI, 2006 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-gti-2006.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md); what the four Mk5 years share is on the
[2009 page](volkswagen-gti-2009.md#the-mk5-lists).

Source: **Volkswagen's reissue** of its 2006 "VW Service" schedule, 22 pages, printed **07.2022**, from
Volkswagen's owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/document/12fc5fd7-9a91-4f05-adaa-bf311ecb9267`).
Read by eye on 2026-10-26. **No card from the time was found** (#145's survey); the owner ruled on #145
that the reissue may be the only source, cited as one with its print date. The file is named `USA5R504021`,
but page 1 prints `USA5R503921`, the 2007 reissue's code; its content is not 2007's (below), so it is taken as
2006's with a wrong label.

- **The document covers every Volkswagen model** in the USA and Canada; only the USA part is read. A line marked
  for another model or engine is left out; the GTI's engine is the 2.0L TFSI. Lines with no mark are kept.
- **The table:** "Service at" 10,000 to 100,000, every 10,000, and "Service every 5,000 miles". **The every-5,000
  oil change is the GTI's** (the owner's ruling on #145): the line names no engine and sits next to a
  "1.8L Turbo only" oil line. So the table has a stop every 5,000 to 100,000, each carrying the every-5,000 jobs
  (oil, the service display), and the oil reminder is every 5,000. The headings "Service every 15,000 … 105,000"
  hold only the 1.8T's oil change and are left out.
- **Reminders:** engine oil 5,000; tire rotation 10,000; DSG fluid and filter 40,000 (not for a manual car);
  air filter 40,000 / 72 months; spark plugs 60,000 / 72 months (once, at 60,000, in a table to 100,000).
- **From the time-dependent list**, as reminders apart from the table (`additional`): brake fluid every 2 years
  and the tire mobility kit's sealant bottle every 4 years, by time alone. The 6-year air filter and spark plug
  items give the two reminders above their months.
  - Air filter: the grid has it at 40,000 and 80,000; the 6-year item says "for vehicles driven less than
    60,000 miles in 6 years". The reminder is the grid's 40,000 with the item's 6 years.
  - Spark plugs: an unmarked 4-year item ("less than 40,000 miles in 4 years") and a "2.0L TFSI only" 6-year
    item. The one naming the GTI's engine is taken.
  - Left out: the yearly airbag check and the 2-year sunroof drain check (inspections, which make no reminder
    and are never placed on the grid); the convertible top; the New Beetle's 4-year air filter.
- **"(where applicable)" with no engine or model named** (#33's rule: in the table, no service, no reminder):
  - the dust and pollen filter, every 20,000, is in the table without a reminder, and its 2-year item
    ("if applicable") is left out. **Needs a decision** (see the 2009 page).
  - the tire pressure sensors' 6-year replacement is left out for the same reason; their 10,000-mile check is in
    the table.
- **Gearbox:** the DSG fluid change and check, the automatic shift lock check and the automatic's leak, ATF and
  final drive checks are `manualExcluded`; the manual gearbox's oil check is `manualOnly`. Whether Volkswagen
  means its "Automatic" lines for the DSG isn't said, so a DSG car keeps them.
- **What the page shows that the table keeps as printed:**
  - at 80,000 the air suspension line and the automatic shift lock line are printed run into one row; the shift
    lock check is kept there, the air suspension (other engines) is not;
  - the windshield washer check is missing at 50,000 and 60,000.
- Left out as not the GTI's: TDI jobs (water separator, fuel and particle filters), the 1.8T's oil line, the
  New Beetle's spoiler, the battery and lamp checks and power steering lines for other engines (none names the
  2.0L TFSI), the air suspension, the 4MOTION (BorgWarner) clutch, the Phaeton's belt rollers, other engines'
  spark plugs, timing belts and automatic fluid.
- **Not covered:** the other Mk5 years have lists of their own, since no two years agree:
  [2007](volkswagen-gti-2007.md), [2008](volkswagen-gti-2008.md), [2009](volkswagen-gti-2009.md).
