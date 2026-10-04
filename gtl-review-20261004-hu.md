# GTL kódreview — 2026-10-04

**Projekt:** GPS Track Logger (GTL), Kotlin + Jetpack Compose, `versionName` 2.0.19, `versionCode` 37, minSdk 24, targetSdk 36  
**Hatáskör:** a teljes fa (`app/`, `engine/`), a staged sötét-téma munkával együtt (`Theme.kt`, `AppTheme`, `CivilTwilight`, `NightRenderTheme`, `map_style_night.json`).  
**Szempont:** modern Android (Android 14–16, edge-to-edge, FGS-szabályok), Kotlin coroutines / Flow, Compose, Room, geodézia (magassági datum, vetület, koordináta-formátum), Google Maps (maps-compose) és Mapsforge (OSM, Turistautak).  
**Súlyosság:** Kritikus → Magas → Közepes → Alacsony. Csak a forrásban ellenőrzött hibák; ahol a következmény eszközön még nincs bizonyítva, a tétel **„eszközön ellenőrizendő”** jelölést kap.  
**Előzmény:** [gtl-review-20260920-hu.md](gtl-review-20260920-hu.md). Onnan nyitva maradt: Kz5, A1, A3 — ezek itt újra szerepelnek.

Minden tétel: forrás (fájl, sor), mi a baj, miért baj, javasolt javítás, miért oldja meg.

> **Sorszámok:** a fa 2026-10-04 13:05-ös állapota. A `GtlViewModel.kt` a review alatt is változott (párhuzamos munka), ezért a sorszám pár sort elcsúszhat; a függvénynevek a mérvadók.

---

## Rövid rangsor

| # | Súly | Tétel |
| --- | --- | --- |
| — | Kritikus | Nincs ellenőrzött kritikus hiba ebben a körben |
| M1 | Magas | A `uiState` teljes újraszámolása a fő szálon, szenzorütemben, fájl-I/O-val, háttérben is — **kód kész 2026-10-04, telefonon még nem ellenőrzött** |
| M2 | Magas | Mentett útvonalak: minden pontírás az **összes** session összes pontját újratölti — **kész 2026-10-04** |
| M3 | Magas | Magassági datum keveredik (MSL ↔ ellipszoid, ~44 m Magyarországon), és elavult GNSS-magasság kerül friss fixre — **kód kész 2026-10-04, telefonon még nem ellenőrzött** |
| M4 | Magas | `START_STICKY` újraindítás: védtelen `startForeground(LOCATION)` háttérből — **kód kész 2026-10-04, telefonon még nem ellenőrzött** |
| Kz1 | Közepes | „Csak GNSS” + kikapcsolt GPS a Startnál: bekapcsolás után sem jön több pont — **kód kész 2026-10-04, telefonon még nem ellenőrzött** |
| Kz2 | Közepes | GPX / KML koordináta tudományos jelöléssel (`-5.0E-4`) a 0° hosszúság / szélesség közelében — **kész 2026-10-04** |
| Kz3 | Közepes | Világos témában Android 15+ alatt láthatatlanok az állapotsor ikonjai — **kód kész 2026-10-04, telefonon még nem ellenőrzött** |
| Kz4 | Közepes | Térképfrissítés minden rekompozíciónál: teljes polyline-csere, `redrawLayers`, ős-invalidálás — **részben kész 2026-10-04, az ős-invalidálás megmaradt** |
| Kz5 | Közepes | A felvétel szűrője a Start pillanatában lefagy *(2026-09-20 óta nyitott)* — **kész 2026-10-04, a viselkedés szándékosan marad, a Beállítások mondja** |
| A1 | Alacsony | Room `exportSchema = false` *(2026-09-20 óta nyitott)* — **kész 2026-10-04** |
| A2 | Alacsony | A Tuhu worker az OSM work-taget viseli *(2026-09-20 óta nyitott)* |
| A3 | Alacsony | Éjszakai térképtéma-váltás a fő szálon: XML átszínezés, fájlírás, csempe-purge |
| A4 | Alacsony | Rekurzív Douglas–Peucker: O(n²) legrosszabb eset és mély verem |
| A5 | Alacsony | `gps_events`: nincs `(sessionId, timestamp)` összetett index — **kész 2026-10-04** |
| A6 | Alacsony | Automatikus téma hely nélkül mindig világos, a rendszer sötét módját nem nézi |

---

## Kritikus

Ebben a körben nincs ellenőrzött kritikus hiba. A 2026-09-20-i K1 és K2 (Start/Stop verseny, élő session törlése) javítva maradt: a `recordingLock` és a `recordingGeneration` a `TrackingForegroundService`-ben a helyén van.

A ma reggeli **indítási crash-loop** (`ClassCastException … MarginLayoutParams`) nem a repó hibája volt: egy kísérleti, nem commitolt build tette a `ComposeView`-ra a nyers `ViewGroup.LayoutParams`-ot. A jelenlegi forrásból épített app indul. Lásd a *Megfigyelések* részt.

