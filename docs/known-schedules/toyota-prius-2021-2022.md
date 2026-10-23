# Toyota Prius, 2021–2022

How `app/src/main/assets/known-schedules/toyota-prius-2021-2022.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year. Each address came from Toyota's
owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/prius/<year>`).
Read on 2026-10-23.

| Model year | Guide | How it was read |
|---|---|---|
| 2021 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-21Prius/pdf/T-MMS-21Prius.pdf` | In full, by eye; the list was written from it |
| 2022 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-22Prius/pdf/T-MMS-22Prius.pdf` | Compared by script, milestone by milestone, with the year read in full; no difference in the jobs or where they fall |

- The log has 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles. Nothing in the guide says it
  repeats, so the table doesn't.
- Script counts of every job per milestone box (text extraction is only a pointer) found no difference between
  the years; each difference the script reported was text-extraction noise, checked on the page.
- **One engine**, the 1.8L hybrid, in every year, so the list has no `engine` block; every Prius is a hybrid,
  so it needs no `powertrain` either.
- **Model name.** NHTSA's model list for Toyota (`GetModelsForMakeYear`, sent only the make and year, checked
  2026-10-23) gives `Prius` in every year from 2016 to 2022, which is the list's `model`. The `Prius C`,
  `Prius V` and `Prius Prime (PHEV)` are separate models there, with guides of their own, and aren't covered
  by this list.
- **AWD-e.** These years add an all-wheel-drive Prius, and the guide marks its lines "(AWD)" itself, so they
  are in `allWheelDriveOnly`: re-torque the drive shaft nut (AWD) every 15,000 miles, or at every other
  milestone too on dusty roads; inspect the rear differential oil (AWD) every 30,000; replace it at 60,000 and
  120,000 when towing. Only a Prius known to drive two wheels loses them. Nothing is marked "(4WD)", so the
  ruling on Toyota's "(4WD)" lines doesn't arise. How NHTSA decodes an AWD-e Prius's drive type wasn't checked
  (no VIN was sent anywhere).
- **Hybrid battery intake filter:** an ordinary job from 2021: inspected at every milestone and cleaned every
  20,000 miles (the guide's footnote: look at it every 5,000 and clean it if needed, clean it every 20,000).
  The cleaning is a reminder every 20,000 / 24 months (`hybrid-battery-filter`); the inspection is in the
  table only. It is no longer under the idling condition.
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
  36, spark plugs 120,000 / 144, engine coolant first at 100,000 then every 50,000 / 60, hybrid battery intake
  filter 20,000 / 24.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or carrying heavy
  loads; rear differential oil (AWD) every 60,000 / 72 when towing. Under ordinary driving the guide doesn't
  replace these (it only inspects the differential oil).
- Inspections stay in the table only (rule 7), with a `-check` line where Glovebox has one (fluid levels,
  wiper blades, coolant, rear differential oil, brakes).
- Left out, because they aren't jobs at a milestone: the note at every odd 5,000 from 15,000 to change the oil
  if 0W-16 wasn't used at the last change, the note on the cabin air filter, and the explanations of each
  item.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered here: the other years of this generation, which have lists of their own
  ([2016](toyota-prius-2016.md), [2017](toyota-prius-2017.md), [2018](toyota-prius-2018.md),
  [2019–2020](toyota-prius-2019-2020.md)), 2015 and earlier (the previous generation, whose guides weren't
  read) and 2023 on (a new generation, whose guides weren't read).
