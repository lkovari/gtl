# GTL development roadmap

[English](dev-roadmap-en.md) · [Magyar](dev-roadmap-hu.md)

**Status:** Product plan after 2.0.5 (versionCode 23).  
**Not a code spec:** this document records *why* the order is this order, and the release waves. Write a short brief / test list for the wave you actually start.  
**Effort:** calendar days for one developer who already knows this repo (not person-months, not a team week).

Related: [README-en.md](../README-en.md), [CHANGELOGS.md](../CHANGELOGS.md), [RENEWAL-REPORT.md](RENEWAL-REPORT.md), [DBSTRUCT-en.md](DBSTRUCT-en.md), [GPSDATAFLOW-en.md](GPSDATAFLOW-en.md).

---

## How to read this

GTL (GPS Track Logger) is the Kotlin + Compose rewrite of the 2014 Eclipse app. The 2.0.x releases fixed the **logging chain**: Room is the single source of truth, Kalman runs on stored points, GNSS-only for runner/bicycle, KMZ, OSM, fix cloud.

The next gap is not a new filter. It is **product experience and interchange**: the map looks empty while recording, export is friendly mainly to Google Earth, and the Play feature graphic promises a cockpit the real UI does not yet deliver.

Items are ordered by **value** (retention × Play conversion × leverage of data you already store), not by easy wins. Effort is secondary; when two items are close in value, the cheaper one moves earlier inside the same wave.

---

## Product position

GTL is a **local, precise GPS track logger**. Nothing is uploaded to our server. The map polyline **is** the SQLite tracklog — not map-matching, not a second sketch.

Primary users (Settings usage order and default **motorbike**):

- riders and drivers who later open the path in Google Earth or keep a private archive
- runners and cyclists who want a sports-watch-style GNSS track with no snap-to-street
- boat / aircraft users with ICAO units — smaller audience, already in the usage model

The competition is **not** Strava, Komoot, or Google Maps Navigation. Those are social, training plans, turn-by-turn. GTL’s moat is:

1. data stays on the phone
2. the line is what the chip / Kalman actually stored
3. GNSS HUD (constellations, SNR, fix cloud / CEP95)
4. KMZ balloons for Earth

Every new feature should strengthen that, or **unlock** it (GPX: take the data out; HUD: see it while recording) — not replace it with a social feed.

---

## Where 2.0.5 stands

### What is strong

- Foreground service, visible notification, no `ACCESS_BACKGROUND_LOCATION`
- Usage presets (aircraft, watercraft, car, motorbike, bicycle, runner) in one DataStore edit
- Filter chain: accuracy / satellites → optional Kalman → density → Room → Map / Route / KMZ
- GPS tab: L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC, SNR
- KMZ tessellated `LineString` (visible, height 0) plus hidden `gx:Track` for timed data; Start / Pause / Stop balloons on the stored line (Stop is the last accepted point)
- OSM Mapsforge region download; Google Maps when `MAPS_API_KEY` is set
- Compose palette: light sage/paper, dark **Cockpit** (`Theme.kt`); dark follows the system theme

### What is weak for listing and for use

- **Map while logging:** HUD (large speed, trip, REC) over Google and OSM. Listing screenshots in `docs/screenshots/` may still be the old empty map.
- **Route tab:** 2×4 `HudMetric` cards plus an elevation profile when a session has points.
- **Saved tracks:** date + raw `usageType` enum + `METRIC`. No name, distance, or mini-map.
- **Export:** KMZ and GPX 1.1. No FIT / TCX / GPX import.
- **Theme:** cockpit colours exist, but the **map stays daylight**, there is no in-app System / Light / Dark control, and `themes.xml` keeps a light status bar.
- **Notification:** static title + text + Stop (`TrackingForegroundService.buildNotification`). No live speed / distance.
- **Play feature graphic** (`docs/play-console/feature-graphic.png`): dark dash, glowing red track, skyplot. The app does not compose that yet. That is the largest eye-catcher gap.

### Intentionally absent (keep it that way)

See the renewal report: IMEI, live lat/lng upload, follow-me web, remote unlock, Google Directions, app-toggled GPS, boot auto-start. `RemoteTrackSync` is a no-op stub; **do not fill it** with a backend while the product is a local logger.

---

## Do not put these at the front of the backlog

