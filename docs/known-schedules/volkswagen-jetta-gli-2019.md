# Volkswagen Jetta GLI, 2019 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-jetta-gli-2019.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 Jetta card read, the model names and what isn't covered are on
the [2019 Jetta 1.4T's page](volkswagen-jetta-2019-1.4t.md#every-mk7-jetta-card-read).

Source: Volkswagen's 2019 "Maintenance Schedule" card for all its models except the e-Golf, filed with NHTSA as a
manufacturer communication: the newest revision, revised 11/12/2020, `2020/MC-10183295-0001`, read in full on
2026-10-29, as page images, by eye.

- **The card is read as the [2019 GTI's list](volkswagen-gti-2019.md) reads it**, and the rows are that list's, in
  its wording: the three services name no model where the GLI and the GTI could differ, and every additional item
  that names one names both.
  - **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months): "Only … Jetta GLI …".
  - **The 7-speed DSG**, "Change fluid (Golf GTI, Jetta GLI, Golf R only)", every 80,000 miles: "Change DSG
    transmission fluid (DSG only)", 80,000, in `manualExcluded`. The 6-speed DSG row names no model and is left out,
    as on the GTI's list, since the newest card puts the GLI on the 7-speed row. The non-DSG automatic's row names
    no model and is left out (the GLI has a manual or the DSG).
  - **The hood latch** is left out: "Does not apply to: Passat, Golf, Beetle, and Jetta". vPIC calls the 2019 GLI
    "Jetta", and the [2018 Jetta list](volkswagen-jetta-2018.md), which covers the GLI, leaves it out by those
    words. (On the GTI's list it is left out too, asked as whether the GTI counts as a "Golf".)
  - The panoramic sunroof is a table row "(if fitted)" and an additional item, as on the GTI's list (#143).
- Left out by their own words: the Passat-only sunroof water drains, the trailer hitch, the convertible top. The AWD
  clutch fluid is left out: the Mk7 Jetta was sold with front-wheel drive only.
- **Model and engine.** vPIC has no "Jetta GLI" for 2019 and decodes the GLI's VIN pattern (`3VW6T7BU*KM`) as
  "Jetta", trim "GLI (S, 35th Anniversary Ed., Autobahn)", 2.0 litres, "2.0 TSI"; the other 2019 Jetta is the 1.4
  TSI. So the list is for "Jetta" with `"engine": { "label": "2.0T (GLI)", "displacementL": 2.0 }`, beside the
  [1.4T's](volkswagen-jetta-2019-1.4t.md): the split is by the engine's size, never by the trim.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24 months by
  time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24 mo; front axle
  differential lock fluid every 3 years "(if fitted)"; panoramic sunroof rails every 40,000 or 2 years, first at
  40,000; the tire filler bottle's date every 2 years "(if fitted)"; spark plugs 80,000 / 72 mo; 7-speed DSG fluid
  80,000 (not for a manual).
