# Toyota Corolla 2.0L, 2019–2022

How `app/src/main/assets/known-schedules/toyota-corolla-2019-2022-2.0.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guides, read on 2026-10-23:

- the sedan's, `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>Corolla/pdf/T-MMS-<yy>Corolla.pdf`
  (`20` to `22`);
- the hatchback's, whose addresses vary: `T-MMS-19CorollaHatchback/pdf/T-MMS-19CorollaHatchback.pdf`,
  `T-MMS-20CorollaHB/pdf/T-MMS-20CorollaHB.pdf`, `T-MMS-21CorollaHatchback/pdf/T-MMS-21CorollaHB.pdf` and
  `T-MMS-22CorollaHB/pdf/T-MMS-22CorollaHB.pdf`, all under the same `…/omms-s/` folder.

- The 2020 sedan guide's maintenance log (pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2021 and 2022 (sedan) and 2019 to 2022 (hatchback) were compared with it by a script that reads each
  milestone's words, then by a script that names each job it finds at each milestone and holds them to this
  list. The jobs and where they fall are the same in all seven guides, apart from the engine lines below.
  What differs is wording: the hatchback's EFI additive line says "Hawaii and Puerto Rico only" and names no
  engine, and the 2021 sedan guide has a printer's proof stamp on its log pages. The hatchback's 120,000
  page was looked at on the page in 2019 and 2020.
- **One log, split by engine** (the owner's ruling of 2026-10-23). The sedan's log is for both its engines,
  the 1.8L (2ZR-FAE) and the 2.0L (M20A-FKS). Three lines name an engine, and every other line names none
  and so is for both:
  - Replace the brake booster's vacuum pump at 120,000, "(2ZR-FAE engine only)": left out of this list.
  - Inspect the brake booster's vacuum pump at 120,000, "(M20A-FKS engine only)": in this list.
  - Add Toyota's EFI tank additive, "(M20A-FKS engine AND Hawaii, Puerto Rico or Saipan only)": left out
    (see below).
  Every line's engine is printed plainly, so the split could be made. The hatchback's guide is for the
  2.0L alone and has exactly this list's lines. The 1.8L has a list of its own
  ([toyota-corolla-2020-2022-1.8](toyota-corolla-2020-2022-1.8.md)).
- **Sedan and hatchback are one list.** NHTSA gives both as "Corolla" (`GetModelsForMakeYear` for Toyota,
  2019 to 2026, checked 2026-10-23: no "Corolla Hatchback" and no "Corolla Hybrid"; only "Corolla Cross"
  and "GR Corolla" besides), so they can't be told apart, and here they don't need to be. The 2019 sedan
  is the previous generation, with a 1.8L only, so the only 2019 Corolla with a 2.0L is the hatchback.
- **The gearbox.** The 2.0L came with a manual or a CVT. Under towing or heavy loads the log replaces the
  manual transmission oil at 30,000 and 90,000, and the "transmission fluid or oil" at 60,000 and 120,000;
  under long idling it replaces the CVT fluid at 60,000 and 120,000. So:
  - "Replace manual transmission oil · towing or heavy loads" is in `manualOnly`: a car known to shift
    itself doesn't get it.
  - "Replace CVT fluid · long idling" is in `manualExcluded`: a car known to be a manual doesn't get it.
  - Both are in the table only, with no service line. The towing "fluid or oil" change carries the
    transmission fluid line, and a service may appear only once at a milestone, so the CVT change beside it
    can't; and the manual oil change can't either, or the transmission fluid would be at every 30,000 and
    its 60,000 reminder wouldn't be the table's own. On Severe, the towing reminder already falls at the
    CVT change's mileages.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: adding Toyota's EFI
  tank additive (printed at every milestone, for Hawaii, Puerto Rico and Saipan only), the note that the oil
  is to be replaced at 5,000 miles if 0W-16 (the 2.0L's oil) wasn't used at the last change, and the
  explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval
  under any of its four special operating conditions), tire rotation 5,000 / 6, cabin and engine air
  filters 30,000 / 36, spark plugs 120,000 / 144, and engine coolant first at 100,000 miles and then every
  50,000 / 60. The guide's "or 120 months" for the first coolant change isn't used: it counts from the day
  the vehicle went into service, which Glovebox doesn't know.
- Only for severe use (#136): the transmission fluid or oil every 60,000 / 72 when towing or carrying heavy
  loads.
- The inspections are in the table only, with the check line where Glovebox has one (fluid levels, wiper
  blades, brakes, engine coolant, drive belts). The drive belts are inspected first at 60,000 and then
  every 15,000.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: the 2019 sedan (the previous generation; #192), and 2023 on, which are lists of their own
  ([2023–2024](toyota-corolla-2023-2024-2.0.md), [2025](toyota-corolla-2025-2.0.md)).
