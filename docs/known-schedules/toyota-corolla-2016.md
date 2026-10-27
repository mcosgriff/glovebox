# Toyota Corolla 1.8L, 2016

How `app/src/main/assets/known-schedules/toyota-corolla-2016.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2016 Corolla sedan (E170),
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-16Corolla/pdf/T-MMS-16Corolla.pdf`, listed by
Toyota's owners site for that year. Read on 2026-10-26.

- The log (PDF pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles) has the
  same milestones as the 2017–2019 guides. It was compared with the 2018 guide, which was read in full by
  eye ([toyota-corolla-2017-2019](toyota-corolla-2017-2019.md)), by a script that counts every word on each
  log page against 2018's page. Every page it found different was looked at: 15,000, 30,000, 60,000,
  90,000 and 120,000 by eye, and 45,000 / 75,000 / 105,000 have exactly the 15,000 page's differences in
  the count. The pages it found the same are the ones that hold two milestones each.
- **What differs from 2017–2019**, so 2016 is a list of its own:
  - No cabin air filter inspection at 15,000, 45,000, 75,000 and 105,000. (The note on the cabin air filter
    in heavy traffic or dust is still printed there.)
  - The front differential oil is inspected at every 30,000.
  - Under towing or heavy loads the front differential oil is replaced at 30,000 and 90,000 marked
    "manual transmission only", and at 60,000 and 120,000 with no mark.
  - There is no CVT fluid change under long idling.
  - It says "steering gear box" where later years say "steering gear"; the item keeps the later lists'
    name, "Inspect steering gear", as it is the same check.
  - The fluid-level check has no footnote about sealed transmissions and differentials (as in 2017).
- Everything else is as on the 2017–2019 list, with the same names and service lines.
- **The engine.** As on 2017–2019: replace the brake booster's vane-type vacuum pump at 120,000, "VALVEMATIC
  engine only", is kept for every 1.8L, in the table only, with no service line and no reminder, named
  "Replace brake booster vacuum pump (Valvematic engine)" (the owner's ruling of 2026-10-25, #192). The
  warranty pages name the 2ZR-FAE; Glovebox can't tell it from the 2ZR-FE, both 1.8L. The list has a 1.8L
  engine block, as the 2017–2019 one does.
- **The front differential.** The inspection at every 30,000 / 36 is a reminder (`front-diff-check`), as
  the differential oil checks are on every list whose guide prints them. Under towing, the change at 60,000
  and 120,000 is a severe-use reminder every 60,000 / 72 (`front-diff`), as on the 2016 Camry. The change
  at 30,000 and 90,000 is in the table only, with no service line, named "(manual transmission)" and in
  `manualOnly`, so a car known to shift itself doesn't get it: with a service it would put the change at
  every 30,000 and the 60,000 reminder wouldn't be the table's own. So a manual car's reminder is at 60,000
  and the table shows the 30,000 and 90,000 changes; this is the same arrangement as the manual
  transmission oil beside the transmission fluid on the later lists.
- **The gearbox.** The manual transmission oil under towing at 30,000 and 90,000 (`manualOnly`, table only)
  and the transmission fluid or oil under towing at 60,000 and 120,000 (the `trans-fluid` reminder, severe
  use only), as on 2017–2019. There is no CVT line, so no `manualExcluded`.
- Left out, because they aren't jobs at a milestone: the note that the oil is to be replaced at 5,000 miles
  if 0W-20 wasn't used at the last change, the cabin air filter note, and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use), tire rotation 5,000 / 6, cabin and
  engine air filters 30,000 / 36, spark plugs 120,000 / 144, engine coolant first at 100,000 miles and then
  every 50,000 / 60 (a footnote, as in later years; "or 120 months" isn't used), front differential oil
  inspection 30,000 / 36. Only for severe use: front differential oil and transmission fluid or oil, each
  every 60,000 / 72, when towing or carrying heavy loads.
- The other inspections are in the table only, with the check line where Glovebox has one. The drive belts
  are inspected first at 60,000 and then every 15,000. Brake fluid has no replacement in the log.
- **Not checked:** vPIC's model list for 2016 (`GetModelsForMakeYear`), which answered 503 on 2026-10-26.
  The 2016 Scion iM is a Scion, not a Toyota, so it wouldn't match a Toyota list in any case.
- Not covered: 2015 and earlier, whose guides weren't read.
