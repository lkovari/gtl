# GTL development roadmap

[English](dev-roadmap-en.md) · [Magyar](dev-roadmap-hu.md)

**Status:** Product plan after 2.0.6 (versionCode 24). The tree already has the map HUD, GPX 1.1, skyplot, elevation profile, QNH, GPS altitude pick, and OSM file/camera guards.  
**Not a code spec:** this document records *why* the order is this order, and the release waves. Write a short brief / test list for the wave you actually start.  
**Effort:** calendar days for one developer who already knows this repo (not person-months, not a team week).

Related: [README-en.md](../README-en.md), [CHANGELOGS.md](../CHANGELOGS.md), [RENEWAL-REPORT.md](RENEWAL-REPORT.md), [DBSTRUCT-en.md](DBSTRUCT-en.md), [GPSDATAFLOW-en.md](GPSDATAFLOW-en.md).

---

## How to read this

GTL (GPS Track Logger) is the Kotlin + Compose rewrite of the 2014 Eclipse app. The 2.0.x releases fixed the **logging chain**: Room is the single source of truth, Kalman runs on stored points, GNSS-only for runner/bicycle, KMZ, OSM, fix cloud. After 2.0.6 the tree also has the live **map HUD**, **GPX**, GPS **skyplot**, an **elevation profile** (dashed baro line with Settings QNH), **GPS altitude** pick (MSL then GNSS, implausible fused dropped), and OSM **file validation** (Use no longer crash-loops; camera stays on the downloaded region).

The remaining gap is **night use, archive, and the second eye-catcher**: the map stays daylight, the notification is static, the saved list is a date, the line is one colour. The Play feature graphic promises a dark cockpit and a glowing track; HUD and skyplot already match, dark tiles and speed colour do not.

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
3. GNSS HUD (constellations, SNR, fix cloud / CEP95, skyplot)
4. KMZ balloons for Earth; GPX for everything else

Every new feature should strengthen that, or **unlock** it (dark map: you can see it at night; cards: your own log is readable) — not replace it with a social feed.

---

## Where we stand

### What is strong

- Foreground service, visible notification, no `ACCESS_BACKGROUND_LOCATION`
- Usage presets (aircraft, watercraft, car, motorbike, bicycle, runner) in one DataStore edit
- Filter chain: accuracy / satellites → optional Kalman → density → Room → Map / Route / KMZ / GPX
- Map HUD (large speed, accuracy, GNSS used/in view; while logging: trip, elapsed, pulsing REC); keep-screen-on setting
- GPS tab: L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC, SNR, polar skyplot; altitude from `GpsAltitude.pick`; baro when a pressure sensor exists
- KMZ tessellated `LineString` (visible, height 0) plus hidden `gx:Track` for timed data; Start / Pause / Stop balloons on the stored line (Stop is the last accepted point)
- GPX 1.1 share (one file, several `trk`; START/PAUSE/STOP `wpt`)
- Elevation profile (GPS × distance; dashed baro using Settings QNH 900–1100 hPa; axis at least 50 m)
- OSM Mapsforge region download with `OsmMapFile` checks; failed open turns **Use downloaded OSM map** off; camera stays on the `.map` when GPS is outside it. Google Maps when `MAPS_API_KEY` is set
- Green **S** / red **E** on the drawn track (end hidden while logging)
- Compose palette: light sage/paper, dark **Cockpit** (`Theme.kt`); dark follows the system theme

### What is weak for listing and for use

- **Map at night:** HUD is there, tiles stay daylight. Listing shots in `docs/screenshots/` (2026-09-12) show GPS skyplot, Route elevation, Compass MAG rose, Settings QNH, Saved-track elevation, and Map with S/E. Map HUD is still missing from the listing (HUD is hidden when a saved track is shown).
- **Route tab:** 2×4 `HudMetric` cards plus an elevation profile. Speed is not *the* number.
- **Saved tracks:** date + raw `usageType` enum + `METRIC`. Elevation, delete confirm, and KMZ/GPX share exist. No name, distance, or mini-map.
- **Theme:** cockpit colours exist, but the **map stays daylight**, there is no in-app System / Light / Dark control, and `themes.xml` keeps a light status bar.
- **Notification:** static title + text + Stop (`TrackingForegroundService.buildNotification`). No live speed / distance.
- **Play feature graphic** (`docs/play-console/feature-graphic.png`): dark dash, glowing track, skyplot. HUD and skyplot exist; dark tiles and speed colour do not.

