# Vehicles

Rules for adding by VIN or by hand, VIN scanning, vehicle details, archiving, deleting, the odometer offset, distance units. Read before changing any of it; review holds a diff to every line here.

- Adding by VIN (`AddVehicleViewModel`) is two steps: entry, then "Is this your vehicle?", where decoded values
  can be corrected. **A vehicle is never saved without a real year, make and model** (`VinDecode.quality`), so an
  unidentified VIN is fixed or filled in by hand. The drivetrain is picked, never preselected. The vehicle is
  saved before the schedule screen, so Back from there keeps it.
- Adding without a VIN (`ManualAddScreen`): makes and models from vPIC (`makes` sends nothing about the vehicle,
  `models` only make and year); every picker also takes typed text. A VIN added later from Vehicle details isn't
  decoded on its own; only the owner's lookup below fills anything from it.
- Looking a saved VIN up again (Vehicle details, #462) happens only on the owner's tap. **It sends the VIN alone,
  never `modelyear`:** vPIC answers with whatever year it is sent, which would hide a VIN that is another vehicle's.
  It offers only fields that are empty here (`VinRefresh.additions`), never the drivetrain (picked, as above), and
  fills them with `VehicleDao.fillFromVin`, which writes a column only while it is still null or blank. **It never
  replaces a value**, and it fills nothing when the decode's year or make differ from the vehicle's
  (`VinRefresh.sameVehicle`).
- VIN scanning (`ScanVinScreen`, `CameraFeed.kt`): barcodes through zxing-cpp, printed VINs through Tesseract.
  **A VIN is only ever a 17-character run that passes the check digit;** nothing slides along longer text and no
  substitutions are tried (either would pass a wrong VIN about one time in eleven). The only repair is I/O/Q to
  1/0. A printed VIN needs two of five frames to agree. Frames are never stored. **Frames follow the display's
  rotation; never set the target rotation from an orientation listener** (a tipped phone then reads nothing).
- Vehicle details' fields are written one column at a time (`VehicleDao.setNickname` etc.), **never with a
  whole-row `@Update`**, which could undo a recall check that landed meanwhile. Rows with nowhere to go are left
  out, not shown dead.
- A saved VIN can be corrected from Vehicle details (#468), in the dialog that adds one, with `setVin`. **It changes
  the VIN alone:** nothing is decoded or rewritten, and nothing is sent; the lookup above is the owner's next tap, if
  they want it. It is never cleared, and a wrong check digit only warns, as when one is added (`Vin.canSave`).
- The VIN is copied (long press) with `copyText`, marked sensitive. A licence plate is kept on the phone and
  never sent.
- Archiving is only `vehicles.archivedOn`; nothing is deleted. Screens that list the garage read
  `observeActive`, so a sold vehicle gets no reminders or recall checks. Its history is read-only.
- Delete vehicle (`DataWipe.deleteVehicle`) has no Undo; `RecordActions.undo` checks the vehicle still exists,
  in the same transaction as the restore (`ServiceLogWriter`).
- Odometer offset (`ClusterSwap`): **every reading stays as typed**, and everything computed is in "dash terms"
  (what the cluster now in the dash reads). **Never hand `record.odometer` or an `odometer_readings` row to
  `:domain` raw:** go through `toEntry(swap)` / `toReadings(swap)` (`data/log/VehicleLog.kt`); compare a typed reading
  with `swap.toDash`, show a past figure with `swap.asReadOn`. The swap day belongs to the new cluster.
- Distance units (#93): `UnitDao.convert` is the only thing that changes a vehicle's unit, in one transaction.
  **Two conversions** (`DistanceKind`): a measurement (reading, snooze point, offset) goes exactly at 1.609344; a
  schedule figure (interval, milestone, repeat) goes at 1.6, as manuals print them. Glovebox's own figures are
  kept in miles and given to a km vehicle as schedule figures. **Always take the lead distance through
  `Settings.leadFor(vehicle.unit)`, never `Settings.lead`.** Each vehicle in a list is worded in its own unit.
- The owner's own oil change interval (#517, `vehicles.ownOilMiles`, `ownOilMonths`) is a schedule figure in the
  vehicle's unit as typed, written with `setOwnOil` alone, and converted at 1.6 by `UnitDao.convert`; its months are not
  a distance. The "Oil change" row is there only while the schedule has an oil change (`oilChange`); its rules are in
  `schedule.md`.
