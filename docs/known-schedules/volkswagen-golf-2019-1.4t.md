# Volkswagen Golf 1.4T, 2019 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-2019-1.4t.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). The [Golf SportWagen 1.4T](volkswagen-golf-sportwagen-2019-1.4t.md),
[Golf SportWagen 1.8T](volkswagen-golf-sportwagen-2019-1.8t.md) and [Golf Alltrack](volkswagen-golf-alltrack-2019.md)
lists are made from the same card. Every Mk7 card read, and what isn't covered, is on the
[2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).

Source: Volkswagen's 2019 "Maintenance Schedule" card for all its models except the e-Golf, filed with NHTSA
as a manufacturer communication: the newest revision, revised 11/12/2020, `2020/MC-10183295-0001`, read in
full on 2026-10-29, as page images, by eye. NHTSA's index files it under the Golf, the Golf SportWagen and the
Golf Alltrack. The earlier revisions (`2019/MC-10163818-0001`, `2019/MC-10168645-0001`) weren't read again;
the [2019 GTI's page](volkswagen-gti-2019.md) says what they print differently.

- **The card is read as the [2019 Passat's](volkswagen-passat-2019.md) and the
  [2019 Beetle's](volkswagen-beetle-2019.md) lists read it**, which is the
  [2018 card's reading](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read). The rows are the 2019
  Beetle list's, in its wording, without its convertible top row ("Beetle Convertible only"). So, as there:
  - **the hood latch** is left out by its own words, "Does not apply to: Passat, Golf, Beetle, and Jetta";
  - **both gearbox rows are left out**: "Transmission: 6-Speed DSG automatic transmission: Change fluid and
    filter" (every 40,000) and "Transmission, automatic (non-DSG automatic transmission): Change fluid" (every
    80,000) name no model ([#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333),
    [#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)); the 7-speed
    DSG is "Golf GTI, Jetta GLI, Golf R only";
  - left out by their own words: the Passat-only sunroof water drains, the trailer hitch, the convertible top.
    The AWD clutch fluid is left out: the 2019 Golf was sold with front-wheel drive only.
- **Spark plugs by engine.** "Spark plugs: Replace (Only Atlas, Golf SportWagen 1.4L, Golf 1.4L, GTI, Jetta
  1.4L, Jetta GLI, Tiguan)", every 80,000 miles or 6 years; the 40,000-mile or 4-year row is "Except" the same
  models. The Golf's mark names its engine, so the list has `"engine": { "label": "1.4T", "displacementL": 1.4 }`
  and spark plugs 80,000 / 72 mo. No other 2019 Golf list is made: the card's other row would cover a Golf of
  another engine, but nothing says one was sold, so a Golf with no engine on record is asked, "1.4T" or
  "Neither".
- **Model name.** vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`, read 2026-10-29), has
  "Golf", "Golf SportWagen", "Golf Alltrack", "Golf GTI", "Golf R" and "e-Golf" apart.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24
  months by time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24
  mo; front axle differential lock fluid every 3 years "(if fitted)"; panoramic sunroof rails every 40,000 or
  2 years, first at 40,000 (the card's "or 3 years" for the first time is recorded, not put into months); the
  tire filler bottle's date every 2 years "(if fitted)"; spark plugs 80,000 / 72 mo.
