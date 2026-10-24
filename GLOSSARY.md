# Glovebox

A private, offline record of one person's vehicles: what was done to them, what they read, and what is due next.

## Language

**Vehicle log**:
Everything known about one vehicle's upkeep: its service records, its odometer readings and its tracked items, read in dash terms.
_Avoid_: history (that is the screen that lists the records), service log (that is the records alone)

**Service record**:
One visit's work on a vehicle, on a date at an odometer reading, with the lines of work done.
_Avoid_: entry, log entry

**Tracked item**:
A schedule item the user wants reminders for, with the interval and rule it counts by.
_Avoid_: reminder (that is what a tracked item produces), task

**Last done**:
The most recent service record that counts for a tracked item. Always worked out from the records, never kept on its own.
_Avoid_: last service, completed

**Cluster swap**:
The replacement of a vehicle's instrument cluster, after which the dash reads a different figure than the distance the vehicle has covered.
_Avoid_: odometer reset, odometer change

**Dash terms**:
A figure as the cluster now in the dash reads, or would have read. Readings are kept as typed; everything worked out from them is in dash terms.
_Avoid_: corrected mileage, adjusted reading

**Actual mileage**:
The distance the vehicle has really covered, whatever any cluster reads. A maker's manual counts in actual mileage.
_Avoid_: true mileage, real odometer
