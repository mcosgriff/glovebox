# Toyota Tundra, 2022 (gas)

How `app/src/main/assets/known-schedules/toyota-tundra-2022-gas.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2022 gas Tundra (the 3.4L twin-turbo V6),
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-22Tundra/pdf/T-MMS-22Tundra.pdf` (the same file
is on `assets.sia.toyota.com`). Read on 2026-10-23.

- The guide's maintenance log (pages 39 to 54: 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles)
  was read in full, page by page, and the list written from it.
- **Why 2022 is a list of its own.** It was compared with the 2023 and 2024 guides milestone by milestone.
  The jobs and milestones are the same except in two places:
  - The oil change under special operating conditions is given for dusty roads, towing, short trips in
    freezing weather and long idling (`Replace engine oil and filter · dust, towing, short cold trips or long
    idling`). In 2023 and 2024 it is for dusty roads only.
  - The inspection is "Radiator and condenser"; 2023 and 2024 say "Radiator, condenser and/or intercooler".
  So 2023–2024 is [another list](toyota-tundra-2023-2024-gas.md).
- **Gas and hybrid.** Toyota prints a separate guide for the hybrid (i-FORCE MAX), which is also 3.4L, so
  this list is marked `"powertrain": "gas"` and the [2022 hybrid](toyota-tundra-2022-hybrid.md) has its own.
  vPIC lists the model as "Tundra" for both.
- Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the list; the jobs it marks "(4WD)"
  (drive shaft boots, front differential oil and transfer case oil) say `(4WD)` and are left out for a truck
  known to be 2WD. The Tundra is a true 4WD truck; the guide has no "AWD" lines.
- Inspections are in the table, with a `-check` service where Glovebox has one (fluid levels, wiper blades,
  brakes, engine coolant, drive belts, differential and transfer case oils). None of them is a reminder.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 under any of the four special conditions), tire rotation
  5,000 / 6, cabin and engine air filters 30,000 / 36, spark plugs 40,000 / 48, and engine coolant and
  intercooler coolant each first at 100,000 miles and then every 50,000 / 60 (the guide's own footnotes give
  both). The "or 120 months" for the first coolant change isn't used: it counts from the day the truck went
  into service, which Glovebox doesn't know.
- Only for severe use (#136): front differential oil (4WD) every 15,000 / 18, rear differential oil every
  30,000 / 36 and automatic transmission fluid every 60,000 / 72 when towing or carrying heavy loads; transfer
  case oil (4WD) every 30,000 / 36 on dusty roads or when towing.
- In the table only: the propeller shaft bolt re-torque (every 15,000, or every 5,000 on dusty or salted
  roads or when towing), tightening the chassis and body nuts and bolts under special conditions, and the
  brake booster vacuum pump inspection at 120,000 (the guide's note says to replace its vane and caps; there
  is no Glovebox line for it).
- Left out: "Add Toyota EFI Tank Additive or equivalent (Hawaii only)", at every milestone, which is for one
  region; the note to change the oil at 5,000 miles if 0W-20 wasn't used last time; the off-road daily checks;
  and the explanations of each item.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: 2021 and earlier (another generation).
