# GTL development roadmap

[English](dev-roadmap-en.md) · [Magyar](dev-roadmap-hu.md)

**Status:** Product plan after 2.0.19 (versionCode 37), updated 2026-10-04. The tree already has the map HUD, GPX 1.1, skyplot, elevation profile, QNH, GPS altitude pick, OSM file/camera guards, the map line coloured by usage speed bands (with a legend), the large live speed with a sparkline on Route, and saved-track cards.  
**Not a code spec:** this document records *why* the order is this order, and the release waves. Write a short brief / test list for the wave you actually start.  
**Effort:** calendar days for one developer who already knows this repo (not person-months, not a team week).

Related: [README-en.md](../README-en.md), [CHANGELOGS.md](../CHANGELOGS.md), [DBSTRUCT-en.md](DBSTRUCT-en.md), [GPSDATAFLOW-en.md](GPSDATAFLOW-en.md).

---

## How to read this

GTL (GPS Track Logger) is the Kotlin + Compose rewrite of the 2014 Eclipse app. The 2.0.x releases fixed the **logging chain**: Room is the single source of truth, Kalman runs on stored points, GNSS-only for Run/Hike and bicycle, KMZ, OSM, fix cloud. After 2.0.19 the tree also has the live **map HUD**, **GPX**, GPS **skyplot**, an **elevation profile** (dashed baro line), **GPS altitude** pick, OSM **file validation**, **speed-coloured** map tracks, and **saved-track cards**.

The remaining gap is **night use, a live map that turns with the ride, and the motorbike log**: tiles stay daylight, the map always faces north, the lean stored on every point shows as one number, and the notification is static. The Play feature graphic promises a dark cockpit and a glowing track; HUD, skyplot, and speed colour already match, dark tiles do not.

Items are ordered by **value** (retention × Play conversion × leverage of data you already store), not by easy wins. Effort is secondary; when two items are close in value, the cheaper one moves earlier inside the same wave. The front of the list is what you see **on every recording**; the end is what you look at once after a trip.

---

## Product position

GTL is a **local, precise GPS track logger**. Nothing is uploaded to our server. The map polyline **is** the SQLite tracklog — not map-matching, not a second sketch.

Primary users (Settings usage order and default **motorbike**):

- riders and drivers who later open the path in Google Earth or keep a private archive
- runners, hikers, and cyclists who want a sports-watch-style GNSS track with no snap-to-street
- boat / aircraft users with ICAO units — smaller audience, already in the usage model

The competition is **not** Strava, Komoot, or Google Maps Navigation. Those are social, training plans, turn-by-turn. GTL’s moat is:

1. data stays on the phone
2. the line is what the chip / Kalman actually stored
3. GNSS HUD (constellations, SNR, fix cloud / CEP95, skyplot)
4. KMZ balloons for Earth; GPX for everything else

Every new feature should strengthen that, or **unlock** it (dark map: you can see it at night; lean band: a mountain road becomes a riding log) — not replace it with a social feed.

---

## Where we stand

### What is strong

- Foreground service, visible notification, no `ACCESS_BACKGROUND_LOCATION`
- Usage presets (aircraft, watercraft, car, motorbike, bicycle, Run/Hike) in one DataStore edit
- Filter chain: accuracy / satellites → optional Kalman → density → Room → Map / Route / KMZ / GPX
- Map HUD (large speed, accuracy, GNSS used/in view; while logging: trip, elapsed, pulsing REC); keep-screen-on setting
- Line coloured by the usage speed bands (`SpeedBands`, `SpeedTrack`), corner legend (open, or collapsed to dots)
- Route tab: large live speed with a sparkline; average for a saved session
- GPS tab: L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC, SNR, polar skyplot; altitude from `GpsAltitude.pick`; baro when a pressure sensor exists
- KMZ `LineString` at stored GPS altitude (`absolute`; `clampToGround` only when the track has no altitude) plus hidden `gx:Track` at height 0 for timed data; Start / Pause / Stop balloons
- GPX 1.1 share (one file, several `trk`; START/PAUSE/STOP `wpt`)
- Elevation profile (GPS × distance; dashed baro, QNH 900–1100 hPa; 1500 m GPS guard)
- OSM Mapsforge and Turistautak; `OsmMapFile` checks; Google Maps when `MAPS_API_KEY` is set
- Green **S** / red **E** on the drawn track; usage silhouette at your position
- Compose palette: light sage/paper, dark **Cockpit** (`Theme.kt`); dark follows the system theme

