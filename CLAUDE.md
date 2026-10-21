# Glovebox — notes for Claude

Private, offline-first Android app for tracking vehicle maintenance. Single user, no account, no
server. Distributed by sideloading via Obtainium from GitHub Releases, not the Play Store.
Public repo, GPL-3.0 (`LICENSE`).

Design canvas (screens, flows, light/dark, nav map):
https://claude.ai/code/artifact/7f521081-34e6-46e9-99aa-641765621ad8

**Local copy of the design: `design/`** — `design/screens/<Name>.png` (renders) and
`design/source/<Name>.dc.html` (exact colors, spacing, copy). Start from `design/README.md` and
`design/screens/NavMap.png`. Before building a screen, look at its PNG and read its source.

## Status

`./gradlew build` and `./gradlew :domain:test` are green (Gradle 9.8.1, AGP 9.4.1, lint included).
Keep them green; lint fails the build on a warning as on an error (`warningsAsErrors`, in `:app` and `:data`),
and there is no baseline: fix a warning, or suppress that one case with a comment saying why (`app/lint.xml`).

## Commands

- `./gradlew :domain:test` — fast pure-Kotlin tests (reminder engine, torque, VIN). Run often.
- `./gradlew build` — everything, including the database migration tests (`:data`).
- `./gradlew :app:installDebug` — install on a connected phone (debug app id `io.cosgriff.glovebox.debug`).
- **Dependency verification** (#246): every jar, POM and plugin Gradle downloads must match
  `gradle/verification-metadata.xml` (a SHA-256 each, or a trusted PGP key for the group; keys in
  `gradle/verification-keyring.keys`), or the build fails, locally and in CI. After changing a dependency
  or plugin by hand, run
  `./gradlew --write-verification-metadata sha256,pgp --export-keys --refresh-dependencies --no-configuration-cache build :app:assembleDebugAndroidTest :baselineprofile:assemble`
  then `uv run .github/scripts/verification-tidy.py`, and commit both files (not `verification-keyring.gpg`,
  which is ignored). The script drops a key listed as ignored (a key server didn't answer on an earlier
  run) that this run trusts; Gradle keeps both, the ignored one wins, and what it signs fails. `--refresh-dependencies` matters:
  with a warm cache Gradle records less than CI's cold one fetches (a BOM's `.pom` beside its `.module`). It adds what the change
  resolves to and keeps old entries; read the diff, since it is the list of what is now trusted. Those
  tasks cover every configuration CI and `release.yml` use. Dependabot's Gradle PRs get the same command
  from `dependabot-verification.yml`, which commits the result to the PR and starts CI on it. Old entries
  pile up: to prune, delete both files and run the command again. Tesseract4Android (JitPack) has no
  signature, so only its checksum vouches for it; **never accept a changed checksum for a version that
  was already in the file.**
- On-device tests (`app/src/androidTest`): the text reader (`TextReaderTest`) and **`EndToEndTest`**, the app
  driven with UI Automator from first launch through the maker's schedule, "Log it", a mileage update, an
  encrypted backup and its restore. It finds things only by the words on screen, turns airplane mode on
  so nothing calls NHTSA, and **skips itself unless the welcome screen shows**, because it ends by
  replacing everything with a backup. A change to wording it looks for breaks it, which is the point:
  change the test with the screen. CI runs both on a fresh emulator (the `end-to-end` job in `ci.yml`).
  Locally, run them only on a throwaway emulator (the `Glovebox_E2E` AVD), never the one with test data.
  Not part of `build`. Run them with
  `./gradlew :app:installDebug :app:installDebugAndroidTest` and then
  `adb shell am instrument -w io.cosgriff.glovebox.debug.test/androidx.test.runner.AndroidJUnitRunner`,
  which leaves the debug app and its data alone. **Don't use `connectedDebugAndroidTest`: it uninstalls
  the debug app when it finishes, which wipes that app's data on the emulator or phone.**
