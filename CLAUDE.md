# Glovebox — notes for Claude

Private, offline-first Android app for tracking vehicle maintenance. Single user, no account, no
server. Distributed by sideloading via Obtainium from GitHub Releases, not the Play Store.
Public repo, GPL-3.0 (`LICENSE`).

Design canvas (screens, flows, light/dark, nav map):
https://claude.ai/code/artifact/7f521081-34e6-46e9-99aa-641765621ad8

**Local copy of the design: `design/`** — `design/screens/<Name>.png` (renders) and
`design/source/<Name>.dc.html` (exact colors, spacing, copy). Start from `design/README.md` and
`design/screens/NavMap.png`. Before building a screen, look at its PNG and read its source.

This file holds the rules and the decisions behind them. How a feature works is in its code and tests;
the issue numbers lead to the discussion.

## Status

`./gradlew build` and `./gradlew :domain:test` are green (Gradle 9.8.1, AGP 9.4.1, lint included).
Keep them green; lint fails the build on a warning as on an error (`warningsAsErrors`, in `:app` and `:data`),
and there is no baseline: fix a warning, or suppress that one case with a comment saying why (`app/lint.xml`).

## Commands

- `./gradlew :domain:test` — fast pure-Kotlin tests. Run often.
- `./gradlew build` — everything, including the database migration tests (`:data`).
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

### Vehicles

- Adding by VIN (`AddVehicleViewModel`) is two steps: entry, then "Is this your vehicle?", where decoded values
  can be corrected. **A vehicle is never saved without a real year, make and model** (`VinDecode.quality`), so an
  unidentified VIN is fixed or filled in by hand. The drivetrain is picked, never preselected. The vehicle is
  saved before the schedule screen, so Back from there keeps it.
- Adding without a VIN (`ManualAddScreen`): makes and models from vPIC (`makes` sends nothing about the vehicle,
  `models` only make and year); every picker also takes typed text. A VIN added later from Vehicle details isn't
  decoded and changes nothing else.
- VIN scanning (`ScanVinScreen`, `CameraFeed.kt`): barcodes through zxing-cpp, printed VINs through Tesseract.
  **A VIN is only ever a 17-character run that passes the check digit;** nothing slides along longer text and no
  substitutions are tried (either would pass a wrong VIN about one time in eleven). The only repair is I/O/Q to
  1/0. A printed VIN needs two of five frames to agree. Frames are never stored. **Frames follow the display's
  rotation; never set the target rotation from an orientation listener** (a tipped phone then reads nothing).
- Vehicle details' fields are written one column at a time (`VehicleDao.setNickname` etc.), **never with a
  whole-row `@Update`**, which could undo a recall check that landed meanwhile. Rows with nowhere to go are left
  out, not shown dead.
- The VIN is copied (long press) with `copyText`, marked sensitive. A licence plate is kept on the phone and
  never sent.
- Archiving is only `vehicles.archivedOn`; nothing is deleted. Screens that list the garage read
  `observeActive`, so a sold vehicle gets no reminders or recall checks. Its history is read-only.
- Delete vehicle (`DataWipe.deleteVehicle`) has no Undo; `RecordActions.undo` checks the vehicle still exists.
- Odometer offset (`ClusterSwap`): **every reading stays as typed**, and everything computed is in "dash terms"
  (what the cluster now in the dash reads). **Never hand `record.odometer` or an `odometer_readings` row to
  `:domain` raw:** go through `toEntry(swap)` / `toReadings(swap)` (`Mappers.kt`); compare a typed reading
  with `swap.toDash`, show a past figure with `swap.asReadOn`. The swap day belongs to the new cluster.
