# Volkswagen ID.4, 2022

How `app/src/main/assets/known-schedules/volkswagen-id4-2022.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissued 2022 maintenance card**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`, document type
"Maintenance Card", importer `444`, `en-US`). No 2022 card from the time for the electric models was found,
so the reissue is the only source; the owner's ruling of 2026-10-23 allows that, cited as a reissue with its
print date, and the owner's answers on #456 ship this list from it
([the survey](https://github.com/mcosgriff/glovebox/issues/456#issuecomment-6493872434),
[the owner's answers](https://github.com/mcosgriff/glovebox/issues/456#issuecomment-6511734730)).
Read on 2026-10-29, as page images, by eye.

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2022 | `USA5R507921`, `…/v1/document/7902c126-ce9f-44ea-842e-754effc6d27c` | 12/03/2025 | Section 2.2, "Maintenance Schedule (Battery Electric Vehicles)", pp. 5–7, in full; the list was written from it |

- **The section is a grid of two service levels**, 10 columns from 10,000 to 100,000 miles (with kilometres
  under them): "Standard Maintenance – small" at 10,000, 30,000 … 90,000 and "Standard Maintenance" at 20,000,
  40,000 … 100,000. Each milestone here is the jobs of its column's level, each named once. Nothing says it
  repeats.
- **Standard here doesn't say it includes the small service** (from 2023 it does). Its own rows already hold
  every small job but one wording: the small service's "check for fault codes with the diagnostic tester" and
  the standard's "guided fault finding with the diagnostic tester" are taken as one job, as on the 2021 card,
  where both levels print it the same. So every 20,000 milestone has all of the small service's jobs anyway.
- **No months per column.** The footnotes give the first small service at 10,000 or 1 year, then every 20,000
  or 2 years, and the standard service every 20,000 or 2 years. The columns carry no months, so the milestones
  have none.
- **Every job in the table is an inspection**, so the table drives no reminder (rule 7) and the list has no
  `intervals`, as the owner allowed on #456. Rows marked "BEV", "BEV and PHEV" or "All Vehicles" are all taken;
  the section is for battery-electric cars.
- **Reminders** are the section's "Additional Maintenance Items": brake fluid every 24 months by time alone
  (USA: 3 years after registration first, then every 2, so the name says it), and the dust and pollen filter
  every 40,000 miles or 24 months.
- **Left out:** "Heat Pump – Replace refrigerant", every 4 years, is marked "(Only Canada)"; this is a USA list.
- **The model.** vPIC calls it "ID.4" for 2022 (`GetModelsForMakeYear`), so the list is for that model, with no
  `engine`. No drivetrain rows; nothing for the all-wheel-drive car alone.
- **Not covered:** other years have their own lists: [2021](volkswagen-id4-2021.md) (from the card of the time),
  [2023–2026](volkswagen-id4-2023-2026.md).
