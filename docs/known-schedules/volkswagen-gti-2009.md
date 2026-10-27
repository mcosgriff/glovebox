# Volkswagen GTI, 2009 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-gti-2009.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissue** of its 2009 "VW Service" schedule (the file is named `USA5R503721`; the pages
print no code), 22 pages, printed **06.2024**, from Volkswagen's owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/document/903cda69-6bfe-4c4b-bb47-0444c41d5ce2`).
Read by eye on 2026-10-26. **No card from the time was found** (#145's survey); the owner ruled on #145 that
the reissue may be the only source, cited as one with its print date. Volkswagen's model-year 2009 sheet of
transmission and spark plug intervals, filed with NHTSA (`2012/MC-10071285-7690`), gives the GTI 2.0T DSG fluid
at 40,000 and spark plugs at 60,000, as the reissue does.

- **The document covers every Volkswagen model** except the Routan, in the USA and Canada; only the USA part is
  read. A line marked for another model or engine is left out; lines with no mark are kept.
- **The table:** 10,000 to 130,000, every 10,000. Its "every 5,000 miles" section holds only the 3.0L TDI's
  AdBlue, so the table has no 5,000-mile stops.
- **"Or 1 Year after Last Service"** heads every milestone. That is a time limit counted from the last visit,
  not months from new, so the table has no months and **no reminder is given months for it** (the owner's ruling
  on #145). Check the booklet: by this schedule a car driven little is still serviced once a year.
- **Reminders:** engine oil and tire rotation 10,000; DSG fluid and filter 40,000 (not for a manual car); spark
  plugs 60,000 / 72 months. The spark plug rows are "1.8L, 2.0L, 3.6L and 4.2L only" at 60,000 (the GTI's 2.0T
  is the year's only 2.0L petrol engine; the NHTSA sheet above agrees) and "all gasoline engines" at 120,000; the
  months are the time list's 6 years.
- **From the time-dependent list**, as reminders apart from the table (`additional`), by time alone:
  - brake fluid at 3 years, then every 2 (the row names the GTI), as a 24-month reminder with the rest in its
    name, as on the Mk6;
  - the air filter every 6 years (unmarked). In the grid it is only at 60,000, "(if applicable)", so that row is
    in the table with no service and the time item is the reminder;
  - the tire mobility kit's sealant bottle every 4 years.
  - Left out: the yearly airbag check and the 2-year sealant bottle check (inspections, which make no reminder
    and are never placed on the grid); the convertible latch; the New Beetle's brake fluid; and two "(if
    applicable)" / "(where applicable)" items that name nothing: **the dust and pollen filter every 2 years** and
    the tire pressure sensors every 6 years (**needs a decision**, below).
- **Timing belt:** "Timing Belt: Replace (if applicable)" at 110,000 is in the table with no service and no
  reminder (the owner's ruling on #145). No document read says whether every 2009 GTI has a belt-driven engine.
  The 120,000 belt row is the 2.0L TDI's.
- "(if applicable)" inspection rows (headlight and rear washer nozzles, second battery, sunroof and its drains)
  are in the table as printed.
- Left out as not the GTI's: TDI jobs (AdBlue, fuel filters, particle filter, the TDI's timing belt), the
  convertible roof and retractable hard top, the 4MOTION (BorgWarner) clutch, the 2.5L / New Beetle / Rabbit /
  Golf automatic's fluid change, the 1.9L / 2.5L / 3.2L spark plugs, and the dealer's service sticker.
- **Not covered:** the other Mk5 years have lists of their own: [2006](volkswagen-gti-2006.md),
  [2007](volkswagen-gti-2007.md), [2008](volkswagen-gti-2008.md).

## The Mk5 lists

What the 2006–2009 lists share, and what is waiting on the owner.

- **One list per year.** The four reissues disagree on the GTI's own jobs (#145's survey: the air filter, the
  dust and pollen filter, the timing belt and brake fluid all differ), so no two years share a list.
- **Reissues only.** Volkswagen serves later reissues of each year's all-model schedule; no card from the time
  was found for any year. NHTSA's manufacturer communication files for 2005–2009 and 2010–2014 were searched
  for Volkswagen rows of model years 2006–2009 mentioning maintenance, a schedule, an interval or spark plugs:
  none is a schedule card (only the 2009 sheet above, and a sunroof drain campaign for other models). A card
  from the time, if one turns up, wins.
- **Model name.** vPIC's model list (`GetModelsForMakeYear`) names the car `GTI` in 2006, 2007, 2008 and 2009,
  so the lists have no alias.
- **One engine.** The GTI had the 2.0L turbocharged FSI engine in every year (written "2.0L TFSI", "2.0T FSI"
  and "2.0L" as the years go), so the lists name none. A line is the GTI's only where its mark names that engine,
  the GTI, every model, or nothing.
- **Gearbox.** As on the Mk6: the DSG fluid change is `manualExcluded`; the automatic's checks are too, and the
  manual gearbox's oil check is `manualOnly`.
- **Time-dependent items** are reminders apart from the table (`additional`) with the card's own figures, never
  placed on the grid (the owner's ruling on #145). Where the same job is also in the grid (the air filter, spark
  plugs, the pollen filter), the item's years are the grid reminder's months, as on the Mk6, since a job can't be
  both. Inspections in that list are left out.
- **Jobs named in Glovebox's words**; the dealer's service sticker is left out, as on the Mk6.

### Needs a decision

- **"(if applicable)" / "(where applicable)" on a job that names no engine or model.** #33's rule puts such a row
  in the table with no service and no reminder, and `additional` holds only reminders, so these are not
  reminders now:
  - the dust and pollen filter in 2006 (every 20,000 in the grid, "where applicable"; every 2 years in the time
    list, "if applicable") and 2009 (every 2 years in the time list only, "if applicable");
  - the tire pressure sensors' replacement every 6 years, "where applicable", in all four years;
  - the 2009 air filter row at 60,000 (its 6-year time item, unmarked, is a reminder).

  Lines that name what makes them apply (the DSG change, the manual gearbox check) are taken by the gearbox
  fields instead.
