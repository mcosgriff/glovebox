# Bundled torque lists

Glovebox can ship a list of torque values for a model, offered on that vehicle's Torque specs screen
as "Add known specs for …". **No list ships today.** This file is how one gets added, and what has
to be true of it first.

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
