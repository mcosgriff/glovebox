# Volkswagen Passat 2.0T, 2018 (NMS)

How `app/src/main/assets/known-schedules/volkswagen-passat-2018-2.0t.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). The [2018 3.6L](volkswagen-passat-2018-3.6.md) list is made
the same way from the same card.

Source: Volkswagen's 2018 "Maintenance Schedule" card for all its models except the e-Golf, filed with NHTSA
as a manufacturer communication: the newest revision, revised 11/26/2019, `2019/MC-10168646-0001`, read in
full on 2026-10-29, as page images, by eye. The earlier revisions (`2017/MC-10125867-9999`,
`2017/MC-10127897-9999`) weren't read again; the [2018 GTI's page](volkswagen-gti-2018.md#what-the-earlier-revisions-print-differently)
says what they print differently. Every Passat NMS card read, and what isn't covered, is on the
[2014 2.5L's page](volkswagen-passat-2014-2.5.md).

- **The card is read as the [2018 Jetta's page](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read)
  says**: the newest revision; the grid of Minor, Standard and Extended services written out, 12 columns, with
  the footnotes' months on each column; the oil change every 10,000 miles or 12 months (#517); the services'
  inspections table-only; the Additional Maintenance Items as `additional` reminders with the card's figures.
- **The rows are the [2018 Jetta](volkswagen-jetta-2018.md)'s**, in that list's wording, and one more:
  "Sunroof water drains: Check for clearance and clean if necessary (Passat only)", in the Minor and the
  Standard service, so in every column, as "Check and clean sunroof water drains". The card marks it for no
  sunroof fitting, so it isn't named "(if fitted)"; it has no service and no reminder (an inspection).
- **Left out by their own words:** the hood latch ("Does not apply to: Passat, …"), the 1.4L Jetta's belt
  checks, the 7-speed DSG (Golf R only), the trailer hitch (Tiguan, Tiguan Limited and Atlas), the convertible
  top (Beetle Convertible).
- **The AWD clutch fluid** ("AWD only vehicles") is left out: the 2018 Passat was sold with front-wheel drive
  only, as the Jetta was (point 8 of the Jetta's reading).
- **One list per engine** (the owner's ruling of 2026-10-23): the only rows that name engines are the two
  spark plug rows, "*4-cyl Gasoline: 1.4L, 1.8L, and 2.0L" every 40,000 miles or 4 years, and "*6-cyl
  Gasoline: 3.6L engines only" every 60,000 miles or 6 years. This list has
  `"engine": { "label": "2.0T", "displacementL": 2.0 }` and spark plugs every 40,000 miles or 48 months; the
  3.6L's list has 60,000 / 72. Every other row is the same in both. vPIC's model list for Volkswagen, 2018,
  names the car "Passat" (read 2026-10-29).
- **Left out, as on #443: both gearbox rows.** "Transmission, 6-Speed DSG: Change fluid and filter" (every
  40,000) and "Transmission, Automatic (non-DSG): Change fluid" (every 80,000) name no model or engine, and the
  card doesn't say which Passat engine has which gearbox, so the format can't give either row to the right
  car ([#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333)). Asked
  again on #444 for the Passat's split by engine.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24
  months by time alone (3 years after first registration first, in its name); dust and pollen filter
  40,000 / 24 mo; front axle differential lock fluid every 3 years "(if fitted)"; panoramic sunroof rails every
  40,000 or 2 years, first at 40,000 (the card's "or 3 years" for the first time is recorded, not put into
  months); the tire filler bottle's date every 2 years "(if fitted)"; spark plugs 40,000 / 48 mo.
- Severe conditions: one general sentence with no jobs or figures. No tire rotation: the card has none.
