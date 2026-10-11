# Changelog

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