- Distance units (#93): `UnitDao.convert` is the only thing that changes a vehicle's unit, in one transaction.
  **Two conversions** (`DistanceKind`): a measurement (reading, snooze point, offset) goes exactly at 1.609344; a
  schedule figure (interval, milestone, repeat) goes at 1.6, as manuals print them. Glovebox's own figures are
  kept in miles and given to a km vehicle as schedule figures. **Always take the lead distance through
  `Settings.leadFor(vehicle.unit)`, never `Settings.lead`.** Each vehicle in a list is worded in its own unit.

### Schedule and reminders

- Setup starts from the typical list (`ScheduleTemplates.generic`, labelled typical, not the maker's) **unless a
  bundled maker's schedule covers the vehicle; then that is the schedule** (the user's decision, 2026-10-13).
  Its items become the user's own (`knownList` set) and its table the vehicle's milestones.
- **Never send a vehicle that already has items back through setup**: it inserts every item again.
- Known maintenance schedules (#134, `KnownSchedules`, `docs/known-schedules.md`, which has the rules, and
  `docs/known-schedules/<id>.md`, what was read for each list). **Glovebox has no schedule of its own for any
  model and works none out; a bundled schedule is a maker's published guide, named.** Only facts are taken, in
  Glovebox's words, for only the years whose guides were read and agree. **An interval has to be the table's
  own** (`KnownSchedules.parse` refuses one that isn't). An inspection is a reminder only where the user asked. A
  special-condition job has no `service`.
- "(4WD)" and "(AWD)" jobs (`fourWheelDriveOnly`, `allWheelDriveOnly`) are left out only for a vehicle **known**
  to be two-wheel drive, `manualExcluded` jobs only for one known to be manual, and `manualOnly` jobs only for
  one known to shift itself. **4WD and AWD are different systems** (`DriveKind`): a guide's "(4WD)" on a model
  sold as AWD goes in `allWheelDriveOnly`, named "(AWD)".
  An engine that isn't on record matches no per-engine list; only setup asks which engine. A list's
  `engine.powertrain` (#33's rulings) tells a hybrid or flex-fuel model from its petrol twin of one size;
  `vehicles.powertrain` is set from vPIC only where it says so plainly, **never "gas" from a blank**.
- A vehicle set up earlier is offered "Use the maker's schedule"; there is no replace of the user's own items.
  **A replaced typical item is deleted** (the user found an unticked twin confusing) unless a service was logged
  against it, when it stays untracked. That is the one place a typical item is deleted.
- **A list's source is said once per screen, never under every item** (the user found that too busy).
- `firstMiles` ("first at 100,000, then every 50,000") is distance really covered; **pass the vehicle's swap
  wherever statuses are worked out** (`toTracked(rule, swap)`).
- Severe-only jobs (#136): `toTracked` returns null for them while `useSevere` is off. **One Normal / Severe
  switch, not one per condition** (the user's choice). The editor can't make one.
- **Use `ItemStatus.counting`, not `lastDone != null`, to decide whether a reminder has something to count
  from.** A job the vehicle hasn't reached counts down to its first mileage, by distance only; it is not "Not
  logged yet".
- Which reminders a logged line counts for is **derived, never stored** (`ServiceLinks`): the item it was saved
  against plus every item answering to its `catalogId` (a typical item through `templateKey`, the user's through
  `service`). **Never read `scheduleItemId` on its own:** go through `toEntry(swap, items.answering())`, or
  `HistoryImport.itemIds` for an unsaved record. `templateKey` means "Glovebox's own", which keeps imports off
  those items. `CatalogItem.covers` is one way (a change covers its check); `ServiceLinks.own`, without
  `covers`, decides what a Log service line stands for.
- Never-logged reminders are listed apart as "Not logged yet", above Overdue and Coming up (the user's choice,
  2026-10-13), and left out of overdue counts. They are never given a guessed next-due point; a distance job says
  in amber (never red) where the schedule last put it. **"Up to date" is said only when every tracked job has
  something to count from;** otherwise "N to log". **Every reminder row is one tap from Log service with that
  job ticked** (the user's request).
- A snooze only holds a due reminder back and writes no record. A stale snooze on a reminder that isn't due is
  ignored, so nothing clears it on logging. **Pass `lead` to every `VehicleReminders.statuses` call.**
- The rule preview uses only a real logged service, never sample data, and names no maker.

### Service log

- **All service-log writes go through `RecordActions`**, which drives the snackbar and its Undo.
- Log service: **the odometer is never prefilled** (a prefilled reading gets saved untouched and miscounts every
  reminder); "Use current" fills it on a tap. Future dates are refused. "Due" chips only for reminders that have
  a log entry. Category buttons are tabs, not ticks (the user found the solid "open" button read as chosen).
- "When were these last done?" saves a one-line record with `enteredAsLastDone`; nothing else changes. "Import
  from a dealer record" and "assume everything was done on schedule" are deliberately not built.
- **Photo and document files are never deleted when a row is**, so Undo can put the row back: the files go in
  `RecordActions.discard` when the Undo bar ends untaken, and `RecordPhotoStore.sweep` removes leftovers on
  launch. An Undo bar lasts `LogEdits.UNDO_SECONDS` (10) and has a ✕; whatever ends it without Undo discards its
  files.
- A file is in the clear only in `cache/capture/` (while the camera writes it) and `cache/shared/` (the copy for
  the share sheet), both in `res/xml/file_paths.xml`.
- A new record, a delete and a restore end on History (`RecordActions.focus` outlines the record); an edit
  returns to the record.
- History's spending line (#285) counts records with a cost and is **hidden while a filter or search is on**,
  since a cost covers all a record's lines. Search (#284) and the sort order live in saved state; nothing is
  stored.
- "Add custom item" (#309) only offers lines, never ticks one; filing a custom line under a line **keeps its
  wording**. So Custom stays rare.
- Custom lines and category rules (#310): **a line is Custom only when it has no home** (no `catalogId`, no
  `scheduleItemId`, not a recall's line). History's count and the sort screen use the same `CustomLines.count`,
  so they never seem to disagree. Filing keeps labels as written, in one transaction, one Undo. **A rule is the
  whole wording, never part of it**, the user's, not a vehicle's. Rules apply only where lines come in, never to
  saved records; **a file's own `service` always wins**, and on a Carfax report the rule beats Glovebox's
  wording. A rule whose line a later version drops is kept, shown in red and never applied. Changing a rule
  changes no record.
- Guesses (#312, `LineGuess`, `LineWords`): **asked last** (file's `service`, then rules, then
  `CarfaxReport.service`, then this) **and only ever a suggestion labelled a guess, never picked for the
  user.** A check line needs the text to say checked or inspected, and no other line takes text that says only
  that ("Water pump checked" must never reset the pump). A known word is never fuzzed ("lower" is never
  "blower"). **A new catalog line needs its words in `LineWords`** (`LineGuessTest` checks). The honest figure is
  the unseen set in `LineGuessTest`. Nothing is looked up.
- Shop suggestions come from every vehicle's records; what is typed is saved as typed.
- Visits (#307, `Visits`, `service_records.visitId`): **a visit only groups records.** Each keeps its own lines,
  flags, photos, recall link, cost and Undo, and reminders and spending are per record. **Nothing is linked for
  the user;** `Visits.offers` is only an offer. An id held by one record alone means nothing, so deleting needs
  no tidying. Two readings in one visit are both shown, never settled. Link and unlink have no Undo. Exports lay
  a visit out as one entry; a record on its own is written exactly as before.
- Clear service history deletes imported records only by default, or all; typed DELETE, no Undo.

### Mileage

- Update mileage adds a reading dated today, with Undo. A reading far above the last
  (`MileageReadings.farAhead`) or lower turns Save into "Save anyway": **a question, never a block.**
- **The odometer is the highest reading on record**, so one slipped digit makes every reminder overdue; Mileage
  readings (#65) is where it is fixed. **Nothing is ever decided wrong or changed for the user:** a reading is
  only marked "check this one". The chart (#288) uses the same rows and flags as the list.
- Read dash (`DashReading`): with a last reading, **only a number from it to 200,000 past it is ever offered**;
  three of five frames must agree; **it only fills the field**, says "check it against the dash", never
  "detected", and nothing is saved until Save. Frames are never stored.

### Import

- `docs/import/README.md` and `docs/import/schema/` are the specification. **Change the schema, the Kotlin reader
  and the example files together, and never change how a version-1 file is read.** `ImportFormatTest` holds the
  schema and the reader to each other. A rule a schema can't state goes in `examples/beyond-schema/` and the
  README. The format is strict: unknown fields, `null`, stray spaces and out-of-range values refuse the file,
  nothing is repaired, every error has its path. A file never names a vehicle.
- A new kind: a schema, a valid example, `examples/invalid/<kind>/` and `examples/beyond-schema/<kind>/`, a
  reader and plan in `:domain`, a case in `ImportFormat.parse` and `supported`, and a `…Preview` in
  `ImportViewModel`. The page is kind-agnostic.
- A file says its unit by its field names (`everyMiles` / `everyKm` …), one unit per file; `FileDistances` puts
  it into the vehicle's unit. A file the app writes is in the vehicle's unit.
- **An import never deletes, is one transaction, has no Undo, and only writes the user's own rows**; nothing
  Glovebox supplies may be changed by one. "Add new only" is the default; "Replace matching" lists what it will
  overwrite first. The picked file is read into memory (2 MB cap) and never written to disk.
- Schedule import: a typical item is never matched or replaced; the only thing an import may change on one is to
  stop tracking it, unticked by default.
- History import: **adding a catalog item means adding it to `service-history.schema.json`, the README table,
  `CatalogText.kt`, `strings.xml` and `LineWords`.** Records match on date and odometer. **Readings that look
  mistyped are left out unless the user ticks them** (a shop's typo once added 60,000 miles). "Not in the
  future" is against the `today` passed to `ImportFormat.parse`.
- Carfax report PDF (#160): read in memory with PDFBox, never written to disk. **Only service visits are
  taken**, and a comment becomes a catalog line only where the wording is plain; pads on an unnamed axle are
  never front or rear. A visit with no mileage is refused, never guessed. Ligatures are repaired only into known
  words, and **a misnamed letter is learnt from the whole report** (two different known words), never from one.
  **Never commit a real report**; tests build a made-up one. Keeping a copy in Documents is off by default.
- Writing import files (#118, #120): **a written file must always be one the app accepts.** The screens are looser
  than the format, so `ImportFiles.sift` runs each item through the reader; what it refuses is listed before
  saving, never dropped quietly or repaired. An unconfirmed bundled torque value is left out. Service history has
  no such file: the export and the backup are for it. A new writer gets a round-trip test in `ImportFilesTest`.

### Milestones

- **Milestones are a view, never a second source of what is due:** nothing on that screen creates, moves or
  silences a reminder, and nothing about them is stored; "completed" is derived from the log, with no tick-box.
- The user's own table (`milestones`, `milestone_jobs`) shows **in place of** the generated view. A job is done by
  its `service` or, failing that, its name. Nothing about reminders, notifications or exports reads these tables.
- A table repeats one way or not at all (`vehicles.milestonesRepeatEveryMiles`, set by an import or the editor's
  repeat row, cleared with the table by `forgetTable`); a file with a different repeat is refused whole. A repeat
  has no `orMonths`.
- **A table typed in the app keeps exactly the rules of one read from a file, from the same code**
  (`MilestoneImport`'s limits and checks, called by `MilestoneEdits`). A table starts **empty**, never a copy of
  the generated view; a job's service is **never suggested from its name**; deleting is a confirmation with no
  Undo; Close and Back ask before discarding. Read jobs through `MilestoneWithJobs.ordered`. Where a table came
  from is not stored.

### What it takes, torque and maker's lists

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

### Recalls

- `RecallRepository` re-checks a week-old list; a failed check leaves the stored list alone. "No open recalls"
  only once NHTSA has answered. NHTSA answers 400 with an empty list for no recalls, so the body is read, not the
  status. `Settings.recallCadence` governs the in-app checks too.
- `completedOn` (the user's mark) survives refreshes; a dropped campaign is deleted only while open. Archived
  (`archivedOn`) means "doesn't apply" and isn't open. A mark-done record resets no reminder.

### Documents and photos

- **Every encrypted file the database points at comes from `DocumentDao.allEncryptedFiles()`**, and files are
  deleted through `PhotoCrypto.delete`. Documents are encrypted with the photo key in `photoDir`, so the sweep,
  backup, restore and crypto-erase cover them.
- Pictures are redrawn as JPEG (`redrawJpeg`), which drops metadata; a PDF is kept as it came.
- A document opens inside Glovebox (#224, the user's decision of 2026-10-16). The PDF renders in an **isolated
  process**, which starts `GloveboxApp` too, so **`GloveboxApp.onCreate` does nothing in an isolated process**;
  keep anything new there behind that check. The decrypted PDF goes through `memfd_create` (Android 11+) or
  `cache/viewing/`, deleted at once. **`pdf-ocr-play-services` must never be on the classpath**; check on an
  upgrade. Exports list documents and never carry their contents.
- Pickers and "save as" launchers are registered in `MainActivity`, because returning from them can lock the
  app and the lock replaces the nav host.

### Export and backup

- `Exports.document` applies the options once; the Markdown and PDF writers only lay out what they are given.
  The file goes through `cache/export/` and is deleted either way. An export is not encrypted, and every recall
  or schedule table says what it is.
- Backup format: `docs/backup-format.md` and `BackupFormat.kt`. **Change the two together, and never change how a
  version-1 file is read.** Tables are read generically, so new columns need nothing. The password is never
  stored. The last-backup date is recorded only after Android has really written the file.
- **Restore is all or nothing:** read and authenticate everything first, replace every table in one
  transaction, then move the staged files in. `finishInterrupted` completes that on launch and **must run before
  the orphan sweep.** App-lock settings are never restored. Replacing data is confirmed by typing REPLACE.

### Lock, navigation and app shell

- Unlock: what is offered comes from `Unlock.options`; a cancel says nothing. **On Android 10 the check opens the
  system's confirm-credentials screen, not `BiometricPrompt`** (androidx.biometric cancels on stop, so a correct
  PIN unlocked nothing). Don't move it back without testing the PIN fallback on `Pixel_API_29`: fingerprint,
  "Use PIN", too many wrong fingers, cancel, and PIN with no fingerprint.
- **While locked, the screens are out of composition** (no sheet or dialog is readable behind the lock), and
  their saved state and **ViewModel stores** live in `MainActivity`'s holders. Don't swap this for drawing the
  lock over a live nav host: sheets and dialogs are separate windows.
- Navigation 3: each tab is its own back stack over Garage. **Add a destination with `screen<Route>`, never
  `entry<Route>`** (it draws the page colour and hands the route to ViewModels). A route without fields is a
  `data object`. Results go back through the host's `results`. State that must survive a restart goes in
  `rememberSaveable` or a `SavedStateHandle`.
- A notification or shortcut's `Destination` waits in `PendingDestination` until unlock. **Collect that flow;
  don't key an effect on it.** `DestinationViewModel.take` looks up before clearing.
- Launcher shortcuts (#235): static and generic, **no per-vehicle dynamic or pinned shortcuts**. They open
  `ShortcutActivity`, never `MainActivity` (a static shortcut's `CLEAR_TASK` would lose a half-filled form).
  The debug build has its own copy of `shortcuts.xml`; `ShortcutsTest` holds them the same.
- Home-screen widget (#238): **off by default**, because it shows the garage outside the lock. **It works nothing
  out**: rows and wording are the garage list's. Nothing on it acts.
- Notifications: `NoticeRules` decides what is said; `NoticeLogStore` is updated even when notifications are off,
  so turning them on releases no backlog. Workers skip sold vehicles. The lock-screen version says only the kind.
- The welcome shows only with the flag unset and no vehicles; the flag is not backed up.
- Delete all data: typed DELETE, then the screen lock. **Anything new the app stores on disk has to be added to
  `DataWipe.erase`.** `DataWipe.finishInterrupted` runs before Hilt and completes a wipe the process didn't
  survive.
- Settings rows that lead nowhere are left out, not shown dead.
- The Privacy screen (`privacy_*` arrays in `strings.xml`) **must stay true of the code**: update it with any
  change to the network, permissions, storage, backup or export.
- Licences come from the AboutLibraries plugin at build time; a library with a licence of its own needs its text
  in `app/src/main/assets/licences/<group>_<artifact>.txt`.

### Database

- Schema changes are Room auto-migrations; exported schemas live in `data/schemas`. `MigrationTest` fills every
  column at every version and reads it back. **A new version is covered once its schema file is there; a new
  table has to be added to the test's `order` list.**

## Conventions

- Business logic goes in `:domain` with a test; ViewModels stay thin.
- **A composable or helper used by more than one screen lives in `ui/components/`**, not in the screen that
  first needed it (#297). Look there first (`ScreenHeader`, the cards, `GloveboxSheet`, `OdometerField`, the
  buttons, rows, pills and dialogs). One used by a single feature stays in that feature's package.
- **All wording lives in `res/values/strings.xml`** (#236). Composables use `stringResource` /
  `pluralStringResource`; other code uses `LocalResources` or `AppText.res` (never
  `LocalContext.current.getString` in a composable). Figures go in already carrying their unit, counts through a
  plural. Names are `<screen>_<what>`. `HardcodedTextTest` fails on text typed into the UI.
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