### What is weak for listing and for use

- **Map at night:** HUD is there, tiles stay daylight. The speed colour has never been seen on dark tiles; on white, the light bands wash out.
- **The map does not turn:** the Google silhouette is `rotation = 0f`, and the OSM layers drop the `draw(..., _rotation)` parameter. While riding, a bend runs sideways across the screen, not ahead. Every fix already carries a `bearing`.
- **Standing speed:** the map HUD rounds the chip’s raw Doppler to a whole km/h. Indoors, with the pin not moving, it still shows ~5 km/h. Speed accuracy is never read.
- **Lean:** `leanAngle` is stored on every point, and the UI shows one number. Its source is also `TYPE_GRAVITY` (fallback: accelerometer), which in a steady turn measures apparent gravity lying in the bike’s plane, so it reads **close to 0° in a turn**. See 5.
- **Saved tracks:** the card exists. An optional file name is still missing; average and max are recomputed from the points on every open.
- **Theme:** no in-app System / Light / Dark control, and `themes.xml` keeps a light status bar.
- **Notification:** static title + text + Stop (`TrackingForegroundService.buildNotification`). No live speed / distance.
- **Play feature graphic** (`docs/play-console/feature-graphic.png`): dark dash, glowing track, skyplot. Dark tiles are still missing.

### Intentionally absent (keep it that way)

IMEI, live lat/lng upload, follow-me web, remote unlock, Google Directions, app-toggled GPS, boot auto-start. `RemoteTrackSync` is a no-op stub; **do not fill it** with a backend while the product is a local logger.

---

## Do not put these at the front of the backlog

| Idea | Why not now |
| ---- | ----------- |
| Live sharing / own server / `RemoteTrackSync` upload | Against the privacy policy and the 2.0 promise |
| Snap-to-street (OSM/Google map-matching) | Against the Run/Hike GNSS track; Kalman is deliberately not this |
| Strava-like social, kudos, segments | A different product |
| Wear OS | Weeks, extra store, test matrix; the phone HUD already ships |
| GPX import | The app is a logger, not an archive manager |
| FIT / TCX | If someone asks for Garmin Connect |
| Turn-by-turn | Policy / APIs / distraction; 2014 Directions was dropped on purpose |
| Launcher widget | A Quick Settings tile is cheaper; widget later |
| **Naive headingUp on the silhouette**, on by default, `heading = 0` on every update when off | Spins at a red light (standing Doppler course noise), the bounding box clips the track, the next fix spins a two-finger rotation back, and OSM still does not turn. Use gated course-up following (3) instead |
| **Temperature band** (colour the track by temperature) | `ambientTemperature` is missing on many phones; the feature would be empty for most users |

---

## Eye-catcher principle

Do not “restyle it as generic Material 3”. Teal, carmine, magenta, and cockpit already distinguish the brand. HUD, skyplot, and the speed-coloured line exist. The problem is a **daylight map at night**, a map that **always faces north** while riding, and stored lean that **does not show**.

What makes the feature graphic honest:

1. Live **map HUD** — done
2. **Skyplot** on the GPS tab — done
3. **Speed-coloured** line — done
4. The same on **dark tiles**, with a camera that turns with the ride and a comet tail — the next visual

Recapture a logging HUD Map and replace the feature graphic with a **real UI crop** once dark tiles ship.

Default usage is motorbike: the eye-catcher must work **day and night, in gloves, at a glance** (large digits, few taps, dark map, the road ahead pointing up).

---

## Priority (by value)

Effort is one developer-day. “Files” are natural entry points, not an exhaustive list. Up front: what shows on every recording. At the back: what you look at rarely, or what works on few phones.

### 1. Dark map + in-app theme

