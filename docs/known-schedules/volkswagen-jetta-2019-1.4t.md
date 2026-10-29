# Volkswagen Jetta 1.4T, 2019 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-jetta-2019-1.4t.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). The [2019 Jetta GLI](volkswagen-jetta-gli-2019.md) list is made from the
same card. Every Mk7 Jetta card read, and what isn't covered, is [below](#every-mk7-jetta-card-read).

Source: Volkswagen's 2019 "Maintenance Schedule" card for all its models except the e-Golf, filed with NHTSA as a
manufacturer communication: the newest revision, revised 11/12/2020, `2020/MC-10183295-0001`, read in full on
2026-10-29, as page images, by eye. The earlier revisions (`2019/MC-10163818-0001`, `2019/MC-10168645-0001`) weren't
read again; the [2019 GTI's page](volkswagen-gti-2019.md) says what they print differently.

- **The card is read as the [2019 Golf 1.4T's](volkswagen-golf-2019-1.4t.md) list reads it**, which is the
  [2018 card's reading](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read). The rows are that list's, in
  its wording. So, as there:
  - **the hood latch** is left out by its own words, "Does not apply to: Passat, Golf, Beetle, and Jetta";
  - **both gearbox rows are left out**: "Transmission: 6-Speed DSG automatic transmission: Change fluid and filter"
    (every 40,000) and "Transmission, automatic (non-DSG automatic transmission): Change fluid" (every 80,000) name no
    model ([#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333),
    [#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)); the 7-speed DSG
    is "Golf GTI, Jetta GLI, Golf R only", which is the GLI's;
  - left out by their own words: the Passat-only sunroof water drains, the trailer hitch (Tiguan, Tiguan Limited and
    Atlas), the convertible top (Beetle Convertible). The AWD clutch fluid is left out: the Mk7 Jetta was sold with
    front-wheel drive only (vPIC decodes the 2020 and 2021 GLI's VIN patterns as "FWD/Front-Wheel Drive").
- **Spark plugs by engine.** "Spark plugs: Replace (Only Atlas, Golf SportWagen 1.4L, Golf 1.4L, GTI, Jetta 1.4L,
  Jetta GLI, Tiguan)", every 80,000 miles or 6 years; the 40,000-mile or 4-year row is "Except" the same models. The
  "Jetta 1.4L" mark names the engine: 80,000 / 72 mo.
- **The 2018 card's 1.4L belt checks are gone.** The 2018 card's "Coolant pump toothed belt" and "Timing belt"
  checks "(*1.4L Jetta only)", at 150,000 miles and every 20,000 after
  ([#440's question 3](https://github.com/mcosgriff/glovebox/issues/440#issuecomment-6489388227)), are not on the
  2019 card.
- **Why a list per engine.** vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`, read 2026-10-29), has
  "Jetta" and no "Jetta GLI", and decodes the GLI's VIN pattern (`3VW6T7BU*KM`) as "Jetta", trim "GLI (S, 35th
  Anniversary Ed., Autobahn)", 2.0 litres, "2.0 TSI", and the Jetta's (`3VWC57BU*KM`) as "Jetta", trim "S, SE,
  R-Line", 1.4 litres, "1.4 TSI". The two differ on the card only by the 7-speed DSG row, which names the "Jetta
  GLI". So this list has `"engine": { "label": "1.4T", "displacementL": 1.4 }` and the GLI's has the 2.0; the
  split is by the engine's size, never by the trim.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24 months by
  time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24 mo; front axle
  differential lock fluid every 3 years "(if fitted)"; panoramic sunroof rails every 40,000 or 2 years, first at
  40,000 (the card's "or 3 years" for the first time is recorded, not put into months); the tire filler bottle's
  date every 2 years "(if fitted)"; spark plugs 80,000 / 72 mo.

## Every Mk7 Jetta card read

All are Volkswagen's cards for every model, read on 2026-10-29 as page images, by eye, each as the GTI's list of the
same year reads it (the readings shared by every model are on the
[2018 Jetta's](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read) and the
[2020–2021 Passat's](volkswagen-passat-2020-2021.md#how-the-20202022-all-model-cards-are-read) pages, and the
[2023–2026 GTI's](volkswagen-gti-2023-2026.md)).

| Model year | Document | Lists |
|---|---|---|
| 2019 | `2020/MC-10183295-0001`, revised 11/12/2020, the newest | [1.4T](volkswagen-jetta-2019-1.4t.md), [GLI](volkswagen-jetta-gli-2019.md) |
| 2020 | `2019/MC-10168643-0001`, revised 11/26/2019, the newest | [1.4T](volkswagen-jetta-2020-2021-1.4t.md), [GLI](volkswagen-jetta-gli-2020-2021.md) |
| 2021 | `2020/MC-10177239-0001`, 7/9/2020, the only one | the same two |
| 2022 | `2021/MC-10199495-0001`, revised 7/21/2021, the newest (its reissue `USA5R507921` not used: the card from the time wins) | [Jetta](volkswagen-jetta-2022.md), [GLI](volkswagen-jetta-gli-2022.md) |
| 2023 | Reissue `USA5R507521`, printed 12/03/2025 | [Jetta](volkswagen-jetta-2023.md), [GLI](volkswagen-jetta-gli-2023-2026.md) |
| 2024 | Reissue `USA5R505721`, printed 12/03/2025 | [Jetta](volkswagen-jetta-2024-2026.md), [GLI](volkswagen-jetta-gli-2023-2026.md) |
| 2025 | Reissue `USA5R701521`, printed 12/03/2025 | the same two |
| 2026 | Reissue `USA5R506821`, printed 02/2026 | the same two |

**Model names.** vPIC's model lists for Volkswagen (`GetModelsForMakeYear`, read 2026-10-29) have "Jetta" alone for
2019 and 2020, and "Jetta" and "Jetta GLI" for 2021 to 2026. VIN pattern decodes (`DecodeVinValues`) give the GLI as
"Jetta" for 2019 and 2020 (`3VW6T7BU*KM`, `3VW6T7BU*LM`, trim "GLI …", 2.0 litres) and "Jetta GLI" for 2021
(`3VW6T7BU*MM`). So the 2019 GLI list is for "Jetta" with the 2.0 engine; the 2020–2021 GLI list is for "Jetta GLI"
with `modelAliases` "Jetta" (seen for 2020) and the 2.0 engine, beside the "Jetta" 1.4T list; the 2022-on lists are
for "Jetta" and "Jetta GLI" and name no engine.

## Not covered

- **The gearbox rows on the 1.4T and the 2022 Jetta**: the non-DSG automatic's row names no model (above).
- **The 2023–2026 GLI's DSG** and **the 2024–2026 Jetta's automatic**: their pages say why, asked on #442.
- 2027: no maintenance card on the owners-literature site (as on the GTI's page).
