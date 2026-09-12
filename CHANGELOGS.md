# Changelog

All notable changes to **GPS Track Logger** (`com.lkovari.mobile.apps.gtl`).

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning matches `versionName` **2.0.5** / `versionCode` **23** (minSdk 24, targetSdk 36).

Canonical history is this file. Play Console what’s-new: [docs/play-console/whatsnew.txt](docs/play-console/whatsnew.txt). How logging writes the Map polyline: [README-en.md — How logging works](README-en.md#how-logging-works) / [README-hu.md](README-hu.md#hogyan-működik-a-naplózás).

## [Unreleased]

### 2026-09-12

OSM map robustness, GPS altitude pick, QNH, GNSS skyplot, map S/E, Map HUD, GPX 1.1, elevation profile, compass rose, barometric column, Play listing screenshots.

### Added

- Map tab **HUD** over Google Maps and OSM: large speed (metric / imperial / ICAO), accuracy in metres, GNSS used/in view. While logging: odometer, elapsed time, pulsing **REC**. Idle with a GPS fix: dim compact panel at the bottom left, sized to the numbers. Hidden when a saved track is shown and logging is off. Speed and accuracy follow the raw HUD fix (same philosophy as the pale purple circle); trip totals come from Room.
- Settings **Keep screen on while logging** (off by default). The flag is cleared when logging stops.
- **GPX 1.1** export from Saved tracks. Share selected opens KMZ or GPX. One session → `GTL_yyyyMMdd_HHmmss.gpx`; several sessions → one file with several `<trk>`. Core `trkpt` (`lat`, `lon`, `ele`, `time`); START / PAUSE / STOP as `<wpt>`. MIME `application/gpx+xml`. Engine `GpxExporter` unit tests with fixed coordinates.
- **Elevation profile** (GPS altitude vs distance) on Saved tracks (Elevation) and on the Route tab when a session has points. Optional dashed barometric line when at least two pressure samples exist. Axis min/max is GPS and baro together, at least 50 m. Legend shows the last GPS and baro values. Route totals now compute for a saved / last session, not only while logging.
- Compass tab: rotating rose, fixed lubber line, MAG / TRUE in the centre with the three-digit heading. MAG is the sensor; TRUE adds declination from the last GPS fix. Switch is on the Compass tab (default MAG). Low accuracy: figure-8 warning under the dial.
- GPS tab **skyplot**: north-up polar plot under SNR (zenith at centre, horizon outer ring, 30°/60° rings). Colour by constellation; filled = used in fix; hollow = in view; inner ring = L5-class carrier. Dual-frequency L1+L5 of the same SVID is one marker. Live without Start; not stored in SQLite, KMZ, or GPX. Constellation chips stay above and use the same colours.
- `gps_events.baroAltitude` and `pressureHpa` (Room schema **4**). Written from `TYPE_PRESSURE` via `BaroAltitude.metersFromPressureHpa` using Settings QNH at insert (900–1100 hPa, default ISA 1013.25) when the sensor exists; otherwise null. GPX `ele` stays GPS altitude.
- KMZ **Distance** on every `gx:Track` point (cumulative metres in ExtendedData), plus `baro` (metres at the QNH in force when the row was stored, empty if no sample). Pause / Stop balloons also show `distance=` in session units.
- Settings **QNH** (900–1100 hPa, default 1013.25). Use METAR sea-level QNH (Qxxxx), not station pressure. Live baro and the elevation-profile dashed line use the current slider. Stored `pressureHpa` is unchanged so you can recalibrate; `baroAltitude` at insert uses the QNH in force then. GPS altitude is independent (`GpsAltitude.pick`). ISA 1013 vs LHBP Q1022 is about 70 m on the baro line.
- Map **Start / End** markers on Google Maps and OSM: green **S** at the first drawn point, red **E** at the last when idle (`TrackEndpoints`). While logging the usage silhouette is “now”.
- Google Maps **layers** button left of zoom: Map, Satellite, Hybrid, Terrain. Remembered. Hidden on OSM. Directions / Open in Maps toolbar stays off.
- Engine tests for skyplot merge, `GpsAltitude`, `OsmMapFile`, `OsmMapCamera`, `OsmMapViewRedraw`, `MapFitZoom`, `TrackEndpoints`, and elevation plot scale.
- Play listing phone shots recaptured 2026-09-12 as 1080×1920 RGB PNG (no alpha, 9:16; status bar and home indicator cropped, bottom tabs kept): `gps-idle.png` (skyplot), `route.png` (elevation), `map.png` (S/E track), `compass.png` (MAG rose), `settings.png` (QNH, Runner), `saved-tracks.png` (Elevation). HUD Map listing and the feature graphic still wait on dark tiles.

### Changed

- GPS tab **Latitude / Longitude / Accuracy / Altitude** cards use less vertical space (tighter padding, smaller type).
- GPS tab **Provider** sits in the constellation strip, to the right of NavIC (under GLO). Long provider names shorten (GPS, Fused, NET).
- GPS tab **Altitude** card shows **GPS / Baro** after the label and `GPS / baro` values (baro is — when there is no pressure sample).
- Route tab has **Status** and **Ambient** in the same-size metric cards as elapsed and speed.
- GPS tab **Signal SNR** and **skyplot** use less vertical space. SNR padding and the meter bar are tighter, and the waiting line no longer reserves two rows. Skyplot title and legend sit on the four corners of the circle: SKYPLOT / In view on the top edge, Used / L5 on the bottom.
- Settings rows, switches, sliders, and usage icons are shorter so more controls fit without scrolling.
- Saved tracks **Delete** wraps onto a second row on a phone (it sat off-screen after Elevation). Confirm dialog before cascade-delete.
- Settings is vertically scrollable again so the QNH slider fits with the other controls.
- KMZ Earth details match the Start / Pause / Stop field spec. Datetime is UTC `YYYY:MM:DD HH:MM:SS` (no `time=` prefix, no `UTC` suffix). Then `temp=` in session units or `temp=N/A`, `lon=` then `lat=`, `Altitude:` (GPS) and `Baro:` (stored baro at insert QNH, or `N/A`) in session units. Pause adds `Speed:` (instant GPS speed at that row), `duration=` (≤60 s as `N s`, under 60 min as whole `N min`, otherwise `HH:MM:SS` from Start), and `distance=` so far. Stop adds `Avg. Speed:` and `Max speed:` (one decimal from `TrackStatsCalculator`; metric `km/h`, imperial `mph`, ICAO `kt`; imperial/ICAO altitude `ft`; imperial temp `°F`), then session `duration=` and `distance=`. KMZ `gx:Track` ExtendedData includes `baro` metres and GPS `alt`; `gx:coord` height is 0. The visible line is a tessellated `LineString`. Dropped from balloons: `usage=`, `lean=`, `Distance:`, `Duration:`, forced `speed=0`, `Avg. speed:` / `Max. speed:`. Start has no duration/distance.

### Fixed

- GPS tab altitude of about **−1787 m** with a good fused lat/lon: fused often reports a junk ellipsoid height. `GpsAltitude.pick` order is GNSS MSL, fused MSL, GNSS ellipsoid, fused ellipsoid. Values outside −430…9000 m (Dead Sea to typical flight level) are dropped; if none remain, altitude is removed from the `Location`. Live **Baro** appears when a pressure sensor exists.
- OSM **Use** crashed on Mapsforge zoom (`FATAL EXCEPTION: Animator`, `CalledFromWrongThreadException` at `GtlOsmMapView.repaint`): the white-map parent `invalidate()` ran on the zoom Animator thread. Ancestor invalidation is posted to the main thread when not already on it (`OsmMapViewRedraw`).
- A missing, truncated, or non-Mapsforge `.map` no longer crash-loops the next launch. `OsmMapFile.isReadable` requires magic `mapsforge binary OSM` and a header file size that matches the file. Download keeps only those files. A failed open or unreadable path turns **Use downloaded OSM map** off and shows an on-device message.
- OSM Map tab was blank with a Hungary `.map` when the GPS fix was outside that file (emulator default Mountain View). The camera now uses the map file start/bounds (`OsmMapCamera`), follows GPS only inside the file, and requests tiles as soon as the view has a size.
- Elevation profile Y labels were GPS min/max while the plot scaled to GPS **and** baro. Changing QNH swapped which line was on top but left the same ~8 m numbers. Labels now follow `ElevationSeries.plotScale` (GPS+baro), the span is at least 50 m so GPS vertical noise does not fill the chart, and the legend shows the last GPS and baro values. Saved-track Elevation recomputes when QNH changes.
- KMZ/Google Earth: Start / Pause / Stop icons sit on the stored track (`clampToGround` on the `gx:Track` and Point placemarks). Stop is the last **accepted** log point, not the raw HUD fix. A trailing STOP row is not drawn as an off-track hook. Pause icons that overlap Start or Stop are omitted; consecutive standing PAUSE rows collapse to one icon.
- Google Earth close zoom: the visible path is a tessellated `LineString` clamped to ground at height 0. `gx:Track` with GPS altitude as `gx:coord` Z was ignored as clamp by Earth Android — the line floated (~GPS alt), slid off the road around 80 m eye altitude, and vanished below ~50 m under the camera. Timed `gx:Track` is hidden; GPS altitude is ExtendedData `alt`.
- Stop balloon title is **Stop** (not Pause). Details use HTML line breaks and a KML `BalloonStyle` so Start / Pause / Stop fields show in Earth (Pause was blank when Earth ignored plain newlines). Missing temperature is `temp=N/A`.
- OSM **Show on map** from Saved tracks: Mapsforge `mapViewDimension.dimension` is still null on the first Compose `update` after returning to the Map tab, so `fitOsmToBounds` NPEd and the process exited. Fit now waits until the view has a size (`MapFitZoom.canFit`), clamps zoom to 3–20, and `destroyAll()`s the MapView on release.
- OSM Map tab was white until a zoom: Mapsforge draws tiles off-screen then waits for `onDraw` to swap buffers. Compose `AndroidView` often skipped that draw (a software layer made it worse), so the map stayed empty until a zoom forced layout. OSM now invalidates the Compose parents after every repaint, keeps the MapView sized while you are on GPS/Route/Compass, and redraws tiles on first layout without a dummy zoom.

### Magyar

- GPS fül **Szélesség / Hosszúság / Pontosság / Magasság** kártyái alacsonyabbak.
- GPS fül **Forrás** a konstelláció-sávban, a NavIC mellett (a GLO alatt). Hosszú provider-név rövidül (GPS, Fused, NET).
- GPS fül **Magasság** kártyán **GPS / Baro** a címke után, az érték `GPS / baro` (baro — ha nincs nyomásminta).
- Útvonal fülön **Állapot** és **Környezeti hőmérséklet** ugyanakkora kártyában, mint az eltelt idő és a sebesség.
- GPS fül **Jel SNR** és **skyplot** alacsonyabb: tömörebb SNR sáv, a skyplot cím és jelmagyarázat a kör négy sarkán (SKYPLOT / Látható fent, Használatban / L5 lent).
- Beállítások sorai, kapcsolói, csúszkái és használati ikonjai alacsonyabbak.
- Magasságprofil Y-címkéi GPS min/max voltak, a rajz GPS+baro skálán: QNH-váltáskor a vonalak cseréltek, a ~8 m maradt. A címke a tényleges skála, legalább 50 m, a jelmagyarázat az utolsó GPS/baro érték. Mentett Elevation QNH-váltáskor újraszámol.
- Mentett útvonalak **Törlés** gombja keskeny kijelzőn a Magasság alá tör (eddig kilógott). Megerősítés a cascade-törlés előtt.
- Beállítás: **Képernyő bekapcsolva naplózáskor** (alapból ki). A Beállítások újra görgethető a QNH csúszka miatt.
- **GPX 1.1** megosztás a Mentett útvonalakon (KMZ vagy GPX).
- **Magasságprofil** a mentett trackeken és az Útvonal fülön. Route-stat mentett sessionre is.
- Iránytű: MAG / TRUE a fülön (alap MAG), forgó rózsa, deklináció a last GPS-fixből, 8-as figyelmeztetés LOW pontosságnál.
- Play listing telefonképek újra véve 2026-09-12, 1080×1920 RGB PNG (nincs alfa, 9:16; status bar és home indicator levágva, alsó fülek megmaradnak): `gps-idle.png` (skyplot), `route.png` (magasságprofil), `map.png` (S/E track), `compass.png` (MAG rózsa), `settings.png` (QNH, Futó), `saved-tracks.png` (Magasság). A HUD-os Térkép listing és a feature graphic a sötét csempére vár.
- GPS fül **skyplot**: észak-fent polar plot az SNR alatt (zenit középen, horizon kívül, 30°/60° gyűrűk). Szín konstellációnként; kitöltött = used; üres = in view; belső gyűrű = L5. Ugyanannak a holdnak az L1+L5 egy pont. Idle-ben is él; nem kerül SQLite-ba, KMZ-be, GPX-be. A chippek ugyanazt a színt használják.
- Barometrikus magasság oszlop (Room 4), ha van nyomásszenzor. Íráskor a Beállítások QNH-ja; a `pressureHpa` nyers marad.
- Beállítás **QNH** (900–1100 hPa, alap 1013,25). METAR tengerszinti QNH (Qxxxx), nem állomásnyomás. Élő baro és magasságprofil a jelenlegi csúszkát használja. A GPS-magasság ettől független (`GpsAltitude.pick`). ISA 1013 vs LHBP Q1022 kb. 70 m a baro vonalon.
- Térkép **S** / **E** a track elején és végén (Google és OSM).
- OSM **Térképen** crash: a MapView mérete még null volt az első Compose update-nél, NPE, az app kilépett. Most megvárja a layoutot, a zoom 3–20, destroyAll release-kor.
- GPS fül magasság kb. **−1787 m** jó fused lat/lon mellett: a fused gyakran hibás ellipszoidot ad. `GpsAltitude.pick`: GNSS MSL, fused MSL, GNSS ellipszoid, fused ellipszoid; −430…9000 m-en kívül eldobva. Élő **Baro**, ha van nyomásszenzor.
- OSM **Használ** crash: Mapsforge zoom Animator szálon `GtlOsmMapView.repaint` → `CalledFromWrongThreadException`. A Compose szülő invalidate most a main threadre megy.
- Hiányzó, csonka vagy nem Mapsforge `.map` nem crash-loop a következő indításkor. `OsmMapFile.isReadable` a `mapsforge binary OSM` mágiát és az egyező header-méretet kéri. Sikertelen nyitás kikapcsolja a **Letöltött OSM térkép használatát**.
- OSM Térkép fül üres volt Magyarország `.map` mellett, ha a GPS a fájlon kívül volt (emulátor Mountain View). A kamera a térkép start/bounds pontját használja, a GPS-t csak a fájlon belül követi, és a csempét méret után kéri.
- OSM Térkép fül fehér volt zoomig: a Mapsforge offscreen rajzol, majd `onDraw`-ra vár a buffercseréhez. A Compose `AndroidView` ezt a rajzolást gyakran kihagyta (a szoftveres layer rontott). Most a `repaint` a Compose szülőket is invalidálja, az OSM MapView mérete megmarad GPS/Route/Compass alatt, és az első layout csempét hoz zoom nélkül.
- KMZ **Distance**: minden trackponton kumulatív táv (ExtendedData, méter), plusz `baro` (méter az íráskori QNH-val). Pause / Stop balloon: `distance=` a session mértékegységében.
- KMZ ikonok a letárolt vonalon (clampToGround); Stop az utolsó elfogadott pont; Pause nem takarja a Stopot. Balloon címe Start / Pause / Stop. Pause details nem üres (HTML + BalloonStyle); `temp=N/A`. Earth details: UTC `YYYY:MM:DD HH:MM:SS`, `temp=`, `lon=`, `lat=`, `Altitude:`, `Baro:` (íráskori QNH); Pause: `Speed:`, `duration=`, `distance=`; Stop: `Avg. Speed:`, `Max speed:`, `duration=`, `distance=`. KMZ ExtendedData `baro` és GPS `alt`; a `gx:coord` magasság 0. A látható vonal `LineString`. Nincs usage / lean a balloonban.
- Google Earth közeli zoom: a látható vonal terepre feszített `LineString` (magasság 0). A `gx:Track` GPS-Z-je Earth Androidon 3D vonal volt — 80 m-nél az utca mellett, 50 m alatt eltűnt.

## [2.0.5] — 2026-09-10

Play production track **23 (2.0.5)** (signed AAB). Fix cloud, bicycle usage, KMZ session stats, map broom.

### Added

- Settings **Show fix cloud** / **Pontfelhő** (off by default): pastel magenta dots of raw HUD fixes while you stand still, plus a CEP95 circle around the cloud centroid. Turning it on also turns on Show accuracy marker; turning it off only hides the cloud. Use GNSS only is independent. GPS tab shows n, RMS, CEP95, and median reported accuracy, plus a standing / moving / wait caption. Pauses while moving. Engine `FixCloudBuffer`: 120 point / 120 s window, 0.15 m duplicate floor. Not stored in SQLite or KMZ.
- Map usage silhouette (aircraft, boat, car, motorbike, bicycle, runner) at the live position; red, upright in portrait. A north marker stays on the map. Replaces the OSM start/now dots.
- Each `gps_events` row stores `usageType`. START / PAUSE / STOP KMZ balloons show `usage=` (Aircraft, Watercraft, Car, Motorbike, Bicycle, or Runner).
- Settings **Bicycle** usage: GNSS only on, Smooth recorded track off, Every good, map simplify off (3 m if you turn it on), 0.5 m store floor, 45 m accuracy gate — so a slow curve or plaza loop stays on the map.
- Saved tracks **Show on map** switches Settings to that session’s stored usage and draws Google Maps and OSM with those settings. After that, changing usage or sliders redraws the same log that way. Next Start uses the Settings that are then selected.
- Map **Clear map** broom (top left, idle with a saved track shown): takes the polyline off the map without deleting the SQLite log. Start or Show on map draws again. Logging still draws even if the map was cleared.

### Changed

- CEP95 circle is magenta; claimed-accuracy circle stays pale purple so the two rings stay distinct.
- OSM map start/now dots use a 2 m radius instead of 8 m.
- Settings: more space between usage type and Units.
- Settings rows, switches, chips, and sliders are shorter so the page still fits without a vertical scrollbar when both optional sliders are visible.
- Help (EN/HU) spells out CEP = Circular Error Probable / körkörös hibavalószínűség.
- KMZ STOP balloon: session duration (`Duration: 20 s` if 60 seconds or less, whole minutes if under 60 minutes, otherwise `HH:MM:SS`), plus labeled `Avg. speed:` and `Max. speed:` as whole numbers (metric `km/h`, imperial `mile/h`, ICAO `kt`) from `TrackStatsCalculator`. Instant `speed=` on START/PAUSE/STOP is unchanged.

### Magyar

- **Pontfelhő** (alapból ki): állóhelyen nyers GPS-pöttyök, magenta CEP95, világos lila jelentett pontosság. Bekapcsoláskor a pontossági jelzés is bekapcsol. A Csak GNSS független. GPS fül: n, RMS, CEP95, Reported, álló / mozgás / várakozás.
- Térkép-sziluett (repülő, hajó, autó, motor, kerékpár, futó) és északjelző.
- Minden GPS-pont `usageType`; KMZ balloon `usage=`.
- **Kerékpár** használati mód: GNSS be, simítás ki, minden jó, térkép-egyszerűsítés ki.
- Mentett track **Térképen**: a session usage-ét beírja a Beállításokba, és azzal rajzol (Google és OSM). A **Térképen** után a usage vagy a csúszkák váltása más módban mutatja ugyanazt a logot.
- Térkép **Térkép ürítése** seprő (bal felső, idle, ha mentett track látszik): leveszi a vonalat, a logot nem törli. Indítás vagy Térképen újra kirajzol.

## [2.0.4] — 2026-09-09

Play production track **22 (2.0.4)** (signed AAB). GNSS-only option, runner sports-watch logging, 0.5 m pedestrian store floor.

### Added

- Settings **Use GNSS only** (`GPS_PROVIDER` satellite chip; fused HIGH_ACCURACY fallback if that provider is disabled). Runner preset turns it on; vehicles stay fused.
- README section on how Runner logs like a sports watch (GNSS chip vs fused, no second Kalman flattening of 5–10 m on-road loops).
- README **How logging works**: full Start→Room→Map pipeline, why the red polyline is the stored tracklog.

### Changed

- Runner preset: GNSS only on, Smooth recorded track **off**, Every good, map simplify off. Vehicles keep fused + Kalman + Smart.
- Every-good duplicate floor is **0.5 m** for runner / pedestrian and **1 m** for vehicles.
- If GPS bearing is 0, curve detection can use heading from consecutive positions.
- Pedestrian Kalman (if you turn smoothing back on) adds extra position process noise so a 5 m road loop is not pulled onto the street.
- Settings page has no vertical scrollbar. Help Settings / Track logging document the new switch and Runner preset (EN/HU).
- README, GPS data-flow (EN/HU), SQLite schema, Kalman brief, and renewal report match GNSS-only, runner smoothing-off, 0.5 m pedestrian duplicate floor, and map-from-Room.

### Fixed

- Street-scale runner loops were flattened by fused location plus constant-velocity Kalman even at Low strength.

### Magyar

- Beállítások **Csak GNSS**: műholdchip; fused tartalék. Futó előbeállítás bekapcsolja.
- Futó: GNSS be, simítás ki, minden jó, térkép-egyszerűsítés ki. Ismétlődésküszöb 0,5 m.
- Az utcai léptékű futóhurkokat a fused hely + Kalman még Alacsony erősségnél is ellapította.

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

### Magyar

- Kalman a rögzítési láncban, csúszkák a chippek helyett, usage előbeállítás (repülőnél ICAO).
- Térkép-egyszerűsítés 1–20 m csúszka, csak a kirajzolt vonal. Álláskor a GPS kóborlása rögzül.

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
