# Toyota Corolla 1.8L, 2017–2019

How `app/src/main/assets/known-schedules/toyota-corolla-2017-2019.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the Corolla sedan (E170) for each model year, which
Toyota publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>Corolla/pdf/T-MMS-<yy>Corolla.pdf`
(`17` to `19`). Each was listed by Toyota's owners site for its year. Read on 2026-10-26.

- The 2018 guide's maintenance log (PDF pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full, by eye, and the list written from it. No line says the log repeats.
- 2017 and 2019 were compared with it by a script that counts every word on each log page against 2018's
  page; each difference it found was looked at on the page:
  - 2017 has no footnote on the fluid-level check (the one about sealed transmissions and differentials,
    brake fluid and coolant) on its full-page milestones. The jobs are the same.
  - 2019 is reset in a new typeface, with the 120,000 page's ticked jobs in two columns, and its 25,000
    towing line says "engine oil and filter" where 2018 says "engine oil and oil filter". The jobs are the
    same.
  - The 120,000 page was also looked at in 2017 and 2019.
- **Same jobs as the 2020–2022 1.8L.** Every line, mileage and month here is as on
  [toyota-corolla-2020-2022-1.8](toyota-corolla-2020-2022-1.8.md), apart from the vacuum pump's name below,
  so the items carry the same names and service lines.
- **The engine.** The E170 sedan had two 1.8L engines, the 2ZR-FE and the Valvematic 2ZR-FAE (the warranty
  pages name the 2ZR-FAE). One line names an engine: replace the brake booster's vane-type vacuum pump at
  120,000, "VALVEMATIC engine only". Glovebox matches engines by size and can't tell these two apart, so
  (the owner's ruling of 2026-10-25, #192) the line is kept for every 1.8L, in the table only, with no
  service line and no reminder, named "Replace brake booster vacuum pump (Valvematic engine)". It is
  treated as an "(if applicable)" row: an owner whose car isn't the Valvematic one ignores it.
- **An engine block of its own (1.8L).** The 2019 Corolla hatchback (E210) has the 2.0L and its own list,
  [toyota-corolla-2019-2022-2.0](toyota-corolla-2019-2022-2.0.md); NHTSA gives it as "Corolla" too. With the
  engine on record each gets its own list; with none, setup asks which engine. There was no Corolla Hybrid
  in these years, so the block has no `powertrain`.
- **The gearbox.** As on the 2020–2022 list: the manual transmission oil under towing at 30,000 and 90,000
  (`manualOnly`, table only), the transmission fluid or oil under towing at 60,000 and 120,000 (the
  `trans-fluid` reminder, severe use only), and the CVT fluid under long idling at 60,000 and 120,000
  (`manualExcluded`, table only). The reason the manual oil and CVT lines have no service is on the
  [2.0L's page](toyota-corolla-2019-2022-2.0.md).
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles
  if 0W-20 wasn't used at the last change, the note on the cabin air filter in heavy traffic or dust, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the guide's interval under any of its
  four special operating conditions), tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36,
  spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every 50,000 / 60 (a
  footnote; only 100,000 is in the table). The guide's "or 120 months" for the first coolant change isn't
  used: it counts from the day the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): the transmission fluid or oil every 60,000 / 72 when towing or carrying heavy
  loads.
- The inspections are in the table only, with the check line where Glovebox has one (fluid levels, wiper
  blades, brakes, engine coolant, drive belts). The drive belts are inspected first at 60,000 and then
  every 15,000.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- **Not checked:**
  - Whether any 2017–2019 Corolla was sold with an automatic that isn't a CVT. If one was, it keeps the
    CVT line (`manualExcluded` can't tell a CVT from another automatic).
  - vPIC's model list for 2017 and 2018 (`GetModelsForMakeYear`): it answered 503 on 2026-10-26. Whether it
    gives the 2017–2018 Corolla iM hatchback, which has a guide of its own that wasn't read, as "Corolla" is
    not known. If it does, an iM would be offered this sedan's list.
- Not covered: 2016, whose log differs ([toyota-corolla-2016](toyota-corolla-2016.md)), and 2015 and
  earlier, whose guides weren't read.
