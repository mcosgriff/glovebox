# Glovebox — notes for Claude

Private, offline-first Android app for tracking vehicle maintenance. Single user, no account, no
server. Owner: Matthew (sideloads via Obtainium from GitHub Releases, not the Play Store).

Design canvas (screens, flows, light/dark, nav map):
https://claude.ai/code/artifact/7f521081-34e6-46e9-99aa-641765621ad8

## Status

The scaffold was written without a successful Gradle build (no network/SDK in the environment that
created it). First job: get `./gradlew build` and `./gradlew :domain:test` green, fixing any
dependency-version or compile issues, before adding features.

## Commands

- `./gradlew :domain:test` — fast pure-Kotlin tests (reminder engine, torque, VIN). Run often.
- `./gradlew build` — everything.
- `./gradlew :app:installDebug` — install on a connected phone (debug app id `io.cosgriff.glovebox.debug`).
- Release: push a tag `vX.Y.Z`; `.github/workflows/release.yml` builds a signed APK. See README.

## Modules

- `:domain` — pure Kotlin, **no Android imports**. All business rules live here and get unit tests:
  `ReminderEngine` (due status, whichever-first/last, mileage-only/time-only, lead time, pace),
  `ServiceLog` (last-done derivation), `Torque`/`TorqueUnit`, `Vin`.
- `:data` — Room + SQLCipher, Keystore key management (`DatabaseKeyManager`), Tink photo
  encryption (`PhotoCrypto`), `NhtsaClient`, `SettingsRepository` (DataStore), `DataWipe`, Hilt module.
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
- **AGP pinned to 9.4.1** so the project also opens in IntelliJ IDEA. If Matthew confirms he's using
  Android Studio only, it's fine to upgrade AGP.
- minSdk 29, target/compile 36, JDK 17, Kotlin + Compose + Material 3, Hilt (KSP), Navigation Compose
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

Done in code: unlock, empty garage, garage list, add vehicle via VIN (decode + save), settings (theme,
biometrics, hide in recents, torque units). Placeholders: History, Reminders.
To build, roughly in order: recommended schedule after decode (+ edit interval), Log service,
History + service record detail + edit/delete with Undo, Reminders tab + snooze, update-mileage
sheet, recalls, vehicle details (+ mark sold / archive / delete), torque specs, VIN scanning
(CameraX + ML Kit bundled barcode, Code 39), notifications (WorkManager), encrypted backup/restore,
PDF + Markdown export, welcome/first run, manual add (no VIN), about.

## Conventions

- Business logic goes in `:domain` with a test; ViewModels stay thin.
- Never show placeholder data as if real; mark sample values clearly.
- Torque/interval reference values are user-verified; UI says "check your service manual".
- Any Python helper scripts: use `uv run` with inline script dependencies.
