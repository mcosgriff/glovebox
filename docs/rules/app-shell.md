# Lock, navigation and app shell

Rules for the lock, navigation, shortcuts, the widget, notifications, the welcome, Delete all data, Settings, the Privacy screen, licences. Read before changing any of it; review holds a diff to every line here.

- Unlock: what is offered comes from `Unlock.options`; a cancel says nothing. **On Android 10 the check opens the
  system's confirm-credentials screen, not `BiometricPrompt`** (androidx.biometric cancels on stop, so a correct
  PIN unlocked nothing). Don't move it back without testing the PIN fallback on `Pixel_API_29`: fingerprint,
  "Use PIN", too many wrong fingers, cancel, and PIN with no fingerprint.
- **While locked, the screens are out of composition** (no sheet or dialog is readable behind the lock), and
  their saved state and **ViewModel stores** live in `MainActivity`'s holders. Don't swap this for drawing the
  lock over a live nav host: sheets and dialogs are separate windows.
- Navigation 3: each tab is its own back stack over Garage. **Add a destination with `screen<Route>`, never
  `entry<Route>`** (it draws the page colour and hands the route to ViewModels). A route without fields is a
  `data object`. Results go back through the host's `results`. State that must survive a restart goes in
  `rememberSaveable` or a `SavedStateHandle`.
- **Notifications, the widget and shortcuts open `ShortcutActivity` (`launchIntent`), which isn't exported**; it
  puts their `Destination` in `PendingDestination` and opens `MainActivity` with nothing on its intent.
  `MainActivity` is exported, as the launcher's, so **it never reads a destination from its intent**: any app can
  start it.
- A notification or shortcut's `Destination` waits in `PendingDestination` until unlock. **Collect that flow;
  don't key an effect on it.** `DestinationViewModel.take` looks up before clearing.
- Launcher shortcuts (#235): static and generic, **no per-vehicle dynamic or pinned shortcuts**. They open
  `ShortcutActivity`, never `MainActivity` (a static shortcut's `CLEAR_TASK` would lose a half-filled form).
  The debug build has its own copy of `shortcuts.xml`; `ShortcutsTest` holds them the same.
- Home-screen widget (#238): **off by default**, because it shows the garage outside the lock. **It works nothing
  out**: rows and wording are the garage list's. Nothing on it acts.
- Notifications: `NoticeRules` decides what is said; `NoticeLogStore` is updated even when notifications are off,
  so turning them on releases no backlog. Workers skip sold vehicles. The lock-screen version says only the kind.
  A document's expiry (#283) is a kind of its own (channel `documents`, its own switch, off with the welcome's
  "notifications off"): said once 30 days out and once on the day, per date (`NoticeLog.expiryAnnounced`, keyed
  by document id), and opens that vehicle's Documents. **A new channel goes last in `Notifier.Channel`**: its
  place makes the notifications' ids.
- The welcome shows only with the flag unset and no vehicles; the flag is not backed up.
- Delete all data: typed DELETE, then the screen lock. **Anything new the app stores on disk has to be added to
  `DataWipe.erase`.** `DataWipe.finishInterrupted` runs before Hilt and completes a wipe the process didn't
  survive.
- Settings rows that lead nowhere are left out, not shown dead.
- The Privacy screen (`privacy_*` arrays in `strings.xml`) **must stay true of the code**: update it with any
  change to the network, permissions, storage, backup or export.
- Licences come from the AboutLibraries plugin at build time, **offline** (`offlineMode`: it would otherwise download
  the wording from GitHub and leave it blank when that failed). So the app carries every wording: a standard licence
  in `app/src/main/assets/licences/spdx/<SPDX id>.txt` with its name in `LicenceCatalog.STANDARD`, a library's own
  in `app/src/main/assets/licences/<group>_<artifact>.txt`. `LicencesScreenTest` fails on a licence with no wording.