### Intentionally absent (keep it that way)

See the renewal report: IMEI, live lat/lng upload, follow-me web, remote unlock, Google Directions, app-toggled GPS, boot auto-start. `RemoteTrackSync` is a no-op stub; **do not fill it** with a backend while the product is a local logger.

---

## Do not put these at the front of the backlog

| Idea | Why not now |
| ---- | ----------- |
| Live sharing / own server / `RemoteTrackSync` upload | Against the privacy policy and the 2.0 promise |
| Snap-to-street (OSM/Google map-matching) | Against the runner GNSS track; Kalman is deliberately not this |
| Strava-like social, kudos, segments | A different product |
| Wear OS | Weeks, extra store, test matrix; the phone HUD already ships |
| GPX import | The app is a logger, not an archive manager |
| FIT / TCX | If someone asks for Garmin Connect |
| Turn-by-turn | Policy / APIs / distraction; 2014 Directions was dropped on purpose |
| Launcher widget | A Quick Settings tile is cheaper; widget later |

---

## Eye-catcher principle

Do not “restyle it as generic Material 3”. Teal, carmine, magenta, and cockpit already distinguish the brand. HUD and skyplot exist. The problem is **hierarchy** on Route, a **daylight map at night**, and a **single-colour** line.

What makes the feature graphic honest:

1. Live **map HUD** — done
2. **Skyplot** on the GPS tab — done
3. **Speed-coloured** line on a dark map — the remaining visual

Play listing (2026-09-12) has GPS skyplot, Route elevation, Compass, Settings QNH, Saved tracks, and Map with S/E — not the HUD Map (HUD is hidden on a shown saved track). Recapture a logging HUD Map and replace the feature graphic with a **real UI crop** once dark tiles and the coloured line match it.

Default usage is motorbike: the eye-catcher must work **day and night, in gloves, at a glance** (large digits, few taps, dark map).

---

## Priority (by value)

Effort is one developer-day. “Files” are natural entry points, not an exhaustive list.

### 1. Dark map + in-app theme

**Value:** high — brand, night riding, listing match  
**Effort:** 2–3 days  
**Wave:** 1

**Why.** Compose already has cockpit dark (`isSystemInDarkTheme()`). Google Maps and Mapsforge stay **daylight** tiles. At night the white map glares; magenta title + dark top bar + light map fall apart. README still lists “light and dark themes” because **map and system chrome** are unfinished, not because the cards lack a palette. HUD dark styling belongs with the tiles.

**Today.** `GtlTheme(darkTheme = isSystemInDarkTheme())`. `gtlWash` gradient. `values/themes.xml`: teal status bar, paper nav bar, light. No DataStore theme key.

**Build.**

- Setting: **System / Light / Dark** (DataStore)
- Google Maps `MapStyleOptions` night JSON when the theme is dark
- Mapsforge: dark render theme (built-in or custom XML) on the same switch
- `Theme.Gtl` status/nav bars follow the theme; splash can stay black
- HUD, polyline, fix-cloud contrast on dark tiles (carmine can stay; the purple circle needs a lighter stroke)

**Do not.** A third “high contrast” palette. The two schemes in `Color.kt` are enough.

**Depends on.** Nothing. The HUD already sits on both map engines.

**Test.** System / Light / Dark in Settings; Google and a downloaded OSM region; HUD and fix cloud readable on dark tiles; status bar follows the theme.

---

### 2. Live foreground notification

**Value:** medium–high — second HUD, phone in a pocket  
**Effort:** 1–2 days  
**Wave:** 1

**Why.** Logging is a foreground service. Riders and runners are not staring at the screen. The notification currently says “logging is on”. Same numbers as the map HUD, on the lock screen / shade.

**Today.** `NOTIFICATION_ID = 17`, `IMPORTANCE_LOW`, Stop action, static strings.

**Build.** Periodic `notify()` updates: speed, distance, accuracy (short `contentText` or `BigText`). Keep Stop. No sound/vibration (stay LOW). `FLAG_UPDATE_CURRENT`.

**Depends on.** Same formatters as the HUD (`Units`). The HUD already exists, so rounding must not fork.

---

