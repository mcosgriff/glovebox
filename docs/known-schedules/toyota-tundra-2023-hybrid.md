# Toyota Tundra, 2023 (hybrid)

How `app/src/main/assets/known-schedules/toyota-tundra-2023-hybrid.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2023 Tundra hybrid (i-FORCE MAX, 3.4L),
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-23TundraHV/pdf/T-MMS-23TundraHV.pdf`. Toyota's
owners-site index lists only the gas guide; the hybrid guide was found at its own address. Read on 2026-10-23.

- The guide's maintenance log (pages 40 to 55: 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles)
  was read in full, page by page, and the list written from it.
- **Why 2023 is a list of its own.** At 15,000 miles the guide gives no oil change for dusty roads; it gives
  one at every other odd 5,000 (5,000, 25,000, 35,000 … 115,000), and so do the 2023 gas guide and the 2024
  hybrid guide at 15,000. Nothing is assumed about why: the table is as printed, and the year doesn't share a
  list with 2024, which has the line ([2024](toyota-tundra-2024-hybrid.md)). The 2022 hybrid guide differs
  as well, giving that oil change for dusty roads, towing, short trips in freezing weather and long idling
  ([2022](toyota-tundra-2022-hybrid.md)).
- **So the oil reminder has no shorter interval for severe use here.** Elsewhere the special-conditions oil
  change every odd 5,000 makes it 5,000 / 6; with 15,000 missing, the table doesn't say that, and an interval
  has to be the table's own. The dusty-roads oil changes are in the table at the milestones the guide gives.
- **Gas and hybrid.** The gas Tundra (twin-turbo V6) is also 3.4L and has a guide of its own, so this list is
  marked `"powertrain": "hybrid"` and the [gas list](toyota-tundra-2023-2024-gas.md) `gas`. vPIC lists the
  model as "Tundra" for both.
- What the hybrid guide has that the gas one doesn't, and the reverse:
  - The HV battery cooling intake filter: inspected every 5,000 miles and cleaned every 20,000 (the clean
    takes the inspection's place at 20,000, 40,000 and so on). Cleaning it is a reminder, 20,000 / 24
    (`hybrid-battery-filter`); the inspection is in the table only.
  - Engine and inverter coolant are one line, inspected every 15,000 and replaced at 100,000. The replacement
    is `Replace engine coolant` (`coolant`), first at 100,000 and then every 50,000 / 60, as the guide's
    footnote gives for the engine coolant. **The inverter coolant has no reminder:** the same footnote puts its
    first change at 150,000 miles / 180 months, then every 50,000 / 60, which is past the end of the log and
    printed only in a footnote, so it is neither a table job nor one printed apart from the table, and
    `additional` holds only the latter (the ruling of 2026-10-23, #33).
  - No brake booster vacuum pump inspection (the gas guide has one at 120,000).
- Drivetrain is marked on the guide's own lines, so 2WD and 4WD share the list; the jobs it marks "(4WD)"
  (drive shaft boots, front differential oil and transfer case oil) say `(4WD)` and are left out for a truck
  known to be 2WD. The Tundra is a true 4WD truck; the guide has no "AWD" lines.
- Inspections are in the table, with a `-check` service where Glovebox has one (fluid levels, wiper blades,
  brakes, engine and inverter coolant, drive belts, differential and transfer case oils). None of them is a
  reminder.
- Reminders: engine oil 10,000 mi / 12 mo, tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36,
  spark plugs 40,000 / 48, the HV battery cooling filter clean 20,000 / 24, and engine coolant and intercooler
  coolant each first at 100,000 miles and then every 50,000 / 60. The "or 120 months" for the first coolant
  change isn't used: it counts from the day the truck went into service, which Glovebox doesn't know.
- Only for severe use (#136): front differential oil (4WD) every 15,000 / 18, rear differential oil every
  30,000 / 36 and automatic transmission fluid every 60,000 / 72 when towing or carrying heavy loads; transfer
  case oil (4WD) every 30,000 / 36 on dusty roads or when towing.
- In the table only: the propeller shaft bolt re-torque (every 15,000, or every 5,000 on dusty or salted
  roads or when towing) and tightening the chassis and body nuts and bolts under special conditions.
- Left out: "Add Toyota EFI Tank Additive or equivalent (Hawaii only)", at every milestone, which is for one
  region; the note to change the oil at 5,000 miles if 0W-20 wasn't used last time; the off-road daily checks;
  and the explanations of each item.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered: every other year (see the pages linked above). There was no Tundra hybrid before 2022.
