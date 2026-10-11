# Changelog

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