### 3. Speed-coloured track + Route cockpit

**Value:** high — second eye-catcher, instead of equal cards  
**Effort:** 4–6 days  
**Wave:** 2

**Why.** A single carmine polyline is accurate and looks like a red scribble on the listing. Speed colour (slow teal → mid amber → fast carmine) tells city vs highway, climb vs descent at a glance. On Route, speed should be **the** number, not one of eight equal tiles. The elevation profile under the cards already ships.

**Today.** `Polyline` / Mapsforge polyline is one colour, `CarmineTrack`. `RoutePane`: `HudMetric` grid + elevation profile. `TrackStats`: odometer, moving/waiting, max/avg speed, min/max altitude — no speed time-series.

**Build.**

- Engine: segments by `speedMps` (fixed bands per unit system so the legend is stable; usage-specific bands if runner vs car collide)
- Google: several short polylines or spans; OSM: segment overlay. Colour **after** Douglas–Peucker, or the simplified chord lies about speed
- Legend in a map corner
- Route: large speed, sparkline underneath (speed or altitude), other metrics secondary

**Not in v1.** Altitude colour and speed colour at once (a toggle later). Per-metre interpolated gradients — segments are enough.

**Depends on.** Dark map (1) so colours do not wash out on white tiles. HUD can stay a single-colour live head; history is coloured.

---

### 4. Saved tracks: cards, name, stats

**Value:** medium–high — the private archive becomes usable  
**Effort:** 3–4 days  
**Wave:** 2

**Why.** After Stop, the list is a date. Two Saturday rides are indistinguishable. No distance, no usage icon, raw `TWO_WHEELERS` on screen. Share filenames are timestamps.

**Today.** `track_sessions`: `startedAt`, `stoppedAt`, `usageType`, `measurementSystem`. No `displayName`. `TracksScreen`: checkbox, Show on map, Elevation, Delete with confirm, share selected as KMZ or GPX.

**Build.**

- Optional `displayName` (Room migrate 4→5). Empty = date, as now
- List card: usage icon, name/date, distance, duration, max/avg (from `TrackStatsCalculator` per session — cache for the list, do not scan all `gps_events` on every scroll)
- Mini-polyline optional (costlier; stats + icon already help)
- `<name>` / KMZ folder = displayName (the KMZ/GPX chooser already exists)

**Depends on.** Coloured track (3) beautifies Show on map; it does not block the list.

---

### 5. Manual pause and lap / split

**Value:** medium  
**Effort:** 2–3 days  
**Wave:** 3

**Why.** `PAUSE` today is a speed threshold (0.25 m/s pedestrian, 0.4 vehicle). The user cannot hold the log at a red light without Stop (new session). Runner lap, rider fuel stop: pause + resume in the **same** session. Optional lap via `eventKind` or a split table.

**Today.** `EventKind`: START, MOVE, PAUSE, STOP. STOP closes the session (`stoppedAt`). UI is only Start / Stop.

**Build.** A third control while logging: Pause / Resume. While paused the service may keep running but must not write MOVE (or write a PAUSE placemark and skip density). Resume must not insert a new `track_sessions` row. KMZ already has a pause icon. GPX `trkseg` at a pause is natural.

**Lap.** After pause; manual pause is 80% of the value.

**Depends on.** HUD: Pause state instead of REC.

---

### 6. Landscape / tank HUD mode

**Value:** medium for default motorbike, high effort  
**Effort:** 5–8 days  
**Wave:** 3 or later

**Why.** The app is `portrait`. On a tank mount, huge digits in landscape are readable. The portrait HUD already delivers ~80% of that benefit.

**Build if you get here.** Unlock orientation while logging, or a landscape activity; giant speed; map in a thin strip; both map engines. Watch Mapsforge `MapView` + Compose rotation.

**Depends on.** Dark map (1) done, or you lay out the HUD twice for night.

---

### 7. Track image / postcard share

**Value:** medium — social eye-catcher with no server  
**Effort:** 4–6 days  
**Wave:** 3+

**Why.** Dark background, glowing line, distance, time, GTL stamp, PNG on the share sheet. Privacy holds (no upload). Also a listing-screenshot source.

**Expensive because:** a static map snapshot (Google Static / OSM render / own polyline on a dark canvas). The last is simplest and offline: no tiles, just line + stats. Start there, not with the Static Maps API.

