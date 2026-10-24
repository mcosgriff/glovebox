# Mileage

Rules for Update mileage, the readings list and chart, Read dash. Read before changing any of it; review holds a diff to every line here.

- Update mileage adds a reading dated today, with Undo. A reading far above the last
  (`MileageReadings.farAhead`) or lower turns Save into "Save anyway": **a question, never a block.**
- **The odometer is the highest reading on record**, so one slipped digit makes every reminder overdue; Mileage
  readings (#65) is where it is fixed. **Nothing is ever decided wrong or changed for the user:** a reading is
  only marked "check this one". The chart (#288) uses the same rows and flags as the list.
- Read dash (`DashReading`): with a last reading, **only a number from it to 200,000 past it is ever offered**;
  three of five frames must agree; **it only fills the field**, says "check it against the dash", never
  "detected", and nothing is saved until Save. Frames are never stored.