---

## Magas

### M1. A `uiState` teljes újraszámolása a fő szálon, szenzorütemben, fájl-I/O-val, háttérben is

- **Forrás:**
  - `app/.../viewmodel/GtlViewModel.kt`, **467–608** (`uiState = combine(…) { … }.stateIn(viewModelScope, …)`), benne **513** `TrackStatsCalculator.compute`, **536** `DouglasPeucker.keepIndices`, **567** `OsmMapFile.isReadable`, **482** és **590** `tuhuMapStore.downloadedFile()` / `OsmHillshading.available`
  - `GtlViewModel.kt` **156**: `GtlUiState.osmHillshadingAvailable` getter → `OsmHillshading.available(osmFile)` (fájlnyitás + mappalistázás) minden olvasáskor; a `MapPane` kompozíció közben olvassa
  - `app/.../service/TrackingForegroundService.kt` **154–171**: az iránytű minden mintája `trackingState.update`
  - `app/.../data/sensor/SensorSources.kt` **133**: `CompassSource` `SENSOR_DELAY_UI` (~16 Hz)
  - `app/.../MainActivity.kt` **25–31**: `lifecycleScope.launch { viewModel.uiState.collect { … } }`
- **Probléma:** A `combine` a `live` (`trackingState.state`) minden változására lefut. Felvétel közben a `live` az iránytű ütemében (~16 Hz), a gyorsulásmérő és a nyomás ütemében is változik, nem csak GPS-fixenként. Minden lefutás a **teljes** tracken: `toSamples`, statisztika, DP, `SpeedTrack.runs`, `SpeedSeries`. Ugyanitt **három fájlnyitás és fejlécolvasás**, plusz mappalistázás (`hasElevationFiles`) történik. A `viewModelScope` dispatcher-e `Dispatchers.Main.immediate`, `flowOn` nincs, tehát mindez a fő szálon fut. A `MainActivity` gyűjtője a `lifecycleScope`-ban nem `repeatOnLifecycle`-ben fut, ezért a `WhileSubscribed(5_000)` soha nem engedi el a folyamot. **Háttérben is fut**, amíg az Activity él.
- **Miért probléma:** Egy 3 órás felvételen (~10 000 pont, 1 Hz) másodpercenként kb. 16-szor fut O(n) számítás, a DP legrosszabb esetben O(n²), plusz I/O. Mindez a fő szálon és zsebben is. Következmények:
  - akkumulátor- és CPU-terhelés;
  - előtérben akadozó térkép (jank), szélsőséges esetben ANR;
  - minden új `GtlUiState` rekompozíciót és térképfrissítést indít (lásd Kz4).

  A StrictMode `DiskReadViolation`-t jelezne. Ez a legnagyobb, ma is jelen lévő erőforrás-hiba a kódban.
- **Javítás:**
  1. Szeleteld szét az állapotot. A track-függő részt (`events` → statisztika, DP, `SpeedTrack`, `SpeedSeries`) külön `StateFlow`-ba tedd, ami **csak az `events` és a releváns beállítások** változására számol: `distinctUntilChanged`, `flowOn(Dispatchers.Default)`.
  2. A fájlállapotot (`osmFile` olvashatósága, hillshading, Tuhu-fájl) a letöltés/kiválasztás eseményére számold egyszer, `Dispatchers.IO`-n. Ne getterben és ne a `combine`-ban.
  3. A `GtlUiState.osmHillshadingAvailable` legyen tárolt mező, ne számolt getter.
  4. A `MainActivity` gyűjtője `repeatOnLifecycle(STARTED)` legyen, vagy szűnjön meg. A `content.invalidate()` kerülőút helyett elég a splash `setKeepOnScreenCondition`.
  5. Az iránytű / szenzor állapota külön flow legyen. A HUD és az iránytű fül olvassa közvetlenül, ne a teljes `uiState`-en keresztül.
- **Miért oldja meg:** A drága számítás a track változásához kötődik (~1 Hz), nem a szenzorütemhez (~16 Hz), és nem a fő szálon fut. Az I/O eseményenként egyszer fut, háttérben pedig nincs gyűjtő, ami a számítást életben tartaná.
- **Állapot (2026-10-04):** kész a kódban. A trackrajz `TrackPresentation` kulcson, `Dispatchers.Default`-on fut; az iránytű, a dőlés, a baro és a hőmérséklet a `viewModel.live`-ot olvassa. A fájlolvashatóság és a hillshading tárolt mező, `Dispatchers.IO`-n. A splash a `settingsLoaded` Eagerly folyamot nézi; a `content.invalidate()` gyűjtő megszűnt. A rögzítés a szolgáltatásban marad, háttér és képernyőzár nem állítja le. Telefonos próba nem futott.

