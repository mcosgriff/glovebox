# Volkswagen ID.4, 2021

How `app/src/main/assets/known-schedules/volkswagen-id4-2021.json` was made. The rules it follows
are in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's "2021 VW Maintenance Schedule – Electric Models" card, which Volkswagen files with NHTSA
as a manufacturer communication (`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are
in NHTSA's index at `https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.
What was read and the questions it raised are on #456
([the survey](https://github.com/mcosgriff/glovebox/issues/456#issuecomment-6493872434),
[the owner's answers](https://github.com/mcosgriff/glovebox/issues/456#issuecomment-6511734730)).

| Document | What it is | How it was used |
|---|---|---|
| `2021/MC-10185652-0001` (`21VWUSELCMN121`, dated 1/5/2021) | The electric models' card, revised. NHTSA's index files it under the ID.4 | In full, off the page; the list was written from it |
| `2020/MC-10183306-0001` (`21VWUSELCMN1120`, dated 11/13/2020) | The same card's first printing, filed under the 2021 gas models | Compared: the same but for the dust and pollen filter (below) |
| `USA5R507821`, `…/v1/document/b84f4eef-af10-494e-9115-309968da22d7`, printed 12/03/2025 | Volkswagen's 2021 reissue | For comparison only; the card from the time wins (the owner's ruling of 2026-10-23) |

- **The card is a grid of two service levels**, 12 columns from 10,000 to 120,000 miles: Minor at 10,000,
  30,000 … 110,000 and Standard at 20,000, 40,000 … 120,000, Standard "including all minor maintenance
  items". Each milestone here is the jobs of its column's level, each named once. Nothing says it repeats.
- **No months per column.** The footnotes give the first minor service at 10,000 or 1 year, then every
  20,000 or 2 years, and the standard service at 20,000 or 2 years, then every 20,000 or 2 years. The columns
  carry no months, so the milestones have none. There is no oil change, so no oil months either.
- **Every job in the table is an inspection** (12-volt and high-voltage batteries, charging cable and sockets,
  high-voltage parts and labels, brakes, cooling, lights, suspension, tires and tire pressure monitoring,
  underbody, wipers, the fault memory), so the table drives no reminder (rule 7) and the list has no
  `intervals`, as the owner allowed on #456.
- **Reminders** are the card's "Additional Maintenance Items", apart from the table: brake fluid every
  24 months by time alone (the card says 3 years after registration first, then every 2 years, so the name
  says it), and the dust and pollen filter every 40,000 miles or 24 months.
- **The dust and pollen filter.** The 11/13/2020 printing says "every 2 years"; the 1/5/2021 revision says
  every 40,000 miles or 2 years, whichever comes first, with the change printed in red. The newest revision
  wins (the owner's ruling of 2026-10-25, confirmed for this card on #456), so the list has 40,000 / 24 months.
- **The reissue differs** and isn't used for 2021: its battery-electric section is the later layout (10 columns
  to 100,000, "Standard Maintenance – small"), with somewhat different checks.
- **No drivetrain rows.** The card has nothing for the all-wheel-drive ID.4 alone, and no tire rotation, oil or
  air filter; the list has none.
- **The model.** vPIC calls it "ID.4" for 2021 (`GetModelsForMakeYear`), so the list is for that model, with no
  `engine`.
- **Not covered:** nothing on the card is left out. Other years have their own lists:
  [2022](volkswagen-id4-2022.md), [2023–2026](volkswagen-id4-2023-2026.md).
