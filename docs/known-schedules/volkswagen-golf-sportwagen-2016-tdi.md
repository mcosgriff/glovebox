# Volkswagen Golf SportWagen TDI, 2016 (Mk7)

How `app/src/main/assets/known-schedules/volkswagen-golf-sportwagen-2016-tdi.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: the "Golf and Golf SportWagen" 2.0L TDI card (posted August 2015, `2015/MC-10123074-9999`), read on
2026-10-29, and Volkswagen's 2016 reissue for the oil change's time limit only. The card names both cars, so
this list is the [2016 Golf TDI](volkswagen-golf-2016-tdi.md)'s, job for job, and that page says how it was
read: the 130,000 column left out, with its AdBlue check, and the timing belt an `additional` reminder every
130,000 miles (the owner's ruling of 2026-10-29 on [#551](https://github.com/mcosgriff/glovebox/issues/551),
part A); the DSG row "(if applicable)", so table-only.

- `"engine": { "label": "2.0L TDI", "displacementL": 2.0 }`. No `fuel`: the 2016 Golf SportWagen's other card
  is the [1.8T's](volkswagen-golf-sportwagen-2016-1.8t.md).
- Reminders: engine oil 10,000 / 12 mo; dust and pollen filter 40,000 / 24 mo; fuel filter 20,000; air
  filter 60,000 / 72 mo; timing belt 130,000 (apart from the table); brake fluid every 24 months by time
  alone.
