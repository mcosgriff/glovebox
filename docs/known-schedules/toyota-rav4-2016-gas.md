# Toyota RAV4, 2016 (gasoline)

How `app/src/main/assets/known-schedules/toyota-rav4-2016-gas.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for 2016, as listed by Toyota's owners site:
`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-16RAV4/pdf/T-MMS-16RAV4.pdf`. Read on 2026-10-23.

- The 2017 guide's log was read in full by eye ([toyota-rav4-2017-2018-gas](toyota-rav4-2017-2018-gas.md)),
  and the 2016 log (printed pages 38 to 53, the same 24 milestones to 120,000 miles or 144 months) compared
  with it box by box. Every box where a script found a difference was read on the page. The 2016 log differs,
  so it has a list of its own:
  - there is no cabin air filter inspection at 15,000, 45,000, 75,000 and 105,000;
  - at every 30,000 it inspects the front differential oil, with no drive mark;
  - at 60,000 and 120,000 it replaces the front differential oil when towing or carrying heavy loads, with no
    drive mark;
  - it says "steering gear box" where 2017 says "steering gear" (the same job, worded the same here).
- The front differential jobs are taken as printed, for every drive, since the guide marks them for none.
- Everything else is as on the 2017–2018 list: `"powertrain": "gas"` beside the hybrid's own list, the guide's
  "(4WD)" lines named "(AWD)" and listed in `allWheelDriveOnly` (owner's ruling of 2026-10-23, #33), the same
  notes left out and the same reminders, plus the front differential oil every 60,000 / 72 when towing, only for
  severe use.
- Inspections that are reminders, at the table's own intervals: front differential oil 30,000 / 36
  (`front-diff-check`), rear differential oil (AWD) and transfer case oil (AWD) 15,000 / 18
  (`rear-diff-check`, `transfer-case-check`); logging a change counts as its check. By the owner's ruling of
  2026-10-23 (#33) any vehicle with a differential needs it inspected, so the 4Runner's rule applies wherever a
  guide prints the inspection. The other inspections are not reminders.
- Not covered:
  - **2013–2015.** Toyota's owners site lists a guide for each
    (`https://assets.sipb.toyota.com/publications/en/omms-s/T-MMS-13RAV4/pdf/2013_Toyota_RAV4_WMG_lr.pdf`,
    `…/T-MMS-14RAV4/pdf/2014_Toyota_RAV4_WMG_lr.pdf`, `…/T-MMS-15RAV4/pdf/2015_Toyota_RAV4_WMG.pdf`). They were
    downloaded and compared by script only, not read in full, so no year of them is covered. The script shows
    none of them matches 2016: none has the propeller shaft bolt re-torque, 2013 has no wiper blade inspection,
    and 2013 and 2014 inspect "automatic transmission fluid" where 2015 inspects the transmission for leaks.
    They would need lists of their own. vPIC also gives the 2013–2014 RAV4 EV the model name "RAV4".
  - 2017–2018: their own list. 2019 on: a new generation.
