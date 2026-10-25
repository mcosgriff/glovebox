# Delete all data destroys the keys first (crypto-erase)

"Delete all data" destroys the Keystore keys first, then deletes the database and files. Flash storage can keep deleted blocks around, so deleting files alone doesn't guarantee they are gone; once the keys are destroyed, whatever survives is ciphertext nobody can decrypt. Deleting one vehicle relies on `ON DELETE CASCADE` from `vehicles`, plus deleting that vehicle's own files.

## Consequences

- A marker file is written before anything is erased, so a wipe the process doesn't survive is finished on the next launch (`DataWipe.finishInterrupted`). Otherwise the app would be left with a database whose key is gone.
- Backup files the user saved elsewhere are not touched; the screen says so.
