# Volkswagen GTI, 2022 (Mk8)

How `app/src/main/assets/known-schedules/volkswagen-gti-2022.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "2022 VW Maintenance Schedule - All Model Gasoline Vehicles", USA, which Volkswagen
filed with NHTSA as a manufacturer communication. Read on 2026-10-23.

| Document | What was read |
|---|---|
| `https://static.nhtsa.gov/odi/tsbs/2021/MC-10199495-0001.pdf` (revised 7/21/2021) | The newest revision, in full, off the page; the list was written from it |
| `…/2021/MC-10190513-0001.pdf` (4/9/2021), `…/MC-10190515-0001.pdf`, `…/MC-10190996-0001.pdf` (4/21/2021) | Earlier revisions, compared by script. They give the GTI's spark plugs every 40,000 miles or 4 years; the 7/21 revision moves "Golf GTI" to 80,000 or 6 years, and the DSG row's "GTI" becomes "Golf GTI". The list follows the newest |
| Volkswagen's reissue of the 2022 card, `USA5R507921`, dated 12/03/2025 (`https://ownersliterature.vw.com/owners-literature-service/v1/document/7902c126-ce9f-44ea-842e-754effc6d27c`) | Not used: the card from the time wins (the owner's ruling of 2026-10-23, #33). It differs from the card: its grid ends at 100,000, and it adds a belt starter-generator belt at 110,000, toothed-belt checks at 180,000 / 15 years and the tire mobility kit bottle every 4 years |

- **Model name.** vPIC's model list for 2022 (`GetModelsForMakeYear`, read 2026-10-23) has "Golf GTI" and
  no "GTI", so the list is for "Golf GTI" with no other spelling.
- **The card covers every gasoline model.** It is a grid of service levels, not of jobs: Minor at 10,000,
  30,000 … 110,000; Standard at 20,000, 40,000 … 120,000; Extended at 40,000, 80,000 and 120,000. Each
  level's jobs are listed under the grid. Standard includes every minor job and Extended every standard
  one, so each milestone here is the jobs of the levels marked in its column, each named once. The grid
  runs to 120,000 and nothing says it repeats.
- **No months per column.** The footnotes give a first minor service at 10,000 or 1 year, then every
  20,000 or 2 years, and standard services at 20,000 or 2 years, then every 20,000 or 2 years, each
  counted from the last. The grid's columns carry no months, so the milestones have none, and the oil
  reminder has no months either (the footnotes come to about once a year).
- **"Additional Maintenance Items"** are a separate table of job and interval with no column marked. They
  are reminders in `additional` with the card's figures, never placed on the grid (the owner's ruling of
  2026-10-23, #33): air filter 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says 3
  years after registration first, then every 2); dust and pollen filter 40,000 / 24 mo; spark plugs
  80,000 / 72 mo ("Golf GTI"); DSG fluid 80,000 ("Golf GTI"; not for a manual car, `manualExcluded`).
- **Left out of the additional items:**
  - Not the GTI's, by the card's own naming: spark plugs every 40,000 (Arteon, Golf R, Passat) and the
    non-DSG automatic's fluid (the GTI's automatic is the DSG named on the row above).
  - Marked "(if equipped)": AWD clutch fluid and front axle differential lock fluid, each every 3 years. A
    row marked so is never a reminder (the owner's ruling on "(if applicable)" rows), and these have no
    table to stand in.
  - Inspections, which aren't reminders (rule 7) and have no table to stand in: the panorama sunroof
    noise and function test every 10,000 / 1 year, and the sunroof drains check every 20,000 / 2 years.
- **The table's jobs** are all inspections apart from the oil change: they stay table-only, with a
  `-check` service where one exists. Two lines that would share one service in a milestone are kept
  apart and only one has it: the brake pad check is `brake-inspect` and the brake system and shock
  absorber check has none; the extended "ribbed belt" check is `belt-check` and the standard "ribbed
  belt and tensioning roller for belt starter-generator" check has none. Rows the card marks "(if
  equipped)" say "(if fitted)" (headlight washers, trailer hitch, torque splitter oil, sunroof).
- Nothing in the three levels was left out: the card marks none of their rows for another model.
- Reminders: engine oil 10,000 (from the table); the five additional items above.
- Not covered: 2021 and earlier (Mk7), and 2023 on, which has its own list from Volkswagen's reissued
  cards ([2023–2026](volkswagen-gti-2023-2026.md)).
