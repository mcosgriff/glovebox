# Volkswagen ID. Buzz, 2025

How `app/src/main/assets/known-schedules/volkswagen-id-buzz-2025.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: **Volkswagen's reissued 2025 maintenance card**, from its owners-literature site
(`https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<year>`, document type
"Maintenance Card", importer `444`, `en-US`). It is the only maintenance card the site has for 2025, and no
card from the time was found; the owner's ruling of 2026-10-23 allows a reissue, cited with its print date,
and the owner's answers on #456 ship this list from it
([the survey](https://github.com/mcosgriff/glovebox/issues/456#issuecomment-6493872434),
[the owner's answers](https://github.com/mcosgriff/glovebox/issues/456#issuecomment-6511734730)).
Read on 2026-10-29, as page images, by eye.

| Model year | Document | Printed | What was read |
|---|---|---|---|
| 2025 | `USA5R701521`, `…/v1/document/760f5f9a-0290-4dfe-adf0-30d03981a9ad` | 12/03/2025 | Section 1.2, "Maintenance Schedule (Battery Electric Vehicles)", pp. 4–6, in full |

- **Why the ID. Buzz gets this section.** It names no model; its rows say "All Vehicles" or "BEV". vPIC's 2025
  Volkswagen models (`GetModelsForMakeYear`) have two battery-electric cars, "ID.4" and "ID. Buzz", and the
  card is Volkswagen's one schedule for 2025, so its battery-electric section is the ID. Buzz's (asked as
  question 4 on #456; the owner's answers ship the list).
- **The list is the [2023–2026 ID.4 list](volkswagen-id4-2023-2026.md)'s jobs**, from the same section of the
  same card; that page has how the grid and levels were read. In short: 10 columns, 10,000 to 100,000 miles,
  the small service at the odd ten-thousands and the standard (which includes it) at the even ones; no months
  per column; every table job an inspection, so no `intervals` (the owner allowed that on #456).
- **Reminders:** brake fluid every 24 months by time alone (3 years after registration first, then every 2),
  and the dust and pollen filter every 24 months by time alone ("Every 2 years").
- **Left out, the heat pump** (p. 6, §1.2.3): "Heat Pump – Replace refrigerant (only PR code KK2, if
  equipped)", every 4 years regardless of mileage (USA), every 8 (Canada). It is keyed to a factory option code
  Glovebox can't know and has no catalog line, so it stays out (#456, question 3).
- **The model.** vPIC calls it "ID. Buzz" for 2025 only; it has no ID. Buzz for 2026. Models are matched
  ignoring spaces, so "ID.Buzz" finds it too. No `engine`, no drivetrain rows.
- **Not covered:** 2026, which vPIC doesn't list.
