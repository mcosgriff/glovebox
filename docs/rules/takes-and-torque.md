# What it takes, torque and maker's lists

Rules for What it takes, torque values, bundled maker's lists, maker's document links. Read before changing any of it; review holds a diff to every line here.

- "What it takes" (`WhatItTakes`, `TakesItem` are the one model): each editor checks entries and stores one
  standard form. Nothing is looked up or suggested from the model except from a named maker's list.
- **A bundled list is what a maker's own documents print, named and cited** (`docs/known-takes.md`,
  `docs/known-specs.md`); never a parts site, forum or compilation. **Every value it adds is tagged DOUBLE-CHECK**
  until the user checks or changes it. A list fills only empty rows (or rows that are only a name) and never
  writes over the user's; there is no replace.
- Takes variants apply only to a vehicle **known** to have that drive or gearbox (a wrong fluid is worse than an
  empty row). Several tire sizes are only offered as chips; a range or alternatives is left out.
- Torque: **Glovebox has no factory torque values and none may be invented.** "Start with common fasteners" adds
  names only. A torque list's **`basis` must be a real answer** (written permission, or the maintainer's own
  manual); "it was on a website" is not one. Don't add rows from a source that doesn't state units, and don't
  correct a figure from memory. `KnownSpecsTest` holds every list to these rules. `knownList` is permanent.
  Torque is capped at 1,000 N·m; the unit switch doesn't change the setting.
- Maker's documents open in the browser; **the app fetches nothing.** Links only to `MakerDocuments.hosts`. **The
  VIN never goes in a link and is never sent to a maker.**
