# The release APK is 64-bit ARM only, and there is one per release

Tesseract's native libraries took the release APK from about 16 MB to 49 MB across four processor types (#123). The release now carries `arm64-v8a` only, about 22 MB. That covers phones sold with Android 10 or later almost without exception. It is still a single APK, as `release.yml` and Obtainium expect.

## Considered Options

From #123:
- **One APK per ABI.** `release.yml` would upload several files and Obtainium would have to pick the right one.
- **Leave it at 49 MB.**

## Consequences

- A 32-bit ARM phone or an x86 emulator can't install a release.
- Debug builds carry every ABI. `nonMinifiedRelease` and `benchmarkRelease` also carry x86_64 for emulators, and are never released.
- Don't add an ABI back or split the APK without asking.
