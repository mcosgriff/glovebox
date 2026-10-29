# Volkswagen Jetta SportWagen 2.0T, 2010 (Mk5)

How `app/src/main/assets/known-schedules/volkswagen-jetta-sportwagen-2010-2.0t.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

The documents read are those of the [2010 Jetta SportWagen 2.5L](volkswagen-jetta-sportwagen-2010-2.5.md):
the 2010 card for all models except the Routan (`2012/MC-10156104-9999`, the same as
`2012/MC-10157646-9999`), in full, from which the list was written; Volkswagen's 2010 reissue
(`USA5R503621`, printed 11/24/2025) for the oil change's time limit and the engines; and the 2014 Jetta
SportWagen cards (`2013/MC-10155822-9999`, `2013/MC-10125864-9999`) for the "(if applicable)" rows. Read on
2026-10-28, as page images, by eye.

- **The card is read as the 2.5L's page says.** The rows are the 2.5L list's, with the 2.0T's spark plugs.
- **The engine.** The reissue's spark plug rows put "Jetta Wag. (AJ5 - CCTA/CBFA)" with "Golf / GTI
  (5K-2.0T)" at every 60,000 or 6 years, the card's "(2.0T, 3.6L and 4.2L only)" figure, and the rest of the
  wagon at 40,000, the 2.5L's; so the 2010 Jetta SportWagen had a 2.0T.
- **One list per engine** (the owner's ruling of 2026-10-23). This list takes "Spark plugs: Replace (2.0T,
  3.6L and 4.2L only)" at 60,000 and 120,000 (footnote 8, every 6 years): a reminder every 60,000 miles or
  72 months. It leaves out the 2.5L / 3.2L spark plugs and every row marked 2.0L or 3.0L TDI.
  It has `"engine": { "label": "2.0T", "displacementL": 2.0, "fuel": "Gasoline" }`: the
  [TDI's list](volkswagen-jetta-sportwagen-2010-tdi.md) is 2.0L too, so the two are told apart by fuel, as
  vPIC spells it. A Jetta SportWagen with no engine or no fuel on record is asked which it has.
- **"(if applicable)" rows are left out**, as on the 2.5L's list: Haldex clutch oil and the tire pressure
  sensors aren't on either 2014 Jetta SportWagen card. Those cards are for the 2.5L and the TDI; for the
  2.0T's timing belt row ("(if applicable, except 2.0L TDI)") the GTI's 2012 and 2014 cards, for the same
  2.0T, have none either, and the [GTI's list](volkswagen-gti-2010-2014.md) leaves it out on that evidence.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; air
  filter and spark plugs 60,000 / 72 mo; brake fluid every 24 months by time alone (the card says at 3 years
  first, then every 2).
- **Left out, asked on #439: the DSG fluid change**, for the reason on the
  [2010 Jetta 2.0T's page](volkswagen-jetta-2010-2.0t.md): the automatic row is marked for the 2.5L Jetta
  only and the DSG row names no model, but the reissue's gearbox table names the Jetta SportWagen ("Golf
  Variant (AJ5)") under `09G`, a conventional automatic, by model and not engine.
- Always left out: the dealer's service sticker, the New Beetle's brake fluid row, both convertible roof
  rows, and the grey "Carefree Maintenance" shading.
- **Not covered:** as on the [2.5L's page](volkswagen-jetta-sportwagen-2010-2.5.md).
