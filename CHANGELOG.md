# Changelog

## [0.36.0](https://github.com/mcosgriff/glovebox/compare/v0.35.0...v0.36.0) (2026-11-01)


### Features

* **milestones:** continue the maker's table past its last column ([#568](https://github.com/mcosgriff/glovebox/issues/568)) ([#587](https://github.com/mcosgriff/glovebox/issues/587)) ([9b63590](https://github.com/mcosgriff/glovebox/commit/9b63590fdb106ce26b3416fe6d8ea4b5186a1249))
* **schedule:** tell apart cars vPIC names alike ([#569](https://github.com/mcosgriff/glovebox/issues/569)) ([#590](https://github.com/mcosgriff/glovebox/issues/590)) ([4efe4e0](https://github.com/mcosgriff/glovebox/commit/4efe4e0cea8ea92d5165ed260ada210afe6f6daa))
* **service-log:** offer "Done with these?" on edit and import ([#593](https://github.com/mcosgriff/glovebox/issues/593)) ([#598](https://github.com/mcosgriff/glovebox/issues/598)) ([aff06ec](https://github.com/mcosgriff/glovebox/commit/aff06ec380b522fddc6838fb858676087c072968))
* **service-log:** shop-recommended work as items to keep, note or let go ([#572](https://github.com/mcosgriff/glovebox/issues/572)) ([#592](https://github.com/mcosgriff/glovebox/issues/592)) ([dabbcf9](https://github.com/mcosgriff/glovebox/commit/dabbcf9ea04cbb6d7706f74104a9a84d7dd79844))
* **vehicles:** ask which gearbox a vehicle has, so maker's lists can split DSG and automatic rows ([#549](https://github.com/mcosgriff/glovebox/issues/549)) ([#599](https://github.com/mcosgriff/glovebox/issues/599)) ([1dc6af6](https://github.com/mcosgriff/glovebox/commit/1dc6af6fa88a2b821f16c840ae83d1dd7c63ab57))


### Bug Fixes

* **export:** one record summary for the report, Markdown and timeline ([#579](https://github.com/mcosgriff/glovebox/issues/579)) ([#591](https://github.com/mcosgriff/glovebox/issues/591)) ([d39ea9d](https://github.com/mcosgriff/glovebox/commit/d39ea9d4770b1afde2c6e74fad27242f5bdaf27c))
* **schedule:** a trim that doesn't name a list never picks it on its own ([#595](https://github.com/mcosgriff/glovebox/issues/595)) ([#596](https://github.com/mcosgriff/glovebox/issues/596)) ([3db81a4](https://github.com/mcosgriff/glovebox/commit/3db81a4fba20204bcc62e0f1002996c03062fb55))
* **schedule:** one answer to what interval an item counts by ([#575](https://github.com/mcosgriff/glovebox/issues/575)) ([#581](https://github.com/mcosgriff/glovebox/issues/581)) ([b121975](https://github.com/mcosgriff/glovebox/commit/b1219756abe260ce91881af8ee3d12513b9fdd9d))
* **service-log:** say what happened to recommended work in "What was read" ([#594](https://github.com/mcosgriff/glovebox/issues/594)) ([#597](https://github.com/mcosgriff/glovebox/issues/597)) ([12bbb76](https://github.com/mcosgriff/glovebox/commit/12bbb769177d3d7d1edcbea2ea5a465d6d1a14f5))


### Code Refactoring

* **app:** screens read a vehicle through VehicleLog ([#577](https://github.com/mcosgriff/glovebox/issues/577)) ([#586](https://github.com/mcosgriff/glovebox/issues/586)) ([e746666](https://github.com/mcosgriff/glovebox/commit/e7466668c6cd8f00e996d4688642a5e34f6d3659))
* **schedule:** one place that knows a vehicle's maker's schedule ([#576](https://github.com/mcosgriff/glovebox/issues/576)) ([#583](https://github.com/mcosgriff/glovebox/issues/583)) ([c989efa](https://github.com/mcosgriff/glovebox/commit/c989efafb76195e74592d830377f60363b90aa61))
* **service-log:** Log service's draft and invoice merge in :domain ([#578](https://github.com/mcosgriff/glovebox/issues/578)) ([#589](https://github.com/mcosgriff/glovebox/issues/589)) ([a849e06](https://github.com/mcosgriff/glovebox/commit/a849e0606a988683b00cba860bb056e8c132c520))
* **service-log:** one service record model ([#574](https://github.com/mcosgriff/glovebox/issues/574)) ([#582](https://github.com/mcosgriff/glovebox/issues/582)) ([aac73c8](https://github.com/mcosgriff/glovebox/commit/aac73c858a27b41a7429bbf5de8977ce8c0f5f2f))

## [0.35.0](https://github.com/mcosgriff/glovebox/compare/v0.34.0...v0.35.0) (2026-10-31)


### Features

* **schedule:** Volkswagen 2014–2016 Golf, Jetta and wagon TDI lists ([#551](https://github.com/mcosgriff/glovebox/issues/551)) ([2c0caa8](https://github.com/mcosgriff/glovebox/commit/2c0caa8a8ab53c0443d95423f5e66389f7c24852))
* **schedule:** Volkswagen 2014–2016 Golf, Jetta and wagon TDI lists from the owner's ruling on [#551](https://github.com/mcosgriff/glovebox/issues/551) ([#561](https://github.com/mcosgriff/glovebox/issues/561)) ([2c0caa8](https://github.com/mcosgriff/glovebox/commit/2c0caa8a8ab53c0443d95423f5e66389f7c24852))
* **schedule:** Volkswagen Haldex and 2026 Golf R diff from the owner's rulings on [#554](https://github.com/mcosgriff/glovebox/issues/554) ([#557](https://github.com/mcosgriff/glovebox/issues/557)) ([76d62f9](https://github.com/mcosgriff/glovebox/commit/76d62f96ba7bc2123ddc3c25e6f4515f9b4cb97f))
* **schedule:** Volkswagen Haldex and Golf R diff from the owner's rulings on [#554](https://github.com/mcosgriff/glovebox/issues/554) ([76d62f9](https://github.com/mcosgriff/glovebox/commit/76d62f96ba7bc2123ddc3c25e6f4515f9b4cb97f))
* **schedule:** Volkswagen ID.4 and ID. Buzz maker's schedules ([#456](https://github.com/mcosgriff/glovebox/issues/456)) ([#559](https://github.com/mcosgriff/glovebox/issues/559)) ([89dbd31](https://github.com/mcosgriff/glovebox/commit/89dbd311cdbf185895fdf6ef00743ecc6001b945))
* **schedule:** Volkswagen lists from the owner's rulings on [#553](https://github.com/mcosgriff/glovebox/issues/553) ([#558](https://github.com/mcosgriff/glovebox/issues/558)) ([1ee1edf](https://github.com/mcosgriff/glovebox/commit/1ee1edfa3c7eed2596639512330c6a4512c8b914))
* **schedule:** Volkswagen lists from the reissues alone ([#552](https://github.com/mcosgriff/glovebox/issues/552)) ([#562](https://github.com/mcosgriff/glovebox/issues/562)) ([2b39c66](https://github.com/mcosgriff/glovebox/commit/2b39c66cc3fa90f925f0bf31168b46d5544f56e2))
* **schedule:** Volkswagen sister-model lists from the owner's rulings on [#555](https://github.com/mcosgriff/glovebox/issues/555) and [#556](https://github.com/mcosgriff/glovebox/issues/556) ([#560](https://github.com/mcosgriff/glovebox/issues/560)) ([aab055f](https://github.com/mcosgriff/glovebox/commit/aab055f575eb0d13a115b0d389c7b2e3dd653d04))


### Bug Fixes

* **service-log:** read a service's one-line parts header as its parts ([#484](https://github.com/mcosgriff/glovebox/issues/484)) ([#570](https://github.com/mcosgriff/glovebox/issues/570)) ([582315e](https://github.com/mcosgriff/glovebox/commit/582315e9fdf2cfe9b13be60691fe3243ccbb5175))
* **service-log:** read every Shop-Ware service and offer only open work ([#484](https://github.com/mcosgriff/glovebox/issues/484)) ([#571](https://github.com/mcosgriff/glovebox/issues/571)) ([9b113bc](https://github.com/mcosgriff/glovebox/commit/9b113bc772f85a34e870fc07d5b63f9800bf620e))

## [0.34.0](https://github.com/mcosgriff/glovebox/compare/v0.33.0...v0.34.0) (2026-10-29)


### Features

* **schedule:** oil change gets a worked-out time limit ([#517](https://github.com/mcosgriff/glovebox/issues/517)) ([#520](https://github.com/mcosgriff/glovebox/issues/520)) ([ce8f19c](https://github.com/mcosgriff/glovebox/commit/ce8f19c124f05d080cac69f1640908ab371d301d))
* **schedule:** Volkswagen e-Golf 2015 and 2016–2017 maker's schedules ([#438](https://github.com/mcosgriff/glovebox/issues/438)) ([#524](https://github.com/mcosgriff/glovebox/issues/524)) ([d0f008e](https://github.com/mcosgriff/glovebox/commit/d0f008e8ce19d286a88c2aed0980ebe096fdec6b))
* **schedule:** Volkswagen Golf 2010 and 2014 maker's schedules ([#434](https://github.com/mcosgriff/glovebox/issues/434)) ([#526](https://github.com/mcosgriff/glovebox/issues/526)) ([4fc6081](https://github.com/mcosgriff/glovebox/commit/4fc6081aeedd55d4dbc35dc713c980f3ad5c8363))
* **schedule:** Volkswagen Jetta 2014–2016 and 2018 maker's schedules ([#440](https://github.com/mcosgriff/glovebox/issues/440)) ([#529](https://github.com/mcosgriff/glovebox/issues/529)) ([12a30f6](https://github.com/mcosgriff/glovebox/commit/12a30f66c60e4c4ca109211f889e7f1b94cc9124))
* **schedule:** Volkswagen Jetta and Jetta SportWagen 2010 and 2014 maker's schedules ([#439](https://github.com/mcosgriff/glovebox/issues/439)) ([#527](https://github.com/mcosgriff/glovebox/issues/527)) ([88caed3](https://github.com/mcosgriff/glovebox/commit/88caed392f943afcc0ffca4755acd7339c41eab3))
* **schedule:** Volkswagen Jetta Hybrid 2015 and 2016 maker's schedules ([#441](https://github.com/mcosgriff/glovebox/issues/441)) ([#521](https://github.com/mcosgriff/glovebox/issues/521)) ([d2eb62d](https://github.com/mcosgriff/glovebox/commit/d2eb62de490e9e9db384dd4e7c468a458f89957c))
* **schedule:** Volkswagen maker's schedules for 16 more models ([#147](https://github.com/mcosgriff/glovebox/issues/147)) ([#548](https://github.com/mcosgriff/glovebox/issues/548)) ([a035da3](https://github.com/mcosgriff/glovebox/commit/a035da34593fa76cdf0a2288847af1b992c0a3de))
* **schedule:** Volkswagen Passat 2010 maker's schedule ([#443](https://github.com/mcosgriff/glovebox/issues/443)) ([#525](https://github.com/mcosgriff/glovebox/issues/525)) ([045c9b5](https://github.com/mcosgriff/glovebox/commit/045c9b538d3ae7537b1cff61ca4ee08ddb54b42d))
* **vehicles:** the owner's own oil change interval ([#517](https://github.com/mcosgriff/glovebox/issues/517)) ([#522](https://github.com/mcosgriff/glovebox/issues/522)) ([bcebf54](https://github.com/mcosgriff/glovebox/commit/bcebf543398567b178ee8384a8eef7c5d8dd61e0))


### Bug Fixes

* **schedule:** GTI oil change every 10,000 or 12 months ([#517](https://github.com/mcosgriff/glovebox/issues/517)) ([f7efac4](https://github.com/mcosgriff/glovebox/commit/f7efac42850c79a0099a99616e01f4d462100115))
* **schedule:** GTI oil change every 10,000 or 12 months ([#518](https://github.com/mcosgriff/glovebox/issues/518)) ([f7efac4](https://github.com/mcosgriff/glovebox/commit/f7efac42850c79a0099a99616e01f4d462100115))


### Build System

* **domain:** rerun the domain tests when a bundled list changes ([#528](https://github.com/mcosgriff/glovebox/issues/528)) ([6351da4](https://github.com/mcosgriff/glovebox/commit/6351da4863214b8f729502a4142f71b73e12a714))

## [0.33.0](https://github.com/mcosgriff/glovebox/compare/v0.32.0...v0.33.0) (2026-10-29)


### Features

* **service-log:** read a paper invoice from photos of its pages ([#513](https://github.com/mcosgriff/glovebox/issues/513)) ([#516](https://github.com/mcosgriff/glovebox/issues/516)) ([96c0fac](https://github.com/mcosgriff/glovebox/commit/96c0faca1c7f81ca9122db26dc8f56a8cc79cc9f))
* **service-log:** read a shop's emailed invoice into Log service ([#484](https://github.com/mcosgriff/glovebox/issues/484)) ([#514](https://github.com/mcosgriff/glovebox/issues/514)) ([9df9818](https://github.com/mcosgriff/glovebox/commit/9df981880d87458f8562c8a2be8e1a26ebf5c64a))

## [0.32.0](https://github.com/mcosgriff/glovebox/compare/v0.31.0...v0.32.0) (2026-10-29)


### Features

* **history:** filter by one job within a category ([#511](https://github.com/mcosgriff/glovebox/issues/511)) ([a3d49bc](https://github.com/mcosgriff/glovebox/commit/a3d49bc3823f8046796bfaaa6728990cc2d17c11))
* **history:** show the vehicle's odometer under it ([#509](https://github.com/mcosgriff/glovebox/issues/509)) ([f68b052](https://github.com/mcosgriff/glovebox/commit/f68b0522171b153135fac45928cbe972684d92b9))

## [0.31.0](https://github.com/mcosgriff/glovebox/compare/v0.30.0...v0.31.0) (2026-10-28)


### Features

* **service-log:** type or pick a part, and a band between jobs ([#506](https://github.com/mcosgriff/glovebox/issues/506)) ([8000cf7](https://github.com/mcosgriff/glovebox/commit/8000cf73c302d95eac4a8754e79619d89cf9558c))

## [0.30.0](https://github.com/mcosgriff/glovebox/compare/v0.29.0...v0.30.0) (2026-10-28)


### Features

* **service-log:** part rows and invoice charges ([#502](https://github.com/mcosgriff/glovebox/issues/502)) ([#504](https://github.com/mcosgriff/glovebox/issues/504)) ([5223efd](https://github.com/mcosgriff/glovebox/commit/5223efd6b867dfacfa87de117cc89727a40637e2))

## [0.29.0](https://github.com/mcosgriff/glovebox/compare/v0.28.0...v0.29.0) (2026-10-27)


### Features

* **export:** job prices in the Markdown export ([#481](https://github.com/mcosgriff/glovebox/issues/481)) ([#495](https://github.com/mcosgriff/glovebox/issues/495)) ([17bb9ee](https://github.com/mcosgriff/glovebox/commit/17bb9ee4e764cf3f851f65d7188b1871c7120c11))
* **export:** job prices on the PDF report ([#481](https://github.com/mcosgriff/glovebox/issues/481)) ([#498](https://github.com/mcosgriff/glovebox/issues/498)) ([73bbaa0](https://github.com/mcosgriff/glovebox/commit/73bbaa0a54a0242724a46120ad145ee88a752a66))
* **history:** labor and parts under the spending line ([#485](https://github.com/mcosgriff/glovebox/issues/485)) ([#499](https://github.com/mcosgriff/glovebox/issues/499)) ([5feb216](https://github.com/mcosgriff/glovebox/commit/5feb2166b81ede486ba24aa551e06fe039646425))
* **import:** service history version 2 carries job prices ([#481](https://github.com/mcosgriff/glovebox/issues/481)) ([#494](https://github.com/mcosgriff/glovebox/issues/494)) ([ceb84e4](https://github.com/mcosgriff/glovebox/commit/ceb84e4ba80022674ea88d78853ebd6fc5785fd0))
* **schedule:** 2026 Corolla sedan and hatchback lists, matched on body style ([#493](https://github.com/mcosgriff/glovebox/issues/493)) ([cec83a3](https://github.com/mcosgriff/glovebox/commit/cec83a38169325fdd52f1523ea05a1f357ea8e6a))
* **schedule:** 2026 Grand Highlander hybrid and Hybrid MAX lists ([#199](https://github.com/mcosgriff/glovebox/issues/199)) ([#487](https://github.com/mcosgriff/glovebox/issues/487)) ([07b906f](https://github.com/mcosgriff/glovebox/commit/07b906f015d0aaa40b3e68e3d224af164ff40dc7))
* **schedule:** Corolla 1.8L maintenance schedule, 2016–2019 (E170) ([#488](https://github.com/mcosgriff/glovebox/issues/488)) ([1d5cd78](https://github.com/mcosgriff/glovebox/commit/1d5cd78b060ba5bbca6129d028cce01607555e53)), closes [#192](https://github.com/mcosgriff/glovebox/issues/192)
* **schedule:** GTI maintenance schedules, 2017, 2018 and 2019 ([#492](https://github.com/mcosgriff/glovebox/issues/492)) ([f7b742d](https://github.com/mcosgriff/glovebox/commit/f7b742d230c3b6412d8b3efd5233d65c49ec15ca)), closes [#143](https://github.com/mcosgriff/glovebox/issues/143)
* **schedule:** Mk5 GTI lists, 2006–2009, from VW's reissues ([#490](https://github.com/mcosgriff/glovebox/issues/490)) ([6bc5670](https://github.com/mcosgriff/glovebox/commit/6bc5670da394094130954b98950550b03e7a955f)), closes [#145](https://github.com/mcosgriff/glovebox/issues/145)
* **service-log:** invoice parser and trace in :domain ([#483](https://github.com/mcosgriff/glovebox/issues/483)) ([#497](https://github.com/mcosgriff/glovebox/issues/497)) ([abcb73c](https://github.com/mcosgriff/glovebox/commit/abcb73ce0c57d2489cfff4d6e2145b331228ddfe))
* **service:** labor and parts prices per job ([#481](https://github.com/mcosgriff/glovebox/issues/481)) ([#491](https://github.com/mcosgriff/glovebox/issues/491)) ([7e9a73d](https://github.com/mcosgriff/glovebox/commit/7e9a73daae0d24d1cacbfe1d14bd249c0670024e))


### Code Refactoring

* **vehicles:** one row composable for vehicle details ([#479](https://github.com/mcosgriff/glovebox/issues/479)) ([#496](https://github.com/mcosgriff/glovebox/issues/496)) ([28f0e24](https://github.com/mcosgriff/glovebox/commit/28f0e24077cdcfb08e5aed8ed0ace23d858ae745))

## [0.28.0](https://github.com/mcosgriff/glovebox/compare/v0.27.0...v0.28.0) (2026-10-26)


### Features

* **bulletins:** say when a program's summary says it ended ([#480](https://github.com/mcosgriff/glovebox/issues/480)) ([55ecbf4](https://github.com/mcosgriff/glovebox/commit/55ecbf4619dfa81ac1bf0630e65f9eac9021c80c))
* **vehicle:** correct a saved VIN from Vehicle details ([#478](https://github.com/mcosgriff/glovebox/issues/478)) ([f4ec5be](https://github.com/mcosgriff/glovebox/commit/f4ec5be5e76cccecfea14ba4eec22ad69b0fd859))


### Bug Fixes

* **lists:** match a plug-in typed without vPIC's "(PHEV)" tag ([#476](https://github.com/mcosgriff/glovebox/issues/476)) ([89ede6a](https://github.com/mcosgriff/glovebox/commit/89ede6a552d90cb600bd133be251a508e13fbe37))

## [0.27.0](https://github.com/mcosgriff/glovebox/compare/v0.26.0...v0.27.0) (2026-10-25)


### Features

* **bulletins:** bundle the maker's free-repair bulletins for models with a schedule ([#473](https://github.com/mcosgriff/glovebox/issues/473)) ([b7e0af5](https://github.com/mcosgriff/glovebox/commit/b7e0af5e333b1c431c1b83dcb770ebce23269822)), closes [#459](https://github.com/mcosgriff/glovebox/issues/459)
* **recalls:** say when a recall's maker says not to drive, or to park outside ([#472](https://github.com/mcosgriff/glovebox/issues/472)) ([304384a](https://github.com/mcosgriff/glovebox/commit/304384a7c33a1cdee66ae1daa608dd51f18a5ee7))
* **vehicles:** keep the engine code, TPMS and chassis code, and look a saved VIN up again ([#466](https://github.com/mcosgriff/glovebox/issues/466)) ([3ec9f47](https://github.com/mcosgriff/glovebox/commit/3ec9f476ac0f03b4312f98e2b65a9181a9bc1990)), closes [#462](https://github.com/mcosgriff/glovebox/issues/462)


### Bug Fixes

* **recalls:** check recalls under the other model names NHTSA files them under ([#470](https://github.com/mcosgriff/glovebox/issues/470)) ([3d8c297](https://github.com/mcosgriff/glovebox/commit/3d8c29798e4376c8515edd8c50d3d0335b5e51a7)), closes [#460](https://github.com/mcosgriff/glovebox/issues/460)
* **vehicles:** the decode's source note counts the eight values that can be corrected ([#467](https://github.com/mcosgriff/glovebox/issues/467)) ([d660ce6](https://github.com/mcosgriff/glovebox/commit/d660ce6e62bb6af4a35284bf921ada04d86f8f97))

## [0.26.0](https://github.com/mcosgriff/glovebox/compare/v0.25.0...v0.26.0) (2026-10-25)


### Features

* **documents:** expiry dates, with a notification before they run out ([#427](https://github.com/mcosgriff/glovebox/issues/427)) ([211d7e0](https://github.com/mcosgriff/glovebox/commit/211d7e0fadf4bbad99fa90a9bda6a3222fcd99af))


### Bug Fixes

* close the low findings of the security review ([#426](https://github.com/mcosgriff/glovebox/issues/426)) ([8a725d4](https://github.com/mcosgriff/glovebox/commit/8a725d4dda36d23748db8a2b8386471fa71657b8))
* **data:** don't trust a backup's contents further than its own checks ([#424](https://github.com/mcosgriff/glovebox/issues/424)) ([a5e5237](https://github.com/mcosgriff/glovebox/commit/a5e52373c646d9300f18391bbc1e1200a8a8856f))
* **data:** write each multi-step service-log change in one transaction ([#421](https://github.com/mcosgriff/glovebox/issues/421)) ([c16af3e](https://github.com/mcosgriff/glovebox/commit/c16af3e0138dac0a7a106aebd830e639260d3448)), closes [#419](https://github.com/mcosgriff/glovebox/issues/419)
* **export:** keep page one of the PDF report from cutting text short ([#428](https://github.com/mcosgriff/glovebox/issues/428)) ([4a9af5a](https://github.com/mcosgriff/glovebox/commit/4a9af5ace5e1a2676f8debec36c612695a69d00e))
* **schedule:** take up a maker's schedule in one transaction ([#432](https://github.com/mcosgriff/glovebox/issues/432)) ([208e733](https://github.com/mcosgriff/glovebox/commit/208e733b37fd34c73a9cf6fb8def9ce828accea6))


### Code Refactoring

* **log:** ask the vehicle log which reminders a change would move ([#430](https://github.com/mcosgriff/glovebox/issues/430)) ([#433](https://github.com/mcosgriff/glovebox/issues/433)) ([515966c](https://github.com/mcosgriff/glovebox/commit/515966c7a75a7dfd932df830782c54e3d49f771f))
* read a vehicle's log through VehicleLogs in :data ([#423](https://github.com/mcosgriff/glovebox/issues/423)) ([1cc317e](https://github.com/mcosgriff/glovebox/commit/1cc317edc7f6c50d0ab7485f724f7129721e8ba8)), closes [#420](https://github.com/mcosgriff/glovebox/issues/420)
* **rules:** read the rule preview through VehicleLogs ([#431](https://github.com/mcosgriff/glovebox/issues/431)) ([#457](https://github.com/mcosgriff/glovebox/issues/457)) ([dd4e4b3](https://github.com/mcosgriff/glovebox/commit/dd4e4b3d725e0e9a8778695d9682b7266b43230c))

## [0.25.0](https://github.com/mcosgriff/glovebox/compare/v0.24.0...v0.25.0) (2026-10-24)


### Features

* **export:** PDF infographic report and a live preview in the export sheet ([#409](https://github.com/mcosgriff/glovebox/issues/409)) ([ee6b7af](https://github.com/mcosgriff/glovebox/commit/ee6b7af008a56d5ba238feec8edfe8dc7832d818))
* **history:** seven colours for the timeline and the PDF report, light and dark ([#412](https://github.com/mcosgriff/glovebox/issues/412)) ([83a2495](https://github.com/mcosgriff/glovebox/commit/83a249529a3c460bbf7e825e4c49e7bf32a04599)), closes [#410](https://github.com/mcosgriff/glovebox/issues/410)


### Bug Fixes

* **build:** hold Bouncy Castle and httpclient at patched versions in the build tools ([#413](https://github.com/mcosgriff/glovebox/issues/413)) ([b805d3d](https://github.com/mcosgriff/glovebox/commit/b805d3d3fb6aa0b98802b1aa644586b0911e8f71))

## [0.24.0](https://github.com/mcosgriff/glovebox/compare/v0.23.0...v0.24.0) (2026-10-24)


### Features

* **ui:** the Top button on Reminders, Garage, Vehicle details and Settings ([#405](https://github.com/mcosgriff/glovebox/issues/405)) ([da4350b](https://github.com/mcosgriff/glovebox/commit/da4350b12692308843108302cf509f10023a1434))

## [0.23.0](https://github.com/mcosgriff/glovebox/compare/v0.22.0...v0.23.0) (2026-10-24)


### Features

* **history:** scroll the filters away with the list, and a Top button ([#402](https://github.com/mcosgriff/glovebox/issues/402)) ([c796a43](https://github.com/mcosgriff/glovebox/commit/c796a430cdc0c5bfa8d488ffdf68383ba28ec8d2))

## [0.22.0](https://github.com/mcosgriff/glovebox/compare/v0.21.0...v0.22.0) (2026-10-24)


### Features

* **history:** add the timeline view ([#401](https://github.com/mcosgriff/glovebox/issues/401)) ([6cd4957](https://github.com/mcosgriff/glovebox/commit/6cd4957069eafb06d50c667e9d9063a9dc722602))
* **milestones:** up to 60 jobs at a milestone ([#353](https://github.com/mcosgriff/glovebox/issues/353)) ([f859c15](https://github.com/mcosgriff/glovebox/commit/f859c154a7b604d118af042484df3c3ef0ed8c78))
* **schedule:** "Runs mostly on E85" switch for flex-fuel lists ([#390](https://github.com/mcosgriff/glovebox/issues/390)) ([5e36e3b](https://github.com/mcosgriff/glovebox/commit/5e36e3b76688bb3b0da2de74f29eacdf49cd5e70))
* **schedule:** 4Runner maintenance schedule, 2006, 2008, 2009 ([#355](https://github.com/mcosgriff/glovebox/issues/355)) ([82da636](https://github.com/mcosgriff/glovebox/commit/82da6368882b9b4c812a2826e93b68e84190f652))
* **schedule:** 4Runner maintenance schedule, 2007 ([#384](https://github.com/mcosgriff/glovebox/issues/384)) ([c24bd9f](https://github.com/mcosgriff/glovebox/commit/c24bd9f81e2f81c7292e0674f8ad734a643a05fc))
* **schedule:** 4Runner maintenance schedule, 2010–2016 ([#354](https://github.com/mcosgriff/glovebox/issues/354)) ([363c03f](https://github.com/mcosgriff/glovebox/commit/363c03f0e10d8355e87acf223cd7adf634d91058))
* **schedule:** 4Runner maintenance schedule, 2025–2026 ([#381](https://github.com/mcosgriff/glovebox/issues/381)) ([2a6f6af](https://github.com/mcosgriff/glovebox/commit/2a6f6afb226e0de121d319a38fc8ccd451047b9d))
* **schedule:** a maker's list can name the model's other spellings ([#351](https://github.com/mcosgriff/glovebox/issues/351)) ([d0ae340](https://github.com/mcosgriff/glovebox/commit/d0ae340bd054fc259ee51652cbd529bcbbf38a60))
* **schedule:** allWheelDriveOnly jobs on a maker's list ([#346](https://github.com/mcosgriff/glovebox/issues/346)) ([4628d44](https://github.com/mcosgriff/glovebox/commit/4628d44eb4766cce26c216b3491adbb9b9b4a478))
* **schedule:** apply the 2026-10-23 rulings on coolant, inspections and flex-fuel lists ([#389](https://github.com/mcosgriff/glovebox/issues/389)) ([b4555d2](https://github.com/mcosgriff/glovebox/commit/b4555d244d3394d9f954a00f05e60d6a330f2634))
* **schedule:** Camry 2.5L and Hybrid maintenance schedule, 2018–2024 ([#366](https://github.com/mcosgriff/glovebox/issues/366)) ([d0b35e4](https://github.com/mcosgriff/glovebox/commit/d0b35e474c58fa726bbddb131bfffd5bc20288c0))
* **schedule:** Camry maintenance schedule, 2016–2017 ([#369](https://github.com/mcosgriff/glovebox/issues/369)) ([b758639](https://github.com/mcosgriff/glovebox/commit/b75863954e04b052e9c4cd83274688a4c9b135d0))
* **schedule:** Camry maintenance schedule, 2025–2027 ([#376](https://github.com/mcosgriff/glovebox/issues/376)) ([44887bd](https://github.com/mcosgriff/glovebox/commit/44887bd74ff53514f877b562aaebc73f07162ab1))
* **schedule:** Camry V6 maintenance schedule, 2018–2024 ([#361](https://github.com/mcosgriff/glovebox/issues/361)) ([a5269b4](https://github.com/mcosgriff/glovebox/commit/a5269b43a72c648e848be047d38883506634796e))
* **schedule:** Corolla Cross maintenance schedule, 2022–2026 ([#377](https://github.com/mcosgriff/glovebox/issues/377)) ([80f96fa](https://github.com/mcosgriff/glovebox/commit/80f96fa16a8424aeea7798e296c80c3266c717e7))
* **schedule:** Corolla maintenance schedule, 2019–2026 ([#387](https://github.com/mcosgriff/glovebox/issues/387)) ([040eb89](https://github.com/mcosgriff/glovebox/commit/040eb89812eebd2a166bdb7dfe12b1ecec6d3126))
* **schedule:** Grand Highlander maintenance schedule, 2024–2026 ([#379](https://github.com/mcosgriff/glovebox/issues/379)) ([c8e7c97](https://github.com/mcosgriff/glovebox/commit/c8e7c979eab3445cd8922371ffb16f57cadf2357))
* **schedule:** GTI maintenance schedule, 2015 ([#362](https://github.com/mcosgriff/glovebox/issues/362)) ([4c7a55e](https://github.com/mcosgriff/glovebox/commit/4c7a55e2711aba01cdf0fdc4411b4bb4ef84ca63))
* **schedule:** GTI maintenance schedule, 2016 and 2020–2021 ([#383](https://github.com/mcosgriff/glovebox/issues/383)) ([8c5965a](https://github.com/mcosgriff/glovebox/commit/8c5965a38ac2ce8183047a4a2043fbc948263f47))
* **schedule:** GTI maintenance schedule, 2022–2026 ([#386](https://github.com/mcosgriff/glovebox/issues/386)) ([36e5e3a](https://github.com/mcosgriff/glovebox/commit/36e5e3ab085fb49cd840eb6575fba3342f6c6628))
* **schedule:** Highlander 2.7L maintenance schedule, 2016–2019 ([#359](https://github.com/mcosgriff/glovebox/issues/359)) ([b600851](https://github.com/mcosgriff/glovebox/commit/b6008517f0ea1e1bfefe6356562a7e5bc201049f))
* **schedule:** Highlander 3.5L maintenance schedule, 2016–2019 ([#371](https://github.com/mcosgriff/glovebox/issues/371)) ([294be58](https://github.com/mcosgriff/glovebox/commit/294be5875b158d25267d463ae9b2aa139d2ad928))
* **schedule:** Highlander maintenance schedule, 2020–2022 ([#358](https://github.com/mcosgriff/glovebox/issues/358)) ([ed43da6](https://github.com/mcosgriff/glovebox/commit/ed43da644a246d67c793dddf6d5c6956bc1e9b46))
* **schedule:** Highlander maintenance schedule, 2020–2026 ([#375](https://github.com/mcosgriff/glovebox/issues/375)) ([c9acb2d](https://github.com/mcosgriff/glovebox/commit/c9acb2d2fe7d3e06a197dc3836ec65b60d80e08e))
* **schedule:** jobs a maker's guide gives apart from its table ([#352](https://github.com/mcosgriff/glovebox/issues/352)) ([bbd99bd](https://github.com/mcosgriff/glovebox/commit/bbd99bd41ca0c96d3220e90d6667943143bef435))
* **schedule:** manualOnly jobs on a maker's list ([#350](https://github.com/mcosgriff/glovebox/issues/350)) ([1a62de8](https://github.com/mcosgriff/glovebox/commit/1a62de8e024b9793eef9179960cc13b9063a5815))
* **schedule:** Prius maintenance schedule, 2016–2022 ([#367](https://github.com/mcosgriff/glovebox/issues/367)) ([7c0d415](https://github.com/mcosgriff/glovebox/commit/7c0d4155c55f946e866cd0e9e78dc4437356f0d1))
* **schedule:** Prius maintenance schedule, 2023–2027 ([#380](https://github.com/mcosgriff/glovebox/issues/380)) ([60b5595](https://github.com/mcosgriff/glovebox/commit/60b5595090fa7c83cc97f93b9a109c4a98936fc6))
* **schedule:** RAV4 maintenance schedule, 2016–2018 ([#370](https://github.com/mcosgriff/glovebox/issues/370)) ([0911168](https://github.com/mcosgriff/glovebox/commit/0911168dc1784feebf172f2783bd8e07ced27a73))
* **schedule:** RAV4 maintenance schedule, 2019–2025 ([#368](https://github.com/mcosgriff/glovebox/issues/368)) ([00455eb](https://github.com/mcosgriff/glovebox/commit/00455eb10e33f2a7320c5f62b8888c58ea092fb8))
* **schedule:** RAV4 maintenance schedule, 2026 ([#378](https://github.com/mcosgriff/glovebox/issues/378)) ([ce444a0](https://github.com/mcosgriff/glovebox/commit/ce444a0f1b82fa892ea67b82d422c2fa62d1103d))
* **schedule:** Sequoia maintenance schedule, 2016–2018 ([#382](https://github.com/mcosgriff/glovebox/issues/382)) ([054ec68](https://github.com/mcosgriff/glovebox/commit/054ec6876bc24862632a49dae05bc2b6dd338aba))
* **schedule:** Sequoia maintenance schedule, 2019–2022 ([#357](https://github.com/mcosgriff/glovebox/issues/357)) ([a778fc3](https://github.com/mcosgriff/glovebox/commit/a778fc3b9687c1940036378f105b6f9cc08d9e8b))
* **schedule:** Sequoia maintenance schedule, 2023–2026 ([#364](https://github.com/mcosgriff/glovebox/issues/364)) ([1c9daa1](https://github.com/mcosgriff/glovebox/commit/1c9daa1174f956efcb949dfb8040df3750957c67))
* **schedule:** Sienna maintenance schedule, 2017–2020 ([#360](https://github.com/mcosgriff/glovebox/issues/360)) ([33fe21e](https://github.com/mcosgriff/glovebox/commit/33fe21ebfb96a9d8bf61ec74aee9160b6a10db33))
* **schedule:** Sienna maintenance schedule, 2021–2026 ([#372](https://github.com/mcosgriff/glovebox/issues/372)) ([123252c](https://github.com/mcosgriff/glovebox/commit/123252ca7cdbd257a219645493ae7d7a6a2b109a))
* **schedule:** Tacoma maintenance schedule, 2016–2023 ([#365](https://github.com/mcosgriff/glovebox/issues/365)) ([9bf0a7e](https://github.com/mcosgriff/glovebox/commit/9bf0a7e5238d1248291863660317d4efbb531251))
* **schedule:** Tacoma maintenance schedule, 2024–2026 ([#373](https://github.com/mcosgriff/glovebox/issues/373)) ([c3a78e5](https://github.com/mcosgriff/glovebox/commit/c3a78e52f22a2c6f135ab6b6eaeb36157977dd3c))
* **schedule:** tell a hybrid or flex-fuel model from its petrol twin ([#349](https://github.com/mcosgriff/glovebox/issues/349)) ([d3cf164](https://github.com/mcosgriff/glovebox/commit/d3cf16407a265d1be038db76d0b4083870cfb7a6))
* **schedule:** Tundra maintenance schedule, 2016–2019 ([#385](https://github.com/mcosgriff/glovebox/issues/385)) ([af7cc90](https://github.com/mcosgriff/glovebox/commit/af7cc90c201e04f2f3dbb41bc866013ca4721d8f))
* **schedule:** Tundra maintenance schedule, 2020–2021 ([#356](https://github.com/mcosgriff/glovebox/issues/356)) ([904539d](https://github.com/mcosgriff/glovebox/commit/904539d7d17b509cf5015c693db7efd8dbd32508))
* **schedule:** Tundra maintenance schedule, 2022–2026 ([#374](https://github.com/mcosgriff/glovebox/issues/374)) ([35a1573](https://github.com/mcosgriff/glovebox/commit/35a1573835bc430a7c74eb13c7a24f82801eaa1d))
* **service:** inverter coolant, intercooler coolant and hybrid battery filter lines ([#348](https://github.com/mcosgriff/glovebox/issues/348)) ([7e1563a](https://github.com/mcosgriff/glovebox/commit/7e1563a90900a7172733892e8e7c47d59103d91e))

## [0.21.0](https://github.com/mcosgriff/glovebox/compare/v0.20.0...v0.21.0) (2026-10-23)


### Features

* **domain:** LineGuess, a guess at the line for unfamiliar wording ([#335](https://github.com/mcosgriff/glovebox/issues/335)) ([11d3f56](https://github.com/mcosgriff/glovebox/commit/11d3f561fc59249b3747152ee32033eddd46ef5c)), closes [#312](https://github.com/mcosgriff/glovebox/issues/312)
* **history:** guesses on the sort page, Log service and the Import preview ([#337](https://github.com/mcosgriff/glovebox/issues/337)) ([59a1334](https://github.com/mcosgriff/glovebox/commit/59a1334c4efc3045d9031b667a40442ed1ed5368))


### Bug Fixes

* **history:** the record's delete sheet is a GloveboxSheet ([#343](https://github.com/mcosgriff/glovebox/issues/343)) ([2b9e26f](https://github.com/mcosgriff/glovebox/commit/2b9e26fcb507f88d561b8be47005246198f6b20f))


### Code Refactoring

* [#297](https://github.com/mcosgriff/glovebox/issues/297)'s :data section ([#342](https://github.com/mcosgriff/glovebox/issues/342)) ([861779a](https://github.com/mcosgriff/glovebox/commit/861779a2ebc1698f6ac0e167fa6f07e0c65dbbf7))
* [#297](https://github.com/mcosgriff/glovebox/issues/297)'s :domain section ([#341](https://github.com/mcosgriff/glovebox/issues/341)) ([44f4dc7](https://github.com/mcosgriff/glovebox/commit/44f4dc782a1d544635dc651325d78a3a17431cdb))
* schedule rows, rules out of view models, adding a vehicle ([#297](https://github.com/mcosgriff/glovebox/issues/297)) ([#339](https://github.com/mcosgriff/glovebox/issues/339)) ([69725ee](https://github.com/mcosgriff/glovebox/commit/69725eee5b82c849609d8ddbbe20d1cc2a279a12))
* the rest of [#297](https://github.com/mcosgriff/glovebox/issues/297)'s app plumbing ([#340](https://github.com/mcosgriff/glovebox/issues/340)) ([ad23eb8](https://github.com/mcosgriff/glovebox/commit/ad23eb8630370368155b7c588b42bab3eaeb3b35))
* vehicle name, a vehicle's log, and What it takes writes ([#297](https://github.com/mcosgriff/glovebox/issues/297)) ([#338](https://github.com/mcosgriff/glovebox/issues/338)) ([22a1189](https://github.com/mcosgriff/glovebox/commit/22a1189399f1ac854ddee1f8eeeaeca748084c93))

## [0.20.0](https://github.com/mcosgriff/glovebox/compare/v0.19.0...v0.20.0) (2026-10-22)


### Features

* **catalog:** A/C flush, wiper check and secondary air pump lines ([#334](https://github.com/mcosgriff/glovebox/issues/334)) ([cfb7057](https://github.com/mcosgriff/glovebox/commit/cfb70577e8493652dd6aa85b7d4cce67eb914c8a))
* **data:** link service records into visits (data and domain) ([#326](https://github.com/mcosgriff/glovebox/issues/326)) ([34618ab](https://github.com/mcosgriff/glovebox/commit/34618abee1ba5b0261925277c17b3ab409d6b2a9))
* **export:** a visit as one entry in the PDF and Markdown ([#331](https://github.com/mcosgriff/glovebox/issues/331)) ([005c89d](https://github.com/mcosgriff/glovebox/commit/005c89db4264b86d85d2d0eb77c29dabe356fcd0)), closes [#307](https://github.com/mcosgriff/glovebox/issues/307)
* **history:** a visit as one card ([#329](https://github.com/mcosgriff/glovebox/issues/329)) ([8fcfa9e](https://github.com/mcosgriff/glovebox/commit/8fcfa9eeeb726860516a552a9b88696d6dd4c718)), closes [#307](https://github.com/mcosgriff/glovebox/issues/307)
* **history:** link records into a visit from the record screen ([#328](https://github.com/mcosgriff/glovebox/issues/328)) ([854cad5](https://github.com/mcosgriff/glovebox/commit/854cad574d3ec8ebce458fa7b0ee3735ed12bbea)), closes [#307](https://github.com/mcosgriff/glovebox/issues/307)
* **recalls:** add a recall's record to the visit it was done on ([#330](https://github.com/mcosgriff/glovebox/issues/330)) ([ace149f](https://github.com/mcosgriff/glovebox/commit/ace149fec9a7a455161081fd8010faa3b83d9ae1)), closes [#307](https://github.com/mcosgriff/glovebox/issues/307)
* **service:** collapsible categories in File under a line ([#332](https://github.com/mcosgriff/glovebox/issues/332)) ([0c45ad0](https://github.com/mcosgriff/glovebox/commit/0c45ad08f15c7c9ca265041020d264c32d717351))


### Bug Fixes

* **history:** say how many ways Custom lines are written ([#333](https://github.com/mcosgriff/glovebox/issues/333)) ([f93ed0b](https://github.com/mcosgriff/glovebox/commit/f93ed0ba94f753b935c36cc693f000ae3f17997f))

## [0.19.0](https://github.com/mcosgriff/glovebox/compare/v0.18.0...v0.19.0) (2026-10-22)


### Features

* **catalog:** more service lines, from Wikipedia's list of auto parts ([#323](https://github.com/mcosgriff/glovebox/issues/323)) ([0662d51](https://github.com/mcosgriff/glovebox/commit/0662d514fa3b1e1fb2369c6420cc5528d2034a2a))


### Bug Fixes

* **ui:** Undo bars last 10 seconds and can be dismissed ([#321](https://github.com/mcosgriff/glovebox/issues/321)) ([ab7e235](https://github.com/mcosgriff/glovebox/commit/ab7e235997ae0836a733f9061569d52c6bcbd2a0))

## [0.18.0](https://github.com/mcosgriff/glovebox/compare/v0.17.0...v0.18.0) (2026-10-21)


### Features

* **data:** category rules and Custom line grouping (no screens yet) ([#317](https://github.com/mcosgriff/glovebox/issues/317)) ([d4b175b](https://github.com/mcosgriff/glovebox/commit/d4b175bfccf710bcb45dc120c0a9d1daf7229618))
* **history:** sort Custom lines in bulk, with remembered rules ([#318](https://github.com/mcosgriff/glovebox/issues/318)) ([82d211e](https://github.com/mcosgriff/glovebox/commit/82d211e08cbbefe339c09e2b8e9dbdeccc35e7c9))
* **import:** apply category rules to imports and Log service ([#319](https://github.com/mcosgriff/glovebox/issues/319)) ([ef5e652](https://github.com/mcosgriff/glovebox/commit/ef5e6525d04731adcc5c60f8f51160b1a463f943))
* **readings:** mark only the readings the chart has room for ([#296](https://github.com/mcosgriff/glovebox/issues/296)) ([6bc8cc0](https://github.com/mcosgriff/glovebox/commit/6bc8cc07d8013dd41ecc7cb860bceaaf6caaa8d8))
* **service:** file a custom line under a line, keeping its wording ([#316](https://github.com/mcosgriff/glovebox/issues/316)) ([f76d779](https://github.com/mcosgriff/glovebox/commit/f76d779bd7b926a1a8eb71c1166e3648b6ba99ed)), closes [#309](https://github.com/mcosgriff/glovebox/issues/309)
* **service:** more catalog lines and Carfax wording, so less work is Custom ([#314](https://github.com/mcosgriff/glovebox/issues/314)) ([353e331](https://github.com/mcosgriff/glovebox/commit/353e331b10deea3e20f2191d687efbe6b1c043b4))
* **service:** offer matching lines when a custom item is typed ([#315](https://github.com/mcosgriff/glovebox/issues/315)) ([e8d4bf4](https://github.com/mcosgriff/glovebox/commit/e8d4bf494bc923fe0f81d681fedd355418c0a7be))
* **settings:** a page for your category rules ([#320](https://github.com/mcosgriff/glovebox/issues/320)) ([65707d4](https://github.com/mcosgriff/glovebox/commit/65707d4ed0f61d511100d6d7e719c7b19bbed265))


### Bug Fixes

* six small bugs found by the refactoring scan ([#297](https://github.com/mcosgriff/glovebox/issues/297)) ([#299](https://github.com/mcosgriff/glovebox/issues/299)) ([43c7321](https://github.com/mcosgriff/glovebox/commit/43c73211c94b7cef2dc6a4e88e514d1e7b58474f))


### Code Refactoring

* **theme:** corners from the theme's shapes ([#313](https://github.com/mcosgriff/glovebox/issues/313)) ([820d074](https://github.com/mcosgriff/glovebox/commit/820d0748c1c44a10a62970d3f2610b9f123f9bda))
* **ui:** move shared composables into ui/components ([#300](https://github.com/mcosgriff/glovebox/issues/300)) ([0c04b41](https://github.com/mcosgriff/glovebox/commit/0c04b41a8f40b3c90d256310dd70811ab3e1eb87))
* **ui:** one button per role ([#308](https://github.com/mcosgriff/glovebox/issues/308)) ([30bb78d](https://github.com/mcosgriff/glovebox/commit/30bb78d6ef87dd8a70071b0ef0f9c2fd90e15403))
* **ui:** one empty state for a vehicle's lists ([#311](https://github.com/mcosgriff/glovebox/issues/311)) ([4f98b81](https://github.com/mcosgriff/glovebox/commit/4f98b815bb60319cc62dbfb5072c4fa08e1e338d))
* **ui:** one filter chip, the canvas's white pill ([#305](https://github.com/mcosgriff/glovebox/issues/305)) ([8c4cc07](https://github.com/mcosgriff/glovebox/commit/8c4cc07f7255639caa00138aa58dee3af0d2f4fd))
* **ui:** one pill and one tag ([#306](https://github.com/mcosgriff/glovebox/issues/306)) ([8a9b21a](https://github.com/mcosgriff/glovebox/commit/8a9b21a6ca0c8fd242e9e433b135bca88f164491))
* **ui:** shared cards, screen header and bottom sheet ([#301](https://github.com/mcosgriff/glovebox/issues/301)) ([75e6f36](https://github.com/mcosgriff/glovebox/commit/75e6f36f88983adafd32029ac40867545e559d8b))
* **ui:** shared rows and choice dialogs ([#304](https://github.com/mcosgriff/glovebox/issues/304)) ([f35a3d8](https://github.com/mcosgriff/glovebox/commit/f35a3d8f56eb06f36957d3f1d1799aa7b8ad6f6e))

## [0.17.0](https://github.com/mcosgriff/glovebox/compare/v0.16.0...v0.17.0) (2026-10-20)


### Features

* **readings:** choose the chart's range a year at a time ([#294](https://github.com/mcosgriff/glovebox/issues/294)) ([e0e9597](https://github.com/mcosgriff/glovebox/commit/e0e9597576944dfe327cdceff6e9c70f5efce02f))

## [0.16.0](https://github.com/mcosgriff/glovebox/compare/v0.15.0...v0.16.0) (2026-10-20)


### Features

* **history:** search records, and offer shops already used on Log service ([#293](https://github.com/mcosgriff/glovebox/issues/293)) ([ddf89d3](https://github.com/mcosgriff/glovebox/commit/ddf89d31456fbf8b9c831240b2451ab880cab7fa)), closes [#284](https://github.com/mcosgriff/glovebox/issues/284)
* **history:** what a vehicle has cost, in one line ([#292](https://github.com/mcosgriff/glovebox/issues/292)) ([145d8da](https://github.com/mcosgriff/glovebox/commit/145d8dac24d82ee3d1748711811595290085de05)), closes [#285](https://github.com/mcosgriff/glovebox/issues/285)
* **notifications:** mileage check-in by any number of days, per vehicle, at its own time ([#289](https://github.com/mcosgriff/glovebox/issues/289)) ([8d4eb6a](https://github.com/mcosgriff/glovebox/commit/8d4eb6acd5e135e305b38b49f56e3ebd6f6bf43a))
* **readings:** chart the odometer over time, with miles a year ([#291](https://github.com/mcosgriff/glovebox/issues/291)) ([002d8e6](https://github.com/mcosgriff/glovebox/commit/002d8e6625a23497dec9ad3d5381ab6e45f43fd7)), closes [#288](https://github.com/mcosgriff/glovebox/issues/288)

## [0.15.0](https://github.com/mcosgriff/glovebox/compare/v0.14.0...v0.15.0) (2026-10-19)


### Features

* **history:** sort by date or mileage, either way round ([ccdd0b8](https://github.com/mcosgriff/glovebox/commit/ccdd0b8a5c892553a6b05eb5c240ca569ea41d2c))

## [0.14.0](https://github.com/mcosgriff/glovebox/compare/v0.13.0...v0.14.0) (2026-10-18)


### Features

* launcher shortcuts for Log service and Update mileage ([#281](https://github.com/mcosgriff/glovebox/issues/281)) ([0b12f8a](https://github.com/mcosgriff/glovebox/commit/0b12f8adea385e1883e6f7f073e125594367aa41)), closes [#235](https://github.com/mcosgriff/glovebox/issues/235)

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
