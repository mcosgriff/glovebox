# Volkswagen Golf SportWagen TDI, 2015 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-sportwagen-2015-tdi.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: the "Golf and Golf SportWagen" 2.0L TDI card (posted December 2014, `2014/MC-10242448-9999`), read on
2026-10-29, and Volkswagen's 2015 reissue for the oil change's time limit only. The card names both cars, so
this list is the [2015 Golf TDI](volkswagen-golf-2015-tdi.md)'s, job for job, and that page says how it was
read: the 130,000 column left out, with its AdBlue check, and the timing belt an `additional` reminder every
130,000 miles (the owner's ruling of 2026-10-29 on [#551](https://github.com/mcosgriff/glovebox/issues/551),
part A); the DSG row "(if applicable)", so table-only.

- `"engine": { "label": "2.0L TDI", "displacementL": 2.0 }`. No `fuel`: the 2015 Golf SportWagen's other card
  is the [1.8T's](volkswagen-golf-sportwagen-2015-1.8t.md). vPIC's 2015 model list has "Golf SportWagen" apart
  from "Golf".
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter and fuel filter 20,000
  (the dust filter or 24 mo); air filter 60,000 / 72 mo; timing belt 130,000 (apart from the table); brake
  fluid every 24 months by time alone.