---

### M2. Mentett útvonalak: minden pontírás az összes session összes pontját újratölti

- **Forrás:** `app/.../viewmodel/GtlViewModel.kt`, **212–231** (`savedTrackCards`).
- **Probléma:** A kártyákhoz sessionönként külön `observeEvents(session.id)` Room-`Flow` indul, a `combine` ezeket fogja össze. A Room invalidációja **tábla szintű**: egy `gps_events` INSERT **mindegyik** flow-t újrafuttatja. Felvétel közben, nyitott Mentett útvonalak képernyőnél másodpercenként N lekérdezés fut, mindegyik a session összes pontját tölti be (`SELECT *`, 21 oszlop), és mindegyikre újraszámol `SavedTrackCards.from`. Csak a számolás megy a `Dispatchers.Default`-ra, az entitás-listák mind a memóriában vannak.
- **Miért probléma:** 200 track × 5 000 pont már ~1 M `GpsEventEntity` objektum a heapen. GC-nyomás, lassú lista, kis RAM-ú telefonon `OutOfMemoryError`. A tünet a hosszú archívumú, régi felhasználóknál jön elő, vagyis a leghűségesebbeknél.
- **Javítás:** A roadmap 8. tétele (`track_sessions` bővítése) itt hibajavítás.
  - Room-migráció 6→7: `distanceMeters`, `durationMs`, `avgSpeed`, `maxSpeed` (és a bélyegképhez egy kis, ritkított polyline, pl. 64 pont, encoded polyline szövegként). Ezeket Stopkor írd.
  - A lista egyetlen `observeSessions()` lekérdezést futtasson.
  - Régi sessionökre egyszeri háttér-backfill (WorkManager), ami session-szintű tranzakcióban tölti fel az oszlopokat.
- **Miért oldja meg:** A lista O(sessionök) sort olvas, nem O(összes pont)-ot. Egy pontírás nem futtatja újra a többi session kártyáját, csak az élőét (vagy azt sem, ha a kártyát Stopkor írod).

**Állapot (2026-10-04):** kész. Séma 7, `exportSchema = true`, a `schemas/` a gitben. Leállításkor a kártya (`distanceMeters`, `durationMs`, `avgSpeedMps`, `maxSpeedMps`, `previewPolyline`) egy tranzakcióban íródik. A lezárt trackek a session-sorból jönnek. Csak a nyitott session pontlistája él. A backfill WorkManager, felvétel közben vár, magasságot nem ír. `GtlDatabaseMigrationTest` 6→7 sikeres: a pont megmarad, az új oszlopok nullok, az összetett index létrejön.

---

### M3. Magassági datum keveredik, és elavult GNSS-magasság kerül friss fixre

- **Forrás:**
  - `app/.../data/location/LocationClient.kt`, **109–113** (`lastGnss` frissítése), **148** (`withTrustedAltitude(location, lastGnss)`), **188–209**
  - `engine/.../GpsAltitude.kt`, **11–20** (`pick`: GNSS-MSL → fused-MSL → GNSS-ellipszoid → fused-ellipszoid)
- **Probléma:**
  1. **Datum-keverés.** A `pick` fixenként azt választja, ami épp van. Ha az MSL magasság (`LocationCompat.hasMslAltitude`, Android 14+, chipfüggő) csak egyes fixeken érhető el, a tárolt magasság fixenként MSL (geoid feletti) és **ellipszoid** (WGS84) között ugrál. Magyarországon a geoidunduláció (EGM2008) N ≈ +40…+46 m, tehát a lépcső **~44 m**.
  2. **Elavult GNSS.** Fused módban a `lastGnss` az utolsó GPS-provider fix, **koréllenőrzés nélkül**. Alagút, épület vagy GPS-kiesés után percekkel régebbi GNSS-magasság (és elsőbbséggel annak MSL-je) kerül a friss fused pozícióra.
- **Miért probléma:**
  - A magasságprofil lépcsős lesz.
  - Az emelkedés- és ereszkedésösszeg felfújódik: minden váltás ±44 m.
  - A KMZ `altitudeMode=absolute` MSL-t vár, ezért a vonal 44 m-rel a terep fölött vagy alatt lebeg.
  - A GPX `<ele>`-t a Garmin, a Komoot és a Strava MSL-ként értelmezi.
  - A baro autokalibráció (`maybeAutoCalibrateBaro`) is a hibás GPS-magassághoz kalibrál, ezért a hiba a baro-vonalba is átkerül.
