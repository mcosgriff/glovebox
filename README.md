# Glovebox

Private, offline-first vehicle maintenance tracker for Android. Everything is stored on the phone,
encrypted. The only network calls send a VIN (or year/make/model) to NHTSA.

## Modules

| Module | What lives there |
|---|---|
| `:domain` | Pure Kotlin: reminder engine (whichever first/last, mileage/time only), service-log rollback, torque units, VIN check digit. Unit-tested, no Android. |
| `:data` | Room + SQLCipher database, Keystore key management, Tink photo encryption, NHTSA client, settings (DataStore), data wipe. |
| `:app` | Jetpack Compose UI, Hilt, navigation, biometric app lock. |

## Opening the project

1. Open this folder in Android Studio and let Gradle sync (it installs the SDK if needed).
2. Run on a device or emulator with the `app` run configuration.
3. Run the fast tests anytime: `./gradlew :domain:test`
4. Upgrade the Android Gradle Plugin with Tools → AGP Upgrade Assistant.

## Security model

- Database: SQLCipher with a random 32-byte passphrase, wrapped by an AES-256-GCM key in Android Keystore
  (StrongBox when available). The key does **not** require a fingerprint, so background reminder and
  recall checks work while the phone is locked.
- Photos: Tink streaming AEAD, keyset wrapped by a separate Keystore key.
- Biometrics lock the **UI** (`AppLock`). "Hide in recent apps" sets `FLAG_SECURE`.
- Android auto-backup and device transfer are disabled; the app's own password-protected backup file
  is the only way to move data to a new phone.
- Delete all data destroys the Keystore keys first (crypto-erase), then deletes the files.

## Releases (Obtainium)

1. Create a release keystore once and **back it up** — every update must be signed with the same key:
   ```
   keytool -genkeypair -v -keystore glovebox-release.jks -alias glovebox -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Add the signing secrets to the `release` environment (Settings → Environments → release →
   Environment secrets), not as repository secrets:
   - `KEYSTORE_B64` — `base64 -i glovebox-release.jks | pbcopy`
   - `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`glovebox`), `KEY_PASSWORD`
3. Release: merge the open **release PR** (title `chore(main): release X.Y.Z`). The
   `release-please` workflow keeps it up to date from the Conventional Commits on `main` — `fix:` bumps
   the patch version, `feat:` the minor, a breaking change (`feat!:` / `BREAKING CHANGE:`) the major —
   and it updates `CHANGELOG.md` and `version.txt`. Merging tags `vX.Y.Z`, creates the GitHub Release
   and starts the signed build; approve the waiting `release` run in the Actions tab. Release tags
   can't be moved or deleted afterwards. (Pushing a `v*` tag by hand still works.)
4. In Obtainium, add the GitHub repo URL. (Private repo: add a GitHub token in Obtainium's settings.)

`applicationId` (`io.cosgriff.glovebox`) must never change after the first release.

## Next up

- Recommended schedule seeding after decode, Log service, History, Reminders tab
- WorkManager: daily reminder check, weekly recall check, mileage check-in notifications
- Encrypted backup / restore, PDF and Markdown export
- Bundle IBM Plex Sans / Mono fonts (`ui/theme/Theme.kt` TODO)

Design canvas: the Glovebox design artifact in Claude.

## License

GPL-3.0. See [LICENSE](LICENSE).
