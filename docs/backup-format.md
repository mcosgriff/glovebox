# Glovebox backup file, version 1

A backup is one file, `glovebox-backup-YYYY-MM-DD.glovebox`, encrypted with a password the user
chooses. It is the only way to move data to another phone: the database key and the photo key
live in the Android Keystore and never leave the device they were made on.

The code is `data/.../backup/BackupFormat.kt` (the file itself, plain JVM code with unit tests in
`BackupFormatTest`) and `BackupWriter.kt` (what goes in it). Keep this document in step with both.

## What is in it

- Every table in the database, row for row.
- Every photo the database points at: receipt and parts photos, and each vehicle's own photo.
  They are stored on the phone under a device-bound key, so each is decrypted and written into
  the backup under the backup's key.
- Every document the database points at (a vehicle's paperwork, `documents.fileName`): a PDF or
  a JPEG kept in the same folder under the same key, and written the same way, as a photo entry.
- The settings (theme, reminder rule and lead time, units, notification choices). The app-lock
  settings are in the file as well, but a restore leaves them as they are on the phone being
  restored to: a backup made with the lock off must not switch it off somewhere else. When the
  last backup was made, and when the reminder last went out, are not in the file at all.

Nothing else: no keys, no cached exports, no logs.

## Layout

All integers are big-endian.

### Header, 39 bytes, not encrypted

| Offset | Bytes | What |
|---|---|---|
| 0 | 8 | `GLOVEBOX` in ASCII |
| 8 | 1 | Format version, `1` |
| 9 | 1 | Key derivation, `1` = Argon2id v1.3 |
| 10 | 4 | Argon2 memory in KiB |
| 14 | 4 | Argon2 passes |
| 18 | 1 | Argon2 lanes |
| 19 | 16 | Salt, random per file |
| 35 | 4 | Nonce prefix, random per file |

### Key

`key = Argon2id(password as UTF-8, salt, memory, passes, lanes)`, 32 bytes.

New backups use 64 MiB, 3 passes, 1 lane. The values are in the header, so they can be raised
later without breaking older files. A reader refuses more than 1 GiB, 16 passes or 8 lanes, so a
hostile file can't make the phone do unbounded work before the password has even been checked.

### Blocks

After the header the file is a run of blocks:

| Bytes | What |
|---|---|
| 1 | `0` = more follow, `1` = this is the last block |
| 4 | Length of the ciphertext that follows |
| n | AES-256-GCM ciphertext with its 16-byte tag |

- A block holds at most 65,536 bytes of plaintext.
- Nonce for block *i*: the 4-byte prefix, then *i* as 8 bytes. Blocks count from 0.
- Additional data for block *i*: the whole 39-byte header, then *i* as 8 bytes, then the
  last-block byte. So a block can't be moved, dropped, copied in from another backup, or
  passed off as the end, and the header can't be altered without every block failing.
- **Block 0 is always empty.** It exists so a reader can check the password before anything
  else: if block 0 doesn't open, the password is wrong (or the first bytes are damaged, which
  can't be told apart at that point).
- The last block may be empty. A file that ends before a last block is incomplete.

### Inside the blocks

The decrypted stream is a list of entries, then a single `0` byte.

| Bytes | What |
|---|---|
| 1 | Kind (below) |
| 2 + n | Name: length, then modified UTF-8 (Java `DataOutput.writeUTF`) |
| 4 | Length of the data |
| n | Data |

| Kind | Name | Data |
|---|---|---|
| 1 manifest | empty | JSON, see below. Always the first entry. |
| 2 table | table name | JSON array of rows; each row an object of column name to value |
| 3 settings | empty | JSON object of setting name to `{ "type": …, "value": … }` |
| 4 photo | file name in the photo folder | The photo as plain JPEG bytes, or a document as plain PDF or JPEG bytes |

A reader skips kinds it doesn't know, so a later app can add kinds without a new format version.

Manifest:

```json
{
  "format": 1,
  "createdOn": "2026-10-11",
  "schemaVersion": 8,
  "counts": { "vehicles": 2, "records": 14, "photos": 9, "documents": 2 }
}
```

`schemaVersion` is the Room schema the tables were written from. `counts` is what the restore
screen shows before anything is replaced. Documents go in as photo entries; `photos` counts
the photos and `documents` the documents (schema 25 on). A file without `documents` has none.

Table rows are written generically, straight from SQLite: integers and reals as JSON numbers,
text as strings, null as null. Dates are the same integers the database holds (days since
1970-01-01). There are no blob columns.

## What the reader reports

| Situation | Reported as |
|---|---|
| Doesn't start with `GLOVEBOX` | Not a backup |
| Version higher than this app knows | Made by a newer version |
| Block 0 doesn't open | Wrong password |
| A later block doesn't open, or blocks are out of order | Damaged |
| Ends before the last block | Incomplete |
| Entries end without the closing `0` | Damaged |

## Restoring

`BackupRestorer` reads the whole file before it changes anything, so a damaged or incomplete
backup leaves the phone as it was. Then:

1. Photos are encrypted under this phone's key into a staging folder as they are read.
2. Every table is emptied and refilled in one database transaction.
3. The old photos are removed and the staged ones moved into place.
4. Settings are put back, apart from the app-lock ones.

If the app dies between steps 2 and 3, the next launch finishes step 3: staged photos the
database points at are moved in, and any others are removed.

A backup whose `schemaVersion` is higher than the app's is refused as "made by a newer version".
One from an older schema is accepted: its rows are inserted using the columns they have, and
newer columns take their defaults.

## Limits and open points

- The password is the whole of the protection. A weak one can be guessed however good the rest
  is. The choice is the user's: the app shows a strength hint and refuses only an empty password.
- File size and the approximate number of photos are visible to anyone holding the file. Nothing
  else is.
- AES-GCM with a counter nonce is safe for far more blocks than any backup will have.
- This has not had an independent security review. It should before the app is relied on by
  anyone but its author.
