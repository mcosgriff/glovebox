# Toyota Corolla 1.8L (gas), 2020–2022

How `app/src/main/assets/known-schedules/toyota-corolla-2020-2022-1.8.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the Corolla sedan for each model year, which Toyota
publishes at
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-<yy>Corolla/pdf/T-MMS-<yy>Corolla.pdf`
(`20` to `22`). Read on 2026-10-23.

- The 2020 guide's maintenance log (pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to
  120,000 miles) was read in full and the list written from it.
- 2021 and 2022 were compared with it by a script that reads each milestone's words, then by a script that
  names each job it finds at each milestone and holds them to this list. The jobs and where they fall are
  the same in all three; the 2021 guide has a printer's proof stamp on its log pages.
- **One log, split by engine** (the owner's ruling of 2026-10-23). The sedan's log is for both its engines,
  the 1.8L (2ZR-FAE) and the 2.0L (M20A-FKS). Three lines name an engine, and every other line names none
  and so is for both:
  - Replace the brake booster's vacuum pump at 120,000, "(2ZR-FAE engine only)": in this list.
  - Inspect the brake booster's vacuum pump at 120,000, "(M20A-FKS engine only)": left out.
  - Add Toyota's EFI tank additive, "(M20A-FKS engine AND Hawaii, Puerto Rico or Saipan only)": left out.
  Every line's engine is printed plainly, so the split could be made. The 2.0L has a list of its own
  ([toyota-corolla-2019-2022-2.0](toyota-corolla-2019-2022-2.0.md)).
- **Gas, not hybrid.** The Corolla Hybrid also has a 1.8L engine and has a guide of its own, and NHTSA
  gives both as "Corolla" (`GetModelsForMakeYear` for Toyota, 2020 to 2022, checked 2026-10-23: no
  "Corolla Hybrid"). So this list has `engine.powertrain` `gas` and the hybrid lists for these years have
  `hybrid`; a 1.8L Corolla whose powertrain isn't on record is asked which it is.
- **The brake booster's vacuum pump** is replaced at 120,000. Glovebox has no line for it and it is a
  one-off within the log, so it is in the table only, with no reminder.
- **The gearbox.** The log's gearbox lines name no engine, so they are here as on the 2.0L's list: the
  manual transmission oil under towing at 30,000 and 90,000 (`manualOnly`, so a car known to shift itself
  doesn't get it), the transmission fluid or oil under towing at 60,000 and 120,000, and the CVT fluid
  under long idling at 60,000 and 120,000 (`manualExcluded`). The manual oil and CVT lines are in the table
  only, with no service line, for the reason given on the 2.0L's page.
- Left out, because they aren't jobs for every owner or aren't jobs at a milestone: the EFI tank additive
  (which is for the 2.0L in any case), the note that the oil is to be replaced at 5,000 miles if 0W-20 (the
  1.8L's oil) wasn't used at the last change, and the explanations of each item.
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
- Not covered: 2019 and earlier (the previous generation; #192), and 2023 on, when the gas sedan had the
  2.0L only.
