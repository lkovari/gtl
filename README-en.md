# GPS Track Logger

[English](README-en.md) · [Magyar](README-hu.md)

On-device GPS track logger. Route points stay in SQLite on the phone. Share a KMZ (KML + icons) to Google Earth or a GPX 1.1 file to OsmAnd, Komoot, Garmin Connect, QGIS, and other apps. Nothing is uploaded to our servers.

Kotlin + Jetpack Compose rewrite of the 2014 Eclipse app (`gtl-e`). Application id `com.lkovari.mobile.apps.gtl`.

**Version:** 2.0.10 (versionCode 28)  
**SDK:** minSdk 24 · targetSdk 36 · compileSdk 36  
**UI:** English and Hungarian, Material 3, portrait

Privacy policy: [https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html](https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html)

---

## Table of contents

- [Features](#features)
  - [Logging](#logging)
  - [GPS tab](#gps-tab)
  - [Route tab](#route-tab)
  - [Map tab](#map-tab)
  - [Compass tab](#compass-tab)
  - [Saved tracks](#saved-tracks)
  - [KMZ export](#kmz-export)
  - [GPX export](#gpx-export)
  - [Settings](#settings)
  - [Other screens](#other-screens)
- [Architecture](#architecture)
  - [Data](#data)
  - [How logging works](#how-logging-works)
  - [GNSS Skyplot](#gnss-skyplot)
  - [Barometric altitude (Baro)](#barometric-altitude-baro)
  - [How Run/Hike logs like a sports watch](#how-runhike-logs-like-a-sports-watch)
  - [Kalman filter (how stored points are smoothed)](#kalman-filter-how-stored-points-are-smoothed)
  - [Effect of settings on the tracklog](#effect-of-settings-on-the-tracklog)
  - [Recording density](#recording-density)
  - [Douglas–Peucker (map simplify)](#douglaspeucker-map-simplify)
  - [Permissions](#permissions)
- [Setup](#setup)
  - [Build](#build)
  - [Stack](#stack)
- [Technical documents](#technical-documents)
- [Play listing screenshots](#play-listing-screenshots)
- [Next to do](#next-to-do)
- [Not in this app](#not-in-this-app)

---

## Features

### Logging

- **Start / Stop** records a session as a visible foreground service with a notification.
- Fixes are stored only after they pass accuracy and satellite-count gates. Optional **Kalman** smoothing then moves the point. **Smart** or **Every good fix** density decides whether to write it (see Settings). Run/Hike default is **Use GNSS only** (satellite chip, not fused location) with smoothing off so small on-road shapes stay in the tracklog. Full pipeline: [How logging works](#how-logging-works).
- Event kinds: `START`, `MOVE`, `PAUSE` (below usage pause speed), `STOP`.
- Usage modes: aircraft, watercraft, car, motorbike (default), bicycle, Run/Hike. Choosing a usage writes a full preset (filters, GNSS only, smoothing, density, map simplify). Run/Hike and bicycle use a looser accuracy filter and a lower pause threshold.
- Optional ambient temperature (`TYPE_AMBIENT_TEMPERATURE`), barometric altitude (`TYPE_PRESSURE`; see [Barometric altitude (Baro)](#barometric-altitude-baro)), accelerometer samples, and lean angle (gravity, tank-mount) on each stored point.



### GPS tab

- Live satellite counts: GPS L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC. Chip colours match the skyplot.
- Polar **skyplot** under SNR (north-up, used vs in view, L5 ring). See [GNSS Skyplot](#gnss-skyplot). Constellation and band primer: [docs/all-gps-systems-hu.md](docs/all-gps-systems-hu.md) (Hungarian).
- SNR quality (excellent / good / fair / poor / none).
- Latitude, longitude, accuracy, provider, altitude, logging status. **Baro** when a pressure sensor exists (Settings QNH; [how Baro is calculated](#barometric-altitude-baro)). Ambient temperature.
- **Altitude** prefers Mean Sea Level, then GNSS ellipsoid, then fused ellipsoid (`GpsAltitude.pick`). Values outside −430…9000 m (fused junk near −1800 m on some phones) are treated as missing.
- When **Show fix cloud** is on: n, RMS, CEP95, median reported accuracy, and a standing / moving / wait caption (same in-memory window as the map dots; CEP95 needs 8 samples).



### Route tab

Session totals after Start (and for a saved / last session on Map): elapsed time, odometer, time moving, time waiting, speed, average speed, altitude, bearing, lean angle (phone flat on a motorbike tank), temperature range when a sensor exists, and a GPS elevation profile (dashed barometric line when pressure samples exist). Axis min/max is GPS and baro together, at least 50 m. The legend shows the last GPS and baro values. Baro uses Settings QNH and the same rules as [Barometric altitude (Baro)](#barometric-altitude-baro).

### Map tab

- Centers on current location; follows while logging. **Keep whole track on the screen** fits the whole route after each GPS refresh (pan and zoom stay allowed until the next fix).
- Red polyline from Room (live session, last saved track, or a track chosen in Saved tracks). The Map line **is** the stored log; there is no separate sketch. See [How logging works](#how-logging-works).
- **Google Maps** when `MAPS_API_KEY` is set; otherwise an on-device message.
- **OSM Mapsforge** after you download a region and enable **Use downloaded OSM map**. The same polyline and accuracy ring draw on OSM. A missing or non-Mapsforge file shows an on-device message and turns that switch off so the next launch is not a crash loop. Download keeps only files with magic `mapsforge binary OSM` and a matching header size. Camera starts on the `.map` start/bounds when the GPS fix is outside that file; live follow only inside the file. The OSM `MapView` stays laid out when you leave the Map tab.
- Pale purple accuracy circle (radius = GPS accuracy in metres). Toggle in Settings. The circle follows the **raw** location (GNSS chip or fused), not a Kalman-smoothed stored track.
- **HUD** over both map engines: large speed (units from Settings), accuracy, GNSS used/in view. While logging: odometer, elapsed time, pulsing REC. Idle with a fix: dim compact panel at the bottom left. Hidden when a saved track is shown and you are not logging.
- Small red usage silhouette at your position (same icons as Settings). Stays upright in portrait. A north marker stays on the map.
- Green **S** at the start of the drawn track; red **E** at the end when you are not logging (while logging the silhouette is now).
- When a saved track is shown and logging is off, a broom at the top left takes the line off the map without deleting the log. Start or Saved tracks → Show on map draws it again.
- Douglas–Peucker simplification on the drawn line when **Simplify track on map** is on (see below). SQLite, Route totals, and KMZ are never simplified.



### Compass tab

Magnetic heading (MAG) from the rotation sensor, or TRUE (geographic north = MAG + declination from the last GPS fix). MAG / TRUE on this tab, default MAG, not tied to usage. Without a GPS fix TRUE stays MAG and shows No GPS. Low magnetometer accuracy: “Figure-8 in the air” under the dial. Rotating rose, fixed lubber, MAG or TRUE plus three-digit heading in the centre. Works without Start.

### Saved tracks

- List of sessions with date, usage, units.
- **Show on map** opens the Map tab on that session (Google Maps or OSM), switches Settings to the usage stored on the session, and draws it with those settings. After that, changing usage or sliders redraws the same log that way. Next Start uses the Settings then selected. The Map broom takes that line off without deleting the session.
- **Elevation** opens a GPS altitude vs distance chart for that session (dashed barometric line when those samples exist; same Baro rules as [Barometric altitude (Baro)](#barometric-altitude-baro)).
- **Delete** on each session (wraps under Elevation on a narrow phone). Confirms, then cascade-deletes the SQLite session and its points.
- Checkboxes, **Select all**, **Share selected** → KMZ or GPX:
  - one session → `GTL_yyyyMMdd_HHmmss.kmz` or `.gpx`
  - several sessions → one KMZ with a folder per track, or one GPX with several `<trk>`



### KMZ export

- Bundled play (start), pause, and stop icons (`IconStyle` scale **0.8**); map labels hidden (`LabelStyle` scale 0). The **visible** line is a KML `LineString` with `tessellate` and `clampToGround` at height 0, so Google Earth drapes it on the terrain (a `gx:Track` with GPS altitude as the 3rd `gx:coord` floats beside the road at close zoom and can vanish under the camera). Start / Pause / Stop Points also use height 0. A hidden `gx:Track` still stores `when`, speed, odometer, GPS `alt`, and `baro`.
- Path vertices are the stored log; a trailing STOP row that is only a session marker is not drawn as an extra hook. The Stop icon is on the last path vertex. Pause icons sit on pause vertices (one icon per standstill; omitted if they overlap Start or Stop).
- START / PAUSE / STOP balloons (tap the play, pause, or stop icon in Google Earth). Placemark names are **Start**, **Pause**, **Stop**. Description is HTML (`<br/>`) so every field shows in Earth details. Time is UTC with no `time=` prefix and no `UTC` suffix. Units follow Settings (metric: km/h, m / km, °C; imperial: mph, ft / mi, °F; ICAO: kt, ft / NM, °C). Balloons do **not** include `usage=` or `lean=`.
  - **Start:** `YYYY:MM:DD HH:MM:SS`, `temp=` (`N/A` when no sensor sample), `lon=`, `lat=`, `Altitude:` (GPS), `Baro:` (from stored `pressureHpa` at the **current** Settings QNH and GPS-calibration offset when you share, same as the elevation dashed line, or `-`; omitted if more than 1500 m from that point’s GPS altitude). No Speed / Avg. Speed / Max speed / duration / distance.
  - **Pause:** the same lines, plus `Speed:` (instantaneous GPS speed at that pause row), `duration=` (seconds if 60 s or less, whole minutes under 60 min, otherwise `HH:MM:SS` from Start), and `distance=` so far in the selected unit. No Avg. Speed / Max speed.
  - **Stop:** the same lines, plus `Avg. Speed:` and `Max speed:` (one decimal) from `TrackStatsCalculator` on the path, then `duration=` and `distance=` for the full session. No instant `Speed:`.
- Each hidden `gx:Track` point carries ExtendedData `speed` (m/s), `odometer` (m), `alt` (GPS metres), and `baro` (metres from `pressureHpa` at share-time QNH and offset, `-` if no sample or if the value is more than 1500 m from GPS altitude). `gx:coord` height is 0 so Earth does not lift the timed track. Re-share after changing QNH or Calibrate from GPS. Full formula: [Barometric altitude (Baro)](#barometric-altitude-baro).
- MIME `application/vnd.google-earth.kmz`. Open with Google Earth (install from Play if needed).
- Help **Sharing KMZ and GPX** lists balloon fields (EN/HU) and the SQLite `gps_events` fields.

### GPX export

- GPX 1.1 core: one `<trk>` / one `<trkseg>` per session (auto-PAUSE does not split the line). A trailing STOP marker is not an extra `<trkpt>`.
- Each stored point is a `<trkpt>` with `lat`, `lon`, `<ele>` (GPS altitude), `<time>` (UTC). No speed extension, so OsmAnd, Komoot, Garmin Connect, Relive, and QGIS can import it. Baro is not written to GPX; it stays in SQLite and KMZ. See [Barometric altitude (Baro)](#barometric-altitude-baro).
- START / PAUSE / STOP are `<wpt>` named Start, Pause, Stop. The Stop waypoint uses the last path point (same snap as KMZ).
- Several selected sessions → one `.gpx` with several `<trk>`. Filename `GTL_yyyyMMdd_HHmmss.gpx`. MIME `application/gpx+xml`.
- Saved tracks → Share selected → KMZ or GPX.



### Settings

Choosing a **usage** overwrites the linked defaults in one DataStore edit. You can change any control afterwards.


| Usage               | Units  | GNSS only | Smooth recorded track | Strength | Hold still | Density    | Simplify on map | Tolerance |
| ------------------- | ------ | --------- | --------------------- | -------- | ---------- | ---------- | --------------- | --------- |
| Run/Hike            | Metric | on        | off                   | Low      | on         | Every good | off             | 2 m       |
| Bicycle             | Metric | on        | off                   | Low      | on         | Every good | off             | 3 m       |
| Motorbike (default) | Metric | off       | on                    | Medium   | on         | Smart      | on              | 6 m       |
| Car                 | Metric | off       | on                    | Medium   | on         | Smart      | on              | 8 m       |
| Watercraft          | ICAO   | off       | on                    | Medium   | on         | Smart      | on              | 8 m       |
| Aircraft            | ICAO   | off       | on                    | High     | on         | Smart      | on              | 15 m      |


**What each control does**

- **Usage** — activity type. Reloads the table above plus the 2017 accuracy / satellite gates (Run/Hike and bicycle 45 m, others 30 m). Aircraft and watercraft also switch units to ICAO; other usages switch to metric.
- **Units** — Metric, Imperial, or ICAO on Route (km/h and metres; mph and feet/miles; knots, nautical miles, and feet). Does not move stored coordinates.
- **QNH** — sea-level pressure for the barometer, **900–1100 hPa** (default `PRESSURE_STANDARD_ATMOSPHERE` 1013.25). Shown only when the phone has a pressure sensor. Live baro, the elevation dashed line, and KMZ `Baro:` / ExtendedData `baro` use `getAltitude(QNH, pressure − offset)` (KMZ at **share** time). If that height is more than 1500 m from the point’s GPS altitude, the stored insert-time `baroAltitude` is used instead, or baro is omitted. Stored `pressureHpa` is raw; `baroAltitude` at insert uses the QNH and offset in force then. Look up a real sea-level QNH from METAR, ATIS, or airport weather (not station pressure). **Calibrate from GPS** (stand still, good GPS altitude) stores a chip offset in DataStore (±10 hPa) without changing the QNH slider; **Reset baro** clears it. **Auto-calibrate at start** (default on) runs that same calibration automatically once two consecutive GPS fixes agree on altitude (within 15 m) after each recording starts, so you don't have to tap Calibrate yourself. Full write-up: [Barometric altitude (Baro)](#barometric-altitude-baro).
- **Use downloaded OSM map** — Mapsforge file versus Google Maps. A missing or invalid `.map` turns the switch off.
- **Simplify track on map** — fewer vertices on Map only. Slider **1–20 m** (1 m steps) when the switch is on. KMZ and odometer keep every stored point.
- **Show last logged route on map** — after Stop, the last (or selected) track stays on Map. The Map broom hides a shown saved track without deleting the log.
- **Keep whole track on the screen** — while logging, each GPS refresh fits the whole track. Pan and zoom stay allowed until the next fix.
- **Keep screen on while logging** — off by default. Holds the display awake only while a session is recording (tank-mount).
- **Show accuracy marker** — pale purple circle; radius is GPS accuracy. HUD stays on the raw location (chip or fused).
- **Show fix cloud** — pastel magenta dots of raw GPS fixes while you stand still, plus a magenta CEP95 circle around the cloud centroid. Off by default. Turning it on also turns on Show accuracy marker; turning it off only hides the cloud. Pauses while you move. Not written to the log or KMZ.
- **Use GNSS only** — satellite-chip positions instead of fused location. On for Run/Hike and bicycle; off for vehicles.
- **Smooth recorded track**, **Smoothing strength**, **Hold still when stopped**, **Recording density** — these change what is **written into the tracklog**. Details below.

Existing installs that still have the old **19.5 m** simplify default migrate to the usage table the first time the new Kalman keys are written. A custom tolerance that is not 19.5 is kept.

### Other screens

- First-run safe-driving disclaimer.
- Download OSM map (Mapsforge v5 regions: Europe, selected Asia / Americas / Australia).
- Location settings (opens the system GPS panel).
- Help: accordion (one section open at a time). Usage, **Settings** (usage presets, QNH, and each control), Track logging (Kalman vs Douglas–Peucker vs density), GPS (skyplot, altitude pick, baro), Route, Map (OSM file, S/E), Compass, Viewing KMZ/KML, privacy policy, stored-trackpoint field table. English and Hungarian.
- Privacy-policy link.

---



## Architecture

Two Gradle modules:


| Module    | Role                                                                                                                                                                            |
| --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `:engine` | Pure JVM: GNSS classification, skyplot projection, GPS altitude pick, baro/QNH, Kalman track filter, fix acceptance, speed-adaptive spacing, Douglas–Peucker, track stats, KML/KMZ, GPX 1.1, map HUD visibility, compass MAG/TRUE heading, elevation series, OSM file/camera/redraw, map-visibility rules, track endpoints, fix-cloud buffer. JUnit tests live here. |
| `:app`    | Android: Compose UI, Room, DataStore, location/GNSS/sensors, foreground service, Google Maps, Mapsforge, WorkManager OSM download, FileProvider share.                          |


```
app/     Compose, Room, services, maps
engine/  Domain algorithms (no Android SDK)
docs/    Privacy policy, Play assets
```



### Data

- **Room:** `track_sessions` + `gps_events` (cascade delete). The Map polyline is always read from Room, not from an in-memory sketch. That is why the line you see is the log you stored.
- **DataStore:** disclaimer, usage, units, QNH, baro pressure offset, filters, OSM file path, map options, Kalman / density / GNSS-only / map-simplify / fix-cloud settings.
- **Files:** OSM `.map` downloads; KMZ under `files/gtltracklogs/` (FileProvider).
- **RemoteTrackSync:** no-op stub for a later backend. No live location upload.



### How logging works

The red line on Map is the stored tracklog, not a second sketch. `GtlViewModel` observes `gps_events` in Room and draws those coordinates (Google Maps polyline or Mapsforge overlay). Route totals and the shared KMZ read the same rows. If the line on the map looks like the path you actually took, that is because the logger wrote those points — not because the map snapped to a road.

```
Start
  → foreground service (visible location notification)
  → location updates (GNSS chip or fused)
  → HUD always gets the raw fix (pale purple accuracy circle)
  → optional Fix cloud (memory only: pastel magenta dots + CEP95 while standing)
  → drop poor accuracy / too few satellites
  → optional Kalman (moves lat/lon; does not drop the point)
  → density gate (Smart / Every good / mix) — this is what writes or skips
  → SQLite gps_events (START / MOVE / PAUSE)
Stop
  → STOP placemark (last **accepted** stored point, so the icon sits on the tracklog end)
  → Map / Route / KMZ all read Room
```

**Two location streams.** While the app is open, `GtlViewModel` also listens about once a second so GPS and Map HUDs update before you tap Start. After Start, `TrackingForegroundService` is the only writer. It requests updates at least every 500 ms (`minTimeMillis`, min distance `0`). Spacing is applied later in `FixAcceptance`, not by Android.

**Source.** **Use GNSS only** on → Android `GPS_PROVIDER` (the satellite chip: GPS, Galileo, GLONASS, BeiDou, QZSS, NavIC — the provider name is historical). Off → Play Services fused `PRIORITY_HIGH_ACCURACY` (satellites mixed with Wi-Fi, cell, and IMU). If the GPS provider is disabled, fused is used either way. Run/Hike default is GNSS only so a 5–10 m on-road loop is not flattened by the phone’s “where is the user?” filter before GTL ever sees it.

**HUD vs stored track.** Every update copies the **raw** `Location` to `lastLocation`. The pale purple accuracy circle, live lat/lon, provider, and accuracy are that raw fix. **Altitude** on that object is already `GpsAltitude.pick` (GNSS MSL, fused MSL, GNSS ellipsoid, fused ellipsoid; drop outside −430…9000 m). **Show fix cloud** samples the same `lastLocation` into an in-memory window (centroid RMS / CEP95) and does not write SQLite. Turning the switch on also turns on Show accuracy marker; turning it off only hides the cloud. The red polyline is whatever was **accepted into Room** (Kalman-smoothed when that switch is on). They can sit a few metres apart on purpose.

**Gate 1 — accuracy and satellites.** A fix worse than the usage accuracy (30 m, Run/Hike and bicycle 45 m) or with fewer than 4 satellites in the fix is discarded. It never enters Kalman and never becomes a row. The HUD still updates.

**Gate 2 — optional Kalman.** If **Smooth recorded track** is on, `KalmanTrackFilter.observe` runs on every accuracy-passed fix (one filter instance per Start→Stop session; resume seeds from the last stored point). It outputs a new lat/lon. Timestamp, altitude, accuracy, and satellite count stay those of the GPS fix. Speed and bearing come from the filter velocity when that speed is at least 0.3 m/s; otherwise they stay with the GPS fix (or last bearing). Kalman **moves** points and **keeps the same candidate count**. It is not map-matching and it does not drop vertices. Vehicles default this on so roundabouts look round and cruise is a clean line. Run/Hike defaults it **off** so a small figure-8 is not treated as measurement noise.

**Gate 3 — recording density.** `FixAcceptance.shouldAccept` decides whether that (possibly smoothed) point becomes a SQLite row. Kalman still updates when a point is skipped.

- First fix of the session → always stored as `START`.
- **Smart** (vehicles): write when haversine distance from the last **stored** point reaches the 2014 speed band; half that band in a curve (heading change > 15°). Run/Hike Smart uses half of that band again (min 1 m).
- **Every good** (Run/Hike default): write when `minTimeMillis` (500 ms) has elapsed **or** the heading is in a curve, and distance is at least **1 m** (vehicles) or **0.5 m** (Run/Hike and bicycle).
- Slider positions between the ends mix Smart spacing with the Every-good floor; the min-time / curve path can still accept a point.
- If GPS `bearing` is 0 (common when jogging), curve detection can use heading from consecutive positions.

**Event kind.** After a write: `START` on the first point; `PAUSE` if speed is below the usage pause threshold (0.25 m/s Run/Hike, 0.4 m/s vehicles); otherwise `MOVE`. Optional ambient temperature, last accelerometer XYZ, lean angle (gravity, tank-mount), raw `pressureHpa`, and insert-time `baroAltitude` are copied onto the row. Compass azimuth is HUD-only and is not stored. See [Barometric altitude (Baro)](#barometric-altitude-baro).

**Stop.** Always writes a `STOP` row (`isPlacemark` true) even if density would have dropped the point. Coordinates are the last **accepted** stored fix (not the raw HUD fix, which can sit a few metres off the log). KMZ/GPX then place the Stop icon on that last path vertex.

**Map draw.** `GtlViewModel` maps Room rows to `displayPoints`. `MapTrackVisibility` shows the line while logging, when **Show last logged route on map** is on, or when a Saved-tracks session is selected — unless the Map broom set `mapCleared` (idle only; logging still draws). If **Simplify track on map** is on and there are more than 4 points, Douglas–Peucker thins **only those display vertices** at the 1–20 m slider. SQLite, Route odometer, and KMZ never run through DP. With simplify **off** (Run/Hike and bicycle default), every stored vertex is on the map — that is why a small on-road loop stays visible.

**Why the map looks like your log**


| Layer                | What it does               | Map effect                                                                              |
| -------------------- | -------------------------- | --------------------------------------------------------------------------------------- |
| GNSS chip vs fused   | Who answers “where am I?”  | Run/Hike and bicycle: chip track, street-scale shape kept. Vehicles: fused, less Wi-Fi/cell jump.     |
| Accuracy / sat gates | Drop junk before Kalman    | No 200 m teleport spikes in the line.                                                   |
| Kalman (optional)    | Move points, keep count    | Vehicle roundabouts and cruise look smooth; standing lock stops a 10 m scribble.        |
| Density              | How many points are stored | Smart: fewer points at highway speed. Every good: ~2 Hz, tight loops keep vertices.     |
| Room                 | Single source of truth     | Map, Route, and KMZ are the same path.                                                  |
| Douglas–Peucker      | Display-only thin          | Long vehicle tracks stay cheap to draw; Run/Hike default is off so the map equals SQLite. |


Nothing is uploaded. `RemoteTrackSync` on stop is a no-op.

Pipeline mermaid (same flow, more boxes): [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md) / [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md).

### GNSS Skyplot

The GPS tab polar plot is a **map of the sky as the chip sees it**, not a 3D globe and not a second tracklog. It sits under SNR, after the constellation chips. Those chips stay: they are the glanceable `used/in view` counts (GPS L1, GPS L5, Galileo, GLONASS, BeiDou, QZSS, NavIC). The skyplot shows **where** those satellites are. Constellation and band primer: [docs/all-gps-systems-hu.md](docs/all-gps-systems-hu.md) (Hungarian). Data path: [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md#skyplot-circles-gps-tab).

There are two kinds of circles: the **grid** (sky geometry) and the **satellite markers**.

**Grid (large concentric rings).** Polar map, north up. Centre is the zenith (90° elevation, satellite overhead). The outer thick ring is the **horizon** (0°). The two thinner rings are **30°** and **60°**. Closer to the centre means higher elevation. Twelve o’clock is north (azimuth 0°); east, south, and west follow clockwise. The plot does **not** rotate with the phone — the Compass tab does that. A satellite below the horizon is omitted.

**Satellite markers (small circles).** Each marker is one satellite (L1+L5 rows of the same SVID are one point).

| Mark | Meaning |
|---|---|
| **Filled** disk | **Used** — in the current position fix |
| **Hollow** ring | **In view** — the chip sees it, but it is not in the fix |
| **Inner ring** on the disk | **L5** — L5-class carrier (~1176.45 MHz; GPS L5, Galileo E5a too) |

**Colour** is the constellation, the same as the chips above: GPS blue, Galileo lime, GLONASS carmine, BeiDou amber, QZSS magenta, NavIC cyan; SBAS/unknown muted. Marker **size is fixed**; signal strength (SNR) stays on the bar above, not on the circle size.

**Overlays.** Upper-left title **SKYPLOT**. Upper-right **In view** (hollow ring). Lower-left **Used** (filled). Lower-right **L5** (filled plus inner ring). Cardinal letters sit on the horizon ring: **N** carmine at 12 o’clock, then **E**, **S**, **W**.

**Dual frequency.** Android reports L1 and L5 of the same SVID as two `GnssStatus` rows at the same azimuth/elevation. The plot merges them into one point so you do not see two stacked dots. The chips still count those rows separately (`satellitesInView` is the raw row count, same as the Map HUD `used/in view`).

**Data path.** `GnssStatus.Callback` → per-satellite `SatelliteSample` (azimuth, elevation, CN0, used, constellation, carrier) → `GnssSnapshot.satellites` in memory → polar canvas. Nothing is written to `gps_events`, KMZ, or GPX. Kalman, recording density, and **Use GNSS only** change *where the fix comes from*, not this plot. The skyplot is the chip’s current sky, fused or not.

**When it runs.** Live as soon as the app has location permission, like the compass and the GPS numbers. Start is not required. Empty rings until satellites appear (or if permission is missing). It does not cache the last “pretty” sky in a tunnel.

Engine: `Gnss.kt` (sample + snapshot) and `Skyplot.kt` (projection + L1/L5 merge). UI: `GnssSkyplot` on the GPS tab. Map HUD is unchanged.

### Barometric altitude (Baro)

**What it is.** Height from the phone’s air-pressure sensor (`TYPE_PRESSURE`), not GPS altitude and not Google Earth’s terrain DEM. The number is aviation **QNH altitude**: sea-level pressure plus the International Standard Atmosphere (ISA) so the result is approximate metres above mean sea level. Weather, chip bias, and a wrong QNH still shift it versus GPS.

**How it is calculated.** Live and stored conversion uses Android `SensorManager.getAltitude(qnhHpa, pressureHpa − offsetHpa)`. The `:engine` copy (`BaroAltitude.metersFromPressureHpa`) is the same ISA formula:

`h = 44330 × (1 − (p_corr / QNH)^(1 / 5.255))`

where `p_corr` is raw hectopascals minus the GPS-calibration offset. QNH is **900–1100 hPa**, default `PRESSURE_STANDARD_ATMOSPHERE` **1013.25**. The offset is **±10 hPa**. Look up sea-level QNH from METAR, ATIS, or airport weather — not station pressure (QFE).

**Calibrate from GPS.** Stand still with a trusted GPS altitude. The app computes the station pressure the ISA would expect at that height and QNH (`expectedStationHpa`), then stores `pressureHpa − expected` as the DataStore offset. The QNH slider does not move. **Reset baro** clears the offset.

**Auto-calibrate at start.** Settings → Baro → **Auto-calibrate at start** (default **on**, shown only when the phone has a pressure sensor). After Start, each GPS fix that passes the normal accuracy/satellite filter is compared with the previous one; once two **consecutive** fixes agree on altitude within `MaxAltitudeJitterMeters` (**15 m**), the service runs the identical calculation as **Calibrate from GPS** — using that fix's altitude and the current pressure reading — and stores the resulting offset, once per recording. Requiring two fixes to agree, rather than trusting the very first one, matters in practice: GPS altitude fixes right after a location request starts can be off by tens of metres for a single sample (vertical accuracy converges slower than horizontal, and Android's own accuracy figure only describes horizontal error) — an early build of this feature calibrated straight off the first passing fix and locked in a ~41 m error for an entire ride after that fix's altitude spiked from ~144 m to 185 m for one sample. You no longer have to stand still and tap Calibrate before every ride; the offset is simply current for that session's actual weather instead of defaulting to ISA 1013.25 hPa (which is normally 40–90 m off from GPS, see the 1500 m guard note below). If pressure, a plausible altitude, or a corroborating previous fix isn't available yet, the app just waits and tries again on the next fix — still only once it succeeds. Turning the toggle off restores the old behaviour: the offset only changes when you tap **Calibrate from GPS** or **Reset baro** yourself. A large, sudden mid-ride jump in the dashed baro line (independent of this feature) is usually the phone's barometer picking up a real but unrelated pressure change — pocket, bag, car door, air conditioning — not a bug; **Calibrate from GPS** once things settle, or **Reset baro**, recovers it. Engine: `BaroAltitude.autoCalibrateEligible`. App: `TrackingForegroundService.maybeAutoCalibrateBaro`.

**At insert vs on screen.** Each `gps_events` row stores raw `pressureHpa` and `baroAltitude` computed with the QNH and offset **then**. The GPS-tab Baro, Route / Saved-tracks dashed elevation line, and KMZ `Baro:` / ExtendedData `baro` recompute from `pressureHpa` with the **current** QNH and offset (`displayedMeters`; KMZ at **share** time). Changing QNH after the ride updates those displays without rewriting SQLite. Re-share the KMZ after a QNH or Calibrate change.

**1500 m GPS guard.** If the recomputed height is more than `MaxGpsDeltaMeters` (**1500 m**) from that point’s GPS altitude, `pickDisplayed` uses stored `baroAltitude` when that value is within 1500 m of GPS; otherwise baro is omitted (`Baro: -` / no dashed sample). A few tens of metres between ISA 1013.25 and a real METAR (for example LHBP ~1022 hPa, about 70 m vs ISA, with GPS ~140 m) is valid and kept. About 2000 m next to 140 m GPS is rejected (a bogus ~800 hPa sample).

**GPX.** `<ele>` is GPS altitude only. Baro stays in SQLite and in KMZ balloons / ExtendedData.

Engine: `BaroAltitude.kt`. App: `AndroidBaroAltitude.kt`.

**Accuracy — this is an estimate, not a measurement.** `TYPE_PRESSURE` only reports ambient air pressure; "altitude" is a computed conversion of that pressure through the ISA model, not a direct reading. It requires the local sea-level pressure (QNH) to be accurate, and drifts as real weather moves away from that reference — no sensor precision fixes a wrong or stale QNH. This is documented by Android itself, not just behaviour observed in this app.

**Official documentation.** The Javadoc directly above `SensorManager.getAltitude(p0, p)` in AOSP (`frameworks/base/core/java/android/hardware/SensorManager.java`) says:

> "The pressure at sea level must be known [...] If unknown, you can use `PRESSURE_STANDARD_ATMOSPHERE` as an approximation, but absolute altitudes won't be accurate."

— and recommends using the function for the *difference* between two altitudes rather than trusting the absolute value. The `44330` scale height and `1/5.255` exponent in that method (and in this app's `BaroAltitude.metersFromPressureHpa`) are not Android-specific: they implement the International Standard Atmosphere (ISA) hypsometric formula, standardized by ICAO and equivalently by the U.S. Standard Atmosphere, 1976.

**References:**
- Android API reference: [`SensorManager.getAltitude(float, float)`](https://developer.android.com/reference/android/hardware/SensorManager#getAltitude(float,%20float))
- AOSP source, verbatim Javadoc and implementation: [`SensorManager.java`](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/hardware/SensorManager.java)
- ICAO Doc 7488, *Manual of the ICAO Standard Atmosphere* — defines the barometric formula's constants
- U.S. Standard Atmosphere, 1976 (NOAA / NASA / USAF) — equivalent standard atmosphere model

### How Run/Hike logs like a sports watch

A dedicated watch such as a Suunto Ambit 3 Peak records the **GNSS chip** about once per second. It does not snap the line to a street, and it does not run a phone “fused” filter. **GNSS** is the family of satellite systems (GPS, Galileo, GLONASS, BeiDou, QZSS, NavIC). GPS is one constellation; the chip uses all of them.

A phone **fused** location API answers a different question: “where is the user?” It mixes satellites with Wi-Fi, cell, and IMU, then smooths. A 5–10 m loop you actually ran on the road looks like pedestrian noise and is flattened **before** GTL would store it.

**Use GNSS only** asks Android `GPS_PROVIDER` (the chip, all constellations — the name is historical). Stored points are those chip positions, the same idea as the watch’s 1 s GPS track. (Suunto FusedSpeed is pace, not the polyline. This app does not implement FusedTrack IMU gap-fill.)

The rest of the Run/Hike preset keeps that shape in SQLite, on Map, and in KMZ:

- **Smooth recorded track off** — no second constant-velocity Kalman that treats a small round as measurement noise. You can turn Kalman back on; Run/Hike then adds extra process noise so a 5 m loop is not pulled onto the street.
- **Every good** — store about every 500 ms (settings min time), or sooner in a curve. If GPS bearing is 0 (common when jogging), heading can come from consecutive positions.
- **0.5 m** duplicate drop (vehicles stay at 1 m) so a tight loop keeps vertices.
- **Simplify track on map off** — the drawn line is every stored point.

Accuracy and satellite gates still drop bad fixes. This does **not** store fewer satellite fixes; it stores more of the good ones, and it stops using Wi-Fi/cell/fused guesses as the track source.

### Kalman filter (how stored points are smoothed)

**Purpose.** Cut GPS jitter on a driving or flying track (roundabouts look round, cruise is a clean line) without flattening a Run/Hike figure-8, and without a 10 m scribble while you stand still. Kalman is a **noise filter**: it **moves** accepted points and **keeps the same count**. It is not map-matching (no snap to OSM/Google roads) and not Douglas–Peucker (DP **drops** vertices, and only on the Map tab).

**Where it sits in the pipeline.** One `KalmanTrackFilter` per Start→Stop session, in `:engine`. `TrackingForegroundService` does this for every location update:

1. Copy the fix to the HUD (`lastLocation`). The pale purple accuracy circle always follows this **raw** point.
2. Drop the fix if accuracy is worse than the usage gate (30 m, Run/Hike and bicycle 45 m) or satellites-in-fix is below 4. Rejected fixes never reach Kalman or SQLite.
3. If **Smooth recorded track** is on, run `KalmanTrackFilter.observe`. The filter outputs a new lat/lon. Timestamp, altitude, accuracy, and satellite count stay those of the GPS fix. Speed and bearing come from the filter velocity when that speed is at least 0.3 m/s. Run/Hike and pedestrian modes add extra position process noise so a 5 m loop is not pulled onto the chord.
4. **Recording density** (`FixAcceptance`) decides whether to **write** that (possibly smoothed) point. If the gap is too small, the Kalman state is still updated, but Room does not get a row.
5. On Stop, the STOP row uses the last **accepted** stored point so Map, KMZ, and GPX end on the log.

So Kalman changes **where** stored points sit. Density changes **how many** of them are stored. Map simplify changes **neither** — it only thins the polyline drawn on Map.

**How the filter works.** Constant-velocity model in local metres (`GeoProjection`, same `111_320` m/deg as DP). State is `[east, north, vEast, vNorth]`. The GPS measurement is **position only** (no speed/heading update). Each step:

1. **Predict** — move the state forward by `dt` (clamped to a small range so a pause in GNSS does not explode the covariance).
2. **Process noise** `q` (m²/s⁴) = `baseQ(usage) × strengthMultiplier(slider)`. Then `× turnBoost(usage)` when the heading vs the previous **output** bearing (or heading from consecutive positions if GPS bearing is 0) changes more than 15°. **High** `q` **= trust GPS more = less smoothing.** Low `q` = trust the motion model more = smoother arcs, more lag when you actually turn.
3. **Update** — Joseph-form Kalman update with measurement σ = max(GPS accuracy, 2 m).
4. **Jump** — if the innovation is larger than `max(50 m, 8 × accuracy)` (tunnel exit, GPS teleport), re-initialize at the new fix. The gap is **not** interpolated.
5. **Stationary lock** (if enabled) — when GPS speed or predicted speed is below the usage pause threshold (0.25 m/s Run/Hike, 0.4 m/s vehicles) and displacement is under 1.5 m, freeze the last output, zero velocity, shrink position covariance.

Base `q` at mid slider (old Medium): Run/Hike 8.0, bicycle 6.0, motorbike 2.5, car/watercraft 1.5, aircraft 0.8. Turn boost: Run/Hike 10, bicycle 8, motorbike 5, car/water 3, aircraft 2. Strength slider `t` in `[0, 1]` (Low→High) multiplies `q` by `4^(1 − 2t)`: Low ×4, mid ×1, High ×0.25.

**Not implemented (on purpose).** OSM/Google snap-to-road, RTS forward–backward smoother, IMU dead reckoning / Suunto FusedTrack gap-fill, display splines.

### Effect of settings on the tracklog

These are the controls that change SQLite `gps_events`, Route odometer / speeds, and the shared KMZ. Everything else is display-only.


| Setting                                                           | Written into the tracklog?    | Effect                                                                                                                                                                                                                                                                                                                                                                                               |
| ----------------------------------------------------------------- | ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Use GNSS only**                                                 | Yes (source)                  | **On:** `GPS_PROVIDER` satellite-chip positions (all GNSS constellations). **Off:** Play Services fused HIGH_ACCURACY. If the GPS provider is disabled, fused is used either way.                                                                                                                                                                                                                    |
| **Smooth recorded track**                                         | Yes                           | **On:** each candidate point is Kalman-smoothed before density. Route, Map (raw polyline), and KMZ all show the smoothed path. **Off:** the location source is stored as-is after the accuracy/sats gate (Run/Hike default).                                                                                                                                                                           |
| **Smoothing strength** (Low–High slider; only if smoothing is on) | Yes                           | **Low:** GPS jitter stays, figure-8 and zigzag remain. **High** (aircraft default): roundabouts and cruise are cleaner; hairpins lag a little. Mid is motorbike/car/water.                                                                                                                                                                                                                           |
| **Hold still when stopped**                                       | Yes (only if smoothing is on) | Below pause speed the stored coordinate does not wander. A 6 m GPS cluster at a red light collapses toward one point. Does not drop rows by itself — density still decides writes.                                                                                                                                                                                                                   |
| **Recording density** (Smart–Every good slider)                   | Yes                           | **Smart:** write when distance from the last **stored** point reaches the 2014 speed band (half in a curve; Run/Hike Smart half again, min 1 m). Highway stores fewer points; walking stores more. **Every good:** write about once per `minTime` (500 ms) or sooner in a curve. Vehicles drop stacks closer than **1 m**; Run/Hike closer than **0.5 m**. Positions between the ends mix the two rules. |
| **Simplify track on map** (1–20 m slider)                         | **No**                        | Fewer vertices on the Map tab only. Stored points, odometer, and KMZ are unchanged.                                                                                                                                                                                                                                                                                                                  |
| **Show accuracy marker**                                          | **No**                        | Pale purple circle on the **raw** GPS fix, even when Kalman is on.                                                                                                                                                                                                                                                                                                                                       |
| **Show fix cloud**                                                | **No**                        | Pastel magenta dots of raw HUD fixes while standing, CEP95 around the centroid. Off by default. Turning it on also turns on Show accuracy marker; turning it off only hides the cloud. Pauses while moving. Not stored.                                                                                                                                                                    |
| **Units**                                                         | Labels only                   | Metric / Imperial / ICAO format Route and KMZ balloons (metric km/h, m, °C; imperial mph, ft, °F; ICAO kt, ft, °C). Coordinates stay WGS-84. Aircraft and watercraft presets select ICAO.                                                                                                                                                                                                                                                                        |
| **QNH** (900–1100 hPa)                                            | Baro at insert; KMZ at share  | Live baro, the elevation dashed line, and KMZ balloons / ExtendedData `baro` use `getAltitude(QNH, pressure − offset)` (KMZ when you share). If that height is more than 1500 m from GPS altitude, stored insert-time `baroAltitude` is used, or baro is omitted. `baroAltitude` stored on the row uses the QNH and offset in force then; raw `pressureHpa` is unchanged. **Calibrate from GPS** writes a DataStore offset (±10 hPa) without changing the slider. Default ISA 1013.25. |
| Accuracy / satellite gates                                        | Yes (rejection)               | Fixes worse than 30 m (Run/Hike and bicycle 45 m) or with fewer than 4 satellites in the fix are discarded before Kalman. Not shown as Settings sliders.                                                                                                                                                                                                                                                           |


**Practical result.** Motorbike default: fused + smoothed street track, Smart spacing, Map line thinned at 6 m. Run/Hike and bicycle default: GNSS chip, no Kalman, almost every good fix stored (0.5 m floor), Map shows every stored vertex so a small on-road loop stays visible. Aircraft default: stronger smoothing, Smart spacing, 15 m Map thinning, speed/distance in knots and nautical miles.

### Recording density

Separate from Kalman. Kalman always sees every accuracy-passed fix while smoothing is on; density only gates **storage**.

- **Smart** (vehicles): write a point when haversine distance from the last **stored** fix reaches the 2014 speed band; half that in a curve. Run/Hike Smart uses half of that band again (walk/jog was too coarse for a small figure-8).
- **Every good** (Run/Hike and bicycle default): accept when `minTimeMillis` has elapsed or heading is in a curve. Vehicles drop stacks closer than 1 m; Run/Hike and bicycle drop closer than 0.5 m.
- **Between the slider ends:** required distance is a mix of the Smart band and the Every-good floor (1 m vehicles, 0.5 m Run/Hike and bicycle); the min-time / curve path can also accept a point.



### Douglas–Peucker (map simplify)

**Purpose.** Reduce how many vertices the Map tab has to draw. A long session can have thousands of stored fixes; most of them sit almost on a straight line. Dropping those intermediates keeps the map responsive without changing what was recorded.

This is **display-only**. `gps_events`, Route odometer / speeds, and KMZ export always use the Room rows (already Kalman-smoothed when that setting is on). Douglas–Peucker does not smooth GPS noise: remaining corners stay sharp. It only discards points that are close enough to a chord.

**When it runs.** Settings → **Simplify track on map** (`optimizationActive`; on for vehicles, off for Run/Hike and bicycle). While logging, `GtlViewModel` uses that switch. **Show on map** copies the session usage into Settings first, then the drawn line follows the current Settings sliders, so changing usage after load previews another simplify mode. Tolerance is a **1–20 m** slider (1 m steps; motorbike default 6 m, car 8 m). `DouglasPeucker.clampTolerance` still snaps and clamps on write.

**How it works.** Classic Ramer–Douglas–Peucker, with distances in metres on a local tangent plane (`111_320` m per degree of latitude; longitude scaled by `cos(lat)`):

1. Always keep the first and last points of the current segment.
2. For every point between them, measure the perpendicular distance to the straight line (chord) from start to end.
3. Take the farthest point. If that distance is **greater than** the tolerance, keep it — it is a real bend — and recurse on the two sub-segments (start→farthest, farthest→end).
4. If the farthest point is **within** the tolerance, drop every intermediate point: they all lie close enough to the chord.

So a nearly colinear stretch collapses to two endpoints, while a corner that sticks out more than the threshold is kept. A two-way road loop (~6–8 m wide) needs a low threshold (about 2–8 m) or it collapses to a single line at high tolerance. Implementation: `engine/.../DouglasPeucker.kt`. Pipeline context: [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md) / [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md).

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
./gradlew bundleRelease      # signed AAB for Play
```

Release APK: `app/build/outputs/apk/release/app-release.apk`  
Release AAB: `app/build/outputs/bundle/release/app-release.aab` (Play App Signing; upload key = EKL release keystore)

### Stack

Kotlin 2.2 · AGP 9.2 · Compose BOM 2025.12 · Room 2.7 · DataStore · Navigation Compose · Play Services Location / Maps · Maps Compose · Mapsforge 0.25 · WorkManager · KSP

---



## Technical documents


| Document                                                                       | What it is                                                                                                          |
| ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------- |
| [CHANGELOGS.md](CHANGELOGS.md)                                                 | Canonical version history (2.0.0 rewrite through Unreleased, English and Hungarian)                                 |
| [docs/play-console/whatsnew.txt](docs/play-console/whatsnew.txt)               | Play Console release name and EN/HU what’s-new text                                                                 |
| [docs/play-console/privacy-policy.html](docs/play-console/privacy-policy.html) | Privacy policy (local copy of the live KLHome page)                                                                 |
| [docs/play-console/feature-graphic.png](docs/play-console/feature-graphic.png) | Play Store feature graphic                                                                                          |
| [docs/screenshots/](docs/screenshots/)                                         | Play listing screenshots (GPS, route, map/tracking, compass, settings, saved tracks, help, about, Google Earth KMZ) |
| [docs/DBSTRUCT-en.md](docs/DBSTRUCT-en.md)                                     | SQLite schema (`gtl.db`) mermaid                                                                                    |
| [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md)                               | GPS listen → filter → Room → UI / KMZ (EN); mermaid of the logging pipeline                                         |
| [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md)                               | GPS figyelés → szűrés → Room → UI / KMZ (HU)                                                                        |
| [docs/all-gps-systems-hu.md](docs/all-gps-systems-hu.md)                       | GNSS systems and bands: GPS L1/L5, Galileo, BeiDou, GLONASS, QZSS, NavIC (Hungarian primer)                         |
| [docs/dp-kalman-smoothing-en.md](docs/dp-kalman-smoothing-en.md)               | Original Kalman implementation brief; **as-built notes at the top** (current behaviour is this README)              |


Engine entry points worth reading:

- `engine/.../FixAcceptance.kt` — accuracy / sats / Smart or Every-fix density
- `engine/.../KalmanTrackFilter.kt` — constant-velocity smoother (stored points)
- `engine/.../UsageSmoothingDefaults.kt` — usage preset (GNSS only, Kalman, density, map simplify)
- `engine/.../SpeedAdaptiveSpacing.kt` — metres between points by km/h and curves
- `engine/.../DouglasPeucker.kt` — map-only polyline simplify (metres, local projection)
- `engine/.../TrackStats.kt` — odometer, moving vs waiting
- `engine/.../KmlExporter.kt` + `KmzExporter.kt` — KMZ with local icons (`IconStyle` scale 0.8), clampToGround, HTML balloons
- `engine/.../KmlDescriptions.kt` — Start / Pause / Stop Earth details (datetime, temp, lon/lat, Altitude, Baro, Speed / Avg. Speed / Max speed, duration, distance)
- `engine/.../TrackLogExport.kt` — path vs Start/Pause/Stop markers for KMZ and GPX; KMZ baro from `pressureHpa` at share-time QNH (`displayedMeters`, 1500 m GPS guard)
- `engine/.../GpxExporter.kt` — GPX 1.1 `trk` / `trkseg` / `trkpt` + Start/Pause/Stop `wpt`
- `engine/.../Gnss.kt` — constellation / L1 vs L5 / SNR / satellite list
- `engine/.../Skyplot.kt` — polar projection / dual-frequency merge
- `engine/.../GpsAltitude.kt` — MSL then GNSS then fused; drop outside −430…9000 m
- `engine/.../BaroAltitude.kt` — ISA / QNH metres from `pressureHpa`; `displayedMeters` / `pickDisplayed` (1500 m vs GPS)
- `engine/.../OsmMapFile.kt` — Mapsforge magic + header file size
- `engine/.../OsmMapCamera.kt` — OSM centre/zoom inside the `.map` bounds
- `engine/.../OsmMapViewRedraw.kt` — when Compose must invalidate OSM tiles
- `engine/.../MapFitZoom.kt` — zoom clamp / fit size check
- `engine/.../TrackEndpoints.kt` — green S / red E (end hidden while logging)
- `engine/.../FixCloud.kt` — in-memory standing-fix cloud / CEP95
- `engine/.../MapDisplayUsage.kt` — which usage and simplify the map follows
- `engine/.../MapTrackVisibility.kt` — when the map must draw a track (logging always; otherwise last-track or a selected session, unless cleared)
- `app/.../LocationClient.kt` — fused HIGH_ACCURACY or `GPS_PROVIDER` when Use GNSS only is on; fused still listens to `GPS_PROVIDER` for altitude

---



## Play listing screenshots

`docs/screenshots/`

- `gps-idle.png` — GPS tab: constellation chips, SNR, polar skyplot, GPS / Baro altitude, bottom tabs (idle, waiting for a fix). Recaptured 2026-09-12.
- `gps-logging.png` — older GPS tab while logging (pre-skyplot layout, 460×1024)
- `route.png` — Route totals, lean, GPS/baro elevation profile, bottom tabs. Recaptured 2026-09-12.
- `map.png` — Full phone frame: Map with a shown track, green S / red E, idle Map HUD (speed, place, accuracy, GNSS used/in view), Google Maps, bottom tabs. Recaptured 2026-09-13 (1080×2160).
- `tracking.png` — older Map while recording (pre-S/E, 460×1024)
- `googleearth.png` — shared KMZ in Google Earth
- `compass.png` — Compass MAG / TRUE rose, bottom tabs. Recaptured 2026-09-12.
- `about.png`
- `settings.png` — Full phone frame: six usage types (Watercraft selected), QNH 1023 hPa with Calibrate / Reset, OSM, simplify, GNSS only, smooth, hold still, recording density. Recaptured 2026-09-13 (1080×2160).
- `saved-tracks.png` — Saved tracks: Show on map, Elevation, Delete, GPS/baro profile. Recaptured 2026-09-12.
- `settings-density.png` — Older Settings layout with simplify / smoothing sliders (2.0.3)
- `help.png` — Help topics (2.0.3)
- `app-icon.png`

Phone listing: 24-bit PNG, no alpha. Older shots (`gps-idle.png`, `route.png`, `compass.png`, `saved-tracks.png`) are 1080×1920 (9:16, chrome cropped). `map.png` and `settings.png` are the full device frame scaled to 1080×2160 (Play long-edge = 2× short-edge; no UI cropped). Upload those six with this release. Logging HUD Map and the feature graphic wait on dark tiles (see the roadmap).

---



## Next to do

- Add light and dark themes

---



## Not in this app

Intentionally not ported from 2014 (policy or dead APIs): IMEI / `READ_PHONE_STATE`, live lat/lng upload, follow-me web page, remote unlock, Google Directions, app-driven GPS/Wi-Fi toggles, boot auto-start.