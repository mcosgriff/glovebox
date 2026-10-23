# Toyota Prius Prime, 2021–2022

How `app/src/main/assets/known-schedules/toyota-prius-prime-2021-2022.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year. Each address came from Toyota's
owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/prius-prime/<year>`).
Read on 2026-10-23.

| Model year | Guide | How it was read |
|---|---|---|
| 2021 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-21PriusPrime/pdf/T-MMS-21PriusPrime.pdf` | In full, by eye; the list was written from it |
| 2022 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-22PriusPHV/pdf/T-MMS-22PriusPHV.pdf` | Compared by script, milestone by milestone, with the year read in full; no difference in the jobs or where they fall |

- The log has 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles. Nothing in the guide says it
  repeats, so the table doesn't.
- Script counts of every job per milestone box (text extraction is only a pointer) found no difference between
  the years; each difference the script reported was text-extraction noise, checked on the page.
- **One engine**, the 1.8L hybrid, in every year, so the list has no `engine` block; every Prime is a plug-in
  hybrid, so it needs no `powertrain` either.
- **Model name.** NHTSA's model list for Toyota (`GetModelsForMakeYear`, sent only the make and year, checked
  2026-10-23) gives the car as `Prius Prime (PHEV)` in every year from 2017 to 2022 and never as `Prius
  Prime`, so that is the list's `model`. No other spelling was seen, so it has no `modelAliases`. A Prime
  whose model was typed by hand as "Prius Prime" isn't matched.
- The Prime's guide has the same jobs at the same milestones as the front-wheel-drive Prius of its year. It is
  its own model in NHTSA's list, so it has its own list.
- No line is marked for a drivetrain (the Prime was front-wheel drive only). The gearbox is the hybrid's
  automatic only; nothing is marked for a manual.
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
  loads. Under ordinary driving the guide doesn't replace these.
- Inspections stay in the table only (rule 7), with a `-check` line where Glovebox has one (fluid levels,
  wiper blades, coolant, brakes).
- Left out, because they aren't jobs at a milestone: the note at every odd 5,000 from 15,000 to change the oil
  if 0W-16 wasn't used at the last change, the note on the cabin air filter, and the explanations of each
  item.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered here: the other years of this generation, which have lists of their own
  ([2017](toyota-prius-prime-2017.md), [2018–2020](toyota-prius-prime-2018-2020.md)), 2016 (no Prime that
  year) and 2023 on (a new generation, whose guides weren't read).
