# Volkswagen Routan, 2013–2014

How `app/src/main/assets/known-schedules/volkswagen-routan-2013-2014.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md). This page also lists every card read for the Routan
(2009–2014) and says what wasn't covered and why.

Source: Volkswagen's Routan maintenance cards, which Volkswagen files with NHTSA as manufacturer communications
(`https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf`; the ids are in NHTSA's index at
`https://static.nhtsa.gov/odi/ffdd/tsbs/`). Read on 2026-10-29, as page images, by eye.

## Every Routan card read

The Routan has cards of its own: the 2010 card for every other model says "All Models except Routan".

| Model year | What was read | Document | List |
|---|---|---|---|
| 2010 | "2010 USA Volkswagen Maintenance Schedule by Miles - Routan Only" (revised November 2013), in full. Also fetched: `MC-10242441` and `MC-10212706`, the same card (same text, same marks). `MC-10157583` was not fetched | `2013/MC-10156035-9999` | [2010 3.8L](volkswagen-routan-2010-3.8.md), [2010 4.0L](volkswagen-routan-2010-4.0.md) |
| 2011 | The 2011 "Routan Only" card (revised November 2013), in full. Also fetched: `MC-10242442` and `MC-10212707`, the same card. `MC-10157622` was not fetched | `2013/MC-10156036-9999` | [2011](volkswagen-routan-2011.md) |
| 2012 | The 2012 "Routan Only" card (revised November 2013), in full. Also fetched: `MC-10242443` and `MC-10212708`, the same card. `MC-10157584` was not fetched | `2013/MC-10156003-9999` | [2012](volkswagen-routan-2012.md) |
| 2013 | "2013 Volkswagen Routan Maintenance Schedule For USA, 3.6L Gasoline Engine" (posted January 2013), in full | `2013/MC-10242445-9999` | this one |
| 2014 | The 2014 card of the same title (posted June 2013), in full; **this list was written from it**. NHTSA files it again as `2014/MC-10242475-9999`; that file has the same text and marks | `2013/MC-10125859-9999` | this one |

A script's reading of every grid (which column each mark sits in) was compared between the cards, as a pointer
only; each difference was looked at on the page. **The 2013 and 2014 cards print the same rows with the same
marks**; only the year and the posting date differ. The 2011 and 2012 cards differ in one mark (below), and the
2010 card is a different grid.

**The model.** NHTSA's vPIC model list for Volkswagen (`GetModelsForMakeYear`, read 2026-10-28) names it
"Routan" in 2010, 2013 and 2014. **No engine block** on the 2011–2014 lists: the cards are for the 3.6L, and no
other Routan engine is named on them; the 2010 card names two engines and has a list for each.

## This list

The card is Chrysler's layout in Volkswagen's frame: 15 columns, 10,000 to 150,000.

- **A block headed "at every oil change interval, as indicated by the oil change indicator system"**, drawn in
  every column: oil and filter, tire rotation ("at the first sign of irregular wear, even if before the oil
  indicator turns on"), battery and terminals, automatic transmission fluid "if equipped with dipstick", brake
  pads, shoes, rotors, drums, hoses and parking brake, cooling system protection and hoses, exhaust system, and
  the engine air cleaner "if using in dusty or off-road conditions". All are in the table at every 10,000.
- **"Additional inspections"**: CV joints at 30,000 and every 30,000 to 150,000; front suspension, tie rod ends
  and boot seals, and brake linings and parking brake, at every 20,000 to 140,000. Only the block's brake row
  carries `brake-inspect`, so the two brake rows are not one job twice in a milestone.
- **"Additional maintenance"**: engine air filter every 30,000; cabin / air conditioning filter every 20,000 (to
  140,000); spark plugs at 100,000; PCV valve inspected at 100,000; automatic transmission fluid and filter at
  120,000; the same "if using your vehicle for police, taxi, fleet, or frequent trailer towing" at 60,000;
  engine coolant "at 10 years or 150,000 miles whichever comes first", drawn at 100,000 and 150,000.
- Reminders: engine air filter 30,000; cabin air filter 20,000; spark plugs 100,000; automatic transmission
  fluid 120,000, with 60,000 as its interval under Severe. The card prints no months for any of them.
- **No oil or tire rotation reminder.** The card puts both under the oil change indicator, not a fixed
  distance (#147's survey, point 6): it draws them every 10,000, so the table does, but a 10,000-mile reminder
  would state a figure the card doesn't fix. They stay in the table only until the owner decides ([question 1 on
  #452](https://github.com/mcosgriff/glovebox/issues/452#issuecomment-6492943764)). Their names say the indicator decides.
- **The towing / fleet transmission fluid change** is not a `severeOnly` reminder: it is drawn once, at 60,000,
  and a 60,000 interval would put it at 120,000 too, where the card has only the ordinary change, so rule 7
  refuses it. It is the guide giving the ordinary job a shorter interval under those conditions, so the ordinary
  transmission fluid reminder takes 60,000 as its `severe` interval, and the 60,000 row stays in the table,
  named with its condition and with no `service` (rule 6).
- **Engine coolant is table-only**, at 100,000 and 150,000 as drawn: "10 years or 150,000" is no one interval
  that falls at both, so no reminder is the table's own. Its name gives the card's figures.
- Left out: nothing else; the card has no service sticker or footnotes.

## What isn't covered

- **2009.** NHTSA's index has no Routan maintenance card for 2009 (its 2009 Routan rows are Carefree policy
  letters), and the 2010 card isn't stretched over it.
- **The oil and rotation reminders** on this list (above); the open questions are on
  [#452](https://github.com/mcosgriff/glovebox/issues/452#issuecomment-6492943764).
