# Security policy

## Reporting a vulnerability

Please report it privately through GitHub:
[Report a vulnerability](https://github.com/mcosgriff/glovebox/security/advisories/new) (the
"Security" tab, then "Report a vulnerability"). Don't open a public issue for it.

Glovebox is maintained by one person in their own time, so there is no fixed response time; a report
will be acknowledged as soon as it is read, and a fix goes out as a normal release, which Obtainium
picks up.

## What counts

Anything that breaks what the README's [Security model](README.md#security-model) promises, for
example:

- data leaving the phone other than a VIN or year/make/model sent to NHTSA;
- the database, photos, documents or a backup file being readable without their key or password;
- "Delete all data" leaving anything recoverable;
- the app lock showing a screen, sheet or notification's contents before unlocking;
- a release APK that wasn't built from this repo, or a dependency that isn't the one recorded in
  `gradle/verification-metadata.xml`.

The app lock guards the screens only, by design: someone who can unlock the phone itself is not kept
out of the data by it. Reminders and recall checks run while the phone is locked, so the database key
is deliberately usable then.

## Supported versions

Only the latest release. Updates come through the same GitHub Releases page.