- **Javítás:**
  - Egységesen **MSL**-t tárolj. Ha csak ellipszoid-magasság van, konvertáld: Android 14+ `android.location.altitude.AltitudeConverter` (geoid-modell beépítve); régebbi eszközre egy kis EGM96/EGM2008 rács az `engine`-ben (pl. 1°-os rács, bilineáris interpoláció, ~30 KB).
  - Sessiononként (vagy pontonként egy flaggel) rögzítsd a datumot.
  - A `lastGnss`-t csak akkor használd, ha `|lastGnss.elapsedRealtimeNanos − location.elapsedRealtimeNanos| ≤ 2 s`.
  - Teszt: szintetikus fixsor váltakozó MSL/ellipszoid adattal → a tárolt sor folytonos.
- **Miért oldja meg:** Egy session minden pontja egy referenciafelülethez viszonyul, ezért nincs 44 m-es ugrás, az export pedig azt a datumot kapja, amit a fogadó app vár. Elavult GNSS-adat nem kerülhet friss pozícióra.

**Állapot (2026-10-04):** kód kész, telefonon még nem ellenőrzött. Az `engine` 1°-os EGM2008 rácsot használ (`Egm2008Geoid`, bilineáris, ugyanaz API 24-en és 36-on). Az `AltitudeConverter` kimaradt. Új pont: hihető MSL, különben ellipszoid mínusz N, majd a −430…20000 m kapu. A `lastGnss` csak 2 s-en belül számít. A régi `altitude` sorok érintetlenek. Budapest unduláció a rácson 43,64 m (±0,05). `:engine:test` `GpsAltitudeTest` sikeres.

---

### M4. `START_STICKY` újraindítás: védtelen `startForeground(LOCATION)` háttérből — *eszközön ellenőrizendő*

- **Forrás:** `app/.../service/TrackingForegroundService.kt`, **64–74** (`START_STICKY`, `null` intent → `startRecording()`), **478–487** (`startAsForeground`, nincs try/catch).
- **Probléma:** Ha a rendszer felvétel közben megöli a folyamatot (memória, gyártói akkukímélő, ezen a MediaTek-telefonon is jellemző), a `START_STICKY` miatt a service `null` intenttel újraindul, **háttérből**, látható Activity nélkül. A `startRecording()` azonnal `startForeground(…, FOREGROUND_SERVICE_TYPE_LOCATION)`-t hív:
  - Android 12+ alatt a háttérből indított FGS `ForegroundServiceStartNotAllowedException`-t dobhat;
  - Android 14+ alatt a `location` típusú FGS-nek „while-in-use” jogosultság kell. Ha nincs meg, `SecurityException`-t dob, vagy fut, de **nem kap helyet**: a session nyitva marad, a UI „Naplóz”, és pont nem jön.

  Hogy ezen a ROM-on melyik következik be, azt egy `am kill` + újraindítás próbával kell eldönteni.
- **Miért probléma:** Rossz esetben háttér-crash (és a sticky miatt ismétlődő újraindítás). Jobb esetben néma, pont nélküli „felvétel”, ami a felhasználó szemében adatvesztés.
- **Javítás:**
  - `startForeground` try/catch alatt (`ForegroundServiceStartNotAllowedException`, `SecurityException`). Hiba esetén zárd le tisztán a nyitott sessiont (STOP az utolsó elfogadott ponttal), írd az `AppErrorLog`-ba, és `stopSelf()`.
  - Fontold meg a `START_NOT_STICKY`-t, plusz egy „A felvétel megszakadt — folytatod?” jelzést a következő app-indításkor. A meglévő `openSession()`-ágból ez olcsó.
  - Teszt: `adb shell am kill` felvétel közben, majd figyeld, mi indul újra.
- **Miért oldja meg:** Az újraindítás vagy rendben, jogosultsággal fut, vagy egyértelműen leáll, és a felhasználó tud róla. Nincs crash, és nincs néma, üres „Naplóz”.

**Állapot (2026-10-04):** kód kész, telefonon még nem ellenőrzött. A sikeres indulás `START_STICKY` marad, a nyitott session folytatódik. A `startForeground` `SecurityException` és `ForegroundServiceStartNotAllowedException` ága lezárja a sessiont az utolsó ponttal, `track.foreground` kerül az `AppErrorLog`-ba, `stopSelf()`, és csak ez a sikertelen indulás ad `START_NOT_STICKY`-t. A következő megnyitás egyszer jelzi. Háttér és képernyőzár nem állítja le a futó rögzítést. `adb` eszköz nem volt csatlakoztatva, ezért a Home / zár / `am kill` próba nem futott.

---

## Közepes

### Kz1. „Csak GNSS” + kikapcsolt GPS a Startnál: bekapcsolás után sem jön több pont

