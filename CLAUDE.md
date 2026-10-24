# Glovebox — notes for Claude

Private, offline-first Android app for tracking vehicle maintenance. Single user, no account, no
server. Distributed by sideloading via Obtainium from GitHub Releases, not the Play Store.
Public repo, GPL-3.0 (`LICENSE`).

Design canvas (screens, flows, light/dark, nav map):
https://claude.ai/code/artifact/7f521081-34e6-46e9-99aa-641765621ad8

**Local copy of the design: `design/`** — `design/screens/<Name>.png` (renders) and
`design/source/<Name>.dc.html` (exact colors, spacing, copy). Start from `design/README.md` and
`design/screens/NavMap.png`. Before building a screen, look at its PNG and read its source.

This file and `docs/rules/` hold the rules and the decisions behind them. How a feature works is in its code and tests;
the issue numbers lead to the discussion.

## Status

`./gradlew build` and `./gradlew :domain:test` are green (Gradle 9.8.1, AGP 9.4.1, lint included).
Keep them green; lint fails the build on a warning as on an error (`warningsAsErrors`, in `:app` and `:data`),
and there is no baseline: fix a warning, or suppress that one case with a comment saying why (`app/lint.xml`).

## Commands

- `./gradlew :domain:test` — fast pure-Kotlin tests. Run often.
- `./gradlew build` — everything, including the database migration tests (`:data`).
- `./gradlew koverXmlReportCoverage` — unit-test coverage (Kover) of `:domain`, `:data` and `:app`, merged into
  `build/reports/kover/reportCoverage.xml`; CI sends it to Codecov (`codecov.yml`: reported, never a gate). A new
  generated-code pattern is excluded in the root `build.gradle.kts`.
