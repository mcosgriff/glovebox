# Toyota Prius, 2023–2024

How `app/src/main/assets/known-schedules/toyota-prius-2023-2024.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year. Each address came from Toyota's
owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/prius/<year>`).
Read on 2026-10-23.

| Model year | Guide | How it was read |
|---|---|---|
| 2023 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-23Prius/pdf/T-MMS-23Prius.pdf` | In full, by eye (the maintenance log, pages 40 to 55); the list was written from it |
| 2024 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-24Prius/pdf/T-MMS-24Prius.pdf` | Compared by script, page by page, with the year read in full; each difference looked at on the page |

- The log has 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles. Nothing in the guide says it
  repeats, so the table doesn't.
- 2023 and 2024 have the same jobs at the same milestones. The only difference is the note at every odd
  5,000 from 15,000 to change the oil if the right oil wasn't used: 2023 says "0W-16", 2024 "recommended oil".
  The note isn't a job and is left out.
- **One engine**, the 2.0L hybrid, in both years, so the list has no `engine` block; every Prius is a hybrid,
  so it needs no `powertrain` either.
- **Model name.** NHTSA's model list for Toyota (`GetModelsForMakeYear`, sent only the make and year, checked
  2026-10-23) gives `Prius` for 2023 and 2024, which is the list's `model`. The `Prius Prime (PHEV)` is a
  separate model there with a guide and a list of its own ([2023–2024](toyota-prius-prime-2023-2024.md)).
- **AWD.** The guide marks its all-wheel-drive lines "(AWD)" itself, so they are in `allWheelDriveOnly`:
  re-torque the drive shaft nut (AWD) every 15,000 miles, or at every other milestone too on dusty roads;
  inspect the rear differential oil (AWD) every 30,000; replace it at 60,000 and 120,000 when towing. Only a
  Prius known to drive two wheels loses them. Nothing is marked "(4WD)", so the ruling on Toyota's "(4WD)"
  lines doesn't arise.
- **Hybrid battery intake filter:** inspected at every milestone and cleaned every 20,000 miles (the guide's
  footnote: look at it every 5,000 and clean it if needed, clean it every 20,000). The cleaning is a reminder
  every 20,000 / 24 months (`hybrid-battery-filter`); the inspection is in the table only.
- **Coolant.** The 100,000-mile page prints one line, "engine/inverter coolant", and its footnote gives the
  engine coolant first at 100,000 miles / 120 months and every 50,000 / 60 after, and the inverter coolant
  first at 150,000 / 180 months and every 50,000 / 60 after. The line at 100,000 is the engine coolant
  ("Replace engine coolant", `coolant`, first at 100,000 then every 50,000 / 60). The inverter coolant, which
  the log never reaches, is printed only in that footnote, apart from the table, so it is an `additional` job
  ("Replace inverter coolant", `inverter-coolant`, first at 150,000 then every 50,000 / 60), never placed on
  the table (the owner's ruling of 2026-10-23, #33). The "or 120 months" and "or 180 months" for the first
  changes aren't used: they count from the day the vehicle went into service, which Glovebox doesn't know.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the guide's interval on dusty roads),
  tire rotation 5,000 / 6, cabin and engine air filters 30,000 / 36, spark plugs 120,000 / 144, engine coolant
  first at 100,000 then every 50,000 / 60, hybrid battery intake filter 20,000 / 24, rear differential oil
  inspection (AWD) 30,000 / 36 (`rear-diff-check`). Apart from the table: inverter coolant first at 150,000
  then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or carrying heavy
  loads; rear differential oil (AWD) every 60,000 / 72 when towing. Under ordinary driving the guide doesn't
  replace these (it only inspects the transmission for leaks and the differential oil).
- The guide's special operating conditions are dirt or dusty roads, and towing, a car-top carrier or heavy
  loads. Each job under them is named with its condition; the chassis nuts and bolts are tightened at every
  milestone under either, so that is one line, "dusty roads or towing".
- **The rear differential oil inspection (AWD) is a reminder** (30,000 / 36, the table's own interval, with
  the `rear-diff-check` line; logging a change counts as its check). By the owner's ruling of 2026-10-23 (#33)
  any vehicle with a differential needs it inspected, so the 4Runner's rule applies wherever a guide prints
  the inspection. The other inspections stay in the table only (rule 7), with a check line where Glovebox has
  one (fluid levels, wiper blades, engine and inverter coolant, brakes). The guide's "radiator, condenser
  and/or intercooler" is "Inspect radiator, condenser and intercooler".
- Left out: "Add Toyota EFI Tank Additive or equivalent (Hawaii only)", at every milestone (a job for one
  state only); the note on the oil (above), the note on the cabin air filter, and the explanations of each
  item.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered here: 2025 and 2026 ([a chart, a list of its own](toyota-prius-2025-2026.md)), 2027
  ([a new chart, a list of its own](toyota-prius-2027.md)), and 2022 and earlier (the previous generation).