- **Forrás:** `app/.../data/location/LocationClient.kt`, **45–48** (`gnssOnly && !GPS_PROVIDER enabled` → `emptyFlow()`); `TrackingForegroundService.kt`, **199–209**.
- **Probléma:** Fut/túrán és kerékpáron alapból „Csak GNSS” van. Ha a Startkor ki van kapcsolva a helymeghatározás, a `locations()` üres flow-t ad, a `collectLocation` azonnal visszatér. A UI „GPS kikapcsolva” állapotba kerül. Ha a felhasználó utána bekapcsolja a GPS-t, **semmi nem iratkozik fel újra**, és a felvétel pont nélkül megy tovább. A fused ágban ugyanez enyhébben jelentkezik: a GNSS-listener csak akkor regisztrál, ha a Startkor be volt kapcsolva, így később nincs MSL-forrás.
- **Miért probléma:** A felhasználó azt teszi, amit az app kér (bekapcsolja a GPS-t), mégis üres track lesz belőle.
- **Javítás:** Mindkét esetben regisztráld a `GPS_PROVIDER` listenert (provider-állapottól függetlenül), és kezeld az `onProviderEnabled` / `onProviderDisabled` eseményt (gpsOff állapot). Másik megoldás: a `LocationManager.PROVIDERS_CHANGED_ACTION` broadcastra újraindítani a gyűjtést.
- **Miért oldja meg:** A GPS bekapcsolásakor a fixek ugyanabba a sessionbe kezdenek érkezni.
- **Állapot (2026-10-04):** kész a kódban. A GNSS-only ág nem ad `emptyFlow()`-t, ha a GPS a Startkor ki van. A `GPS_PROVIDER` listener és a `PROVIDERS_CHANGED_ACTION` nyitva tartja a gyűjtést; a `gpsEnabled` állapot írja a `gpsOff` jelzést. Fused módban a GNSS-listener akkor is regisztrál, ha a GPS a Startkor ki van. Fused tartalék GNSS-only módban továbbra sincs. A szolgáltatás háttérben és képernyőzár alatt nem áll le. Telefonos próba ebben a körben nem futott.

---

### Kz2. GPX / KML koordináta tudományos jelöléssel a 0° közelében

- **Forrás:** `engine/.../GpxExporter.kt`, **60** (`lat="${point.latitude}" lon="${point.longitude}"`), `<ele>$trackEle</ele>`; `engine/.../KmlExporter.kt`, **76** (`gx:coord`), **174–177** (`lonLatAlt`).
- **Probléma:** A Kotlin/JVM `Double.toString()` 10⁻³ alatt exponenst ír. Ellenőrizve: `-0.0005` → `"-5.0E-4"`, `0.00001` → `"1.0E-5"`. Greenwich környékén (London, Le Havre, Valencia keleti része) a hosszúság, az Egyenlítő közelében a szélesség ilyen lesz.
- **Miért probléma:** A GPX 1.1 `latitudeType` / `longitudeType` `xsd:decimal`, ami **nem enged exponenst**. Szigorú olvasók (Garmin Connect, egyes XSD-validáló importálók) elutasítják a fájlt, vagy rosszul olvassák, például 0-nak. A KML `coordinates` is decimális vesszős listát vár. A hiba ritka, de a megosztott fájl csendben elromlik.
- **Javítás:** Rögzített formátum, `Locale.ROOT`: koordinátára 7 tizedes (~1 cm), magasságra 1–2 tizedes, például `String.format(Locale.ROOT, "%.7f", v)` vagy `BigDecimal(v).setScale(7, HALF_UP).toPlainString()`. Teszt: −0.0005, 0.00001, 179.9999999, −89.9.
- **Miért oldja meg:** Mindig decimális, sémakonform szám kerül a fájlba, locale-tól és nagyságrendtől függetlenül.
- **Állapot (2026-10-04):** kész. `CoordinateFormat` (`Locale.ROOT`, koordináta 7 tizedes, magasság 1 tizedes) a GPX `lat`/`lon`/`ele` és a KML `coordinates`, `gx:coord`, `alt`, `baro` mezőkön. A rögzítés nincs érintve. A README mindkét nyelvén benne van.

---

### Kz3. Világos témában Android 15+ alatt láthatatlanok az állapotsor ikonjai — *staged kód*

