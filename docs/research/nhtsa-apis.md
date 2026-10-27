# NHTSA APIs: what more Glovebox could use

Research note, 2026-10-25. The question: can Glovebox make more use of vPIC (`vpic.nhtsa.dot.gov`) and
`api.nhtsa.gov`, within the rule that only those hosts are called and only a VIN or year/make/model is sent?
Claims about the APIs cite a source in [Sources](#sources). "Observed" means a live call made on 2026-10-25;
"inferred" means not stated by NHTSA.

## Summary: best opportunities, ranked

1. **Recall checks can miss recalls when NHTSA files them under a different model name** (#460). For a 2019
   Ford, `model=F-250` returns 4 campaigns and `model=F-250 SD` returns 12 others (none shared). vPIC
   decodes a Super Duty as "F-250". `products/vehicle/models` lists the names NHTSA uses, so the user could
   pick the extra names their vehicle is filed under. Sends only year/make/model. [S3, observed]
2. **Show the recall flags already in the response: `parkIt`, `parkOutSide`, `overTheAirUpdate`, `Notes`** (#461).
   "Don't drive it" and "park outside" are the most urgent things a recall can say, and Glovebox drops them.
   Nothing new is sent. [observed]
3. **Owner complaints by component (`complaints/complaintsByVehicle`)**: what other owners of the same
   year/make/model report, grouped by component. Sends only year/make/model. Unverified reports, so worded
   that way. [S3, observed]
4. **Keep more of the vPIC decode Glovebox already fetches** (#462): `EngineModel` (e.g. "1GR-FE"), `Turbo`,
   `TPMS`, `Series`, battery fields. Most useful is the engine code, a sharper key than displacement for
   picking a per-engine maker's list. Nothing new is sent. Also send `modelyear`, as vPIC recommends. [S1]
5. **NCAP safety ratings (`SafetyRatings`)**: star ratings plus counts of complaints, recalls and
   investigations per variant. Year/make/model, then an NHTSA variant id. Low maintenance value; a
   nice-to-have on Vehicle details. [S3, observed]

Not available: **there is no public API for TSBs / manufacturer communications or for investigations**,
only bulk downloads from `static.nhtsa.gov`, a host the network rule doesn't allow. [S3]

## Used today

All in `data/src/main/kotlin/io/cosgriff/glovebox/data/nhtsa/NhtsaClient.kt`.

| Call | Sends | Fields read | Where they go |
|---|---|---|---|
| vPIC `GET /api/vehicles/DecodeVinValues/{vin}?format=json` (`decodeVin`) | VIN (no `modelyear`) | `ModelYear`, `Make`, `Model` | `VehicleEntity.year/make/model` (correctable on "Is this your vehicle?") |
| | | `Trim` | `VinDecode.trimChoices` → `trim` (user picks when vPIC lists several) |
| | | `BodyClass` | `bodyClass`; `BodyStyles.fromVpic` → `bodyStyle` (sedan or hatchback, for lists that differ by body; never guessed from anything else) |
| | | `DriveType` | `drivetrain` → `Drivetrains.kind` (4WD/AWD/2WD) for `fourWheelDriveOnly` / `allWheelDriveOnly` jobs |
| | | `DisplacementL`, `EngineCylinders` | `engine` ("4.0L 6-cyl") → `Engines.displacementL` matches per-engine lists |
| | | `FuelTypePrimary` | `fuel` |
| | | `FuelTypePrimary`, `FuelTypeSecondary`, `ElectrificationLevel` | `Powertrains.fromVpic` → `powertrain` (hybrid, plug-in, BEV, flex-fuel; never "gas" from a blank) |
| | | `TransmissionSpeeds`, `TransmissionStyle` | `transmission` ("5-speed Automatic") → `Transmissions.isManual/isAutomatic` |
| | | `EngineModel`, `EngineHP`, `GVWR`, `PlantCity`, `PlantCountry` | **shown on the confirm step only, not stored** (`AddVehicleScreen.kt` ~556–575) |
| | | `ErrorCode`, `ErrorText` | `VinDecode.quality` (no save without a real year/make/model) |
| vPIC `GET /api/vehicles/GetMakesForVehicleType/{car,truck,mpv}` (`makes`) | nothing about the vehicle | `MakeName` | `ManualAddScreen` make picker |
| vPIC `GET /api/vehicles/GetModelsForMakeYear/make/{make}/modelyear/{year}` (`models`) | make, year | `Model_Name` | `ManualAddScreen` model picker |
| `GET api.nhtsa.gov/recalls/recallsByVehicle?make&model&modelYear` (`recalls`) | year/make/model | `NHTSACampaignNumber`, `Component`, `Summary`, `Consequence`, `Remedy`, `ReportReceivedDate` | `RecallNotice` → `RecallEntity` via `RecallRepository` (add-vehicle check, then on `recallCadence`) |

## Available endpoints

Fit: **Good** = useful and within the rule; **Maybe** = within the rule but limited value or needs a
ruling; **No** = breaks the rule or not useful.

| Endpoint | Sends | Returns | Fit |
|---|---|---|---|
| vPIC `DecodeVinValues/{vin}` [S1] | VIN, optional `modelyear` | ~150 flat fields (observed 154) | **Used.** More fields could be kept (opp. 4) |
| vPIC `DecodeVin/{vin}` [S1] | VIN | same data as key/value rows with ids | No: the flat form is easier |
| vPIC `DecodeVinValuesExtended/{vin}`, `DecodeVinExtended` [S1] | VIN | as above plus "variables related to other NHTSA programs like NCSA" | No: observed extra values were only `NCSABodyType`, `NCSAMake`, `NCSAModel` |
| vPIC `DecodeVINValuesBatch` (POST, ≤50 VINs) [S1] | VINs | flat decodes | No: one user, a few vehicles |
| vPIC `DecodeWMI/{wmi}`, `GetWMIsForManufacturer` [S1] | VIN prefix / maker | maker of a WMI | No: the full decode already names it |
| vPIC `GetAllMakes`, `GetMakesForVehicleType`, `GetModelsForMake…`, `GetVehicleTypesForMake…` [S1] | nothing / make / year | name lists | **Used** (two of them) |
| vPIC `GetVehicleVariableList`, `GetVehicleVariableValuesList` [S1] | nothing | field names, descriptions, allowed values | Maybe: build-time reference, not a runtime call |
| vPIC `GetCanadianVehicleSpecifications?year&make&model&units` [S1] | year/make/model | Transport Canada dimensions: length, width, wheelbase, curb weight, track, weight split (observed) | Maybe: little maintenance value |
| vPIC `GetManufacturerDetails`, `GetAllManufacturers`, `GetParts`, `GetEquipmentPlantCodes` [S1] | maker / dates / year | maker records, Part 565/566 filings, tire/glass plant codes | No |
| `recalls/recallsByVehicle?make&model&modelYear` [S3] | year/make/model | recalls; observed fields also include `parkIt`, `parkOutSide`, `overTheAirUpdate`, `Notes`, `Manufacturer` | **Used.** Unread flags (opp. 2) |
| `recalls/campaignNumber?campaignNumber` [S3] | a campaign number | one campaign; observed adds `PotentialNumberofUnitsAffected` | Maybe: a campaign number isn't a VIN or year/make/model, so it needs the owner's ruling |
| `products/vehicle/modelYears`, `/makes`, `/models` `?issueType=r\|c` [S3] | year, make | the year/make/model names NHTSA's recall and complaint records use | **Good** (opp. 1) |
| `complaints/complaintsByVehicle?make&model&modelYear` [S3] | year/make/model | owner complaints (opp. 3) | **Good** |
| `complaints/odinumber?odinumber` [S3] | a complaint number | one complaint | No: nothing to look up |
| `SafetyRatings/modelyear/{y}/make/{m}/model/{mo}` then `SafetyRatings/VehicleId/{id}` [S3] | year/make/model, then NHTSA's variant id | variants, then star ratings, ESC/FCW/LDW, `ComplaintsCount`, `RecallsCount`, `InvestigationCount`, picture/video links on `static.nhtsa.gov` | Maybe (opp. 5) |
| `CSSIStation/zip/{zip}`, `/state/{st}`, `?lat&long&miles` [S3] | ZIP, state or coordinates | car seat inspection stations | **No**: sends location |
| Investigations [S3] | — | no documented API; bulk `FLAT_INV.zip` on `static.nhtsa.gov` | **No** |
| Manufacturer communications / TSBs [S3] | — | no documented API; bulk `MFR_COMMS_RECEIVED_*.zip` and `TSBS_RECEIVED_*.zip` on `static.nhtsa.gov` | **No** |

Limits and terms:
- vPIC: free, no registration, "no limit to the amount of actions … per day", calls are subject to "an
  automated traffic rate control mechanism"; uptime aims at 99% in US business hours, with weekend
  maintenance; release notes by subscription. Current version 4.10, last code change 2026-10-17. [S1, S2]
- `api.nhtsa.gov`: the datasets page states no rate limit or terms. It says "Request parameters and method
  names are case-sensitive" [S3] (observed: lower-case make/model values work). Recalls data is updated
  daily, NCAP annually. [S3]

## Opportunities

### 1. Recall model names (correctness fix, not just a feature)

Observed for model year 2019 Ford:

| `model=` | recalls | complaints |
|---|---|---|
| `F-250` | 4 (18V894, 19V633, 19V688, 20V576) | 0 |
| `F-250 SD` | 12 (18V802, 19V278, … 25V572) | 8 |
| `TRANSIT` | 9 | 0 |
| `TRANSIT VAN` | 0 | 38 |

vPIC's `GetModelsForMakeYear` gives "F-250" and "Transit" [observed]. `products/vehicle/models` lists, among
others, `F-250 SD`, `F-250 CREW CAB`, `F-250 SUPERCAB`, `TRANSIT VAN`, `TRANSIT HIGH ROOF 15 PASS` [observed].
So a Super Duty saved as decoded is checked under "F-250" and misses the "F-250 SD" campaigns. The
match looks exact, not partial (inferred from the counts above).

Shape (inferred design): after decode, fetch `products/vehicle/models?modelYear&make&issueType=r`, show the
names that start with the vehicle's model, and let the owner tick the ones that are theirs; check recalls
under each and merge by campaign (`Recalls.merge` already keys by campaign). Nothing is guessed: names
are the owner's pick, per "nothing is assumed for the user". Still only year/make/model. Needs a stored list of
extra recall names per vehicle (a schema change, `docs/rules/database.md`). Observed: the `issueType=r` and
`issueType=c` lists for 2019 Ford were identical, and the recall query for `F-250` succeeded though `F-250`
wasn't in that list, so the list helps the owner choose but doesn't cover every name.

### 2. Recall flags already in the response

`recallsByVehicle` returns `parkIt`, `parkOutSide`, `overTheAirUpdate` (booleans) and `Notes` (text)
[observed; not described on the datasets page, which only says "a list of recalls" — S3]. `NhtsaClient`
drops them. A "Do not drive" / "Park outside" banner on an open recall, and "Fixed by a software update"
where it applies, are high value and need no new call. Needs columns on `recalls` (migration). All four
observed 2019 4Runner campaigns were `false` for all three flags, so test fixtures need made-up `true` cases.

### 3. Owner complaints

`complaints/complaintsByVehicle` [S3] returns `count` and `results` with, per complaint (observed):
`odiNumber`, `crash`, `fire`, `numberOfInjuries`, `numberOfDeaths`, `dateOfIncident`, `dateComplaintFiled`,
`vin` (first 11 characters of the complainant's VIN), `components` (comma list, e.g. "SUSPENSION",
"STEERING", "UNKNOWN OR OTHER"), `summary` (free text), `products`. A 2019 Toyota 4Runner had 65
complaints in 66 KB; there is no paging (observed), so a popular model could be large (inferred).

Use: "What owners report" on Vehicle details, counts by component with the summaries behind a tap. It can
point at a common failure (one observed complaint was a broken coil spring at 44k miles). Caveats: reports
are unverified and their component codes are the filer's choice; another owner's partial VIN is in the data
and should be neither shown nor stored (inferred: nothing gained by keeping it). It is model-level, not this
vehicle. Same model-name issue as opportunity 1.

### 4. More of the decode, and `modelyear`

vPIC fields Glovebox receives and drops, with maintenance value (observed on `JTEBU5JR*K5`, a partial
4Runner pattern taken from public complaint data):
- `EngineModel` ("1GR-FE"). Shown, not stored. `docs/known-schedules.md` matches engines by displacement
  and, where needed, fuel and powertrain; an engine code would tell apart two engines of one size (inferred).
  It would be a hint the owner confirms, like displacement now, never a silent pick.
- `Turbo`, `EngineConfiguration`, `ValveTrainDesign`, `OtherEngineInfo` ("PORT"), `FuelInjectionType`.
- `TPMS` ("Direct"/"Indirect"): relevant to tyre jobs (sensor batteries, relearn).
- `Series` ("GRN280L/GRN285L"): the maker's chassis code, which service manuals and parts catalogues use.
- EV/hybrid: `BatteryKWh`, `BatteryType`, `EVDriveUnit`, `ChargerLevel` [S1 variable list].
- `WheelSizeFront`/`Rear`, `CurbWeightLB`.

vPIC says "It is recommended to always send in the model year" [S1]. Glovebox sends only the VIN. The year
can be worked out from VIN position 10 (inferred; the 30-year cycle is ambiguous without position 7 on
light vehicles), and it is part of the VIN, so sending it stays within the rule.

### 5. NCAP safety ratings

Two steps [S3]: `SafetyRatings/modelyear/2019/make/toyota/model/4runner` returned two variants, "2019 Toyota
4Runner SUV AWD" (13207) and "… 2WD" (13206) [observed]; `SafetyRatings/VehicleId/13207` returned overall,
front, side, pole and rollover stars, rollover probability, whether ESC/FCW/LDW are standard, and
`ComplaintsCount` 65, `RecallsCount` 4, `InvestigationCount` 0 [observed]. The variant id is NHTSA's, derived
from year/make/model (inferred: within the rule, but worth a ruling). Picking a variant is the owner's
choice. The picture and video links point at `static.nhtsa.gov` and must not be loaded. Note NCAP called
the 4Runner "AWD" where vPIC says "4WD/4-Wheel Drive/4x4" [observed]: never feed NCAP's words into
`Drivetrains.kind`.

## Doesn't fit / not available

- **TSBs and manufacturer communications: no public API.** NHTSA publishes them only as zip files
  (`TSBS_RECEIVED_<years>.zip`, up to 31 MB; `MFR_COMMS_RECEIVED_<years>.zip`) on `static.nhtsa.gov` [S3].
  That host isn't allowed, and a 30 MB download to show one vehicle's bulletins is out of proportion.
  A guessed `api.nhtsa.gov/manufacturerCommunications/...` answered 403 "Missing Authentication Token"
  [observed], i.e. no such public route (inferred). Bundling a filtered extract in the APK would need its
  own decision.
- **Investigations: no documented API**, only `FLAT_INV.zip` on `static.nhtsa.gov` [S3]. An undocumented
  `api.nhtsa.gov/investigations` exists but ignored make/model/year filters (returned all 4,184 entries,
  paged) [observed]; undocumented, so don't build on it. NCAP's `InvestigationCount` is the only
  per-vehicle figure.
- **Recall repair status by VIN: not in the public API** [S3 lists only year/make/model and campaign-number
  lookups]. Matches `docs/rules/recalls.md`.
- **Car seat inspection stations**: sends ZIP, state or coordinates [S3]. Breaks the rule.
- `DecodeVinValuesExtended`: only NCSA naming fields beyond the plain decode [S1, observed].
- Batch decode, WMI, manufacturer, Part 565/566, equipment plant code endpoints [S1]: nothing for a
  maintenance tracker.
- Canadian Vehicle Specifications [S1]: allowed (year/make/model), but dimensions for crash reconstruction
  add little; its `MYR` was "10" for a 2019 query [observed], so which year the figures describe is unclear.

## Caveats

- **vPIC is only as full as the maker's Part 565 filing** [S1]. Fields are often blank; decoding from
  1995 on is "about … 99%" for most major brands, 1980–1994 "60-65%", pre-1980 none [S2]. Glovebox already
  treats blank as unknown; keep doing so for any new field.
- **vPIC can be wrong.** The partial 4Runner pattern decoded `SteeringLocation` "Right-Hand Drive (RHD)"
  [observed] for a US-market truck. Decoded values stay correctable and are the owner's to verify.
- **Partial decodes list options**, e.g. `Trim` "SR5/SR5 Premium/TRD Off Road/…/TRD Pro" [observed].
- **Model-level vs VIN-level**: decode is per VIN; recalls, complaints and ratings are per year/make/model
  (or NCAP variant). A recall listed may not apply to this VIN; the maker's VIN lookup decides.
- **Names differ across NHTSA's own systems** (vPIC vs recalls/complaints vs NCAP): see opportunity 1 and
  the AWD/4WD note in 5.
- **Undocumented fields can change.** `parkIt` etc. are observed, not documented [S3]; parse them as optional.
- Recalls still answer 400 with `Count` 0, an empty list and "Results returned successfully" when there is
  nothing (observed for 2019 Mercedes-Benz C-Class and a made-up make). `docs/rules/recalls.md` already
  handles this; any new `api.nhtsa.gov` call should read the body the same way (inferred).

## Sources

- [S1] vPIC API page, https://vpic.nhtsa.dot.gov/api/ (read 2026-10-25): method list, partial-VIN and
  `modelyear` advice, Extended and Batch descriptions, Canadian specs, Part 565 origin, rate control.
- [S2] vPIC FAQ, https://vpic.nhtsa.dot.gov/api/Home/Index/FAQ (read 2026-10-25): uptime, no daily
  limit, no registration, decode confidence by year.
- [S3] NHTSA Datasets and APIs, https://www.nhtsa.gov/nhtsa-datasets-and-apis. The live page answered 403
  to scripted requests; read through the Internet Archive capture of 2026-10-03,
  https://web.archive.org/web/20261003194909/https://www.nhtsa.gov/nhtsa-datasets-and-apis : Ratings,
  Recalls, Investigations, Complaints, Manufacturer Communications and Car Seat Inspection Locator sections.
- Observed calls (2026-10-25), all on the two allowed hosts:
  - https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVinValues/5UXWX7C5*BA?format=json&modelyear=2011 (NHTSA's own sample)
  - https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVinValuesExtended/5UXWX7C5*BA?format=json&modelyear=2011
  - https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVinValues/JTEBU5JR*K5?format=json&modelyear=2019 (and Extended)
  - https://vpic.nhtsa.dot.gov/api/vehicles/GetVehicleVariableList?format=json
  - https://vpic.nhtsa.dot.gov/api/vehicles/GetModelsForMakeYear/make/ford/modelyear/2019?format=json
  - https://vpic.nhtsa.dot.gov/api/vehicles/GetCanadianVehicleSpecifications/?year=2019&make=Toyota&model=4Runner&units=US&format=json
  - https://api.nhtsa.gov/recalls/recallsByVehicle?make=toyota&model=4runner&modelYear=2019 (and Ford F-250, F-250 SD, Transit, Transit Van)
  - https://api.nhtsa.gov/recalls/campaignNumber?campaignNumber=12V176000
  - https://api.nhtsa.gov/products/vehicle/models?modelYear=2019&make=ford&issueType=r (and `issueType=c`)
  - https://api.nhtsa.gov/complaints/complaintsByVehicle?make=toyota&model=4runner&modelYear=2019 (and Ford)
  - https://api.nhtsa.gov/SafetyRatings/modelyear/2019/make/toyota/model/4runner and https://api.nhtsa.gov/SafetyRatings/VehicleId/13207
  - https://api.nhtsa.gov/investigations?make=toyota&model=4runner&modelYear=2019 (undocumented)
  - https://api.nhtsa.gov/manufacturerCommunications/byVehicle?… (guessed; 403)
- Glovebox code read: `data/.../nhtsa/NhtsaClient.kt`, `data/.../nhtsa/RecallRepository.kt`,
  `app/.../ui/garage/AddVehicleScreen.kt`, `app/.../ui/garage/ManualAddScreen.kt`,
  `domain/.../Powertrains.kt`, `domain/.../Drivetrains.kt`.

## Addendum: what the TSB index holds (checked 2026-10-25, #459)

Downloaded `TSBS.txt` (the field list) and the newest chunk, `TSBS_RECEIVED_2025-2026.zip` (12.5 MB, one
415 MB tab-separated file), from `https://static.nhtsa.gov/odi/ffdd/tsbs/`, linked from the datasets page.

- 14 fields: NHTSA ID, replacement bulletin no. (deprecated), date added, the maker's TSB/document ID, the
  maker's date, the maker's campaign ID or software version, communication type, make, model, model year,
  NHTSA components, the maker's system and subsystem, and a summary (up to 4,000 characters since June 2024,
  240 before). No VIN ranges, part numbers, labour times or procedure: those are only in the PDF.
- One row per communication × model year × component, so 814,402 rows hold 23,360 communications.
- Types in that chunk: Service Bulletin/Repair Instructions 16,151; Other 3,041; Service Campaign 2,080;
  Over The Air 1,236; Warranty Program/Extension 577; Emissions 275.
- Summary length: median 179 characters.
- Toyota 4Runner: 70 communications in that chunk, some generic and stamped on every year back to 2000
  (e.g. rail-dust paint contamination). None about oil, fluids or intervals. VW: 9 to 212 per model.
- The full bulletin is a PDF at `https://static.nhtsa.gov/odi/tsbs/<year received>/MC-<NHTSA ID>-0001.pdf`:
  `MC-11012727-0001.pdf` answered 200 under `2025` and 404 under `2024`. The URL pattern is inferred from
  that one check, not documented.
- There is also a lighter CSV per chunk (`MFR_COMMS_RECEIVED_*.zip`: ID, make, model, years, summary).
