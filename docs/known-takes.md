# Bundled "What it takes" lists

Glovebox can ship what a maker's own documents say a model takes (engine oil, fluids, tire sizes and
pressures, spark plugs, bulbs) for a model and run of years. Where one covers a vehicle, Vehicle
details and Fluids, filters & parts offer "Fill in from the manual for the …". This file covers the
rules for such a list, how to find a maker's documents, and where each shipped list came from. The code
is `KnownTakes` in `:domain`, and the lists are the files in `app/src/main/assets/known-takes/`. Torque
values and maintenance schedules work the same way: [known-specs.md](known-specs.md),
[known-schedules.md](known-schedules.md).

## Rules

1. **Only the maker's own documents.** That means the owner's manual, the maker's fluid capacity
   charts, a specification book or a technical bulletin. It never means a parts site, forum or
   compilation, even one that cites the manual. Web searches only help find where the maker
   published something.
2. **Every figure is cited.** The file's `source` names the documents and says why Glovebox may ship
   them (`basis`). The tables below give the page each figure is on, so anyone can check it.
3. **Facts only.** Take the grade, the capacity, the part code. Copy no text, footnote or layout.
4. **A list covers only the years whose documents were read**, and splits where a figure changes.
   The 4Runner's oil approval changed for 2022 and its bulbs for 2021, so it has three lists.
5. **Nothing that can't be known is filled.**
   - A figure the documents give by trim or option fills nothing. Tire sizes are offered as choices in
     the tire editor (`tireSizes`), never filled when there are several.
   - A figure for one drive or gearbox goes in a `variants` entry. It is used only for a vehicle
     **known** to have that drive or gearbox; one with nothing on record gets none of it.
   - A figure the document gives as a range or as alternatives, which one row can't hold, is left
     out. Examples: a plug gap of "1.0 to 1.1 mm", or "5W-40 or 5W-30".
   - A model whose engines take different figures gets a list per engine (`engine`, as in a
     schedule list). An engine that isn't on record matches none.
6. **Write every value as the row's editor stores it** (`VehicleSpecEntry`): "0W-20", "75W-85 GL-5",
   "DOT 3", "0.043 in". The reader refuses anything else and says what the stored form is.
7. **Nothing of the owner's is written over.** Filling takes only rows that are empty, each row whole.
   Every row filled is tagged DOUBLE-CHECK (`vehicles.takesToCheck`). The tag stays until the owner
   ticks "I've checked this against my manual" in that row's editor, or changes the row. The source
   is named once per screen while any row is tagged.

## The file

`app/src/main/assets/known-takes/<id>.json`, named after its id:

```json
{
  "id": "acme-roadster-2001-2005",
  "title": "2001–2005 Roadster",
  "make": "Acme", "model": "Roadster", "years": { "from": 2001, "to": 2005 },
  "source": { "name": "Acme Roadster owner's manual", "url": "https://…", "retrieved": "YYYY-MM-DD", "note": "…", "basis": "…" },
  "engine": { "label": "2.0L 4-cylinder", "displacementL": 2.0 },
  "takes": {
    "oil": { "grade": "5W-30", "approval": "API SP", "capacity": 5.5, "capacityUnit": "qt", "filter": "ACME 1234" },
    "tires": { "sizes": ["215/55R17 94V", "225/45R18 91W"], "pressure": { "front": 33, "rear": 31, "spare": 60, "unit": "psi" } },
    "wipers": { "driver": 26, "passenger": 17, "rear": 12 },
    "transmission": "Acme ATF 9",
    "differential": "75W-90 GL-5",
    "transferCase": "75W-90 GL-5",
    "coolant": "Acme Long Life Coolant",
    "brakeFluid": "DOT 4",
    "airFilters": { "engine": "ACME 11", "cabin": "ACME 12" },
    "sparkPlugs": { "part": "NGK ILZKR7B11", "gap": "1.1 mm" },
    "battery": { "group": "35", "cca": 650 },
    "bulbs": { "low": "H11", "high": "9005", "fog": "H16" }
  },
  "variants": [
    { "drive": "AWD", "takes": { "transferCase": "75W-90 GL-5" } },
    { "gearbox": "manual", "takes": { "transmission": "Acme MTF 3" } },
    { "gearbox": "automatic", "takes": { "transmission": "Acme ATF 9" } }
  ]
}
```

The values above are made up. Every field of `takes` is optional; give only what the documents
print.

| Field | Rules |
|---|---|
| `id`, `title`, `make`, `model`, `years`, `source` | As for a torque or schedule list. `source.name` is short: it is named on the screen with the list's title |
| `engine` | Optional. For a model whose engines take different figures; one list per engine |
| `takes` | What the whole model takes |
| `tires.sizes` | One size fills the row. Several are offered to pick from |
| `tires.pressure` | Only where the document gives one pressure for every size it lists |
| `variants[].drive` | `2WD`, `4WD` or `AWD` (`Drivetrains.kind`). 4WD and AWD are kept apart |
| `variants[].gearbox` | `manual`, or `automatic` (anything known to shift itself: automatic, CVT, dual-clutch) |
| `variants[].takes` | May not give a row that `takes` gives. Two variants that one vehicle could both fit may not give the same row |

