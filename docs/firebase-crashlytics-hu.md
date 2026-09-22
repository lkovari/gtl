# Prompt: Firebase Crashlytics beépítése a GTL-be

Építsd be a Firebase Crashlyticsot ebbe a tárolóba. Ez a szelet a nem kezelt összeomlást, az ANR-t és az alkalmazáskód minden elkapott hibáját jelenti. Az adatbázist nem módosítja, és a jelentéshez nem olvas SQLite-ot.

A nyomvonal már a Roomban van (`gps_events`). A Crashlytics stack trace-t és rövid kulcsokat kap. Az útvonalat nem kapja meg. Nincs incidens-tábla, nincs `gps_events`-másolat, és nincs diagnosztikai lekérdezés.

## Kemény korlát

- Ne adj hozzá táblát, oszlopot, entityt, DAO-t, migrációt vagy sémaverziót. Ne módosítsd az `Entities.kt`, `Daos.kt`, `GtlDatabase.kt`, `TrackRepository.kt` fájlokat, és a `docs/DBSTRUCT-en.md` dokumentumot.
- A hibajelentő ne kérdezze le a Roomot vagy a SQLite-ot. Menetazonosítót csak abból a változóból vegyél, amelyet a híváshely már a memóriában tart.
- Ne írj szélességet, hosszúságot, magasságot, sebességet, irányt, pontosságot, hőmérsékletet, nyomást, műholdszámot, gyorsulásmintát, koordinátát tartalmazó fájlútvonalat vagy `GpsEventEntity`-t Crashlytics-kulcsba, logba vagy új kivételüzenetbe. Az eredeti kivételt add tovább. Ne csomagold olyan üzenetbe, amelyet egy pontból állítasz össze.
- Ne hívd a `setUserId`-t. Nincs fiók.
- Ne add hozzá a `firebase-analytics` és a `firebase-crashlytics-ndk` csomagot.
- Ne tegyél be saját `UncaughtExceptionHandler`-t, és a crash handlerből ne küldj hálózati jelentést. Az SDK fájlt ír, és a következő hideg induláskor tölti fel.
- A teszteket ne instrumentáld (`app/src/test`, `app/src/androidTest`, `engine/src/test`). Azok a `catch` ágak a teszthez tartoznak, nem produkciós hibák.
- Ne tegyél kommentet a forrásba.
- A `google-services.json` már szerepel a `.gitignore`-ban. Hagyd figyelmen kívül. Ne commitold.

## Mit kell látnia a fejlesztőnek

A nem kezelt kivételt és az ANR-t az SDK híváshely nélkül naplózza.

Az `app/src/main` minden más elkapott `Throwable` értéke a `recordException` híváson keresztül megy fel. A `CancellationException` nem hiba: dobd tovább, és ne jelentsd. Ez az `OsmDownloadWorker` és a `TuhuDownloadWorker` `catch (Exception)` ágán számít, mert az ma elnyeli a megszakítást. Előbb dobd tovább, utána jelents minden más kivételt, majd hagyd meg a meglévő takarítást és a `Result.failure()`-t.

A throwable nélküli `Result.failure()` ágak maradjanak. Ne gyárts hozzájuk kivételt.

Minden nem végzetes jelentéshez `op` kulcs tartozik, közvetlenül a `recordException` előtt. A `session_id` csak akkor kerüljön fel, ha a híváshelyen már van memóriában menetazonosító. Egyébként hagyd ki a kulcsot. Ne küldj `0`-t helyette.

| Kulcs | Érték |
|---|---|
| `op` | stabil név az alábbi táblából |
| `session_id` | a memóriában már meglévő nyitott menetazonosító (`Long`), csak a két rögzítési beszúrásnál |

Az appverziót, a készülékmodellt és az OS-verziót az SDK teszi rá. Ezeket ne duplikáld saját kulccsal.

Az `insert_event` után a mai viselkedés marad: `loggingError = true` és `stopRecording()`. Ez leállítja a további beszúrásokat, ezért egy rögzítés egy `insert_event` jelentést ad. Az `insert_stop` után a `stopSession` továbbra is lefut, hogy a menet ne maradjon nyitva. A többi `op`-ot ne vesd el menetnyi egyszeri őrrel.

A kivételt kösd névhez. Ne dobd el `_` néven.

