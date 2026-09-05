# Changelog

All notable changes to **GTL GPS Track Logger** (`com.lkovari.mobile.apps.gtl`).

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning matches `versionName` **2.0.0** / `versionCode` **18** (minSdk 24, targetSdk 36).

## [Unreleased] — 2026-09-05

Offline OSM map and saved-track sharing (working tree; not yet committed).

### Fixed

- Live track no longer missing on a downloaded OSM map: Mapsforge now draws the red polyline (and start/end dots) the same way Google Maps already did.
- **Show on map** from Saved tracks now displays that session on OSM as well as Google Maps.
- Explicitly chosen sessions still appear when **Show last logged route on map** is off.

### Added

- Checkboxes on Saved tracks, **Select all**, and **Share selected**.
- One selected session exports one KMZ; several selected sessions export one KMZ with a folder per track.

## [2.0.0] — 2026-09-04

Kotlin + Jetpack Compose rewrite of the 2014 Eclipse logger. Location stays on the device. Privacy policy: https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html

### Added

- Room SQLite (`TrackSession` + `GpsEvent`); map polyline is drawn from Room.
- Foreground location logging (no `ACCESS_BACKGROUND_LOCATION`).
- GPS / Route / Map / Compass tabs with Material 3 UI (English and Hungarian).
- GNSS HUD: GPS L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC, SNR quality.
- Route totals: elapsed, odometer, time moving / waiting, speed, average speed, altitude, bearing, temperature range when the sensor exists.
- Google Maps (Maps SDK key in `local.properties`) and OSM Mapsforge region download with progress.
- Settings: usage (aircraft, watercraft, car, motorbike, runner), metric/imperial, fix filters, OSM offline map, track simplification, last track on map.
- Saved tracks: show on map, delete.
- Share track as KML/KMZ via the system share sheet.
- Help, About, disclaimer, location settings, privacy-policy link.
- Play listing assets under `docs/screenshots/` and `docs/play-console/`.

### Changed

- Default usage is motorbike; logging density follows speed bands (standing / walking / city / highway, tighter in curves).
- Accuracy circle on the map (radius = GPS accuracy in metres); can be turned off in Settings.

### Removed

Not ported from the 2014 app (Play policy / dead APIs):

- IMEI / `READ_PHONE_STATE` / device-id files
- Live lat/lng upload and follow-me web tracking
- Remote feature unlock / IMEI whitelist
- Google Directions POST
- App-driven GPS/Wi-Fi toggles and boot auto-start

### Fixed — 2026-09-05

- GPS accuracy ring on Google Maps and OSM.
- Point spacing by speed (`SpeedAdaptiveSpacing`): denser when slow or turning, sparser at high speed.
- Pause and stop placemarks in KMZ balloons now report `speed=0` (GPS jitter is no longer shown as motion).
- START / PAUSE / STOP map labels replaced with bundled play / pause / stop icons; export is KMZ (`doc.kml` + icons) so Google Earth works offline.
- Help: how to open the shared KMZ in Google Earth (Play Store if needed) and tap the icons for point details.
- Store screenshots: `docs/screenshots/tracking.png`, `route.png`, `googleearth.png`.
