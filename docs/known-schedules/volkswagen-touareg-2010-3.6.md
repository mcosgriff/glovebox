# Volkswagen Touareg 3.6L, 2010 (1st generation)

How `app/src/main/assets/known-schedules/volkswagen-touareg-2010-3.6.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Touareg card read, and what isn't covered, is on the
[2014–2015 3.6L page](volkswagen-touareg-2014-2015-3.6.md).

| Model year | What was read | Document |
|---|---|---|
| 2010 | The card for all models except the Routan (revised October 2012), in full, read again on 2026-10-29 as page images; the list was written from it | `2012/MC-10156104-9999` |
| 2010 | Volkswagen's 2010 reissue, as read for the [2010 Passat](volkswagen-passat-2010.md), for the oil change's time limit; its rows name the first-generation Touareg as `7L` | reissue `USA5R503621`, printed 11/24/2025 |

The card is read as the [2010 Passat's page](volkswagen-passat-2010.md#how-the-2010-all-model-card-is-read)
says, and the list is the 2010 Passat list's, in its wording, with these differences:

- **Left out on the evidence of the Touareg's own cards**: the timing belt "(if applicable, except 2.0L TDI)".
  None of the Touareg's own cards (the 2010 TDI card and the 2014, 2015 and 2016 3.6L, TDI and Hybrid cards)
  has a timing belt row, as the [2010 Tiguan](volkswagen-tiguan-2010.md) left it out on its own cards' evidence.
- **The tire pressure sensors stay "(if applicable)"**, table-only, as on the Passat list: the 2010 TDI card
  has them for the TDI, but the 2014–2016 3.6L cards have no such row, so nothing shows they are every 3.6L's.
- **The Haldex row**, "Haldex Clutch: Change oil (if applicable)", drawn at 40,000, 80,000 and 120,000, is
  read as the [CC 3.6L](volkswagen-cc-2014-2015-3.6.md) reads it, by the owner's ruling on [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883) (part A, confirmed for every list in [the second round](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511795038)): named "Change Haldex clutch oil, every 3 years (AWD)", in
  `allWheelDriveOnly`, and a reminder every 36 months by time alone, with no `service` (no catalog line names the
  job). This card prints no time for the row; the 3 years is the ruling's. The table keeps the card's columns. A
  vehicle known to drive two wheels loses it. Every Touareg has 4Motion.
- **No gearbox row.** The card's "Transmission: DSG (non automatic)" row isn't the Touareg's: the reissue's
  gearbox rows (page 5) name the Touareg only under the conventional automatic `0C8`, never under a DSG. The
  card's automatic row names other models only (2.5L Jetta, New Beetle and convertible, Golf). The reissue's
  `0C8` fluid change every 80,000 is not on the card from the time and is not taken (a card from the time wins,
  the owner's ruling of 2026-10-23).
- **Spark plugs**: the "(2.0T, 3.6L and 4.2L only)" row, 60,000 and 120,000, footnote 8 every 6 years. The
  reissue's spark plug rows (page 5) put the Touareg under every 60,000 miles or 6 years.
- **One engine.** `"engine": { "label": "3.6L", "displacementL": 3.6 }`, so the 2010 Touareg TDI gets
  [its own list](volkswagen-touareg-2010-tdi.md) and not this one. A 2010 Touareg with the 4.2L V8, if there
  was one, matches neither (see the [3.6L page](volkswagen-touareg-2014-2015-3.6.md#what-isnt-covered)).
- Brake fluid: the card's footnote 6 row names the Touareg ("Touareg, Eos, Passat models, Jetta models, GTI,
  Golf, Tiguan, and CC").
- **The model.** vPIC's 2010 model list for Volkswagen (`GetModelsForMakeYear`, read 2026-10-28) names it "Touareg".
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; air filter
  and spark plugs 60,000 / 72 mo; Haldex clutch oil every 36 months by time alone (AWD); brake fluid every 24
  months by time alone (the card says at 3 years first, then every 2).
- **Oil 12 months**, from the reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out as not the 3.6L's, by the card's own marks: AdBlue, the diesel particulate filter, the fuel filter
  rows and the 2.0L TDI timing belt (TDI), the New Beetle's brake fluid, the 2.5L / 3.2L spark plugs, the
  automatic gearbox fluid, and both convertible roof rows. Always left out: the dealer's service sticker and
  the grey "Carefree Maintenance" shading.