A `GtlApplication.onCreate` Crashlytics-kulcs és helyadat nélkül marad. A Google Services content provider a crash handlert a `onCreate` előtt telepíti.

## Hová kell hívni

| Fájl | Catch | `op` | `session_id` |
|---|---|---|---|
| `service/TrackingForegroundService.kt` | `SQLException` a rögzítés közbeni `insertEvent` körül (ez állítja a `loggingError`-t) | `insert_event` | a beszúrás `sessionId` értéke |
| `service/TrackingForegroundService.kt` | üres `SQLException` a `stopRecording` STOP `insertEvent` körül | `insert_stop` | az ott lezárt `sessionId` |
| `data/location/LocationClient.kt` | `SecurityException` a `gpsProviderLocations` `requestLocationUpdates` hívásán | `gps_request_updates` | nincs |
| `data/location/LocationClient.kt` | `SecurityException` a `gpsProviderLocations` `removeUpdates` hívásán | `gps_remove_updates` | nincs |
| `data/location/LocationClient.kt` | `SecurityException` a `fusedLocations` GPS `requestLocationUpdates` hívásán | `fused_gps_request_updates` | nincs |
| `data/location/LocationClient.kt` | `SecurityException` a fused `requestLocationUpdates` hívásán | `fused_request_updates` | nincs |
| `data/location/LocationClient.kt` | `SecurityException` a fused `removeLocationUpdates` hívásán | `fused_remove_updates` | nincs |
| `data/location/LocationClient.kt` | `SecurityException` a `fusedLocations` GNSS-figyelő `removeUpdates` hívásán | `fused_gps_remove_updates` | nincs |
| `data/gnss/GnssStatusSource.kt` | `SecurityException` a `registerGnssStatusCallback` hívásán | `gnss_register` | nincs |
| `data/gnss/GnssStatusSource.kt` | `SecurityException` az `unregisterGnssStatusCallback` hívásán | `gnss_unregister` | nincs |
| `ui/screens/MapPane.kt` | `IllegalStateException` az `animateToTrackBounds` függvényben | `map_fit_bounds` | nincs |
| `ui/screens/MapPane.kt` | `Throwable` az OSM `AndroidView` factoryban (`attachOsmLayers`) | `osm_attach` | nincs |
| `ui/screens/MapPane.kt` | `Throwable` az OSM `onRelease` ágban (`destroyAll`) | `osm_destroy` | nincs |
| `ui/screens/MapPane.kt` | `Throwable` az `applyOsmXmlTheme` függvényben | `osm_theme` | nincs |
| `ui/screens/MapPane.kt` | `RuntimeException` a `fitOsmToBounds` függvényben | `osm_fit_bounds` | nincs |
| `data/maps/OsmRenderTheme.kt` | `Throwable` a `create` függvényben | `osm_theme_create` | nincs |
| `tuhu/TuhuRenderTheme.kt` | `Throwable` a `create` függvényben | `tuhu_theme_create` | nincs |
| `data/maps/OsmDownloadWorker.kt` | `Exception` a `doWork` függvényben, a `CancellationException` továbbdobása után | `osm_download` | nincs |
| `tuhu/TuhuDownloadWorker.kt` | `Exception` a `doWork` függvényben, a `CancellationException` továbbdobása után | `tuhu_download` | nincs |

Minden `catch` meglévő helyreállítása maradjon (folyam lezárása, alap theme, részleges letöltés törlése, `onOsmFailed`, zoom a határoló közepére, rögzítés leállítása, menet lezárása). A jelentés hozzáadódik. A helyreállítás nem kerül ki.

Ha a beépítés közben új `catch` jelenik meg az `app/src/main` alatt, azt is jelentsd, új stabil `op` névvel. Ne maradjon olyan produkciós `catch`, amely eldobja a kivételt.

## Build

Katalógus: `gradle/libs.versions.toml`. Gyökér pluginok: `build.gradle.kts`. App modul: `app/build.gradle.kts`. Az AGP 9.2.1, a Kotlin 2.2.10. A `firebase-bom`, a `com.google.gms.google-services` és a `com.google.firebase.crashlytics` aktuális stabil verzióját pineld, olyat, amely ezt az AGP-t támogatja. Lebegő verziót ne használj. Verziót ne találj ki emlékezetből; beépítéskor olvasd el az aktuális Firebase Android kiadási jegyzetet.

