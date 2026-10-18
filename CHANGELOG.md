# Changelog

## [0.13.0](https://github.com/mcosgriff/glovebox/compare/v0.12.0...v0.13.0) (2026-10-18)


### Features

* fill "What it takes" from a maker's manual, and link the maker's documents ([#277](https://github.com/mcosgriff/glovebox/issues/277)) ([6dbe1e1](https://github.com/mcosgriff/glovebox/commit/6dbe1e13fb52ea29d1bf392b376ce7bf247877ae))

## [0.12.0](https://github.com/mcosgriff/glovebox/compare/v0.11.1...v0.12.0) (2026-10-18)


### Features

* target Android 17 (API 37) ([#249](https://github.com/mcosgriff/glovebox/issues/249)) ([e23936a](https://github.com/mcosgriff/glovebox/commit/e23936acc97fcf51d55d5d47997f54be4bf4298b))
* **widget:** add a home-screen widget, off by default ([#276](https://github.com/mcosgriff/glovebox/issues/276)) ([1a16b43](https://github.com/mcosgriff/glovebox/commit/1a16b430b2a30f5a85734954223d6d1d16e351a4)), closes [#238](https://github.com/mcosgriff/glovebox/issues/238)


### Code Refactoring

* **garage:** move Garage, Add vehicle and scanner wording into string resources ([#255](https://github.com/mcosgriff/glovebox/issues/255)) ([d93fc62](https://github.com/mcosgriff/glovebox/commit/d93fc6225400e42e18804bfb293f6d6fb558db7b)), closes [#236](https://github.com/mcosgriff/glovebox/issues/236)
* **i18n:** move the shared formatters into string resources ([#254](https://github.com/mcosgriff/glovebox/issues/254)) ([ce88fb3](https://github.com/mcosgriff/glovebox/commit/ce88fb30c323249faffed9fb66af34a6c2326a98)), closes [#236](https://github.com/mcosgriff/glovebox/issues/236)
* move user-facing text into string resources ([#252](https://github.com/mcosgriff/glovebox/issues/252)) ([7823978](https://github.com/mcosgriff/glovebox/commit/782397826f97e8caa7b1453804ae4135ee5bf24e))
* **navigation:** move to Navigation 3 ([#268](https://github.com/mcosgriff/glovebox/issues/268)) ([6ab51b1](https://github.com/mcosgriff/glovebox/commit/6ab51b1774c9b65183e2fdacaf6131dd2516498e)), closes [#237](https://github.com/mcosgriff/glovebox/issues/237)
* **reminders:** move Reminders, Reminder rules and Recalls wording into string resources ([#260](https://github.com/mcosgriff/glovebox/issues/260)) ([834028f](https://github.com/mcosgriff/glovebox/commit/834028f5fd1c4ec6e82d36d8c36ddd495e40bdb4)), closes [#236](https://github.com/mcosgriff/glovebox/issues/236)
* **schedule:** move Schedule, setup, last done and Milestones wording into string resources ([#259](https://github.com/mcosgriff/glovebox/issues/259)) ([1552366](https://github.com/mcosgriff/glovebox/commit/15523666615ece4a81dbdc5df37b3e6c816d941d)), closes [#236](https://github.com/mcosgriff/glovebox/issues/236)
* **service:** move Log service, History and photo wording into string resources ([#258](https://github.com/mcosgriff/glovebox/issues/258)) ([062858a](https://github.com/mcosgriff/glovebox/commit/062858a586c915fe0e4679f6b60040c25550511c)), closes [#236](https://github.com/mcosgriff/glovebox/issues/236)
* **settings:** move Settings, notifications, backup, restore and About wording into string resources ([#262](https://github.com/mcosgriff/glovebox/issues/262)) ([d9f6dd3](https://github.com/mcosgriff/glovebox/commit/d9f6dd3baa75484831b1449840224d20795c3ad0))
* **torque:** move Torque specs wording into string resources ([#261](https://github.com/mcosgriff/glovebox/issues/261)) ([22e5fd3](https://github.com/mcosgriff/glovebox/commit/22e5fd3caf468b1f83b3301f224a5be9e534d1a2)), closes [#236](https://github.com/mcosgriff/glovebox/issues/236)
* **unlock:** move the unlock screen's wording into string resources ([7823978](https://github.com/mcosgriff/glovebox/commit/782397826f97e8caa7b1453804ae4135ee5bf24e)), closes [#236](https://github.com/mcosgriff/glovebox/issues/236)
* **vehicle:** move Vehicle details and Fluids, filters & parts wording into string resources ([#257](https://github.com/mcosgriff/glovebox/issues/257)) ([8ae08c6](https://github.com/mcosgriff/glovebox/commit/8ae08c67b58bcbdad08fa94c200a048005d2d7a4)), closes [#236](https://github.com/mcosgriff/glovebox/issues/236)
* **vehicle:** move Vehicle home and the vehicle sheets' wording into string resources ([#256](https://github.com/mcosgriff/glovebox/issues/256)) ([aa87360](https://github.com/mcosgriff/glovebox/commit/aa8736048a810c0ddcc81c49693a9ca38b31e034)), closes [#236](https://github.com/mcosgriff/glovebox/issues/236)
* **welcome:** move Welcome and Delete all data wording into string resources ([#253](https://github.com/mcosgriff/glovebox/issues/253)) ([935c3c3](https://github.com/mcosgriff/glovebox/commit/935c3c339b0fe77e30e166b3c88b5819f31f5369)), closes [#236](https://github.com/mcosgriff/glovebox/issues/236)


### Build System

* add a script for the developer verification ownership APK ([#274](https://github.com/mcosgriff/glovebox/issues/274)) ([b7fc27b](https://github.com/mcosgriff/glovebox/commit/b7fc27b556c9a15ad3c439ceb19e3eaf0cfd4256)), closes [#267](https://github.com/mcosgriff/glovebox/issues/267)

## [0.11.1](https://github.com/mcosgriff/glovebox/compare/v0.11.0...v0.11.1) (2026-10-17)


### Bug Fixes

* **release:** attach the APK with gh instead of action-gh-release ([#248](https://github.com/mcosgriff/glovebox/issues/248)) ([b1fc0d7](https://github.com/mcosgriff/glovebox/commit/b1fc0d76368aea53dedfa9934e84f864d84223c5))


### Build System

* **deps:** Bump actions/setup-java from 6.0.1 to 6.1.0 ([#240](https://github.com/mcosgriff/glovebox/issues/240)) ([8e4b488](https://github.com/mcosgriff/glovebox/commit/8e4b488c4a1f0a9355d06302f82c703a8072ccb5))
* **deps:** Bump androidx.test.uiautomator:uiautomator ([6e4d803](https://github.com/mcosgriff/glovebox/commit/6e4d80366d4926d02adbbab03882ddff5228c64a))
* **deps:** Bump androidx.test.uiautomator:uiautomator from 2.3.0 to 2.4.0 in the androidx group ([#243](https://github.com/mcosgriff/glovebox/issues/243)) ([6e4d803](https://github.com/mcosgriff/glovebox/commit/6e4d80366d4926d02adbbab03882ddff5228c64a))
* **deps:** Bump the everything-else group across 1 directory with 3 updates ([#244](https://github.com/mcosgriff/glovebox/issues/244)) ([a99f5aa](https://github.com/mcosgriff/glovebox/commit/a99f5aa8952aa779d7a16be0a430c505b78097c9))

## [0.11.0](https://github.com/mcosgriff/glovebox/compare/v0.10.0...v0.11.0) (2026-10-17)


### Code Refactoring

* **data:** open the database through SQLCipher's driver and Room's connections ([#231](https://github.com/mcosgriff/glovebox/issues/231)) ([af9d983](https://github.com/mcosgriff/glovebox/commit/af9d983b8c5b6c09e1f4b371596b1cc78115d18f))


### Continuous Integration

* **release:** count refactor and build commits as releasable ([#241](https://github.com/mcosgriff/glovebox/issues/241)) ([fc779a8](https://github.com/mcosgriff/glovebox/commit/fc779a895d457596067e038c9bb01434618a4ba4))

## [0.10.0](https://github.com/mcosgriff/glovebox/compare/v0.9.0...v0.10.0) (2026-10-17)


### Features

* **documents:** view documents inside the app ([#229](https://github.com/mcosgriff/glovebox/issues/229)) ([5b20e14](https://github.com/mcosgriff/glovebox/commit/5b20e14e8961ed7620923dfa30e9d0fb5f7cea96))


### Bug Fixes

* **home:** give the Log service button a label TalkBack can read ([#226](https://github.com/mcosgriff/glovebox/issues/226)) ([23a3722](https://github.com/mcosgriff/glovebox/commit/23a37228aace80d88e54b8ab7d75acd1f6ebe927))


### Performance Improvements

* add a baseline profile for faster start and scrolling ([#225](https://github.com/mcosgriff/glovebox/issues/225)) ([5624cc1](https://github.com/mcosgriff/glovebox/commit/5624cc1490e32ac70380783e7fd19e562fb0a09b))


### Code Refactoring

* **scan:** show the camera with CameraX's Compose viewfinder ([#228](https://github.com/mcosgriff/glovebox/issues/228)) ([e6c7cf4](https://github.com/mcosgriff/glovebox/commit/e6c7cf4dd7543d42005ecdfc86206e6a299068ab))

## [0.9.0](https://github.com/mcosgriff/glovebox/compare/v0.8.2...v0.9.0) (2026-10-16)


### Features

* **recalls:** make Mark as done and Archive a button group, beside Find a dealer ([#212](https://github.com/mcosgriff/glovebox/issues/212)) ([8612b66](https://github.com/mcosgriff/glovebox/commit/8612b664306066516932014d4bf11b266c4e5d10))

## [0.8.2](https://github.com/mcosgriff/glovebox/compare/v0.8.1...v0.8.2) (2026-10-16)


### Bug Fixes

* **reminders:** draw Log it in ink, so orange on a row means it is late ([#210](https://github.com/mcosgriff/glovebox/issues/210)) ([b34da8c](https://github.com/mcosgriff/glovebox/commit/b34da8c61ba8eb74da8739bd0c372e913c4d8e69))

## [0.8.1](https://github.com/mcosgriff/glovebox/compare/v0.8.0...v0.8.1) (2026-10-16)


### Bug Fixes

* **scan:** read frames the way the screen shows them, and make the scanner's panel a setting ([#208](https://github.com/mcosgriff/glovebox/issues/208)) ([e5e520a](https://github.com/mcosgriff/glovebox/commit/e5e520a5aa2c47f7746c64afb742c3545102fec6))

## [0.8.0](https://github.com/mcosgriff/glovebox/compare/v0.7.1...v0.8.0) (2026-10-16)


### Features

* **scan:** show what the VIN scanner is reading, live ([#207](https://github.com/mcosgriff/glovebox/issues/207)) ([0a8cc18](https://github.com/mcosgriff/glovebox/commit/0a8cc18bfc31d60593d734c093e2a9c0331e21cc))
* **vehicle:** keep a vehicle's documents, encrypted, with backup and wipe ([#205](https://github.com/mcosgriff/glovebox/issues/205)) ([d3bc6d0](https://github.com/mcosgriff/glovebox/commit/d3bc6d07413bfafd8b8218df1cb3095cd533f911))

## [0.7.1](https://github.com/mcosgriff/glovebox/compare/v0.7.0...v0.7.1) (2026-10-15)


### Bug Fixes

* **home:** show the interval on a reminder counting down to its first time ([#181](https://github.com/mcosgriff/glovebox/issues/181)) ([4bab3fc](https://github.com/mcosgriff/glovebox/commit/4bab3fc334bbd2fda467a787a68246f771fa1f0c))

## [0.7.0](https://github.com/mcosgriff/glovebox/compare/v0.6.0...v0.7.0) (2026-10-15)


### Features

* **home:** give overdue its own group, and log any reminder from vehicle home in one tap ([#180](https://github.com/mcosgriff/glovebox/issues/180)) ([5b597aa](https://github.com/mcosgriff/glovebox/commit/5b597aa42029619f334e9d5d9a626eea81667fda))
* **vehicle:** give fluids, filters and parts their own page ([#179](https://github.com/mcosgriff/glovebox/issues/179)) ([6365cae](https://github.com/mcosgriff/glovebox/commit/6365cae9e6ed186a842131382766b3fdf444b660))


### Bug Fixes

* **service:** make Log service's categories read as tabs, and open the one a due chip ticks ([#177](https://github.com/mcosgriff/glovebox/issues/177)) ([d58e4fd](https://github.com/mcosgriff/glovebox/commit/d58e4fd65fc1702f03d4c40ca68eafc1a63bbb49))

## [0.6.0](https://github.com/mcosgriff/glovebox/compare/v0.5.0...v0.6.0) (2026-10-15)


### Features

* **recalls:** archive a recall that doesn't apply to the vehicle ([#174](https://github.com/mcosgriff/glovebox/issues/174)) ([9973bf9](https://github.com/mcosgriff/glovebox/commit/9973bf9bf466aaf841232bb0803e56cea09185cb))
* **vehicle:** add a license plate, and clear a vehicle's service history ([#176](https://github.com/mcosgriff/glovebox/issues/176)) ([1854f29](https://github.com/mcosgriff/glovebox/commit/1854f2948aa1418b0b108caaab24687cd35cb0e2))


### Bug Fixes

* **import:** leave out mistyped Carfax readings, map more wording, and fill the odometer on a tap ([#173](https://github.com/mcosgriff/glovebox/issues/173)) ([3ef84fe](https://github.com/mcosgriff/glovebox/commit/3ef84fe9320781463c81ad1576073af3e89cc57f))

## [0.5.0](https://github.com/mcosgriff/glovebox/compare/v0.4.0...v0.5.0) (2026-10-15)


### Features

* **import:** import the service visits from a Carfax report PDF ([#169](https://github.com/mcosgriff/glovebox/issues/169)) ([7fd0e32](https://github.com/mcosgriff/glovebox/commit/7fd0e326ef8bb6e40677fb48a658ec7f6b07d54e))


### Bug Fixes

* **vehicle:** pick the drive type from FWD, RWD, AWD and 4WD when adding by VIN ([#170](https://github.com/mcosgriff/glovebox/issues/170)) ([0c6fb85](https://github.com/mcosgriff/glovebox/commit/0c6fb854e275073d76604b6f047e3ee63b231a98))
* **vehicle:** say when NHTSA has no trim for a VIN ([#172](https://github.com/mcosgriff/glovebox/issues/172)) ([a4578a0](https://github.com/mcosgriff/glovebox/commit/a4578a0fcc26ac9238604f06719089b49053462c))

## [0.4.0](https://github.com/mcosgriff/glovebox/compare/v0.3.2...v0.4.0) (2026-10-15)


### Features

* **scan:** read VIN barcodes with zxing-cpp, and Data Matrix and Code 128 too ([#162](https://github.com/mcosgriff/glovebox/issues/162)) ([2d59486](https://github.com/mcosgriff/glovebox/commit/2d59486672de5e8dcc299bd37437ade7438a1579))
* **vehicle:** check what is entered under "What it takes" ([#165](https://github.com/mcosgriff/glovebox/issues/165)) ([d9ad26e](https://github.com/mcosgriff/glovebox/commit/d9ad26e6bffe48d911757d6687c1554b31501421))
* **vehicle:** long-press the VIN on Vehicle details to copy it ([#164](https://github.com/mcosgriff/glovebox/issues/164)) ([bc5bb34](https://github.com/mcosgriff/glovebox/commit/bc5bb34e6a3334243bbe8aca3e822cd52f10f830))
* **vehicle:** more of what a vehicle takes, tire pressures, copy part numbers, and put it in exports ([#167](https://github.com/mcosgriff/glovebox/issues/167)) ([546c3f8](https://github.com/mcosgriff/glovebox/commit/546c3f82a779b571780f07c512421406ac7774fd))

## [0.3.2](https://github.com/mcosgriff/glovebox/compare/v0.3.1...v0.3.2) (2026-10-15)


### Bug Fixes

* **scan:** read a door-jamb barcode faster, and don't call it "not a VIN" after one misread ([#156](https://github.com/mcosgriff/glovebox/issues/156)) ([a760af6](https://github.com/mcosgriff/glovebox/commit/a760af6b518864a6a821a91fedd3a4f39567598c))

## [0.3.1](https://github.com/mcosgriff/glovebox/compare/v0.3.0...v0.3.1) (2026-10-15)


### Bug Fixes

* **release:** run the unit tests that exist before building the release ([#154](https://github.com/mcosgriff/glovebox/issues/154)) ([19e5c17](https://github.com/mcosgriff/glovebox/commit/19e5c17342f9130a499d8ee8fddf54e46b9e79c5))

## [0.3.0](https://github.com/mcosgriff/glovebox/compare/v0.2.0...v0.3.0) (2026-10-14)


### Features

* **about:** about screen with privacy and open-source licences ([#94](https://github.com/mcosgriff/glovebox/issues/94)) ([bd2a5c9](https://github.com/mcosgriff/glovebox/commit/bd2a5c9ecf36e766f32e0a30d5406bc717af3f11)), closes [#26](https://github.com/mcosgriff/glovebox/issues/26)
* **backup:** create an encrypted backup file ([#88](https://github.com/mcosgriff/glovebox/issues/88)) ([c99eb64](https://github.com/mcosgriff/glovebox/commit/c99eb64a03c00697730a26c7e34ec1f18bba604e))
* **backup:** restore from a backup file ([#89](https://github.com/mcosgriff/glovebox/issues/89)) ([1eed8fc](https://github.com/mcosgriff/glovebox/commit/1eed8fc6dd1bfb69a8000784bb7add7304d3b7d9))
* **export:** export service history as PDF or Markdown ([#85](https://github.com/mcosgriff/glovebox/issues/85)) ([42f4d68](https://github.com/mcosgriff/glovebox/commit/42f4d6846fe11c0bf6df9e2a6682b94f2109077a))
* **export:** save a schedule and torque specs as import files ([#122](https://github.com/mcosgriff/glovebox/issues/122)) ([19a2180](https://github.com/mcosgriff/glovebox/commit/19a2180515f3cca8af29f37463e07089f0415e0f))
* **history:** land on History after a save or delete, and keep Undo for 30 seconds ([#97](https://github.com/mcosgriff/glovebox/issues/97)) ([d4f6919](https://github.com/mcosgriff/glovebox/commit/d4f69197656c1f82e4f20ac1fce5f31461d646cb)), closes [#30](https://github.com/mcosgriff/glovebox/issues/30)
* **import:** a milestone table that repeats (repeatsEveryMiles) ([#117](https://github.com/mcosgriff/glovebox/issues/117)) ([ca170f9](https://github.com/mcosgriff/glovebox/commit/ca170f9b930000807df3c05a11a831b8e6817a83))
* **import:** import a maintenance schedule by interval ([#109](https://github.com/mcosgriff/glovebox/issues/109)) ([8bc10a7](https://github.com/mcosgriff/glovebox/commit/8bc10a7b21a25fa26695d9f7de30056669903ee6))
* **import:** import a milestone table and show it on Milestones ([#112](https://github.com/mcosgriff/glovebox/issues/112)) ([432c8e4](https://github.com/mcosgriff/glovebox/commit/432c8e40cb1876a1c571f5879291b14ffde1e09f)), closes [#105](https://github.com/mcosgriff/glovebox/issues/105)
* **import:** import previous maintenance as service history ([#110](https://github.com/mcosgriff/glovebox/issues/110)) ([f61b21a](https://github.com/mcosgriff/glovebox/commit/f61b21a9b2afebe1d822cab802e652f5332cee35))
* **import:** import torque specs from a JSON file, with published schemas ([#108](https://github.com/mcosgriff/glovebox/issues/108)) ([b8ccfdb](https://github.com/mcosgriff/glovebox/commit/b8ccfdb66b59c4c2a3bd1d8e5cfdddb7c62b8681))
* **import:** let a file give distances in kilometres ([#130](https://github.com/mcosgriff/glovebox/issues/130)) ([2666aee](https://github.com/mcosgriff/glovebox/commit/2666aee1e5538550ecd37de5314202a2d5429f91))
* **lock:** unlock screen to the design, with cancel and error handling ([#95](https://github.com/mcosgriff/glovebox/issues/95)) ([dd6de53](https://github.com/mcosgriff/glovebox/commit/dd6de533700f8c9f415fd488139a3d2dc4aaeba3)), closes [#27](https://github.com/mcosgriff/glovebox/issues/27)
* **log:** match the Log service form to the rendering ([#98](https://github.com/mcosgriff/glovebox/issues/98)) ([d5b58b1](https://github.com/mcosgriff/glovebox/commit/d5b58b132371a35bc8cb8f53286159602f3fc36c)), closes [#31](https://github.com/mcosgriff/glovebox/issues/31)
* **mileage:** read the odometer from the dash with the camera ([#124](https://github.com/mcosgriff/glovebox/issues/124)) ([d6b615a](https://github.com/mcosgriff/glovebox/commit/d6b615a3dc6a657334b00f750ca2bde7bf9c38a7)), closes [#64](https://github.com/mcosgriff/glovebox/issues/64)
* **mileage:** view, correct and delete past odometer readings ([#125](https://github.com/mcosgriff/glovebox/issues/125)) ([d8d0e66](https://github.com/mcosgriff/glovebox/commit/d8d0e66820ba533efbf224d393032e532fe93501))
* **nav:** predictive back, opaque screens and History state that survives a restart ([#99](https://github.com/mcosgriff/glovebox/issues/99)) ([e958641](https://github.com/mcosgriff/glovebox/commit/e958641353c605b0f1f9261c8365a3370cd64f92)), closes [#32](https://github.com/mcosgriff/glovebox/issues/32)
* **notify:** notify about due services, new recalls and stale mileage ([#87](https://github.com/mcosgriff/glovebox/issues/87)) ([6f8a3d0](https://github.com/mcosgriff/glovebox/commit/6f8a3d0b54352d69a85cad88304b3b95e9c12feb))
* **odometer:** offset for a replaced instrument cluster ([#100](https://github.com/mcosgriff/glovebox/issues/100)) ([bad74b4](https://github.com/mcosgriff/glovebox/commit/bad74b419de6eeac2fa9a542f75bddbdcb3a80eb)), closes [#73](https://github.com/mcosgriff/glovebox/issues/73)
* **scan:** read a printed VIN when there is no barcode ([#128](https://github.com/mcosgriff/glovebox/issues/128)) ([83c8aea](https://github.com/mcosgriff/glovebox/commit/83c8aead15e24bc13af9f436ec5d2ab3c6939311)), closes [#82](https://github.com/mcosgriff/glovebox/issues/82)
* **schedule:** build and edit your own milestone table in the app ([#119](https://github.com/mcosgriff/glovebox/issues/119)) ([b1fd3d7](https://github.com/mcosgriff/glovebox/commit/b1fd3d705e8976060564b7a26f4de2e12ca9088c))
* **schedule:** enter when a reminder was last done ([#101](https://github.com/mcosgriff/glovebox/issues/101)) ([71dc25f](https://github.com/mcosgriff/glovebox/commit/71dc25f4cb7752703f021521c5338c58802f1185))
* **schedule:** let the user's own item be reset by a built-in service line ([#116](https://github.com/mcosgriff/glovebox/issues/116)) ([ab423d5](https://github.com/mcosgriff/glovebox/commit/ab423d5c1fc1c8cc1a37575a723a49015d2a5ab0))
* **schedule:** match a maker's schedule by engine ([#139](https://github.com/mcosgriff/glovebox/issues/139)) ([6656568](https://github.com/mcosgriff/glovebox/commit/6656568bf49b6f713ebcdba067a9aa7621a9f1df))
* **schedule:** match a maker's schedule by engine, and ask when the engine isn't on record ([#134](https://github.com/mcosgriff/glovebox/issues/134)) ([6656568](https://github.com/mcosgriff/glovebox/commit/6656568bf49b6f713ebcdba067a9aa7621a9f1df))
* **schedule:** milestones, the schedule by mileage with what was done ([#111](https://github.com/mcosgriff/glovebox/issues/111)) ([de0bedb](https://github.com/mcosgriff/glovebox/commit/de0bedba9962696dc6e189b18f5f5439c5c31f4a))
* **schedule:** reminders for jobs a maker schedules only under severe use ([#137](https://github.com/mcosgriff/glovebox/issues/137)) ([5d9c3a7](https://github.com/mcosgriff/glovebox/commit/5d9c3a76dbd893f177f371123151ebb4d4615a9a))
* **schedule:** save your milestone table as an import file ([#121](https://github.com/mcosgriff/glovebox/issues/121)) ([608e23e](https://github.com/mcosgriff/glovebox/commit/608e23e8af9f76494f17ecb94cf139d9c11a471e)), closes [#118](https://github.com/mcosgriff/glovebox/issues/118)
* **schedule:** the Mk6 GTI's maintenance schedule, 2010 to 2014 ([#134](https://github.com/mcosgriff/glovebox/issues/134)) ([eb0f8f3](https://github.com/mcosgriff/glovebox/commit/eb0f8f3189b4da76951c2c3c1d9ae3b1c7e7f137))
* **schedule:** the Mk6 GTI's maintenance schedule, 2010 to 2014 ([#138](https://github.com/mcosgriff/glovebox/issues/138)) ([eb0f8f3](https://github.com/mcosgriff/glovebox/commit/eb0f8f3189b4da76951c2c3c1d9ae3b1c7e7f137))
* **schedule:** use the maker's maintenance schedule where there is one, starting with the 2017–2024 4Runner ([#135](https://github.com/mcosgriff/glovebox/issues/135)) ([8e2b19c](https://github.com/mcosgriff/glovebox/commit/8e2b19c196319878efc3b06e40819d23ce02fdbd))
* **settings:** delete all data ([#91](https://github.com/mcosgriff/glovebox/issues/91)) ([5752112](https://github.com/mcosgriff/glovebox/commit/57521126ac9f737750d28b928189cf91330f1e10)), closes [#24](https://github.com/mcosgriff/glovebox/issues/24)
* **settings:** grouped cards, lock delay and the remaining rows ([#92](https://github.com/mcosgriff/glovebox/issues/92)) ([137df8b](https://github.com/mcosgriff/glovebox/commit/137df8b8769908c5a525cb5005f5d6af9b26d106))
* **theme:** bundle IBM Plex fonts and fix dark-mode colours ([#96](https://github.com/mcosgriff/glovebox/issues/96)) ([e77569e](https://github.com/mcosgriff/glovebox/commit/e77569ef950ffd1c3a7d8cd665f9f85d3dc54d30)), closes [#28](https://github.com/mcosgriff/glovebox/issues/28)
* **torque:** known specs, starting with the 4Runner's owner's manual figures ([#132](https://github.com/mcosgriff/glovebox/issues/132)) ([d357c5e](https://github.com/mcosgriff/glovebox/commit/d357c5e7aadcdf9630aa2b2f2fe347c9c1ff0932))
* **units:** show and enter distances in kilometres, per vehicle ([#127](https://github.com/mcosgriff/glovebox/issues/127)) ([5c0a531](https://github.com/mcosgriff/glovebox/commit/5c0a5313ff0957d88822ef310e687ab24abe6957))
* **welcome:** first-run welcome screen ([#90](https://github.com/mcosgriff/glovebox/issues/90)) ([6ffbc34](https://github.com/mcosgriff/glovebox/commit/6ffbc34fdf1dc27051eaf2ea128a7ce48bb8362d)), closes [#46](https://github.com/mcosgriff/glovebox/issues/46)


### Bug Fixes

* **lock:** a correct PIN unlocks the app on Android 10 ([#131](https://github.com/mcosgriff/glovebox/issues/131)) ([1e5469d](https://github.com/mcosgriff/glovebox/commit/1e5469dfa609de7cbc727496f954bae1b2b06f8f)), closes [#27](https://github.com/mcosgriff/glovebox/issues/27)
* **service:** Log it on a check ticks the check on the Log service screen too ([#152](https://github.com/mcosgriff/glovebox/issues/152)) ([f898e31](https://github.com/mcosgriff/glovebox/commit/f898e314c07e54f1511d5a6f79fffff0d1797ba7))
* **service:** Log it on a check ticks the check, not the change that includes it ([#148](https://github.com/mcosgriff/glovebox/issues/148)) ([1f112b7](https://github.com/mcosgriff/glovebox/commit/1f112b71ad68c590957b839da63e95ef464bf762))

## [0.2.0](https://github.com/mcosgriff/glovebox/compare/v0.1.0...v0.2.0) (2026-10-11)


### Features

* **garage:** add a vehicle without a VIN ([#80](https://github.com/mcosgriff/glovebox/issues/80)) ([b786a51](https://github.com/mcosgriff/glovebox/commit/b786a51d5f7b56297554913673e864ba3c784f76))
* **garage:** show vehicle status and an across-all summary on the garage list ([#61](https://github.com/mcosgriff/glovebox/issues/61)) ([107c410](https://github.com/mcosgriff/glovebox/commit/107c41021f7680a33b5860e625c44577347cafe2))
* **garage:** split adding a vehicle into VIN entry and a confirm screen ([#79](https://github.com/mcosgriff/glovebox/issues/79)) ([4fc6a0d](https://github.com/mcosgriff/glovebox/commit/4fc6a0d140bf7fc628b4798f6db3ea1337330777))
* **photos:** attach receipt and parts photos to service records ([#84](https://github.com/mcosgriff/glovebox/issues/84)) ([2b4d077](https://github.com/mcosgriff/glovebox/commit/2b4d0773a3f0b52ed73d5230530d939013cff4f7))
* **recalls:** list recalls, mark them done and surface open ones ([#69](https://github.com/mcosgriff/glovebox/issues/69)) ([00e1273](https://github.com/mcosgriff/glovebox/commit/00e12733b1d87ebe5d5c98394754fca47bce2b89))
* **reminders:** choose the due rule, lead time and per-service overrides ([#68](https://github.com/mcosgriff/glovebox/issues/68)) ([d0b7941](https://github.com/mcosgriff/glovebox/commit/d0b7941332859a95b9820cd6e62cc93b122ffba7))
* **reminders:** list what's due across every vehicle ([#66](https://github.com/mcosgriff/glovebox/issues/66)) ([acd6557](https://github.com/mcosgriff/glovebox/commit/acd6557a28ac2e309f622845e4314e7bf4c794a2))
* **reminders:** snooze or skip a due reminder ([#67](https://github.com/mcosgriff/glovebox/issues/67)) ([d83d3a4](https://github.com/mcosgriff/glovebox/commit/d83d3a4c92ec428696fbae95b824dd60030db640))
* **scan:** scan the VIN barcode when adding a vehicle ([#81](https://github.com/mcosgriff/glovebox/issues/81)) ([65349a1](https://github.com/mcosgriff/glovebox/commit/65349a17eaf655552ed6a61228e0294bfb7d695b))
* **schedule:** open and change a vehicle's schedule after setup ([#77](https://github.com/mcosgriff/glovebox/issues/77)) ([9e7135f](https://github.com/mcosgriff/glovebox/commit/9e7135fc6ccd8e90ca62c2dbf6a41440d4ae5bc6))
* **torque:** keep a vehicle's socket sizes and torque values ([#75](https://github.com/mcosgriff/glovebox/issues/75)) ([90728ab](https://github.com/mcosgriff/glovebox/commit/90728ab17704ee5cb2ef2e07d61cd8ade78c0be8))
* **vehicle:** delete a vehicle and all its data ([#74](https://github.com/mcosgriff/glovebox/issues/74)) ([21918aa](https://github.com/mcosgriff/glovebox/commit/21918aae3cfff260a3e02e6563e2029e2b5a7f62))
* **vehicle:** mark a vehicle as sold and keep it in an archive ([#72](https://github.com/mcosgriff/glovebox/issues/72)) ([7065ad2](https://github.com/mcosgriff/glovebox/commit/7065ad2278cbcdfacf5bfa77550811aaecd44476))
* **vehicle:** update mileage from vehicle home ([#63](https://github.com/mcosgriff/glovebox/issues/63)) ([b5f7e7a](https://github.com/mcosgriff/glovebox/commit/b5f7e7a592fa6e655a3cbf460b3a00aa4cb00b32))
* **vehicle:** vehicle details with photo, nickname and what it takes ([#70](https://github.com/mcosgriff/glovebox/issues/70)) ([bc7ee3b](https://github.com/mcosgriff/glovebox/commit/bc7ee3ba29c155823ea1ccfbb071d51654980b41))


### Bug Fixes

* **lock:** return to the same screen after unlocking ([#71](https://github.com/mcosgriff/glovebox/issues/71)) ([30e200a](https://github.com/mcosgriff/glovebox/commit/30e200a4c5d4feb65eb759b34769dce035fed97b))

## 0.1.0 (2026-10-11)


### Features

* **data:** link schedule items to their template and add log queries ([f72741b](https://github.com/mcosgriff/glovebox/commit/f72741ba7eadbaa6248adc6daa911c3c53f32b42))
* **data:** record which catalog line a logged item came from ([1a76cf4](https://github.com/mcosgriff/glovebox/commit/1a76cf45d43279cf9f18f646620fce7acc9903ab))
* **domain:** add generic starter schedule and interval input parsing ([7a0ef16](https://github.com/mcosgriff/glovebox/commit/7a0ef16fb1ec58658ce0c950580d71b42412e595))
* **domain:** add service catalog, per-vehicle due statuses and input parsing ([6c20359](https://github.com/mcosgriff/glovebox/commit/6c2035981502276b06e222740cc70b909557f0b5))
* **domain:** preview reminder changes for log edits; next-due and money formatting ([175e50b](https://github.com/mcosgriff/glovebox/commit/175e50b23dbbb8952426a2442ee1bd413ce4caf1))
* **domain:** reminder progress and Coming up grouping for vehicle home ([09cc0b1](https://github.com/mcosgriff/glovebox/commit/09cc0b18c64043e6521338a57ee3970b3ad27172)), closes [#5](https://github.com/mcosgriff/glovebox/issues/5)
* **garage:** vehicle home with Coming up list and Log service button ([b765670](https://github.com/mcosgriff/glovebox/commit/b76567087f4ba3b2d8598a5003c380608b7f4e11))
* **garage:** vehicle home with Coming up list and Log service button ([83010c1](https://github.com/mcosgriff/glovebox/commit/83010c1e4714ff3fec74089d31c076b8bac47358)), closes [#5](https://github.com/mcosgriff/glovebox/issues/5)
* **history:** service history, record detail, edit and delete with undo ([54836ba](https://github.com/mcosgriff/glovebox/commit/54836ba3402042ff13f065d6ef18d678df2c6038))
* **schedule:** recommended schedule setup and schedule item editor ([0e6e97c](https://github.com/mcosgriff/glovebox/commit/0e6e97caffe64464d4b18d640d204e9ff74914ed))
* **service:** log service screen with due suggestions and garage entry point ([becbcbb](https://github.com/mcosgriff/glovebox/commit/becbcbbfe447af218723c923d9154cc5979bebe3))


### Bug Fixes

* **domain:** use the highest same-day odometer reading for driving pace ([cb77666](https://github.com/mcosgriff/glovebox/commit/cb776660fcc63df298da01afafa8545f96439361)), closes [#5](https://github.com/mcosgriff/glovebox/issues/5)
* **garage:** keep decode result and action button usable with the keyboard open ([054a7d2](https://github.com/mcosgriff/glovebox/commit/054a7d21e4d317ae4ed4464c4d94c708ebae4a04))
* **lock:** don't trap users without a screen lock; fix API 29 authenticators ([d0a0ae6](https://github.com/mcosgriff/glovebox/commit/d0a0ae6f7267f0b689432de5991c65acd55040c9))
* **lock:** use LocalActivity instead of casting LocalContext ([ef3cfea](https://github.com/mcosgriff/glovebox/commit/ef3cfeab5781943684408e31159528f9ab23c63d))
