# Toyota Prius, 2018

How `app/src/main/assets/known-schedules/toyota-prius-2018.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year. Each address came from Toyota's
owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/prius/<year>`).
Read on 2026-10-23.

| Model year | Guide | How it was read |
|---|---|---|
| 2018 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-18Prius/pdf/T-MMS-18Prius.pdf` | In full, by eye; the list was written from it |

- The log has 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles. Nothing in the guide says it
  repeats, so the table doesn't.
- **One engine**, the 1.8L hybrid, in every year, so the list has no `engine` block; every Prius is a hybrid,
  so it needs no `powertrain` either.
- **Model name.** NHTSA's model list for Toyota (`GetModelsForMakeYear`, sent only the make and year, checked
  2026-10-23) gives `Prius` in every year from 2016 to 2022, which is the list's `model`. The `Prius C`,
  `Prius V` and `Prius Prime (PHEV)` are separate models there, with guides of their own, and aren't covered
  by this list.
- No line is marked for a drivetrain (the Prius was front-wheel drive only in this year). The gearbox is the
  hybrid's automatic only; nothing is marked for a manual.
- **Hybrid battery intake filter:** cleaned at every milestone under long idling or low-speed driving only, so
  it is a severe-only reminder every 5,000 miles / 6 months (`hybrid-battery-filter`). The guide has no
  ordinary line for it in these years.
- **Coolant.** The 100,000-mile box prints one row, "engine/inverter coolant", and its footnote gives the
  engine coolant first at 100,000 miles / 120 months and every 50,000 / 60 after, and the inverter coolant
  first at 150,000 / 180 months and every 50,000 / 60 after. So the row at 100,000 is the engine coolant
  ("Replace engine coolant", `coolant`, first at 100,000 then every 50,000 / 60), and the inverter coolant,
  which the log never reaches, has no reminder (first at 150,000, then every 50,000 / 60 by the footnote): the owner's
  ruling puts in `additional` only jobs printed apart from the table, not a footnote's, so add it by
  hand if you want it. The "or 120 months" and "or 180 months"
  for the first changes aren't used: they count from the day the vehicle went into service, which Glovebox
  doesn't know.
- **Spark plugs** every 120,000 / 144, as the log prints. A footnote gives 150,000 for PZEV vehicles; Glovebox
  has no PZEV mark, so a PZEV owner should change that reminder by hand.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval under any
  of its four special operating conditions), tire rotation 5,000 / 6, cabin and engine air filters 30,000 /
  36, spark plugs 120,000 / 144, engine coolant first at 100,000 then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or carrying heavy
  loads; cleaning the hybrid battery intake filter every 5,000 / 6 under long idling or low-speed driving.
  Under ordinary driving the guide doesn't replace these.
- Inspections stay in the table only (rule 7), with a `-check` line where Glovebox has one (fluid levels,
  wiper blades, coolant, brakes).
- Left out, because they aren't jobs at a milestone: the note at every odd 5,000 from 15,000 to change the oil
  if 0W-20 wasn't used at the last change, the note on the cabin air filter, and the explanations of each
  item.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered here: the other years of this generation, which have lists of their own
  ([2016](toyota-prius-2016.md), [2017](toyota-prius-2017.md), [2019–2020](toyota-prius-2019-2020.md),
  [2021–2022](toyota-prius-2021-2022.md)), 2015 and earlier (the previous generation, whose guides weren't
  read) and 2023 on (a new generation, whose guides weren't read).
