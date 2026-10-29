# Volkswagen Passat, 2019 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2019.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

Source: Volkswagen's 2019 "Maintenance Schedule" card for all its models except the e-Golf, filed with NHTSA
as a manufacturer communication: the newest revision, revised 11/12/2020, `2020/MC-10183295-0001`, read in
full on 2026-10-29, as page images, by eye. The earlier revisions (`2019/MC-10163818-0001`,
`2019/MC-10168645-0001`) weren't read again; the [2019 GTI's page](volkswagen-gti-2019.md) says what they
print differently. Every Passat NMS card read, and what isn't covered, is on the
[2014 2.5L's page](volkswagen-passat-2014-2.5.md).

- **The card is read as the [2019 GTI's](volkswagen-gti-2019.md) list reads it**, which is the 2018 card's
  reading ([the Jetta's page](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read)). The rows are the
  2019 GTI list's, in its wording, with these differences:
  - "Sunroof water drains: Check for clearance and clean if necessary (Passat only)", in the Minor and the
    Standard service, so in every column: "Check and clean sunroof water drains", table-only, as on the
    [2018 Passat](volkswagen-passat-2018-2.0t.md).
  - **Spark plugs every 40,000 miles or 4 years** (40,000 / 48 months): the row "(Except Atlas, Golf
    SportWagen 1.4L, Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI, Tiguan)" doesn't except the Passat, and the 80,000
    / 6-year row is "(Only …)" those models. No row names an engine, so the list names none.
  - **Both gearbox rows are left out** (the GTI's list has the 6-speed DSG): "Transmission: 6-Speed DSG
    automatic transmission: Change fluid and filter" (every 40,000) and "Transmission, automatic (non-DSG
    automatic transmission): Change fluid" (every 80,000) name no model, and the card doesn't say which a
    Passat has ([#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333);
    asked again on #444). The 7-speed DSG is "Golf GTI, Jetta GLI, Golf R only".
- Left out by their own words: the hood latch ("Does not apply to: Passat, …"), the trailer hitch and the
  convertible top. The AWD clutch fluid is left out: the 2019 Passat was sold with front-wheel drive only.
- vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`, read 2026-10-29), names it "Passat".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24
  months by time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24
  mo; front axle differential lock fluid every 3 years "(if fitted)"; panoramic sunroof rails every 40,000 or
  2 years, first at 40,000 (the card's "or 3 years" for the first time is recorded, not put into months); the
  tire filler bottle's date every 2 years "(if fitted)"; spark plugs 40,000 / 48 mo.
