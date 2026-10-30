# Volkswagen ID.4, 2023–2026

How `app/src/main/assets/known-schedules/volkswagen-id4-2023-2026.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissued maintenance cards**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`, document type
"Maintenance Card", importer `444`, `en-US`), the same four the [GTI 2023–2026 page](volkswagen-gti-2023-2026.md)
lists. No card from the time was found for these years, so the reissues are the only source; the owner's
ruling of 2026-10-23 allows that, cited as reissues with their print dates, and the owner's answers on #456
ship this list from them ([the survey](https://github.com/mcosgriff/glovebox/issues/456#issuecomment-6493872434),
[the owner's answers](https://github.com/mcosgriff/glovebox/issues/456#issuecomment-6511734730)).
Read on 2026-10-29, as page images, by eye.

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2023 | `USA5R507521`, `…/v1/document/7f3ce02f-ee20-4e97-8747-fb18f14b9b66` | 12/03/2025 | Section 2.2, "Maintenance Schedule (Battery Electric Vehicles)", pp. 4–7, in full; the list was written from it |
| 2024 | `USA5R505721`, `…/v1/document/2721b3eb-0228-4be4-94d6-0b8e24dbf74c` | 12/03/2025 | Its battery-electric section: grid on the page, rows compared by script, differences looked at |
| 2025 | `USA5R701521`, `…/v1/document/760f5f9a-0290-4dfe-adf0-30d03981a9ad` | 12/03/2025 | The same (section 1.2) |
| 2026 | `USA5R506821`, `…/v1/document/d063309f-f793-4d23-8799-a4ff5ffb3850` | 02/2026 | The same (section 1.2) |

- **The section is a grid of two service levels**, 10 columns from 10,000 to 100,000 miles: "Standard
  Maintenance – small" at 10,000, 30,000 … 90,000 and "Standard Maintenance" at 20,000, 40,000 … 100,000; the
  standard service starts with "Perform Minor Maintenance". Each milestone here is the jobs of its column's
  levels, each named once. Nothing says it repeats. The grid is the same in all four years.
- **What differs between the years** is wording ("check for, DTCs" corrected), the charging cable row's
  applicability ("BEV and PHEV" in 2023, "BEV" after), the footnote naming the small service, and the heat pump
  (below). Every job and column is the same.
- **No months per column.** The footnotes give the first small service at 10,000 or 1 year, then every 20,000
  or 2 years, and the standard service every 20,000 or 2 years. The columns carry no months, so the milestones
  have none.
- **Every job in the table is an inspection**, so the table drives no reminder (rule 7) and the list has no
  `intervals`, as the owner allowed on #456. The small service's "check for fault codes with the diagnostic
  tester" and the standard's "guided fault finding" are one job here, as on the 2021 card.
- **Reminders** are the section's "Additional Maintenance Items": brake fluid every 24 months by time alone
  (USA: 3 years after registration first, then every 2, so the name says it), and the dust and pollen filter
  every 24 months by time alone: from 2023 the section prints "Every 2 years" with no distance (2022: 40,000
  or 2 years, [its own list](volkswagen-id4-2022.md)).
- **Left out, the heat pump.** 2023 and 2024 print "Heat Pump – Replace refrigerant (Only Canada)", every 4
  years; this is a USA list. 2025 (`USA5R701521`, p. 6, §1.2.3) prints it for the USA as "only PR code KK2, if
  equipped", every 4 years regardless of mileage (Canada: every 8). It is keyed to a factory option code
  Glovebox can't know, has no catalog line, and earlier lists leave "if equipped" additional items out, so it
  stays out and 2025 shares this list (#456, question 3). 2026 has no heat pump row.
- **The model.** vPIC calls it "ID.4" for 2023 to 2026 (`GetModelsForMakeYear`), so the list is for that model,
  with no `engine`. No drivetrain rows; nothing for the all-wheel-drive car alone.
- **Not covered:** [2021](volkswagen-id4-2021.md) and [2022](volkswagen-id4-2022.md) have their own lists. The
  [2025 ID. Buzz](volkswagen-id-buzz-2025.md) has a list of its own with the same jobs.