**Value:** high — brand, night riding, listing match; speed colour and the comet tail only shine on dark  
**Effort:** 2–3 days  
**Wave:** 1

**Why.** At night the white map glares and the speed colour washes out. Less unique than the other items, still strong in use, and the visuals of 3, 5, and 6 rest on it.

**Today.** `GtlTheme(darkTheme = isSystemInDarkTheme())`. `gtlWash` gradient. `values/themes.xml`: teal status bar, paper nav bar, light. No DataStore theme key. No `MapStyleOptions`.

**Build.**

- Setting: **System / Light / Dark** (DataStore)
- Two styles: Google Maps `MapStyleOptions` night JSON, and a Mapsforge dark render theme (OSM and Turistautak), on the same switch
- `Theme.Gtl` status/nav bars follow the theme; splash can stay black
- HUD, polyline, fix-cloud contrast on dark tiles (lighter stroke for the purple circle); a dark variant of the speed bands if the deep-green / black band disappears

**Do not.** A third “high contrast” palette.

**Test.** System / Light / Dark; Google, OSM, and Turistautak; HUD, speed bands, and fix cloud readable on dark tiles.

---

### 2. Standing speed on the HUD

**Value:** high — the instrument shows motion while you are still; the same gate drives the course in 3  
**Effort:** ~1 day  
**Wave:** 1

**Why.** Indoors, with the pin not moving, the map HUD reads ~5 km/h (raw Doppler, `Units.hudSpeedNumber`). The Route tab already shows 0 while idle (`RouteTabSpeeds`). A fixed km/h cutoff would hide slow walking and still let a larger indoor spike through.

**Build.** One pure engine function; the map HUD, the Route instant speed, and the notification (4) call it.

- No speed field: “—”.
- Speed accuracy present (`getSpeedAccuracyMetersPerSecond`; `LocationCompat` on minSdk 24): when speed ≤ its accuracy, the display is **0**.
- No speed accuracy: when displacement since the previous fix ≤ horizontal accuracy, **0**.
- Hysteresis: two significant samples to leave 0, two to return.
- The stored track stays Kalman + `pauseSpeedMps()`; this is a display rule.

**Do not.** A fixed km/h cutoff. Speed from the coordinate delta. Kalman on the idle preview.

**Test.** Standing indoor fix: HUD 0. Walking in the open: the number appears. No 0/5 flicker.

---

### 3. Gated course-up following (+ silhouette toward the course)

**Value:** high — shows on every recording; the road runs ahead on screen  
**Effort:** 3–4 days (Google ~0.5 day; OSM is the work)  
**Wave:** 2

**Why.** In live follow the camera turns with the course, so a bend points ahead, not sideways. The bearing is already on the fix; no new data.

**Today.** Google: `MarkerComposable(rotation = 0f)`, camera bearing 0. OSM: `UsagePositionLayer` and the track-end layers drop the `draw(..., _rotation: Rotation)` parameter. The north indicator (`NorthIndicator`) already turns with the camera bearing.

**Build.**

- Engine: a pure `heading` gate. **Below 1 m/s keep the last good course** (no spinning at a red light); above it the fix `bearing` when `hasBearing()`. Hysteresis and light smoothing (e.g. 2–3 fixes) so one noisy fix does not jerk the camera. Same decision as the speed gate in 2.
- Camera modes: **live follow** = course-up; **whole route on screen** and **saved track** = north-up fit (`TrackCameraBounds` unchanged). A two-finger rotate or a drag leaves course-up until the next My location tap; the next fix does not spin it back.
- Google: `CameraPosition.bearing`.
- OSM: Mapsforge camera rotation; the silhouette, S/E, accuracy circle, and the tap-menu anchor honour the `Rotation`.
- **Silhouette toward the course when the map faces north:** the silhouette turns to the gated course. With a course-up camera the icon stays pointing to the top of the screen, otherwise the bend turns twice.
- Setting: Course-up / North-up switch, Course-up by default for motorbike and car.

**Do not.** Compass heading while riding (a tank-mounted phone is not the travel direction). Naive headingUp (see the “do not” table).

