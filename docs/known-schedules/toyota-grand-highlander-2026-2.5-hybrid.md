# Toyota Grand Highlander Hybrid, 2026

How `app/src/main/assets/known-schedules/toyota-grand-highlander-2026-2.5-hybrid.json` was made. The rules it
follows are in [known-schedules.md](../known-schedules.md).

Source: Toyota's 2026 Grand Highlander Hybrid Warranty and Maintenance Guide,
`https://assets.sia.toyota.com/publications/en/omms-s/T-MMS-26GrandHighlanderHV/pdf/T-MMS-26GrandHighlanderHV.pdf`.
Toyota's owners site lists no warranty guide for the hybrid; the guide was found only at that address.
Read on 2026-10-26 (#199).

- The guide has a maintenance chart: jobs are rows, mileages are columns from 5,000 to 150,000 miles (PDF
  pages 40 to 45, printed 38 to 43: three recommended-service charts, each followed by a chart for special
  operating conditions). It was read in full, cell by cell, as page images, and the list written from it:
  30 milestones, 5,000 miles apart.
- Compared with 2025 ([2025](toyota-grand-highlander-2025-2.5-hybrid.md)), by a text diff of the chart pages
  with every difference looked at on the page:
  - The recommended-service charts have the same cells. Wording differs ("HV" became "HEV",
    "Radiator/Condenser/Intercooler"), the footnotes are renumbered, the EFI tank additive is "Hawaii only"
    (2025: "Island use only"), and **the drive belt and vacuum pump rows are marked "(if applicable)"**, where
    2025 marked them "T24A-FTS engine only" (below).
  - **The special-condition charts mark every column**, as the 2026 gas chart does: on dirt or dusty roads the
    ball joints, drive shaft boots and steering linkage are inspected and the oil changed at every 5,000 miles,
    and the engine air filter is inspected at every milestone and replaced at the 30,000s. When towing, the
    rear differential oil (if applicable) and the automatic transmission fluid are inspected at 30,000, 90,000
    and 150,000 and replaced at 60,000 and 120,000. 2025 listed only the work the recommended chart doesn't
    already do. So 2025 and 2026 are separate lists, each as its own chart draws it.
- **The chart prints no months on its columns**, so the milestones have none. A reminder keeps months only
  where the guide prints them for that job: the oil change (12 months, or 5,000 miles / 6 months, in its
  footnote) and the two coolants (60 months for every change after the first, in their footnotes; below). The
  other reminders are by distance only.
- **One chart, split by engine** (the owner's rulings of 2026-10-23 and 2026-10-25, #33 and #199). The guide
  prints one chart for both hybrids: the 2.5L hybrid and the Hybrid MAX (2.4L turbo, T24A-FTS). Left out of
  this list as the turbo's, each marked so on the page:
  - spark plugs at 40,000 and 80,000 ("(T24A-FTS)");
  - the automatic transmission fluid work with long idling or low-speed driving (its footnote is
    "T24A-FTS engine only"); the same row's towing footnote is for both, so the towing work is here.
  The spark plug change at 120,000 is unmarked, so it is here, which makes this engine's plugs every 120,000.
- **Drive belts and vacuum pump in both lists.** In 2026 these two rows say "(if applicable)" and name no
  engine. By the owner's ruling of 2026-10-25 (#199) they are in both hybrid lists, table only, with no
  `service` and no reminder, keeping "(if applicable)" in their names: "Inspect drive belts (if applicable)" at
  60,000 and every 15,000 after, and "Inspect vacuum pump (if applicable; new vane and caps)" at 120,000 (its
  footnote says to replace the vane and vane caps). Every other row's engine is printed or it is for both.
- **Engine and powertrain.** No other Grand Highlander has a 2.5L engine, so the size alone tells it apart;
  the list has `engine.powertrain` `hybrid` as well, to say so plainly. NHTSA's model list names it "Grand
  Highlander" only (checked for 2024, 2025 and 2026 on 2026-10-23).
- **"(if applicable)" rows** (the owner's ruling of 2026-10-23) are in the table with no `service`, so they
  never drive a reminder: the radiator, condenser and intercooler, the rear differential oil (inspected every
  30,000, and when towing inspected and replaced as above), the intercooler coolant, the drive belts and the
  vacuum pump. Nothing is inferred from the 2024 log's "(4WD)": these rows keep the chart's "(if applicable)"
  and are not in `allWheelDriveOnly`.
- **Coolant rows split** (the owner's ruling of 2026-10-23, #33). The chart's rows are "Engine
  Coolant/Intercooler Coolant (if applicable)" (replaced at 100,000 and 150,000; footnote: first at 100,000
  miles / 120 months, then every 50,000 / 60) and "Inverter Coolant/Intercooler Coolant (if applicable)"
  (replaced at 150,000; footnote: first at 150,000 miles / 180 months, then every 50,000 / 60). The "(if
  applicable)" mark belongs to the intercooler: every one of these hybrids has engine coolant and an inverter
  loop. So each row is split into "Replace engine coolant" (`coolant`) or "Replace inverter coolant"
  (`inverter-coolant`) and "Replace intercooler coolant (if applicable)" (table only, once per milestone). The
  engine coolant is a reminder first at 100,000 miles and the inverter coolant first at 150,000, each then
  every 50,000 miles / 60 months, as the footnotes give them. The inverter coolant's change is in the chart
  (at 150,000), so it is an interval, not `additional`. The footnotes' "120 months" and "180 months" for the
  first changes count from the day the vehicle went into service, which Glovebox doesn't know, so the first
  changes are by mileage only.
- Hybrid jobs: the hybrid battery's cooling intake filter is inspected at every milestone and cleaned every
  20,000 miles; the cleaning is a reminder (`hybrid-battery-filter`), the inspection is in the table only.
- The special oil change's footnote names dirt or dusty roads and repeated trips under five miles below
  freezing; the job is named "· dusty roads", as on the 2026 gas list.
- Left out: "Add EFI Tank Additive" at every milestone (Hawaii only: a job for one region, which the format
  can't name), and the explanations of each item.
- Reminders: engine oil 10,000 mi / 12 mo (5,000 / 6 for severe use, the chart's dusty-roads oil change),
  tire rotation 5,000, hybrid battery cooling filter cleaning 20,000, cabin and engine air filters 30,000,
  spark plugs 120,000, engine coolant first at 100,000 miles and inverter coolant first at 150,000, each then
  every 50,000 / 60.
- Only for severe use (#136): automatic transmission fluid every 60,000 when towing or carrying heavy loads.
  The rear differential oil beside it is "(if applicable)" and has no reminder. The dusty-roads engine air
  filter replacement at the 30,000s falls with the ordinary one and has no reminder of its own.
- Inspections are in the table only, with a check line where the catalog has one (fluid levels, wiper
  blades, brakes). The brake inspection is visual at every 5,000 miles, with thickness and runout at the
  30,000s (footnote). Brake fluid has no replacement in the chart, so it has no reminder from this list.
- Busiest milestone: 32 jobs (the cap is 40).
- Not covered here: 2024 and 2025 (lists of their own); 2027 on (Toyota's owners site lists only a warranty
  guide with no schedule for 2027, and the maintenance guide's address answered 403 on 2026-10-22). The
  Hybrid MAX has a list of its own
  ([toyota-grand-highlander-2026-2.4-hybrid](toyota-grand-highlander-2026-2.4-hybrid.md)).
