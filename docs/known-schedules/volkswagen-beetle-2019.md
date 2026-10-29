# Volkswagen Beetle, 2019 (A5)

How `app/src/main/assets/known-schedules/volkswagen-beetle-2019.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Beetle card read, and what isn't covered, is on the
[2014 2.5L's page](volkswagen-beetle-2014-2.5.md).

Source: Volkswagen's 2019 "Maintenance Schedule" card for all its models except the e-Golf, filed with NHTSA
as a manufacturer communication: the newest revision, revised 11/12/2020, `2020/MC-10183295-0001`, read in
full on 2026-10-29, as page images, by eye. The earlier revisions (`2019/MC-10163818-0001`,
`2019/MC-10168645-0001`) weren't read again; the [2019 GTI's page](volkswagen-gti-2019.md) says what they
print differently.

- **The card is read as the [2019 Passat's page](volkswagen-passat-2019.md) says**, which is the 2019 GTI's and
  the 2018 card's reading. The rows are the [2019 Passat](volkswagen-passat-2019.md) list's, in its wording, but
  for two:
  - the sunroof water drains are left out ("Passat only");
  - "Convertible top: Clean and lubricate locking mechanism, and perform leak test (Beetle Convertible only)",
    in the Extended service, so at 40,000, 80,000 and 120,000: "Lubricate convertible top latch and leak test
    (Beetle Convertible)", table-only, as on the [2018 Beetle](volkswagen-beetle-2018.md).
- **Spark plugs every 40,000 miles or 4 years** (40,000 / 48 months): the row "(Except Atlas, Golf SportWagen
  1.4L, Golf 1.4L, GTI, Jetta 1.4L, Jetta GLI, Tiguan)" doesn't except the Beetle, and the 80,000 / 6-year row is
  "(Only …)" those models. No row names an engine, so the list names none.
- **Both gearbox rows are left out**: the 6-speed DSG's and the conventional automatic's rows name no model, and
  the card doesn't say which a Beetle has ([#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333)).
  The 7-speed DSG is "Golf GTI, Jetta GLI, Golf R only".
- Left out by their own words: the hood latch ("Does not apply to: Passat, Golf, Beetle, and Jetta") and the
  trailer hitch. The AWD clutch fluid is left out: the Beetle was sold with front-wheel drive only.
- vPIC's model list for Volkswagen, 2019 (`GetModelsForMakeYear`, read 2026-10-29), names it "Beetle".
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24
  months by time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24
  mo; front axle differential lock fluid every 3 years "(if fitted)"; panoramic sunroof rails every 40,000 or
  2 years, first at 40,000 (the card's "or 3 years" for the first time is recorded, not put into months); the
  tire filler bottle's date every 2 years "(if fitted)"; spark plugs 40,000 / 48 mo.
