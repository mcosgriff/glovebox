# Bundled torque lists

Glovebox can ship a list of torque values for a model, offered on that vehicle's Torque specs screen
as "Add known specs for …". This file is how one gets added, what has to be true of it first, and
where the lists that ship came from.

## What has to be true before a list goes in

Glovebox has no factory torque data and invents none. A list is somebody's values, named.

1. **You can say why you may ship it.** One of:
   - written permission from whoever compiled it (an email reply kept on file is enough);
   - values you took yourself from a manual you have access to;
   - the manufacturer's own owner's manual, for the few rows it covers.

   "It was on a website" is not a basis. A site's table is its compilation even where each number is a
   fact, and the app may one day be sold. The answer goes in the file, in `source.basis`.
2. **Every row states its unit in the source.** A row whose unit has to be assumed is left out.
3. **Nothing is corrected from memory.** If a figure looks wrong or sources disagree, leave the row out
   or say so in its `note`. Don't put in the number you believe is right.
4. **The source is described honestly** in `source.note`, which the app shows: who compiled it and what
   it isn't (a manufacturer's data, checked by Glovebox).

## A repeatable way to build one

1. **Owner's manual first.** Free from the manufacturer, citable by page. It usually gives wheel nuts
   and little else, but those matter most.
2. **The rest from a real manual.** A short subscription to the manufacturer's service site, a printed
   repair manual, or a library's access to a repair database. Type the values into a `torque-specs`
   import file ([format](import/README.md#torque-specs)); you can import that into your own vehicle
   straight away.
3. **Turn the import file into a list** by wrapping its `items` as below.
4. Web searches are for finding where a figure is documented, not for the figure itself: pages copy
   each other, and two that agree are often one source.

## The file

`app/src/main/assets/known-specs/<id>.json`:

```json
{
  "id": "acme-roadster-mk2",
  "title": "Mk2 Roadster",
  "make": "Acme",
  "model": "Roadster",
  "years": { "from": 2001, "to": 2005 },
  "source": {
    "name": "Acme Roadster repair manual",
    "retrieved": "2026-01-02",
    "note": "From the manufacturer's repair manual, typed in by the app's maintainer. Not checked by Glovebox.",
    "basis": "Values taken by the maintainer from their own copy of the manual."
  },
  "items": [
    { "name": "Lug nuts", "category": "wheels", "socket": "19 mm", "torque": { "value": 90, "unit": "ft-lb" } }
  ]
}
```

| Field | Rules |
|---|---|
| `id` | Unique. Kept on every row added from the list, for good, so never change it |
| `title` | What the offer calls the vehicle: "Add known specs for the Mk2 Roadster" |
| `make`, `model`, `years` | What the list is offered for. Matched ignoring capitals and spaces; no near misses |
| `source.name` | Short: shown on each row as "Started from the … list" |
| `source.url` | Optional; `https` only |
| `source.retrieved` | The day the values were copied, `YYYY-MM-DD` |
| `source.note` | Shown wherever the list is offered and on the About screen |
| `source.basis` | Required. Why it may be shipped. Not shown in the app |
| `items` | Exactly as in a `torque-specs` import file, except that every item needs a `torque` and none may have a `source` |

The values above are made up. `KnownSpecsTest` in `:domain` reads every file in that folder and fails
if one doesn't parse, has a row without a value, or has no basis.

## What the app does with one

Every value arrives tagged DOUBLE-CHECK. The tag clears when the user says they have checked it, says
it matches the factory service manual, or changes it. The row keeps "Started from the … list" for
good. A row the user had already filled in is never overwritten, and an unconfirmed value is left out
of a file the user saves.

## The lists that ship

### Toyota 4Runner, 2010 to 2024

Only what Toyota's owner's manuals give: the wheel nut torque and, from 2017, the torque for raising
the spare tire into its carrier. Nothing else in those manuals is a torque figure. The wheel nut
figure changed twice, so there are four lists.

| Model years | Wheel nuts | Spare tire carrier | Toyota documents read |
|---|---|---|---|
| 2010–2011 | 83 ft·lbf (110 N·m) | not given | OM35A36U, OM35A52U |
| 2012–2014 | 81 ft·lbf (110 N·m) | not given | OM35819U, OM35A22U, OM35A83U |
| 2015–2016 | steel 83 ft·lbf (112 N·m), aluminum 76 ft·lbf (103 N·m) | not given | OM35B18U, OM35B23U |
| 2017–2024 | steel 83 ft·lbf, aluminum 76 ft·lbf | 34.4 ft·lbf (46.6 N·m) | OM35B37U, OM35B46U, OM35B48U, OM35B54U, OM35B41U, OM35B65U, OM35B72U, OM35B73U, OM35C03U |

How they were read, so it can be repeated for another model:

- Each document is Toyota's own PDF at
  `https://assets.sia.toyota.com/publications/en/om-s/<code>/pdf/<code>.pdf`. Its text was extracted
  with `pdftotext -layout` and every line containing `ft·lbf` was listed with its page. The figures
  appear in the tire-changing steps and again in the specifications table.
- The manuals don't print their model year. Which document belongs to which year comes from manual
  catalogues that list Toyota's document code per year (ownersman.com, manualsnet.com,
  manual-directory.com), checked against each PDF's creation date. 2019 has no catalogue entry and is
  taken to be OM35B48U from its date (July 2018) and its place in the sequence. The figures are the
  same on both sides of every year whose document is less certain, so a wrong pairing inside a run
  of years would not change a list; the two changes fall between documents the catalogues agree on.
- Not in these lists, because the owner's manual doesn't give them: every other fastener. Those need
  a repair manual.
