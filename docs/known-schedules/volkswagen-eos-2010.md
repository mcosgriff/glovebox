# Volkswagen Eos, 2010

How `app/src/main/assets/known-schedules/volkswagen-eos-2010.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Eos card read, and what isn't covered, is on the
[2014–2015 page](volkswagen-eos-2014-2015.md).

| Model year | What was read | Document |
|---|---|---|
| 2010 | The card for all models except the Routan (revised October 2012), in full, read again on 2026-10-29 as page images; the list was written from it | `2012/MC-10156104-9999` |
| 2010 | Volkswagen's 2010 reissue, as read for the [2010 Passat](volkswagen-passat-2010.md) (for the oil change's time limit; not read again apart from its spark plug and gearbox rows on page 5). Its model codes include the Eos (`1F7`) | reissue `USA5R503621`, printed 11/24/2025 |

The card is read as the [2010 Passat's page](volkswagen-passat-2010.md#how-the-2010-all-model-card-is-read)
says, and the list is the 2010 Passat list's, in its wording, with these differences:

- **The Eos's own rows**, both table-only (no catalog line names them):
  - "Convertible roof, CSC roof, and retractable hard top: Check for function and damage, clean and lubricate
    seals (if applicable)" at 40,000, 80,000 and 120,000: "Check convertible roof for function and damage; clean
    and lubricate seals", as on the [2014–2015 list](volkswagen-eos-2014-2015.md).
  - "Convertible roof: Check latch (if applicable) (Every 2 years regardless of mileage)", footnote 2, at
    20,000, 40,000, 60,000, 80,000, 100,000 and 120,000: "Check convertible roof latch (every 2 years)".
- **The DSG row is in**: "Transmission: DSG (non automatic): Change fluid and filter" at 40,000, 80,000 and
  120,000 is `trans-fluid`, a reminder every 40,000 miles, named "(DSG only)", in `manualExcluded`, as on the
  [2010–2014 GTI](volkswagen-gti-2010-2014.md). The Eos has no conventional automatic beside its DSG: the
  reissue's gearbox rows (page 5) name the Eos only under `02E (DSG)`, every 40,000, and its 2014–2016 cards
  print only the DSG's row. So the Passat's open question ([#443](https://github.com/mcosgriff/glovebox/issues/443#issuecomment-6488582333))
  doesn't arise here.
- **Left out on the evidence of the Eos's own cards**: Haldex clutch oil, the timing belt "(if applicable,
  except 2.0L TDI)" and the tire pressure sensors. The 2014, 2015 and 2016 Eos cards have none of the three,
  as the GTI list left them out on the evidence of its 2012 and 2014 cards.
- **Spark plugs**: the "(2.0T, 3.6L and 4.2L only)" row, 60,000 and 120,000, footnote 8 every 6 years. The
  reissue's spark plug rows (page 5) put the "EOS" under every 60,000 miles or 6 years and name no other engine
  for it, so the "(2.5L and 3.2L only)" row is left out and the list has no `engine`.
- Brake fluid: the card's footnote 6 row names the Eos ("Touareg, Eos, Passat models, …").
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; DSG fluid
  and filter 40,000 (not for a manual); air filter and spark plugs 60,000 / 72 mo; brake fluid every 24 months
  by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from the reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out as not the Eos's, by the card's own marks: AdBlue, the diesel particulate filter, the fuel filter
  rows and the 2.0L TDI timing belt (TDI), the New Beetle's brake fluid, the 2.5L / 3.2L spark plugs, and the
  automatic gearbox fluid (2.5L Jetta, New Beetle and convertible, Golf). Always left out: the dealer's service
  sticker and the grey "Carefree Maintenance" shading.
