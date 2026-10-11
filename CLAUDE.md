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
- **Barcodes are read with ZXing, not ML Kit.** ML Kit's bundled barcode library depends on Google's
  log-upload transport (`transport-backend-cct`, `transport-runtime`) plus Firebase and Play Services
  pieces, which breaks the rule above. ZXing is pure Java with no network code. Check the dependency tree
  the same way before adding any Google or Firebase library.
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
service), vehicle details (photo, nickname, from the VIN, what it takes, schedule and recalls rows,
Mark as sold), archived vehicles (read-only history, move back to garage), delete vehicle (counts, type the
name to confirm), torque specs (unit switch, categories, search, add / edit / delete),
update-mileage sheet (with Undo), add vehicle via
VIN (VIN entry, then a confirm screen where decoded values can be corrected), add without a VIN (year,
make and model from NHTSA's lists or typed), VIN scanning (Code 39 and QR), receipt and parts photos
(attach, grid, viewer, share), export as PDF or Markdown, notifications (service due, overdue, new
recalls, mileage check-in) with their settings screen, encrypted backup (create, monthly reminder, restore), recommended schedule after decode (+ edit item, add own), maintenance schedule after
setup (track / untrack, edit, add, last done and next due), Log service, History
(per-vehicle list, category filters), service record detail, edit and delete with Undo, settings
(theme, biometrics, hide in recents, torque units), Reminders tab (grouped list, vehicle filter,
Log it, snooze and skip), reminder rules (default rule, lead time, per-service overrides), recalls
(list, mark done / open, banner on vehicle home, garage chips, row on the decode result).
First-run welcome (lock and notification choices, add a vehicle or restore), delete all data (type
DELETE, then the phone's screen lock).
To build: finish Settings (#25), about (#26), unlock screen (#27), fonts and dark pass (#28), History
and Log service polish (#30, #31), navigation map (#32), mileage readings (#64, #65), odometer offset
(#73, model undecided), known torque specs (#76), last done at setup (#78), VIN scan follow-ups (#82, #83).

Notes on what's built:
- Add vehicle is one route with two steps in `AddVehicleViewModel`: VIN entry, then "Is this your
  vehicle?". Body, drive type, engine, fuel, transmission and the year / make / model can be corrected
  before saving. `VinDecode.quality` sorts a decode into clean, partial (vPIC flagged it) or unidentified;
  a vehicle is never saved without a real year, make and model, so an unidentified VIN has to be fixed or
  filled in by hand. vPIC trims that list several ("SR5/SR5 Premium") are offered as choices, and
  `vehicles.trimConfirmed` records that the user picked or typed one. The vehicle is saved before the
  schedule screen, so Back from there keeps it.
- VIN scanning (`ScanVinScreen`): CameraX's `LifecycleCameraController` feeds frames at about 1080p to
  `VinBarcodeReader`, which takes the luminance plane, turns it upright (a 1D reader only scans along
  rows), cuts out a band across the middle where the guide is, and asks ZXing for a Code 39 or QR code.
  The camera starts at 2x zoom so the phone can sit back far enough to focus. If a frame can't be read it
  is tried again after `evenOut`: for 1D barcodes ZXing uses one black/white threshold per row whichever
  binarizer is chosen, so a barcode half in shadow fails without it (a test pins this). `VinScan.vin` accepts only a 17-character run that passes
  the check digit; it deliberately doesn't slide a window along longer text, which would match by chance
  about one time in eleven. Frames are never stored. The VIN goes back to the add screen through the nav
  entry's saved state and is decoded at once. The scanner keeps the canvas's dark colours in both themes.
- Add without a VIN (`ManualAddScreen`) picks year, make and model. Makes come from vPIC's car, truck and
  MPV lists and models from make + year; `NhtsaClient.makes` sends nothing about the vehicle and `.models`
  only the make and year. Every picker also takes typed text, which is the only way in when the lists
  can't be loaded. Such a vehicle has `vin` null and `enteredByHand` true; a VIN or older serial number
  can be added from Vehicle details (`Vin.isPlausible`), which doesn't decode it or change the details.
  The drivetrain is never preselected.
- The schedule is a generic starter list (`ScheduleTemplates.generic`), labelled as typical intervals,
  not manufacturer data — there is no source for factory schedules.
- After setup the schedule lives on `ScheduleScreen` (from Vehicle details). It writes straight to
  `schedule_items`: a tick is `tracked`, and an edit keeps whatever the editor doesn't show, such as a
  snooze. `ScheduleItemEditor` is shared with setup; `ItemHistory` adds LAST DONE (derived from the log,
  linking to the record) and NEXT DUE, which follows the interval as it is typed. The pace estimate is
  shown only for the saved interval and rule. An empty schedule offers setup or adding an item; one that
  lacks typical items (`ScheduleTemplates.missing`) offers to add them unticked. Never send a vehicle that
  already has items back through setup: it inserts every item again.
- `vehicles.fuel` is vPIC's primary fuel type, kept since schema v6; vehicles added earlier have none.
  `ScheduleTemplates.generic` leaves engine oil, the engine air filter and spark plugs out for "Electric".
- Logged work resets a reminder through `schedule_items.templateKey` matching `CatalogItem.resets`
  (`ServiceCatalog`); the user's own schedule items appear under "Your items".
- Log service only offers one-tap "due" chips for reminders that have a log entry.
- Never-logged reminders are "overdue" to `ReminderEngine`, but the UI lists them apart ("Not logged yet")
  and leaves them out of overdue counts — see `ReminderPresentation.comingUp`.
- The garage list is skipped once per launch when there is exactly one vehicle (`GarageScreen`); "All
  vehicles" still returns to it, which is where another vehicle is added.
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
- While locked, the app's screens are out of composition (`MainActivity`), so no screen, sheet or dialog
  is drawn or readable behind the lock screen. Their saved state is held in a `SaveableStateHolder`, which
  is what returns the user to the same screen after unlocking. Don't swap this for drawing the lock
  screen over a live nav host: sheets and dialogs are separate windows and would sit on top of it.
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
  launch). Taking a photo needs the camera permission because the app declares it for VIN scanning.
- Export: `Exports.document` (domain) applies the options once (dates, costs and notes, schedule and
  recalls, photos), so the Markdown writer (`Exports.markdown`) and the PDF writer (`PdfExport`, app,
  Android's `PdfDocument`, US Letter) only lay out what they are given. `ExportSheet` is one composable
  used from History, Settings, Vehicle details' sold sheet, the delete screen and the archive card.
  The file is written to `cache/export/` and copied to wherever the user picks through `ExportSaver`,
  whose "save as" launchers live in `MainActivity` (same lock-screen reason as the photo pickers); the
  working copy is deleted either way. Android's PDF writer stores photos without JPEG compression, so
  they go in at 1,000 px and the sheet's size figure is an estimate (`RecordPhotos.PDF_BYTES_PER_PHOTO`).
  An export is not encrypted, and every recall or schedule table carries a note saying what it is.
- Notifications: two WorkManager jobs, kept in step with the settings by `GloveboxApp` through
  `NoticeScheduler`. `DailyCheckWorker` runs at about the chosen time and uses no network; what it says
  is decided by `NoticeRules.daily` (domain): a service is announced once when it enters its lead time,
  an overdue one is nudged weekly, a snoozed or never-logged one stays quiet, and a mileage check-in is
  asked for when the newest reading is older than the cadence. `NoticeLogStore` (its own DataStore file,
  ids and dates only) remembers what was said; it is updated even when a switch is off or Android blocks
  notifications, so turning them on later doesn't release a backlog. `RecallCheckWorker` needs a network,
  retries silently, and only announces campaigns added after a vehicle's first check. Both skip archived
  vehicles. The workers are plain `CoroutineWorker`s that reach Hilt through `NotifyEntryPoint`.
- "Check for new recalls" (`Settings.recallCadence`) is honoured in `RecallRepository.refreshIfStale`, so
  it governs the in-app checks as well as the job; Never leaves only the Refresh button.
- A notification carries a `Destination` on its intent. `MainActivity` puts it in `PendingDestination`
  and the nav host, which only exists once the app is unlocked, takes it from there, so nothing is drawn
  before unlock. Collect that flow; don't key an effect on it (taking it clears it, and a keyed effect
  cancels its own vehicle lookup on a cold start). Each kind has its own channel, and the lock-screen
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
  Odometer offset (`vehicles.odometerOffset` exists but
  nothing applies it). Maintenance schedule always opens `ScheduleScreen`.
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
  `TorqueSpecs.common` with no size, value or note: there is no source for factory torque values and none
  may be invented. `torque_specs.category` and `.source` hold `TorqueCategory` and `TorqueSource` names.
  `FACTORY` means the user says they copied the value from the factory service manual, and is only kept
  while the row has a value. Torque is typed in any unit (`TorqueSpecs.parse`, capped at 1,000 N·m) and
  stored in N·m. The unit switch starts at the Settings unit and doesn't change it.
- The vehicle photo is picked with the system photo picker (no permission), redrawn as a JPEG of at most
  1600 px, which drops its metadata, and encrypted by `VehiclePhotoStore` into `PhotoCrypto.photoDir`
  (`vehicles.photoFile`). The launcher is registered in `MainActivity` and answered by
  `VehiclePhotoPicker`, because returning from the picker can lock the app and the lock screen replaces
  the nav host.
- Schema changes so far are Room auto-migrations (v8); keep exported schemas in `data/schemas`.

## Conventions

- Business logic goes in `:domain` with a test; ViewModels stay thin.
- Never show placeholder data as if real; mark sample values clearly.
- Torque/interval reference values are user-verified; UI says "check your service manual".
- Any Python helper scripts: use `uv run` with inline script dependencies.
- Public repo: never commit real personal data (VINs, records, photos), keystores, `local.properties`
  or `.idea/`. Test fixtures use made-up values.
