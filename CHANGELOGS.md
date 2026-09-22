# Changelog

All notable changes to **GPS Track Logger** (`com.lkovari.mobile.apps.gtl`).

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning matches `versionName` **2.0.12** / `versionCode` **30** (minSdk 24, targetSdk 36).

Canonical history is this file. Play Console what’s-new: [docs/play-console/whatsnew.txt](docs/play-console/whatsnew.txt). How logging writes the Map polyline: [README-en.md — How logging works](README-en.md#how-logging-works) / [README-hu.md](README-hu.md#hogyan-működik-a-naplózás).

## [Unreleased]

### Fixed

- Fixes without a reported accuracy (including accuracy 0) are not stored. Recording waits for an accurate location and drops fixes older than 10 s. STOP uses the last accepted GPS time.
- GNSS-only no longer falls back to fused when GPS is off. Logging requires fine location. The HUD preview stops while the service is recording.
- Missing GPS altitude is stored as null (schema 5); GPX omits `<ele>`. Aircraft altitudes up to 20 000 m are kept. A missing speed is MOVE, not PAUSE.
- Turistautak map download is HTTPS only (no HTTP fallback), with redirect host checks and size limits.

### Magyar

- Pontosság nélküli (és 0-s) fix nem tárolódik. Felvételnél pontos helyre vár, és 10 s-nél idősebb pontot eldob. A STOP az utolsó elfogadott GPS-idő.
- Csak GNSS mellett GPS ki esetén nincs fused tartalék. Naplózáshoz precíz hely kell. A HUD preview leáll, amíg a service rögzít.
- Hiányzó GPS-magasság null a Roomban (séma 5); a GPX-ből kimarad az `<ele>`. A 20 000 m-ig tartó magasság bent marad. Hiányzó sebesség MOVE, nem PAUSE.
- A Turistautak térkép csak HTTPS-en jön (nincs HTTP tartalék), redirect-hoszt és méretplafonnal.

## [2.0.12] — 2026-09-21

Play production track **30 (2.0.12)** (signed AAB). Route Idle speeds, OSM Help, thinner magenta paths.

### Fixed

- Stop then a quick Start no longer closes the new recording. Deleting the live session is blocked; a database write error stops logging and shows it on the HUD.
- Show on map no longer overwrites the logging profile. Missing thermometer reads as —; while logging with no stored point the status is Waiting for GPS.
- Baro auto-calibration ignores 0 / NaN / out-of-range pressure. Invalid lat/lon is not stored. OSM map replace keeps the old file if the rename fails.

### Changed

- Route tab: while Idle (not logging), Speed and Avg. speed show 0 even if you are moving. After Start they keep live GPS speed and the session average. Map HUD is unchanged.
- Help: **OSM map options** accordion (same style as Turistautak options) lists Buildings, POI, Public transport, Highlight cycleways, Parks, and Terrain relief. It does not mention Turistautak.
- Turistautak **Emphasize paths** uses the same magenta as OSM Highlight cycleways (`#C4007A` / `#FF4FBF`). Cycleway and path highlights are thinner (halo 2.0 / core 1.1) so they stay conspicuous without a thick overlay.

### Magyar

- Stop után gyors Start nem zárja le az új felvételt. Az élő session törlése tiltva; adatbázis-hiba leállítja a naplózást, és a HUD jelzi.
- A Mutasd a térképen nem írja felül a naplózó profilt. Hiányzó hőmérő: —; felvétel 0 tárolt ponttal: Várakozás a GPS-re.
- A baro autokalibráció elutasítja a 0 / NaN / tartományon kívüli nyomást. Érvénytelen lat/lon nem tárolódik. OSM csere megtartja a régi fájlt, ha a rename elhasal.
- Útvonal fül: Idle-ben (nincs naplózás) a Sebesség és az Átlagsebesség 0, akkor is ha mozogsz. Indítás után az élő GPS-sebesség és a session átlaga. A térkép HUD nem változott.
- Súgó: **OSM térkép opciók** harmonika (ugyanolyan, mint a Turistautak opciók) Épületek, POI, Tömegközlekedés, Kerékpárutak kiemelése, Parkok, Domborzat. Nem említi a Turistautakot.
- Turistautak **Ösvénykiemelés** ugyanazt a magentát használja, mint az OSM Kerékpárutak kiemelése (`#C4007A` / `#FF4FBF`). A kerékpárút- és ösvénykiemelés vékonyabb (halo 2,0 / mag 1,1), feltűnő, de nem vastag.

## [2.0.11] — 2026-09-20

Play production track **29 (2.0.11)** (signed AAB). Offline OSM maps, layers, GPS recenter.

### Added

- Download OSM map: **Delete** on each downloaded region (confirm first). Deleting the map for the phone locale country, or the map currently in use, turns **Use downloaded OSM map** off so Map uses Google Maps.
- Settings → **OSM map** (only when **Use downloaded OSM map** is on): Buildings (default on), **POI** (off; shops, restaurants, parking, fuel from zoom 14 — not bus stops), Public transport (off; rail/tram/stations **and bus stops**), Highlight cycleways (on for Bicycle usage; magenta overlay from zoom 12; dedicated paths stay blue; changing usage resets this), Parks and protected areas (on), Terrain relief (off unless HGT files sit next to the `.map`). The same switches are on the Map tab layers button (same spot as Google layers). Toggling a switch redraws Mapsforge tiles without moving the camera. Official Mapsforge files for any country only store dedicated `highway=cycleway` (not `cycleway:lane` on the carriageway); base cycleways now draw in blue so they are visible without the highlight. Terrain relief stays disabled on official Mapsforge downloads (Hungary, Switzerland, and the rest); enable it by placing `.hgt` / `.hf2` files next to the map or in `hills/`. About names optional OpenStreetMap use (ODbL); the OSM Map tab shows **© OpenStreetMap**.
- Map **My location** (top left: cyan GPS crosshair, same circle as the broom): recenters Google Maps and OSM on the GPS fix without changing zoom. Always shown; dimmed without a fix. Under the broom when a saved track is on the map. A live GPS fix sits on a cyan GNSS reticle around the usage silhouette.

### Changed

- Settings: a small gap between the two usage-type rows.
- **Use downloaded OSM map** is off and disabled until a region is downloaded. Turning it off stays off; Map shows Google Maps.
- Download OSM map: **Use**, **Delete**, and **Download** are smaller compact buttons.
- Menu **Download Offline map** (was Download OSM map). Turistautak.hu is first. A downloaded map is **Can Use** or **In Use**; only one OSM region or Turistautak can be In Use. Tapping **In Use** returns to Google Maps. Settings **OSM map** only while an OSM region is In Use; **Turistautak.hu** only while that map is In Use.

### Fixed

- Downloaded OSM map showed a blank canvas (marker, scale bar, and zoom still worked). The custom Mapsforge theme was opened as `/assets/mapsforge/gtl.xml`, which is the JAR resource prefix for `MapsforgeThemes.DEFAULT`, not an Android `AssetManager` path. Theme parse runs off the UI thread, so the fallback to DEFAULT never ran and tiles stayed empty. Theme now loads `mapsforge/gtl.xml`.
- OSM idle camera snapped back to GPS on every Compose update, so you could not pan away from your location (Google Maps already allowed free pan until Start). Follow is only while logging (`MapCameraMode`).
- Dedicated cycleways at Ceglédi út × Üllői út are in the Hungary `.map` (`highway=cycleway`), but the base theme drew them `#F0F0FF` / 0.8 px on a `#F8F8F8` canvas, so they vanished beside the yellow primary. On-road `cycleway:right=lane` is not stored in official Mapsforge tag-mapping. Base cycleways now draw blue; highlight stays magenta. Bus stops moved from POI to Public transport. English Settings label is **POI**, not only “Points of interest”.
- OSM layer switches did nothing: `gtl.xml` listed the base stylemenu layer before the overlay layers it references, so Mapsforge resolved every `<overlay>` to null and parsed the theme with an empty category set. Categorized rules (buildings, POI, transit, magenta cycleways, parks) never drew; only the uncategorized base (roads, blue `highway=cycleway`, water, forest) showed. Overlay layers are now defined first, and `OsmRenderCategories` still enables requested ids if an overlay lookup is missing.

### Magyar

- OSM térkép letöltése: **Törlés** minden letöltött régión (előtte megerősítés). Ha a törölt térkép országa a telefon locale-je, vagy épp ezt a térképet használod, a **Letöltött OSM térkép használata** kikapcsol, a Térkép Google Térképre vált.
- Beállítások → **OSM térkép** (csak **Letöltött OSM térkép használata** mellett): Épületek (alapból be), **POI** (ki; boltok, éttermek, parkolók, kutak 14-es zoomtól — nem buszmegálló), Tömegközlekedés (ki; vasút/villamos/állomás **és buszmegálló**), Kerékpárutak kiemelése (Kerékpár usage-nél be; magenta overlay 12-es zoomtól; a külön kerékpárút kék marad; usage-váltás visszaállítja), Védett terület / park (be), Domborzat (ki, kivéve ha HGT fájlok vannak a `.map` mellett). Ugyanezek a kapcsolók a Térkép fül réteg gombján is (ugyanott, ahol a Google rétegek). A kapcsoló a Mapsforge csempét újrarajzolja, a kamera nem mozog. A hivatalos Mapsforge fájlban bármely országnál csak a külön `highway=cycleway` van (az úttesti `cycleway:lane` nincs); az alap kerékpárút most kék, kiemelés nélkül is látszik. A Domborzat a hivatalos Mapsforge-letöltéseken (Magyarország, Svájc és a többi) ki marad; `.hgt` / `.hf2` a térkép mellett vagy `hills/` mappában kapcsolja be. A Névjegy leírja az opcionális OpenStreetMap-használatot (ODbL); az OSM Térkép fülön **© OpenStreetMap**.
- Térkép **Saját hely** (bal felső: cián GPS-kereszt, ugyanolyan kör, mint a seprő): Google és OSM a GPS-fixre centrál, a zoomot nem változtatja. Mindig látszik; GPS nélkül halvány. Mentett tracknél a seprő alatt. Élő GPS-fix cián GNSS-retikuluson ül, a usage sziluett körül.

- Beállítások: kis rés a két használati-mód sor között.
- A **Letöltött OSM térkép használata** letöltésig ki van kapcsolva és nem állítható. Kikapcsolva kikapcsolva marad; a Térkép Google Térképet mutat.
- OSM térkép letöltése: a **Használ**, **Törlés** és **Letöltés** gombok kisebbek.
- Menü **Offline térkép letöltése** (korábban OSM térkép letöltése). A Turistautak.hu felül van. A letöltött térkép **Használható** vagy **Használatban**; egyszerre egy OSM-régió vagy a Turistautak lehet Használatban. A **Használatban** Google Térképre vált. Beállítások **OSM térkép** csak OSM-régió Használatban; **Turistautak.hu** csak akkor, ha az van Használatban.

- Letöltött OSM térkép üres vászon volt (jelölő, lépték, zoom megvolt). A saját Mapsforge téma `/assets/mapsforge/gtl.xml` úton nyílt — ez a `MapsforgeThemes.DEFAULT` JAR-előtagja, nem Android `AssetManager` útvonal. A téma parse háttérszálon fut, ezért a DEFAULT tartalék nem futott, a csempék üresek maradtak. Most `mapsforge/gtl.xml` töltődik.
- OSM idle kamera minden Compose update-nél visszaugrott a GPS-re, ezért nem lehetett elhúzni a helyedtől (a Google Térkép idle-ben már engedte). Követés csak naplózáskor (`MapCameraMode`).
- A Ceglédi út × Üllői út külön kerékpárútjai benne vannak a Hungary `.map`-ben (`highway=cycleway`), de az alap téma `#F0F0FF` / 0,8 px volt `#F8F8F8` alapon, ezért a sárga főút mellett eltűntek. Az úttesti `cycleway:right=lane` nincs a hivatalos Mapsforge tag-mappingben. Az alap kerékpárút most kék; a kiemelés magenta marad. A buszmegálló a Tömegközlekedéshez került. Az angol Beállítások felirata **POI**, nem csak “Points of interest”.
- OSM-rétegkapcsolók nem változtattak semmit: a `gtl.xml` a base stylemenu réteget az overlay-ek előtt listázta, a Mapsforge minden `<overlay>`-t null-ra oldott, üres kategóriahalmazzal parse-olt. A kategóriás szabályok (épületek, POI, tömegközlekedés, magenta kerékpárút, parkok) nem rajzolódtak; csak az alaptérkép (utak, kék `highway=cycleway`, víz, erdő). Az overlay rétegek most előbb vannak definiálva; hiányzó overlay-nél az `OsmRenderCategories` a kért id-t akkor is engedélyezi.

## [2.0.10] — 2026-09-19

Play production track **28 (2.0.10)** (signed AAB). Auto-calibrate baro at Start.

### Added

- Settings → Baro → **Auto-calibrate at start** (default **on**, shown only with a pressure sensor). After Start, once two **consecutive** GPS fixes that pass the normal accuracy/satellite filter agree on altitude within `MaxAltitudeJitterMeters` (**15 m**), the service triggers the same calculation as **Calibrate from GPS** (`BaroAltitude.offsetHpa` from that fix's altitude and the live pressure reading), storing the offset once per recording — no more standing still and tapping Calibrate before every ride. Root cause it addresses: without calibration, baro defaults to ISA 1013.25 hPa, which is commonly 40–90 m off from GPS whenever the real local QNH differs (confirmed from a field KMZ where GPS read ~143 m and uncalibrated baro read ~87 m — a ~56 m gap matching an estimated real QNH near 1020 hPa). The two-fix agreement check is required, not optional: an early build of this feature calibrated off the very first passing fix and, in a second field test, locked in a ~41 m error for an entire ~4.5 minute recording after that first fix's GPS altitude spiked from ~144 m to 185 m for a single sample (GPS vertical accuracy converges slower than horizontal, and Android's reported accuracy figure only covers horizontal error). Toggle off to keep the old manual-only behaviour; **Calibrate from GPS** and **Reset baro** are unchanged and still available — and remain the only recourse if a mid-ride pressure artifact (pocket, bag, car door) throws baro off after the one-time auto-calibration. New pure gate `BaroAltitude.autoCalibrateEligible` (engine, unit-tested, now also requires a corroborating previous fix within `MaxAltitudeJitterMeters`) and `TrackingForegroundService.maybeAutoCalibrateBaro` (app). Full write-up: [README-en.md — Barometric altitude (Baro)](README-en.md#barometric-altitude-baro).

### Magyar

- Beállítások → Baro → **Automatikus kalibrálás induláskor** (alapból **be**, csak nyomásszenzornál látszik). Indítás után, amint két **egymást követő**, a szokásos pontosság-/műholdszűrőn átment GPS-fix 15 méteren belül egyezik a magasságban (`MaxAltitudeJitterMeters`), a szolgáltatás lefuttatja ugyanazt a számítást, mint a **Kalibrálás GPS-ből** (`BaroAltitude.offsetHpa` a fix magasságából és az aktuális nyomásból), és eltárolja az offsetet — session-enként egyszer. Nem kell többé megállni és Kalibrálást nyomni minden túra előtt. Ezt a hibát orvosolja: kalibrálás nélkül a baro az ISA 1013,25 hPa alapértéket használja, ami a GPS-hez képest gyakran 40–90 m eltérést okoz, ha a valós helyi QNH ettől eltér (egy terepi KMZ alapján igazolva, ahol a GPS ~143 m-et, a kalibrálatlan baro ~87 m-et mutatott — ez a ~56 m-es rés egy ~1020 hPa körüli valós QNH-nak felel meg). A két fix egyezésének megkövetelése nem elhagyható lépés: a funkció egy korábbi verziója közvetlenül az első átmenő fixről kalibrált, és egy második terepi tesztben egy teljes, ~4,5 perces felvétel egészére ~41 m-es hibát zárt be, miután az első fix GPS-magassága egyetlen mintára ~144 m-ről 185 m-re ugrott (a GPS függőleges pontossága lassabban áll be, mint a vízszintes, az Android jelentett pontosság-értéke pedig csak a vízszintes hibát fedi le). A kapcsoló kikapcsolásával a régi, csak kézi működés marad; a **Kalibrálás GPS-ből** és a **Baro visszaállítás** változatlan — és ezek maradnak az egyetlen megoldás, ha egy menet közbeni nyomás-műtermék (zseb, táska, autóajtó) az egyszeri automatikus kalibrálás után billenti ki a baro-t. Új tiszta állapotfüggvény: `BaroAltitude.autoCalibrateEligible` (engine, unit teszttel, mostantól egy megerősítő előző fixet is megkövetel `MaxAltitudeJitterMeters`-en belül), és `TrackingForegroundService.maybeAutoCalibrateBaro` (app). Részletek: [README-hu.md — Barometrikus magasság (Baro)](README-hu.md#barometrikus-magasság-baro).

## [2.0.9] — 2026-09-18

Play production track **27 (2.0.9)** (signed AAB). Settings Appearance / Recording / Baro cards.

### Changed

- Settings groups map and recording switches into **Appearance**, **Recording**, and **Baro** cards (Help-style accordion; several sections can stay open). Usage type and units stay at the top. Baro only if the phone has a pressure sensor.

### Magyar

- A Beállítások térkép- és rögzítéskapcsolói **Megjelenés**, **Rögzítés** és **Baro** kártyákba kerültek (Súgó-szerű harmonika; több szakasz nyitva maradhat). A használat és a mértékegység fent marad. Baro csak nyomásszenzornál.

## [2.0.8] — 2026-09-14

Play production track **26 (2.0.8)** (signed AAB). KMZ baro at share-time QNH; larger Earth icons.

### 2026-09-14

KMZ baro at share-time QNH; larger Google Earth Start / Pause / Stop icons.

### Changed

- KMZ Start / Pause / Stop balloons and `gx:Track` ExtendedData `baro` use `BaroAltitude.displayedMeters` from stored `pressureHpa` at **share time** (current Settings QNH and GPS-calibration offset), same source as the Saved-tracks / Route elevation dashed line. If that recompute is more than 1500 m from the point’s GPS altitude (impossible vs the track, e.g. ~2000 m next to 140 m GPS), the stored insert-time `baroAltitude` is used instead, or baro is omitted. Insert-time `baroAltitude` stays on the SQLite row. Missing pressure still falls back to stored baro or `-`. GPS `Altitude:` / ExtendedData `alt` unchanged. Re-share KMZ after this change.
- README EN/HU: table of contents; **Barometric altitude (Baro)** (ISA / QNH formula, Calibrate from GPS, insert vs display, 1500 m guard); GNSS skyplot overlay labels (SKYPLOT / In view / Used / L5, N E S W).
- Google Earth play / pause / stop `IconStyle` scale **0.6 → 0.8** so the icons are easier to tap without covering the track. App Map S/E markers are unchanged.

### Magyar

- A KMZ Start / Pause / Stop balloon és a `gx:Track` ExtendedData `baro` a letárolt `pressureHpa`-ból számol **megosztáskor** (`BaroAltitude.displayedMeters`, jelenlegi Beállítások QNH és GPS-kalibrációs offset), ugyanúgy, mint a mentett / Útvonal magasságprofil szaggatott vonala. Ha az újraszámolt érték több mint 1500 m-re van a pont GPS-magasságától (lehetetlen, pl. ~2000 m a 140 m-es GPS mellett), a letárolt íráskori `baroAltitude` marad, vagy a baro kimarad. A SQLite `baroAltitude` az íráskori QNH. Nyomás nélkül a letárolt baro vagy `-`. A GPS `Altitude:` / `alt` változatlan. A javítás után oszd meg újra a KMZ-t.
- README EN/HU: tartalomjegyzék; **Barometrikus magasság (Baro)** (ISA / QNH képlet, Kalibrálás GPS-ből, írás vs megjelenítés, 1500 m-es őr); GNSS skyplot sarkok (SKYPLOT / Látható / Használatban / L5, N E S W).
- Google Earth play / pause / stop `IconStyle` scale **0.6 → 0.8**: könnyebb koppintás, a tracket nem takarja. Az app Térkép S/E jelölői változatlanok.

## [2.0.7] — 2026-09-13

Play production track **25 (2.0.7)** (signed AAB). Baro from QNH; Calibrate from GPS.

### Changed

- Live and profile barometric altitude use `SensorManager.getAltitude(qnh, pressure − offset)` (`AndroidBaroAltitude`). Default QNH is `PRESSURE_STANDARD_ATMOSPHERE` (1013.25 hPa). The Settings QNH slider is shown only when the phone has a pressure sensor. **Calibrate from GPS** stores a DataStore pressure offset (±10 hPa) so the dashed baro line can match a trusted GPS altitude; it does not overwrite METAR QNH. Reset baro clears the offset. Help names METAR, ATIS, and airport weather as the QNH source (no URL), and describes Map HUD fields plus GPS / Baro. KMZ balloons and ExtendedData show `-` when baro is missing (`temp=N/A` unchanged). GPS altitude is unchanged.
- Play listing `map.png` and `settings.png` recaptured 2026-09-13 as 1080×2160 RGB PNG (no alpha, 2:1; full device frame scaled to Play long-edge ≤ 2× short-edge, no UI cropped): idle Map HUD with S/E track; Settings QNH Calibrate / Reset through recording density. Logging HUD Map and the feature graphic still wait on dark tiles.

## [2.0.6] — 2026-09-12

Play production track **24 (2.0.6)** (signed AAB). Skyplot, elevation profile, GPX, compass rose, QNH, OSM robustness.

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
- Play listing phone shots recaptured 2026-09-12 as 1080×1920 RGB PNG (no alpha, 9:16; status bar and home indicator cropped, bottom tabs kept): `gps-idle.png` (skyplot), `route.png` (elevation), `map.png` (S/E track), `compass.png` (MAG rose), `settings.png` (QNH, Run/Hike), `saved-tracks.png` (Elevation). HUD Map listing and the feature graphic still wait on dark tiles.

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
- Play listing telefonképek újra véve 2026-09-12, 1080×1920 RGB PNG (nincs alfa, 9:16; status bar és home indicator levágva, alsó fülek megmaradnak): `gps-idle.png` (skyplot), `route.png` (magasságprofil), `map.png` (S/E track), `compass.png` (MAG rózsa), `settings.png` (QNH, Fut/túra), `saved-tracks.png` (Magasság). A HUD-os Térkép listing és a feature graphic a sötét csempére vár.
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
- Map usage silhouette (aircraft, boat, car, motorbike, bicycle, Run/Hike) at the live position; red, upright in portrait. A north marker stays on the map. Replaces the OSM start/now dots.
- Each `gps_events` row stores `usageType`. START / PAUSE / STOP KMZ balloons show `usage=` (Aircraft, Watercraft, Car, Motorbike, Bicycle, or Run/Hike).
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
- Térkép-sziluett (repülő, hajó, autó, motor, kerékpár, Fut/túra) és északjelző.
- Minden GPS-pont `usageType`; KMZ balloon `usage=`.
- **Kerékpár** használati mód: GNSS be, simítás ki, minden jó, térkép-egyszerűsítés ki.
- Mentett track **Térképen**: a session usage-ét beírja a Beállításokba, és azzal rajzol (Google és OSM). A **Térképen** után a usage vagy a csúszkák váltása más módban mutatja ugyanazt a logot.
- Térkép **Térkép ürítése** seprő (bal felső, idle, ha mentett track látszik): leveszi a vonalat, a logot nem törli. Indítás vagy Térképen újra kirajzol.

## [2.0.4] — 2026-09-09

Play production track **22 (2.0.4)** (signed AAB). GNSS-only option, Run/Hike sports-watch logging, 0.5 m pedestrian store floor.

### Added

- Settings **Use GNSS only** (`GPS_PROVIDER` satellite chip; fused HIGH_ACCURACY fallback if that provider is disabled). Run/Hike preset turns it on; vehicles stay fused.
- README section on how Run/Hike logs like a sports watch (GNSS chip vs fused, no second Kalman flattening of 5–10 m on-road loops).
- README **How logging works**: full Start→Room→Map pipeline, why the red polyline is the stored tracklog.

### Changed

- Run/Hike preset: GNSS only on, Smooth recorded track **off**, Every good, map simplify off. Vehicles keep fused + Kalman + Smart.
- Every-good duplicate floor is **0.5 m** for Run/Hike / pedestrian and **1 m** for vehicles.
- If GPS bearing is 0, curve detection can use heading from consecutive positions.
- Pedestrian Kalman (if you turn smoothing back on) adds extra position process noise so a 5 m road loop is not pulled onto the street.
- Settings page has no vertical scrollbar. Help Settings / Track logging document the new switch and Run/Hike preset (EN/HU).
- README, GPS data-flow (EN/HU), SQLite schema, and Kalman brief match GNSS-only, Run/Hike smoothing-off, 0.5 m pedestrian duplicate floor, and map-from-Room.

### Fixed

- Street-scale Run/Hike loops were flattened by fused location plus constant-velocity Kalman even at Low strength.

### Magyar

- Beállítások **Csak GNSS**: műholdchip; fused tartalék. Fut/túra előbeállítás bekapcsolja.
- Fut/túra: GNSS be, simítás ki, minden jó, térkép-egyszerűsítés ki. Ismétlődésküszöb 0,5 m.
- Az utcai léptékű Fut/túra hurkokat a fused hely + Kalman még Alacsony erősségnél is ellapította.

## [2.0.3] — 2026-09-08

Play production track **21 (2.0.3)** (signed AAB). Kalman smoothing on stored points, Settings sliders, aircraft ICAO units.

### Added

- Constant-velocity **Kalman** smoother in `:engine` (`KalmanTrackFilter`), applied before SQLite so Route, Map, and KMZ share the same path. HUD accuracy circle stays on the raw fused fix.
- Settings: **Smooth recorded track**, **Smoothing strength** (Low–High slider), **Hold still when stopped**, **Recording density** (Smart–Every good slider). English and Hungarian.
- Usage presets write smoothing, density, map-simplify, and units in one DataStore edit (Run/Hike: Low + every good + DP off; motorbike: Medium + Smart + 6 m DP; aircraft: High + 15 m DP + ICAO).
- Help **Settings** section: presets, sliders, and ICAO for aircraft.
- Engine tests for highway RMSE, roundabout, figure-8, zigzag, stationary lock, jump re-init, usage defaults, and slider interpolation.

### Changed

- **Simplify track on map** tolerance is a 1–20 m slider (1 m steps), not chips. Defaults follow usage (not a global 19.5 m). Old 19.5 m sentinel migrates once when Kalman keys are first written. Display-only: KMZ and odometer keep every stored point.
- Smooth recorded track and recording density use continuous sliders (Low–High and Smart–Every good). Usage presets set the slider positions.
- Aircraft usage defaults to ICAO units (knots, NM, feet); other usages default to metric.
- Smart density: Run/Hike uses half of the 2014 speed bands (still half again in a curve, min 1 m). Vehicles keep the existing bands. Intermediate density mixes Smart spacing with Every good.
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
- Settings: usage (aircraft, watercraft, car, motorbike, Run/Hike), metric/imperial, fix filters, OSM offline map, track simplification, last track on map.
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