| Idea | Why not now |
| ---- | ----------- |
| Live sharing / own server / `RemoteTrackSync` upload | Against the privacy policy and the 2.0 promise |
| Snap-to-street (OSM/Google map-matching) | Against the runner GNSS track; Kalman is deliberately not this |
| Strava-like social, kudos, segments | A different product |
| Wear OS | Weeks, extra store, test matrix; phone HUD first |
| GPX import | The app is a logger, not an archive manager |
| FIT / TCX | After GPX, if someone asks for Garmin Connect |
| Turn-by-turn | Policy / APIs / distraction; 2014 Directions was dropped on purpose |
| Launcher widget | A Quick Settings tile is cheaper; widget later |

---

## Eye-catcher principle

Do not “restyle it as generic Material 3”. Teal, carmine, magenta, and cockpit already distinguish the brand. The problem is **hierarchy** and an **empty map**.

Three visuals that make the feature graphic honest:

1. Live **map HUD** (large speed, distance, REC) — wave 1
2. **Speed-coloured** line on a dark map — wave 2
3. **Skyplot** on the GPS tab — wave 2

Play screenshots from then on should be the Map tab with HUD, not the Route number grid. After wave 1, replace the feature graphic with a **real UI crop** if it already matches, instead of a 3D satellite montage.

Default usage is motorbike: the eye-catcher must work **day and night, in gloves, at a glance** (large digits, few taps, dark map).

---

## Priority (by value)

Effort is one developer-day. “Files” are natural entry points, not an exhaustive list.

### 1. Map HUD overlay — done 2026-09-12

**Value:** very high — eye-catcher and utility together  
**Effort:** 3–5 days  
**Wave:** 1

**Why.** While recording, people watch the Map tab. Today that is a red line on a daylight Google map, with no numbers. Speed lives on GPS / Route — unsafe to tap while riding, and on the listing it looks like an empty map. The feature graphic promises a HUD; this is the largest gap.

**Today.** `MapPane.kt`: Google Maps Compose + Mapsforge `AndroidView`. Overlays: accuracy circle, fix cloud, usage silhouette, north marker, broom. No telemetry. Route totals already come from `GtlViewModel` / `TrackStatsCalculator` live samples, but only on the Route tab.

**Build.** One **shared Compose HUD** on top of the map (both engines, not two overlay implementations):

- large **speed** (metric / imperial / ICAO from Settings)
- **distance** and **elapsed time**
- **accuracy** in metres + GNSS used / in view
- pulsing **REC** when `live.logging`
- optional **keep screen on** while logging (phone on the tank)

HUD speed / accuracy should follow the **raw** HUD fix (same idea as the pale purple circle); distance from Room stats. Do not cover the line: a bottom or top strip, semi-transparent, cockpit colours in dark theme.

**Depends on.** Nothing. Dark map (3) makes it look finished.

**Test.** Logging on Google and on a downloaded OSM region; idle + saved track (HUD dims or hides when not logging — pick one rule); unit switch; keep-screen-on only while logging.

---

### 2. GPX export — done 2026-09-12

**Value:** very high — data leaves the island  
**Effort:** 1.5–2.5 days  
**Wave:** 1

**Why.** KMZ is ideal for Google Earth (`gx:Track`, play/pause/stop icons, balloons). The rest of the tracklog world expects **GPX 1.1**: OsmAnd, Komoot, Garmin Connect, Relive, QGIS, many watch sites. Without it GTL is a closed-format diary. The README already lists this.

**Today.** `KmlExportUseCase` → `KmlExporter` + `KmzExporter`, FileProvider, share sheet. Saved tracks: one session → one KMZ; several sessions → one KMZ with a folder per track.

**Build.**

- `:engine` `GpxExporter`: `trk` / `trkseg` / `trkpt` (`lat`, `lon`, `ele`, `time`; optional `speed` in a GPX extension or omit it — ship core GPX first so every importer swallows it)
- START/PAUSE/STOP as `wpt`, or one `trkseg` per stretch once user-pause exists
- Share: **KMZ or GPX** (system chooser or two in-app actions). Multi-select: one `.gpx` with several `trk` elements, or several files — one file with several tracks is simpler
- MIME `application/gpx+xml`, filename `GTL_yyyyMMdd_HHmmss.gpx`
- Help EN/HU, engine unit test with fixed coordinates

**Not now.** FIT, TCX, GPX import.

**Depends on.** Nothing. Session name (7) can fill `<name>` later.

---

### 3. Dark map + in-app theme

**Value:** high — brand, night riding, listing match  
**Effort:** 2–3 days  
**Wave:** 1