Függőség, csak ezek:

- `implementation(platform(...firebase-bom...))`
- `implementation(...firebase-crashlytics...)`

A `com.google.gms.google-services` és a `com.google.firebase.crashlytics` plugint az app modulban csak akkor alkalmazd, ha az `app/google-services.json` létezik. A fájl nélküli checkoutnak fordulnia és összeállnia kell. A `BuildConfig` már be van kapcsolva. Adj hozzá egy `CRASHLYTICS_ENABLED` logikai mezőt, amely csak akkor igaz, ha a build típus release és a json fájl létezik, egyébként hamis.

Az app modulba tegyél egy `CrashReport` típust, `record(op: String, sessionId: Long?, error: Throwable)` függvénnyel. Ha a `CRASHLYTICS_ENABLED` hamis, térj vissza minden `FirebaseCrashlytics` hívás előtt. Ha igaz, állítsd az `op` kulcsot. A `session_id` kulcsot csak akkor állítsd, ha a `sessionId` nem null. Utána hívd a `recordException(error)` metódust.

A debug gyűjtés kikapcsolva marad, hogy a helyi összeomlás ne érje el a konzolt. Adj hozzá egy `app/src/debug/AndroidManifest.xml` fájlt (manifest merger) ezzel:

```xml
<meta-data
    android:name="firebase_crashlytics_collection_enabled"
    android:value="false" />
```

A release az SDK alapértelmezését használja, amely küldi a jelentést. A Crashlytics Gradle-plugin a release bundle részeként feltölti az R8 `mapping.txt` fájlját, ha a plugin alkalmazva van. Az `isMinifyEnabled` már igaz. A minify-t ne kapcsold ki. Az `ndk.debugSymbolLevel = SYMBOL_TABLE` sort hagyd meg; az a Playhez kell, nem ehhez az SDK-hoz.

A Firebase-projektet és a `com.lkovari.mobile.apps.gtl` `applicationId`-hoz tartozó `google-services.json` letöltését az ember végzi, a fájl helye `app/google-services.json`. A kód ezt a fájlt nem tartalmazza.

## Adatvédelmi tájékoztató

A SDK-t tartalmazó Play-kiadás előtt frissítsd a tároló `docs/play-console/privacy-policy.html` fájlját. Az alkalmazás Súgója csak a `https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html` címet nyitja meg. Ne készíts második tájékoztató-képernyőt, és emiatt ne módosítsd a `strings.xml` fájlt. Az élő KLHome-oldal közzététele a kiadó kézi lépése; a helyi HTML-nek és az élő oldalnak egyeznie kell a görgetés előtt. Mindkét „Utolsó frissítés” sor a szövegváltozás napja legyen.

Az oldal kétnyelvű (`data-lang="en"` és `data-lang="hu"`). Ugyanabban a fájlban mindkét nyelvet módosítsd.

### Összefoglaló

Az angol bekezdést cseréld erre:

> GTL is a GPS track logger. It records your route, satellite status, and optional ambient temperature on this phone. There is no account. We do not operate a tracking server. The app uses the internet for Google Maps tiles (if you set an API key), for OpenStreetMap region file downloads that you start, and to send a crash report to Google Firebase Crashlytics the next time you open the app after a crash or a caught error.

A magyar bekezdést cseréld erre:

> A GTL GPS útvonalnapló. Az útvonalat, a műholdállapotot és — ha van szenzor — a környezeti hőmérsékletet ezen a telefonon rögzíti. Nincs fiók. Nem üzemeltetünk követőszervert. Internetre a Google Térkép csempéihez (ha beállítasz API-kulcsot), az általad indított OpenStreetMap-letöltésekhez, és ahhoz van szükség, hogy egy összeomlás vagy egy elkapott hiba után a következő megnyitáskor hibajelentés menjen a Google Firebase Crashlyticsnek.

### Adatok a készüléken

Az angol bekezdéshez fűzd hozzá:

> A crash report may remain in app-private storage until the next time you open the app, and is then sent as described under Crash reports.

A magyar bekezdéshez fűzd hozzá:

> A hibajelentés az app saját tárhelyén maradhat a következő megnyitásig, és utána a Hibajelentések szakasz szerint megy el.

