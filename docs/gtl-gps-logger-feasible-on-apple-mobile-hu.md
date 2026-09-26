# GTL GPS Logger — natív iOS/Apple megvalósíthatósági elemzés

**Készült:** 2026-09-23
**Alap:** a jelenlegi Android app (2.0.14, versionCode 32) teljes funkciólistája, `README-hu.md`, `docs/dev-roadmap-hu.md`, `AndroidManifest.xml`, `app/build.gradle.kts`, `engine/` modul forrásfájljai.
**Kérdés:** mi valósítható meg natív Apple (Swift/SwiftUI + Apple keretrendszerek) implementációként, és mi az, ami platformkorlát miatt nem, vagy csak más formában.

---

## Tartalomjegyzék

- [Vezetői összefoglaló](#vezetői-összefoglaló)
- [Miért jó kiinduló pont ez a kódbázis](#miért-jó-kiinduló-pont-ez-a-kódbázis)
- [Stratégiai opciók](#stratégiai-opciók)
- [Kritikus platformkorlátok (a termék moatját érintik)](#kritikus-platformkorlátok-a-termék-moatját-érintik)
- [Funkciónkénti megvalósíthatóság](#funkciónkénti-megvalósíthatóság)
- [Engine modul — soronkénti KMP-újrafelhasználhatóság](#engine-modul--soronkénti-kmp-újrafelhasználhatóság)
- [Térkép-stratégia](#térkép-stratégia)
- [Engedélyek, háttérműködés, App Store felülvizsgálat](#engedélyek-háttérműködés-app-store-felülvizsgálat)
- [Stack-megfeleltetés (Android → Apple)](#stack-megfeleltetés-android--apple)
- [Javasolt fázisterv](#javasolt-fázisterv)
- [Összegzés és ajánlás](#összegzés-és-ajánlás)

---

## Vezetői összefoglaló

A GTL funkcióinak **nagy többsége** natívan megvalósítható Apple keretrendszerekkel (CoreLocation, CoreMotion, MapKit, SwiftData/Core Data, WidgetKit, ActivityKit), és a `:engine` modul — mivel **tiszta Kotlin, Android SDK-függőség nélkül** — Kotlin Multiplatformmal (KMP) szinte változtatás nélkül lefordítható iOS natív frameworkké, így a Kalman-szűrő, a Douglas–Peucker egyszerűsítés, a KML/KMZ/GPX exportálók, a baro/QNH számítás és a többi algoritmus **nem íródik újra**, csak egy Swift UI kerül köréje.

Ugyanakkor **két, a termék pozicionálásában központi funkció nem valósítható meg** nyilvános Apple API-val:

1. **GNSS skyplot és élő konstelláció-lista (GPS L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC műholdankénti SNR és azimut/eleváció)** — az Apple soha nem tett közzé nyilvános API-t nyers GNSS-műhold-mérésekhez (nincs `GnssStatus`/`GnssMeasurement` megfelelő). A `CLLocationManager` csak a kész, már összesített koordinátát adja.
2. **„Csak GNSS” mód (chip-pozíció fused hely helyett)** — a `CoreLocation` nem enged forrást választani; mindig az OS saját, már kevert (GPS + Wi-Fi + cella + IMU) becslését adja. A README „Fut/túra mint egy sportóra” fejezetének technikai alapja (a Suunk chip-track elve) iOS-en **nem reprodukálható ugyanazzal a pontossággal**.

Ez a két korlát nem blokkolja a portolást, de **át kell gondolni a termékpozíciót** iOS-re: a „GNSS HUD, chip vs. fused” érvelés (lásd `docs/dev-roadmap-hu.md` „Termékpozíció” szakasz) az Android-verzió megkülönböztető ereje, ami iOS-en részlegesen elesik. Az elemzés alább minden funkciót külön kezel, és javasol reális helyettesítést, ahol van.

---

## Miért jó kiinduló pont ez a kódbázis

A projekt már **két Gradle-modulra** van bontva:

| Modul | Tartalom | iOS szempontból |
|---|---|---|
| `:engine` | Tiszta JVM Kotlin: Kalman-szűrő, Douglas–Peucker, fix-elfogadás, sebességadaptív térköz, track-statisztika, KML/KMZ/GPX export, GPS-magasság választás, baro/QNH ISA-képlet, skyplot-projekció, OSM fájl/kamera logika, mértékegység-formázás | **Újrafordítható** Kotlin/Native-tal iOS `.xcframework`-ké, Android SDK hívás nélkül |
| `:app` | Compose UI, Room, DataStore, Play Services Location/Maps, Mapsforge, WorkManager, FileProvider | Android-specifikus, **nem** hordozható; ez cserélendő Swift/SwiftUI rétegre |

Ez azt jelenti, hogy egy iOS port **nem a teljes app újraírása** — a doménlogika (kb. 45 fájl a `engine/src/main/kotlin`-ban) egyszer van megírva és tesztelve (JUnit), és ugyanaz a `.kt` forrás fut majd az Android JVM-en és az iOS Kotlin/Native binárisban is.

---

## Stratégiai opciók

| Opció | Leírás | Előny | Hátrány |
|---|---|---|---|
| **A. Kotlin Multiplatform + natív SwiftUI (ajánlott)** | `:engine` KMP-modullá alakítva (`expect`/`actual` csak ott, ahol platform-specifikus, pl. `AndroidBaroAltitude` → `IosBaroAltitude`), iOS oldalon teljesen natív SwiftUI UI, CoreLocation, MapKit/MapLibre | Az algoritmusok egy helyen élnek, nem drifthetnek szét a két platform között; natív UI/UX, App Store szempontból „valódi” natív app | KMP build-lánc bevezetése (Xcode + Gradle összefésülés), a csapatnak Swiftet is tudnia kell |
| **B. Teljes natív Swift újraírás, logika is portolva** | Minden `.kt` fájl logikáját kézzel Swiftre írva | Nincs KMP build-komplexitás | Duplikált logika, két helyen kell karbantartani (pl. a Kalman-paraméterek finomhangolását kétszer kell elvégezni); a jövőben könnyen szétcsúszik a két platform viselkedése — ez pont az a hiba, amit a jelenlegi `:engine`/`:app` szétválasztás el akar kerülni |
| **C. Cross-platform UI keretrendszer (Flutter/React Native/.NET MAUI/Expo)** | Egy UI-kódbázis mindkét platformra | Leggyorsabb UI-fejlesztés | **Nem natív** — a felhasználó explicit natív Apple implementációt kért; a mély szenzor-/GNSS-integráció (barométer, GNSS finomhangolás) ezeken a kereteken keresztül esetlegesebb és plusz híd-réteget igényel |

**Javaslat: A opció.** A jelenlegi kódbázis szerkezete (tiszta engine modul) kifejezetten erre lett előkészítve (talán nem tudatosan, de a hatás ugyanaz), és ez tartja egyben a két platform viselkedését hosszú távon.

---

## Kritikus platformkorlátok (a termék moatját érintik)

A `docs/dev-roadmap-hu.md` a GTL moatját így írja le: *„1. adat a telefonon marad, 2. a vonal az, amit a chip/Kalman tényleg rögzített, 3. GNSS HUD (konstelláció, SNR, pontfelhő/CEP95, skyplot), 4. KMZ/GPX.”* Az 1., 2. és 4. pont iOS-en tartható. A 3. pont és a 2. pont fele **nem**, az alábbiak szerint:

### 1. Nincs nyilvános nyers GNSS-műhold API

Android: `GnssStatus.Callback` ad műholdankénti azimutot, elevációt, CN0/SNR-t, konstellációt, L1/L5 vivőt, used/in-view állapotot — ez táplálja a **GPS fület** (élő chip-számok) és a **skyplot** polar plotot.

Apple soha nem tett közzé hasonló API-t. A `CoreLocation` (`CLLocationManager`, `CLLocation`) csak a végeredmény koordinátát, pontosságot, sebességet és irányt adja — a mögöttes műholdgeometriát az OS nem osztja meg harmadik féllel. Ez nem hiányzó feature, hanem szándékos Apple-döntés, és semmilyen App Store-kompatibilis workaround nincs rá (jailbreak/privát API kizárt).

**Következmény:** a **GPS fül** konstelláció-chipjei (GPS L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC), az SNR-minőség sáv és a **polar skyplot** iOS-en **nem építhető meg** a jelenlegi formában.

**Reális helyettesítés:** a GPS fül megmarad pozíció/pontosság/magasság/sebesség panelként (`CLLocation.horizontalAccuracy`, `.verticalAccuracy`, `.speedAccuracy` — ez utóbbi iOS-en is elérhető és finomabb, mint Androidon sokáig volt), de a skyplot és a konstelláció-lista kikerül a hatókörből, vagy egy „**Nem elérhető iOS-en**” magyarázó kártyával helyettesítendő a Súgóban.

### 2. Nincs „csak GNSS chip” forrásválasztás

Android GPS_PROVIDER-t lehet direktben kérni (nyers chip-pozíció, fused-szűrés nélkül) — ez adja a Fut/túra és kerékpár mód alapját, hogy egy 5–10 m-es úttest-hurok ne simuljon el a telefon „hol van a felhasználó?” szűrőjében, mielőtt a GTL egyáltalán látná.

A `CoreLocation` **mindig** a kész, már Wi-Fi/cella/IMU-val kevert becslést adja; nincs paraméter, ami kikapcsolná ezt a fúziót. A `CLLocationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation` a legjobb elérhető mód, de ez sem chip-only.

**Következmény:** a Fut/túra és kerékpár mód iOS-en **nem tud ugyanolyan „sportóra-szerű” chip-tracket adni**, mint Androidon. A kis úttest-hurkok valószínűleg jobban elsimulnak.

**Reális helyettesítés:** a `RecordingDensity` (Minden jó sűrűség) és a `FixAcceptance` engine-logika változatlanul átvihető; a Kalman-szűrő paraméterezése (alacsonyabb `q`, gyengébb simítás Fut/túránál) iOS-en is értelmes marad, csak a bemenet lesz simább már a forrásnál. Ezt a Súgóban/termékleírásban érdemes explicit módon jelezni, nem hallgatni el.

### 3. Nincs környezeti hőmérséklet-szenzor

Az `Android.hardware.sensor.ambient_temperature` (`TYPE_AMBIENT_TEMPERATURE`) már Androidon is opcionális és ritka hardver. Az iPhone-okban **nincs** nyilvánosan elérhető környezeti hőmérséklet-szenzor, és a `CoreMotion` sem tesz közzé ilyet.

**Következmény:** a letárolt pontonkénti opcionális hőmérséklet mező iOS-en mindig üres/„N/A” marad — ez elhagyható feature, nem blokkoló.

### 4. Barométer — jó hír

A `CMAltimeter` (`startRelativeAltitudeUpdates`) natívan ad nyomásadatot (`CMAltitudeData.pressure`, kPa-ban), és minden barométerrel szerelt iPhone-on elérhető (iPhone 6 óta gyakorlatilag mind). A meglévő `BaroAltitude.kt` ISA/QNH-képlete (`44330 × (1 − (p/QNH)^(1/5.255))`) **platformfüggetlen matek**, tehát a `:engine` modulból változtatás nélkül átvihető; csak egy `IosBaroAltitude` adapter kell, ami a `CMAltimeter` nyers hPa-értékét adja át neki — ugyanúgy, ahogy ma az `AndroidBaroAltitude.kt` teszi a `SensorManager`-rel.

---

## Funkciónkénti megvalósíthatóság

| Funkció (README szerint) | Megvalósítható natívan? | Apple API / megközelítés | Megjegyzés |
|---|---|---|---|
| Indít/Leállít naplózás, előtér-folyamat | Igen | `CLLocationManager` + `allowsBackgroundLocationUpdates`, `Always` engedély | Nincs Android-szerű „foreground service” fogalom; helyette háttér-frissítés + kék állapotsáv/Live Activity jelzi a felhasználónak |
| Fix-elfogadás, sűrűség (Okos/Minden jó) | Igen | `:engine` `FixAcceptance.kt` változtatás nélkül | Tisztán Kotlin logika |
| Kalman-simítás | Igen | `:engine` `KalmanTrackFilter.kt` változtatás nélkül | — |
| Csak GNSS mód | **Nem** (natív iOS forrásválasztás nélkül) | — | Lásd fenti korlát #2 |
| GPS fül — koordináta, pontosság, forrás, magasság | Igen | `CLLocation` mezői | — |
| GPS fül — konstelláció-chipek, SNR, skyplot | **Nem** | — | Lásd fenti korlát #1 |
| Baro magasság, QNH, kalibrálás GPS-ből | Igen | `CMAltimeter` + `:engine` `BaroAltitude.kt` | Automatikus induláskori kalibrálás logikája is átvihető |
| Ambient hőmérséklet | **Nem** (nincs szenzor) | — | Mező üresen marad |
| Gyorsulásmérő minták, dőlésszög (tankra szerelve) | Igen | `CoreMotion` `CMMotionManager`/`CMDeviceMotion` (attitude: roll/pitch/yaw + gravity) | Az iOS API ebben inkább gazdagabb, mint az Android nyers accelerométer |
| Útvonal fül — összesítők, magasságprofil | Igen | `:engine` `TrackStats.kt`, `ElevationSeries.kt` | Chart: Swift Charts (natív, iOS 16+) |
| Térkép fül — online térkép | Igen | `MapKit` (`Map` SwiftUI view) | Nem kell API-kulcs, ingyenes, natívabb, mint a Google Maps SDK Androidon |
| Térkép fül — offline OSM régió (Mapsforge) | Részben, más motorral | `MapLibre Native` (iOS SDK) + előre generált vektorcsempe-csomagok (MBTiles/PMTiles) | A `MapKit` **nem** támogat letöltött, appban tárolt offline vektorcsempéket; ehhez harmadik féltől kell megoldás. Részletek lent. |
| Turistautak.hu réteg | Részben | ugyanaz, mint fent, ha a forrás vektorcsempeként kiadható | Feltételezi, hogy a Turistautak.hu adat átalakítható MapLibre-kompatibilis formátumra |
| Koppintás → Távolság / GPS koordináta / Cím | Igen | `MapKit` gesztus + `:engine` `TapReadout.kt` | — |
| Keresés (OSM `.map` helynevek) | Részben | saját index (pl. SQLite FTS az MBTiles metaadatból) | A jelenlegi `MapSearch.kt`/`IndexResume.kt` logika portolható, ha az offline adatforrás cserélve van |
| Iránytű fül (MAG/TRUE heading) | Igen | `CLLocationManager.headingAvailable` + `startUpdatingHeading` | `:engine` `CompassHeading.kt` átvihető |
| Mentett útvonalak lista, törlés, megosztás | Igen | `SwiftData`/Core Data + `UIActivityViewController` | — |
| KMZ export (KML + ikonok) | Igen | `:engine` `KmlExporter.kt`, `KmzExporter.kt`, `KmlDescriptions.kt` — tiszta fájlgenerálás | Az Android sem használ platform-KML API-t, ez már ma is „kézzel írt” logika → 1:1 portolható |
| GPX 1.1 export | Igen | `:engine` `GpxExporter.kt` | Ua. |
| Beállítások (usage előbeállítások, csúszkák) | Igen | SwiftUI `Form`, `@AppStorage`/`UserDefaults` vagy `SwiftData` | `:engine` `UsageSmoothingDefaults.kt`, `RecordingDensity.kt`, `SmoothingStrength.kt` változtatás nélkül |
| Biztonságos vezetés nyilatkozat | Igen | egyszerű onboarding képernyő + `UserDefaults` flag | — |
| Offline térkép letöltés (WorkManager) | Igen, más API-val | `URLSession` háttér-letöltés (`URLSessionConfiguration.background`) vagy `BGTaskScheduler` | — |
| Súgó (harmonika), Névjegy, hibanapló | Igen | SwiftUI `DisclosureGroup`, saját fájlalapú napló | `ErrorLogStoreTest` mögötti logika platformfüggetlen, portolható |
| Sötét/világos téma (roadmap #1) | Igen, iOS-en „ingyen” jobb | `MapKit` beépített `.dark`/`.light`/`.system` `MapStyle` | Nem kell egyedi éjszakai JSON-stílus, mint a Google Maps SDK-nál Androidon — a `MapKit` naponta/rendszertéma szerint automatikusan vált |
| Élő értesítés számokkal (roadmap #3) | Igen, sőt jobb megoldás | **Live Activities / Dynamic Island** (`ActivityKit`) | Lásd külön szakasz lent — ez iOS-en natívan erősebb, mint az Android statikus notification |
| Quick Settings tile (roadmap #9) | Igen, más UI-val | **Control Center gomb** (`ControlWidget`, iOS 18+) vagy Home Screen widget interaktív gombbal (`AppIntent`, iOS 17+) | Közvetlen megfelelő |
| Fekvő/tank HUD mód (roadmap #7) | Igen | SwiftUI `orientation` kezelés, `UIInterfaceOrientationMask` | Ua. komplexitás, mint Androidon |
| Track-kép/képeslap megosztás (roadmap #8) | Igen | `ImageRenderer` (SwiftUI → kép, iOS 16+) | Natívan egyszerűbb, mint Androidon egy Compose→Bitmap útvonal |
| Wear OS megfelelő (Apple Watch) | Kihagyandó, ugyanaz az indok | `watchOS` app + `WatchConnectivity` | A roadmap Wear OS-t explicit kizárja effort miatt; ugyanez az érv áll Apple Watchra is |

---

## Engine modul — soronkénti KMP-újrafelhasználhatóság

A `engine/src/main/kotlin` alatti 45 fájl gyakorlatilag mind tiszta algoritmus vagy adatosztály; az alábbi csoportosítás mutatja, mi megy **változtatás nélkül**, és mi igényel platform-adaptert (`expect`/`actual`):

**Változtatás nélkül KMP-re fordítható (nincs Android SDK hívás):**
`BaroAltitude`, `BikeLeanAngle`, `CompassHeading`, `DouglasPeucker`, `ElevationSeries`, `EventKind`, `FixAcceptance`, `FixCloud`, `GeoPoint`, `GeoProjection`, `GpsAltitude`, `GpsQualityNotice`, `GpxExporter`, `KalmanTrackFilter`, `KmlDescriptions`, `KmlExporter`, `KmzExporter`, `MapCameraMode`, `MapDisplayUsage`, `MapFitZoom`, `MapHudVisibility`, `MapPlaceKind`, `MapTrackVisibility`, `MeasurementSystem`, `RecordingDensity`, `RouteTabSpeeds`, `SmoothingStrength`, `SpeedAdaptiveSpacing`, `TapReadout`, `TrackCameraBounds`, `TrackEndpoints`, `TrackLogExport`, `TrackStats`, `Units`, `UsageSmoothingDefaults`, `UsageType`

**Platform-adaptert igényel (a logika marad, a bemenet forrása változik):**
- `Gnss.kt`, `Skyplot.kt` — a *projekció/osztályozás* algoritmusa portolható, de iOS-en **nincs bemeneti adat**, ami táplálná (lásd korlát #1); ezek a fájlok iOS build-en „élő” bemenet nélkül maradnak, vagy kikerülnek a target-ből
- `OsmMapFile.kt`, `OsmMapCamera.kt`, `OsmMapViewRedraw.kt`, `OsmRenderCategories.kt`, `OsmRenderOptions.kt`, `OsmRenderThemePath.kt`, `OsmHillshading.kt`, `OsmOfflineAvailability.kt`, `MapSearch.kt`, `IndexResume.kt` — a Mapsforge-specifikus fájlformátum-feltevések (a `.map` bináris header) helyett MapLibre/MBTiles-adapterre kell írni; a *döntési logika* (mikor Használatban, mikor kapcsol vissza online térképre, kamera-illesztés) megtartható, csak az I/O-réteg cserélendő
- `OfflineMapUse.kt`, `OsmMapLocale.kt` — DataStore-mezőkre épülő enum/állapotlogika, a tárolási réteg cseréje (`UserDefaults`/`SwiftData`) mellett átvihető
- `TrackInspectDump.kt` — diagnosztikai dump, platformfüggetlen

Ez azt jelenti, hogy a portolási munka **túlnyomó része az `:app` UI/platform-rétegre esik**, nem az algoritmusokra — ami jelentősen csökkenti a hiba- és regressziókockázatot, mert a már bevált, JUnit-tesztelt logika nem íródik újra.

---

## Térkép-stratégia

Ez a legnagyobb architekturális döntés az iOS porton, mert az Android-oldali két térképmotor (Google Maps + Mapsforge OSM) egyike sem elérhető változatlan formában:

| Igény | Android megoldás | iOS opció | Értékelés |
|---|---|---|---|
| Online térkép | Google Maps SDK (API-kulcsos) | **MapKit** | Jobb választás iOS-en: natív, ingyenes, nem kell kulcskezelés, automatikus sötét téma |
| Offline vektor OSM-régió | Mapsforge `.map` fájl, appban letöltve | **MapLibre Native (iOS)** + MBTiles/PMTiles csempecsomag | Ez a legnagyobb portolási tétel; a Mapsforge-fájlformátum nem kompatibilis, új csempe-előállítási/disztribúciós láncot igényel (pl. `tilemaker`/`planetiler` + saját hosting vagy bundle-elt csomag) |
| Turistautak.hu réteg | Mapsforge overlay | ugyanaz, mint fent, ha a forrás átalakítható | Függ a Turistautak.hu adatforrás elérhetőségétől MapLibre-kompatibilis formában — ezt külön kell egyeztetni/vizsgálni |
| Offline keresés | SQLite `map-search.db` a `.map` fájlból építve | saját index az MBTiles metaadatból (pl. SQLite FTS5) | `MapSearch.kt`/`IndexResume.kt` logika megtartható |

**Következtetés:** az online térkép iOS-en **egyszerűbb és olcsóbb**, mint Androidon (nincs API-kulcs-gondozás). Az offline térkép viszont **nem egyszerű 1:1 port** — ez saját, több napos tétel, és érdemes külön briefben kezelni, mielőtt a fázistervbe kerül.

---

## Engedélyek, háttérműködés, App Store felülvizsgálat

| Terület | Android ma | iOS megfelelő | Megjegyzés |
|---|---|---|---|
| Helyengedély | `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`, nincs `ACCESS_BACKGROUND_LOCATION` | `NSLocationWhenInUseUsageDescription` + (ha képernyőzárolt naplózás is kell) `NSLocationAlwaysAndWhenInUseUsageDescription` | Ha a naplózás csak nyitott appnál/aktív képernyőnél fut (mint ma Androidon a látható előtér-szolgáltatással), elég a „When In Use” + `allowsBackgroundLocationUpdates` a folyamatos frissítéshez, amíg az app a háttérben fut |
| Előtér-szolgáltatás értesítés | `FOREGROUND_SERVICE_LOCATION`, látható notification | Live Activity / kék „helymeghatározás aktív” állapotsáv | Az iOS automatikusan mutat rendszerszintű jelzést folyamatban lévő helymeghatározásnál; ez a jelenlegi Android-terv szerinti explicit notification (roadmap #3) helyett/mellett **Live Activity**-vel váltható ki, ami kifejezetten erre való és a lock screenen/Dynamic Islandben is megjelenik |
| App Store felülvizsgálat | Play Console, kevésbé szigorú a háttér-GPS-re | Apple **szigorúan** vizsgálja a folyamatos helymeghatározást; egyértelmű, felhasználó-orientált indoklás kell (fitness/navigációs napló — ez a GTL esetében adott és könnyen indokolható) | A privacy policy már megvan (`gtl-privacy-policy.html`), ez jó kiindulópont az App Privacy „Nutrition Label” kitöltéséhez |
| Adatvédelem | Nincs feltöltés, `RemoteTrackSync` no-op | Ugyanez a helyi-only elv iOS-en is tartható, sőt erősíthető App Store „Privacy Nutrition Label”-lel | Ez marketing-előny is: „Adat nem hagyja el a telefont” jól kommunikálható az App Store terméklapon |
| Push/rendszerértesítés engedély | `POST_NOTIFICATIONS` | `UNUserNotificationCenter` engedélykérés | Csak akkor kell, ha Live Activity/helyi notification is jár vele |

---

## Stack-megfeleltetés (Android → Apple)

| Android | Apple megfelelő |
|---|---|
| Kotlin + Jetpack Compose | Swift + SwiftUI |
| Room | SwiftData (iOS 17+) vagy Core Data (visszafelé kompatibilisebb) |
| DataStore (Preferences) | `UserDefaults` / `@AppStorage` |
| Play Services Location (fused) | `CoreLocation` (`CLLocationManager`) |
| Play Services Maps / Maps Compose | `MapKit` (SwiftUI `Map`) |
| Mapsforge (offline vektor OSM) | MapLibre Native (iOS) + MBTiles/PMTiles |
| WorkManager (OSM letöltés háttérben) | `URLSession` háttér-letöltés / `BGTaskScheduler` |
| Foreground Service + notification | `CLLocationManager` háttér-frissítés + Live Activity (`ActivityKit`) |
| FileProvider (megosztás) | `UIActivityViewController` (share sheet) |
| KSP (Room annotációfeldolgozás) | nem releváns (SwiftData deklaratív, nincs kódgenerálás igény) |
| JUnit (`:engine` tesztek) | ugyanaz a Kotlin JUnit-szuita fut KMP alatt is; iOS UI-tesztekhez `XCTest` |

---

## Javasolt fázisterv

Az effort-becslések egy, a kódbázist ismerő fejlesztő naptári napjaiban értendők, ugyanúgy, mint a `docs/dev-roadmap-hu.md`-ban.

### 0. fázis — KMP alapozás
- `:engine` modul KMP-célokkal bővítése (`iosArm64`, `iosSimulatorArm64`), `.xcframework` export
- A meglévő JUnit-tesztek futtatása KMP alatt is zöldre
- **Kapu:** az `:engine` build zöld Androidon és iOS szimulátoron egyaránt, UI még nincs

### 1. fázis — Core naplózási MVP
- SwiftUI váz, `CLLocationManager` integráció, `FixAcceptance`/`KalmanTrackFilter` bekötése
- SwiftData séma (session + pont, a Room sémával analóg)
- `MapKit` online térkép, piros polyline a helyi adatbázisból (nem külön vázlat — ugyanaz az elv, mint Androidon)
- Beállítások képernyő az usage-előbeállításokkal

### 2. fázis — Export és megosztás
- `KmzExporter`/`GpxExporter` bekötése, `UIActivityViewController` megosztás
- Mentett útvonalak lista, törlés, magasságprofil (Swift Charts)

### 3. fázis — Szenzorok
- Iránytű (`CLHeading`), barométer/QNH (`CMAltimeter` + `BaroAltitude.kt`), gyorsulásmérő/dőlésszög (`CoreMotion`)
- GPS fül a **skyplot és konstelláció-lista nélkül** (lásd korlátok), helyette pontosság/forrás/magasság panel

### 4. fázis — Offline térkép
- MapLibre Native integráció, csempe-előállítási/disztribúciós lánc kiválasztása és tesztelése egy régión
- Ez a legbizonytalanabb effort-becslésű tétel — **külön technikai spike ajánlott a fázisterv elfogadása előtt**

### 5. fázis — iOS-natív pluszok
- **Live Activity** élő számokkal (sebesség, út, pontosság) — ez a roadmap #3 tételének iOS-en natívan erősebb megfelelője
- Control Center gomb / interaktív Home Screen widget Indít/Leállít-hoz (roadmap #9 megfelelője)
- Sötét térkép **ingyen** a `MapKit` rendszertéma-követésével (roadmap #1 nagy részét kiváltja)

### 6. fázis — App Store beadás
- Privacy Nutrition Label, `NSLocationAlwaysAndWhenInUseUsageDescription` indoklás szövegezése
- TestFlight béta, majd beadás

---

## Összegzés és ajánlás

- **A funkciók kb. 80–85%-a** natívan, jó minőségben megvalósítható Apple keretrendszerekkel, és a meglévő `:engine` algoritmuskódnak becslés szerint **túlnyomó része (≈35–38 a 45 fájlból) változtatás nélkül** újrafordítható KMP-vel — ez jelentősen csökkenti a portolás kockázatát és idejét a „nulláról Swiftben újraírt logikához” képest.
- **A legnagyobb tétel az offline térkép** (Mapsforge → MapLibre migráció): ez nem API-csere, hanem adatlánc- és disztribúciós kérdés, önálló vizsgálatot érdemel.
- **Két funkció esik ki érdemben**: a GNSS skyplot/konstelláció-panel és a „csak GNSS chip” mód — mindkettő azért, mert az Apple nem tesz közzé nyers GNSS-műhold API-t, és nem enged forrás-választást a `CoreLocation`-ben. Ez nem fejlesztői hiányosság, hanem platformkorlát; a termékleírást és a Súgót ennek megfelelően kell iOS-re igazítani.
- **Két terület iOS-en kifejezetten jobb**, mint a jelenlegi Android-megoldás: az online térkép (nincs API-kulcs-gondozás) és az élő HUD (Live Activity/Dynamic Island erősebb, mint egy statikus notification).
- **Javasolt megközelítés:** Kotlin Multiplatform a megosztott doménlogikára, teljesen natív SwiftUI a felületre — ez tartja egyben a két platform viselkedését hosszú távon, és illeszkedik ahhoz az elvhez, amit a jelenlegi `:engine`/`:app` szétválasztás már ma is követ.
