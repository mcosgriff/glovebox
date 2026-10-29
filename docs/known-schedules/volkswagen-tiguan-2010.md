# Volkswagen Tiguan, 2010 (1st generation)

How `app/src/main/assets/known-schedules/volkswagen-tiguan-2010.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). Every Tiguan card read, and what isn't covered, is on the
[2013–2015 page](volkswagen-tiguan-2013-2015.md).

| Model year | What was read | Document |
|---|---|---|
| 2010 | The card for all models except the Routan (revised October 2012), in full, read again on 2026-10-29 as page images; the list was written from it | `2012/MC-10156104-9999` |
| 2010 | Volkswagen's 2010 reissue, as read for the [2010 Passat](volkswagen-passat-2010.md) (for the oil change's time limit; read again only for its spark plug and gearbox rows on page 5). Its rows name the Tiguan as `5N` | reissue `USA5R503621`, printed 11/24/2025 |

The card is read as the [2010 Passat's page](volkswagen-passat-2010.md#how-the-2010-all-model-card-is-read)
says, and the list is the 2010 Passat list's, in its wording, with these differences:

- **Left out on the evidence of the Tiguan's own cards**: the timing belt "(if applicable, except 2.0L TDI)"
  and the tire pressure sensors. None of the 2013, 2014, 2015 and 2016 Tiguan cards has either, as the
  [2010 Eos](volkswagen-eos-2010.md) and the GTI list left them out on the evidence of their own cards.
- **The Haldex row stays**, table-only: "Haldex Clutch: Change oil (if applicable)", drawn at 40,000, 80,000 and
  120,000 on this card (the Tiguan's own cards from 2013 draw it every 3 years, at 30,000, 60,000, 90,000 and
  120,000). The table takes this card's columns. Named "Change Haldex clutch oil (if applicable)", with no
  `service` and no reminder, as on the 2010 Passat list; whether it should be an all-wheel-drive reminder is
  the open question on the CC ([#446](https://github.com/mcosgriff/glovebox/issues/446#issuecomment-6490065640)).
- **No gearbox row.** The card's "Transmission: DSG (non automatic)" row isn't the Tiguan's: the reissue's
  gearbox rows (page 5) name the Tiguan only under the conventional automatics `09M` and `09P`, never under a
  DSG, and the Tiguan's 2013–2016 cards print no gearbox row. The card's automatic row names other models only
  (2.5L Jetta, New Beetle and convertible, Golf). The reissue's `09M` fluid change every 80,000 is not on the
  card from the time and is not taken (a card from the time wins, the owner's ruling of 2026-10-23).
- **Spark plugs**: the "(2.0T, 3.6L and 4.2L only)" row, 60,000 and 120,000, footnote 8 every 6 years. The
  reissue's spark plug rows (page 5) put "Tiguan (5N)" under every 60,000 miles or 6 years, and the 2.0T is the
  Tiguan's only engine, so the "(2.5L and 3.2L only)" row is left out and the list has no `engine`.
- Brake fluid: the card's footnote 6 row names the Tiguan ("Touareg, Eos, Passat models, Jetta models, GTI,
  Golf, Tiguan, and CC").
- **The model.** vPIC's 2010 model list for Volkswagen (`GetModelsForMakeYear`, read 2026-10-28) names it "Tiguan".
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; air filter
  and spark plugs 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years first,
  then every 2).
- **Oil 12 months**, from the reissue (the owner's ruling of 2026-10-28, #517). The table has none.
- Left out as not the Tiguan's, by the card's own marks: AdBlue, the diesel particulate filter, the fuel filter
  rows and the 2.0L TDI timing belt (TDI), the New Beetle's brake fluid, the 2.5L / 3.2L spark plugs, the
  automatic gearbox fluid, and both convertible roof rows. Always left out: the dealer's service sticker and
  the grey "Carefree Maintenance" shading.