### Új szakasz a Google Térkép után, az OpenStreetMap-letöltések előtt

Angolul:

> ## Crash reports
>
> If the app crashes, stops responding, or catches an error, GTL stores a crash report on the device and sends it to Google Firebase Crashlytics the next time you open the app. The report contains the stack trace, app version, device model, Android version, an operation name for the code path, and, when a recording is open, a numeric session id. It does not contain latitude, longitude, altitude, speed, temperature, pressure, satellite details, or the track. Google processes the report, including the IP address used at upload time. Google’s policy: https://policies.google.com/privacy

Magyarul:

> ## Hibajelentések
>
> Ha az alkalmazás összeomlik, nem válaszol, vagy hibát kap el, a GTL hibajelentést tárol a készüléken, és a következő megnyitáskor elküldi a Google Firebase Crashlyticsnek. A jelentésben stack trace, appverzió, készülékmodell, Android-verzió, a kódútvonal műveletneve, és nyitott rögzítéskor egy numerikus menetazonosító van. Nincs benne szélesség, hosszúság, magasság, sebesség, hőmérséklet, nyomás, műholdadat vagy az útvonal. A Google feldolgozza a jelentést, a feltöltéskori IP-címmel együtt. A Google tájékoztatója: https://policies.google.com/privacy

A Google Térkép szakasz azon mondatai maradjanak, amelyek szerint az útvonal-adatbázis nem megy a Google-hoz. A hibajelentés ez az új szakasz, nem a térképcsempe.

### Amit nem csinálunk

Az angol `No ads or third-party analytics SDKs` pont helyett:

> No ads, no product-analytics SDK, and no advertising identifier. Firebase Crashlytics is used only for the crash reports described above.

A magyar `Nincs reklám és külső analitikai SDK` pont helyett:

> Nincs reklám, nincs termékanalitikai SDK, és nincs hirdetésazonosító. A Firebase Crashlytics csak a fent leírt hibajelentésre szolgál.

A többi pont marad (nincs fiók, nincs IMEI, nincs élő hely feltöltése, személyes adatot nem adunk el).

## Play Console adatbiztonság

Ez a Play Console kézi űrlapja, nem kódmódosítás. Az SDK-t tartalmazó kiadás előtt töltsd ki. Ebben a szeletben nincs alkalmazáson belüli kikapcsolás, ezért a gyűjtés annak a verziónak minden telepítőjére kötelező.

Beépítéskor kövesd a Firebase aktuális „Prepare for Google Play’s Data safety section” oldalát. A minimumnak ehhez a kódhoz kell igazodnia:

- Crash logs: gyűjtött. A cél az, amit a Firebase a Crashlyticshoz ír (diagnosztika, nem hirdetés).
- Eszköz- vagy egyéb azonosító csak akkor, ha az az oldal ehhez az SDK-verzióhoz még felsorolja a Crashlytics telepítési UUID-t.
- Hely, precíz hely és az útvonal tartalma: ezt a funkciót nem gyűjti.
- Az adatot a Google dolgozza fel, Crashlytics-szolgáltatóként. Eladásra és hirdetésre nem kerül.

A Play Android vitals megmarad. A Crashlytics nem váltja ki.

## Ellenőrzés

- A debug build se dobott kivételre, se `recordException` hívásra ne hozzon létre Crashlytics-issue-t.
- A `google-services.json` jelenlétében készült release build, a folyamat újraindítása után, elkapott hibára nem végzetes issue-t mutat. A stack a kapó osztály nevét tartalmazza, az `op` a fenti nevek egyike, és a jelentés szövegében nincs szélesség vagy hosszúság. A `session_id` az `insert_event` és az `insert_stop` jelentésen megvan, a többin nincs.
- Ugyanez a release build egy nem kezelt tesztkivételre csak a következő hideg indulás után mutat fatal issue-t.
- Az OSM- vagy a Turistautak-letöltés megszakítása nem hoz létre Crashlytics-issue-t, és a lenyelt `CancellationException` nem lesz `Result.failure()`.
- A sikertelen beszúrás után a már lementett `gps_events` pontok a készüléken maradnak. A Tracks inspect ugyanazt a menetet megnyitja.
- A `./gradlew :app:assembleDebug` lefut, ha az `app/google-services.json` hiányzik.
- A Room sémaverziója változatlan.
