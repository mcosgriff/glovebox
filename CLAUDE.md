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
Keep them green; lint errors fail the build.

## Commands

- `./gradlew :domain:test` — fast pure-Kotlin tests (reminder engine, torque, VIN). Run often.
- `./gradlew build` — everything.
- `./gradlew :app:installDebug` — install on a connected phone (debug app id `io.cosgriff.glovebox.debug`).
- Release: merge the release PR that `release-please` keeps open (version and changelog come from the
  Conventional Commits on `main`); it tags `vX.Y.Z` and starts `release.yml`, which builds the signed
  APK after approval. See README.

## Modules

- `:domain` — pure Kotlin, **no Android imports**. All business rules live here and get unit tests:
  `ReminderEngine` (due status, whichever-first/last, mileage-only/time-only, lead time, pace),
  `ServiceLog` (last-done derivation), `Recalls` (list merge, campaign and component names),
  `Torque`/`TorqueUnit`, `Vin`.
- `:data` — Room + SQLCipher, Keystore key management (`DatabaseKeyManager`), Tink photo
  encryption (`PhotoCrypto`), `NhtsaClient`, `RecallRepository`, `SettingsRepository` (DataStore),
  `DataWipe`, Hilt module.
- `:app` — Compose UI, navigation, ViewModels, `AppLock` + `LockScreen`, theme.

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
- **Torque stored in N·m**, converted for display. Odometer stored in miles (km is a display setting later).
- **Network:** only NHTSA (`vpic.nhtsa.dot.gov`, `api.nhtsa.gov`). Only a VIN or year/make/model is
  sent. No analytics, crash reporting, ads, or other third-party SDKs that phone home.
- **NHTSA recalls are by year/make/model**; the public API can't confirm repair status for a VIN.
  UI lets the user mark recalls done and links to the manufacturer's VIN lookup.