**Test.** Standing at a red light the camera does not turn; in a bend it follows; a saved track and whole-route fit face north; a two-finger rotate does not snap back; on OSM the silhouette and S/E sit correctly under rotation.

---

### 4. Live foreground notification

**Value:** medium–high — second HUD, phone in a pocket  
**Effort:** 1–2 days  
**Wave:** 1

**Why.** Riders, runners, and hikers are not staring at the screen. Same numbers as the map HUD, on the lock screen / shade.

**Today.** `NOTIFICATION_ID = 17`, `IMPORTANCE_LOW`, Stop action, static strings.

**Build.** Periodic `notify()`: speed, distance, elapsed. Keep Stop. No sound/vibration. On Android 16 (targetSdk 36) consider the promoted ongoing-notification style; a plain updating notification on older versions.

**Depends on.** The gated speed from 2.

---

### 5. Lean band (riding log)

**Value:** high for the default motorbike usage — a mountain road becomes a riding log  
**Effort:** 3–4 days  
**Wave:** 2

**Why.** Lean is on the phone for every point; the UI shows one number. A left/right lean band along the line, a live lean gauge, max left / max right on the card.

**Risk — the current source lies in a turn.** `BikeLeanAngle.fromGravity` uses the `TYPE_GRAVITY` vector (fallback: accelerometer). In a steady, coordinated turn the apparent gravity (g + centripetal) lies in the bike’s plane, so a tank-mounted phone reads **~0°**. Gyro-fused gravity shows something briefly and pulls back in a longer turn. In a pocket, gravity lies outright.

**Build.**

- Engine: lean from **kinematics**: `lean ≈ atan(v · ω / g)`, where `v` is speed and `ω` the turn rate. On a saved track `ω` is the change of the stored `bearing` over time, so it works **retroactively on every existing motorbike track** and does not depend on how the phone is mounted. Live, `ω` comes from the gyroscope (rotation vector yaw rate) when present.
- No band at low speed (e.g. < 3 m/s) or across sparse points (bearing noise dominates).
- Draw on both engines: a band beside the line, or a selectable colouring (speed / lean), separate shades for left and right; legend in degrees.
- Saved card: max left / max right.
- **Hidden on Run/Hike**, optional on bicycle.

**Not in v1.** Drawing the `TYPE_GRAVITY` lean as the band. A mount-calibration wizard.

**Test.** Engine: a synthetic arc at a given speed → known lean. Straight: ~0°. Standing: no band. A real mountain-road track: left/right sign correct.

---

### 6. Comet tail

**Value:** medium — cheap, adds motion while riding  
**Effort:** ~1 day  
**Wave:** 2

**Why.** While logging, the last minute is thicker and full colour, the older line is quieter. It sits on the existing coloured line; no new data.

**Build.** `SpeedTrack` segments get an “age”; width and alpha by age (2–3 steps are enough, not a per-metre gradient). Live session only; a saved track stays uniform. Watch the polyline count on Google.

**Depends on.** Shows best on dark tiles (1).

---

### 7. Flight in Google Earth (KMZ `gx:Tour`)

**Value:** medium — big visual, cheap, aimed at the KMZ audience  
**Effort:** 1–2 days  
**Wave:** 3

**Why.** The KMZ already hands Earth the line at stored GPS altitude; a `gx:Tour` flies the camera along it in Google Earth. Most of the “local flyover” (12) visual at a fraction of the cost.

**Build.** `KmzExporter`: `gx:Tour` / `gx:Playlist` with `gx:FlyTo` steps on the thinned path (heading from the course, fixed tilt, range by speed), optional in the share menu. Pure engine test on the KML.

---

### 8. Saved tracks: file name and stored stats

**Value:** medium — the card exists; the name and speeds stored on the session are missing  
**Effort:** 1–2 days  
**Wave:** 3

**Still to build.**

- Optional `displayName` on the session. Empty = date. The KMZ/GPX filename and the `<name>` / KMZ folder use it; replace characters that are illegal in a filename.
- Extend `track_sessions` (Room migrate 6→7): `avgSpeed`, `maxSpeed` (m/s), and once 5 ships `maxLeanLeft` / `maxLeanRight`, computed at Stop. The list reads those columns.

