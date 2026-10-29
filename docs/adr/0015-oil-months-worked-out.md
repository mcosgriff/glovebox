# An oil change given in miles alone gets months Glovebox works out

Glovebox works out no schedule of its own ([ADR-0004](0004-last-done-is-derived.md), `docs/rules/schedule.md`). This is the one exception, for the oil change only (#517, the owner's decision of 2026-10-28).

A GTI whose oil was last changed on 2024-10-24 at 126,677 mi showed, two years later at 131,550 mi, "due in 5,127 mi or by Jun 7, 2027": a year overdue by date, shown as fine. Volkswagen's cards for several years print the oil change as 10,000 mi with no months, though the maker's own footnotes and reissued cards say "10,000 or 1 year". With no months a reminder counts miles only, and the date beside it is a pace estimate, not a time limit. The typical list and the Toyota lists have the same gap where a guide prints none. Oil ages whether or not the car is driven, so a reminder that never comes due by date is the one that does harm.

## The rule

- Only the oil change: the typical item (`templateKey` `oil`) or one of the owner's set to the `oil` line (`service`), the items `ServiceLinks.own` gives that line. A special-condition oil change has no `service` and is left alone.
- Only where the interval in force has miles and no months, **after** the Normal / Severe and E85 choice (`ActiveInterval.counted`). Months the item has, the maker's or the owner's, are never replaced, and a severe interval without months still takes the normal one's.
- Months = miles ÷ 833, rounded, at least 1 and **never more than 12** (`OilMonths`): 10,000 → 12, 7,500 → 9, 5,000 → 6. The maker's severe miles go through the same rule; there is no separate severe rate.
- A kilometre vehicle's figure is read as the schedule figure it is (`DistanceUnit.scheduledInMiles`, 16,000 km is a manual's 10,000 mi), so it gets the months its miles would. The figure itself stays as typed ([ADR-0006](0006-distances-in-the-vehicles-own-unit.md)).
- **Worked out each time reminders are read, never stored.** No column, no migration: existing vehicles get it at once, and editing the miles moves the months with them. Import and export files carry the item's own figures only.
- **The screen says the months are Glovebox's** wherever the reminder's interval is shown ("12 mo (Glovebox's: check your manual)"); `TrackedItem.monthsWorkedOut` says which, and `app` words it.

## Considered Options

- **Leave it to the owner.** The rule until now. Turned down: the reminder looked fine a year past due, and few owners would think to type months the card didn't print.
- **Fix the bundled lists only.** Done where the maker's own notes give the months (#518), but it doesn't cover the lists whose guides record none, the typical list's own items edited to miles only, or what owners typed themselves.
- **Store the months on the item.** Turned down: they would read back as the maker's or the owner's figure, wouldn't follow a change to the miles, and existing vehicles would need a migration.
- **A separate severe rate.** Turned down by the owner: the severe miles are already the maker's shorter figure.
- **The same rule for every job.** Turned down: other fluids and parts don't age on the shelf the way oil does, and the reason for the exception is oil's.

## Consequences

- An oil change in miles alone now comes due by date as well as distance, under the vehicle's rule (whichever first, by default), and in notifications, the widget, snoozes, "Skip this one" and the report alike, since all of them count through `ActiveInterval.counted`.
- The Maintenance schedule and its import and export still show and carry the item's own figures; the editor's next-due card shows the months and says whose they are.
- The owner's own oil interval (#517's last part, `OwnOil`) gives its miles the same months until the owner types months (`OilMonths.forDistance`): stored as none, worked out on reading, like the item's. Typed months are stored and stay as typed.