- **Forrás:** `app/.../ui/theme/Theme.kt`, **72–75** (`isAppearanceLightStatusBars = false`, `window.statusBarColor`, `window.navigationBarColor`); `MainActivity.kt` `enableEdgeToEdge()`.
- **Probléma:** targetSdk 36 mellett Android 15+-on az edge-to-edge kötelező, a `window.statusBarColor` és a `navigationBarColor` **hatástalan** (deprecated, no-op). Az állapotsor átlátszó, alatta a világos `PaperGrid` felső sáv látszik. A kód közben `isAppearanceLightStatusBars = false`-t ír elő, vagyis **fehér ikonokat** kér. A 11:32-es képernyőképen ez látszik: fehér óra és ikonok a `#F4F7F1` háttéren. Ez a sötét-téma munka újdonsága, a commitolt `themes.xml` még teal állapotsorra számított.
- **Miért probléma:** Világos témában az óra, az akkumulátor és a hely-ikon alig olvasható. Pont a helymeghatározás jelzése vész el egy GPS-appban.
- **Javítás:** A `SideEffect` helyett `enableEdgeToEdge(statusBarStyle = …, navigationBarStyle = …)` a `darkTheme` szerint: világosban `SystemBarStyle.light(scrim, darkScrim)`, sötétben `SystemBarStyle.dark(scrim)`. Ha teal sáv kell, azt Compose-ban rajzold a `WindowInsets.statusBars` magasságában. A `statusBarColor` / `navigationBarColor` hívás maradjon el.
- **Miért oldja meg:** Az ikonok színe a ténylegesen alattuk lévő háttérhez igazodik, és a megoldás Android 15+ alatt is működik.
- **Állapot (2026-10-04):** kész a kódban. `applyGtlSystemBars` világos témában `SystemBarStyle.light(PaperGrid, Cockpit)`, sötétben `SystemBarStyle.dark(Cockpit)`. A `statusBarColor` / `navigationBarColor` hívás kikerült. A téma váltásakor a `GtlTheme` ugyanazt hívja újra. A rögzítés nincs érintve. Telefonos ikonellenőrzés ebben a körben nem futott (nincs `adb` a környezetben).

---

### Kz4. Térképfrissítés minden rekompozíciónál: teljes polyline-csere, `redrawLayers`, ős-invalidálás

- **Forrás:** `app/.../ui/screens/MapPane.kt`:
  - **623**: Google — `points.map { LatLng(…) }` minden rekompozíciónál, a két `Polyline` (**652–673**) új listát és új `spans`-t kap
  - **1322–1388**: OSM `update` — minden hívásnál `updateOsmTrack` (**1630–1650**: a casing és **mindegyik** sebesség-szakasz `setPoints` a teljes listával), majd `mapView.requestVisibleTiles()` → `layerManager.redrawLayers()` + `repaint()`
  - **1199–1214**: `GtlOsmMapView.repaint()` minden Mapsforge-frame-nél végiginvalidálja **az összes ős View-t** a `DecorView`-ig
- **Probléma:** Az M1 miatt a `GtlUiState` másodpercenként sokszor változik, így az `AndroidView.update` és a `GoogleMap` tartalom is. A maps-compose `Polyline` új pontlistánál natív `setPoints`-ot hív, ami O(n) JNI-másolás. A Mapsforge-ben a `redrawLayers` minden overlay-t újrarajzol. Az ős-invalidálás minden térképframe-nél a teljes Compose-fát újrarajzoltatja.
- **Miért probléma:** Hosszú tracken akadozik a pásztázás és a zoom, a GPU-terhelés és az akkumulátor-használat felvétel közben folyamatos. A kerülőutak (ős-invalidálás, `content.invalidate()`) a valódi okot, a túl gyakori és túl széles állapotváltozást takarják. A 10:58-as, nem reprodukálható fehér képernyő is ezen a területen történt.
- **Javítás:**
  - Az M1 után a track-lista csak új pontnál változik.
  - Google: `remember(points) { points.map(::LatLng) }`.
  - OSM: csak az utolsó (élő) szakasz `setPoints`-ja frissüljön, ha nem nőtt az előző szakaszok száma. A `requestVisibleTiles()` csak kamera- vagy témaváltáskor fusson.
  - Az ős-invalidálás helyett a `MapView` saját `postInvalidate()` elég. Ha a Compose `AndroidView` ezt nem rajzolja ki, az a fő hiba, és azt kell javítani: például ellenőrizni kell a hardveres réteg / `SurfaceView` használatát.
- **Miért oldja meg:** A térkép csak akkor dolgozik, ha a rajzolt adat ténylegesen változott, és a módosítás a térképnézetnél marad.
- **Állapot (2026-10-04):** részben kész. A Google polyline `remember(points)`. Az OSM `update` azonos rajzkulcsnál visszatér, a `requestVisibleTiles()` kamera-, téma- és fókuszváltáskor fut. Az ős-`invalidate()` a `repaint()`-ben megmaradt: telefon nélkül nem derült ki, hogy a Mapsforge saját invalidálása elég-e, és a fehér képernyő miatt ez a workaround a helyén marad.

---

### Kz5. A felvétel szűrője a Start pillanatában lefagy — *2026-09-20 óta nyitott*