---

### 9. Track image / postcard share

**Value:** medium — social eye-catcher with no server  
**Effort:** 3–5 days  
**Wave:** 3

**Why.** A glowing, speed-coloured line on dark, distance, time, an elevation strip, GTL stamp, PNG on the share sheet. No upload. Also a listing source.

**Build.** No tiles: own polyline on a dark Canvas (most of the `TrackRouteThumbnail` drawing is reusable) → Bitmap → share. Do not start with the Static Maps API.

**Depends on.** 8 (name, stats); 5 if lean goes on it too.

---

### 10. Curve gallery

**Value:** medium on a motorbike; on its own just a list  
**Effort:** 2–3 days  
**Wave:** 3

**Why.** “Left 38°, 72 km/h”: the strongest bends of a saved track, from the bearing jump and the lean; a tap jumps the map there. Worth it after the band (5), because it uses the same engine computation.

---

### 11. Manual pause and lap / split

**Value:** medium  
**Effort:** 2–3 days  
**Wave:** 4

**Why.** `PAUSE` today is a speed threshold. You cannot hold the log at a red light or a fuel stop without Stop (new session).

**Build.** Pause / Resume while logging; no MOVE while paused; resume does not insert a new `track_sessions` row. KMZ has a pause icon; GPX `trkseg` at the pause. Lap afterwards.

---

### 12. Local flyover / replay

**Value:** medium — a strong film, but you watch it once after the trip  
**Effort:** 5–8 days  
**Wave:** 4

**Why.** The saved track draws itself, a leaning silhouette rides along it, and the HUD shows that point’s numbers. A good Play video.

**Expensive because:** scrubber, tempo (1× / 10× / 60×), leaning icon, camera on both engines. 7 (`gx:Tour`) delivers most of the visual cheaper; build this only if users ask for it in the app.

**Depends on.** 3 (turning camera and silhouette), 5 (leaning icon).

---

### 13. Landscape / tank HUD mode

**Value:** medium for default motorbike, high effort  
**Effort:** 5–8 days  
**Wave:** 4

**Why.** The app is `portrait`. On a tank mount, huge digits in landscape are readable. The portrait HUD already delivers ~80% of that benefit.

**Watch.** Mapsforge `MapView` + Compose rotation; test together with the course-up camera from 3.

---

### 14. Quick Settings tile (Start / Stop)

**Value:** low–medium  
**Effort:** ~1 day  
**Wave:** any time

**Why.** Start from the shade wearing gloves. `TileService`, same permissions as the Start button. Not a listing visual.

---

### 15. Altitude sculpture

**Value:** low — pretty on a pass or a flight, flat on a city ride  
**Effort:** 3–5 days  
**Wave:** later

**Why so far back.** The KMZ already hands this to Google Earth (`absolute` altitude), and 7 adds a flight on top. An in-app 2.5D drawing only pays off for mountain or aircraft usage.

---

### 16. Two tracks overlaid

**Value:** low — archive  
**Effort:** 2–3 days  
**Wave:** later

**Why so far back.** Two selected saved tracks on one map in different colours. The eye catches on the difference, but the picture while riding does not change, and it is rarely used.

---

## Release waves

Version numbers are **suggestions**.

### Wave 1 — “you can see it at night” (about 4–6 days)

| # | Item | Effort |
| - | ---- | ------ |
| 1 | Dark map + System/Light/Dark | 2–3 days |
| 2 | Standing speed on the HUD | ~1 day |
| 4 | Notification with live numbers | 1–2 days |
| — | Play screenshots + feature graphic from the **real** dark HUD map | 0.5 day |

**Done when:** dark mode uses dark tiles (Google, OSM, Turistautak); a standing indoor fix shows 0 km/h; the notification shows the same number as the HUD; the new listing shot is the dark HUD Map.

### Wave 2 — “the map is alive while you ride” (about 7–9 days)

