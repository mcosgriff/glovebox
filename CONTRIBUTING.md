# Contributing

Glovebox is maintained by one person; issues and pull requests are welcome. This page is for people
working on the code. The rules a change is held to live in [`CLAUDE.md`](CLAUDE.md) (Decisions,
Conventions) and, per feature, in [`docs/rules/`](docs/rules/); [`CODING_STANDARDS.md`](CODING_STANDARDS.md)
says which apply to a diff. The reasons behind the big decisions are in [`docs/adr/`](docs/adr/).

## Setup

Open the folder in Android Studio and let Gradle sync. JDK 17. The project tracks the latest Android
Gradle Plugin; upgrade it with Tools → AGP Upgrade Assistant.

## Tests

| Command | What it runs |
|---|---|
| `./gradlew :domain:test` | The business rules: pure Kotlin, fast. Run it often. |
| `./gradlew build` | Everything: unit tests, database migration tests, screenshot and accessibility tests, lint. Lint fails on a warning. |
| `./gradlew :app:recordRoborazziDebug` | Re-records the screenshots after a change meant to alter a screen. Look at every image it changed before committing them. |

**On-device tests wipe data.** `EndToEndTest` ends by restoring a backup over everything, so run it only on
a throwaway emulator:

```
./gradlew :app:installDebug :app:installDebugAndroidTest
adb shell am instrument -w io.cosgriff.glovebox.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Don't use `connectedDebugAndroidTest`: it uninstalls the debug app when it finishes, taking its data with it.

## Changing a dependency

Every jar and plugin is checked against `gradle/verification-metadata.xml`
([ADR-0011](docs/adr/0011-dependency-verification.md)), so after changing one, regenerate the file with
the command under "Dependency verification" in `CLAUDE.md`, then read the diff: it lists what is now
trusted. A library must make no network calls and carry no trackers
([ADR-0007](docs/adr/0007-network-nhtsa-only.md)); CI scans the APK with Exodus Privacy and fails on
either.

## Wording and translations

All text the app shows is in `app/src/main/res/values/strings.xml`. A new language is a
`res/values-xx/strings.xml` with its own `format_locale`. Change English wording in its own pull request,
since the end-to-end test finds things by their words.

## Pull requests

- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`fix:`, `feat:`,
  `docs:`, …); `release-please` builds the version and changelog from them.
- `./gradlew build` must be green.
- Test data is made up: no real VINs, records, photos or reports in the repo.
- Security problems go through a private report, not an issue: see [`SECURITY.md`](SECURITY.md).
