# Documents and photos

Rules for encrypted files, pictures, the in-app PDF viewer, pickers. Read before changing any of it; review holds a diff to every line here.

- **Every encrypted file the database points at comes from `DocumentDao.allEncryptedFiles()`**, and files are
  deleted through `PhotoCrypto.delete`. Documents are encrypted with the photo key in `photoDir`, so the sweep,
  backup, restore and crypto-erase cover them.
- Pictures are redrawn as JPEG (`redrawJpeg`), which drops metadata; a PDF is kept as it came.
- A document opens inside Glovebox (#224, the user's decision of 2026-10-16). The PDF renders in an **isolated
  process**, which starts `GloveboxApp` too, so **`GloveboxApp.onCreate` does nothing in an isolated process**;
  keep anything new there behind that check. The decrypted PDF goes through `memfd_create` (Android 11+) or
  `cache/viewing/`, deleted at once. **`pdf-ocr-play-services` must never be on the classpath**; check on an
  upgrade. Exports list documents and never carry their contents.
- Pickers and "save as" launchers are registered in `MainActivity`, because returning from them can lock the
  app and the lock replaces the nav host.
