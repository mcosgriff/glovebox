# Maker's bulletins are bundled with the app, not fetched

NHTSA holds the makers' communications to their dealers (service campaigns, warranty extensions, over-the-air updates), but has no API for them: only multi-gigabyte flat files on `static.nhtsa.gov`. `scripts/bulletins.py` filters those files, by hand, down to the free-repair kinds for the models Glovebox has maker's schedules for, and the APK carries the result (#459). The screen says how current the list is.

## Considered Options

- **Fetching them on the phone.** No API to ask, the files are hundreds of megabytes unzipped, and `static.nhtsa.gov` isn't one of the hosts [ADR-0007](0007-network-nhtsa-only.md) lets the app talk to. A bulletin's PDF opens there in the browser instead.
- **Every bulletin, or every make and model.** Repair instructions are most of NHTSA's file and rarely mean anything to an owner; every model would make the APK much bigger. Coverage grows as schedules are added.
- **Regenerating in CI on every release.** It would make a release depend on a large download and a moving input; refreshing is a reviewed commit instead.
- **NHTSA's type alone.** NHTSA typed nothing before 2024, so older free-repair programs are kept when their summary names one, and marked as found by their wording.

## Consequences

- New bulletins reach the owner only with an app update; nothing alerts them between updates.
- Adding a vehicle's schedule includes adding its NHTSA spellings to the script and refreshing the list (`docs/bulletins.md`).
- One program often appears as several documents (dealer letter, technical instructions); only exact repeats are dropped.
