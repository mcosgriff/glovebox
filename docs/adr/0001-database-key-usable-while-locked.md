# The database key works while the phone is locked

The database is SQLCipher with a random passphrase, wrapped by an AES-GCM Android Keystore key. That key is made with `setUserAuthenticationRequired(false)` and without `setUnlockedDeviceRequired`, because WorkManager has to open the database while the phone is locked to send reminders and run recall checks. Biometrics lock the UI (`AppLock`), not the data.

## Considered Options

- **A key that needs the user's fingerprint or an unlocked phone.** Stronger at rest, but no reminder or recall check could run in the background, and those are the app's main job.
- **No Keystore wrapping (a passphrase kept in app storage).** Readable by anyone who can read the app's files; rejected.

## Consequences

- The README's security model tells users the key needs no fingerprint, and why.
- Don't add `setUnlockedDeviceRequired` or user authentication to this key: background work would fail silently.
