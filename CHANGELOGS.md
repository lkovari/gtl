# Changelog

All notable changes to **GTL GPS Track Logger** (`com.lkovari.mobile.apps.gtl`).

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning matches `versionName` **2.0.3** / `versionCode` **21** (minSdk 24, targetSdk 36).

Bilingual release note for this version: [docs/CHANGELOG-2026-09-08.md](docs/CHANGELOG-2026-09-08.md). Play Console what’s-new: [docs/play-console/whatsnew.txt](docs/play-console/whatsnew.txt).

## [Unreleased]

## [2.0.3] — 2026-09-08

Play production track **21 (2.0.3)** (signed AAB). Kalman smoothing on stored points, Settings sliders, aircraft ICAO units.

### Added

- Constant-velocity **Kalman** smoother in `:engine` (`KalmanTrackFilter`), applied before SQLite so Route, Map, and KMZ share the same path. HUD accuracy circle stays on the raw fused fix.
- Settings: **Smooth recorded track**, **Smoothing strength** (Low–High slider), **Hold still when stopped**, **Recording density** (Smart–Every good slider). English and Hungarian.
- Usage presets write smoothing, density, map-simplify, and units in one DataStore edit (runner: Low + every good + DP off; motorbike: Medium + Smart + 6 m DP; aircraft: High + 15 m DP + ICAO).
- Help **Settings** section: presets, sliders, and ICAO for aircraft.
- Engine tests for highway RMSE, roundabout, figure-8, zigzag, stationary lock, jump re-init, usage defaults, and slider interpolation.

### Changed

- **Simplify track on map** tolerance is a 1–20 m slider (1 m steps), not chips. Defaults follow usage (not a global 19.5 m). Old 19.5 m sentinel migrates once when Kalman keys are first written. Display-only: KMZ and odometer keep every stored point.
- Smooth recorded track and recording density use continuous sliders (Low–High and Smart–Every good). Usage presets set the slider positions.
- Aircraft usage defaults to ICAO units (knots, NM, feet); other usages default to metric.
- Smart density: runner uses half of the 2014 speed bands (still half again in a curve, min 1 m). Vehicles keep the existing bands. Intermediate density mixes Smart spacing with Every good.
- Standing min-distance preset is 2 m (same as the Smart standing band). 1 m stays the Every-good duplicate drop.
- Settings layout fits the safe drawing area without a vertical scrollbar. The read-only Fix filters row is removed; accuracy and satellite gates still run.
- README and GPS data-flow docs: Kalman box before `FixAcceptance`. README documents Kalman behaviour and how each setting affects the stored tracklog.

### Fixed

- Standing GPS wander is pinned when stationary lock is on (no 10 m scribble).
- Poor-accuracy / low-satellite fixes never enter the Kalman filter.

## [2.0.2] — 2026-09-05

Play production track **20 (2.0.2)** (signed AAB). `versionCode` 19 was already used on Play, so this release is 20. Play Console what’s-new: `docs/play-console/whatsnew.txt`.

### Added

- Checkboxes on Saved tracks, **Select all**, and **Share selected**.
- One selected session exports one KMZ; several selected sessions export one KMZ with a folder per track.
- KMZ `gx:Track` for every stored point (`when`, lon/lat/alt, speed).
- START / PAUSE / STOP balloons: UTC time, lat, lon, speed, temperature; STOP also max and average speed.
- Help: stored-trackpoint table of `gps_events` fields (EN/HU).

### Fixed

- Live track no longer missing on a downloaded OSM map: Mapsforge draws the red polyline (and start/end dots) the same way Google Maps already did.
- **Show on map** from Saved tracks now displays that session on OSM as well as Google Maps.
- Explicitly chosen sessions still appear when **Show last logged route on map** is off.

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
