# Agent prompt: Kalman track smoothing for GTL

**As built (current).** This file was the implementation brief. Do not re-implement it as written. Current behaviour is [README-en.md — How logging works](../README-en.md#how-logging-works) and [GPSDATAFLOW-en.md](GPSDATAFLOW-en.md). Differences from the original goal table:

| Brief said | Shipped |
|---|---|
| Runner: Kalman on, LOW, EVERY_FIX, DP off | Runner: **Kalman off**, GNSS only on, EVERY_FIX, 0.5 m duplicate floor, DP off. Bicycle: same idea, DP 3 m if turned on |
| Location source always fused HIGH_ACCURACY | **Use GNSS only** → `GPS_PROVIDER`; fused fallback if that provider is disabled |
| Strength / density as named chips | Continuous sliders (`smoothingStrengthValue`, `recordingDensityValue` in `[0, 1]`) |
| Measurement σ = max(accuracy, 2.0) | Same (2 m, not 3 m) |
| Position-only Kalman | Same; speed/bearing from filter velocity when ≥ 0.3 m/s |
| Pedestrian extra process noise | Yes, when Kalman is on for runner / bicycle / walk / hike |
| Heading from GPS bearing only | Curve detection can use heading from consecutive positions if bearing is 0 |
| `WALKING_HIKE` / `PEDESTRIAN` in Settings | Engine enums exist and share runner defaults; Settings `selectable` is the six usages including bicycle |
| Settings cramped → keep read-only filter row | Read-only Fix filters row removed; gates still run |

The sections below are the original brief (why DP 19.5 m looked wrong, algorithm, tests). Treat them as history.

---

Use this file as the full implementation brief. Paste it, `@`-mention it, or start a session with:

> Implement `docs/dp-kalman-smoothing-en.md`. Follow every constraint. Do not invent extra features.

Works for Cursor Agent and Claude Code. Read the listed source files before editing. Prefer small, tested engine changes over a settings redesign.

---

## Role

You are implementing GPS track processing in the GTL Android app (Kotlin, Jetpack Compose, Room, DataStore). You write production code and JUnit tests. You do not add drive-by refactors, new comments, TypeScript/Kotlin type assertions (`as`, `!!` on converted types, `unchecked`), or map-matching.

---

## Goal

Make recorded and drawn tracks look like a dedicated sports watch:

- **Driving / riding:** smooth arcs (roundabouts, ramps), not a jittery polyline and not a coarse Douglas–Peucker polygon.
- **Running:** keep a zigzag and a figure-8. Do not flatten them into a straight line.
- **Standing:** the point must not wander (Garmin-style static lock).

Douglas–Peucker stays **map display only**. It is not the smoother. The smoother is a **constant-velocity Kalman filter** in `:engine`, applied **before** `FixAcceptance` so SQLite, Route stats, the map, and KMZ all see the same path.

---

## Non-goals (do not implement)

- OSM / Google map-matching (snap to roads).
- Chaikin, Catmull–Rom, or other display splines.
- RTS (forward–backward) smoother.
- Dead reckoning / Suunto FusedTrack IMU fill-in of GPS gaps.
- New Room columns or schema version bump.
- Changing fused-location min distance (already `0f` in `TrackingForegroundService`).
- Uploading tracks.
- Comments in source.

---

## Project constraints (mandatory)

- **No comments** in source (Kotlin, XML resource files may get new strings; do not comment Kotlin).
- **No type assertions.**
- Algorithms live in **`:engine`** (pure JVM, no Android SDK). Android wiring stays in `:app`.
- Tests: JUnit 4 in `engine/src/test/kotlin/.../EngineTest.kt` (append new classes; match existing style).
- UI strings: **English and Hungarian** (`values/strings.xml` and `values-hu/strings.xml`).
- Settings UI: reuse `SettingSwitch`, `FilterChip`, `RadioButton` / selectable rows. Do not introduce a new settings library or a `Slider` unless the existing chips cannot express a value.
- When usage type changes, apply **all** usage-linked defaults in one DataStore edit (same pattern as `GtlPreferences.setUsageType` already writing `FixFilter` fields).
- Keep `FixAcceptance` accuracy / satellite gates. Do not remove speed-adaptive spacing; gate it behind the new recording-density setting.

---

## Current system (read these first)

| Path | What it does today |
|---|---|
| `docs/GPSDATAFLOW-en.md` / `docs/GPSDATAFLOW-hu.md` | Pipeline: fused location → `FixAcceptance` → Room → DP on map |
| `engine/.../FixAcceptance.kt` | Drop if accuracy or sat count fail; else require `SpeedAdaptiveSpacing` distance |
| `engine/.../SpeedAdaptiveSpacing.kt` | Speed bands from the 2014 logger; half spacing if heading change > 15° |
| `engine/.../DouglasPeucker.kt` | Polyline simplification in metres (local projection) |
| `engine/.../UsageType.kt` | `AIRCRAFT`, `WATERCRAFT`, `FOUR_WHEELERS`, `TWO_WHEELERS`, `BICYCLE`, `RUNNER`; runner/bicycle pause 0.25 m/s, others 0.4 m/s; runner/bicycle accuracy 45 m vs 30 m |
| `app/.../TrackingForegroundService.kt` | `LocationClient.locations(minTime, 0f)` then `FixAcceptance` then insert `GpsEventEntity` |
| `app/.../GtlViewModel.kt` | If `optimizationActive && points.size > 4` → `DouglasPeucker.simplify(points, optimizationTolerance)` for **map only**. Stats and KMZ use raw Room rows |
| `app/.../GtlPreferences.kt` | DataStore. DP default: `optimizationActive = true`, `optimizationTolerance = 19.5`. **No setter and no Settings control for tolerance** |
| `app/.../ui/screens/SecondaryScreens.kt` | Settings: usage, units, OSM, simplify-on-map switch, last-track, accuracy marker, fix cloud, read-only filter numbers |
| `app/.../data/sensor/SensorSources.kt` | Accelerometer stored on rows; compass is HUD-only |

**Why the map looks wrong today**

1. DP with **19.5 m** (on by default) eats runner figure-8s and turns highway curves into polygons.
2. DP does not filter GPS noise; it **drops points**. Remaining corners stay sharp.
3. Runner spacing is 4 m (walk) / 10 m (jog). A small figure-8 can fail the distance gate before DP even runs.
4. There is no process model, so standing GPS wander becomes a scribble, then DP may keep the scribble’s farthest spikes.

**Industry split (do not conflate)**

| Layer | Job | GTL today | Target |
|---|---|---|---|
| Noise filter | Move points, keep count | None (fused location only) | Kalman + stationary lock |
| Sampling | Drop redundant points | `SpeedAdaptiveSpacing` | Same, plus `EVERY_FIX` for runner |
| Display simplify | Fewer vertices on the map | DP 19.5 m, always-on default | DP optional, usage-scaled metres |
| Stats / KMZ | Truth for distance | Room rows | Room rows **after** Kalman, **never** after DP |

Garmin analogue: chip Kalman + static-mode filter + Smart vs Every Second recording. Suunto Ambit 3: 1 s GPS; FusedSpeed is **pace**, not the polyline. Do not copy FusedTrack (Suunto 9 IMU gap-fill).

---

## Target pipeline

```
Fused Location (HIGH_ACCURACY, minTime ≥ 500 ms, minDistance 0)
    → TrackFix (lat, lon, alt, speed, bearing, accuracy, sats)
    → KalmanTrackFilter.observe(fix, usage, settings)   // NEW, optional
    → FixAcceptance.shouldAccept(previousStored, filteredFix, filter, density)  // density NEW
    → GpsEventEntity in Room
    → GtlViewModel
         → TrackStatsCalculator on Room samples          // unchanged source
         → DouglasPeucker on map iff optimizationActive  // retuned defaults
         → KMZ from Room                                 // unchanged source
```

HUD `lastLocation` may stay the **unfiltered** fused fix so the pale purple accuracy circle and live lat/lon match the phone GNSS. **Show fix cloud** also samples that raw `lastLocation` (memory only); turning it on also enables the accuracy marker. Stored / drawn track uses the Kalman output when smoothing is on.

Reset the Kalman filter when a new session starts (`lastAccepted == null` after `startSession`). If the service resumes an open session, seed the filter from `latestEvent` (position + speed/bearing) so the first new fix is not a jump.

---

## Algorithm: `KalmanTrackFilter` (engine)

Pure Kotlin class (instance per recording session, **not** a global `object` with leftover state).

### State

Local ENU metres from the first accepted observation of the session:

`[east, north, vEast, vNorth]`

Reuse the same metre projection already in `DouglasPeucker` (`111_320` m/deg lat, `cos(lat)` for lng). Put shared projection in a small `GeoProjection` helper if both call sites would otherwise duplicate it. If you extract it, update `DouglasPeucker` to use it. No behavior change for DP tests.

### Model

Constant-velocity, discrete time `dt = (t_k - t_{k-1}) / 1000` seconds, clamp `dt` to `(0.05, 5.0)`.

```
F = [[1, 0, dt, 0],
     [0, 1, 0, dt],
     [0, 0, 1,  0],
     [0, 0, 0,  1]]
```

Process noise: white-noise acceleration `q` (m²/s⁴):

```
dt2 = dt*dt; dt3 = dt2*dt; dt4 = dt2*dt2
Q = q * [[dt4/4, 0, dt3/2, 0],
         [0, dt4/4, 0, dt3/2],
         [dt3/2, 0, dt2, 0],
         [0, dt3/2, 0, dt2]]
```

Measurement: position only.

`z = [east_meas, north_meas]`  
`H` extracts east/north from the state.  
`R = diag(σ², σ²)` with `σ = max(accuracyMeters, 2.0)`.

Optional velocity measurement from GPS speed + bearing **only if** `speedMps >= pauseSpeed` and `bearing != 0`. If used, add two rows with `R_vel = max(1.0, 0.25 * speedMps)²`. If this complicates the first version, ship **position-only** Kalman; tests must still pass the scenarios below.

### Predict / update

Standard Kalman: `x = F x`, `P = F P Fᵀ + Q`, then Joseph-form or classic update. Guard against non-finite lat/lon; on NaN/Inf return the previous state converted back to lat/lon (or the raw fix if there is no previous).

First fix: initialize `x = [0,0, vE, vN]` from speed/bearing (0 if unknown), `P` diagonal `[σ², σ², 25, 25]`.

Output: convert east/north back to lat/lon. Keep `altitude`, `accuracyMeters`, `satellitesInFix`, `timestampMillis` from the input fix. Set `speedMps` / `bearing` from the velocity state when speed ≥ 0.3 m/s; otherwise keep input speed/bearing (or 0 / last bearing). Do not invent a new `TrackFix` field.

### Stationary lock (required)

If `speedMps < usage.pauseSpeedMps()` **or** predicted speed `< pauseSpeed` and displacement since last output `< 1.5 m`:

- Hold **position** at the last output (or last stored) lat/lon.
- Zero velocity in the state.
- Shrink `P` position terms so the next wander does not immediately pull the point.

This is the Garmin static-mode equivalent. Tests: a cluster of 8 m random GPS noise around a stop must collapse to ≈ one point (within 2 m), not a star.

### Adaptive `q` (required — this is how figure-8 survives)

Base `q` from usage × smoothing strength (tables below).

If heading change vs previous **output** bearing `> 15°` (same wrap logic as `SpeedAdaptiveSpacing.isInCurve`), multiply `q` by `turnBoost`:

| Usage | turnBoost |
|---|---|
| RUNNER | 10 |
| TWO_WHEELERS | 5 |
| FOUR_WHEELERS / WATERCRAFT | 3 |
| AIRCRAFT | 2 |

If `AccelerometerSource` values are present on the live state, you **may** multiply `q` by up to 2 when horizontal accel magnitude (excluding gravity) exceeds 1.5 m/s². Do not require a gyroscope. Compass must not drive the Kalman in v1 (magnetic interference). If accel is missing, heading-only boost is enough.

High `q` = trust GPS = **less** smoothing = zigzag kept.  
Low `q` = trust the CV model = **more** smoothing = pretty car arcs, lag on hairpins.

### Jump handling

If the innovation (measurement minus predicted position) exceeds `max(50 m, 8 * accuracyMeters)`:

- Do **not** blend. Re-initialize at the new fix (tunnel exit / first fix after a drop).
- Do not store the jump as a smoothed interpolation across the gap.

### Outlier vs Kalman order

1. Accuracy / satellite gates stay in `FixAcceptance` and still run on the **incoming** (pre-Kalman or post-Kalman) fix — pick **post-Kalman** for the distance gate (spacing uses the smoothed point), and **pre-Kalman** for accuracy/sats so a 200 m-accuracy blip never enters the filter. Implement as: if accuracy/sats fail, skip Kalman and skip store; HUD can still show the raw location.
2. Then Kalman.
3. Then spacing / `EVERY_FIX`.

---

## Recording density

Add `enum class RecordingDensity { SMART, EVERY_FIX }` in `:engine`.

- **SMART** (current): `FixAcceptance` requires `distance >= SpeedAdaptiveSpacing.spacingMeters(speed, inCurve)`.
- **EVERY_FIX**: if accuracy and sats pass and this is not the first point, accept when `current.timestamp - previous.timestamp >= filter.minTimeMillis` **or** heading curve (so 1 Hz-ish logging, Garmin Every Second analogue). Still drop duplicates closer than **1.0 m** to avoid stacking on a stop (stationary lock should already pin them).

Change `FixAcceptance.shouldAccept` to take density (or a boolean `ignoreSpeedSpacing`). Keep the old 4-arg tests working by defaulting to SMART in the test helper.

**Runner spacing tweak (SMART only):** for `UsageType.RUNNER`, use half of the existing band (and still half again in curve, min 1 m). Jogging 10 m spacing is too coarse for a figure-8. Vehicles keep current bands.

Pass `usage` into spacing only if needed; do not break existing `SpeedAdaptiveSpacingTest` numbers for non-runner speeds. Add runner-specific tests instead of rewriting the 2014 band table.

---

## Settings: existing vs new

### Keep as-is

| Setting | Default | Notes |
|---|---|---|
| Usage | `TWO_WHEELERS` | Selecting a usage **must** now also write smoothing + density + DP defaults |
| Units | `METRIC` | Unrelated |
| Use downloaded OSM map | false | Unrelated |
| Show last logged route | true | Unrelated |
| Show accuracy marker | true | Pale purple claimed-accuracy circle on the raw HUD fix |
| Show fix cloud | false | HUD raw samples, not Kalman. Turning it on also enables the accuracy marker |
| Min distance / time / accuracy / sats | from `UsageType.defaultFilter()` | Still shown read-only unless you already had editors |

### Change existing DP defaults

| Setting | Old | New global fallback | Notes |
|---|---|---|---|
| `optimizationActive` | `true` | `true` for vehicles, `false` for `RUNNER` | Label stays “Simplify track on map” |
| `optimizationTolerance` | `19.5` | usage table below | **Add a Settings control**; add `setOptimizationTolerance` |

**One-time migration:** if DataStore has `tolerance == 19.5f` (the old hardcoded default) **and** the new Kalman keys are absent, replace tolerance and `optimizationActive` with the usage table when first mapping settings. Do not overwrite a user who already changed usage after install if they set a custom tolerance — only migrate the sentinel `19.5`.

### New settings

| Key (DataStore) | Type | Purpose |
|---|---|---|
| `track_smoothing` | Boolean | Master Kalman switch |
| `smoothing_strength` | String enum `LOW` / `MEDIUM` / `HIGH` | Scales `q` |
| `stationary_lock` | Boolean | Wander freeze when slower than pause speed |
| `recording_density` | String enum `SMART` / `EVERY_FIX` | Spacing vs every good fix |

Suggested Kotlin names on `GtlSettings`:

```kotlin
val trackSmoothingEnabled: Boolean
val smoothingStrength: SmoothingStrength
val stationaryLockEnabled: Boolean
val recordingDensity: RecordingDensity
```

`SmoothingStrength` and `RecordingDensity` live in `:engine`.

### Defaults by usage (apply in `setUsageType` and on migration)

`q` is process-noise acceleration squared, m²/s⁴, **before** `turnBoost`. Strength multipliers: `LOW = 4× q` (less smooth), `MEDIUM = 1×`, `HIGH = 0.25×` (more smooth).

| Usage | Kalman on | Strength | Stationary lock | Density | DP on map | DP tolerance m | Rationale |
|---|---|---|---|---|---|---|---|
| `RUNNER` | true | LOW | true | EVERY_FIX | false | 2.0 | Keep zigzag / figure-8; 1 Hz store |
| `TWO_WHEELERS` | true | MEDIUM | true | SMART | true | 6.0 | Motorbike default; arcs without erasing hairpins |
| `FOUR_WHEELERS` | true | MEDIUM | true | SMART | true | 8.0 | Smooth ramps/roundabouts |
| `WATERCRAFT` | true | MEDIUM | true | SMART | true | 8.0 | Drift is real; do not over-smooth |
| `AIRCRAFT` | true | HIGH | true | SMART | true | 15.0 | High speed, large radius; DP may thin cruise |

Base `q` at MEDIUM strength:

| Usage | base `q` |
|---|---|
| RUNNER | 8.0 |
| TWO_WHEELERS | 2.5 |
| FOUR_WHEELERS | 1.5 |
| WATERCRAFT | 1.5 |
| AIRCRAFT | 0.8 |

Effective `q = baseQ * strengthMultiplier`, then `× turnBoost` on curves.

If Kalman is **off**, pipeline is today’s (plus density + runner SMART half-spacing). DP still applies on the map only.

### Settings UI copy

English / Hungarian (keep tone of existing strings: short, no marketing):

| EN | HU |
|---|---|
| Smooth recorded track | Rögzített útvonal simítása |
| Smoothing strength | Simítás erőssége |
| Low | Alacsony |
| Medium | Közepes |
| High | Magas |
| Hold still when stopped | Álláskor ne vándoroljon a pont |
| Recording density | Rögzítés sűrűsége |
| Smart (speed bands) | Okos (sávos) |
| Every good fix | Minden jó fix |
| Simplification tolerance | Egyszerűsítés tűrése |

Show strength chips only when Kalman is on. Show DP tolerance chips only when “Simplify track on map” is on.

DP tolerance chips (metres, write `optimizationTolerance`): `2`, `6`, `8`, `15`, `20`. Highlight the closest chip to the stored value.

Help map/GPS bodies: one sentence that Kalman smooths **stored** points; DP only thins the **drawn** line; runner default is every good fix and no DP.

Update `README-en.md`, `README-hu.md`, `docs/GPSDATAFLOW-en.md`, `docs/GPSDATAFLOW-hu.md` mermaid: Kalman box between `TrackFix` and `FixAcceptance`.

---

## Files to create / modify

**Create**

- `engine/src/main/kotlin/com/lkovari/mobile/apps/gtl/engine/KalmanTrackFilter.kt`
- `engine/src/main/kotlin/com/lkovari/mobile/apps/gtl/engine/SmoothingStrength.kt` (or combine small enums in one file if that matches repo taste — `UsageType.kt` is a single enum file; prefer one file per enum)
- `engine/src/main/kotlin/com/lkovari/mobile/apps/gtl/engine/RecordingDensity.kt`
- Optional: `engine/src/main/kotlin/com/lkovari/mobile/apps/gtl/engine/GeoProjection.kt`

**Modify**

- `engine/.../FixAcceptance.kt` — density; accuracy/sats still first
- `engine/.../SpeedAdaptiveSpacing.kt` — runner half-band **or** a `spacingMeters(speed, inCurve, usage)` overload; keep existing two-arg function for current tests
- `engine/.../UsageType.kt` — `defaultSmoothing()` helper returning a data class of the table above (Kalman, strength, lock, density, DP flag, DP metres). `setUsageType` in app calls this.
- `engine/src/test/kotlin/.../EngineTest.kt` — new test classes
- `app/.../GtlPreferences.kt` — keys, mapping, migration, setters
- `app/.../GtlViewModel.kt` — setters; DP still display-only
- `app/.../TrackingForegroundService.kt` — construct filter, observe, reset
- `app/.../ui/screens/SecondaryScreens.kt` — new controls
- `app/src/main/res/values/strings.xml` and `values-hu/strings.xml`
- `docs/GPSDATAFLOW-en.md`, `docs/GPSDATAFLOW-hu.md`, `README-en.md`, `README-hu.md`

Do not change KMZ exporters except if they would start reading display points (they must not).

---

## Wiring in `TrackingForegroundService`

Keep `collectLocation` as the single writer. Pseudocode (implement in real Kotlin, no comments):

```kotlin
val filter = KalmanTrackFilter()
// after loading lastAccepted from Room, filter.seedFrom(lastAccepted)

client.locations(...).collect { location ->
    val raw = TrackFix(...)
    trackingState.update { lastLocation = location } // raw HUD

    if (raw.accuracyMeters > settings.minAccuracyMeters) return@collect
    if (raw.satellitesInFix < settings.minSatellites) return@collect

    val forStore = if (settings.trackSmoothingEnabled) {
        filter.observe(
            raw,
            settings.usageType,
            settings.smoothingStrength,
            settings.stationaryLockEnabled
        )
    } else {
        raw
    }

    if (FixAcceptance.shouldAccept(lastAccepted, forStore, settings.toFilter(), settings.recordingDensity)) {
        // insert forStore, lastAccepted = forStore
    }
}
```

On `ACTION_STOP`, STOP placemark uses the last Kalman position if smoothing is on and a last location exists; do not run DP.

Session start: new `KalmanTrackFilter()` when `lastAccepted == null`. Resume: `seedFrom(lastAccepted)`.

---

## Tests (write first in `:engine`, then implement)

Add focused tests. Do not mock Android.

1. **Straight highway:** 20 points along a line with ±8 m sideways GPS noise, 20 m/s, FOUR_WHEELERS / MEDIUM. Smoothed cross-track RMSE must be **lower** than raw. Path length within 5% of true 400 m.

2. **Roundabout:** points on a 25 m radius circle, 8 m/s, FOUR_WHEELERS. Smoothed path stays within 6 m of the true circle (not a hexagon). DP is not applied in this test.

3. **Figure-8 runner:** two 12 m radius lobes, 3 m/s, RUNNER / LOW, `EVERY_FIX`. After smoothing, the path must still have a **self-crossing** (or two lobes whose centroids are ≥ 8 m apart on opposite sides of the midpoint). Assert it does **not** collapse to a single line (max perpendicular distance from start–end chord ≥ 8 m).

4. **Zigzag:** 8 legs, 6 m amplitude, 4 m wavelength, RUNNER / LOW. Peak-to-peak amplitude after smoothing ≥ 4 m.

5. **Stationary lock:** 30 samples, true speed 0, scatter 6 m, lock on. All outputs within 2 m of the first sample.

6. **Jump:** 100 m teleport with accuracy 5 m. Filter re-inits; no interpolated points in the gap (single-step API: output near the new fix, not halfway).

7. **Kalman off:** `observe` is not required; `FixAcceptance` SMART still matches today’s distance tests.

8. **DP regression:** existing `DouglasPeuckerTest` still passes. Add: 19.5 m tolerance on a 10 m figure-8 **does** collapse (documents why runner DP default is off).

9. **Usage defaults:** helper returns the table (runner EVERY_FIX, DP off, tolerance 2; motorbike SMART, DP on, 6 m).

Use the same haversine / projection as production. Build synthetic WGS84 around `47.0, 19.0`.

Run: `./gradlew :engine:test`

---

## Implementation order

1. Enums + `UsageType.defaultSmoothing()` + tests for the table.
2. `KalmanTrackFilter` + tests 1–6.
3. `FixAcceptance` density + runner SMART half-spacing + tests.
4. DataStore keys, migration from 19.5, `setUsageType` writes smoothing fields, ViewModel setters.
5. Service wiring.
6. Settings UI + EN/HU strings.
7. Docs / README / help strings.
8. `./gradlew :engine:test` and `./gradlew :app:assembleDebug`.

Commit only if the user asks. Do not bump version unless asked.

---

## Acceptance criteria

- Runner defaults: Kalman on, LOW, EVERY_FIX, DP off. A logged figure-8 and a zigzag remain recognizable on the map without turning simplify on.
- Car/motorbike defaults: Kalman on, MEDIUM, SMART, DP on with 6–8 m (not 19.5). A roundabout looks curved, not a 3-segment polyline.
- With simplify off, map polyline equals stored points.
- KMZ / Route odometer never run through Douglas–Peucker.
- Stopped user: no 10 m scribble.
- Changing usage in Settings overwrites density, Kalman, strength, lock, DP on/off, and DP metres (same as today’s filter overwrite).
- Hungarian UI complete for new rows.
- No comments, no type assertions, Kalman has no Android imports.

---

## If you get stuck

- Do not add libraries (no EJML, no Apache Commons Math). 4×4 Kalman by hand is enough.
- Do not put Kalman in `GtlViewModel` only — then KMZ stays jagged.
- Do not “fix” smoothness by raising DP tolerance.
- Do not map-match.
- If Settings becomes cramped, put the new block **above** the read-only fix-filter lines, **below** the existing map switches.
)
