# Volkswagen GTI, 2007 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-gti-2007.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md); what the four Mk5 years share is on the
[2009 page](volkswagen-gti-2009.md#the-mk5-lists).

Source: **Volkswagen's reissue** of its 2007 "VW Service" schedule, `USA5R503921`, 20 pages, printed
**07.2022**, from Volkswagen's owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/document/875e5377-46c8-4d46-8f16-0d1365769220`).
Read by eye on 2026-10-26. **No card from the time was found** (#145's survey); the owner ruled on #145 that
the reissue may be the only source, cited as one with its print date.

- **The document covers every Volkswagen model** in the USA and Canada; only the USA part is read. A line marked
  for another model or engine is left out; the GTI's engine is the 2.0L TFSI, which this year's lines tell apart
  from the 2.0L (eight-valve) engine. Lines with no mark are kept.
- **The table:** "Service at" 10,000 to 120,000, every 10,000, and "Service every 5,000 miles". **The every-5,000
  oil change is the GTI's** (the owner's ruling on #145; the line names no engine). So the table has a stop every
  5,000 to 120,000, each carrying the every-5,000 jobs (oil, the brake check, the service display).
- **Reminders:** engine oil 5,000; tire rotation 10,000; dust and pollen filter 40,000 / 24 months; DSG fluid
  and filter 40,000 (not for a manual car); air filter 40,000 / 72 months; spark plugs 60,000 / 72 months;
  timing belt at 120,000 (once, at the table's end; "2.0L, 2.0L TFSI only").
- **From the time-dependent list**, as reminders apart from the table (`additional`): brake fluid every 2 years
  and the tire mobility kit's sealant bottle every 4 years, by time alone. The 2-year pollen filter and the
  6-year air filter and spark plug items give the reminders above their months.
  - Air filter: the grid has it at 40,000, 80,000 and 120,000; the 6-year item says "for vehicles driven less
    than 60,000 miles in 6 years". The reminder is the grid's 40,000 with the item's 6 years.
  - Spark plugs: an unmarked 4-year item ("less than 40,000 miles in 4 years") and a "2.0L TFSI, 3.6L FSI only"
    6-year item. The one naming the GTI's engine is taken.
  - Left out: the yearly airbag check and the 2-year sunroof drain check (inspections, which make no reminder
    and are never placed on the grid); the convertible top; the New Beetle's 4-year air filter; the tire
    pressure sensors' 6-year replacement, "(where applicable)" with nothing named (**needs a decision**, see the
    2009 page).
- **Rows cut off on the page.** Some rows end mid-bracket in the reissue itself (checked on the page image):
  - "Timing Belt: Check condition (2.0L TFSI" at 60,000 and 80,000 names the GTI's engine, so it is kept;
  - "Dust and Pollen Filter: Replace (2.0L" at 60,000, "Power Steering: Check fluid level (1.9L TDI," at 20,000
    and 60,000, and "V-Belt/Ribbed belt: … (1.9L TDI" at 20,000, 60,000 and 100,000 don't, so they are left out.
    The same rows elsewhere name the 2.0L (20,000 and 100,000 pollen filter) or the 1.9L TDI alone (100,000
    power steering), not the 2.0L TFSI.
- **Gearbox:** the DSG fluid change, the automatic shift lock check and the automatic's leak, ATF and final drive
  checks are `manualExcluded`; the manual gearbox's oil check is `manualOnly`. Whether Volkswagen means its
  "Automatic" lines for the DSG isn't said, so a DSG car keeps them.
- Left out as not the GTI's: TDI jobs (water separator, fuel and particle filters, second battery,
  turbocharger rods), the New Beetle's spoiler, the 2.0L (eight-valve) engine's pollen filter, timing belt checks
  and spark plugs, the TDI's and 4.2L's timing belts, the TDI's ATF check, the Touareg's air suspension, the
  convertible top, the Eos's roof and the 4MOTION (BorgWarner) clutch.
- **Not covered:** the other Mk5 years have lists of their own, since no two years agree:
  [2006](volkswagen-gti-2006.md), [2008](volkswagen-gti-2008.md), [2009](volkswagen-gti-2009.md).
