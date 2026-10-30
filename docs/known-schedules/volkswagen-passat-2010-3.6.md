# Volkswagen Passat 3.6L, 2010 (B6)

How `app/src/main/assets/known-schedules/volkswagen-passat-2010-3.6.json` was made. The rules it follows are in
[known-schedules.md](../known-schedules.md).

- **The list is the [2010 Passat 2.0T](volkswagen-passat-2010-2.0t.md)'s, row for row**, with
  `"engine": { "label": "3.6L", "displacementL": 3.6 }`. That page says what was read, how the 2010 all-model card
  is read, and why there is one list per petrol engine (the owner's ruling on
  [#556](https://github.com/mcosgriff/glovebox/issues/556#issuecomment-6511734459), question 3).
- The card's spark plug row is "(2.0T, 3.6L and 4.2L only)", so the 3.6L's plugs are the 2.0T's: 60,000 / 72 mo.
  Its other rows name no Passat engine apart.
- The Haldex clutch oil "(if applicable)" is read as on the 2.0T's list: "Change Haldex clutch oil, every 3 years (AWD)",
  in `allWheelDriveOnly`, every 36 months by time alone (the owner's ruling on [#554](https://github.com/mcosgriff/glovebox/issues/554#issuecomment-6511733883)).