`KnownTakesTest` reads every file in the folder and fails if one doesn't parse or two claim the same
vehicle. It also holds each shipped list to the figures in the tables below.

## Finding a maker's documents (US market)

These were checked on 2026-10-18. A URL marked *fetched* was downloaded and found to be a PDF.

| Maker | Owner's manual | Maintenance schedule | Specs |
|---|---|---|---|
| **Toyota** | *Fetched*: `https://assets.sia.toyota.com/publications/en/om-s/<code>/pdf/<code>.pdf` (code such as `OM35B54U`). Free, no login | Warranty & Maintenance Guide, *fetched*: `…/omms-s/T-MMS-<yy><Model>/pdf/T-MMS-<yy><Model>.pdf` (`T-MMS-204Runner`) | Owner's manual chapter 8-1 "Specifications" |
| **Subaru** | subaru.com → Owners → Vehicle Resources. The page uses a public JSON API, *fetched*: `/services/vehicles/models/<year>`, then `/services/vehicles/trims/<year>/<MODEL>`, then `/services/documentation/trim/<TRIM>`. That last call lists `ownerManual` and `warrantyBooklet` PDFs, back to 2008. Free, no login, no VIN | Warranty and Maintenance Booklet, one per model year for all models: a 6,000 mi / 6 mo grid | Owner's manual chapter 12, "Specifications" |
| **Honda** | techinfo.honda.com, free, no login. 2017–2022 pattern, *fetched*: `/rjanisis/pubs/OM/AH/A<plat>A<yy><yy>OM/enu/A<plat>A<yy><yy>OM.PDF`, where `<plat>` is Honda's platform code (`TLA` CR-V 2017–22, `TBA` 10th-gen Civic). Older years use other patterns (`/pubs/om/wa1111/wa1111om.pdf`), and the 2023-on pattern isn't known | No fixed schedule for cars with the Maintenance Minder. The manual gives only its codes and a few fixed figures (brake fluid every 3 years, the severe-use footnotes) | Owner's manual "Specifications" section |
| **Volkswagen** | VIN-only (`ownersmanualvw.com`). For the 2010–2014 GTI no manual was reachable without one | "Maintenance Schedule For USA" cards, filed with NHTSA as manufacturer communications ([known-schedules.md](known-schedules.md)). The owners-literature API also serves them: `https://ownersliterature.vw.com/owners-literature-service/v1/documents?model-year=<y>&model-code=<code>` | Fluid capacity charts and Quick Reference Specification Books, filed with NHTSA: `https://static.nhtsa.gov/odi/tsbs/<year>/MC-<id>-<suffix>.pdf` |
| **Mazda** | *Fetched*: `https://www.mazdausa.com/siteassets/pdf/owners-optimized/<year>/<model>/<year>-<model>-owners-manual.pdf`. Free | Same folder, `<year>-<model>-scheduled-maintenance.pdf` | Owner's manual specifications chapter |
| Lexus | Same Toyota `om-s/` path; codes not yet mapped to models | `L-MMS-…` guesses returned 403 | — |
| Ford, GM, Acura | Not found: Ford's content server refuses scripts (403), and guesses for GM and Acura failed | — | — |

### Telling which document is which year

Manuals rarely print their model year.

- **Toyota:** take the document code from a catalogue that lists one per year, then check it against
  the PDF's creation date ([known-specs.md](known-specs.md)).
- **Subaru:** the file name has the year: `MSA5M` + `<yy>` + document number + edition, e.g.
  `MSA5M1903B`. The API's title says it too ("2019 Outback Owner's Manual"). Subaru's PDF links are
  signed and expire after a day, so cite the file name and "subaru.com Vehicle Resources", not the
  URL. Some manuals cover two models (Legacy/Outback) in per-model columns.
- **Honda:** the cover gives the year, and each page header carries the year, model and publication
  number (`19 CR-V … 31TLA620`). techinfo.honda.com sends an incomplete certificate chain. To fetch
  with curl, give it DigiCert's EV RSA CA G2 intermediate (`--cacert`); never turn verification
  off.
- **NHTSA filings:** each document's own heading ("GTI - 2014 FLUID CAPACITY CHART").

### Reading one

