# Toyota Prius, 2016

How `app/src/main/assets/known-schedules/toyota-prius-2016.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for each model year. Each address came from Toyota's
owners site (`https://www.toyota.com/service/tcom/cs/manuals/downloads/prius/<year>`).
Read on 2026-10-23.

| Model year | Guide | How it was read |
|---|---|---|
| 2016 | `https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-Prius/pdf/2016PriusWSG.pdf` | In full, by eye; the list was written from it |

- The log has 24 milestones, 5,000 miles or 6 months apart, to 120,000 miles. Nothing in the guide says it
  repeats, so the table doesn't.
- The 2016 guide is a printer's proof (each page has a job slug dated 7 December 2015), filed in Toyota's
  `T-MMS-Prius` folder rather than as `T-MMS-16Prius`. Its warranty pages name the 2016 model year Prius. Its
  text doesn't extract cleanly, so the whole log was read as page images.
- **One engine**, the 1.8L hybrid, in every year, so the list has no `engine` block; every Prius is a hybrid,
  so it needs no `powertrain` either.
- **Model name.** NHTSA's model list for Toyota (`GetModelsForMakeYear`, sent only the make and year, checked
  2026-10-23) gives `Prius` in every year from 2016 to 2022, which is the list's `model`. The `Prius C`,
  `Prius V` and `Prius Prime (PHEV)` are separate models there, with guides of their own, and aren't covered
  by this list.
- No line is marked for a drivetrain (the Prius was front-wheel drive only in this year). The gearbox is the
  hybrid's automatic only; nothing is marked for a manual.
- **2016 only:** the guide inspects the front differential oil every 30,000 miles and replaces it at 60,000
  and 120,000 when towing. Those lines are taken as printed; the 2017 guide drops them. The 2016 guide also
  has no cabin air filter inspection at 15,000, 45,000, 75,000 and 105,000, and says "steering gear box" where
  later years say "steering gear".
- The guide has no hybrid battery intake filter lines in this year; they start in 2018.
- **Coolant.** The 100,000-mile box prints one row, "engine/inverter coolant", and its footnote gives the
  engine coolant first at 100,000 miles / 120 months and every 50,000 / 60 after, and the inverter coolant
  first at 150,000 / 180 months and every 50,000 / 60 after. So the row at 100,000 is the engine coolant
  ("Replace engine coolant", `coolant`, first at 100,000 then every 50,000 / 60). The inverter coolant, which
  the log never reaches, is printed only in that footnote, apart from the table, so it is an `additional` job
  ("Replace inverter coolant", `inverter-coolant`, first at 150,000 then every 50,000 / 60), never placed on
  the table (the owner's ruling of 2026-10-23, #33). The "or 120 months" and "or 180 months" for the first
  changes aren't used: they count from the day the vehicle went into service, which Glovebox doesn't know.
- **Spark plugs** every 120,000 / 144, as the log prints. A footnote gives 150,000 for PZEV vehicles; Glovebox
  has no PZEV mark, so a PZEV owner should change that reminder by hand.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, which is the guide's interval under any
  of its four special operating conditions), tire rotation 5,000 / 6, cabin and engine air filters 30,000 /
  36, spark plugs 120,000 / 144, engine coolant first at 100,000 then every 50,000 / 60, front differential
  oil inspection 30,000 / 36 (`front-diff-check`). Apart from the table: inverter coolant first at 150,000
  then every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 / 72 when towing or carrying heavy
  loads; front differential oil every 60,000 / 72 when towing. Under ordinary driving the guide doesn't
  replace these (it only inspects the differential oil).
- **The front differential oil inspection is a reminder** (30,000 / 36, the table's own interval, with the
  `front-diff-check` line; logging a change counts as its check). By the owner's ruling of 2026-10-23 (#33)
  any vehicle with a differential needs it inspected, so the 4Runner's rule applies wherever a guide prints
  the inspection. The other inspections stay in the table only (rule 7), with a `-check` line where Glovebox
  has one (fluid levels, wiper blades, coolant, brakes).
- Left out, because they aren't jobs at a milestone: the note at every odd 5,000 from 15,000 to change the oil
  if 0W-20 wasn't used at the last change, the note on the cabin air filter, and the explanations of each
  item.
- Brake fluid has no replacement in the guide's log, so it has no reminder from this list.
- Not covered here: the other years of this generation, which have lists of their own
  ([2017](toyota-prius-2017.md), [2018](toyota-prius-2018.md), [2019–2020](toyota-prius-2019-2020.md),
  [2021–2022](toyota-prius-2021-2022.md)), 2015 and earlier (the previous generation, whose guides weren't
  read) and 2023 on (a new generation, whose guides weren't read).
