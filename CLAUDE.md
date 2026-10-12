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
  A "last done" the user gives on its own is a service record too, flagged `enteredAsLastDone` (#78).
  **Nothing is assumed or estimated for the user:** there is no "assume everything was done on schedule".
- **Torque stored in N·m**, converted for display. Odometer stored in miles (km is a display setting later),
  and as typed from the dash: a cluster replacement converts on read, never by rewriting readings.
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
not black panels. Fonts: IBM Plex Sans + IBM Plex Mono, bundled in `res/font` and set in `Theme.kt`
(`PlexSans` through `MaterialTheme.typography`, `PlexMono` by name for mileage, VINs, torque and other
figures). Never use `FontFamily.Monospace`. Their licence (OFL 1.1) is `assets/licences/com.ibm_plex.txt`
and is listed on the licences screen by hand in `LicenceCatalog`, since fonts aren't Maven dependencies.
Accent text on the soft-orange tint is `onPrimaryContainer`, not `primary` (4.4:1 in light, below the
4.5:1 text needs); `primary` there is fine for icons and large figures. Colours that Material doesn't name
come from `statusColors()`. In dark the urgent pill is solid (`onPrimaryContainer` behind dark text).
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
about what differs; also the user's own milestone table). Milestones (the schedule by mileage with what was done, from the Maintenance schedule
screen).
To build: distance units (km display, #93), unlock on Android 10 (untested, #27), mileage readings (#64, #65),
known torque specs (#76), VIN scan follow-ups (#82, #83).

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
- What an import may do: `ImportPlans.plan` sorts a file's items into new, fills (the saved row is only
  a name), same and differs before anything is written, and the page shows that. "Add new only" is the
  default; "Replace matching" writes over the rows that differ after a dialog lists each one. An import
  never deletes, is one transaction (`TorqueDao.applyImport`), has no Undo, and only ever writes the
  user's own rows: nothing Glovebox supplies may be changed by one (that will matter for #76's bundled
  lists and for template schedule items). The picked file is read into memory, capped at 2 MB, and never
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
  catalog item means adding it to `service-history.schema.json` and the README table.** Records match on
  date and odometer. A matching bare last done (#78) counts as a fill, replaced in either mode, only when
  the imported record resets every reminder it did; otherwise it is a difference. A replace keeps the
  record's id, so its photos and recall link stay, and clears `enteredAsLastDone`. The preview lists the
  reminders that move for the chosen mode (`LogEdits.reminderChanges` over `HistoryImport.entriesAfter`)
  and warns about readings that don't fit the ones around them (`HistoryImport.backwards`: whatever falls
  outside the heaviest run of readings that never goes down, saved readings weighing more), both in dash
  terms. Sold vehicles can take history but no other kind. "Not in the future" is measured against the
  `today` passed to `ImportFormat.parse`. An imported record says "· imported" after who did it
  (`LogEdits.who`) and carries `Exports.IMPORTED_NOTE` in exports; editing it in the form keeps the flag.
- Milestones (`Milestones` in `:domain`, `MilestonesScreen`): the schedule laid out by mileage, as an
  owner's manual prints it. **They are a view, never a second source of what is due:** reminders and
  notifications come only from the interval schedule and the service log, and nothing on that screen
  creates, moves or silences one. Nothing about them is stored; "completed" is derived from the log each
  time, and there is deliberately no tick-box. The list is generated from tracked items that have a mileage
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
  `milestone_jobs`, both cascading from `vehicles`): imported from a file, never edited in the app. When a
  vehicle has one, the Milestones screen shows it **in place of** the generated view, with
  `status(continues = false)`, since a table simply ends and a job's last milestone takes whatever comes
  after the one before it. A job is the same job across milestones, and is completed by logged work, by
  its `service` (a record line's `catalogId`) or failing that its name (the line's label, ignoring case):
  `MilestoneImport.jobKey` and `doneKeys`. Nothing about reminders, notifications or exports reads these
  tables. "Remove this table" deletes it and the generated view returns. Import matches on `atMiles`;
  a replace keeps the row and swaps its months and jobs. Backup and restore carry the tables without
  change, because both go table by table from SQLite.
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
- Log service only offers one-tap "due" chips for reminders that have a log entry.
- Log service leaves the odometer empty and shows "Last recorded" under it; it is deliberately not
  prefilled, because a prefilled reading gets saved untouched and every reminder is then counted from the
  wrong mileage. The note on the right of each line comes from `ServiceCatalog.hint`: what the user wrote
  on Vehicle details for that job (oil, tire size, wiper blades), otherwise the reminder's interval.
  Future dates are refused by the date picker itself, typed or tapped.
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
- The unlock screen (`LockScreen`) is what sits behind Android's own prompt and what is left when the
  prompt is dismissed. It asks once on arrival; after a cancel the user taps the ring. What it offers
  comes from `Unlock.options` (domain): fingerprint wording only where the phone has a sensor, a padlock on
  a phone with only a screen lock, and "Use screen lock instead" only on Android 11+, where Android can ask
  for the screen lock alone. Errors go through `Unlock.message`; a cancel says nothing. With the screen
  lock allowed as a fallback, Android handles too many failed tries itself by switching to the PIN.
- Navigation (checked against `design/screens/NavMap.png`): four tabs, each keeping its own screens
  (`saveState` / `restoreState`); the fourth is labelled Settings, because it opens Settings and not a
  menu. System Back from a tab goes to Garage, then leaves the app. A notification's destination is pushed
  over whichever tab is showing, so Back returns there. Add a destination with `screen<Route>` in
  `GloveboxNavHost`, never `composable<Route>`: screens draw no background of their own, and `screen` puts
  the page colour behind them so nothing shows through during a held back swipe. Predictive back is
  switched on in the manifest. Screen state that should survive Android restarting the app goes in
  `rememberSaveable` or a `SavedStateHandle`, as History's vehicle and category choice do.
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
- Settings rows that lead nowhere are left out, not shown dead: `Distance units` (odometers are stored
  and shown in miles everywhere; km needs every mileage field and label converted) . `Maintenance schedule` opens the only vehicle's schedule, or asks which when there are several,
  and is absent with none. `Lock the app` (`LockDelay`, stored as `lockAfterSeconds`, -1 = on restart)
  shows only while the lock is on and the phone has a screen lock. The privacy line says what is really
  sent: a VIN, or a year, make and model.
- About (`ui/about`): the version comes from the installed package. `PrivacyScreen` states what is kept,
  what is sent and what never happens; every line has to stay true of the code, so update it with any
  change to the network, permissions, backup or export. The copy never names manufacturer data as a source.
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
- An Undo bar stays for `LogEdits.UNDO_SECONDS` (30), and the delete sheet says so. Events are collected
  with `collectLatest`: a newer change replaces the bar for an older one, and whatever ends a bar without
  Undo (timeout, replacement, the app locking) discards the photo files it was holding.
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
- Schema changes so far are Room auto-migrations (v13); keep exported schemas in `data/schemas`.

## Conventions

- Business logic goes in `:domain` with a test; ViewModels stay thin.
- Never show placeholder data as if real; mark sample values clearly.
- Torque/interval reference values are user-verified; UI says "check your service manual".
- Any Python helper scripts: use `uv run` with inline script dependencies.
- Public repo: never commit real personal data (VINs, records, photos), keystores, `local.properties`
  or `.idea/`. Test fixtures use made-up values.