1. Download the PDF into a new, empty folder and run `pdftotext -layout`.
2. Find the specifications chapter, and note its pages for each year.
3. Diff that chapter between years. Only where it differs does a list have to split.
4. For a table with letter keys (Toyota's bulb types), read the key with plain `pdftotext` as well.
   The layout mode can drop it.
5. Write each figure in its stored form, and record each one, with its document and page, in a
   table here.

## The lists that ship

### Toyota 4Runner, 2017 to 2024

Source: Toyota's owner's manuals, chapter 8-1 "Specifications", read 2026-10-18. Every year has the
one engine (1GR-FE, 4.0 L V6). Years 2017–2019 have identical chapters. From 2020 only the oil
approval and the bulbs change.

| Document | Year | Pages (engine · oil · coolant & plugs · transfer · differentials · ATF & brake · tires · bulbs) |
|---|---|---|
| OM35B37U, OM35B46U, OM35B48U | 2017–2019 | 464 · 465 · 468 · 469 · 470 · 471 · 472 · 473 |
| OM35B54U | 2020 | 524 · 525 · 526 · 527 · 528 · 529 · 530 · 531 |
| OM35B41U | 2021 | 522 · 523 · 524 · 525 · 526 · 527 · 528 · 529 |
| OM35B65U, OM35B72U | 2022–2023 | 534 · 535 · 536 · 537 · 538 · 539 · 540 · 541 |
| OM35B73U, OM35C03U | 2023–2024 | 526 · 527 · 528 · 529 · 530 · 531 · 532 · 533 |

| Row | Value | As printed |
|---|---|---|
| Oil | 0W-20, 6.6 qt with filter | "Recommended viscosity: SAE 0W-20"; "With filter 6.6 qt. (6.2 L, 5.5 Imp. qt.)" |
| Oil approval | ILSAC GF-5 (2017–2021), ILSAC GF-6A (2022–2024) | "Oil grade: ILSAC GF-5 multigrade engine oil" / "… GF-6A …" |
| Transmission | Toyota Genuine ATF WS | "Fluid type Toyota Genuine ATF WS" |
| Differential | 75W-85 GL-5 | "Toyota Genuine Differential gear oil LT 75W-85 GL-5 or equivalent", front (4WD) and rear alike |
| Transfer case (4WD only) | Toyota Transfer gear oil LF 75W | "Toyota Genuine Transfer gear oil LF or equivalent"; "SAE 75W" |
| Coolant | Toyota Super Long Life Coolant | "Toyota Super Long Life Coolant" |
| Brake fluid | DOT 3 | "SAE J1703 or FMVSS No.116 DOT 3" |
| Spark plugs | DENSO SK16HR11, 0.043 in | "Make DENSO SK16HR11"; "Gap 0.043 in. (1.1 mm)" |
| Tires | Offered: P265/70R17 113S, P245/60R20 107H. Pressure 32 psi front, rear and spare | "Tire size P265/70R17 113S, P245/60R20 107H"; "32 psi (220 kPa …)" for all three |
| Bulbs (2017–2020) | Low H11, high 9005, fog H16 | Bulb table, with types "A: H11 halogen", "B: HB3 halogen" (9005), "C: H16 halogen" |

Left out:
- **Bulbs from 2021.** The low beams and fogs are LED, and the halogen high beam is "(if equipped)",
  with no trim named.
- **Battery, wiper blades and oil filter.** Not in the manual.
- **Capacities other than oil's.** The rows have no field for them yet.

### Volkswagen GTI, 2010 to 2014 (Mk6)

No owner's manual for these years could be reached from Volkswagen without a VIN. What Volkswagen filed
with NHTSA was read instead, on 2026-10-18. Every year has the 2.0T (CCTA/CBFA).

| Year | Documents | What they give |
|---|---|---|
| 2010–2012 | Job Aid "VW Engine Oil Capacities" (`2016/MC-10121998-9999`): p3 (2010), p2 (2011, 2012) | Oil capacity only: "2.0T (CCTA/CBFA) … 4.9qt" in the Golf/GTI column. No fluid chart was filed for these years |
| 2013 | GTI fluid capacity chart (`2012/MC-10156237-9999`, 1 p.); Quick Reference Specification Book (`2012/MC-10156123-9999`, PDF p74) | Oil VW 502 00, 4.9 qt; coolant G 12 plus-plus (purple); brake fluid DOT 4; manual gearbox oil G 052 171 A2; DSG fluid G 052 182 A2; spark plug 101 905 631 H |
| 2014 | GTI fluid capacity chart (`2013/MC-10155821-9999`, 1 p.); Quick Reference Specification Book (`2014/MC-10122268-9999`, PDF p104) | As 2013, except coolant G 13 (pink) |

Left out:
- **The oil grade.** The chart allows "SAE 5W-40, SAE 5W-30".
- **The plug gap.** Given as "1.0 to 1.1 mm".
- **Tire sizes and pressures, bulbs, battery and wipers.** Not in these documents. The 2014
  tire-chain sheet lists Golf and GTI sizes together.
- **The gearbox fluids for a car with nothing on record.** They depend on the gearbox: manual 02Q,
  DSG 02E.

The fluid part numbers are Volkswagen's "for reference only"; the list's note says so.

## Not yet done

- The 2011 and 2013 all-model Volkswagen maintenance cards exist after all, through the
  owners-literature API (document ids `ce283b19-b48b-41b2-baab-a1f04c81af4f` and
  `8db4252c-5991-4f79-a654-5c725d09bdf8`). [known-schedules.md](known-schedules.md) still has those
  two years as inferred; they should be read against the GTI list.
- Capacities for the transmission, differentials, transfer case and coolant. The rows have no field
  for them yet.
