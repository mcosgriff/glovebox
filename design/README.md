# Glovebox design

**These are the design from before the app was built, not screenshots of it.** The app has moved on
since (screens added, wording and layout changed); for what it looks like now, see the screenshots in
the main [README](../README.md#screenshots). They are still the reference for the colours, spacing and look of new screens.

Exported from the Claude design canvas: https://claude.ai/code/artifact/7f521081-34e6-46e9-99aa-641765621ad8

- `screens/*.png` — renders at 390 dp wide, 2× scale (phone screens; tall ones show the full scroll).
  Fonts fell back to system fonts in the export; the design uses IBM Plex Sans / Mono.
- `source/*.dc.html` — the exact source of each screen. Inline `style` attributes hold the real colors,
  sizes and spacing; copy text is literal. `{{name}}` holes and the `renderVals()` script at the bottom
  show the interactive state and sample data (sample data is not real).
- `source/canvas.json` — board layout and titles.
- `screens/NavMap.png` — navigation map: four bottom tabs and what hangs off each.

When implementing a screen in Compose, read both the PNG (visual) and the `.dc.html` (exact values).
Map raw hex colors to the theme tokens in `app/.../ui/theme/Theme.kt` rather than hard-coding them.

| Screen | Render | Source |
|---|---|---|
| 1 · Add vehicle (VIN) | `screens/Main.png` | `source/Main.dc.html` |
| 2 · Scan VIN | `screens/Scan.png` | `source/Scan.dc.html` |
| 3 · NHTSA decode result | `screens/Decoded.png` | `source/Decoded.dc.html` |
| 4 · Garage / vehicle home | `screens/Garage.png` | `source/Garage.dc.html` |
| 5 · Log service (scrolls) | `screens/LogService.png` | `source/LogService.dc.html` |
| 6 · Service history | `screens/History.png` | `source/History.dc.html` |
| 7 · Reminder rules (scrolls) | `screens/Reminders.png` | `source/Reminders.dc.html` |
| 8 · Recalls (scrolls) | `screens/Recalls.png` | `source/Recalls.dc.html` |
| 3b · Recommended schedule (after decode) | `screens/Schedule.png` | `source/Schedule.dc.html` |
| 9 · Settings — theme toggle is live | `screens/Settings.png` | `source/Settings.dc.html` |
| 0 · Biometric unlock | `screens/Lock.png` | `source/Lock.dc.html` |
| 4a · Garage — two vehicles | `screens/GarageMulti.png` | `source/GarageMulti.dc.html` |
| 10 · Reminders tab (all vehicles) | `screens/Upcoming.png` | `source/Upcoming.dc.html` |
| 4b · Update mileage (sheet) | `screens/UpdateMileage.png` | `source/UpdateMileage.dc.html` |
| 6a · Service record + receipt viewer | `screens/RecordDetail.png` | `source/RecordDetail.dc.html` |
| 0a · First-run welcome / restore | `screens/Welcome.png` | `source/Welcome.dc.html` |
| 4c · Garage — empty | `screens/GarageEmpty.png` | `source/GarageEmpty.dc.html` |
| 4d · Vehicle details + mark as sold | `screens/VehicleDetails.png` | `source/VehicleDetails.dc.html` |
| 3c · Edit schedule item | `screens/EditInterval.png` | `source/EditInterval.dc.html` |
| 4e · Torque & fastener specs | `screens/Specs.png` | `source/Specs.dc.html` |
| 4 · Garage — dark | `screens/GarageDark.png` | `source/GarageDark.dc.html` |
| 5 · Log service — dark | `screens/LogServiceDark.png` | `source/LogServiceDark.dc.html` |
| 6 · History — dark | `screens/HistoryDark.png` | `source/HistoryDark.dc.html` |
| 9 · Settings — dark | `screens/SettingsDark.png` | `source/SettingsDark.dc.html` |
| 0 · Unlock — dark | `screens/LockDark.png` | `source/LockDark.dc.html` |
| Navigation map | `screens/NavMap.png` | `source/NavMap.dc.html` |
| 1a · Add without a VIN | `screens/ManualAdd.png` | `source/ManualAdd.dc.html` |
| 4f · Archived vehicles | `screens/Archived.png` | `source/Archived.dc.html` |
| 6b · Export sheet (with PDF / Markdown preview) | `screens/ExportSheet.png` | `source/ExportSheet.dc.html` |
| 10a · Snooze sheet | `screens/Snooze.png` | `source/Snooze.dc.html` |
| 9a · Notifications | `screens/Notifications.png` | `source/Notifications.dc.html` |
| 9b · Create encrypted backup | `screens/BackupCreate.png` | `source/BackupCreate.dc.html` |
| 9c · About | `screens/About.png` | `source/About.dc.html` |
| 4g · Delete vehicle + its data | `screens/DeleteVehicle.png` | `source/DeleteVehicle.dc.html` |
| 9d · Delete all data | `screens/DeleteAll.png` | `source/DeleteAll.dc.html` |
| 6c · Edit service + undo | `screens/EditRecord.png` | `source/EditRecord.dc.html` |
| 6d · Delete record + undo | `screens/DeleteRecord.png` | `source/DeleteRecord.dc.html` |
| 6e · History — timeline view | `screens/Timeline.png` | `source/Timeline.dc.html` |
| PDF report · page 1 (infographic) — 816×1056 Letter | `screens/ReportPage1.png` | `source/ReportPage1.dc.html` |
| PDF report · page 2 (service log) — 816×1056 Letter | `screens/ReportPage2.png` | `source/ReportPage2.dc.html` |

## PDF report

`ReportPage1` and `ReportPage2` are the exported PDF, one US Letter page each (816×1056 px at 96 dpi
= 8.5×11 in). In the app, render them with Android's `PdfDocument` on a 612×792 pt Letter page (design px × 0.75 = pt). Category colors
(oil `#eb6834`, tires `#2a78d6`, brakes `#eda100`, fluids & 4WD `#1baf7a`, other `#8A8E94`) were
checked for colorblind separation; every color also has a text label, so the report reads in grayscale.

