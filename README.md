# GTL GPS Track Logger

On-device GPS track logger. Route points stay in SQLite on the phone. Share a KMZ (KML + icons) to Google Earth or another map app. Nothing is uploaded to our servers.

Kotlin + Jetpack Compose rewrite of the 2014 Eclipse app (`gtl-e`). Application id `com.lkovari.mobile.apps.gtl`.

**Version:** 2.0.0 (versionCode 18)  
**SDK:** minSdk 24 · targetSdk 36 · compileSdk 36  
**UI:** English and Hungarian, Material 3, portrait

Privacy policy: https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html

---

## Features

### Logging

- **Start / Stop** records a session as a visible foreground service with a notification.
- Fixes are stored only after they pass accuracy, satellite-count, time, and **speed-adaptive distance** filters (same speed bands as the 2014 logger; curves at >15° heading change use half spacing).
- Event kinds: `START`, `MOVE`, `PAUSE` (below usage pause speed), `STOP`.
- Usage modes: aircraft, watercraft, car, motorbike (default), runner. Runner uses a looser accuracy filter and a lower pause threshold.
- Optional ambient temperature (`TYPE_AMBIENT_TEMPERATURE`) and accelerometer samples on each stored point.

### GPS tab

- Live satellite counts: GPS L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC.
- SNR quality (excellent / good / fair / poor / none).
- Latitude, longitude, accuracy, provider, altitude, ambient temperature, logging status.

### Route tab

Session totals after Start: elapsed time, odometer, time moving, time waiting, speed, average speed, altitude, bearing, temperature range when a sensor exists.

### Map tab

- Centers on current location; follows while logging.
- Red polyline from Room (live session, last saved track, or a track chosen in Saved tracks).
- **Google Maps** when `MAPS_API_KEY` is set; otherwise an on-device message.
- **OSM Mapsforge** after you download a region and enable **Use downloaded OSM map**. The same polyline and accuracy ring draw on OSM.
- Light purple accuracy circle (radius = GPS accuracy in metres). Toggle in Settings.
- Douglas–Peucker simplification on the drawn line when **Simplify track on map** is on.

### Compass tab

Magnetic heading and a live dial from the rotation sensor. Works without Start.

### Saved tracks

- List of sessions with date, usage, units.
- **Show on map** opens the Map tab on that session (Google Maps or OSM).
- Delete.
- Checkboxes, **Select all**, **Share selected**:
  - one session → one KMZ named `GTL_yyyyMMdd_HHmmss.kmz`
  - several sessions → one KMZ with a folder per track

### KMZ export

- Bundled play (start), pause, and stop icons; map labels hidden (`LabelStyle` scale 0).
- Pause and stop balloons report `speed=0` (GPS jitter is not treated as motion).
- MIME `application/vnd.google-earth.kmz`. Open with Google Earth (install from Play if needed).
- Help screen documents this flow (EN/HU).

### Settings

- Usage and metric/imperial units.
- Use downloaded OSM map, simplify track, show last logged route, show accuracy marker.
- Fix filters (defaults from the 2017 logger): minimum distance, time, accuracy, satellites in fix.

### Other screens

- First-run safe-driving disclaimer.
- Download OSM map (Mapsforge v5 regions: Europe, selected Asia / Americas / Australia).
- Location settings (opens the system GPS panel).
- Help (tabs + KMZ in Google Earth) and About.
- Privacy-policy link.

---

## Architecture

Two Gradle modules:

| Module | Role |
|---|---|
| `:engine` | Pure JVM: GNSS classification, fix acceptance, speed-adaptive spacing, Douglas–Peucker, track stats, KML/KMZ, map-visibility rules. JUnit tests live here. |
| `:app` | Android: Compose UI, Room, DataStore, location/GNSS/sensors, foreground service, Google Maps, Mapsforge, WorkManager OSM download, FileProvider share. |

```
app/     Compose, Room, services, maps
engine/  Domain algorithms (no Android SDK)
docs/    Privacy policy, Play assets, renewal notes
```

### Data