- `./gradlew :app:installDebug` — install on a connected phone (debug app id `io.cosgriff.glovebox.debug`).
- **Dependency verification** (#246): every jar, POM and plugin must match `gradle/verification-metadata.xml`
  (keys in `gradle/verification-keyring.keys`), or the build fails, locally and in CI. After changing a
  dependency or plugin by hand, run
  `./gradlew --write-verification-metadata sha256,pgp --export-keys --refresh-dependencies --no-configuration-cache build :app:assembleDebugAndroidTest :baselineprofile:assemble`
  then `uv run .github/scripts/verification-tidy.py`, and commit both files (not `verification-keyring.gpg`).
  `--refresh-dependencies` matters: a warm cache records less than CI's cold one fetches. Read the diff; it is
  the list of what is now trusted. Dependabot's PRs get the same command from `dependabot-verification.yml`.
  To prune old entries, delete both files and run it again. Tesseract4Android (JitPack) is unsigned, so only
  its checksum vouches for it: **never accept a changed checksum for a version already in the file.**
  The root `build.gradle.kts` holds Bouncy Castle and httpclient on the build's and lint's classpaths above what
  AGP asks for (#273, advisories in the build tools, not the APK); drop those constraints once AGP itself moves on.
- **On-device tests** (`app/src/androidTest`): `TextReaderTest` and `EndToEndTest` (first launch through the
  maker's schedule, "Log it", a mileage update, a backup and its restore). `EndToEndTest` finds things by the
  words on screen, so change it with the wording; it turns airplane mode on and **skips itself unless the
  welcome screen shows**, because it ends by replacing everything. CI runs both (`end-to-end` in `ci.yml`).
  Locally, only on a throwaway emulator (the `Glovebox_E2E` AVD), never the one with test data:
  `./gradlew :app:installDebug :app:installDebugAndroidTest`, then
  `adb shell am instrument -w io.cosgriff.glovebox.debug.test/androidx.test.runner.AndroidJUnitRunner`.
  **Don't use `connectedDebugAndroidTest`: it uninstalls the debug app when it finishes, wiping its data.**
- **Baseline profile** (#222): `./gradlew :app:generateReleaseBaselineProfile` runs the journeys in
  `:baselineprofile` on a Gradle managed emulator (`pixel6Api34`, never one with data on it) and writes
  `app/src/release/generated/baselineProfiles/`; commit those. Regenerate when screens change a lot, not on
  every build. The journeys find things by their words, like `EndToEndTest`.
  `./gradlew :baselineprofile:pixel6Api34BenchmarkReleaseAndroidTest` compares cold start and the vehicle-home
  scroll with and without it. For another device: a fresh AVD, `-Pglovebox.profileOn=connected` and
  `ANDROID_SERIAL` (task `connectedBenchmarkReleaseAndroidTest`); the committed profile came from a Pixel 10
  Pro AVD, API 37.
- **Screenshot tests** (#233, `app/src/test/.../ui/screens`): Roborazzi under Robolectric draws the main screens
  in light and dark from made-up `Fixtures` and compares them with `app/src/test/screenshots/`, so `build` fails
  on any change. After a change meant to alter a screen, run `./gradlew :app:recordRoborazziDebug`, look at every
  image it changed, and commit them. A screen (`shoot`) must match exactly; a sheet or dialog (`shootScreen`) may
  be one step in 255 out, since a Mac and CI's Linux blend translucent windows differently. **A screen is tested
  through its `…Content` composable** (plain state and callbacks); the `…Screen` around it reads the ViewModel and
  holds sheets and dialogs. Keep new screens that way and add them to `ScreensScreenshotTest`. A `…Content` that
  needs today takes it as a parameter (`Fixtures.TODAY`), never `LocalDate.now()`. Fixtures are made up: VINs
  that pass the check digit and belong to no one, invented records, no photos, no real torque figures.
- **Accessibility checks** (#234) run in the same tests (Google's ATF, latest preset, every window, light and
  dark, warnings fail): touch targets under 48dp, contrast under 4.5:1, missing labels, clickables that read the
  same. **A new screen test gets them by using `shoot` or `shootScreen`.** A deliberate finding is suppressed in
  `Screenshots.kt` with `allowed(check, …)` and a comment; the only one is contrast on a disabled control. Only
  what is on screen is checked. A bottom sheet is a `GloveboxSheet` (`ui/components/Sheets.kt`), whose
  `SheetHandle` is Material's handle in a 48dp slot; a sheet built another way passes
  `dragHandle = { SheetHandle() }` itself.
- Release: merge the PR `release-please` keeps open (version and changelog come from Conventional Commits);
  it tags `vX.Y.Z` and starts `release.yml`, which builds the signed APK after approval. See README.

## Modules

- `:domain` — pure Kotlin, **no Android imports**. All business rules live here and get unit tests.
- `:data` — Room 3 (`androidx.room3`, #220) + SQLCipher (`SQLCipherDriver`; code that goes around the DAOs uses
  `useReaderConnection` / `useWriterConnection`, never `openHelper`), Keystore keys (`DatabaseKeyManager`), Tink
  file encryption (`PhotoCrypto`), `NhtsaClient`, `RecallRepository`, `SettingsRepository` (DataStore),
  `DataWipe`, Hilt module.
- `:app` — Compose UI, navigation, ViewModels, `AppLock` + `LockScreen`, theme.
- `:baselineprofile` — generates the baseline profile and benchmarks startup. Nothing in it ships.

## Decisions — don't change without asking

- **Encryption:** DB passphrase is random, wrapped by an AES-GCM Android Keystore key with
  `setUserAuthenticationRequired(false)` and NOT `setUnlockedDeviceRequired`, because WorkManager must open
  the DB while the phone is locked for reminders and recall checks. Biometrics gate the UI only.
- **No Android auto-backup** (`allowBackup=false`, data extraction rules exclude all). The app's own
  password-encrypted backup file is the only migration path.
- **Delete all data = crypto-erase:** destroy Keystore keys first, then files. Delete vehicle relies on
  `ON DELETE CASCADE` from `vehicles` plus deleting that vehicle's files.
- **"Last done" is never stored.** It's derived from service records (`ServiceLog.lastDone`), so editing
  or deleting a record rolls reminders back. Don't add a cached last-done column. A "last done" the user
  gives on its own is a service record flagged `enteredAsLastDone` (#78). **Nothing is assumed or estimated
  for the user:** there is no "assume everything was done on schedule".
- **Torque stored in N·m**, converted for display.
- **Distances are stored in the vehicle's own unit, as typed** (#93, `vehicles.distanceUnit`). Every reading,
  interval, snooze point, cluster offset and milestone is in that unit exactly as typed, so a figure always
  reads back as typed (whole miles can't hold kilometres, so it isn't a display setting). Column names that
  say "miles" are older than the choice. **Never write a number into a vehicle's tables in another unit, and
  never show one without the vehicle's unit:** there is deliberately no unit-less formatter
  (`Int.asDistance(unit)`, `Interval.text(unit)`, `VehicleEntity.unit`).
- **Network:** only NHTSA (`vpic.nhtsa.dot.gov`, `api.nhtsa.gov`), sent only a VIN or year/make/model. No
  analytics, crash reporting, ads, or other SDKs that phone home. CI and `release.yml` scan the release APK
  with Exodus Privacy (`.github/scripts/exodus-scan.sh`, #217): a tracker fails, and so does a permission not
  in `.github/privacy/permissions.txt` (or one listed that the APK no longer asks for); **a new permission
  means updating that list and the Privacy screen's "Permissions" lines together.** GitHub's dependency
  review (`dependencies.yml`, #246) fails a PR adding a vulnerable dependency or a licence outside
  `.github/dependency-review-config.yml`'s GPL-compatible list; one GitHub can't name is added there by name,
  with its text in `assets/licences/`.
- **Barcodes are read with zxing-cpp, not ML Kit** (#161). ML Kit pulls in Google's log-upload transport and
  Firebase/Play pieces, which breaks the network rule; asked again on 2026-10-15 and still no. zxing-cpp's POM
  needs only Kotlin and CameraX and its `.so` imports no network function (checked with pyelftools; **check
  again on an upgrade**). Java ZXing is test-only, to draw barcodes. Check any Google or Firebase library's
  dependency tree the same way before adding it.
- **Text is read with Tesseract, not ML Kit** (#64), for the same reason; its `.so` files were checked too
  (check again on an upgrade). It is only on JitPack, so `settings.gradle.kts` lets JitPack serve that one
  group (`exclusiveContent`). **Don't widen that filter or add another JitPack dependency without asking.**
- **The release APK is 64-bit ARM only** (`abiFilters`, #123): 22 MB where four ABIs made 49. One APK per
  release, as `release.yml` and Obtainium expect: **don't add an ABI back or split the APK without asking.**
  Debug builds carry every ABI; `nonMinifiedRelease` and `benchmarkRelease` also carry x86_64 for emulators
  (`finalizeDsl`) and are never released.
- **NHTSA recalls are by year/make/model**; the public API can't confirm repair status for a VIN. The user
  marks recalls done, and the app links to the maker's VIN lookup.
- **applicationId `io.cosgriff.glovebox` must never change. The release signing key must never change.** The
  package is registered for Android developer verification with that key (#267, 2026-10-17), so a new key
  would mean registering again. The ownership-challenge APK (`.github/scripts/registration-apk.sh`, with the
  git-ignored `assets/adi-registration.properties`) is built locally and uploaded to the console only, never
  published; releases carry nothing for verification.
- **IDE is Android Studio.** Keep AGP current (Studio's Upgrade Assistant); no IntelliJ IDEA constraint.
- minSdk 29, targetSdk 37, compileSdk 37, JDK 17, Kotlin + Compose + Material 3, Hilt (KSP), Navigation 3
  (#237) with `@Serializable` routes that are `NavKey`s. Dynamic color is off; use the theme tokens.

## Design tokens (from the canvas)

Light: ground `#F4F3EF`, surface `#FFFFFF`, ink `#17191C`, muted `#5A5F66`, line `#DEDCD5`,
accent `#C2410C` (on-accent white), accent-soft `#FBE9DF`/`#9A3412`, ok `#0F766E`/`#DDEFEC`,
danger `#B42318`. Dark: ground `#121416`, surface `#1C1F23`, ink `#EDEBE6`, muted `#A5A9AF`,
accent `#F08A4B` (on-accent `#17191C`), accent-soft `#3A2418`/`#FDBA74`, ok `#5EEAD4`, danger `#F97066`.

- Light mode shows importance with tints (soft orange = needs attention, solid orange = most urgent), not
  black panels. In dark the urgent pill is solid (`onPrimaryContainer` behind dark text).
- Fonts: IBM Plex Sans (through `MaterialTheme.typography`) and IBM Plex Mono (`PlexMono`, by name, for
  mileage, VINs, torque and other figures), bundled in `res/font`. **Never use `FontFamily.Monospace`.** The one
  exception is the home-screen widget, which Glance draws in the system font. The fonts' OFL licence is listed
  by hand in `LicenceCatalog`.
- Accent text on the soft-orange tint is `onPrimaryContainer`, not `primary` (4.4:1 in light, below 4.5:1);
  `primary` there is fine for icons and large figures. Colours Material doesn't name come from
  `statusColors()`. Every `surfaceContainer…` role is set in `Theme.kt`; one left out falls back to lavender.
- Corners come from `MaterialTheme.shapes` (`GloveboxShapes`): 12dp `small` / `extraSmall` for fields, buttons,
  menus and snackbars, 16 `medium` for cards, 20 `large` for the garage's vehicle cards. Type a corner out
  only for something of its own (a 6dp tag, the camera screens).
- Touch targets ≥ 48dp. Destructive actions are red and confirm by typing (vehicle name / DELETE).

## Screens

Every board on the canvas is built. Still to build: VIN scan follow-up (#83).

## Rules by feature

**Before changing a feature, read its rules file.** Each rule there binds as firmly as one in this file.

- **Vehicles** (adding by VIN or by hand, VIN scanning, vehicle details, archiving, deleting, the odometer offset, distance units): `docs/rules/vehicles.md`
- **Schedule and reminders** (setup, maker's schedules, drive and gearbox variants, Severe and E85, statuses, snoozes): `docs/rules/schedule.md`
- **Service log** (Log service, Undo, record files, History's list, timeline and spending, custom lines and rules, guesses, visits): `docs/rules/service-log.md`
- **Mileage** (Update mileage, the readings list and chart, Read dash): `docs/rules/mileage.md`
- **Import** (the import format and schemas, schedule and history imports, the Carfax PDF, writing import files): `docs/rules/import.md`
- **Milestones** (the milestones view and the user's own table): `docs/rules/milestones.md`
- **What it takes, torque and maker's lists** (What it takes, torque values, bundled maker's lists, maker's document links): `docs/rules/takes-and-torque.md`
- **Recalls** (NHTSA checks, marking recalls done): `docs/rules/recalls.md`
- **Documents and photos** (encrypted files, pictures, the in-app PDF viewer, pickers): `docs/rules/documents.md`
- **Export and backup** (exports, the PDF report, backup, restore): `docs/rules/export-and-backup.md`
- **Lock, navigation and app shell** (the lock, navigation, shortcuts, the widget, notifications, the welcome, Delete all data, Settings, the Privacy screen, licences): `docs/rules/app-shell.md`
- **Database** (schema changes and migrations): `docs/rules/database.md`

## Conventions

- Business logic goes in `:domain` with a test; ViewModels stay thin.
- **A composable or helper used by more than one screen lives in `ui/components/`**, not in the screen that
  first needed it (#297). Look there first (`ScreenHeader`, the cards, `GloveboxSheet`, `OdometerField`, the
  buttons, rows, pills and dialogs). One used by a single feature stays in that feature's package.
- **All wording lives in `res/values/strings.xml`** (#236). Composables use `stringResource` /
  `pluralStringResource`; other code uses `LocalResources` or `AppText.res` (never
  `LocalContext.current.getString` in a composable). Figures go in already carrying their unit, counts through a
  plural. Names are `<screen>_<what>`. `HardcodedTextTest` fails on text typed into the UI.
- **A rule that is a fixed pattern in the source goes in `SourceRulesTest`** (`FontFamily.Monospace`,
  `String.format` and display `Locale.US` in `:app`, `entry<Route>`, `openHelper`, `LocalContext.current.getString`),
  so the build catches it; judgement calls stay with review.
- **`:domain` returns no English:** a typed result that `app` words (`…Text.kt` files). Things named by id
  (catalog lines, typical items) are named in `app`. A writer for a whole document stays in `:domain` and is
  handed its words (`ExportWords`). Text from outside is passed through as it comes. **A change to the English
  is its own PR**, since `EndToEndTest` finds things by their words. The one exception: import error messages
  stay in English in `:domain`, with the format they describe.
- **The app runs in its translation's locale, not the phone's** (`format_locale`, `AppText`). **Never format a
  figure or date with `Locale.US` or `String.format` for display, and never build a phrase by cutting up
  another**; give it its own resource. Use the shared formatters in `Mappers.kt`. Typed numbers go through
  `typedNumber()` / `typedDecimal()` before a parser. Costs are dollars (`Money.format`).
- Adding a language is a `res/values-xx/strings.xml` with its own `format_locale`.
- Never show placeholder data as if real; mark sample values clearly.
- Reference values are the user's to verify; the UI says "check your service manual".
- Any Python helper scripts: use `uv run` with inline script dependencies.
- Public repo: never commit real personal data (VINs, records, photos, reports), keystores,
  `local.properties` or `.idea/`. Test fixtures use made-up values.

## Agent skills

### Issue tracker

GitHub Issues on `mcosgriff/glovebox`, via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Five default triage roles. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root, created lazily. See `docs/agents/domain.md`.
