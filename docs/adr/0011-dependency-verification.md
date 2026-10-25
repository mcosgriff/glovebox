# Every dependency is verified, and a known version's checksum never changes

Release APKs go straight to phones through Obtainium, so a tampered dependency would ship to every user (#246). Every jar, POM and plugin has to match `gradle/verification-metadata.xml` (SHA-256, plus PGP keys where they are published, in `gradle/verification-keyring.keys`), or the build fails, locally and in CI. Tesseract4Android, from JitPack, is unsigned, so only its checksum vouches for it: **a changed checksum for a version already in the file is never accepted.**

## Consequences

- Dependabot doesn't update the file. `dependabot-verification.yml` regenerates it on Dependabot's PRs, in a job with a read-only token, and commits it after `verification-guard.py` checks it. Once that workflow pushes, Dependabot stops rebasing the PR; comment `@dependabot recreate` to start over.
- A hand-made dependency change needs the regeneration command in `CLAUDE.md` (Commands), and its diff is read like the version bump itself.