- Baseline profile (#222): `./gradlew :app:generateReleaseBaselineProfile` drives the app on a Gradle managed
  emulator (`pixel6Api34`, made and wiped by Gradle, never an emulator with data on it) through the journeys in
  `:baselineprofile` (`BaselineProfileGenerator`: welcome, add a vehicle by hand, vehicle home, Reminders, Log
  service, History, Maintenance schedule, Milestones) and writes `app/src/release/generated/baselineProfiles/`.
  Commit those files. Regenerate by hand when screens change a lot, not on every build; a stale profile only
  covers less. The journeys find things by the words on screen, so a wording change can break them as it
  breaks `EndToEndTest`. `./gradlew :baselineprofile:pixel6Api34BenchmarkReleaseAndroidTest` measures cold
  start and the vehicle-home scroll with and without the profile (`StartupBenchmark`; emulator figures, to
  compare with each other). The first run downloads the emulator image. For a newer phone, boot a fresh AVD
  and add `-Pglovebox.profileOn=connected` with `ANDROID_SERIAL` set to it (the benchmark task is then
  `connectedBenchmarkReleaseAndroidTest`); the committed profile came from a Pixel 10 Pro AVD, API 37.
  The benchmark asks vehicle home for "fully drawn" (`ReportDrawn`), so it times the start to the reminders. The release APK carries it as
  `assets/dexopt/baseline.prof`, and `profileinstaller` installs it on first launch, sideloaded or not.
- **Screenshot tests** (#233, `app/src/test/.../ui/screens`): Roborazzi under Robolectric draws the main screens
  in light and dark from made-up `Fixtures` (no database, no ViewModel) and compares them with the references in
  `app/src/test/screenshots/`, pixel for pixel. They run with the unit tests, so `./gradlew build` and CI fail
  on any change (`roborazzi.test.verify=true` in `gradle.properties`); a failure leaves each screen as drawn
  now and its difference in `app/build/outputs/roborazzi/`, which CI uploads. After a change meant to alter
  how a screen looks, run `./gradlew :app:recordRoborazziDebug`, look at every image it changed, and commit
  them. Comparison is exact for a screen (`shoot`); a sheet or dialog (`shootScreen`) may be one step in 255
  out per channel (`WithinOneStep`), because blending its translucent window over the page rounds differently
  on a Mac and on CI's Linux. **A screen is tested through its `…Content` composable**, which takes plain state and callbacks; the
  `…Screen` around it only reads the ViewModel and holds the sheets and dialogs. Keep new screens that way
  and add them to `ScreensScreenshotTest`. Fixtures are made up (a public repo): VINs that pass the check
  digit and belong to no one, invented records, no photos, and no real torque figures. Dates are drawn on
  `Fixtures.TODAY`, so a `…Content` that needs today takes it as a parameter, never `LocalDate.now()`.
- **Accessibility checks** (#234) run in the same tests, before each image is compared: Google's Accessibility
  Test Framework (latest preset, through `roborazzi-accessibility-check`) over every window drawn, in light and
  dark, failing on a warning as on an error (touch targets under 48dp, text contrast under 4.5:1, missing
  labels, clickable items that read the same). **A new screen test gets them by using `shoot` or
  `shootScreen`.** Compose's own `enableAccessibilityChecks()` switches itself off under Robolectric, which
  is why it isn't used. A deliberate finding is suppressed in `Screenshots.kt` with `allowed(check, …)` for
  that one kind of element, with a comment saying why; the only one is text contrast on a disabled control
  (WCAG leaves inactive controls out). Only what is on screen is checked, so a long screen's lower part
  isn't. `EndToEndTest` drives the app with UI Automator and has no accessibility pass: the screen tests
  are where these checks live. A bottom sheet is a `GloveboxSheet` (`ui/components/Sheets.kt`), which passes
  `SheetHandle`: Material's handle in a 48dp slot, since Material's alone is a 32dp target. A sheet built another
  way passes `dragHandle = { SheetHandle() }` itself.
- Release: merge the release PR that `release-please` keeps open (version and changelog come from the
  Conventional Commits on `main`); it tags `vX.Y.Z` and starts `release.yml`, which builds the signed
  APK after approval. See README.

## Modules

- `:domain` — pure Kotlin, **no Android imports**. All business rules live here and get unit tests:
  `ReminderEngine` (due status, whichever-first/last, mileage-only/time-only, lead time, pace),
  `ServiceLog` (last-done derivation), `Recalls` (list merge, campaign and component names),
  `KnownSpecs` / `KnownSchedules` / `KnownTakes` (bundled lists from named sources),
  `Torque`/`TorqueUnit`, `Vin`.
- `:data` — Room 3 (`androidx.room3`, #220) + SQLCipher (opened through SQLCipher's `SQLCipherDriver`; code that goes around the
  DAOs uses `useReaderConnection` / `useWriterConnection`, never `openHelper`), Keystore key management (`DatabaseKeyManager`), Tink photo
  encryption (`PhotoCrypto`), `NhtsaClient`, `RecallRepository`, `SettingsRepository` (DataStore),
  `DataWipe`, Hilt module.
- `:app` — Compose UI, navigation, ViewModels, `AppLock` + `LockScreen`, theme.
- `:baselineprofile` — test module (`com.android.test`) that generates the baseline profile and benchmarks
  startup. Nothing in it ships.

## Decisions — don't change without asking

- **Encryption:** DB passphrase is random, wrapped by an AES-GCM Android Keystore key with
  `setUserAuthenticationRequired(false)` and NOT `setUnlockedDeviceRequired`. Reason: WorkManager
  must open the DB while the phone is locked for reminders/recall checks. Biometrics gate the UI only.
- **No Android auto-backup** (`allowBackup=false`, data extraction rules exclude all). The app's own
  password-encrypted backup file is the only migration path.
- **Delete all data = crypto-erase:** destroy Keystore keys first, then files. Delete vehicle relies on
  `ON DELETE CASCADE` from `vehicles` plus deleting that vehicle's photo files.
- **"Last done" is never stored.** It's derived from service records (`ServiceLog.lastDone`), so editing
  or deleting a record automatically rolls reminders back. Don't add a cached last-done column.
  A "last done" the user gives on its own is a service record too, flagged `enteredAsLastDone` (#78).
  **Nothing is assumed or estimated for the user:** there is no "assume everything was done on schedule".
- **Torque stored in N·m**, converted for display.
- **Distances are stored in the vehicle's own unit, as typed** (#93). A vehicle counts in miles or
  kilometres (`vehicles.distanceUnit`, `DistanceUnit` in `:domain`), and every reading, interval, snooze
  point, cluster offset and milestone of it is stored in that unit exactly as it was typed, like readings
  across a cluster replacement. So nothing is converted for display and a figure always reads back as
  typed; whole miles can't hold kilometres (4 in 10 come back 1 km out), which is why it isn't a display
  setting. Almost all of `:domain` neither knows nor cares what unit its numbers are in. Column and field
  names that say "miles" are older than the choice. **Never write a number into a vehicle's tables in
  another unit, and never show one without the vehicle's unit:** there is deliberately no unit-less
  formatter (`Int.asDistance(unit)`, `Interval.text(unit)`, `VehicleEntity.unit`).
- **Network:** only NHTSA (`vpic.nhtsa.dot.gov`, `api.nhtsa.gov`). Only a VIN or year/make/model is
  sent. No analytics, crash reporting, ads, or other third-party SDKs that phone home.
  **CI and `release.yml` scan the release APK with Exodus Privacy's scanner** (`.github/scripts/exodus-scan.sh`,
  #217): a tracker fails the build, and so does any permission not in `.github/privacy/permissions.txt`
  (or one listed there that the APK no longer asks for). A new permission means updating that list and the
  Privacy screen's "Permissions" lines together. The release's notes carry the result and the report is
  attached to it, which is what the README's "trackers: 0" badge rests on.
  **A PR is also checked by GitHub's dependency review** (`dependencies.yml`, #246): a dependency it adds
  to the release classpaths with a known vulnerability, or a licence not in
  `.github/dependency-review-config.yml`'s GPL-3.0-compatible list, fails it. A new dependency whose
  licence GitHub can't name is added there by name, with its licence text in `assets/licences/`.
  OpenSSF Scorecard (`scorecard.yml`) reports on the repository's practices weekly and blocks nothing.
- **Barcodes are read with zxing-cpp, not ML Kit** (#161; before that, Java ZXing). ML Kit's bundled barcode
  library depends on Google's log-upload transport (`transport-backend-cct`, `transport-runtime`) plus
  Firebase and Play Services pieces, which breaks the rule above; asked again on 2026-10-15 after a failed
  phone trial, and still no. zxing-cpp (`io.github.zxing-cpp:android`) is native code with no network
  calls: its POM depends only on the Kotlin standard library and CameraX, and its `.so` imports no network
  function (checked with pyelftools; check again on an upgrade). It adds about 1.6 MB to the arm64
  release. Java ZXing stays as a test-only dependency, to draw the barcodes the tests read. Check any
  Google or Firebase library's dependency tree the same way before adding it.
- **Text is read with Tesseract, not ML Kit** (#64, the odometer off the dash). ML Kit's text recognition
  pulls in the same log-upload transport as its barcode reader. Tesseract4Android is native code with no
  network calls and no permissions, checked in the `.so` files themselves; check again on an upgrade. It is
  published only on JitPack, so `settings.gradle.kts` lets JitPack serve that one group and nothing else
  (`exclusiveContent`). **Don't widen that filter or add another JitPack dependency without asking.** Its
  native libraries are about 7 MB per ABI, plus the 4 MB language file in `assets/tessdata` (from
  `tessdata_fast` 4.1.0).
- **The release APK is 64-bit ARM only** (`abiFilters` on the release build type, #123): 22 MB where four
  ABIs made 49. Every phone the app is meant for is arm64; a 32-bit ARM phone or an x86 emulator can't
  install a release, and Android says so at install time. Debug builds carry every ABI, so any emulator
  works. One APK per release, which is what `release.yml` and Obtainium expect: **don't add an ABI back
  or split the APK per ABI without asking.** The baseline profile plugin's `nonMinifiedRelease` and
  `benchmarkRelease` build types also carry x86_64 (`finalizeDsl` in `app/build.gradle.kts`), so they run on
  a PC's emulator; they are never released.
- **NHTSA recalls are by year/make/model**; the public API can't confirm repair status for a VIN.
  UI lets the user mark recalls done and links to the manufacturer's VIN lookup.
- **applicationId `io.cosgriff.glovebox` must never change.** Release signing key must never change.
  **The package is registered for Android developer verification** (#267, 2026-10-17) with that key's
  certificate in the Android Developer Console (a full-distribution account: sideloading worldwide needs
  one from 2027), so a new key would also mean registering again. The ownership-challenge APK is built on the maintainer's
  machine by `.github/scripts/registration-apk.sh` (the console's snippet in
  `assets/adi-registration.properties`, git-ignored, for that one build), uploaded to the console only,
  never published. Releases carry no snippet and nothing in the app changes for verification.
- **IDE is Android Studio.** Keep AGP current (use Studio's AGP Upgrade Assistant); no IntelliJ IDEA
  compatibility constraint.
- minSdk 29, targetSdk 37, compileSdk 37 (Android 17's target changes checked in #232), JDK 17, Kotlin + Compose + Material 3, Hilt (KSP), Navigation 3
  (#237) with `@Serializable` routes that are `NavKey`s. Dynamic color is off; use the theme tokens.

## Design tokens (from the canvas)

Light: ground `#F4F3EF`, surface `#FFFFFF`, ink `#17191C`, muted `#5A5F66`, line `#DEDCD5`,
accent `#C2410C` (on-accent white), accent-soft `#FBE9DF`/`#9A3412`, ok `#0F766E`/`#DDEFEC`,
danger `#B42318`. Dark: ground `#121416`, surface `#1C1F23`, ink `#EDEBE6`, muted `#A5A9AF`,
accent `#F08A4B` (on-accent `#17191C`), accent-soft `#3A2418`/`#FDBA74`, ok `#5EEAD4`, danger `#F97066`.
Light mode shows importance with tints (soft orange = needs attention, solid orange = most urgent),
not black panels. Fonts: IBM Plex Sans + IBM Plex Mono, bundled in `res/font` and set in `Theme.kt`
(`PlexSans` through `MaterialTheme.typography`, `PlexMono` by name for mileage, VINs, torque and other
figures). Never use `FontFamily.Monospace`. **The one exception is the home-screen widget** (#238): Glance
draws through RemoteViews, which can't use bundled fonts, so it is in the system font. Their licence (OFL 1.1) is `assets/licences/com.ibm_plex.txt`
and is listed on the licences screen by hand in `LicenceCatalog`, since fonts aren't Maven dependencies.
Accent text on the soft-orange tint is `onPrimaryContainer`, not `primary` (4.4:1 in light, below the
4.5:1 text needs); `primary` there is fine for icons and large figures. Colours that Material doesn't name
come from `statusColors()`. Every `surfaceContainer…` role is set in `Theme.kt` (dialogs and date pickers
use High, sheets Low): one left out falls back to Material's lavender. In dark the urgent pill is solid (`onPrimaryContainer` behind dark text).
Touch targets ≥ 48dp. Destructive actions are red and confirm by typing (vehicle name / DELETE).

## Screens (canvas board → status)

Done in code: unlock, empty garage, garage list (status chip, next reminder, across-all strip; opens the
vehicle directly when there is only one), vehicle home (Coming up + Log
service), vehicle details (photo, nickname, from the VIN, what it takes, schedule and recalls rows,
Mark as sold), archived vehicles (read-only history, move back to garage), delete vehicle (counts, type the
name to confirm), torque specs (unit switch, categories, search, add / edit / delete),
update-mileage sheet (with Undo, and Read dash: the odometer read by the camera), add vehicle via
VIN (VIN entry, then a confirm screen where decoded values can be corrected), add without a VIN (year,
make and model from NHTSA's lists or typed), VIN scanning (Code 39, QR and printed VINs), receipt and parts photos
(attach, grid, viewer, share), export as PDF or Markdown, notifications (service due, overdue, new
recalls, mileage check-in) with their settings screen, encrypted backup (create, monthly reminder, restore), recommended schedule after decode (+ edit item, add own), maintenance schedule after
setup (track / untrack, edit, add, last done and next due), Log service, History
(per-vehicle list, category filters), service record detail, edit and delete with Undo, settings
(grouped cards: theme, biometrics, lock delay, hide in recents, reminder rule, schedule, torque units,
notifications, recall checks, backup, export, delete all data), about (version, data sources, privacy,
open-source licences), Reminders tab (grouped list, vehicle filter,
Log it, snooze and skip), reminder rules (default rule, lead time, per-service overrides), recalls
(list, mark done / open, banner on vehicle home, garage chips, row on the decode result).
First-run welcome (lock and notification choices, add a vehicle or restore), delete all data (type
DELETE, then the phone's screen lock). Odometer offset (instrument cluster replacement: date and the two
readings, from Vehicle details). "When were these last done?" (a date and mileage per reminder, from
schedule setup, the Maintenance schedule screen and vehicle home). Import (Settings: torque specs from a
a by-interval maintenance schedule or past service history from a JSON file, with a preview and a choice
about what differs; also the user's own milestone table and a Carfax report PDF; Vehicle details' "Import service
history" row opens the same page with that vehicle chosen and no vehicle choice, `ImportRoute(vehicleId)`). Mileage readings (every odometer reading, correct or delete a wrong one, from Vehicle details). Milestones (the schedule by mileage with what was done, from the Maintenance schedule
screen; the user's own table is built and edited there, and saved from there as an import file). Known
maintenance schedules (a maker's schedule and table, used at setup for the 2017–2024 4Runner and offered to one set up earlier).
Documents (a vehicle's paperwork, from Vehicle details). Fill "What it takes" from a maker's manual
(a bundled list, rows tagged DOUBLE-CHECK until checked). Launcher shortcuts (Log service, Update mileage).
To build: VIN scan follow-up (#83).

Notes on what's built:
- Distance units (#93). The unit is chosen where a vehicle is added (a switch on both paths, starting at
  `Settings.distanceUnit`, the Settings row, which is only the default for new vehicles) and changed on
  Vehicle details, which converts: `UnitDao.convert` rewrites every figure of that vehicle in one
  transaction and is the only thing that changes a unit. **There are two conversions and each figure
  takes one** (`DistanceKind`): a *measurement* (a reading, a snooze point, the cluster offset) goes
  exactly, at 1.609344; a *schedule figure* (an interval, a milestone, how a table repeats) goes at 1.6,
  as manuals for both markets print them, so 5,000 mi is 8,000 km and comes back as 5,000. Glovebox's own
  figures are kept in miles and given to a kilometre vehicle as schedule figures: the typical schedule
  (`ScheduleTemplates.generic(…, unit)`), the two snooze steps (`Snoozing.distance`), and the thresholds
  for a reading far above the last (`MileageReadings.farAhead`) and the dash reader's window. The lead
  distance is one setting, kept as typed in `Settings.distanceUnit`; **always take it through
  `Settings.leadFor(vehicle.unit)`**, never `Settings.lead`, and changing the default unit converts it.
  **An import file says its own unit by the fields it uses** (#126): `everyMiles` or `everyKm`, `atMiles`
  or `atKm`, `repeatsEveryMiles` or `repeatsEveryKm`, `odometer` or `odometerKm`, one unit for the whole
  file (`ImportCheck.distance` refuses a second), reported as `ImportFile.….unit`. `FileDistances` puts a
  file into the vehicle's unit before anything is compared or saved: nothing is converted when the two
  agree, and the Import page says so when they don't. A file the app writes is in the vehicle's unit, so
  it reads back into that vehicle unchanged. The kilometre limits are the miles limits as schedule
  figures (a test holds them equal), except the odometer's, which is seven digits in either. The dash
  reader reads a number and takes it to be in the vehicle's unit. Notifications name services and carry
  no distance. After a conversion a service already announced as due may be announced once more, because
  `NoticeRules.cycle` is keyed on the last-done reading. Each vehicle in a list (garage, Reminders, an
  export of all vehicles) is worded in its own unit.
- Add vehicle is one route with two steps in `AddVehicleViewModel`: VIN entry, then "Is this your
  vehicle?". Body, drive type, engine, fuel, transmission and the year / make / model can be corrected
  before saving; the drive type is picked from `VehiclePick.drivetrains` (vPIC often has none), and a
  decoded one that isn't among them is kept unless one is picked. With no trim from vPIC, the screen says so and that
  the trim is optional, so an empty field doesn't read as a failed decode. `VinDecode.quality` sorts a decode into clean, partial (vPIC flagged it) or unidentified;
  a vehicle is never saved without a real year, make and model, so an unidentified VIN has to be fixed or
  filled in by hand. vPIC trims that list several ("SR5/SR5 Premium") are offered as choices, and
  `vehicles.trimConfirmed` records that the user picked or typed one. The vehicle is saved before the
  schedule screen, so Back from there keeps it.
- VIN scanning (`ScanVinScreen`): the shared camera set-up in `CameraFeed.kt` (#223) binds a `Preview` and an
  `ImageAnalysis` (`rememberFrameAnalysis`) with `ProcessCameraProvider` and shows the picture with
  camera-compose's `CameraXViewfinder` (Crop, `EMBEDDED`, so it moves with a predictive back swipe; pinch zooms
  and a tap focuses through its coordinate transformer). The analysis feeds frames at about 1080p to
  `VinBarcodeReader`, which takes the luminance plane, turns it upright (a 1D reader only scans along
  rows), cuts out a band across the middle where the guide is, and asks zxing-cpp for a Code 39, QR, Data Matrix
  or Code 128 code.
  The camera starts at 2x zoom so the phone can sit back far enough to focus. If a frame can't be read it
  is tried again after `evenOut`, which stretches each row's contrast locally so a barcode half in shadow
  or glare still reads. Reading is native, so its tests run on a device (`VinBarcodeReaderDeviceTest`, in
  CI's emulator job); the frame maths around it stay JVM tests. A barcode that isn't a VIN is only said to
  be one when the same text is read twice in five (`VinScan.notAVin`), and the slow printed-VIN reader runs
  on one frame in four (`VinScan.PRINT_EVERY`). `VinScan.vin` accepts only a 17-character run that passes
  the check digit; it deliberately doesn't slide a window along longer text, which would match by chance
  about one time in eleven. With no barcode in the frame the same band goes to Tesseract
  (`TextReader.vin`, #82) for a VIN in print, on a windshield plate or a registration card.
  `VinScan.printed` makes the one repair there is a reason for (a VIN has no I, O or Q, so one read as
  such is 1 or 0), closes up a VIN printed in groups, and otherwise keeps the barcode's rule: exactly 17
  characters and a valid check digit, nothing guessed, since trying substitutions until one passed would
  pass a wrong VIN one time in eleven. A printed VIN is offered only when two of the last five frames
  read the same one (`VinScan.agreed`); a barcode is taken at once. If the recogniser can't start, the
  scanner is a barcode scanner as before. Frames are never stored. **Frames are turned as the screen shows
  them:** `CameraFeed` keeps both use cases' `targetRotation` at the display's rotation (and follows a turn
  from one landscape to the other, which doesn't recreate the activity), so a frame's `rotationDegrees` is
  the screen's. The `LifecycleCameraController` used before set it from the motion sensor, so a phone
  tipped while aiming had its frames a quarter turn from the preview and read nothing; never set the
  target rotation from an orientation listener. Settings'
  "Show what the scanner sees" (`Settings.scannerDetails`, off by default) adds a panel under the guide
  (`ScanReport`): the band read, any barcode found outlined (green read, amber found but not decoded, from
  zxing-cpp's `returnErrors`), frames a second and what the print came to. The VIN goes back to the add screen through the nav
  host's results (`SCANNED_VIN`) and is decoded at once. The scanner keeps the canvas's dark colours in both themes.
- Add without a VIN (`ManualAddScreen`) picks year, make and model. Makes come from vPIC's car, truck and
  MPV lists and models from make + year; `NhtsaClient.makes` sends nothing about the vehicle and `.models`
  only the make and year. Every picker also takes typed text, which is the only way in when the lists
  can't be loaded. Such a vehicle has `vin` null and `enteredByHand` true; a VIN or older serial number
  can be added from Vehicle details (`Vin.isPlausible`), which doesn't decode it or change the details.
  The drivetrain is never preselected.
- The schedule is a generic starter list (`ScheduleTemplates.generic`), labelled as typical intervals,
  not manufacturer data, **unless a bundled maker's schedule covers the vehicle (next note): then that
  is what setup starts from**, in place of the typical list.
- Known maintenance schedules (#134, `KnownSchedules` in `:domain`, `KnownScheduleCatalog` in app,
  `docs/known-schedules.md`, which has the rules and how each list was made). **Glovebox has no schedule
  of its own for any model and works none out; a bundled schedule is a maker's published guide, named.**
  A list is a file in `assets/known-schedules/`: the same header as a torque list (id, make, model,
  years, source with `basis`; `KnownLists.header` reads both), `milestones` written as a
  `milestone-schedule` file's items and `intervals` as an `interval-schedule` file's, read by those same
  readers. **Only facts are taken (mileages, months, which jobs), named in Glovebox's words; a list
  covers only the model years whose guides were read and agree, never "the generation".** The one that
  ships is the 4Runner's, 2017 to 2024, from Toyota's Warranty and Maintenance Guides. **An interval has
  to be the table's own:** `KnownSchedules.parse` refuses one whose job isn't in the table at exactly
  the multiples of that interval (or at its `firstAtMiles` and every interval after). Reminders are for
  logged work; an inspection is one only where the user asked (the 4Runner's differential and transfer
  case oil checks), never every inspection in a guide. The table holds every
  line of the guide; a special-operating-condition job is named with its condition and has no `service`,
  so it is never taken for the ordinary job.
- A list's `fourWheelDriveOnly` names the jobs its guide marks "(4WD)"; `KnownSchedules.milestonesIn` /
  `intervalsIn(list, unit, drivetrain)` leave them out only for a vehicle **known** to drive two wheels
  (`Drivetrains.kind` is `TWO_WHEEL`: vPIC's "4x2", or RWD / FWD). **4WD and AWD are different systems
  and `DriveKind` keeps them apart** (the user's point): an AWD or unknown vehicle keeps the "(4WD)" jobs
  only because the guide has no other line for it, and a guide with AWD lines gets its own field.
- `manualExcluded` is the same for the gearbox: only a vehicle known to have a manual
  (`Transmissions.isManual`, vPIC's "Manual/Standard") loses the jobs named there (the GTI's DSG fluid).
  A job the guide gives by time alone is an interval with months, no distance and `rule: time-only`;
  `KnownSchedules.contradictions` then only requires the job to be in the table.
- A model whose guide has a schedule per engine gets a list per engine (`KnownScheduleList.engine`:
  a label, the displacement and, where needed, the fuel). `KnownSchedules.match(lists, year, make, model,
  engine, fuel)` reads the size from the vehicle's engine text (`Engines.displacementL`); **an engine that
  isn't on record matches no engine's list**, and `ScheduleSetupViewModel` asks "Which engine does it
  have?" from `KnownSchedules.engineChoices`, writing the pick to the vehicle only if it had none
  (`VehicleDao.setEngineIfBlank`). Only setup asks; the offers on the schedule and Milestones screens
  need the engine on record. No list that ships uses it yet.
- The lists that ship: the 4Runner 2017–2024 (Toyota's guides, every year read) and the Mk6 GTI
  2010–2014 (Volkswagen's cards as filed with NHTSA: 2012 and 2014 read in full, 2010 from the
  all-models card, **2011 and 2013 inferred**; `docs/known-schedules.md` has the table of what was read).
- **Where a list covers a vehicle, it is the schedule** (the user's decision, 2026-10-13, reversing
  "offered, never at setup"). `ScheduleSetupViewModel` matches the list and shows its intervals in place
  of the typical ones; saving writes them as the user's own items (no `templateKey`, `service` set,
  `knownList` set) and adds the list's table as the vehicle's milestones (`MilestoneDao.addKnown`, which
  does nothing if a table has appeared). A vehicle set up earlier gets "Use the maker's schedule" on the
  Maintenance schedule while the list has an item it lacks (`KnownSchedules.plan`: an item of the user's
  own with that name is kept, there is no replace), with a dialog that lists each interval and the
  typical items that would double one, **ticked** to be replaced (an import's are unticked and only stop
  tracking), and "Use the maker's table" on Milestones. **A replaced typical item is removed**
  (`ScheduleDao.applyKnown`; the user found an unticked generic twin beside the maker's item confusing),
  unless a service was logged against it, when it stays untracked so the record keeps its link. This is
  the one place a typical item is deleted. The typical items a maker's schedule doesn't cover can still
  be added, unticked, from the schedule screen; ones whose job an item of the user's own already is
  (by its `service`) are not offered back. **The source is said once per screen, never under every item** (the user found that too
  busy): an info box on the Maintenance schedule when any item has `knownList`, and the introduction on
  Milestones (`vehicles.milestonesKnownList`, cleared with the table). There is no "checked" state as
  torque values have. A list in miles goes to a kilometre vehicle as schedule figures.
- A job's first time can be at its own mileage (`schedule_items.firstMiles`, `TrackedItem.firstAt`,
  `firstAtMiles` / `firstAtKm` in an `interval-schedule` file, "First time at" in the item editor):
  "first at 100,000, then every 50,000". Stored as distance really covered, in the vehicle's unit;
  `toTracked(rule, swap)` puts it in dash terms, so **pass the vehicle's swap wherever statuses are
  worked out**. It matters only until the job is logged once.
- A job can be only for severe use (#136, `schedule_items.severeOnly`, `severeOnly: true` in an
  `interval-schedule` file, `DraftItem.dormant`): a maker's "additional items for special operating
  conditions", such as differential oil when towing. Its interval is in the ordinary interval columns
  and it has no severe interval of its own (the reader refuses both). **`toTracked` returns null for it
  while `useSevere` is off**, so on normal use it reminds nobody, isn't "to log" and isn't counted as
  tracked; it stays on the Maintenance schedule saying "Only for severe use. Off while you're on normal
  use." **One Normal / Severe switch, not one per condition** (the user's choice): Severe turns every
  such job on, each named with its condition, and the owner unticks what doesn't apply. The editor
  can't make one; they come from a maker's list or an import file.
- **A job the vehicle hasn't reached is not "Not logged yet"** (`VehicleReminders.statuses`,
  `ItemStatus.awaitingFirst` / `.counting`): never logged, with a reading on record below its first
  mileage (`firstAt`, or one interval from zero), it counts down to that mileage like any reminder,
  by distance only (`ReminderEngine.first`), since a maker's "or 120 months" counts from a day in
  service that Glovebox doesn't know. A time-only job, a vehicle with no reading, and a job the vehicle
  is already past stay "Not logged yet". **Use `counting`, not `lastDone != null`, to decide whether a
  reminder has something to count from.** `NoticeRules` announces such a first time once.
- After setup the schedule lives on `ScheduleScreen` (from Vehicle details). It writes straight to
  `schedule_items`: a tick is `tracked`, and an edit keeps whatever the editor doesn't show, such as a
  snooze. `ScheduleItemEditor` is shared with setup; `ItemHistory` adds LAST DONE (derived from the log,
  linking to the record) and NEXT DUE, which follows the interval as it is typed. The pace estimate is
  shown only for the saved interval and rule. An empty schedule offers setup or adding an item; one that
  lacks typical items (`ScheduleTemplates.missing`) offers to add them unticked. Never send a vehicle that
  already has items back through setup: it inserts every item again.
- "When were these last done?" (`LastDoneScreen`, `LastDoneSheet`, `LastDoneEntries` in `:domain`) takes a
  date and a mileage for one reminder and saves them through `RecordActions.addLastDone` as a service
  record with that one line and `service_records.enteredAsLastDone` set. Nothing else about reminders
  changes: last done is still derived from the log, and deleting the record puts the reminder back to
  "Not logged yet". Such a record says "Entered as last done" in place of who did the work (History, the
  record's screen), "Entered by you" where a logged one says "From your log", and an export says the owner
  entered it afterwards. Editing it in the Log service form keeps the flag. It announces nothing and
  doesn't open History, because it is entered where its result shows. Setup's dropdown has two choices,
  entering them as they come up or going on to the list; the canvas's "import from a dealer record" and
  "assume everything was done on schedule" are deliberately not built.
- Import (`docs/import/`, `ImportFormat` and `TorqueImport` in `:domain`, `ImportScreen` and `ImportPicker`
  in app): the user picks a vehicle and a JSON file, and the file's items are added to that vehicle.
  `docs/import/README.md` and the JSON Schemas in `docs/import/schema/` are the specification, as
  `docs/backup-format.md` is for backups. **Change the schema, the Kotlin reader and the example files
  together, and never change how a version-1 file is read.** `ImportFormatTest` runs every file in
  `examples/invalid/` through both the schema (a test-only validator) and `ImportFormat`, and checks the
  schema's enums and caps against the Kotlin constants, so they can't drift. A rule a schema can't state
  goes in `examples/beyond-schema/` and in the README. The format is strict on purpose: unknown fields,
  `null`, stray spaces and out-of-range values refuse the file, nothing is repaired, and every error is
  reported with its path (`items[3].torque.unit`). The app ships no JSON Schema library; `:domain` reads
  the file as a JSON tree with kotlinx.serialization. A file never names a vehicle. There are four kinds
  (`ImportKind`), all built. The Settings row is absent with no vehicles.
- A Carfax report PDF is a service-history import (#160): `ImportPicker` sees "%PDF" at the start of the pick,
  `PdfWords` (app, PDFBox for Android, from memory and never written to disk, BouncyCastle excluded) gives
  each word with its position, and `CarfaxReport.read` (`:domain`) turns the history table into
  `ImportFile.ServiceHistory`, so the Import page previews and writes it like any history file. Columns are
  found from positions (an entry starts on a line whose first word is a date at the left; a wide gap
  separates the source from the comments). **Only service visits are taken**: an entry counts when a
  comment is work (`isWork`: replaced, changed, serviced, inspected…, and not a title, registration, owner
  or damage line). A comment becomes a catalog line only where the wording is plain (`CarfaxReport.service`:
  "Oil and filter changed" is oil, "Front differential serviced" is `front-diff`, "Brake pads replaced" stays
  text: which axle?). A visit that names one job twice ("Alignment performed", "Four wheel alignment
  performed") gets one line, worded as the longer (`oneLinePerService`). The source's first line
  is the shop. A visit with no mileage is counted and refused, never guessed. The ligatures (fi, fl, ff,
  ffi, ffl) drawn as one glyph come back as the ligature character, a replacement character, or an ordinary
  letter the PDF misnames them as ("clter", "iuid"), and `repairLigatures` puts them back, only ever into a
  word of its vocabulary. **A misnamed letter is learnt from the whole report** (`standIns`: the letter must
  make at least two different known words), because one word a letter away proves nothing ("Door" and
  "floor", "Lex Brodie's" and "Flex"). Tests use a made-up
  report built word by word; **never commit a real report**. The Import page offers "Keep a copy in this
  vehicle's documents" for a report, off by default: `PickedImport.pdf` holds the bytes in memory and
  `ImportViewModel.import` files them through `DocumentStore.addPdf` once the import is written.
- What an import may do: `ImportPlans.plan` sorts a file's items into new, fills (the saved row is only
  a name), same and differs before anything is written, and the page shows that. "Add new only" is the
  default; "Replace matching" writes over the rows that differ after a dialog lists each one. An import
  never deletes, is one transaction (`TorqueDao.applyImport`), has no Undo, and only ever writes the
  user's own rows: nothing Glovebox supplies may be changed by one (typical schedule items; a bundled
  torque list itself is an asset, and a row added from one is the user's row from then on). The picked file is read into memory, capped at 2 MB, and never
  written to disk, which is why `DataWipe` knows nothing about it and what the privacy screen says.
  Torque rows match on category and name ignoring case; a file's caps are whole figures per unit
  (`TorqueImport.maxIn`) so the schema can state them exactly.
- A new kind of import is: a schema, a valid example, `examples/invalid/<kind>/` and
  `examples/beyond-schema/<kind>/`, a reader and plan in `:domain`, a case in `ImportFormat.parse` and
  `supported`, and a `...Preview` function in `ImportViewModel` that turns the plan into a `PlanView`
  (words only) with a `write` lambda. The page itself is kind-agnostic.
- Schedule import (`ScheduleImport`, kind `interval-schedule`): every item becomes the user's own
  `schedule_items` row (no `templateKey`, tracked). Matching is by name against the user's own items
  only; a typical item (`templateKey` set) is never matched, so it can't be replaced. Replace writes
  only the interval, severe interval and rule (`ruleOverride`), keeping `tracked`, `useSevere`, a snooze
  and the item's records. A tracked typical item with the same name as a file item is a "double"
  (`ScheduleImport.doubles`): the page offers, unticked by default, to stop tracking it, which is the one
  thing an import may change on a typical item and only because the user ticked it. A file item's optional
  `service` sets `schedule_items.service`, is part of same-or-different, and makes the typical item that
  service resets a double too. The file has no last-done; the result card offers "When were these last
  done?".
- History import (`HistoryImport`, kind `service-history`): each item is a service record with
  `service_records.imported` set, written through `RecordActions.applyImport` (no snackbar; the Import
  page shows its own result). A line's optional `service` is a `CatalogItem` id: it files the line under
  that category and resets the schedule item whose `templateKey` the catalog item `resets`. A line without
  one resets one of the user's own items when its label is that item's name (`HistoryImport.resolve`).
  The schema's `service` enum is generated from the catalog and a test holds them equal, so **adding a
  catalog item means adding it to `service-history.schema.json` and the README table**, and its name to
  `CatalogText.kt` and `strings.xml` (`ServiceCatalog` holds ids only; `CatalogTextTest` checks the names). Records match on
  date and odometer. A matching bare last done (#78) counts as a fill, replaced in either mode, only when
  the imported record resets every reminder it did; otherwise it is a difference. A replace keeps the
  record's id, so its photos and recall link stay, and clears `enteredAsLastDone`. The preview lists the
  reminders that move for the chosen mode (`LogEdits.reminderChanges` over `HistoryImport.entriesAfter`)
  and lists, under "Readings to check", records whose reading looks mistyped (`HistoryImport.doubtful`:
  outside the heaviest run of readings that never goes down, saved readings weighing more; or far ahead of
  the reading before, by `MileageReadings.farAhead` or over three times the vehicle's own pace), both in
  dash terms. **Those are left out unless the user ticks them** (the user's GTI gained 60,000 miles from a
  shop's typo in a report: the odometer is the highest reading). The preview is planned without them. Sold vehicles can take history but no other kind. "Not in the future" is measured against the
  `today` passed to `ImportFormat.parse`. An imported record says "· imported" after who did it
  (`LogEdits.who`) and carries `ExportPhrase.IMPORTED_NOTE` in exports; editing it in the form keeps the flag.
- Milestones (`Milestones` in `:domain`, `MilestonesScreen`): the schedule laid out by mileage, as an
  owner's manual prints it. **They are a view, never a second source of what is due:** reminders and
  notifications come only from the interval schedule and the service log, and nothing on that screen
  creates, moves or silences one. Nothing about them is stored; "completed" is derived from the log each
  time, and there is deliberately no tick-box: the green tick beside a completed job (and on a card whose
  jobs are all done) is drawn from the log, and can't be tapped. The list is generated from tracked items that have a mileage
  interval (`Milestones.generate`): every multiple is a point and items on the same point share a card. It
  starts at the vehicle's earliest logged job and runs a few points past the odometer, more on request.
  A run of three or more past milestones with nothing logged is folded into one row (`Milestones.folded`).
  Each card collapses to its mileage and a count ("4 jobs · 1 completed"); only the next one starts open,
  and "Expand all" / "Collapse all" set every card and unfold or refold the empty runs. Cards are filled
  and borderless like the schedule screen's, with the next one outlined in the accent.
  Matching (`Milestones.status`): a logged job belongs to the first of that job's milestones at or after
  the mileage it was done at, so an oil change at 48,000 shows under 50,000 as completed there, and of two
  in one stretch the later is shown. A job nothing matches is listed with no status. Milestone mileages are
  miles really covered: with a cluster replacement everything is placed by `swap.actual`, and the dash
  figure is shown beside it.
- The user's own milestone table (`MilestoneImport`, kind `milestone-schedule`, tables `milestones` and
  `milestone_jobs`, both cascading from `vehicles`): imported from a file, typed in the app (below) or
  added from a maker's schedule that ships with the app. A milestone holds up to 40 jobs. When a
  vehicle has one, the Milestones screen shows it **in place of** the generated view, with
  `status(continues = false)`, since a table simply ends and a job's last milestone takes whatever comes
  after the one before it. A job is the same job across milestones, and is completed by logged work, by
  its `service` (a record line's `catalogId`) or failing that its name (the line's label, ignoring case):
  `MilestoneImport.jobKey` and `doneKeys`. Nothing about reminders, notifications or exports reads these
  tables. "Remove this table" deletes it and the generated view returns. Import matches on `atMiles`;
  a replace keeps the row and swaps its months and jobs. Backup and restore carry the tables without
  change, because both go table by table from SQLite.
- A table that repeats (#113): `repeatsEveryMiles` sits beside `items` in the file, the one field a
  kind has of its own (`ImportFormat.ownKeys`), and is the length of the whole cycle, never less than
  the highest `atMiles`. It is kept in `vehicles.milestonesRepeatEveryMiles`, written only by
  `MilestoneDao.applyImport` and cleared by `deleteTable`; it means nothing while the vehicle has no
  table. `MilestoneImport.milestones(table, every, current, more)` lays the cycles out: the table as
  written, then on to the sixth point past the odometer, with "Show further ahead" adding more, and it
  is matched with `status(continues = true)`. A repeat has `Milestone.repeatOf` and **no `orMonths`**,
  which is the vehicle's age the first time round; nothing is worked out for later rounds. **A vehicle's
  table repeats one way or not at all:** a file whose `repeatsEveryMiles` isn't the saved table's is
  refused whole (`MilestoneImport.repeatConflict`), in either mode, and the user removes the table to
  change it. That is what keeps the cycle from ever being shorter than the saved table.
- The table is built and edited on the Milestones screen (#114, `MilestoneEditor`, `MilestoneEdits` in
  `:domain`): "Make my own table" under the generated view, then "Add a milestone", "Edit this milestone"
  on an open card (a repeat opens the milestone it repeats) and a row for whether the table repeats.
  **A table typed in the app keeps exactly the rules of one read from a file, from the same code:**
  `MilestoneImport`'s limits and its `repeated`, `nameKey` and `cycleCovers`, which the reader and
  `MilestoneEdits.check` / `.repeat` both call, and a test runs the same values through both. A form only
  trims the space round a name. So a milestone can't be added past the end of a cycle, and the cycle
  can't be set shorter than the table. Decided there: a table starts **empty**, never as a copy of the
  generated milestones (those go on for ever and a copy would freeze an arbitrary stretch of them); a
  job's service is picked from the catalog and **never suggested from its name**; deleting a milestone
  or the table is a **confirmation, with no Undo** (nothing else hangs off these rows); Close and Back
  ask before discarding a changed form. The table exists once its first milestone is saved, and deleting
  the last one removes it (`MilestoneDao.deleteMilestone`). Jobs keep the user's order in
  `milestone_jobs.position`; read them through `MilestoneWithJobs.ordered`. The screen calls it "your
  own milestone table" however it arrived: where a table came from is not stored.
- "Save as an import file" on the Milestones screen (#118, `MilestoneExport` in `:domain`) writes the
  user's table as a `milestone-schedule` file: the table, `repeatsEveryMiles` when it repeats, each job's
  `service`, and a `$schema` line. **It must always be a file the app itself accepts:**
  `MilestoneExportTest` puts tables through `MilestoneExport.file`, then `ImportFormat.parse` and the
  published schema, so change the writer with the format. Like every import file it names no vehicle and
  carries nothing of the service log; only the suggested file name has the year, make and model. It goes
  out through `ExportSaver` (`cache/export/`, deleted either way), unencrypted, and the privacy screen
  says so.
- The Maintenance schedule and Torque specs have the same row (#120, `ScheduleExport`, `TorqueExport`,
  `ImportFiles` in `:domain`; `SaveAsImportFile` and `ExportSaver.saveImportFile` in app, which Milestones
  uses too). **A schedule file holds every tracked item, typical as well as the user's own,** each typical
  one with the `service` line that resets it; imported, they become that owner's own items and
  `ScheduleImport.doubles` offers to stop the typical ones. It carries no `useSevere`, snooze or last
  done. A torque file holds every row, values in the Settings unit to two decimals (in N·m where the
  unit's whole-figure cap is in the way), and `source` only beside a value. **The screens are looser
  than the format** (an interval has no upper limit, two torque rows can share a name), so
  `ImportFiles.sift` puts each item through the kind's own reader before writing it; what the reader
  refuses is left out (`WrittenFile.leftOut`) and listed in a dialog before anything is saved, never
  dropped quietly and never repaired. Service history deliberately has no such file: the export and the
  backup are for it. A new writer goes the same way and gets a round-trip test in `ImportFilesTest`.
- Every `ImportKind` is now built, so there is no "not yet" path; a new kind needs one again only if it
  is specified before it is implemented.
- `vehicles.fuel` is vPIC's primary fuel type, kept since schema v6; vehicles added earlier have none.
  `ScheduleTemplates.generic` leaves engine oil, the engine air filter and spark plugs out for "Electric".
- Which reminders a logged line counts for is **derived, never stored** (`ServiceLinks` in `:domain`): the
  item the line was saved against (`service_record_items.scheduleItemId`), plus every item that answers to
  the catalog line it was picked from (`catalogId`). A typical item answers through
  `schedule_items.templateKey` matching `CatalogItem.resets`; one of the user's own answers through
  `schedule_items.service`, a `CatalogItem` id set in the schedule item editor or by an import. So one tick
  of "Oil & filter change" resets the typical item and the user's "Oil change" alike, and pointing an item
  at a service makes work already in the log count at once. **Never read `scheduleItemId` on its own to
  decide what a record resets:** go through `toEntry(swap, items.answering())` (`Mappers.kt`), or
  `HistoryImport.itemIds` for a record not saved yet. `NoLinks` is for callers that only want readings.
  `service` is ignored on a typical item, and `templateKey` still means "Glovebox's own", which is what
  keeps imports from touching those. On Log service an item that answers to a catalog line is logged
  through that line and isn't also listed under "Your items".
- A catalog line can count as another (`CatalogItem.covers`, one way only): a change of the front
  differential oil (`front-diff`) is also its check (`front-diff-check`), likewise `rear-diff` and
  `transfer-case`, and the combined `diff-fluid` line covers all three checks. `ServiceLinks.answering`
  and `MilestoneImport.doneKeys` both follow it, so a logged change resets the reminder to check and
  ticks the inspection on a milestone. **What a Log service line stands for is narrower:**
  `ServiceLinks.own` (no `covers`) decides `WorkOption.scheduleItemId`, so "Log it" on the check ticks the
  check and not the change that would also reset it. This is how "checked" and "replaced" are both loggable without
  a per-line action: they are two lines.
- Log service only offers one-tap "due" chips for reminders that have a log entry.
  Turning one on opens its category, so the tick is in sight. The category buttons are tabs, not ticks
  (the user found the canvas's solid ink "open" button read as chosen): the open one is outlined in ink
  with a caret, and a soft-orange "✓ n" says how many of its lines are ticked.
- Log service leaves the odometer empty and shows "Last recorded" under it; it is deliberately not
  prefilled, because a prefilled reading gets saved untouched and every reminder is then counted from the
  wrong mileage. A "Use current" button under the empty field (new records only) fills it on the user's tap. The note on the right of each line comes from `ServiceCatalog.hint`: what the user wrote
  on Vehicle details for that job (oil, tire size, wiper blades), otherwise the reminder's interval.
  Future dates are refused by the date picker itself, typed or tapped.
- Never-logged reminders are "overdue" to `ReminderEngine`, but the UI lists them apart ("Not logged yet")
  and leaves them out of overdue counts — see `ReminderPresentation.comingUp`. On vehicle home that
  section sits **above** "Coming up" (the user's choice, 2026-10-13: they have nothing to count from, so
  they are dealt with first; the Reminders tab puts that group first too), each row with "Log it" (Log service with that job ticked) and one "Enter
  when these were last done" under the card. Vehicle home has three groups, in this order: Not logged yet, Overdue
  (`ComingUp.overdueNow`) and Coming up (`ComingUp.upcoming`: due soon, fine, then snoozed, since a snooze
  is "not now", as on the Reminders tab), and **every row in all three is one tap from Log service with
  that job ticked** (the user's request: log it from where it's seen). They are still never given a guessed next-due point. A row for a job
  that goes by distance says, in amber (`statusColors().soon`, never the overdue red), where the schedule
  last put it: "Last scheduled at 50,000 mi · 2,000 mi ago · nothing logged" (`ItemStatus.lastScheduled`,
  counted from when the vehicle was new: `TrackedItem.zeroAt`). **"Up to date" is said only when every
  tracked job has something to count from;** with work still to log the badge on vehicle home and the
  garage chip say "N to log" (`ComingUp.attention`, `Attention.toLog`), after anything overdue or due soon.
- The garage list is skipped once per launch when there is exactly one vehicle (`GarageScreen`); "All
  vehicles" still returns to it, which is where another vehicle is added. Only while the list is what
  shows (`showing`): a notification or shortcut that opened a screen over it on a cold start uses the
  launch up, or the skip would land on top of that screen.
- Update mileage adds an `odometer_readings` row dated today, with Undo on the snackbar. The sheet's
  preview comes from `MileageUpdate.preview`. A reading further above the last one than a vehicle is
  driven in the time (`MileageReadings.farAhead`: over 10,000 miles and over 150 a day) turns Save into
  "Save anyway" with a line saying why, as a lower one already did. It is a question, never a block.
- Mileage readings (#65, `MileageReadingsScreen` from Vehicle details, `MileageReadings` in `:domain`)
  lists every reading newest first: the standalone ones and those logged with a service record. **The
  odometer is the highest reading on record, so one slipped digit becomes the odometer and makes every
  mileage reminder overdue;** this screen is where that is found and fixed after the Undo has gone. The
  row the odometer comes from is tagged, and two kinds are marked to check: one outside the longest run
  that never goes down (of two equal runs, the one ending later is believed), and one that is `farAhead`
  of the reading before it. **Nothing is ever decided wrong or changed for the user:** the wording is
  "check this one". A standalone reading opens a sheet to correct its value or date, or delete it, each
  with Undo (`MileageReadingUndo`, `OdometerDao.put`); one from a record opens the record, which is
  where it is edited. Readings are shown as typed and compared in dash terms, like everything else with
  a replaced cluster. The final odometer logged when a vehicle is sold is an ordinary reading here.
  A chart sits above the list (#288, `MileageChart` in `:domain`, `MileageChartCard`): the same readings
  and flags as the list (`MileageReadings.rows`), dates across from the first reading in the range (a slider
  from the past year to every reading, one year a step: `MileageChart.yearsOnRecord`, a part year counting whole), the distance really covered up (`swap.actual`, so a cluster swap doesn't drop the line),
  a service filled and an update hollow, a reading to check ringed in the error colour. **The line runs through
  every reading, but only readings with room get a mark** (`MileageChart.marked`, one per 14dp): a reading to check and the
  odometer's always, then services, then updates, so a reading a week stays a line and not a smear of dots. "About 12,400 mi a
  year" is `ReminderEngine.milesPerDay` over the range's readings in dash terms, the reminders' own pace,
  × 365.25 and rounded (`MileageChart.perYear`); none with readings under a week apart. Tapping a point
  shows it and opens what its row opens. TalkBack gets a summary of the chart; the list reads each reading.
- "Read dash" on that sheet (`ReadDashScreen`, `TextReader.digits`, `DashReading` in `:domain`) points the camera
  at the cluster. It shares the VIN scanner's camera set-up, guide band and colours (`CameraScreen`,
  `CameraFeed`, `CameraTopBar`, `guideBand()`). Each frame's band goes to Tesseract (digits, point and comma only; sparse text; a dark
  frame is turned into its negative, since most odometers are lit digits on black), words under
  70% confidence are dropped (a VIN keeps every word, since its check digit vouches for it), and `DashReading.pick` chooses the number. **With a last
  reading, only a number from it to 200,000 miles past it is ever offered**; that is what keeps a trip
  meter, the clock or a stray digit read out of the trim from being put forward, and a dash that really
  reads lower is typed by hand. With none, the longest whole number. A reading is shown only when three
  of the last five frames agree (`DashReading.steady`), and **it only fills the sheet's field**: the
  screen says "check it against the dash", never "detected", the sheet says where the number came from
  until it is changed, and nothing is saved until the user taps Save. The reading comes back through the
  nav host's results (`DASH_READING`), as a scanned VIN does. Frames are never stored.
  Tesseract's language file is copied from assets to `cache/tesseract/` on first use, which `DataWipe`
  already clears with the cache. `TextReaderTest` (on a device) draws readings and VINs and checks the engine
  reads them; how well a real cluster reads, seven-segment LCDs above all, is only known from real dashes.
- Garage list and Reminders tab share `GarageOverview` (app) for every vehicle's reminders. Reminders
  groups come from `ReminderPresentation.grouped`.
  `LogServiceRoute.scheduleItemId` opens Log service with that reminder's work ticked.
- A snooze (`Snooze`, `Snoozing` in `:domain`) lives in `schedule_items.snoozedUntilOdometer/Date` and ends
  at whichever comes first. It only holds a due reminder back: no service record is written, so the
  reminder returns with its real overdue distance. "Skip this one" is a snooze to the next interval, and
  "with the next oil change" stores the oil change's due point at the time of snoozing. A stale snooze
  on a reminder that isn't due is ignored (`VehicleReminders.statuses`), so nothing clears it on logging.
- Reminder rules: the default rule and lead time (`Settings.lead`) live in DataStore; pass `lead` to
  every `VehicleReminders.statuses` call. Overrides are `schedule_items.ruleOverride`. The preview card
  only ever uses a real logged service (`RulePreviews`), never sample data, and names no manufacturer.
- The unlock screen (`LockScreen`) is what sits behind Android's own prompt and what is left when the
  prompt is dismissed. It asks once on arrival; after a cancel the user taps the ring. What it offers
  comes from `Unlock.options` (domain): fingerprint wording only where the phone has a sensor, a padlock on
  a phone with only a screen lock, and "Use screen lock instead" only on Android 11+, where Android can ask
  for the screen lock alone. Errors go through `Unlock.message`; a cancel says nothing. With the screen
  lock allowed as a fallback, Android handles too many failed tries itself by switching to the PIN.
- The check itself is `rememberScreenLockCheck` (`LockScreen.kt`), used for unlocking and for Delete all
  data. On Android 11 and later it is androidx's `BiometricPrompt`. **On Android 10 it opens the system's
  confirm-credentials screen directly** (`KeyguardManager.createConfirmDeviceCredentialIntent`) and takes
  the answer as an activity result: with `BiometricPrompt` there, the system screen stops the activity,
  androidx.biometric 1.1.0 cancels the check on the stop, and a correct PIN after "Use PIN" unlocked
  nothing. That screen still offers the fingerprint first. Because it covers the app, the app counts as
  backgrounded while the user unlocks it, and the answer arrives just before the app hears it is back, so
  `AppLock.unlock()` also forgets when the app was backgrounded; otherwise the return locks it again.
  Don't move Android 10 back to `BiometricPrompt` without testing the PIN fallback on an API 29 emulator
  (`Pixel_API_29`): fingerprint, "Use PIN", too many wrong fingers, cancel, and PIN with no fingerprint.
- Navigation (checked against `design/screens/NavMap.png`) is Navigation 3 (#237): the app owns the back
  stacks and `NavDisplay` shows them. Four tabs, each its own `NavBackStack` (`Navigator` in
  `GloveboxNavHost`); a tab comes back as it was left. The fourth is labelled Settings, because it opens
  Settings and not a menu. What is shown is the current tab's stack over the Garage list, so System Back
  from a tab's first screen goes to Garage (whose own screens are dropped, as before), then leaves the
  app. A notification's destination is pushed onto whichever tab is showing, so Back returns there. Add a
  destination with `screen<Route>` in `GloveboxNavHost`, never `entry<Route>`: screens draw no background
  of their own, and `screen` puts the page colour behind them so nothing shows through during a held back
  swipe. It also hands the route to the screen's ViewModels (`ProvideRoute`), which read it with
  `savedStateHandle.route<…Route>()` (`Navigation.kt`); Navigation 3 doesn't do that itself. Every route
  is a `@Serializable` `NavKey`, and one without fields is a `data object`, whose name stays the same
  across a restart (a plain object's would not, and its saved state would be lost). A screen hands a
  value back to the one under it through the host's `results` (`SCANNED_VIN`, `DASH_READING`,
  `CONFIRM_DELETE`), which are saved with the stacks. Predictive back is switched on in the manifest.
  Screen state that should survive Android restarting the app goes in `rememberSaveable` or a
  `SavedStateHandle`, as History's vehicle and category choice do.
- While locked, the app's screens are out of composition (`MainActivity`), so no screen, sheet or dialog
  is drawn or readable behind the lock screen. Their saved state is held in a `SaveableStateHolder`, which
  is what returns the user to the same screen after unlocking. **The screens' ViewModel stores live there
  too** (`screenModels`, one per tab): made inside the nav host, Navigation 3 would clear every screen's
  ViewModels each time the lock took the host away, and a half-filled form would go with them. Don't swap
  this for drawing the lock screen over a live nav host: sheets and dialogs are separate windows and would
  sit on top of it.
- Receipt and parts photos: files are encrypted in `PhotoCrypto.photoDir` by `RecordPhotoStore` and
  belong to a record through the `photos` table. `RecordPhotoPicker` (app, singleton) owns taking and
  picking; its launchers are registered in `MainActivity` for the same lock-screen reason as the vehicle
  photo. A form's new photos are "drafts" held by the picker until the form is saved (`takeDraft`) or
  left (`discardDraft`).
- **Photo files are never deleted when a record or photo row is.** Deleting or editing a record removes
  rows only, so Undo can put them back; the files go in `RecordActions.discard`, which the nav host calls
  when an Undo snackbar ends untaken. Anything missed (the app closed first) is removed by
  `RecordPhotoStore.sweep` on the next launch: encrypted files no row points at and older than ten
  minutes, plus everything in the two plaintext cache folders.
- A photo is in the clear in only two places, both in the private cache and listed in
  `res/xml/file_paths.xml`: `capture/` while the camera app writes a new one (deleted as soon as it is
  encrypted) and `shared/` for the copy handed to the share sheet (emptied before the next share and on
  launch). A document uses the same two.
- Documents (#168, `documents` table, schema v25; `VehicleDocuments` in `:domain`, `DocumentStore` in
  `:data`, `DocumentsScreen` and `DocumentPicker` in app): a vehicle's paperwork, a PDF or a picture
  (picked, or a photo taken of the paper), each with a `DocumentType`, a name and an optional date.
  **A document is encrypted with the photo key in `PhotoCrypto.photoDir`, named `document-…`**, so
  `RecordPhotoStore.sweep`, `BackupWriter`, `BackupRestorer` and the crypto-erase cover it as they
  cover photos; any new place that lists photo files must list `DocumentDao.allFiles` too. A PDF is
  kept as it came; a picture is redrawn as a JPEG of at most `VehicleDocuments.MAX_EDGE`, which drops
  its metadata. Files are read into memory up to `MAX_BYTES` (20 MB). **A document opens inside Glovebox**
  (#224, the user's decision of 2026-10-16, reversing "handed to another app"): `DocumentViewerScreen`
  (`DocumentViewerRoute`) shows a picture with the photo viewer's `ZoomableImage` and a PDF with Jetpack's
  `androidx.pdf` (`pdf-compose`'s `PdfViewer`, `pdf-document-service`'s `SandboxedPdfLoader`). The PDF is
  rendered in an **isolated process** of the app (`PdfDocumentServiceImpl`, no network, no files), which
  starts `GloveboxApp` too, so **`GloveboxApp.onCreate` does nothing in an isolated process**; keep anything
  new there behind that check. `DocumentStore.openPdf` hands the renderer a descriptor of the decrypted
  PDF: an anonymous memory file (`memfd_create`) on Android 11+, so nothing touches disk; Android 10 has no
  such call, so there it goes through `cache/viewing/`, deleted as soon as it is open and emptied by the
  sweep, never exported by the FileProvider. The document is opened in composition and closed on leaving
  it, so locking the app closes it. `pdf-ocr-play-services` (OCR through Play services) must never be on
  the classpath; check `releaseRuntimeClasspath` on an upgrade. "Open in another app" and Share still hand
  a decrypted copy in `shared/` to another app (`ACTION_VIEW` / `ACTION_SEND`), only when chosen. The launchers live in
  `MainActivity` for the lock-screen reason; a new document opens its details dialog, whose "Don't keep
  it" removes it. Deleting is a confirmation with no Undo. An export lists them (type, name, date; its
  own switch, `ExportOptions.documents`) and never carries their contents. Delete vehicle counts them. Taking a photo needs the camera permission because the app declares it for VIN scanning.
- Export: `Exports.document` (domain) applies the options once (dates, costs and notes, schedule and
  recalls, photos), so the Markdown writer (`ExportText.markdown`) and the PDF writer (`PdfExport`, app,
  Android's `PdfDocument`, US Letter) only lay out what they are given. `ExportSheet` is one composable
  used from History, Settings, Vehicle details' sold sheet, the delete screen and the archive card.
  The file is written to `cache/export/` and copied to wherever the user picks through `ExportSaver`,
  whose "save as" launchers live in `MainActivity` (same lock-screen reason as the photo pickers); the
  working copy is deleted either way. Android's PDF writer stores photos without JPEG compression, so
  they go in at 1,000 px and the sheet's size figure is an estimate (`RecordPhotos.PDF_BYTES_PER_PHOTO`).
  An export is not encrypted, and every recall or schedule table carries a note saying what it is.
- Launcher shortcuts (#235, `res/xml/shortcuts.xml`, `ShortcutActivity`): Log service and Update mileage,
  static, with generic labels (a launcher shows them, and Kvaesitso's search finds them by their long
  labels, without Glovebox unlocked), so **no per-vehicle dynamic or pinned shortcuts**. They open
  `ShortcutActivity`, never `MainActivity`: Android starts a static shortcut with `CLEAR_TASK`, which would
  end the running app and lose a half-filled form, so that trampoline has a task of its own
  (`taskAffinity=""`) and forwards a `Destination.Shortcut` the way a notification does. The nav host
  then opens the only active vehicle's Log service or update-mileage sheet, asks which one
  (`ChooseVehicleDialog`, year, make and model with the nickname under it) when there are several, and
  shows the garage when there are none; the welcome on a fresh install drops it. A shortcut names its
  app by package, so the debug build has its own copy in `src/debug/res/xml/`; `ShortcutsTest` holds the
  two the same and every `ShortcutTask` covered.
- Home-screen widget (#238, `widget/GarageWidget.kt`, Glance; `HomeWidget` in `:domain`): each active
  vehicle (at most five, then "N more") with its garage chip and "Next:" line. **It works nothing out:**
  rows come from `GarageOverview`, worded by the same `Attention.label` and `ComingUp.nextLine`
  (`GarageWords.kt`) as the garage list, so the two can't disagree. **Off by default**, because it shows
  the garage outside the app lock: `Settings.homeWidget`, a switch on Notifications (with "Add to home
  screen" only where `isRequestPinAppWidgetSupported`; Kvaesitso can't pin), kept on restore like the
  lock settings. The receiver is declared `android:enabled="false"` and `GarageWidgets.follow` sets it
  from the setting on every launch (`GloveboxApp`), so it isn't in the launcher's picker while off, turning
  it off removes placed widgets, and Delete all data (which removes the setting) turns it off on the
  restart. While on, `GloveboxApp` redraws it whenever `GarageOverview`'s garage changes (any record,
  reading, schedule, setting or restore), and `DailyCheckWorker` once a day for what falls due with the
  date; `updatePeriodMillis` is 0. A row opens `Destination.VehicleHome` through `PendingDestination`, so
  the lock comes first; nothing on it acts. System font, canvas colours by the phone's light or dark
  (`LightScheme` / `DarkScheme`, `statusColors(dark)`), corners from `system_app_widget_background_radius`.
- Notifications: two WorkManager jobs, kept in step with the settings by `GloveboxApp` through
  `NoticeScheduler`. `DailyCheckWorker` runs at about the chosen time and uses no network; what it says
  is decided by `NoticeRules.daily` (domain): a service is announced once when it enters its lead time,
  an overdue one is nudged weekly, a snoozed or never-logged one stays quiet, and a mileage check-in is
  asked for when the newest reading is older than the cadence: the vehicle's own (`vehicles.mileageCheckInDays`,
  `VehicleCheckIn`, set on Vehicle details: null follows the default, 0 never asks) or the default on
  Notifications (`NoticePrefs.mileageEveryDays`, any whole number of days from 1 to 365; the old three-way
  `mileage_cadence` setting, and a backup that has it, reads as its days). The check-in can have a time of
  its own ("Check-in time", `NoticePrefs.mileageHour`/`mileageMinute`, for when the user is in the car): then
  `DailyCheckWorker` is a second job that runs only `NoticeRun.MILEAGE`, the first runs `SERVICES` (and the
  backup reminder and widget), and each writes only its own part of the log. `NoticeLogStore` (its own DataStore file,
  ids and dates only) remembers what was said; it is updated even when a switch is off or Android blocks
  notifications, so turning them on later doesn't release a backlog. `RecallCheckWorker` needs a network,
  retries silently, and only announces campaigns added after a vehicle's first check. Both skip archived
  vehicles. The workers are plain `CoroutineWorker`s that reach Hilt through `NotifyEntryPoint`.
- "Check for new recalls" (`Settings.recallCadence`) is honoured in `RecallRepository.refreshIfStale`, so
  it governs the in-app checks as well as the job; Never leaves only the Refresh button.
- A notification carries a `Destination` on its intent. `MainActivity` puts it in `PendingDestination`
  and the nav host, which only exists once the app is unlocked, takes it from there, so nothing is drawn
  before unlock. Collect that flow; don't key an effect on it (taking it clears it, and a keyed effect
  cancels its own vehicle lookup on a cold start). `DestinationViewModel.take` does its database lookup
  **before** clearing it, and the nav host acts on the result without suspending: a tap while the app is
  in the background reaches a host the lock is about to take away, which cancels the lookup, and the
  destination must still be waiting after unlock. A task begun by such an intent has it at its root, which
  the launcher's doesn't match, so a later tap on the icon would stack a second `MainActivity`; that copy
  finishes at once in `onCreate` (`!isTaskRoot` and a launcher intent). Each kind has its own channel, and the lock-screen
  version of a notification says only what kind it is.
- Backup: the file format is specified in `docs/backup-format.md` and implemented in
  `data/.../backup/BackupFormat.kt`, plain JVM code with its tests in `BackupFormatTest`. Change the two
  together, and never change how a version-1 file is read. Argon2id (BouncyCastle's lightweight API,
  64 MiB, 3 passes) turns the password into a key; the contents are AES-256-GCM in 64 KiB blocks bound to
  the header, their position and whether they are the last. `BackupWriter` reads every table generically
  from SQLite, so new columns are backed up without touching it, and re-encrypts photos out from under
  this phone's key. The password is never stored. The last-backup date is recorded only after Android's
  "save as" has really written the file (`ExportSaver.save` with `onSaved`). The reminder is part of
  `DailyCheckWorker` (`BackupRules.shouldRemind`).
- Restore (`BackupRestorer`) is all or nothing: the whole file is read and authenticated first, with
  photos staged under this phone's key in `PhotoCrypto.restoreDir`; then every table is replaced in one
  Room transaction (foreign keys deferred, rows inserted by whatever columns the backup has); then the
  staged photos are moved in. `finishInterrupted` completes that last step on launch if the app died in
  between, and must run before the orphan sweep. App-lock settings are never restored. The sheet lives at
  the nav host and shows whenever `RestorePicker` holds a picked file, so it survives the app locking
  while the file picker is open. Replacing existing data is confirmed by typing REPLACE.
- The welcome (`WelcomeScreen`, `FirstRun`) is shown by `MainActivity` in place of the nav host, before
  the lock, when `Welcome.shouldShow`: the `welcome_seen` flag is unset and there are no vehicles. An
  install that already has vehicles gets the flag set without seeing it. The flag is not backed up, and
  `DataWipe` removes it with the rest of the settings. Its buttons save the two choices, then hand over through
  `Destination.AddVehicle` or `RestorePicker.pick()`. Notifications off there turns every kind off in
  Settings; the lock switch is disabled, and says why, on a phone with no screen lock (`canLockApp`).
- Settings rows that lead nowhere are left out, not shown dead. `Maintenance schedule` opens the only vehicle's schedule, or asks which when there are several,
  and is absent with none. `Lock the app` (`LockDelay`, stored as `lockAfterSeconds`, -1 = on restart)
  shows only while the lock is on and the phone has a screen lock. The privacy line says what is really
  sent: a VIN, or a year, make and model.
- About (`ui/about`): the version comes from the installed package. `PrivacyScreen` states what is kept,
  what is sent and what never happens (its lines are the `privacy_*` string arrays in `strings.xml`); every line has to stay true of the code, so update it with any
  change to the network, permissions, backup or export. The copy names a maker's document as a source only for a
  bundled list, by that list's own `source.name`.
- Open-source licences: the AboutLibraries Gradle plugin (build time only, no runtime library) writes the
  release dependency list with licence texts to `R.raw.aboutlibraries`; `LicenceCatalog` reads it and
  `Licences.grouped` (domain) arranges it, so the list can't fall behind the build. A library whose
  licence is its own (SQLCipher, Bouncy Castle) comes without wording: keep a copy in
  `app/src/main/assets/licences/<group>_<artifact>.txt` and add one for any new such dependency. The app's
  own GPL text is copied from the repo's `LICENSE` at build time (`copyAppLicence`).
- Delete all data (`DeleteAllScreen`, from the bottom of Settings): typing DELETE enables the button,
  which then asks for the fingerprint or screen lock (`confirmWithScreenLock`); a phone with no screen
  lock has only the typed word, and the screen says so (`DeleteAll` in `:domain`). `DataWipe.deleteEverything`
  writes a marker file first, destroys both Keystore keys, then deletes the database, photos, settings and
  cache; the app then restarts its process and lands on the welcome. `DataWipe.finishInterrupted` runs
  in `GloveboxApp.onCreate` before Hilt builds anything and completes a wipe the process didn't survive:
  without it a database whose key is gone could never be opened. Anything new the app stores on disk has
  to be added to `DataWipe.erase`.
- All service-log writes go through `RecordActions` (app), which emits the events behind the
  nav-host snackbar and its Undo. Editing reuses the Log service screen (`LogServiceRoute.recordId`).
- A new service, a delete and a restore all end on the History list (`showHistory` in the nav host), and
  `RecordActions.focus` tells History which vehicle to show and which record to outline ("just added" /
  "restored"); History clears it when it is left. An edit returns to the record. The edit form's "Delete
  this record" goes back to the record's screen, which owns the confirmation sheet.
- History sorts by date or mileage, either way round (`HistoryOrdering.sorted`, `HistoryOrder` in the saved
  state, kept across vehicles): Material's sort icon opens the choice and the arrow beside it reverses it (down
  is newest or highest first). Both show only with two records or more. Mileage is compared in dash terms, ties
  go to the other key and then the id, and a new order scrolls the list back to the top.
- History's spending line (#285, `Spending` in `:domain`): under the subtitle, "$48.50 this year · $138.45 since
  May 2025", from records with a cost only, saying "from 2 of 3 records" when some have none; no line with no costs.
  "This year" is today's calendar year and is left out when every cost is from it. **Hidden while a category filter
  or a search is on**, since a record's cost covers all its lines. The export's total is the same sum (`Spending.total`).
- History's search (#284, `HistorySearch` in `:domain`): a record matches when a line's label as typed, a catalog
  line's name as shown now (`catalogLabel`), the notes or the shop contains what is typed, ignoring case and the spaces
  round it; it works with the vehicle and category choice. The text is in the saved state like them, so it survives a
  restart, and goes with the History entry (a save, delete or restore landing on History clears it). Nothing is stored.
- Log service's shop field (`ShopField`, `Shops.suggest` in `:domain`) offers up to five shops from every vehicle's
  records (`ServiceDao.observeShops`, imported ones too), narrowed by what is typed, most recently used first, one per
  name ignoring case and the spaces round it, each as last typed. What is typed is saved as typed unless a suggestion
  is tapped. The recall "mark done" sheet doesn't offer them.
- An Undo bar stays for `LogEdits.UNDO_SECONDS` (30), and the delete sheet says so. Events are collected
  with `collectLatest`: a newer change replaces the bar for an older one, and whatever ends a bar without
  Undo (timeout, replacement, the app locking) discards the photo files it was holding.
- Recalls live in the `recalls` table, one row per vehicle and campaign. `RecallRepository` re-checks a
  vehicle when its list is a week old (`vehicles.recallsCheckedOn`): once per launch from the garage,
  and on opening vehicle home or the Recalls screen. A failed check leaves the stored list alone.
  "No open recalls" is only shown once NHTSA has answered for that vehicle.
- NHTSA answers HTTP 400 with an empty list when it has no recalls for a vehicle; `NhtsaClient.recalls`
  reads the body rather than the status. Report dates arrive as day/month/year.
- `recalls.archivedOn` (schema v23) is the user saying a recall doesn't apply to this vehicle (no notice came,
  the maker's VIN lookup doesn't list it): an archived recall isn't open (`StoredRecall.open`), so it leaves
  the count, banner and garage chip, sits under its own "Archived" filter on the Recalls screen, and "Move
  back to open" clears it. Only an open recall can be archived. Exports say "Archived as not applying".
  NHTSA dropping the campaign deletes it, as for an open one.
- `recalls.completedOn` is the user's mark and survives refreshes; a campaign NHTSA drops is deleted only
  while open. Marking done goes through `RecordActions.completeRecall`, which can add a service record
  (`recalls.serviceRecordId`) whose line has no schedule item, so it resets no reminder. Mark as open
  removes that record, with Undo.
- Manufacturer VIN lookup pages are a short checked list (`Recalls.manufacturerSite`); other makes get
  NHTSA's lookup. "Find a dealer" hands "<make> dealer" to the maps app. With nothing open, the way in
  is the Recalls row on Vehicle details.
- "What it takes" on Vehicle details is three rows, each with its own editor that checks entries as they
  are typed (`VehicleSpecEntry` in `:domain`) and stores one standard form: **engine oil** (SAE grade,
  picked from chips or typed and normalised to "5W-30"; the approval the manual names; capacity with
  filter in quarts or litres; filter part numbers, up to three), **tires** (a sidewall size checked for
  width, aspect ratio, rim, load index and speed rating, or a flotation size; a separate rear size only when
  it differs) and **wiper blades** (driver, passenger and rear lengths in whole inches, 10 to 34, plus part
  numbers or type). Save is offered only when every filled-in field is valid; empty clears it. Text entered
  before this (schema v21) stays in `oilSpec`, `tireSize` and `wiperBlades` and the editor asks for it to
  be put right. The one-line summaries feed Log service's hints. Nothing is looked up, and nothing is
  suggested from the model except from a named maker's list (next note but one).
- Past oil, tires and wipers (#166): transmission fluid, differential and transfer case oil (a gear grade is
  normalised, "75W-90 GL-5", anything else is taken as the fluid's name), coolant, brake fluid (DOT grade),
  air filters, spark plugs (part number, gap in inches or mm), battery (BCI group, CCA) and headlight
  bulbs (codes). **`WhatItTakes` and `TakesItem` in `:domain` are the one model:** each row's one-line
  summary, which rows go where (oil, tires and wipers on Vehicle details; the rest on their own page,
  "Fluids, filters & parts" (`FluidsPartsScreen`), laid out like Torque specs at the user's request: a card
  for each entered, then one per item still to add, each opening its editor in one tap),
  the part numbers a long press copies, Log service's hint per catalog line (`hint(catalogId)`), and the
  export's lines. Map a vehicle row to it with `VehicleEntity.whatItTakes()`. Tires carry the door
  placard's cold pressures (psi or kPa, front, rear, spare). Exports have a "What it takes" section before
  the records, with its own switch on the export sheet (`ExportOptions.whatItTakes`). Schema v22.
- Known "What it takes" (`KnownTakes` in `:domain`, `KnownTakesUi.kt` in app, `docs/known-takes.md`, schema
  v26; the user's decision of 2026-10-18 to allow named lists, as torque has). **A list is what a maker's
  own documents print for one model and run of years, every figure cited by document and page in the doc;
  never a parts site or a compilation.** `docs/known-takes.md` also records where each maker publishes its
  manuals (Toyota, Subaru, Honda, VW, Mazda), for adding the next. A file in `assets/known-takes/` gives
  `takes` (the whole model) and `variants` (a drive or a gearbox, used only for a vehicle **known** to have
  it: a fluid for the wrong gearbox is worse than an empty row), every value in the stored form its row's
  editor writes (the reader refuses anything else). Several tire sizes are only offered as chips in the
  tire editor, never filled; a figure given as a range or alternatives is left out. "Fill in from the
  manual for the …" on Vehicle details and Fluids, filters & parts fills only **empty rows, each whole**
  (`KnownTakes.plan`, `VehicleDao.fillTakes`), records `vehicles.takesKnownList` and tags each filled row
  DOUBLE-CHECK (`vehicles.takesToCheck`); the source is named once per screen while any is tagged. The
  tag clears when the row's editor is saved with "I've checked this against my manual" ticked or the row
  changed (`KnownTakes.stillToCheck`, `KnownTakesRows.saved`). The lists that ship: the 4Runner 2017–2024
  (three lists: the oil approval changed for 2022, the bulbs for 2021) from Toyota's owner's manuals, and
  the Mk6 GTI from Volkswagen's fluid charts filed with NHTSA (2013, 2014; 2010–2012 has only the oil
  capacity, from VW's job aid), since VW's manuals for those years are VIN-only.
- Maker's documents (`MakerDocuments` in `:domain`, `MakerDocumentsUi.kt` in app, `assets/maker-documents/`,
  `docs/known-takes.md`): Vehicle details' "From the maker" card links each covered year's owner's
  manual, warranty and maintenance guide and fluid chart, which **open in the browser; the app fetches
  nothing** (the privacy screen's "When you leave the app" says so). Links may only point at a host in
  `MakerDocuments.hosts` (the makers' own servers and `static.nhtsa.gov`), and a year has one document
  of a kind unless the maker splits it by `edition` (the 4Runner's 2022 manual, by production date).
  Which code is which year comes from the maker's own data (Toyota's `downloadableManuals` endpoint).
  A row with `vinLookup` (Volkswagen's digital manuals, 2012 on) opens the maker's VIN page and copies the
  VIN, marked sensitive, for the owner to paste: **the VIN never goes in a link and the app never sends it
  to a maker** (Volkswagen's manual link needs a token got by sending the VIN, so it isn't built).
- License plate (schema v24, `vehicles.plate`, `.plateRegion`, `LicensePlates` in `:domain`): a row under the
  nickname on Vehicle details opens a dialog with the plate (capitals, letters, digits, spaces and dashes,
  `LicensePlates.MAX`) and a dropdown of U.S. states, D.C., territories and Canadian provinces, stored as the
  two-letter code. Kept on the phone and never sent (the privacy screen says so); exports give it under the
  VIN (`ExportVehicle.plate`).
- Clear service history (`ClearHistoryScreen`, from Vehicle details): deletes the vehicle's service records
  and their photos, **imported ones only** by default (`service_records.imported`, for importing a Carfax
  report again clean) or all of them, confirmed by typing DELETE, with no Undo. `DataWipe.clearHistory`;
  everything else about the vehicle stays (what it takes, torque specs, schedule, recalls, milestones,
  mileage updates), and reminders count from what is left. A recall marked done keeps its mark.
- A long press on the VIN row of Vehicle details copies the VIN (`copyVin`), marked sensitive
  (`ClipDescription.EXTRA_IS_SENSITIVE`) so Android hides it in its clipboard preview; Android 13+ says
  "Copied" itself, earlier versions get a toast. The row also has an accessibility long-press action.
- Vehicle details opens from the overflow button on vehicle home. Its editable fields are written one
  column at a time (`VehicleDao.setNickname` etc.), never with a whole-row `@Update`, so they can't undo
  a recall check that landed meanwhile. Rows with nowhere to go yet are left out rather than shown dead.
  Maintenance schedule always opens `ScheduleScreen`.
- Odometer offset (`ClusterSwap` in `:domain`, `OdometerOffsetSheet`): an instrument cluster replacement is
  `vehicles.clusterSwappedOn`, `.odometerOffset` (old reading minus new, so it can be negative) and
  `.clusterStartReading`, in force only when the date and start reading are both set. **Every reading stays
  stored exactly as typed**, so setting, correcting or removing a swap rewrites no record. Everything
  computed is in "dash terms", what the cluster now in the dash reads: readings dated before the swap have
  the offset taken off on the way into `:domain`, which makes due points and the current odometer directly
  comparable with the dash. So never hand `record.odometer` or an `odometer_readings` row to `:domain`
  raw: go through `toEntry(swap)` and `toReadings(swap)` (`Mappers.kt`), the second of which also adds the
  new cluster's first reading. A typed reading compared with those goes through `swap.toDash(value, date)`.
  A past figure that comes back out of `:domain` (last done) is shown as it was read that day with
  `swap.asReadOn`; records themselves show what was typed. The swap day belongs to the new cluster.
  Vehicle home and exports also give the actual mileage (`swap.actual`), and an export explains the jump.
  Saving a swap clears that vehicle's snoozes that end at a mileage (`VehicleDao.setClusterSwap`).
  `DailyCheckWorker` passes last done as read, so `NoticeRules.cycle` doesn't change and re-announce.
- Archiving is only `vehicles.archivedOn` (the sold date), set through `VehicleArchiver` (app); nothing is
  deleted. Screens that list the garage read `observeActive`, so archived vehicles get no reminders, and
  `RecallRepository` refuses to check them. A final odometer higher than the last reading is logged as an
  `odometer_readings` row dated the sale (`Archive.finalReading`); Undo removes it, "Move back to garage"
  keeps it. Archived history reuses `HistoryScreen` via `ArchivedHistoryRoute`, and `RecordDetail.readOnly`
  hides Edit and Delete.
- Delete vehicle (`DeleteVehicleScreen`) opens from Vehicle details and the archive card, lists what goes
  with counts, and enables the red button once the nickname or model is typed (`TypedConfirmation`). It
  calls `DataWipe.deleteVehicle` and has no Undo. `RecordActions.undo` checks the vehicle still exists, so
  an Undo snackbar left over from before the delete does nothing instead of failing.
- Torque specs (`TorqueSpecsScreen`) start empty. "Start with common fasteners" adds named rows from
  `TorqueSpecs.common` with no size, value or note: Glovebox has no factory torque values and none may be
  invented. The only values it ships are in a named list (next note). `torque_specs.category` and `.source` hold `TorqueCategory` and `TorqueSource` names.
  `FACTORY` means the user says they copied the value from the factory service manual, and is only kept
  while the row has a value. Torque is typed in any unit (`TorqueSpecs.parse`, capped at 1,000 N·m) and
  stored in N·m. The unit switch starts at the Settings unit and doesn't change it.
- Known torque specs (#76, `KnownSpecs` in `:domain`, `KnownSpecCatalog` in app, `docs/known-specs.md`).
  **The rule is: a torque value ships only as part of a list that names its source, records why Glovebox
  may ship it, and whose every value is tagged to double-check. None is Glovebox's and none is presented
  as the manufacturer's.** A list is a file in `assets/known-specs/`: an id, the make, model and year range
  it is for, its source (name, optional URL, the day it was copied, a sentence on what kind of source it
  is, and `basis`) and items written exactly as in a `torque-specs` import file, read by the same strict
  reader. **The lists that ship are the 4Runner's, 2010 to 2024, and hold only what Toyota's own owner's
  manuals print:** the wheel nut torque (which changed twice, hence four lists by year) and, from 2017,
  the spare tire carrier. `docs/known-specs.md` records which Toyota documents were read and how. A fuller
  4Runner list copied from an enthusiast site was built first and taken out before merging: the site gave
  no permission to reuse its table, the app may one day be sold, and its wheel nut figure turned out not
  to be the manual's. **`basis` has to be a
  real answer** (written permission, or values the maintainer took from a manual they have); "it was on a
  website" is not one. Don't add rows from a source that doesn't state units, and don't correct a figure
  from memory: leave it out or note the dispute. `KnownSpecsTest` holds every file in the folder to these
  rules and tests the logic on a made-up model with made-up values.
- `KnownSpecs.match` offers a list only for its make, model and years; no near misses. "Add known specs for
  …" is shown on Torque specs while the list has something the vehicle lacks, with `offer` (`TorqueText.kt`) saying
  whose values they are every time. `KnownSpecs.plan` adds new rows and fills rows that are only a name; a
  row the user has put anything in is never written over, and there is no "replace". Every value arrives as
  `TorqueSource.REFERENCE` (tag DOUBLE-CHECK) with `torque_specs.knownList` set to the list's id.
  `knownList` is permanent: the row always says "Started from the … list", whatever happens to it later.
  The tag clears through the editor (`KnownSpecs.sourceAfterEdit`): "I've checked this value" or a changed
  figure makes it YOUR ENTRY, "From the factory service manual" makes it FACTORY MANUAL. The first of those
  exists because few owners have the factory manual. Editing the
  socket or note alone clears nothing. A row saved through the editor keeps `knownList` (the view model
  copies the stored row). An unconfirmed reference value is left out of a saved import file and listed as
  such (`TorqueExport`), since a file would pass it on as the user's own. Once added, a row is the user's:
  an import may replace it like any other, and it keeps `knownList`. The list's source is credited on the
  About screen from the catalog.
- The vehicle photo is picked with the system photo picker (no permission), redrawn as a JPEG of at most
  1600 px, which drops its metadata, and encrypted by `VehiclePhotoStore` into `PhotoCrypto.photoDir`
  (`vehicles.photoFile`). The launcher is registered in `MainActivity` and answered by
  `VehiclePhotoPicker`, because returning from the picker can lock the app and the lock screen replaces
  the nav host.
- Schema changes so far are Room auto-migrations (v20); keep exported schemas in `data/schemas`, set by the
  Room Gradle plugin (`room3 { schemaDirectory(…) }` in `data/build.gradle.kts`, one folder for every variant).
  Room 3 writes a schema only when it is new or changed; Room 2.8's and Room 3's exports of version 25 are
  byte-identical, so the identity hash every phone's database carries didn't change with the move.
  `MigrationTest` (`:data`, on the JVM with Robolectric, so part of `./gradlew build` and CI) makes a
  database at every exported version, fills every column of every table, opens it as the current
  database and reads each value back. **A new version is covered as soon as its schema file is in the
  folder; a new table has to be added to the test's `order` list**, which the test says if it is missing.
  A hand-written migration, when one is ever needed, is tested by the same test. It opens the database
  through Android's own driver (`AndroidSQLiteDriver`, Robolectric's SQLite), since SQLCipher's native
  library doesn't load on the JVM; the migrations and schema checks are Room's either way.

## Conventions

- Business logic goes in `:domain` with a test; ViewModels stay thin.
- **A composable or helper used by more than one screen lives in `ui/components/`** (`ScreenHeader`,
  `BorderedCard` / `RowsCard` / `LabelledCard`, `FilledCard`, `GloveboxSheet`, `SectionLabel` (a heading), `Chevron`,
  `DatePickerSheet` / `DateButton`, `OdometerField`, `TypedConfirmField` / `DangerButton`, `CheckedField`, `DistanceUnitSwitch`,
  `copyText`), not in the screen that first needed it, where
  the next screen won't find it and writes its own (#297). One used by a single feature stays in that feature's package.
- **All wording lives in `res/values/strings.xml`** (#236). Composables use `stringResource` /
  `pluralStringResource`; callbacks and code outside a composable use `LocalResources` or `AppText.res`
  (never `LocalContext.current.getString` in a composable: lint flags it). Figures go in as arguments that
  already carry their unit (`Int.asDistance(unit)`), counts through a plural. Names are `<screen>_<what>`
  (`unlock_wait`). **`HardcodedTextTest`** (a JVM test in `app`) fails on words typed straight into the
  UI (`Text("…")`, `contentDescription = "…"`), since Android lint only checks XML layouts for that.
- **`:domain` returns no English:** it returns a typed result (an enum, or a sealed type carrying the
  figures, as `UnlockPrompt`, `OdometerNote` or `TakesPart` do) and `app` words it beside the screen that
  shows it, or in a shared `…Text.kt` (`TakesText`, `CatalogText`, `ScheduleText`, `SettingsWords`); its
  tests check the result, not text. Things `:domain` names by id (catalog lines, typical schedule items,
  starter fasteners) are named in `app`, and a name saved into a row keeps the language it was saved in,
  like anything typed. A writer that lays out a whole document stays in `:domain` and is handed its words
  (`ExportText(ExportWords)`; the tests give `EnglishExportWords`). Text from outside (Android's biometric
  errors, NHTSA's components, vPIC's messages, a bundled list's source and note) is passed through as it
  comes. Wording held in saved state is worded once, when it happens. A change to the English is its own
  PR, since `EndToEndTest` finds things by their words. **One exception, on purpose:** an import file's
  error messages (`ImportError.message`, the reasons in `WrittenFile.leftOut`) stay in English in
  `:domain`, with the format they describe: they name its JSON fields and paths (`items[3].torque.unit`),
  and `docs/import/` is the English specification they point to.
- **The app runs in its translation's locale, not the phone's** (`inAppLocale`, `AppText` in app):
  `strings.xml` says which (`format_locale`, en-US), `MainActivity` wraps its context in it, and figures,
  dates and plural forms follow it. So the English app reads "12,345 mi" and "Oct 17, 2026" on every
  phone, and says "21 days" on a Russian one, where Russian plural rules would give "21 day". The
  shared formatters in `Mappers.kt` (`asDistance`, `asNumber`, `longDate`, `shortDate`, `monthYear`,
  `DueHeadline.text`, `Interval.text` / `.orText` / `.every`, `ruleLabel`, the unit words) read
  `AppText.res`, since view models and workers call them too; so does any other wording outside a
  composable. **Never format a figure or date with `Locale.US` or `String.format` for display**, and never
  build a phrase by cutting up another (`removePrefix("In ")`, `.lowercase()`): give it its own resource.
  `FormatsTest` (on a device) holds the English output of the shared formatters and the export's words.
  Fields the user edits are filled in the app's format (`asNumber`, `editableCost`), and what is typed goes
  through `typedNumber()` (whole numbers, costs) or `typedDecimal()` (torque, oil capacity) before a parser in
  `:domain` reads it; both are no-ops in English. Costs are dollars whatever the language (`Money.format`).
- **Adding a language** is a `res/values-xx/strings.xml` with its own `format_locale`: AGP generates the
  per-app language list from the folders (`generateLocaleConfig`, `res/resources.properties`).
- Never show placeholder data as if real; mark sample values clearly.
- Torque/interval reference values are user-verified; UI says "check your service manual". A bundled torque
  value or "What it takes" row is the exception to "user-entered", and it is tagged DOUBLE-CHECK until the
  user has checked it.
- Any Python helper scripts: use `uv run` with inline script dependencies.
- Public repo: never commit real personal data (VINs, records, photos), keystores, `local.properties`
  or `.idea/`. Test fixtures use made-up values.
