# No Android auto-backup: the app's own encrypted file is the only way to move data

`allowBackup=false`, and the data extraction rules exclude everything, so neither Google's cloud backup nor device-to-device transfer copies Glovebox's data. The database and photo keys live in the Keystore and never leave the phone they were made on, so a copied database couldn't be opened anyway. The only way to move data is the app's own backup file, encrypted with a password the user chooses (`docs/backup-format.md`).

## Considered Options

- **Android auto-backup.** It would upload the data to Google, which breaks the network rule ([ADR-0007](0007-network-nhtsa-only.md)), and the files are useless without the Keystore keys.

## Consequences

- A forgotten backup password can't be recovered, and a lost phone with no backup means the data is gone. The backup screen says both.
- The backup format can't change in a way that breaks reading older files; see `docs/rules/export-and-backup.md`.