- **Forrás:** `app/.../service/TrackingForegroundService.kt`, **86** (`settings.first()`), **137** és **193–260** (`collectLocation(…, settings, …)`).
- **Állapot:** Változatlan. A pontosság, a „Csak GNSS”, a Kalman, a sűrűség és a usage-előbeállítás a Start pillanatában rögzül. Felvétel közbeni változtatásuk a következő Startig nem hat, és a UI ezt nem mondja meg. A QNH-collector élő, a szűrő nem.
- **Javítás:** Az előző review-ban leírt két lehetőség közül a döntés még nem született meg. A Beállításokban egy sor („felvétel közben a szűrő a következő Starttól érvényes”) a legolcsóbb. Az élő `settings.collectLatest` a pontosabb, de a Kalman állapotát váltáskor újra kell indítani.
- **Miért oldja meg:** A felhasználó tudja, mikor hat a beállítás, vagy az azonnal hat.
- **Állapot (2026-10-04):** kész, a viselkedés változatlan. A `settings.first()` snapshot marad; nincs élő `collectLatest` és nincs Kalman-újraindítás felvétel közben. A Rögzítés blokk alatt és a README mindkét nyelvén benne van, hogy a szűrő a következő Indítástól érvényes. A QNH továbbra is élő.

---

## Alacsony

### A1. Room `exportSchema = false` — *2026-09-20 óta nyitott*

- **Forrás:** `app/.../data/db/GtlDatabase.kt`, **10–14**.
- **Állapot (2026-10-04):** kész. `exportSchema = true`, a 6-os és 7-es séma a `schemas/` alatt, `GtlDatabaseMigrationTest` a 6→7 migrációt ellenőrzi.

### A2. A Tuhu worker az OSM work-taget viseli — *2026-09-20 óta nyitott*

- **Forrás:** `app/.../tuhu/TuhuMapStore.kt`, **43** (`addTag(OsmMapStore.WORK_TAG)`).
- **Állapot:** Változatlan. Saját `"tuhu-download"` tag kell. Az `OsmMapStore` **65**-ös sorának tag-alapú figyelése jelenleg mindkettőt látja.

### A3. Éjszakai térképtéma-váltás a fő szálon

- **Forrás:** `app/.../data/maps/OsmRenderTheme.kt` (night ág: asset beolvasás + `NightRenderTheme.recolor` + `writeCache`), `tuhu/TuhuRenderTheme.kt` (ugyanez), `engine/.../NightRenderTheme.kt` **23–30** (`readText`, `writeText`); `MapPane.kt` **1506–1530** (`applyOsmRenderOptions`: téma újratöltés + `tileCache.purge()`).
- **Probléma:** Az „Automatikus” téma alkonyatkor menet közben vált. Ekkor a fő szálon fut a teljes render-XML beolvasása, átszínezése, kiírása, a téma újraparse-olása és az összes csempe eldobása. Egy-két másodperces akadás és egy teljes térkép-újrarajzolás következik, épp vezetés közben.
- **Javítás:** Az éjszakai XML-t egyszer, a telepítés után (vagy az első indításkor) generáld `Dispatchers.IO`-n, verzió-hash-sel, és váltáskor csak a kész fájlt add át. Fontold meg két külön tile cache-t (nappali / éjszakai), így a váltás után nem kell az egész térképet újrarenderelni.

### A4. Rekurzív Douglas–Peucker

- **Forrás:** `engine/.../DouglasPeucker.kt`, **36–59** (`simplifyRange` rekurzív).
- **Probléma:** Legrosszabb esetben (spirál, hosszú, egyenletesen kanyargó track) a rekurzió mélysége O(n), a futásidő O(n²). Az M1 miatt ez ma a fő szálon, sokszor fut.
- **Javítás:** Iteratív változat explicit veremmel. Az M1 javítása után számold egyszer, `Dispatchers.Default`-on, és élő felvételnél inkrementálisan, csak az utolsó lezárt szakaszra.

### A5. `gps_events`: nincs `(sessionId, timestamp)` összetett index

- **Forrás:** `app/.../data/db/Entities.kt`, **27** (`Index("sessionId")`); `Daos.kt` **35–42** (`ORDER BY timestamp`).
- **Javítás:** `Index(value = ["sessionId", "timestamp"])` a 6→7 migrációban. A `latestForSession` és a rendezett listák így index-szkennel futnak, nem kell külön rendezés.
- **Állapot (2026-10-04):** kész. A `MIGRATION_6_7` eldobja az `index_gps_events_sessionId` indexet, és létrehozza az `index_gps_events_sessionId_timestamp` indexet. A migrációs teszt mindkettőt ellenőrzi.

### A6. Automatikus téma hely nélkül mindig világos

- **Forrás:** `engine/.../AppTheme.kt` (`AUTOMATIC`: pozíció nélkül `false`).
- **Probléma:** Az első indításkor, helyengedély vagy fix nélkül az „Automatikus” mindig világos, akkor is, ha a rendszer sötét módban van, és éjszaka van.
- **Javítás:** Pozíció nélkül az `isSystemInDarkTheme()` legyen a tartalék, pozícióval a polgári szürkület. A szürkület-számítás (`CivilTwilight`, −6°, suncalc-modell) rendben van.

---

## Megfigyelések (nem rangsorolt)