**Why.** Compose already has cockpit dark (`isSystemInDarkTheme()`). Google Maps and Mapsforge stay **daylight** tiles. At night the white map glares; magenta title + dark top bar + light map fall apart. README still lists “light and dark themes” because **map and system chrome** are unfinished, not because the cards lack a palette.

**Today.** `GtlTheme(darkTheme = isSystemInDarkTheme())`. `gtlWash` gradient. `values/themes.xml`: teal status bar, paper nav bar, light. No DataStore theme key.

**Build.**

- Setting: **System / Light / Dark** (DataStore)
- Google Maps `MapStyleOptions` night JSON when the theme is dark
- Mapsforge: dark render theme (built-in or custom XML) on the same switch
- `Theme.Gtl` status/nav bars follow the theme; splash can stay black
- HUD, polyline, fix-cloud contrast on dark tiles (carmine can stay; the purple circle needs a lighter stroke)

**Do not.** A third “high contrast” palette. The two schemes in `Color.kt` are enough.

**Depends on.** Polish HUD (1) dark styling in the same release if they ship together.

---

### 4. Live foreground notification

**Value:** medium–high — second HUD, phone in a pocket  
**Effort:** 1–2 days  
**Wave:** 1

**Why.** Logging is a foreground service. Riders and runners are not staring at the screen. The notification currently says “logging is on”. Same numbers as the map HUD, on the lock screen / shade.

**Today.** `NOTIFICATION_ID = 17`, `IMPORTANCE_LOW`, Stop action, static strings.

**Build.** Periodic `notify()` updates: speed, distance, accuracy (short `contentText` or `BigText`). Keep Stop. No sound/vibration (stay LOW). `FLAG_UPDATE_CURRENT`.

**Depends on.** Same formatters as the HUD (`Units`). Do it after or with the HUD so rounding does not fork.

---

### 5. Speed-coloured track + Route cockpit

**Value:** high — second eye-catcher, instead of equal cards  
**Effort:** 4–6 days  
**Wave:** 2

**Why.** A single carmine polyline is accurate and looks like a red scribble on the listing. Speed colour (slow teal → mid amber → fast carmine) tells city vs highway, climb vs descent at a glance. On Route, speed should be **the** number, not one of eight equal tiles.

**Today.** `Polyline` / Mapsforge polyline is one colour, `CarmineTrack`. `RoutePane`: `HudMetric` grid. `TrackStats`: odometer, moving/waiting, max/avg speed, min/max altitude — no time-series drawing.

**Build.**

- Engine: segments by `speedMps` (fixed bands per unit system so the legend is stable; usage-specific bands if runner vs car collide)
- Google: several short polylines or spans; OSM: segment overlay. Colour **after** Douglas–Peucker, or the simplified chord lies about speed
- Legend in a map corner
- Route: large speed, sparkline underneath (speed or altitude), other metrics secondary

**Not in v1.** Altitude colour and speed colour at once (a toggle later). Per-metre interpolated gradients — segments are enough.

**Depends on.** Dark map (3) so colours do not wash out on white tiles. HUD (1) can stay a single-colour live head; history is coloured.

---

### 6. GNSS skyplot

**Value:** high for the brand, medium for a daily rider  
**Effort:** 3–4 days  
**Wave:** 2

**Why.** Constellation chips are already distinctive, but the feature graphic shows a **polar plot**. GPSTest / nerd loggers expect it. GTL’s GNSS credibility becomes visible here: used vs in view, L5, Galileo.

**Today.** `GnssStatusSource` samples, but `SatelliteSample` does **not** store azimuth or elevation, even though `GnssStatus.getAzimuthDegrees` / `getElevationDegrees` exist. `GnssClassifier.snapshot` aggregates; individual birds never reach the UI.

**Build.**

- `SatelliteSample` + snapshot list: azimuth, elevation, CN0, used, constellation, L1/L5
- Canvas polar: 0° = north, rings at 0/30/60° elevation; colour by constellation; filled = used-in-fix
- GPS tab: skyplot above or below SNR; keep the chips
- Live while idle (like the compass) — logging not required

**Do not.** 3D globe, AR. 2D polar plus the chips you already have.

**Depends on.** Not on the HUD. Screenshot: GPS tab with skyplot for the listing.

---

### 7. Saved tracks: cards, name, stats

**Value:** medium–high — the private archive becomes usable  
**Effort:** 3–4 days  
**Wave:** 2

