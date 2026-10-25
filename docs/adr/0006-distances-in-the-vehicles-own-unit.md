# Distances are stored in the vehicle's own unit, as typed

Each vehicle counts in miles or kilometres (`vehicles.distanceUnit`), and every reading, interval, snooze point, cluster offset and milestone is stored in that unit exactly as typed (#93). Stored figures are whole numbers, and whole miles can't hold a kilometre reading: storing in miles brought a typed reading back 1 km off. So distance is not a display setting the way torque is ([ADR-0005](0005-torque-stored-in-newton-metres.md)).

## Considered Options

- **Store miles and convert for display**, the first design (#93). Rejected for the rounding above.
- **One unit for the whole app.** Rejected so an imported vehicle can sit beside a domestic one; the Settings row is only the default for new vehicles.

## Consequences

- Never write a number into a vehicle's tables in another unit, and never show one without the vehicle's unit. There is deliberately no unit-less formatter (`Int.asDistance(unit)`, `Interval.text(unit)`).
- Changing a vehicle's unit converts its figures once, from Vehicle details.
- Column names that say "miles" are older than this decision.