| # | Item | Effort |
| - | ---- | ------ |
| 3 | Gated course-up following + silhouette toward the course | 3–4 days |
| 5 | Lean band (kinematic lean) | 3–4 days |
| 6 | Comet tail | ~1 day |

**Done when:** at a red light the camera does not spin, in a bend it looks ahead; a saved track faces north; a saved mountain-road track shows left/right lean, old tracks too; while logging the last minute stands out.

### Wave 3 — “the archive tells a story” (about 7–12 days)

| # | Item | Effort |
| - | ---- | ------ |
| 7 | KMZ `gx:Tour` | 1–2 days |
| 8 | File name + avg/max (+ max lean) on the session row | 1–2 days |
| 9 | Postcard PNG | 3–5 days |
| 10 | Curve gallery | 2–3 days |

### Wave 4 — deepen (later, sliced)

| # | Item | Effort |
| - | ---- | ------ |
| 11 | Manual pause / lap | 2–3 days |
| 12 | Local flyover | 5–8 days |
| 13 | Landscape HUD | 5–8 days |
| 14 | Quick Settings tile | ~1 day |
| 15 | Altitude sculpture | 3–5 days |
| 16 | Two tracks overlaid | 2–3 days |

12 and 13 are the expensive ones: only if that is still the complaint after waves 1–3.

---

## Summary table

| Rank | Feature | Value | Effort | Wave |
| ---- | ------- | ----- | ------ | ---- |
| 1 | Dark map + theme control | high | 2–3 days | 1 |
| 2 | Standing speed on the HUD | high | ~1 day | 1 |
| 3 | Gated course-up following + silhouette toward the course | high | 3–4 days | 2 |
| 4 | Live notification | medium–high | 1–2 days | 1 |
| 5 | Lean band (kinematic) | high (motorbike) | 3–4 days | 2 |
| 6 | Comet tail | medium | ~1 day | 2 |
| 7 | KMZ `gx:Tour` for Google Earth | medium | 1–2 days | 3 |
| 8 | Saved-track file name + stored stats | medium | 1–2 days | 3 |
| 9 | Postcard share | medium | 3–5 days | 3 |
| 10 | Curve gallery | medium (motorbike) | 2–3 days | 3 |
| 11 | Manual pause / lap | medium | 2–3 days | 4 |
| 12 | Local flyover / replay | medium | 5–8 days | 4 |
| 13 | Landscape tank HUD | medium | 5–8 days | 4 |
| 14 | Quick Settings tile | low–medium | ~1 day | any |
| 15 | Altitude sculpture | low | 3–5 days | later |
| 16 | Two tracks overlaid | low | 2–3 days | later |
| — | Naive headingUp | rejected | — | — |
| — | Temperature band | rejected | — | — |

If you can only ship **two** items: **dark map + gated course-up following**. The first shows at night, the second on every recording.  
If you target riders: the third is the **lean band**, with kinematic lean.

---

## Docs and Play, at the end of every wave

Not optional wrap-up:

- [CHANGELOGS.md](../CHANGELOGS.md) EN + Hungarian
- [docs/play-console/whatsnew.txt](play-console/whatsnew.txt) (500 characters per language)
- Help EN/HU for new controls
- README feature list if the user can see it
- Screenshots 24-bit PNG, no alpha. HUD Map (logging, dark tiles) and the feature graphic after wave 1; a mountain road with the lean band after wave 2.

[GPSDATAFLOW](GPSDATAFLOW-en.md) only changes if the write chain changes (the kinematic lean in 5 is computed, not written). Session name, stats, max lean: [DBSTRUCT](DBSTRUCT-en.md) migration 6→7.

---

## Open decisions (before implementation, per wave)

Wave 1:

- Google night JSON: stock style or a custom one around cockpit teal/carmine?
- Mapsforge dark theme: own XML or a recolour of the built-in one? Does Turistautak need its own?

Wave 2:

- Course-up default per usage (on for motorbike/car, off for Run/Hike?)
- Lean: a band beside the line, or a speed / lean colouring switch?
- Live lean: gyroscope yaw rate, or only the GPS bearing change (lags ~1 fix)?

Lock those in the wave brief; this roadmap deliberately does not freeze pixel layout.
