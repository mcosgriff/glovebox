# Toyota RAV4 Hybrid, 2016

How `app/src/main/assets/known-schedules/toyota-rav4-2016-hybrid.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

Source: Toyota's Warranty and Maintenance Guide for the 2016 RAV4 Hybrid,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-16RAV4HV/pdf/T-MMS-16RAV4HV.pdf`. Read on
2026-10-23.

- The 2017 Hybrid guide's log was read in full by eye ([toyota-rav4-2017-hybrid](toyota-rav4-2017-hybrid.md)),
  and the 2016 log (printed pages 38 to 53, the same 24 milestones) compared with it box by box; every box that
  differs was read on the page. The 2016 log:
  - has no cabin air filter inspection at 15,000, 45,000, 75,000 and 105,000;
  - inspects the front differential oil at every 30,000, and replaces it at 60,000 and 120,000 when towing, with
    no drive mark (a reminder every 60,000 / 72, only for severe use);
  - at 30,000 only, when towing, replaces the rear differential oil and the transfer case oil, both marked
    "(4WD)". By the owner's ruling of 2026-10-23 (#33) they are named "(AWD)" and listed in
    `allWheelDriveOnly`. They come round at no interval (90,000 doesn't have them), so they are in the table
    only, with no service, and the rear differential's towing change at 60,000 and 120,000 stays the reminder.
    They look carried over from the gasoline guide (the 2017 and 2018 hybrid guides have neither), but they are
    what the guide prints, so they are kept;
  - says "steering gear box" where 2017 says "steering gear" (the same job, worded the same here).
- Everything else is as the 2017 page says: `"powertrain": "hybrid"`, the same reminders and severe-use jobs,
  and **no reminder for the inverter coolant** (the 2016 footnote also puts its first change at 150,000, past
  the table; the 100,000 job is "Replace engine coolant").
- Not covered: 2017 and 2018 (their own lists).