- **Fehér képernyő Turistautak → OSM Hungary váltáskor (10:58):** a jelenlegi forrásból épített appon 5 próbából egyszer sem reprodukálható. A 10:49-es build és a korabeli naplók már nincsenek meg. Ha újra előjön, a `tools/capture-white.sh` menti a bizonyítékot (lásd README — Hibakeresés a telefonon). Valószínűleg a Kz4 / M1 területéhez tartozik.
- **Crash-loop (11:14–11:24):** egy nem commitolt kísérleti `MainActivity` okozta (`composeView.layoutParams = ViewGroup.LayoutParams(…)`). Tanulság: ilyen kísérlet külön branchen vagy worktree-ben fusson, és a telefonra kerülő APK mindig a fából épüljön.
- **Párhuzamos módosítások:** a review alatt a `GtlViewModel.kt` és a `TrackingForegroundService.kt` is változott. A dőlésszög már kinematikus (`BikeLeanAngle.fromYawRate` / `fromBearingChange`). Ez a roadmap 5. tételének helyes iránya.

---

## Ami ebben a körben rendben volt

- K1/K2 javítás (Start/Stop mutex + generáció) a helyén.
- Friss fix kapu (`isFreshEnough`, 10 s, `elapsedRealtimeNanos`), monoton időbélyeg-ellenőrzés a `FixAcceptance`-ben.
- Kalman a lokális kelet–észak (ENU) méteres síkban, nem fokban; a DP is méterben, a szakasz kezdőpontjához vetítve. Táv haversine-nel. A szokásos (nem pólusközeli, nem antimeridiánon átnyúló) trackekre ez pontos.
- A `pick` a −430…20 000 m-en kívüli magasságot eldobja (a fused szemét kiszűrve; a repülő 9000 m fölött is rendben).
- FGS `location` típus, nincs `ACCESS_BACKGROUND_LOCATION`, `allowBackup="false"`, `networkSecurityConfig`, release-ben `isMinifyEnabled` + `isShrinkResources`.
- GPX név-escape, KML CDATA, UTC idő (`Instant.toString()`).
- A Mapsforge `MapView` `onRelease`-ben `destroyAll()`, a hibás `.map` nem crash-loopol (`onOsmFailed`).
- A polgári szürkület számítása (−6°) helyes modell; a témapozíció csak 5 km-es elmozdulásnál íródik.
- Google éjszakai stílus `MapStyleOptions`-szel, csak a normál rétegen (`usesNightStyle`).

---

## Összefoglaló: mit javítsunk, és milyen sorrendben

| Lépés | Tételek | Miért ebben a sorrendben | Becsült munka |
| --- | --- | --- | --- |
| 1 | **Kz3** | Staged, még nem kiadott kód. Olcsó, és a sötét-téma kiadás előtt kell. | ~0,5 nap |
| 2 | **M1 + Kz4** (+ A4) | Egy téma: állapot szétszedése, I/O le a fő szálról, térkép csak változásra. A legnagyobb mai erőforrás-hiba, és a fehér-képernyő gyanús területe is ez. | 2–3 nap |
| 3 | **M3** | A tárolt adat igazsága: magasságprofil, emelkedés, KMZ/GPX, baro-kalibráció. Minél később, annál több hibás régi track. | 1,5–2 nap |
| 4 | **M4** (+ eszközteszt) | Előbb `am kill` próba a valódi viselkedésre, utána try/catch + tiszta lezárás. | ~1 nap |
| 5 | **M2 + A1 + A5** | Egy Room-migráció (6→7): session-statok, összetett index, exportált séma, migrációs teszt. | 1,5–2 nap |
| 6 | **Kz1, Kz2** | Kicsi, jól tesztelhető javítások (provider-figyelés; `Locale.ROOT` formátum). | ~1 nap |
| 7 | **Kz5, A2, A3, A6** | Kisebb tételek, a következő hullámba. | ~1 nap |

**Ha csak hármat lehet:**
1. **Kz3**, mert kiadás előtt álló, látható regresszió;
2. **M1 (+Kz4)**, mert ez akkumulátor, jank és ANR-kockázat minden felvételen;
3. **M3**, mert a hibás magasság a tárolt adatot rontja, és nem lehet utólag tisztán javítani.

---

## Módszer

Forrásolvasás az `app/` és az `engine/` modulon, a felvételi lánc mentén:

- út: LocationClient → FGS → FixAcceptance / Kalman → Room → ViewModel → Compose / Google Maps / Mapsforge → GPX / KMZ;
- a letöltők és a staged sötét-téma fájlok is;
- eszközoldali bizonyíték a Rug One Xever 7 Pro telefonról (Android 16, API 36): logcat, events puffer, visszafejtett telepített APK, képernyőkép-pixelmérés;
- a `Double.toString()` exponenses kimenete `jshell`-lel ellenőrizve.

A sorszámok a fa 2026-10-04 13:05-ös állapotára vonatkoznak.
