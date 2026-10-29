# A shop's invoice may fill Log service, and only fill it

Until now, "Import from a dealer record" was left out on purpose. An invoice read wrong would save figures the owner never checked, and a line read as work that wasn't done would reset its reminder. Two changes make it safe enough to build (#484):

- **The sums vouch for the figures.** An invoice checks itself: parts rows against the parts figure, a service's labor and parts against its subtotal, every service against the totals block, and the totals block against the grand total. `InvoiceReader` (#483) takes a figure only when one of those sums agrees, so a misread digit is left blank instead of being offered.
- **It fills the form and saves nothing.** The owner reads the invoice into Log service, checks it there and saves it themselves. Every guard the form already has still holds: no future dates, writes through `RecordActions`, Undo.

The first cut reads a shop's **emailed PDF**. Its text layer is exact, so there's no OCR. Photos of a paper invoice come next, through the same reader with Tesseract's words (#513).

## Guardrails

- Wording says "check it against the invoice", never "detected". A figure that was printed but no sum vouched for is left blank and outlined "Couldn't read: check the invoice".
- One invoice is one record. Each service is a line of the shop's wording with its own job and its vouched labor and parts. It goes under the line the owner's rule names, and is a custom line otherwise. A guess is only offered, through "File under…".
- Recommended, declined or deferred work goes into the notes, never in as a line.
- A VIN that isn't the vehicle's is asked about by its last four characters. It is never a block, and there's no offer to switch vehicles.
- "What was read" shows each line and what it became. It is for viewing only; nothing in it is exported.
- The PDF is read in memory, under the time limit `PdfWords` already applies to the Carfax report. It is kept, encrypted, in the vehicle's documents only if the owner leaves "Keep the invoice" on.
- Debug builds write the words and the trace to `files/invoice-traces/`, to feed the private fixtures. Release builds never write them, and Delete all data removes them.
- Everything happens on the phone. Nothing is sent anywhere ([ADR-0007](0007-network-nhtsa-only.md)).

## Considered Options

- **ML Kit's text recognition.** Turned down for the network rule ([ADR-0008](0008-no-ml-kit.md)). A PDF needs no OCR at all, and photos will use Tesseract.
- **Per-shop templates.** They would only read the shops someone has written a template for, and would break when a shop changes its layout. The reader works from headings, labels, columns and sums instead.
- **Saving without review.** Turned down: a misread line would reset a reminder before anyone saw it.
- **Keeping the PDF on the record itself.** Record photos are JPEG only, and documents belong to a vehicle. Keeping it as one of the vehicle's documents needed no schema change, and backup, restore, the sweep and crypto-erase already cover it. The cost is that an Undo of the record leaves the document where it is.

## Consequences

- A scanned PDF has no text layer. The form says so and is filled in by hand until #513.
- Totals-block charges (shop supplies, tax, fees) are used to check the figures but aren't filled in. The gap shows them as "Tax and fees".
- A second service worded the same as an earlier one can't be a second line of the same name. It is left out, and the form says so.
