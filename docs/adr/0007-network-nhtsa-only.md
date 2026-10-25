# The only network calls go to NHTSA, and they send only a VIN or year/make/model

The app talks only to NHTSA (`vpic.nhtsa.dot.gov` to decode VINs, `api.nhtsa.gov` for recalls), and sends only a VIN or a year/make/model. There are no analytics, crash reporting, ads, or SDKs that phone home. Glovebox is private and offline-first for a single user with no account, so anything else that leaves the phone would break the promise the app is built on.

## Considered Options

Written down after the decision, not at the time; recorded so they aren't proposed again unexamined.

- **Opt-in crash reporting.** Even opt-in, it means a crash SDK in the APK and a host besides NHTSA, which Exodus would flag and the README promises against. Bugs are reported through GitHub issues instead.
- **A server of the project's own** (sync, a schedule catalogue, bulletins). Glovebox has no account and no server by design; anything it needs from outside NHTSA ships inside the APK, like the maker's schedules.

## Consequences

- Every library is checked for network code before it is added. This is why ML Kit is out ([ADR-0008](0008-no-ml-kit.md)).
- CI and `release.yml` scan the release APK with Exodus Privacy (#217). A tracker fails the build, and so does a permission not listed in `.github/privacy/permissions.txt`. A new permission means updating that list and the Privacy screen together.
- Data NHTSA only publishes on other hosts (bulk files on `static.nhtsa.gov`, such as service bulletins) can't be fetched by the app (#459).
