# Volkswagen Jetta GLI, 2022 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-jetta-gli-2022.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md). Every Mk7 Jetta card read, the model names and what isn't covered are on
the [2019 Jetta 1.4T's page](volkswagen-jetta-2019-1.4t.md#every-mk7-jetta-card-read).

Source: Volkswagen's "2022 VW Maintenance Schedule - All Model Gasoline Vehicles", USA, filed with NHTSA as a
manufacturer communication: the newest revision, revised 7/21/2021, `2021/MC-10199495-0001`, read in full on
2026-10-29, as page images, by eye. The reissue (`USA5R507921`, 12/03/2025) isn't used: the card from the time wins.

- **The list is the [2022 GTI's](volkswagen-gti-2022.md)**, read the same way, in its wording: every additional item
  that names the GTI names the "Jetta GLI" too, and the three service levels name no model.
  - **Spark plugs every 80,000 miles or 6 years** (80,000 / 72 months): "Only: … Golf GTI, Jetta, Jetta GLI, …".
  - **The 7-speed DSG**, "Every 80,000 miles. Only: Golf R, Golf GTI, Jetta GLI": "Change DSG transmission fluid (DSG
    only)", 80,000, in `manualExcluded`. The non-DSG automatic's row names no model and is left out (the GLI has a
    manual or the DSG).
- **No months on the columns**; the oil reminder is 10,000 or 12 months (#517).
- Left out, as on the GTI's list: the AWD clutch fluid and the front axle differential lock fluid ("(if equipped)",
  no table to stand in; the GLI is front-wheel drive), and the panorama sunroof test and the sunroof drains check
  (inspections, rule 7).
- **Model name.** vPIC's model list for Volkswagen, 2022 (`GetModelsForMakeYear`, read 2026-10-29), names it "Jetta
  GLI", apart from the "Jetta". One engine, the 2.0T, so the list names none.
- Reminders: engine oil 10,000 / 12 mo; additional items: air filter 60,000 / 72 mo; brake fluid every 24 months by
  time alone (3 years after registration first, in its name); dust and pollen filter 40,000 / 24 mo; spark plugs
  80,000 / 72 mo; 7-speed DSG fluid 80,000 (not for a manual).
