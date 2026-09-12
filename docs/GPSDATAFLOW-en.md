# GPS event listening and data flow

How a location update becomes a SQLite row, then the map, Route HUD, and KMZ. The Map polyline is those Room coordinates — there is no second sketch. Why that line looks like the path you took: [README-en.md — How logging works](../README-en.md#how-logging-works).

```mermaid
flowchart TD
    User["User taps Start"] --> VM["GtlViewModel.startLogging"]
    VM --> FGS["TrackingForegroundService<br/>foreground type location"]
    FGS --> Sess["TrackRepository<br/>open or start session"]
    Sess --> DBSess[("gtl.db<br/>track_sessions")]

    FGS --> Loc["LocationClient<br/>fused HIGH_ACCURACY<br/>or GPS_PROVIDER if GNSS only"]
    FGS --> Gnss["GnssStatusSource"]
    FGS --> Temp["AmbientTemperatureSource"]
    FGS --> Acc["AccelerometerSource"]
    FGS --> Grav["GravitySource"]
    FGS --> Press["PressureSource"]
    FGS --> Comp["CompassSource"]

    Loc --> LiveLoc["LiveTrackingState.lastLocation"]
    Gnss --> LiveGnss["LiveTrackingState.gnss"]
    Temp --> LiveTemp["temperatureCelsius"]
    Acc --> LiveAcc["accel"]
    Grav --> LiveLean["leanAngle"]
    Press --> LiveBaro["baroAltitude pressureHpa"]
    Comp --> LiveAz["azimuthDegrees HUD only"]

    LiveLoc --> Cloud["FixCloudBuffer memory"]
    Cloud --> CloudMap["Map magenta dots + CEP95"]
    Cloud --> CloudGps["GPS tab n RMS CEP95"]

    LiveLoc --> Fix["TrackFix<br/>lat lon alt speed bearing accuracy sats"]
    LiveGnss --> Fix
    Fix --> AccGate{"accuracy / sats"}
    AccGate -->|"too poor or too few sats"| Drop["Discard fix"]
    AccGate -->|ok| Smooth{"Smooth recorded track?"}
    Smooth -->|yes| KF["KalmanTrackFilter.observe"]
    Smooth -->|no| RawFix["TrackFix as-is"]
    KF --> Gate{"FixAcceptance.shouldAccept"}
    RawFix --> Gate

    Gate -->|"first fix of session"| KindStart["eventKind START"]
    Gate -->|"SMART: distance >= spacing<br/>EVERY_FIX: min time or curve"| KindMove{"speed vs pause threshold"}
    Gate -->|"too close / too soon"| Drop

    KindMove -->|"below pause speed"| KindPause["PAUSE"]
    KindMove -->|"moving"| KindGo["MOVE"]

    KindStart --> Row["GpsEventEntity<br/>+ live temp, accel, lean, usageType, baro"]
    KindPause --> Row
    KindGo --> Row

    UserStop["User taps Stop"] --> StopRow["GpsEventEntity STOP<br/>isPlacemark true"]
    StopRow --> Close["stopSession<br/>set stoppedAt"]

    Row --> DBevt[("gtl.db<br/>gps_events")]
    Close --> DBevt
    Close --> DBSess

    DBevt --> Observe["observeEvents Flow"]
    Observe --> VM2["GtlViewModel"]
    VM2 --> Stats["TrackStatsCalculator"]
    VM2 --> Display{"Simplify track on map<br/>and more than 4 points?"}
    Display -->|"yes"| Simpl["Douglas-Peucker<br/>usage-scaled metres<br/>map only"]
    Display -->|"no"| RawPts["Room points as-is"]
    Simpl --> Vis{"MapTrackVisibility"}
    RawPts --> Vis
    Vis --> Map["Map tab polyline<br/>Google Maps or OSM"]
    Stats --> Route["Route tab + elevation profile"]
    Observe --> GPSUI["GPS tab live fields"]
    DBevt --> KMZ["KmlExportUseCase / KMZ share"]
    DBevt --> GPX["GpxExportUseCase / GPX share"]
```

## Listeners (in the foreground service)

| Source | What it feeds |
|---|---|
| `LocationClient` | Fused `PRIORITY_HIGH_ACCURACY`, or `GPS_PROVIDER` when **Use GNSS only** is on. Interval from settings, at least 500 ms. Min distance on the request is `0`; spacing is applied later. If the GPS provider is disabled, fused is used. While the app is open and not logging, `GtlViewModel` also listens about once a second for the GPS/Map HUD. After Start, only the foreground service writes SQLite. |
| `GnssStatusSource` | Satellite counts and SNR for the GPS tab; `satellitesInFix` on each stored row. |
| `AmbientTemperatureSource` | Optional; copied onto the row if the sensor exists. |
| `AccelerometerSource` | Optional; last XYZ on the row. |
| `GravitySource` | Optional; `TYPE_GRAVITY` (else accelerometer) → lean angle on the row and Route HUD. |
| `PressureSource` | Optional; `TYPE_PRESSURE` → `pressureHpa` and ISA `baroAltitude` on the row when the sensor exists. |
| `CompassSource` | Compass tab only; **not** written to SQLite. MAG is the rotation-vector heading. TRUE adds `GeomagneticField.declination` from the last GPS fix. |

All of this runs under a **visible** location foreground notification. There is no `ACCESS_BACKGROUND_LOCATION`.

## Gate: `FixAcceptance`

A poor-accuracy or low-satellite fix never enters the Kalman filter. The HUD `lastLocation` still updates.

A remaining fix is stored only if:

1. `accuracy` ≤ settings minimum accuracy (m) and `satellitesInFix` ≥ settings minimum (default 4) — already checked before Kalman.
2. It is the first point of the session, **or**
   - **Smart density:** haversine distance from the last **stored** point is at least `SpeedAdaptiveSpacing` (speed bands; half spacing if heading change &gt; 15°; runner SMART uses half of that band).
   - **Every good fix:** elapsed time ≥ `minTimeMillis` **or** heading curve, and distance ≥ 1 m (vehicles) or 0.5 m (runner and bicycle). If GPS bearing is 0, heading can come from consecutive positions.

Optional **KalmanTrackFilter** (constant-velocity, position-only) runs after the accuracy/sat gate and before spacing. Output lat/lon is the filter state; timestamp, altitude, accuracy, and sats stay with the GPS fix. Speed and bearing come from filter velocity when that speed is at least 0.3 m/s. Measurement σ = max(GPS accuracy, 2 m). Pedestrian usages add extra position process noise so a 5 m loop is not pulled onto the street. Stationary lock holds the stored point when slower than the usage pause speed. Jump (innovation larger than `max(50 m, 8 × accuracy)`) re-initializes; it does not interpolate across a gap.

Rejected updates still refresh `lastLocation` for the GPS/Map HUD.

**Show fix cloud** (off by default) samples that same raw `lastLocation` into an in-memory `FixCloudBuffer` (max 120 points or 120 s). It is not `gps_events`, not Kalman, and not Douglas–Peucker. Map pastel magenta dots and GPS-tab n / RMS / CEP95 read only this buffer. Turning the switch on also turns on Show accuracy marker; turning it off only hides the cloud.

## `eventKind`

| Kind | When |
|---|---|
| `START` | First accepted fix (or resume with no prior point). |
| `MOVE` | Accepted and speed ≥ usage pause threshold. |
| `PAUSE` | Accepted and speed below pause threshold (default 0.4 m/s; 0.25 m/s for runner and bicycle). |
| `STOP` | User Stop; last location written even if the gate would drop it. |

`isPlacemark` is true for START / PAUSE / STOP (KMZ play / pause / stop icons).

## After SQLite

`GtlViewModel` observes `gps_events` for the live session, a Saved-tracks selection, or the last session if **Show last logged route on map** is on. The Map polyline **is** those Room coordinates. There is no in-memory sketch, so what you see on Map is the log that was stored (optionally thinned by Douglas–Peucker for drawing only).

- **Route** totals from `TrackStatsCalculator` whenever the session has events (logging, last-track, selected session). Elevation profile from Room GPS `altitude` (and baro when present).
- **Map** polyline from Room; optional Douglas–Peucker (below); hidden unless logging, last-track, or a selected session (`MapTrackVisibility`). The Map broom (idle, saved track shown) sets `mapCleared` so the line hides without deleting the log; Start or Show on map draws again. Logging always draws. Compose **HUD** over both map engines: raw speed/accuracy, GNSS used/in view; while logging: trip, elapsed, REC.
- **Share** builds KMZ (`gx:Track` + balloons) or GPX 1.1 (`trk` / `trkpt` / Start-Pause-Stop `wpt`) from the same Room rows. Neither is simplified. The KMZ line and icons use `clampToGround`. The visible line is a tessellated `LineString` at height 0 (Earth Android does not drape `gx:Track` GPS altitude). A trailing STOP marker is snapped to the last path vertex (last accepted log point), so Stop is not a HUD hook off the line. Pause icons that overlap Start/Stop are omitted. Balloons are HTML. All three: UTC `YYYY:MM:DD HH:MM:SS`, `temp=` (session unit or `N/A`), `lon=`, `lat=`, `Altitude:` (GPS, session unit), `Baro:` (ISA, or `N/A`). Pause adds `Speed:`, `duration=` (from Start), and `distance=` so far. Stop is named **Stop** and adds `Avg. Speed:` and `Max speed:` from `TrackStatsCalculator` on the path (not the STOP row’s `speed`), plus session `duration=` and `distance=`. KMZ ExtendedData `baro` is ISA metres and `alt` is GPS metres; `gx:coord` height is 0. GPX `ele` is GPS altitude; baro stays in SQLite and KMZ ExtendedData / balloons. No `usage=` or `lean=` in the balloon.

Nothing is uploaded. `RemoteTrackSync` on stop is a no-op.

## Why the map looks like the stored log

| Layer | Job |
|---|---|
| GNSS chip vs fused | Runner and bicycle default uses `GPS_PROVIDER` so a street-scale loop is not flattened by fused Wi-Fi/cell. Vehicles stay fused. |
| Accuracy / sats | Poor fixes never enter Kalman or Room. |
| Kalman (optional) | Moves stored lat/lon; vehicles on, runner and bicycle off. HUD pale purple circle stays on the raw fix. |
| Fix cloud | Memory-only raw dots + CEP95 while standing. Not Room. Turning it on enables the accuracy marker. |
| Density | Smart (speed bands) or Every good (~500 ms, 0.5 m floor for runner and bicycle). |
| Room | Single source for Map, Route, KMZ, GPX. |
| Douglas–Peucker | Display-only. Runner and bicycle default is off, so every stored vertex is drawn. |

Full prose: [README-en.md — How logging works](../README-en.md#how-logging-works).

## Map polyline: Douglas–Peucker

**Purpose.** Fewer vertices on the Map tab so a long track stays cheap to draw. SQLite, Route stats, KMZ, and GPX keep every stored point.

**When.** Settings **Simplify track on map** (on by default for vehicles, off for runner and bicycle) and more than 4 points. Tolerance is a **1–20 m** slider (1 m steps), chosen by usage (motorbike 6 m, car 8 m, aircraft 15 m, bicycle 3 m and runner 2 m if turned on). **Show on map** copies the session usage into Settings first, then the drawn line follows the current Settings sliders. Hidden when the switch is off; the stored value is kept. `GtlViewModel` → `DouglasPeucker.clampTolerance` → `simplify`. Kalman, not Douglas–Peucker, is the noise filter.

**How.** Keep the segment’s first and last points. Find the intermediate point with the largest perpendicular distance (metres, local `111_320` m/deg projection) to the chord between them. If that distance is above the tolerance, keep the point and recurse on both sides; otherwise drop every intermediate point.

This discards near-colinear jitter. It does not smooth GPS noise — leftover corners stay sharp. Full write-up: [README-en.md](../README-en.md#douglaspeucker-map-simplify).
