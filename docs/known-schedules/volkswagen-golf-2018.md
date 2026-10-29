# Volkswagen Golf, 2018 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-2018.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). The [Golf SportWagen](volkswagen-golf-sportwagen-2018.md) and
[Golf Alltrack](volkswagen-golf-alltrack-2018.md) lists are made from the same card. Every Mk7 card read, and
what isn't covered, is on the [2015 Golf 1.8T's page](volkswagen-golf-2015-1.8t.md).

Source: Volkswagen's 2018 "Maintenance Schedule" card for all its models except the e-Golf, filed with NHTSA
as a manufacturer communication: the newest revision, revised 11/26/2019, `2019/MC-10168646-0001`, read in
full on 2026-10-29, as page images, by eye. NHTSA's index files it under the Golf, the Golf SportWagen and the
Golf Alltrack. The earlier revisions (`2017/MC-10125867-9999`, `2017/MC-10127897-9999`) weren't read again;
the [2018 GTI's page](volkswagen-gti-2018.md#what-the-earlier-revisions-print-differently) says what they print
differently.

- **The card is read as the [2018 Jetta's page](volkswagen-jetta-2018.md#how-the-2018-all-model-card-is-read)
  says**, and the rows are the 2018 Jetta list's, in its wording. Nothing differs for the Golf:
  - **the hood latch** is left out by its own words, "Does not apply to: Passat, Golf, Beetle, and Jetta";
  - **both gearbox rows are left out**: "Transmission, 6-Speed DSG: Change fluid and filter" (every 40,000)
    and "Transmission, Automatic (non-DSG): Change fluid" (every 80,000) name no model, and the card doesn't
    say which a Golf has ([#443's question](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333),
    [#444's question 3](https://github.com/mcosgriff/glovebox/issues/444#issuecomment-6489685514)). The
    7-speed DSG is "Golf R only".
  - Left out as not the Golf's: the AWD clutch fluid ("AWD only vehicles"; the 2018 Golf was sold with
    front-wheel drive only), the 1.4L Jetta's belt checks, the 3.6L's spark plugs, the trailer hitch, the
    convertible top and the sunroof water drains (Passat only).
- **Spark plugs** every 40,000 miles or 4 years, the row for 4-cylinder gasoline "1.4L, 1.8L, and 2.0L"
  engines. No row names one of them alone, so the list names no engine.
- **Model name.** vPIC's model list for Volkswagen, 2018 (`GetModelsForMakeYear`, read 2026-10-29), has
  "Golf", "Golf SportWagen", "Golf Alltrack", "Golf GTI", "Golf R" and "e-Golf" apart.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24
  months by time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24
  mo; front axle differential lock fluid every 3 years "(if fitted)"; panoramic sunroof rails every 40,000 or
  2 years, first at 40,000 (the card's "or 3 years" for the first time is recorded, not put into months); the
  tire filler bottle's date every 2 years "(if fitted)"; spark plugs 40,000 / 48 mo.
- No tire rotation: the card has none. Severe conditions: one general sentence with no jobs or figures.
