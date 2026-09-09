# GTL GPS Track Logger

On-device GPS track logger. Route points stay in SQLite on the phone. Share a KMZ (KML + icons) to Google Earth or another map app. Nothing is uploaded to our servers.

Kotlin + Jetpack Compose rewrite of the 2014 Eclipse app (`gtl-e`). Application id `com.lkovari.mobile.apps.gtl`.

**Version:** 2.0.3 (versionCode 21)  
**SDK:** minSdk 24 · targetSdk 36 · compileSdk 36  
**UI:** English and Hungarian, Material 3, portrait

Privacy policy: https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html

---

## Features

### Logging

- **Start / Stop** records a session as a visible foreground service with a notification.
- Fixes are stored only after they pass accuracy and satellite-count gates. Optional **Kalman** smoothing then moves the point. **Smart** or **Every good fix** density decides whether to write it (see Settings). Runner default is **Use GNSS only** (satellite chip, not fused location) with smoothing off so small on-road shapes stay in the tracklog. Full pipeline: [How logging works](#how-logging-works).
- Event kinds: `START`, `MOVE`, `PAUSE` (below usage pause speed), `STOP`.
- Usage modes: aircraft, watercraft, car, motorbike (default), runner. Choosing a usage writes a full preset (filters, GNSS only, smoothing, density, map simplify). Runner uses a looser accuracy filter and a lower pause threshold.
- Optional ambient temperature (`TYPE_AMBIENT_TEMPERATURE`), accelerometer samples, and lean angle (gravity, tank-mount) on each stored point.

### GPS tab

- Live satellite counts: GPS L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC.
- SNR quality (excellent / good / fair / poor / none).
- Latitude, longitude, accuracy, provider, altitude, ambient temperature, logging status.

### Route tab

Session totals after Start: elapsed time, odometer, time moving, time waiting, speed, average speed, altitude, bearing, lean angle (phone flat on a motorbike tank), temperature range when a sensor exists.

### Map tab

- Centers on current location; follows while logging. **Keep whole track on the screen** fits the whole route after each GPS refresh (pan and zoom stay allowed until the next fix).
- Red polyline from Room (live session, last saved track, or a track chosen in Saved tracks). The Map line **is** the stored log; there is no separate sketch. See [How logging works](#how-logging-works).
- **Google Maps** when `MAPS_API_KEY` is set; otherwise an on-device message.
- **OSM Mapsforge** after you download a region and enable **Use downloaded OSM map**. The same polyline and accuracy ring draw on OSM.
- Light purple accuracy circle (radius = GPS accuracy in metres). Toggle in Settings. The circle follows the **raw** location (GNSS chip or fused), not a Kalman-smoothed stored track.
- Douglas–Peucker simplification on the drawn line when **Simplify track on map** is on (see below). SQLite, Route totals, and KMZ are never simplified.

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
- Every stored GPS point is on a `gx:Track` (`when`, lon/lat/alt, speed).
- START / PAUSE / STOP balloons: `time=` (UTC), `lat=`, `lon=`, `speed=`, `temp=`. Pause and stop force `speed=0`. STOP also `maxSpeed=` / `avgSpeed=`.
- MIME `application/vnd.google-earth.kmz`. Open with Google Earth (install from Play if needed).
- Help documents this flow (EN/HU) and lists SQLite `gps_events` fields.

### Settings

Choosing a **usage** overwrites the linked defaults in one DataStore edit. You can change any control afterwards.

| Usage | Units | GNSS only | Smooth recorded track | Strength | Hold still | Density | Simplify on map | Tolerance |
|---|---|---|---|---|---|---|---|---|
| Runner | Metric | on | off | Low | on | Every good | off | 2 m |
| Motorbike (default) | Metric | off | on | Medium | on | Smart | on | 6 m |
| Car | Metric | off | on | Medium | on | Smart | on | 8 m |
| Watercraft | ICAO | off | on | Medium | on | Smart | on | 8 m |
| Aircraft | ICAO | off | on | High | on | Smart | on | 15 m |

**What each control does**

- **Usage** — activity type. Reloads the table above plus the 2017 accuracy / satellite gates (runner 45 m, others 30 m). Aircraft and watercraft also switch units to ICAO; other usages switch to metric.
- **Units** — Metric, Imperial, or ICAO on Route (km/h and metres; mph and feet/miles; knots, nautical miles, and feet). Does not move stored coordinates.
- **Use downloaded OSM map** — Mapsforge file versus Google Maps.
- **Simplify track on map** — fewer vertices on Map only. Slider **1–20 m** (1 m steps) when the switch is on. KMZ and odometer keep every stored point.
- **Show last logged route on map** — after Stop, the last (or selected) track stays on Map.
- **Keep whole track on the screen** — while logging, each GPS refresh fits the whole track. Pan and zoom stay allowed until the next fix.
- **Show accuracy marker** — purple circle; radius is GPS accuracy. HUD stays on the raw location (chip or fused).
- **Use GNSS only** — satellite-chip positions instead of fused location. On for runner; off for vehicles.
- **Smooth recorded track**, **Smoothing strength**, **Hold still when stopped**, **Recording density** — these change what is **written into the tracklog**. Details below.

Existing installs that still have the old **19.5 m** simplify default migrate to the usage table the first time the new Kalman keys are written. A custom tolerance that is not 19.5 is kept.

### Other screens

- First-run safe-driving disclaimer.
- Download OSM map (Mapsforge v5 regions: Europe, selected Asia / Americas / Australia).
- Location settings (opens the system GPS panel).
- Help: accordion (one section open at a time). Usage, **Settings** (usage presets and each control), Track logging (Kalman vs Douglas–Peucker vs density), GPS, Route, Map, Compass, Viewing KMZ/KML, privacy policy, stored-trackpoint field table. English and Hungarian.
- Privacy-policy link.

---

## Architecture

Two Gradle modules:

| Module | Role |
|---|---|
| `:engine` | Pure JVM: GNSS classification, Kalman track filter, fix acceptance, speed-adaptive spacing, Douglas–Peucker, track stats, KML/KMZ, map-visibility rules. JUnit tests live here. |
| `:app` | Android: Compose UI, Room, DataStore, location/GNSS/sensors, foreground service, Google Maps, Mapsforge, WorkManager OSM download, FileProvider share. |

```
app/     Compose, Room, services, maps
engine/  Domain algorithms (no Android SDK)
docs/    Privacy policy, Play assets, renewal notes
```

### Data

- **Room:** `track_sessions` + `gps_events` (cascade delete). The Map polyline is always read from Room, not from an in-memory sketch. That is why the line you see is the log you stored.
- **DataStore:** disclaimer, usage, units, filters, OSM file path, map options, Kalman / density / GNSS-only / map-simplify settings.
- **Files:** OSM `.map` downloads; KMZ under `files/gtltracklogs/` (FileProvider).
- **RemoteTrackSync:** no-op stub for a later backend. No live location upload.

### How logging works

The red line on Map is the stored tracklog, not a second sketch. `GtlViewModel` observes `gps_events` in Room and draws those coordinates (Google Maps polyline or Mapsforge overlay). Route totals and the shared KMZ read the same rows. If the line on the map looks like the path you actually took, that is because the logger wrote those points — not because the map snapped to a road.

```
Start
  → foreground service (visible location notification)
  → location updates (GNSS chip or fused)
  → HUD always gets the raw fix (purple accuracy circle)
  → drop poor accuracy / too few satellites
  → optional Kalman (moves lat/lon; does not drop the point)
  → density gate (Smart / Every good / mix) — this is what writes or skips
  → SQLite gps_events (START / MOVE / PAUSE)
Stop
  → STOP placemark (last Kalman point if smoothing is on, else last raw fix)
  → Map / Route / KMZ all read Room
```

**Two location streams.** While the app is open, `GtlViewModel` also listens about once a second so GPS and Map HUDs update before you tap Start. After Start, `TrackingForegroundService` is the only writer. It requests updates at least every 500 ms (`minTimeMillis`, min distance `0`). Spacing is applied later in `FixAcceptance`, not by Android.

**Source.** **Use GNSS only** on → Android `GPS_PROVIDER` (the satellite chip: GPS, Galileo, GLONASS, BeiDou, QZSS, NavIC — the provider name is historical). Off → Play Services fused `PRIORITY_HIGH_ACCURACY` (satellites mixed with Wi-Fi, cell, and IMU). If the GPS provider is disabled, fused is used either way. Runner default is GNSS only so a 5–10 m on-road loop is not flattened by the phone’s “where is the user?” filter before GTL ever sees it.

**HUD vs stored track.** Every update copies the **raw** `Location` to `lastLocation`. The purple accuracy circle, live lat/lon, provider, and accuracy are that raw fix. The red polyline is whatever was **accepted into Room** (Kalman-smoothed when that switch is on). They can sit a few metres apart on purpose.

**Gate 1 — accuracy and satellites.** A fix worse than the usage accuracy (30 m, runner 45 m) or with fewer than 4 satellites in the fix is discarded. It never enters Kalman and never becomes a row. The HUD still updates.

**Gate 2 — optional Kalman.** If **Smooth recorded track** is on, `KalmanTrackFilter.observe` runs on every accuracy-passed fix (one filter instance per Start→Stop session; resume seeds from the last stored point). It outputs a new lat/lon. Timestamp, altitude, accuracy, and satellite count stay those of the GPS fix. Speed and bearing come from the filter velocity when that speed is at least 0.3 m/s; otherwise they stay with the GPS fix (or last bearing). Kalman **moves** points and **keeps the same candidate count**. It is not map-matching and it does not drop vertices. Vehicles default this on so roundabouts look round and cruise is a clean line. Runner defaults it **off** so a small figure-8 is not treated as measurement noise.

**Gate 3 — recording density.** `FixAcceptance.shouldAccept` decides whether that (possibly smoothed) point becomes a SQLite row. Kalman still updates when a point is skipped.

- First fix of the session → always stored as `START`.
- **Smart** (vehicles): write when haversine distance from the last **stored** point reaches the 2014 speed band; half that band in a curve (heading change &gt; 15°). Runner Smart uses half of that band again (min 1 m).
- **Every good** (runner default): write when `minTimeMillis` (500 ms) has elapsed **or** the heading is in a curve, and distance is at least **1 m** (vehicles) or **0.5 m** (runner).
- Slider positions between the ends mix Smart spacing with the Every-good floor; the min-time / curve path can still accept a point.
- If GPS `bearing` is 0 (common when jogging), curve detection can use heading from consecutive positions.

**Event kind.** After a write: `START` on the first point; `PAUSE` if speed is below the usage pause threshold (0.25 m/s runner, 0.4 m/s vehicles); otherwise `MOVE`. Optional ambient temperature, last accelerometer XYZ, and lean angle (gravity, tank-mount) are copied onto the row. Compass azimuth is HUD-only and is not stored.

**Stop.** Always writes a `STOP` placemark (`isPlacemark` true) even if density would have dropped the point. With smoothing on, that row uses the last Kalman output so the track end matches the smoothed line.

**Map draw.** `GtlViewModel` maps Room rows to `displayPoints`. `MapTrackVisibility` shows the line while logging, when **Show last logged route on map** is on, or when a Saved-tracks session is selected. If **Simplify track on map** is on and there are more than 4 points, Douglas–Peucker thins **only those display vertices** at the 1–20 m slider. SQLite, Route odometer, and KMZ never run through DP. With simplify **off** (runner default), every stored vertex is on the map — that is why a small on-road loop stays visible.

**Why the map looks like your log**

| Layer | What it does | Map effect |
|---|---|---|
| GNSS chip vs fused | Who answers “where am I?” | Runner: chip track, street-scale shape kept. Vehicles: fused, less Wi-Fi/cell jump. |
| Accuracy / sat gates | Drop junk before Kalman | No 200 m teleport spikes in the line. |
| Kalman (optional) | Move points, keep count | Vehicle roundabouts and cruise look smooth; standing lock stops a 10 m scribble. |
| Density | How many points are stored | Smart: fewer points at highway speed. Every good: ~2 Hz, tight loops keep vertices. |
| Room | Single source of truth | Map, Route, and KMZ are the same path. |
| Douglas–Peucker | Display-only thin | Long vehicle tracks stay cheap to draw; runner default is off so the map equals SQLite. |

Nothing is uploaded. `RemoteTrackSync` on stop is a no-op.

Pipeline mermaid (same flow, more boxes): [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md) / [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md).

### How Runner logs like a sports watch

A dedicated watch such as a Suunto Ambit 3 Peak records the **GNSS chip** about once per second. It does not snap the line to a street, and it does not run a phone “fused” filter. **GNSS** is the family of satellite systems (GPS, Galileo, GLONASS, BeiDou, QZSS, NavIC). GPS is one constellation; the chip uses all of them.

A phone **fused** location API answers a different question: “where is the user?” It mixes satellites with Wi-Fi, cell, and IMU, then smooths. A 5–10 m loop you actually ran on the road looks like pedestrian noise and is flattened **before** GTL would store it.

**Use GNSS only** asks Android `GPS_PROVIDER` (the chip, all constellations — the name is historical). Stored points are those chip positions, the same idea as the watch’s 1 s GPS track. (Suunto FusedSpeed is pace, not the polyline. This app does not implement FusedTrack IMU gap-fill.)

The rest of the Runner preset keeps that shape in SQLite, on Map, and in KMZ:

- **Smooth recorded track off** — no second constant-velocity Kalman that treats a small round as measurement noise. You can turn Kalman back on; Runner then adds extra process noise so a 5 m loop is not pulled onto the street.
- **Every good** — store about every 500 ms (settings min time), or sooner in a curve. If GPS bearing is 0 (common when jogging), heading can come from consecutive positions.
- **0.5 m** duplicate drop (vehicles stay at 1 m) so a tight loop keeps vertices.
- **Simplify track on map off** — the drawn line is every stored point.

Accuracy and satellite gates still drop bad fixes. This does **not** store fewer satellite fixes; it stores more of the good ones, and it stops using Wi-Fi/cell/fused guesses as the track source.

### Kalman filter (how stored points are smoothed)

**Purpose.** Cut GPS jitter on a driving or flying track (roundabouts look round, cruise is a clean line) without flattening a runner figure-8, and without a 10 m scribble while you stand still. Kalman is a **noise filter**: it **moves** accepted points and **keeps the same count**. It is not map-matching (no snap to OSM/Google roads) and not Douglas–Peucker (DP **drops** vertices, and only on the Map tab).

**Where it sits in the pipeline.** One `KalmanTrackFilter` per Start→Stop session, in `:engine`. `TrackingForegroundService` does this for every location update:

1. Copy the fix to the HUD (`lastLocation`). The purple accuracy circle always follows this **raw** point.
2. Drop the fix if accuracy is worse than the usage gate (30 m, runner 45 m) or satellites-in-fix is below 4. Rejected fixes never reach Kalman or SQLite.
3. If **Smooth recorded track** is on, run `KalmanTrackFilter.observe`. The filter outputs a new lat/lon. Timestamp, altitude, accuracy, and satellite count stay those of the GPS fix. Speed and bearing come from the filter velocity when that speed is at least 0.3 m/s. Runner/pedestrian adds extra position process noise so a 5 m loop is not pulled onto the chord.
4. **Recording density** (`FixAcceptance`) decides whether to **write** that (possibly smoothed) point. If the gap is too small, the Kalman state is still updated, but Room does not get a row.
5. On Stop, the last Kalman output is stored as the STOP point when smoothing is on.

So Kalman changes **where** stored points sit. Density changes **how many** of them are stored. Map simplify changes **neither** — it only thins the polyline drawn on Map.

**How the filter works.** Constant-velocity model in local metres (`GeoProjection`, same `111_320` m/deg as DP). State is `[east, north, vEast, vNorth]`. The GPS measurement is **position only** (no speed/heading update). Each step:

1. **Predict** — move the state forward by `dt` (clamped to a small range so a pause in GNSS does not explode the covariance).
2. **Process noise `q`** (m²/s⁴) = `baseQ(usage) × strengthMultiplier(slider)`. Then `× turnBoost(usage)` when the heading vs the previous **output** bearing (or heading from consecutive positions if GPS bearing is 0) changes more than 15°. **High `q` = trust GPS more = less smoothing.** Low `q` = trust the motion model more = smoother arcs, more lag when you actually turn.
3. **Update** — Joseph-form Kalman update with measurement σ = max(GPS accuracy, 2 m).
4. **Jump** — if the innovation is larger than `max(50 m, 8 × accuracy)` (tunnel exit, GPS teleport), re-initialize at the new fix. The gap is **not** interpolated.
5. **Stationary lock** (if enabled) — when GPS speed or predicted speed is below the usage pause threshold (0.25 m/s runner, 0.4 m/s vehicles) and displacement is under 1.5 m, freeze the last output, zero velocity, shrink position covariance.

Base `q` at mid slider (old Medium): runner 8.0, motorbike 2.5, car/watercraft 1.5, aircraft 0.8. Turn boost: runner 10, motorbike 5, car/water 3, aircraft 2. Strength slider `t` in `[0, 1]` (Low→High) multiplies `q` by `4^(1 − 2t)`: Low ×4, mid ×1, High ×0.25.

**Not implemented (on purpose).** OSM/Google snap-to-road, RTS forward–backward smoother, IMU dead reckoning / Suunto FusedTrack gap-fill, display splines.

### Effect of settings on the tracklog

These are the controls that change SQLite `gps_events`, Route odometer / speeds, and the shared KMZ. Everything else is display-only.

| Setting | Written into the tracklog? | Effect |
|---|---|---|
| **Use GNSS only** | Yes (source) | **On:** `GPS_PROVIDER` satellite-chip positions (all GNSS constellations). **Off:** Play Services fused HIGH_ACCURACY. If the GPS provider is disabled, fused is used either way. |
| **Smooth recorded track** | Yes | **On:** each candidate point is Kalman-smoothed before density. Route, Map (raw polyline), and KMZ all show the smoothed path. **Off:** the location source is stored as-is after the accuracy/sats gate (runner default). |
| **Smoothing strength** (Low–High slider; only if smoothing is on) | Yes | **Low:** GPS jitter stays, figure-8 and zigzag remain. **High** (aircraft default): roundabouts and cruise are cleaner; hairpins lag a little. Mid is motorbike/car/water. |
| **Hold still when stopped** | Yes (only if smoothing is on) | Below pause speed the stored coordinate does not wander. A 6 m GPS cluster at a red light collapses toward one point. Does not drop rows by itself — density still decides writes. |
| **Recording density** (Smart–Every good slider) | Yes | **Smart:** write when distance from the last **stored** point reaches the 2014 speed band (half in a curve; runner Smart half again, min 1 m). Highway stores fewer points; walking stores more. **Every good:** write about once per `minTime` (500 ms) or sooner in a curve. Vehicles drop stacks closer than **1 m**; runner closer than **0.5 m**. Positions between the ends mix the two rules. |
| **Simplify track on map** (1–20 m slider) | **No** | Fewer vertices on the Map tab only. Stored points, odometer, and KMZ are unchanged. |
| **Show accuracy marker** | **No** | Purple circle on the **raw** GPS fix, even when Kalman is on. |
| **Units** | Labels only | Metric / Imperial / ICAO format Route and KMZ balloons. Coordinates stay WGS-84. Aircraft and watercraft presets select ICAO. |
| Accuracy / satellite gates | Yes (rejection) | Fixes worse than 30 m (runner 45 m) or with fewer than 4 satellites in the fix are discarded before Kalman. Not shown as Settings sliders. |

**Practical result.** Motorbike default: fused + smoothed street track, Smart spacing, Map line thinned at 6 m. Runner default: GNSS chip, no Kalman, almost every good fix stored (0.5 m floor), Map shows every stored vertex so a small on-road loop stays visible. Aircraft default: stronger smoothing, Smart spacing, 15 m Map thinning, speed/distance in knots and nautical miles.

### Recording density

Separate from Kalman. Kalman always sees every accuracy-passed fix while smoothing is on; density only gates **storage**.

- **Smart** (vehicles): write a point when haversine distance from the last **stored** fix reaches the 2014 speed band; half that in a curve. Runner Smart uses half of that band again (walk/jog was too coarse for a small figure-8).
- **Every good** (runner default): accept when `minTimeMillis` has elapsed or heading is in a curve. Vehicles drop stacks closer than 1 m; runner drops closer than 0.5 m.
- **Between the slider ends:** required distance is a mix of the Smart band and the Every-good floor (1 m vehicles, 0.5 m runner); the min-time / curve path can also accept a point.

### Douglas–Peucker (map simplify)

**Purpose.** Reduce how many vertices the Map tab has to draw. A long session can have thousands of stored fixes; most of them sit almost on a straight line. Dropping those intermediates keeps the map responsive without changing what was recorded.

This is **display-only**. `gps_events`, Route odometer / speeds, and KMZ export always use the Room rows (already Kalman-smoothed when that setting is on). Douglas–Peucker does not smooth GPS noise: remaining corners stay sharp. It only discards points that are close enough to a chord.

**When it runs.** Settings → **Simplify track on map** (`optimizationActive`; on for vehicles, off for runner). `GtlViewModel` calls `DouglasPeucker.simplify(points, clampTolerance(optimizationTolerance))` when that switch is on and `points.size > 4`. Tolerance is a **1–20 m** slider (1 m steps; motorbike default 6 m, car 8 m). `DouglasPeucker.clampTolerance` still snaps and clamps on write.

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

| Document | What it is |
|---|---|
| [CHANGELOGS.md](CHANGELOGS.md) | Version history (2.0.0 rewrite through 2.0.3) |
| [docs/CHANGELOG-2026-09-08.md](docs/CHANGELOG-2026-09-08.md) | 2.0.3 release notes (English and Hungarian) |
| [docs/play-console/whatsnew.txt](docs/play-console/whatsnew.txt) | Play Console release name and EN/HU what’s-new text |
| [docs/RENEWAL-REPORT.md](docs/RENEWAL-REPORT.md) | Rewrite report: what was rebuilt, what was dropped for Play policy, follow-ups |
| [docs/play-console/privacy-policy.html](docs/play-console/privacy-policy.html) | Privacy policy (local copy of the live KLHome page) |
| [docs/play-console/feature-graphic.png](docs/play-console/feature-graphic.png) | Play Store feature graphic |
| [docs/screenshots/](docs/screenshots/) | Play listing screenshots (GPS, route, map/tracking, compass, settings, help, about, Google Earth KMZ) |
| [docs/DBSTRUCT-en.md](docs/DBSTRUCT-en.md) | SQLite schema (`gtl.db`) mermaid |
| [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md) | GPS listen → filter → Room → UI / KMZ (EN); mermaid of the logging pipeline |
| [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md) | GPS figyelés → szűrés → Room → UI / KMZ (HU) |
| [docs/dp-kalman-smoothing-en.md](docs/dp-kalman-smoothing-en.md) | Original Kalman implementation brief; **as-built notes at the top** (current behaviour is this README) |

Engine entry points worth reading:

- `engine/.../FixAcceptance.kt` — accuracy / sats / Smart or Every-fix density
- `engine/.../KalmanTrackFilter.kt` — constant-velocity smoother (stored points)
- `engine/.../UsageSmoothingDefaults.kt` — usage preset (GNSS only, Kalman, density, map simplify)
- `engine/.../SpeedAdaptiveSpacing.kt` — metres between points by km/h and curves
- `engine/.../DouglasPeucker.kt` — map-only polyline simplify (metres, local projection)
- `engine/.../TrackStats.kt` — odometer, moving vs waiting
- `engine/.../KmlExporter.kt` + `KmzExporter.kt` — KMZ with local icons
- `engine/.../Gnss.kt` — constellation / L1 vs L5 / SNR
- `engine/.../MapTrackVisibility.kt` — when the map must draw a track
- `app/.../LocationClient.kt` — fused HIGH_ACCURACY or `GPS_PROVIDER` when Use GNSS only is on

---

## Play listing screenshots

`docs/screenshots/`

- `gps-idle.png`, `gps-logging.png` — GPS tab
- `route.png` — Route totals
- `map.png`, `tracking.png` — Map while recording
- `googleearth.png` — shared KMZ in Google Earth
- `compass.png`, `about.png`
- `settings.png` — Settings with simplify / smoothing sliders (2.0.3)
- `settings-density.png` — Settings with recording density at Every good (2.0.3)
- `help.png` — Help topics (2.0.3)
- `app-icon.png`

Phone listing size: 1080×1920, 24-bit PNG, no alpha (Play 9:16). Upload `settings.png`, `settings-density.png`, and `help.png` with this release.

---

## Next to do

- Implement GPX export. GPX (GPS Exchange Format) is the most common GPS tracklog interchange format.
- Store both barometric and GPS altitude on each tracklog point when usage is aircraft and the measurement system is ICAO.
- Add Internationalization
- Add light and dark themes
- Add diration in mins to the end of the route stop details when the user after finish logging press to stop icon.

---

## Not in this app

Intentionally not ported from 2014 (policy or dead APIs): IMEI / `READ_PHONE_STATE`, live lat/lng upload, follow-me web page, remote unlock, Google Directions, app-driven GPS/Wi-Fi toggles, boot auto-start. See [docs/RENEWAL-REPORT.md](docs/RENEWAL-REPORT.md).