**Why.** After Stop, the list is a date. Two Saturday rides are indistinguishable. No distance, no usage icon, raw `TWO_WHEELERS` on screen. Share filenames are timestamps.

**Today.** `track_sessions`: `startedAt`, `stoppedAt`, `usageType`, `measurementSystem`. No `displayName`. `TracksScreen`: checkbox, Show on map, Delete, share selected.

**Build.**

- Optional `displayName` (Room migrate 3→4). Empty = date, as now
- List card: usage icon, name/date, distance, duration, max/avg (from `TrackStatsCalculator` per session — cache for the list, do not scan all `gps_events` on every scroll)
- Mini-polyline optional (costlier; stats + icon already help)
- Share KMZ **or** GPX; `<name>` / KMZ folder = displayName
- Delete confirmation if missing

**Depends on.** GPX (2) if the chooser lives here. Coloured track (5) beautifies Show on map; it does not block the list.

---

### 8. Elevation profile (GPS first, baro later) — GPS profile done 2026-09-12; QNH later

**Value:** medium  
**Effort:** 2–3 days for the profile; +2–3 days for baro  
**Wave:** 3 (profile), later baro

**Why.** Runners, cyclists, and aircraft look at climb. `gps_events.altitude` is GPS altitude — noisy, but present. DBSTRUCT lists baro (`TYPE_PRESSURE`) as planned, with ICAO feet.

**Today (2026-09-12).** Saved tracks **Elevation** and the Route tab: GPS altitude × distance canvas. `baroAltitude` / `pressureHpa` on `gps_events` (Room 4); ISA, no QNH. A dashed second line if at least two baro samples exist.

**Baro later.** QNH / sea-level calibration, aircraft usage. The column exists; do not mix with GPS alt without a legend (the profile already uses a separate line).

**Depends on.** Route cockpit (5) makes room for a sparkline; the full profile can sit under the card.

---

### 9. Manual pause and lap / split

**Value:** medium  
**Effort:** 2–3 days  
**Wave:** 3

**Why.** `PAUSE` today is a speed threshold (0.25 m/s pedestrian, 0.4 vehicle). The user cannot hold the log at a red light without Stop (new session). Runner lap, rider fuel stop: pause + resume in the **same** session. Optional lap via `eventKind` or a split table.

**Today.** `EventKind`: START, MOVE, PAUSE, STOP. STOP closes the session (`stoppedAt`). UI is only Start / Stop.

**Build.** A third control while logging: Pause / Resume. While paused the service may keep running but must not write MOVE (or write a PAUSE placemark and skip density). Resume must not insert a new `track_sessions` row. KMZ already has a pause icon.

**Lap.** After pause; manual pause is 80% of the value.

**Depends on.** GPX `trkseg` at a pause is natural. HUD: Pause state instead of REC.

---

### 10. Landscape / tank HUD mode

**Value:** medium for default motorbike, high effort  
**Effort:** 5–8 days  
**Wave:** 3 or later

**Why.** The app is `portrait`. On a tank mount, huge digits in landscape are readable. Item 1’s portrait HUD already delivers ~80% of that benefit.

**Build if you get here.** Unlock orientation while logging, or a landscape activity; giant speed; map in a thin strip; both map engines. Watch Mapsforge `MapView` + Compose rotation.

**Depends on.** 1 and 3 done, or you lay out the HUD twice.

---

### 11. Track image / postcard share

**Value:** medium — social eye-catcher with no server  
**Effort:** 4–6 days  
**Wave:** 3+

**Why.** Dark background, glowing line, distance, time, GTL stamp, PNG on the share sheet. Privacy holds (no upload). Also a listing-screenshot source.

**Expensive because:** a static map snapshot (Google Static / OSM render / own polyline on a dark canvas). The last is simplest and offline: no tiles, just line + stats. Start there, not with the Static Maps API.

**Depends on.** 5 (colour) and 7 (name/stats) fill the postcard.

---

### 12. Quick Settings tile (Start / Stop)

**Value:** low–medium  
**Effort:** ~1 day  
**Wave:** from the end of wave 1, or wave 3

**Why.** Start from the shade wearing gloves. `TileService`, same permissions as the Start button. Not a listing visual.

**Depends on.** Nothing. Notification (4) Stop is already one control.

---

## Release waves

Version numbers are **suggestions**. 2.0.5 can stay a hotfix line; the next minor is wave 1.

### Wave 1 — “see it and take it out” (about 1–1.5 weeks)