**Depends on.** 3 (colour) and 4 (name/stats) fill the postcard.

---

### 8. Quick Settings tile (Start / Stop)

**Value:** low–medium  
**Effort:** ~1 day  
**Wave:** from the end of wave 1, or wave 3

**Why.** Start from the shade wearing gloves. `TileService`, same permissions as the Start button. Not a listing visual.

**Depends on.** Nothing. Notification Stop is already one control.

---

## Release waves

Version numbers are **suggestions**. 2.0.6 can stay a hotfix line; the next minor is the rest of wave 1.

### Wave 1 — “you can see it at night” (about 3.5–5.5 days)

Goal: night does not glare, the notification shows the same numbers, listing is the real HUD map.

| # | Item | Effort |
| - | ---- | ------ |
| 1 | Dark map + System/Light/Dark | 2–3 days |
| 2 | Notification with live numbers | 1–2 days |
| — | Play screenshots + feature graphic from the **real** HUD map | 0.5 day |

GPS / Route / Compass / Settings / Saved tracks / Map S/E listing shots recaptured 2026-09-12 (1080×1920, bottom tabs kept). Remaining: HUD Map while logging, and the feature graphic.

**Done when:** dark mode uses dark tiles; the notification shows km/h and km; the new 9:16 listing shot is the HUD Map, not the idle S/E track.

### Wave 2 — “archive and the line tell a story” (about 7–10 days)

| # | Item | Effort |
| - | ---- | ------ |
| 3 | Coloured polyline + Route large speed / sparkline | 4–6 days |
| 4 | Saved-track cards + displayName | 3–4 days |

**Done when:** a highway stretch is not the same colour as city crawling; two sessions are distinguishable by name; Show on map uses that session’s speed bands.

### Wave 3 — deepen (later, sliced)

| # | Item | Effort |
| - | ---- | ------ |
| 5 | Manual pause | 2–3 days |
| 6 | Landscape HUD | 5–8 days |
| 7 | Postcard PNG | 4–6 days |
| 8 | Quick Settings tile | ~1 day |

6 and 7 are the expensive ones: only if after waves 1–2 the remaining complaint is still “I’d mount it on the tank” / “I’d share a picture”.

---

## Summary table

| Rank | Feature | Value | Effort | Wave |
| ---- | ------- | ----- | ------ | ---- |
| 1 | Dark map + theme control | high | 2–3 days | 1 |
| 2 | Live notification | medium–high | 1–2 days | 1 |
| 3 | Coloured track + Route cockpit | high | 4–6 days | 2 |
| 4 | Saved-track cards + name | medium–high | 3–4 days | 2 |
| 5 | Manual pause / lap | medium | 2–3 days | 3 |
| 6 | Landscape tank HUD | medium | 5–8 days | 3+ |
| 7 | Postcard share | medium | 4–6 days | 3+ |
| 8 | Quick Settings tile | low–medium | ~1 day | any |

Wave 1 remaining: **about 3.5–5.5 days** plus listing assets.  
Wave 2: **about 7–10 days**.  
If you can only ship **two** items: **dark map + coloured track**. That closes “it glares at night” and “a red scribble on the listing”.

---

## Docs and Play, at the end of every wave

Not optional wrap-up:

- [CHANGELOGS.md](../CHANGELOGS.md) EN + Hungarian
- [docs/play-console/whatsnew.txt](play-console/whatsnew.txt) (500 characters per language)
- Help EN/HU for new controls
- README feature list if the user can see it
- Screenshots 1080×1920, 24-bit, no alpha; GPS skyplot, Route elevation, Compass, Settings QNH, Saved tracks, and Map S/E recaptured 2026-09-12. Recapture HUD Map (logging) and the feature graphic after dark tiles.

[GPSDATAFLOW](GPSDATAFLOW-en.md) only changes if the write chain changes. Session name: [DBSTRUCT](DBSTRUCT-en.md) migration 4→5.

---

## Open decisions (before implementation, per wave)

Wave 1:

- Google night JSON: stock style or a custom one around cockpit teal/carmine?

Wave 2:

- Speed bands global (0–30 / 30–70 / 70+ km/h) or per usage (runner needs another scale)?

Lock those in the wave brief; this roadmap deliberately does not freeze pixel layout.
