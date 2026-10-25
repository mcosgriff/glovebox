# The applicationId and the release signing key never change

`io.cosgriff.glovebox` and the release signing key are permanent. Android refuses an update signed with a different key, so without key rotation a new key strands every install. The package is also registered for Android developer verification with that key (#267, 2026-10-17); from 2027 an app from an unregistered developer won't install on a certified phone, so a new key would mean registering again. The key's fingerprint is published in the README and in `assetlinks.json`.

## Considered Options

Written down after the decision, not at the time; recorded so they aren't proposed again unexamined.

- **Rotating the key with APK Signature Scheme v3** (Android 9 and later, so every supported phone). It lets an update move to a new key, but the new key would also have to be registered for developer verification, the fingerprint published in the README and `assetlinks.json` would change, and there is no reason to rotate while the old key is safe. If the key were ever compromised, this is the route to look at first.

## Consequences

- The keystore must be backed up outside the repo (README, Releasing); losing it ends updates.
- The ownership-challenge APK for registration is built locally and uploaded to the console only, never published; releases carry nothing for verification.
