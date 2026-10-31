# Volkswagen Golf TDI, 2010 (Mk6)

How `app/src/main/assets/known-schedules/volkswagen-golf-2010-tdi.json` was made. The rules it follows are
in [known-schedules.md](../known-schedules.md).

The documents read are those of the [2010 Golf 2.5L](volkswagen-golf-2010-2.5.md): the 2010 card for all
models except the Routan (`2012/MC-10156104-9999`, the same as `2012/MC-10157646-9999`), in full, from
which the list was written; Volkswagen's 2010 reissue (`USA5R503621`, printed 11/24/2025) for the oil
change's time limit only; and the 2014 Golf 2.0L TDI card (posted October 2013, `2013/MC-10155943-9999`)
as the model's own card for the "(if applicable)" rows. Read on 2026-10-28, as page images, by eye.

- **The card is read as the 2.5L's page says**, which follows the
  [Passat's](volkswagen-passat-2010-2.0t.md#how-the-2010-all-model-card-is-read). The rows that are also on the
  [2010–2014 GTI](volkswagen-gti-2010-2014.md) use that list's wording.
- **One list per engine** (the owner's ruling of 2026-10-23). This list takes the rows marked for the
  2.0L TDI and leaves out the rows marked for other engines only:
  - "Fuel filter: Replace (2.0L TDI only)" at every 20,000: the `fuel-filter` service, a reminder every
    20,000 miles.
  - "Timing belt: Replace (2.0L TDI only)" at 120,000: the `timing-belt` service, a reminder every
    120,000 miles (the grid's only multiple of it). The row "Timing belt: Replace (if applicable, except
    2.0L TDI)" at 110,000 is left out by its own mark.
  - "Diesel particulate filter … (2.0L TDI and 3.0L TDI only)" at 120,000: an inspection, so table-only
    (rule 7). Its "every 10,000 thereafter until replacement" falls past the grid's end, so it is in the
    job's name.
  - Left out: both spark plug rows (2.5L and 3.2L; 2.0T, 3.6L and 4.2L), AdBlue and both fuel filter rows
    marked 3.0L TDI.
  It has `"engine": { "label": "2.0L TDI", "displacementL": 2.0 }`. No `fuel`: the Golf had no other 2.0L
  engine in 2010 (NHTSA's vPIC model list, `GetModelsForMakeYear`, names the GTI apart from the "Golf").
  A Golf with no engine on record is asked which it has.
- **"(if applicable)" rows that the Golf's own cards don't have are left out**, as on the 2.5L's list:
  Haldex clutch oil and the tire pressure sensors. The 2014 Golf TDI card has neither.
- The air filter row ("Air filter element and snow filter", every 6 years) is the `engine-air` service,
  60,000 / 72 months, as on the 2.5L's list; the 2014 TDI card draws it in the same columns.
- Reminders: engine oil 10,000 / 12 mo; tire rotation 10,000; dust and pollen filter 20,000 / 24 mo; fuel
  filter 20,000; air filter 60,000 / 72 mo; timing belt 120,000; brake fluid every 24 months by time alone
  (the card says at 3 years first, then every 2).
- **Left out, asked on #434: both gearbox rows**, for the reason on the
  [2.5L's page](volkswagen-golf-2010-2.5.md): the automatic's fluid change names the Golf, the DSG's names
  every model, and the format can't tell the two gearboxes apart.
- Always left out: the dealer's service sticker, the New Beetle's brake fluid row, both convertible roof
  rows, and the grey "Carefree Maintenance" shading.
- **Not covered:**
  - **2011–2013**, as on the 2.5L's page (asked on #434).
  - **2014.** The Golf 2.0L TDI card (`2013/MC-10155943-9999`, filed again as `2013/MC-10157534-9999` and
    `2013/MC-10242463-9999`, byte for byte the same) has [a list of its own](volkswagen-golf-2014-tdi.md):
    its grid has 13 columns, to 130,000, and the 130,000 column has only "Timing Belt: Replace", so that
    column is left out and the belt is a reminder every 130,000 miles apart from the table
    (the owner's ruling of 2026-10-29 on [#551](https://github.com/mcosgriff/glovebox/issues/551), part A). Its other differences from 2010: the timing belt at 130,000 (not 120,000), the particulate filter checked every
    20,000 after the first 120,000 (not 10,000), brake fluid named with the clutch unit, the ribbed belt
    check "(if applicable)", and only the DSG's fluid change, marked "(if applicable)", at 40,000, 80,000
    and 120,000.