- **applicationId `io.cosgriff.glovebox` must never change.** Release signing key must never change.
- **IDE is Android Studio.** Keep AGP current (use Studio's AGP Upgrade Assistant); no IntelliJ IDEA
  compatibility constraint.
- minSdk 29, targetSdk 36, compileSdk 37 (current androidx/Compose/OkHttp require compiling against 37;
  raising targetSdk is a separate decision), JDK 17, Kotlin + Compose + Material 3, Hilt (KSP), Navigation Compose
  with `@Serializable` routes. Dynamic color is off; use the theme tokens.

## Design tokens (from the canvas)

Light: ground `#F4F3EF`, surface `#FFFFFF`, ink `#17191C`, muted `#5A5F66`, line `#DEDCD5`,
accent `#C2410C` (on-accent white), accent-soft `#FBE9DF`/`#9A3412`, ok `#0F766E`/`#DDEFEC`,
danger `#B42318`. Dark: ground `#121416`, surface `#1C1F23`, ink `#EDEBE6`, muted `#A5A9AF`,
accent `#F08A4B` (on-accent `#17191C`), accent-soft `#3A2418`/`#FDBA74`, ok `#5EEAD4`, danger `#F97066`.
Light mode shows importance with tints (soft orange = needs attention, solid orange = most urgent),
not black panels. Fonts: IBM Plex Sans + IBM Plex Mono (numbers, VINs) — not bundled yet.
Touch targets ≥ 48dp. Destructive actions are red and confirm by typing (vehicle name / DELETE).

## Screens (canvas board → status)

Done in code: unlock, empty garage, garage list (status chip, next reminder, across-all strip; opens the
vehicle directly when there is only one), vehicle home (Coming up + Log
service), vehicle details (photo, nickname, from the VIN, what it takes, schedule and recalls rows),
update-mileage sheet (with Undo), add vehicle via
VIN (decode + save), recommended schedule after decode (+ edit item, add own), Log service, History
(per-vehicle list, category filters), service record detail, edit and delete with Undo, settings
(theme, biometrics, hide in recents, torque units), Reminders tab (grouped list, vehicle filter,
Log it, snooze and skip), reminder rules (default rule, lead time, per-service overrides), recalls
(list, mark done / open, banner on vehicle home, garage chips, row on the decode result).
To build, roughly in order: mark sold / archive / delete vehicle, odometer offset, torque specs, VIN scanning (CameraX + ML Kit bundled barcode,
Code 39), receipt photos on service records, notifications (WorkManager), encrypted backup/restore,
PDF + Markdown export, welcome/first run, manual add (no VIN), about.

Notes on what's built:
- The schedule is a generic starter list (`ScheduleTemplates.generic`), labelled as typical intervals,
  not manufacturer data — there is no source for factory schedules.
- Logged work resets a reminder through `schedule_items.templateKey` matching `CatalogItem.resets`
  (`ServiceCatalog`); the user's own schedule items appear under "Your items".
- Log service only offers one-tap "due" chips for reminders that have a log entry.
- Never-logged reminders are "overdue" to `ReminderEngine`, but the UI lists them apart ("Not logged yet")
  and leaves them out of overdue counts — see `ReminderPresentation.comingUp`.
- The garage list is skipped once per launch when there is exactly one vehicle (`GarageScreen`); "All
  vehicles" still returns to it, which is where another vehicle is added. Its archived-vehicles row
  waits on archiving.
- Update mileage adds an `odometer_readings` row dated today; the snackbar's Undo is the only way to
  remove one so far. The sheet's preview comes from `MileageUpdate.preview`. "Read dash" (camera) and
  the notification entry point are not built.
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
- All service-log writes go through `RecordActions` (app), which emits the events behind the
  nav-host snackbar and its Undo. Editing reuses the Log service screen (`LogServiceRoute.recordId`).
- Recalls live in the `recalls` table, one row per vehicle and campaign. `RecallRepository` re-checks a
  vehicle when its list is a week old (`vehicles.recallsCheckedOn`): once per launch from the garage,
  and on opening vehicle home or the Recalls screen. A failed check leaves the stored list alone.
  "No open recalls" is only shown once NHTSA has answered for that vehicle.
- NHTSA answers HTTP 400 with an empty list when it has no recalls for a vehicle; `NhtsaClient.recalls`
  reads the body rather than the status. Report dates arrive as day/month/year.
- `recalls.completedOn` is the user's mark and survives refreshes; a campaign NHTSA drops is deleted only
  while open. Marking done goes through `RecordActions.completeRecall`, which can add a service record
  (`recalls.serviceRecordId`) whose line has no schedule item, so it resets no reminder. Mark as open
  removes that record, with Undo.
- Manufacturer VIN lookup pages are a short checked list (`Recalls.manufacturerSite`); other makes get
  NHTSA's lookup. "Find a dealer" hands "<make> dealer" to the maps app. With nothing open, the way in
  is the Recalls row on Vehicle details.
- Vehicle details opens from the overflow button on vehicle home. Its editable fields are written one
  column at a time (`VehicleDao.setNickname` etc.), never with a whole-row `@Update`, so they can't undo
  a recall check that landed meanwhile. Rows with nowhere to go yet are left out rather than shown dead:
  torque specs, Mark as sold, Delete vehicle, and Odometer offset (`vehicles.odometerOffset` exists but
  nothing applies it). Maintenance schedule only links to setup when the vehicle has no items.
- The vehicle photo is picked with the system photo picker (no permission), redrawn as a JPEG of at most
  1600 px, which drops its metadata, and encrypted by `VehiclePhotoStore` into `PhotoCrypto.photoDir`
  (`vehicles.photoFile`). The launcher is registered in `MainActivity` and answered by
  `VehiclePhotoPicker`, because returning from the picker can lock the app and the lock screen replaces
  the nav host.
- Schema changes so far are Room auto-migrations (v5); keep exported schemas in `data/schemas`.

## Conventions

- Business logic goes in `:domain` with a test; ViewModels stay thin.
- Never show placeholder data as if real; mark sample values clearly.
- Torque/interval reference values are user-verified; UI says "check your service manual".
- Any Python helper scripts: use `uv run` with inline script dependencies.
- Public repo: never commit real personal data (VINs, records, photos), keystores, `local.properties`
  or `.idea/`. Test fixtures use made-up values.
