# No ML Kit: barcodes with zxing-cpp, text with Tesseract

Google's ML Kit reads barcodes and text better than most alternatives, but its bundled libraries bring Google's log-upload transport and Firebase/Play pieces, which breaks the network rule ([ADR-0007](0007-network-nhtsa-only.md)). This was asked again on 2026-10-15, and the answer is still no.

- **VIN barcodes: zxing-cpp** (#161). On a phone, Java ZXing failed on live frames of real door-jamb barcodes, and zxing-cpp decoded a photo of the same sticker cleanly; it also handles blurred and unevenly lit frames better. Its POM needs only Kotlin and CameraX, and its `.so` imports no network function (checked with pyelftools). Java ZXing is test-only, to draw barcodes.
- **Odometer from the dash: Tesseract** (`tesseract4android`, #64). Its `.so` files were checked the same way. It is only published on JitPack, so `settings.gradle.kts` lets JitPack serve that one group (`exclusiveContent`).

## Consequences

- Check the native libraries again on every upgrade of either one.
- Check any Google or Firebase library's dependency tree the same way before adding it.
- Don't widen the JitPack filter or add another JitPack dependency without asking. JitPack artifacts are unsigned, so only their checksum vouches for them ([ADR-0011](0011-dependency-verification.md)).
- Tesseract's native libraries are what made the APK large ([ADR-0009](0009-release-apk-arm64-only.md)).