- **Room:** `track_sessions` + `gps_events` (cascade delete). Polyline is always read from Room, not from an in-memory sketch.
- **DataStore:** disclaimer, usage, units, filters, OSM file path, map options.
- **Files:** OSM `.map` downloads; KMZ under `files/gtltracklogs/` (FileProvider).
- **RemoteTrackSync:** no-op stub for a later backend. No live location upload.

### Logging pipeline

1. `TrackingForegroundService` receives fused location.
2. `FixAcceptance` + `SpeedAdaptiveSpacing` decide whether to store the fix.
3. Kind is `START` / `PAUSE` / `MOVE`; Stop writes a `STOP` placemark.
4. `GtlViewModel` observes Room, computes `TrackStats`, and feeds `displayPoints` to Map (Google `Polyline` or Mapsforge overlay).

### Permissions

`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE` / `_LOCATION`, `POST_NOTIFICATIONS`, `INTERNET` / `ACCESS_NETWORK_STATE` (maps + OSM download). GPS hardware required; compass and ambient temperature optional. No `ACCESS_BACKGROUND_LOCATION`, no phone-state / IMEI.

---

## Setup

1. Open this folder in Android Studio (JDK 11 toolchain).
2. Copy gitignored `keystore.properties` (same EKL release keystore as sensors-s).
3. Add a Maps SDK key to `local.properties`:

```
sdk.dir=/path/to/Android/sdk
MAPS_API_KEY=your_key_here
```

Restrict the key to `com.lkovari.mobile.apps.gtl` and the EKL keystore SHA-1. Until the key is set, the Map tab still works with a downloaded OSM region.

### Build

```bash
./gradlew :engine:test
./gradlew assembleDebug
./gradlew assembleRelease    # needs keystore.properties
```

Release APK: `app/build/outputs/apk/release/app-release.apk`

### Stack

Kotlin 2.2 · AGP 9.2 · Compose BOM 2025.12 · Room 2.7 · DataStore · Navigation Compose · Play Services Location / Maps · Maps Compose · Mapsforge 0.25 · WorkManager · KSP

---

## Technical documents

| Document | What it is |
|---|---|
| [CHANGELOGS.md](CHANGELOGS.md) | Version history (2.0.0 rewrite and later fixes) |
| [docs/RENEWAL-REPORT.md](docs/RENEWAL-REPORT.md) | Rewrite report: what was rebuilt, what was dropped for Play policy, follow-ups |
| [docs/play-console/privacy-policy.html](docs/play-console/privacy-policy.html) | Privacy policy (local copy of the live KLHome page) |
| [docs/play-console/feature-graphic.png](docs/play-console/feature-graphic.png) | Play Store feature graphic |
| [docs/screenshots/](docs/screenshots/) | Play listing screenshots (GPS, route, map/tracking, compass, settings, help, about, Google Earth KMZ) |

Engine entry points worth reading:

- `engine/.../FixAcceptance.kt` — accuracy / sats / time / distance gate
- `engine/.../SpeedAdaptiveSpacing.kt` — metres between points by km/h and curves
- `engine/.../TrackStats.kt` — odometer, moving vs waiting
- `engine/.../KmlExporter.kt` + `KmzExporter.kt` — KMZ with local icons
- `engine/.../Gnss.kt` — constellation / L1 vs L5 / SNR
- `engine/.../MapTrackVisibility.kt` — when the map must draw a track

---

## Play listing screenshots

`docs/screenshots/`

- `gps-idle.png`, `gps-logging.png` — GPS tab
- `route.png` — Route totals
- `map.png`, `tracking.png` — Map while recording
- `googleearth.png` — shared KMZ in Google Earth
- `compass.png`, `settings.png`, `help.png`, `about.png`
- `app-icon.png`

---

## Not in this app

Intentionally not ported from 2014 (policy or dead APIs): IMEI / `READ_PHONE_STATE`, live lat/lng upload, follow-me web page, remote unlock, Google Directions, app-driven GPS/Wi-Fi toggles, boot auto-start. See [docs/RENEWAL-REPORT.md](docs/RENEWAL-REPORT.md).