Goal: the map is an instrument while recording, data leaves as GPX, night does not glare.

| # | Item | Effort |
| - | ---- | ------ |
| 1 | Map HUD + keep-screen-on | 3–5 days |
| 2 | GPX export in share | 1.5–2.5 days |
| 3 | Dark map + System/Light/Dark | 2–3 days |
| 4 | Notification with live numbers | 1–2 days |
| — | Play screenshots + feature graphic from the **real** HUD map | 0.5 day |

Can overlap: GPX (engine tests) beside HUD UI. Theme and HUD share visual polish.

**Done when:** OsmAnd opens a GTL GPX; while logging the Map tab shows large speed on Google and OSM; dark mode uses dark tiles; the notification shows km/h and km; the new 9:16 listing shot is not the old empty map.

### Wave 2 — “archive and GNSS become visible” (about 1.5–2 weeks)

| # | Item | Effort |
| - | ---- | ------ |
| 5 | Coloured polyline + Route large speed / sparkline | 4–6 days |
| 6 | Skyplot | 3–4 days |
| 7 | Saved-track cards + displayName | 3–4 days |

**Done when:** a highway stretch is not the same colour as city crawling; the GPS tab shows a polar plot while idle; two sessions are distinguishable by name; Show on map uses that session’s speed bands.

### Wave 3 — deepen (later, sliced)

| # | Item | Effort |
| - | ---- | ------ |
| 8 | Elevation profile (GPS) | 2–3 days |
| 9 | Manual pause | 2–3 days |
| 10 | Landscape HUD | 5–8 days |
| 11 | Postcard PNG | 4–6 days |
| 12 | Quick Settings tile | ~1 day |
| — | Baro altitude | +2–3 days |

10 and 11 are the expensive ones: only if after waves 1–2 the remaining complaint is still “I’d mount it on the tank” / “I’d share a picture”.

---

## Summary table

| Rank | Feature | Value | Effort | Wave |
| ---- | ------- | ----- | ------ | ---- |
| 1 | Map HUD overlay | very high | 3–5 days | 1 |
| 2 | GPX export | very high | 1.5–2.5 days | 1 |
| 3 | Dark map + theme control | high | 2–3 days | 1 |
| 4 | Live notification | medium–high | 1–2 days | 1 |
| 5 | Coloured track + Route cockpit | high | 4–6 days | 2 |
| 6 | GNSS skyplot | high (brand) | 3–4 days | 2 |
| 7 | Saved-track cards + name | medium–high | 3–4 days | 2 |
| 8 | Elevation profile | medium | 2–3 days | 3 |
| 9 | Manual pause / lap | medium | 2–3 days | 3 |
| 10 | Landscape tank HUD | medium | 5–8 days | 3+ |
| 11 | Postcard share | medium | 4–6 days | 3+ |
| 12 | Quick Settings tile | low–medium | ~1 day | any |

Wave 1 total: **about 8–13 days** plus listing assets.  
Wave 2: **about 10–14 days**.  
If you can only ship **two** items: **HUD + GPX**. That closes “I show this in the store” and “I can take the data out”.

---

## Docs and Play, at the end of every wave

Not optional wrap-up:

- [CHANGELOGS.md](../CHANGELOGS.md) EN + Hungarian
- [docs/play-console/whatsnew.txt](play-console/whatsnew.txt) (500 characters per language)
- Help EN/HU for new controls
- README feature list if the user can see it
- Screenshots 1080×1920, 24-bit, no alpha; in wave 1 at least `map.png` / `tracking.png` / a GPS shot if skyplot comes later

[GPSDATAFLOW](GPSDATAFLOW-en.md) only changes if the write chain changes (GPX reads Room like KMZ — usually a README export paragraph is enough). Skyplot: `Gnss.kt` plus the GPSDATAFLOW HUD branch if the snapshot schema grows. Session name: [DBSTRUCT](DBSTRUCT-en.md) migration.

---

## Open decisions (before implementation, per wave)

Wave 1 (closed 2026-09-12):

- HUD idle: compact strip with a GPS fix; hidden on a saved track while idle; full strip while logging.
- Keep-screen-on default off.
- GPX: one file with several `trk`.

Wave 2:

- Speed bands global (0–30 / 30–70 / 70+ km/h) or per usage (runner needs another scale)?
- Skyplot at the top of the GPS tab (more scroll) or collapsible?

Lock those in the wave brief; this roadmap deliberately does not freeze pixel layout.
