# GTL kódreview — 2026-09-20

**Projekt:** GPS Track Logger (GTL), Kotlin + Jetpack Compose, `versionName` 2.0.11, `versionCode` 29  
**Hatáskör:** a teljes fa (`app/`, `engine/`), nem egyetlen PR-diff.  
**Nyelv:** magyar.  
**Súlyosság:** Kritikus → Magas → Közepes → Alacsony. Csak a forrásban ellenőrzött, valós hibák.

Minden tétel: forrásfájl, sorszám, mi a baj, miért baj, javasolt javítás, miért oldja meg.

> **Állapot-frissítés (2026-09-22):** minden tételt újraellenőriztünk a jelenlegi munkakönyvtárban, a részben javított tételek lezárása után is. Nyitva maradt: Kz5, A1, A3. Az egyes tételek végén **Ellenőrzés** jelöléssel szerepel, mi lett javítva és mi nem. Jelmagyarázat: ✅ Javítva · 🟡 Részben javítva · ❌ Nem javítva.

---

## Rövid rangsor

| # | Súly | Tétel | Állapot (2026-09-22) |
|---|------|--------|------------------------|
| K1 | Kritikus | Stop utáni gyors Start lezárja / megöli az új felvételt | ✅ Javítva |
| K2 | Kritikus | Az élő session törlése csendben leállítja a pontírást | ✅ Javítva |
| M1 | Magas | Tuhu térkép HTTP-n, méretkorlát nélkül, tetszőleges redirect | ✅ Javítva |
| M2 | Magas | Közelítő (coarse) helyengedéllyel is elindul a naplózás | ✅ Javítva |
| M3 | Magas | „Csak GNSS” fused/hálózati pontra esik, ha a GPS ki van | ✅ Javítva |
| M4 | Magas | Hiányzó `accuracy` = 0 m, átmegy a szűrőn | ✅ Javítva |
| M5 | Magas | Fused cache-elt utolsó pont lehet a START | ✅ Javítva |
| M6 | Magas | A STOP falióra-időt kap, a többi pont GPS-időt | ✅ Javítva |
| M7 | Magas | „Mutasd a térképen” felülírja a naplózó beállításokat | ✅ Javítva |
| M8 | Magas | Felvétel közben két LocationClient fut | ✅ Javítva |
| M9 | Magas | Hiányzó sebesség/magasság 0-ként tárolódik | ✅ Javítva |
| M10 | Magas | Repülőmód: 9000 m felett a magasság szemétnek számít | ✅ Javítva |
| M11 | Magas | Baro autokalibráció elfogadja a 0 / NaN nyomást | ✅ Javítva |
| M12 | Magas | Az első NaN/Inf fix és a visszafelé ugró időbélyeg bekerül a Roomba | ✅ Javítva |
| Kz1 | Közepes | Nincs hőmérő, a HUD mégis 0°-ot mutat | ✅ Javítva |
| Kz2 | Közepes | Haversine `a > 1` esetén NaN-ná mérgezi a távot | ✅ Javítva |
| Kz3 | Közepes | OSM `renameTo` sikertelenség után törölt a régi térkép | ✅ Javítva |
| Kz4 | Közepes | Android 13+: ha a FINE már megvan, a notification permission elmarad | ✅ Javítva |
| Kz5 | Közepes | A felvétel szűrője a Start pillanatában lefagy | ❌ Nem javítva |
| Kz6 | Közepes | Minden pontot eldob a szűrő, a UI továbbra is „Naplóz” | ✅ Javítva |
| A1 | Alacsony | Room `exportSchema = false` | ❌ Nem javítva |
| A2 | Alacsony | OSM / Tuhu letöltésnek nincs tárhely-plafonja | ✅ Javítva |
| A3 | Alacsony | A Tuhu worker az OSM work-taget viseli | ❌ Nem javítva |

---

## Kritikus

### K1. Stop utáni gyors Start lezárja az új felvételt — ✅ Javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/service/TrackingForegroundService.kt`, **62–67** és **264–325**
- **Probléma:** A `stopRecording()` először `locationJob = null`, majd *külön* `lifecycleScope.launch`-ban írja a STOP-ot, hívja a `stopSession()`-t, nullázza a `LiveTrackingState`-et és `stopSelf()`-et. A `startRecording()` csak azt nézi, hogy `locationJob != null`. Gyors Stop → Start (értesítés Stop + app Start, vagy dupla koppintás) közben:

  1. az új Start üres `locationJob`-ot lát, session-t nyit vagy újrahasznál, `logging = true`;
  2. a még futó stop-coroutine STOP-ot ír, `stoppedAt`-et rak, state-et töröl, `stopSelf()`-et hív.

  A stop a *aktuális* `sessionId`-t olvassa a state-ből, tehát az új session-t zárhatja le, nem a régit.
- **Miért probléma:** Az éppen elindított track azonnal véget ér, vagy a régi session lezárul, az új nyitva marad író nélkül. Adatvesztés a termék fő funkcióján.
- **Javítás:** Generáció / token (AtomicInteger) a start–stop párhoz, vagy egyetlen mutex. A `startRecording()` várja meg a stop végét, vagy a stop ne hívjon `stopSelf()`-et / `stopSession()`-t, ha már újabb generáció ment. A stop a *saját* `sessionId`-jét zárja, ne a state későbbi értékét.
- **Miért oldja meg:** A stop többé nem nyúlhat olyan sessionhöz és service-hez, amit egy újabb Start már birtokol.
- **Ellenőrzés (2026-09-21):** `TrackingForegroundService.kt`-ban megjelent a `recordingLock: Mutex` és a `recordingGeneration: AtomicInteger`. A start és a stop egyaránt a lock alatt fut; a stop a saját `activeSessionId`-ját zárja, és a végén ellenőrzi, hogy `recordingGeneration.get() == stopGen` — ha közben egy új Start már megnövelte a generációt, a régi stop-coroutine nem hívja meg a `stopForeground`/`stopSelf`-et. A leírt hibamintát ez kizárja.

---

### K2. Az élő session törlése csendben leállítja a pontírást — ✅ Javítva

- **Forrás:**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/viewmodel/GtlViewModel.kt`, **511–523**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/ui/screens/SecondaryScreens.kt`, **966–998**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/service/TrackingForegroundService.kt`, **213–235**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/data/db/Entities.kt`, **17–26**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/data/db/Daos.kt`, **17–18**
- **Probléma:** A mentett lista minden sessiont mutat, a nyitottat is (`stoppedAt IS NULL`). A törlés gomb felvétel közben is él. A `gps_events.sessionId` `ON DELETE CASCADE`. A következő `insertEvent()` `SQLiteConstraintException`. A `collectLocation` nincs try/catch-ben, a child coroutine meghal. A `lifecycleScope` supervisor, az FGS `logging = true` marad.
- **Miért probléma:** A Start gomb Stop-ot mutat, az értesítés megvan, de innentől egyetlen pont sem íródik. A user azt hiszi, megy a track.
- **Javítás:**
  1. Törlés tiltása, ha `id == live.sessionId` (és a nyitott sessiont jelölni a listában).
  2. `insertEvent` köré try/catch; FK / SQLite hiba esetén tiszta `stopRecording()` és hiba a HUD-on.
- **Miért oldja meg:** Az író sessionje nem tűnhet el alóla, és ha mégis DB-hiba van, nem marad zombi előtér-szolgáltatás.
- **Ellenőrzés (2026-09-21):** `GtlViewModel.deleteSession` most visszatér, ha `id == app.trackingState.state.value.sessionId` — élő session nem törölhető. A `SecondaryScreens.kt`-ban a nyitott session „Naplóz” címkét kap, a törlés gombja `enabled = !recording`. Az `insertEvent` mindkét hívási helyen (`collectLocation` és `stopRecording`) try/catch(`SQLException`)-ben fut; hiba esetén `loggingError = true` és tiszta `stopRecording()`, a HUD `status_logging_error` = „Naplózás leállt” szöveget mutatja.

---

## Magas

### M1. Tuhu térkép HTTP-n, méretkorlát nélkül, tetszőleges redirect — ✅ Javítva

- **Forrás:**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/tuhu/TuhuCatalog.kt`, **5**
  - `app/src/main/res/xml/network_security_config.xml`, **3–6**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/tuhu/TuhuDownloadWorker.kt`, **59–98**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/tuhu/TuhuZip.kt`, **34–55**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/tuhu/TuhuDownloadWorker.kt`, **44–48** (zipből másolt `theme.xml`)
- **Probléma:** A `ZIP_URL` `http://turistautak.elte.hu/tuhu/tuhu_mapsforge.zip`. A cleartext szándékosan engedélyezett erre a hosztra. `instanceFollowRedirects = true` (a redirect már HTTPS-re, más hosztra is mehet). Nincs letöltési, bejegyzés-szám vagy kicsomagolt-bájt plafon. A zip-slip kanonikus úttal le van védve; zip-bomba / teli tárhely nincs.
- **Miért probléma:** Nyilvános Wi‑Fi-n a turistatérkép kicserélhető (rossz ösvény = biztonsági kockázat), a redirect tetszőleges payloadot húz, egy tömörített bomba betelíti a telefont, a theme XML a Mapsforge rendererbe kerül.
- **Javítás:** HTTPS (ha a szervernek nincs TLS-e: a letöltés hiúsuljon meg, ne HTTP fallback). Hoszt+path allowlist, idegen hosztra ne kövesse a redirectet. Pl. 500 MB letöltés, 1 GB kicsomagolás, max néhány ezer entry; afelett abort. A zip-slip check marad. A theme-et csak Mapsforge rendertheme-ként fogadja el.
- **Miért oldja meg:** A payloadot nem lehet a vezetéken kicserélni, és egy zip-bomba nem töltheti meg a tárhelyet.
- **Ellenőrzés (2026-09-21):** `ZIP_URL` még mindig `http://…`, a `network_security_config.xml` még engedi a cleartextet erre a hosztra, a `TuhuDownloadWorker.instanceFollowRedirects` még `true`. Nincs host/path allowlist, nincs méret- vagy entry-plafon. A tétel változatlanul nyitott.
- **Ellenőrzés (2026-09-22):** A hálózati és a bomba-rész kész, és a két kimaradt pont is. `TuhuDownloadPolicy.isAllowedUrl` csak `https` + `turistautak.elte.hu` + pontos path `/tuhu/tuhu_mapsforge.zip`; userinfo és query tiltott. A `TuhuZip` a theme-et nem az első `.xml`-ként veszi: az első 8 KiB-ból az első start tagnek `rendertheme`-nek kell lennie, különben a XML kimarad, a `.map` megmarad, a renderer az asset theme-re esik. Letöltési plafon 500 MB, kicsomagolás 1 GB, legfeljebb 4000 bejegyzés. Van teszt a rossz pathra és a nem-rendertheme XML-re.

---

### M2. Közelítő helyengedéllyel is elindul a naplózás — ✅ Javítva

- **Forrás:**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/ui/screens/MainTrackerScreen.kt`, **101–108**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/data/location/LocationClient.kt`, **35–44**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/data/gnss/GnssStatusSource.kt`, **26–32**
- **Probléma:** A `startLauncher` akkor is `startLogging()`-ot hív, ha csak `ACCESS_COARSE_LOCATION` jött (Android 12+ „Közelítő”, kb. 1–2 km). A `LocationClient` FINE **vagy** COARSE mellett ad flow-t. A `GnssStatusSource` viszont **csak FINE**-nal regisztrál, különben `emptyFlow()`. A `satellitesInFix` így 0, a default `minSatellites = 4` minden pontot eldob.
- **Miért probléma:** Start után FGS fut, a track üres vagy városnegyed-szemét. Nincs magyarázat.
- **Javítás:** Naplózás csak `ACCESS_FINE_LOCATION` mellett. Közelítő választásnál új kérés + rövid indok. A `LocationClient.locations` COARSE-only esetén `emptyFlow()`.
- **Miért oldja meg:** Felvétel csak akkor indul, ha a chip GNSS-minőségű pontot tud adni, és a műholdszám is él.
- **Ellenőrzés (2026-09-21):** A `MainTrackerScreen.kt` `startLauncher`-je jelenleg is elindítja a naplózást, ha `grants[ACCESS_COARSE_LOCATION] == true` (a FINE nincs meg). A `GnssStatusSource` továbbra is csak FINE mellett ad snapshotot. A leírt hibamód (COARSE-szal induló, üres/rossz track) fennáll.
- **Ellenőrzés (2026-09-22):** A Start csak `ACCESS_FINE_LOCATION` mellett hívja a `startLogging()`-ot. Ha a felhasználó csak közelítő engedélyt ad, a launcher toastot mutat (`location_fine_required`) és nem indul felvétel. A `LocationClient.locations` FINE nélkül `emptyFlow()`-t ad. A leírt hibamód zárva van.

---

### M3. „Csak GNSS” fused/hálózati pontra esik, ha a GPS ki van — ✅ Javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/data/location/LocationClient.kt`, **46–51**
- **Probléma:** `gnssOnly && isProviderEnabled(GPS_PROVIDER)` esetén GPS_PROVIDER, különben mindig fused. Ha a user GNSS-only módban van (Fut/túra, kerékpár alapértelmezés), de a rendszer-GPS ki van kapcsolva, Wi‑Fi/cella fused jön. A `minSatellites = 4` ezeket általában el is dobja.
- **Miért probléma:** A kapcsoló ígérete („GNSS track, nem utcára pattintás”) nem teljesül, vagy a track üres marad, üzenet nélkül. A belső `docs/GPSDATAFLOW-hu.md` említi a fused fallbacket; a beállítás neve és a termékígéret ennek ellentmond.
- **Javítás:** `gnssOnly && !GPS_PROVIDER` → ne hívj `fusedLocations()`-t, üres flow + HUD: „Kapcsold be a GPS-t”.
- **Miért oldja meg:** Amit a user beállított, az kerül a fájlba; fused szemét nem keveredik a GNSS-trackbe.
- **Ellenőrzés (2026-09-21):** `LocationClient.locations()` logikája változatlan: `gnssOnly && GPS_PROVIDER enabled` → GPS, minden más esetben (GPS letiltva is) `fusedLocations()`. Nincs üres flow / HUD-figyelmeztetés bevezetve.
- **Ellenőrzés (2026-09-22):** `gnssOnly` és kikapcsolt `GPS_PROVIDER` esetén a `LocationClient` `emptyFlow()`-t ad, nem fusedet. A service `gpsOff = true`-t állít, a HUD és az értesítés a `status_gps_off` szöveget mutatja („Kapcsold be a GPS-t”). Fused pont nem kerül a GNSS-only trackbe.

---

### M4. Hiányzó pontosság 0 m-nek számít, átmegy a kapun — ✅ Javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/service/TrackingForegroundService.kt`, **172**, **179–181**
- **Probléma:** `Location.accuracy` Androidon `0.0`, ha `!hasAccuracy()`. A szűrő `accuracyMeters > minAccuracyMeters` (30 vagy 45). A 0 átmegy: „tökéletes” pont.
- **Miért probléma:** Ismeretlen minőségű fused/cache pontok START-ként és MOVE-ként tárolódnak.
- **Javítás:** `!location.hasAccuracy()` vagy `accuracy <= 0` → eldobni, még a `FixAcceptance` előtt.
- **Miért oldja meg:** Csak az a fix marad meg, aminek van valós vízszintes hibabecslése.
- **Ellenőrzés (2026-09-21):** `collectLocation`-ben a szűrés még mindig csak `fix.accuracyMeters > filter.minAccuracyMeters`; nincs `hasAccuracy()`/`accuracy <= 0` ellenőrzés se a `TrackFix` építése előtt, se a `FixAcceptance`-ben. A hiba fennáll.
- **Ellenőrzés (2026-09-22):** `FixAcceptance.hasUsableAccuracy` csak akkor igaz, ha `hasAccuracy` és a pontosság véges és `> 0`. A `collectLocation` ezt a kaput a tárolás előtt hívja, a 0 m-es „tökéletes” pont nem kerül a Roomba. Van rá teszt.

---

### M5. A fused első pontja lehet tegnapi last-known — ✅ Javítva

- **Forrás:**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/data/location/LocationClient.kt`, **127–133**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/service/TrackingForegroundService.kt`, **166**, **206–207**
- **Probléma:** `setWaitForAccurateLocation(false)`: a Play Services először cache-elt fixet adhat. Nincs `elapsedRealtimeNanos` / kor ellenőrzés. Az első elfogadott pont START.
- **Miért probléma:** Az új track kilométerekkel arrébb indul, aztán ugrik a valós helyre. KMZ/GPX START balloon hamis.
- **Javítás:** 5–10 s-nél idősebb pontot (`elapsedRealtimeNanos`) dobni. Felvételnél `setWaitForAccurateLocation(true)` (HUD preview maradhat gyors).
- **Miért oldja meg:** A START élő GNSS-pont, nem a tegnapi last-known.
- **Ellenőrzés (2026-09-21):** `fusedLocations()` még mindig `setWaitForAccurateLocation(false)`-t használ, nincs kor-/`elapsedRealtimeNanos`-alapú szűrés a cache-elt fixre. A hiba fennáll.
- **Ellenőrzés (2026-09-22):** Felvételkor `setWaitForAccurateLocation(true)`. A `LocationClient.isFreshEnough` a 10 másodpercnél régebbi pontot (`elapsedRealtimeNanos`) eldobja, és a nem pozitív elapsed időt is. A START nem lehet tegnapi last-known. A HUD-előnézet (`recording = false`) továbbra is azonnali marad.

---

### M6. A STOP falióra-időt kap, a többi pont GPS-időt — ✅ Javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/service/TrackingForegroundService.kt`, **166–167** vs **294**; `app/src/main/java/com/lkovari/mobile/apps/gtl/data/db/TrackRepository.kt`, **24**, **34**
- **Probléma:** A MOVE/START/PAUSE `location.time` (GPS UTC). A STOP `System.currentTimeMillis()`. Eltérő eszközóra (gyakori) → a STOP a track közepére vagy órákkal későbbre esik. A `listBySession` `ORDER BY timestamp ASC`.
- **Miért probléma:** Időtartam, GPX `<time>`, KML `<when>`, átlagsebesség, a STOP sorrendje hamis. Crash után `latestForSession` rossz „utolsó” pontot ad.
- **Javítás:** A STOP timestampje `stopFix.timestampMillis` (az utolsó elfogadott GPS-idő). A session `startedAt` / `stoppedAt` maradhat falióra a listázáshoz, de a pontok egy órához tartozzanak.
- **Miért oldja meg:** A STOP a GPS-idősor vége marad, a lejátszás és a statisztika nem keveri a két órát.
- **Ellenőrzés (2026-09-21):** `stopRecording()`-ban a STOP eseményhez írt `timestamp` még mindig `System.currentTimeMillis()`, nem a `stopFix.timestampMillis`. A hiba fennáll (a mutex/generation-javítás — K1 — csak a versenyhelyzetet oldotta meg, az időbélyeg-forrást nem érintette).
- **Ellenőrzés (2026-09-22):** A STOP `timestamp` mezője `stopFix.timestampMillis`. A fix az utolsó elfogadott pont, különben a szűrt, különben az utolsó `location.time`. Nem `System.currentTimeMillis()`. A pontok egy GPS-idősoron maradnak.

---

### M7. „Mutasd a térképen” felülírja a naplózó profilt — ✅ Javítva

- **Forrás:**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/viewmodel/GtlViewModel.kt`, **486–499**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/data/prefs/GtlPreferences.kt`, **157–169**
- **Probléma:** A `showSessionOnMap` meghívja a `setUsageType()`-ot. Az a DataStore-ba írja a usage default min táv/idő/pontosság/műhold, simítás, sűrűség, `gnssOnly`, mértékegység, OSM cycleways értékeket. A gomb a mentett listából felvétel közben is elérhető.
- **Miért probléma:** Egy régi túra megnyitása letörli a user által hangolt logger-beállítást. Motorozás közben egy hike-session megnyitása GNSS-only + más szűrőre állítja a *következő* menetet (a futó service a Startkor olvasott snapshotot használja — lásd Kz5 —, a DataStore viszont már más).
- **Javítás:** Itt ne hívj `setUsageType`-ot. A térkép ikonját a `session.usageType`-ból számold, vagy legyen egy setter, ami csak a megjelenített usage nevet tárolja.
- **Miért oldja meg:** Track megtekintése nem mutatja a felvételi profilt.
- **Ellenőrzés (2026-09-21):** `GtlViewModel.showSessionOnMap` már nem hívja a `setUsageType()`-ot, a preferenciamódosító blokk törölve lett. A track megtekintése nem írja felül a naplózó beállításokat.

---

### M8. Felvétel közben két LocationClient (és két GNSS-figyelő) fut — ✅ Javítva

- **Forrás:**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/viewmodel/GtlViewModel.kt`, **161–168**, **179–190**, **465–468**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/service/TrackingForegroundService.kt`, **104–108**, **175–177**
  - `docs/GPSDATAFLOW-hu.md` (a doksi: Start után a HUD-ot a service írja; a ViewModel csak idle-ben hallgat)
- **Probléma:** A `startLogging()` újra `startPreview()`-t hív. A ViewModel 1000 ms-en, szűrő nélkül kéri a GPS/fused-et, és írja a `lastLocation`-t. A service is írja. Két `GnssStatusSource.snapshots()` collector = két `registerGnssStatusCallback`. Ugyanez szenzorra (nyomás, iránytű, gyorsulás).
- **Miért probléma:** Extra GNSS/fused terhelés (akkumulátor). A HUD, a térkép és a `calibrateBaroFromGps()` más pontot láthat, mint ami a Roomba kerül. A saját adatfolyam-doksi ezt idle-re korlátozza.
- **Javítás:** `logging == true` alatt a ViewModel location / GNSS / szenzor collectora álljon. Egyedül a service írja a `lastLocation`-t és a `gnss` snapshotot.
- **Miért oldja meg:** Egy pipeline: a HUD megegyezik a tárolt vonallal, feleakkora GNSS-költség.
- **Ellenőrzés (2026-09-21):** `GtlViewModel.startLogging()` továbbra is meghívja a `startPreview()`-t (`listenGnss`/`listenLocation`/…), amely külön `LocationClient`-et és GNSS-figyelőt indít, a service sajátja mellett. Nincs `logging == true` alatti leállítás. A hiba fennáll.
- **Ellenőrzés (2026-09-22):** A `startLogging()` csak a foreground service-t indítja, nem hív `startPreview()`-t. Az `observeLoggingPreview` `logging == true` alatt `stopPreview()`-t hív, ami leállítja a ViewModel hely-, GNSS- és szenzor-collectorait. Felvétel közben a service írja a `lastLocation`-t és a GNSS-snapshotot.

---

### M9. Hiányzó sebesség PAUSE-t, hiányzó magasság 0 m-t csinál — ✅ Javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/service/TrackingForegroundService.kt`, **169–171**, **208–209**, **219**; `app/src/main/java/com/lkovari/mobile/apps/gtl/data/db/Entities.kt`, **35–36**
- **Probléma:** `Location.speed` / `altitude` 0, ha a mező hiányzik. A `withTrustedAltitude()` levághatja a magasságot; az event akkor is `0.0`-t tárol (`altitude` NOT NULL). Az első GNSS-fixeken gyakran nincs sebesség. `speedMps < pauseSpeedMps()` (0.25 / 0.4) → PAUSE.
- **Miért probléma:** Mozgás PAUSE-nak minősül (várakozási idő felduzzad). Magasságprofil / KML / GPX tengerszintre esik, majd vissza. A `TrackStats` min/max magassága 0-t kever.
- **Javítás:** `!hasSpeed()` → ne classifikálj PAUSE-t (MOVE, vagy az utolsó ismert sebesség). `!hasAltitude()` → ne írj 0-t; séma: `altitude` nullable (migráció 4→5), exportban hiányzó `<ele>`. Addig: hagyd ki a pont magasságát a statisztikából, ha 0 *és* `!hasAltitude` nem tárolható — legalább a `GpsAltitude.pick == null` ágon külön flag.
- **Miért oldja meg:** Az ismeretlen szenzormező nem viselkedik valós nullaként.
- **Ellenőrzés (2026-09-21):** A PAUSE/MOVE osztályozás és az `insertEvent` hívás változatlan, nincs `!hasSpeed()`/`!hasAltitude()` speciális kezelés. A hiba fennáll.
- **Ellenőrzés (2026-09-22):** A magasság nullable (migráció 4→5). A sebesség is: séma 6, `MIGRATION_5_6`, `speed REAL` nullable. `!hasSpeed()` esetén a Roomba `null` kerül, az esemény MOVE marad, a szűrő csak az utolsó ismert sebességet használja bemenetként és azt nem írja oszlopba. A `TrackStats` a `null` sebességet sem a várakozáshoz, sem a maxhoz nem számolja. Az élő HUD csak `hasSpeed()` mellett mutat számot, különben „—”. A KML sebességtömb hiányzó értéknél `-`. Van teszt (`nullSpeedDoesNotCountAsWaitingAndIsSkippedForMax`).

---

### M10. Repülőmód: 9000 m felett a magasság szemét — ✅ Javítva

- **Forrás:** `engine/src/main/kotlin/com/lkovari/mobile/apps/gtl/engine/GpsAltitude.kt`, **4–8**, **11–19**; felhasználva `LocationClient.kt` **160–173**, `BaroAltitude.kt` **52–53**
- **Probléma:** `MaxPlausibleMeters = 9000`. Az `AIRCRAFT` első osztályú usage (ICAO, Kalman). FL300 = 9144 m, utasszállító cruise 10–12 km. GNSS MSL és ellipszoid is kiesik. A service `removeAltitude()` után `location.altitude` = **0.0**-t tárol (M9). A baro `MaxGpsDeltaMeters` (1500) a 0-hoz képest eldobja a 11 km-es barót is.
- **Miért probléma:** Egy repülőtrack GPS- és baro-magasság nélkül marad; GPX `<ele>0.0</ele>`, statisztika 0. A README „typical flight level”-nek írja a 9000-et; az utasszállító cruise efelett van. A −1787-es Android-szemét továbbra is kint maradhat egy magasabb plafonnal.
- **Javítás:** Pl. `MaxPlausibleMeters = 20000` (~FL650). A −430 (Holt-tenger) alsó korlát marad. Teszt: 11 000 m `isPlausible == true`, −1787 `false`.
- **Miért oldja meg:** A 10–12 km érték bent marad; statisztika, profil, GPX/KML és a baro-vs-GPS kapu ugyanazt a flight-level számot látja, nem 0-t.
- **Ellenőrzés (2026-09-21):** `GpsAltitude.MaxPlausibleMeters` még mindig `9000.0`. A hiba fennáll.
- **Ellenőrzés (2026-09-22):** `MaxPlausibleMeters = 20000.0`, az alsó korlát −430 m maradt. A 11 000 m `isPlausible == true` (teszt: `cruiseAltitude11000IsPlausible`). A −1787-es Android-szemét továbbra is kiesik.

---

### M11. Baro autokalibráció elfogadja a 0 / NaN nyomást — ✅ Javítva

- **Forrás:** `engine/src/main/kotlin/com/lkovari/mobile/apps/gtl/engine/BaroAltitude.kt`, **18–20**, **34–37**, **39–56**; hívó: `TrackingForegroundService.kt` **242–261**; manuális: `GtlViewModel.kt` **689–706**
- **Probléma:** Az `autoCalibrateEligible` csak `pressureHpa == null`-t utasít el. A `0f` és a `NaN` átmegy. `offsetHpa` → `clampOffset(pressure - expected)`. A `Float.coerceIn(-10f, 10f)` a NaN-t **nem** klampolja (összehasonlítás false), az offset NaN lehet. Nyomás 0-nál az offset `clamp(0 - ~1013) = -10` hPa. Sessionenként egyszer (`alreadyCalibratedThisSession`).
- **Miért probléma:** Néhány chip az első mintában 0-t ad. −10 hPa ~80 m eltolás a session összes baro értékén. NaN offset → `metersFromPressureHpa` NaN a session végéig (a `corrected <= 0f` NaN-ra false).
- **Javítás:** Elutasítani a nem véges és a fizikátlan nyomást (kb. 300–1100 hPa) az eligible *és* az `offsetHpa` / `clampOffset` elején. `!hpa.isFinite() → 0f` helyett: ne kalibrálj. Ugyanez a `calibrateBaroFromGps()`-ben.
- **Miért oldja meg:** Egy szemét indulóminta nem zárja be a session offsetjét; baro addig null, amíg valós hPa nem jön.
- **Ellenőrzés (2026-09-21):** `BaroAltitude`-ban megjelent `isPlausiblePressureHpa` (300–1100 hPa); ezt hívja az `autoCalibrateEligible` és az `offsetHpa` is, a `clampOffset` pedig NaN esetén 0f-et ad vissza. A service (`maybeAutoCalibrateBaro`) és a `GtlViewModel` manuális `calibrateBaroFromGps()`-e is ellenőrzi az új feltételt Startkor/hívás előtt. Új unit tesztek is fedik (`autoCalibrateNotEligibleForZeroNanOrLowPressure`, `offsetHpaRejectsNonPlausiblePressure`).

---

### M12. Az első NaN/Inf fix és a visszafelé ugró időbélyeg bekerül a Roomba — ✅ Javítva

- **Forrás:**
  - `engine/src/main/kotlin/com/lkovari/mobile/apps/gtl/engine/FixAcceptance.kt`, **38–47**, **65–66**, **73–74**
  - `engine/src/main/kotlin/com/lkovari/mobile/apps/gtl/engine/KalmanTrackFilter.kt`, **15–19**, **37–42**
- **Probléma:** Pontosság/műhold után az első fix ellenőrzés nélkül ACCEPT. Később csak táv/idő. Nincs `isFinite(lat/lon)`, nincs `|lat|≤90`, nincs `timestamp <= previous` elutasítás. A Kalman `observe` a későbbi NaN-t dobja, a `seedFrom` nem; első `observe` `lastOutput == null` esetén a nyers NaN-t adja vissza.
- **Miért probléma:** NaN/Inf a SQLite-ban → érvénytelen GPX/KML `lat="NaN"`. Fused vs GNSS `location.time` visszaugrás gyakori; a `<time>` sor rendje tönkremegy. A `TrackStatsCalculator` `dt.coerceAtLeast(0)` mellett ad távot 0 időhöz → az átlagsebesség felrobban; ha az utolsó pont korábbi, mint az első, `elapsedMillis = 0`.
- **Javítás:** A `shouldAccept` tetején: nem véges / tartományon kívüli koordináta → false; `previous != null && current.timestampMillis <= previous.timestampMillis` → false. A `seedFrom` ne inicializáljon rossz magból.
- **Miért oldja meg:** Rossz első pont nem kerül a DB-be. Az idősore monoton, elapsed / moving / GPX / KML konzisztens.
- **Ellenőrzés (2026-09-21):** A koordináta-rész **kész**: `FixAcceptance.shouldAccept` most elutasítja a nem véges és a tartományon kívüli (|lat|>90, |lon|>180) koordinátákat, első fixnél is; a `KalmanTrackFilter.seedFrom` ugyanígy védve van, van rá teszt is. Az időbélyeg-rész **hiányzik**: nincs `previous != null && current.timestampMillis <= previous.timestampMillis` elutasítás sem a `shouldAccept`-ben.
- **Ellenőrzés (2026-09-22):** A koordináta-védelem megvan. Az időbélyeg is: `previous != null && current.timestampMillis <= previous.timestampMillis` esetén a `shouldAccept` false, még a smart-sűrűség táv-ága előtt. Az első pontot ez nem érinti. Teszt: `rejectsNonIncreasingTimestamp`.

---

## Közepes

### Kz1. Nincs hőmérő, a HUD 0°-ot mutat — ✅ Javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/ui/screens/MainTrackerScreen.kt`, **466–474**
- **Probléma:** `temperatureAvailable == false` ágon is `Units.formatTemperature(0f, units)`.
- **Miért probléma:** Hőmérő nélküli telefonon a 0° élő mérésnek tűnik (télen hihető is).
- **Javítás:** „—” amíg `temperatureAvailable && temperatureCelsius != null`. A dőlésszög (`leanAngle`) már így csinálja (**477–478**).
- **Miért oldja meg:** A hiányzó szenzor nem jelenik meg valós 0°-ként.
- **Ellenőrzés (2026-09-21):** A HUD most `temperatureAvailable && temperatureCelsius != null` esetén formáz, egyébként „—”-t mutat 0° helyett.

---

### Kz2. Haversine `a > 1` esetén NaN-ná mérgezi a távot — ✅ Javítva

- **Forrás:** `engine/src/main/kotlin/com/lkovari/mobile/apps/gtl/engine/FixAcceptance.kt`, **87–96**; használja: `TrackStats.kt` **56–61**, **108–113**, `ElevationSeries.kt` **43–48**, `KalmanTrackFilter.kt` (~209–214)
- **Probléma:** `c = 2 * atan2(sqrt(a), sqrt(1 - a))`. Lebegőpont / szemét koordináta (M12) esetén `a` kicsit 1 fölé mehet → `sqrt(1-a)` NaN → a táv NaN. Innentől az odometer NaN marad.
- **Miért probléma:** Egy rossz pár az egész track távját és átlagsebességét NaN-ná teszi. Magyarországi, érvényes GPS-re ritka; M12 nélkül szinte csak védelem.
- **Javítás:** `a = formula.coerceIn(0.0, 1.0)`; ha `!meters.isFinite()`, a `TrackStatsCalculator` ne adja hozzá a szakaszt.
- **Miért oldja meg:** Antipodiális / kerekítési eset ~20 000 km-t ad NaN helyett; egy szemét szakasz nem öli meg a statisztikát.
- **Ellenőrzés (2026-09-21):** `haversineMeters`-ben `a` most `.coerceIn(0.0, 1.0)`-ba van szorítva a `sqrt(1 - a)` előtt. Van rá teszt (`haversineAntipodesIsFinite`).

---

### Kz3. OSM letöltés: a régi térkép törlődik, a rename elhasalhat — ✅ Javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/data/maps/OsmDownloadWorker.kt`, **30–35**
- **Probléma:** `target.delete()` után `temp.renameTo(target)` visszatérési értéke nincs ellenőrizve. Sikertelen rename mellett is `Result.success`. A Tuhu worker staging + copy fallbacket használ (**31–43** `TuhuDownloadWorker.kt`).
- **Miért probléma:** A user elveszíti a már letöltött `.map`-et, a UI sikert jelezhet, a fájl nincs a helyén.
- **Javítás:** Ugyanaz, mint Tuhu: staging fájl, `renameTo`, ha false → `copyTo` + törlés; a régi `target`-et csak a sikeres csere után töröld, vagy cseréld atomian.
- **Miért oldja meg:** Vagy megmarad a régi térkép, vagy a új a `target` néven; nincs „törölve + success”.
- **Ellenőrzés (2026-09-21):** `OsmDownloadWorker` már nem törli előre a `target`-et; sikertelen `renameTo` esetén `copyTo(overwrite = true)` fallback fut, majd `OsmMapFile.isReadable(target)` ellenőrzés dönt a sikerről. A régi térkép csak sikeres csere esetén tűnik el.

---

### Kz4. Android 13+: FINE már megvan → notification permission kimarad — ✅ Javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/ui/screens/MainTrackerScreen.kt`, **150–165**; manifest: `AndroidManifest.xml` **10**
- **Probléma:** Ha `ACCESS_FINE_LOCATION` már granted, azonnal `startLogging()`, a `POST_NOTIFICATIONS` kérés csak a permission-launcherben van (amikor a FINE hiányzik).
- **Miért probléma:** Android 13+ elrejtheti az FGS értesítést. A user nem látja a Leállít akciót, Play policy / FGS láthatóság gyengül. Maga a `startForeground` általában így is lefut.
- **Javítás:** Start előtt, API 33+: ha `POST_NOTIFICATIONS` nincs meg, kérd *akkor is*, ha a FINE már megvan. A naplózás mehet utána; a kérés ne legyen összekötve a FINE hiányával.
- **Miért oldja meg:** Az előtér-értesítés megjelenik, a Stop az árnyéksávból elérhető.
- **Ellenőrzés (2026-09-21):** A Start gomb most külön ellenőrzi a `notifyGranted`-et (API 33+: `POST_NOTIFICATIONS`), és ha hiányzik — a FINE meglététől függetlenül — hozzáadja a kért engedmények listájához.

---

### Kz5. A felvétel szűrője a Start pillanatában lefagy — ❌ Nem javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/service/TrackingForegroundService.kt`, **69**, **104**, **156–160**, **178–204**
- **Probléma:** `settings.first()` egyszer, a `collectLocation` ezt a snapshotot használja (`minAccuracy`, `gnssOnly`, Kalman, sűrűség). A Beállítások menü a DataStore-t írja; a futó service nem olvassa újra. (A nyomás/QNH collector külön `preferences.settings`-t combine-ol, az *élő*.)
- **Miért probléma:** Felvétel közben a min. pontosság / GNSS-only / simítás változtatása nem hat a *mostani* trackre, a user azt hiszi, azonnal érvényes. M7-tel együtt: a DataStore már más, a service még a régit írja.
- **Javítás:** Vagy dokumentáld a UI-n („a szűrő a Startkor rögzül”), vagy a location loop `settings.collectLatest` / combine, és a Kalman/szűrő az aktuális prefhez igazodjon. QNH-hoz hasonlóan.
- **Miért oldja meg:** A kapcsolók és a tárolt pontok ugyanahhoz a szabályhoz tartoznak, vagy a user tudja, hogy csak a következő Starttól.
- **Ellenőrzés (2026-09-21):** `startRecording` továbbra is egyszeri `settings.first()`-öt vesz, ezt adja tovább paraméterként a `collectLocation`-nek, ami a session végéig ezt használja. Nincs `collectLatest`/combine, nincs UI-dokumentáció sem. A hiba fennáll.
- **Ellenőrzés (2026-09-22):** Változatlan. A `startRecording` egyszer olvassa a `settings.first()`-öt, a `collectLocation` ezt a snapshotot használja a session végéig. A QNH/nyomás collector élő, a szűrő nem. Nincs UI-szöveg arról, hogy a szűrő a Startkor rögzül.

---

### Kz6. Minden pontot eldob a szűrő, a UI továbbra is „Naplóz” — ✅ Javítva

- **Forrás:** `TrackingForegroundService.kt`, **179–184**; `MainTrackerScreen.kt`, **462–464**; `TrackingForegroundService.kt`, **337–357**
- **Probléma:** GPS ki, coarse only, 0 műhold, rossz accuracy: a collect megy, a Room üres marad. A státusz `status_logging`, az értesítés statikus cím+szöveg, nincs „0 elfogadott pont / várakozás a GPS-re”.
- **Miért probléma:** A user kilométereket megy üres fájllal (M2, M3, M4 együtt). Az értesítés nem mutat sebességet/utat (roadmap is jelzi); itt a *üres track* a hiba.
- **Javítás:** Számláló a dobott vs elfogadott pontra. 15–30 s 0 ACCEPT után HUD + értesítés: „Nincs elég jó GPS”. Ne állítsd `logging = false`-ra automatikusan, csak jelezd.
- **Miért oldja meg:** A szűrő továbbra is szűr, de a user látja, hogy a fájl még üres.
- **Ellenőrzés (2026-09-21):** Új `acceptedFixCount`/`loggingError` állapot érkezett; amíg `acceptedFixCount == 0`, a HUD és az értesítés „Várakozás a GPS-re” szöveget mutat, és a `logging` nem áll le automatikusan. Ez lefedi a review fő aggályát (üres track csendben). Ami **hiányzik**: nincs folyamatos dobott-vs-elfogadott számláló, és ha az *első* pont után minden **további** pont elesik, a HUD visszaáll sima „Naplóz”-ra — nincs „15–30 mp után nincs jó GPS” típusú figyelmeztetés a session teljes idejére, csak az induló szakaszra.
- **Ellenőrzés (2026-09-22):** `GpsQualityNotice` 20 s után jelez (`elapsedRealtime`, az utolsó ACCEPT óta, vagy ha még nincs, a session indulása óta). A HUD és az értesítés ilyenkor „Nincs elég jó GPS” (`status_poor_gps`), az első pont előtt a 20 s-ig továbbra is „Várakozás a GPS-re”. `gpsOff` és a naplózási hiba előrébb való, a `logging` nem áll le. Az `acceptedFixCount` és a `rejectedFixCount` a `LiveTrackingState`-ben van; a figyelő a `locationJob` gyerek coroutine-ja, `delay(5_000)`, stopkor leáll. Az értesítés csak státuszszöveg-váltáskor íródik újra.

---

## Alacsony

### A1. Room séma nincs exportálva — ❌ Nem javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/data/db/GtlDatabase.kt`, **10–13**, **20–50**
- **Probléma:** `version = 4`, `exportSchema = false`. Van 1→2, 2→3, 3→4 migráció, de nincs sémadiff a CI-ben. Az M9 nullable `altitude` 5-ös migrációt igényelne.
- **Miért probléma:** Elhibázott következő `ALTER` production wipe-ot / crash-t okoz (`IllegalStateException` a Roomtól).
- **Javítás:** `exportSchema = true`, a `schemas/` a gitben, CI `fallbackToDestructiveMigration` nélkül.
- **Miért oldja meg:** A következő oszlopváltozás géppel ellenőrizhető, mielőtt a user trackjeihez érne.
- **Ellenőrzés (2026-09-21):** `GtlDatabase` még mindig `exportSchema = false`. A hiba fennáll.
- **Ellenőrzés (2026-09-22):** A sémaverzió 6 (nullable `altitude` és `speed`, lásd M9), a migrációk megvannak, de `exportSchema` továbbra is `false`. Nincs exportált séma a gitben. A tétel nyitott.

---

### A2. OSM régióletöltésnek nincs méretplafonja — ✅ Javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/data/maps/OsmDownloadWorker.kt`, **42–72**; katalógus: `OsmCatalog.kt` (**HTTPS** `download.mapsforge.org`)
- **Probléma:** Nincs `copied` felső korlát, nincs szabadhely-check. Németország `.map` több száz MB. HTTPS, tehát nem ugyanaz a MITM, mint M1.
- **Miért probléma:** Megtelt tárhely / félbehagyott `.part` a `filesDir/maps` alatt. A user indítja, de nincs „ehhez ~X MB kell”.
- **Javítás:** Előzetes méret / `getUsableSpace`, pl. 2 GB abort, a UI-n becsült méret.
- **Miért oldja meg:** A letöltés nem tölti meg észrevétlenül a belső tárhelyet.
- **Ellenőrzés (2026-09-21):** A Kz3-hoz kapcsolódó rename/copy-fallback bekerült, de méret-/szabadhely-plafon (`getUsableSpace`, felső korlát a `copied`-re) továbbra sincs. A hiba fennáll.
- **Ellenőrzés (2026-09-22):** A Tuhu plafon megmaradt (M1). Az OSM `OsmDownloadBudget` 2 GB-nál megáll, és a `Content-Length` nem lehet nagyobb, mint a `usableSpace` mínusz 64 MB. Ismeretlen hossznál a másolás 8 MiB-onként nézi újra a szabad helyet. A túl nagy vagy helyhiányos letöltés hibázik, a `.part` a catch-ben törlődik. A régiósor letöltés közben a worker `KEY_TOTAL` értékét mutatja megabájtban. Nincs előre beírt, tippelt méret a katalógusban. Van JVM teszt.

---

### A3. A Tuhu worker az OSM work-taget viseli — ❌ Nem javítva

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/tuhu/TuhuMapStore.kt`, **42–44**, **59**; `OsmMapStore.kt`, **54**, **105**
- **Probléma:** A Tuhu enqueue `addTag(OsmMapStore.WORK_TAG)` (`"osm-download"`). Az `observeHasDownloadedMap` ezen a tagen refresh-el.
- **Miért probléma:** Összemosott WorkManager-állapot; későbbi OSM-progress / cancel logika Tuhu munkát is OSM-nek láthat. Ma a `hasDownloadedMap` fájllista alapján van, ezért a tünet enyhe.
- **Javítás:** Saját tag, pl. `"tuhu-download"`. A Tuhu observe marad a saját `workName()`-en.
- **Miért oldja meg:** OSM és Tuhu munkái külön listázhatók és törölhetők.
- **Ellenőrzés (2026-09-21):** `TuhuMapStore` még mindig `OsmMapStore.WORK_TAG`-et (`"osm-download"`) adja hozzá a saját enqueue-hoz. A hiba fennáll.
- **Ellenőrzés (2026-09-22):** Változatlan. A `TuhuMapStore.enqueue` továbbra is `OsmMapStore.WORK_TAG`-et (`"osm-download"`) tesz a workerre, a unique work neve `osm-download-${regionId}`. Saját `tuhu-download` tag nincs.

---

## Ami ebben a körben rendben volt

Ezeket ellenőriztük, **nem** hibák:

- Zip-slip: `TuhuZip` kanonikus path (**38–44**), van teszt.
- Service / FileProvider `exported="false"`; FileProvider csak `gtltracklogs/`.
- `allowBackup="false"`.
- Maps API kulcs `local.properties` / placeholder, nincs bekötve a forrásba.
- `RemoteTrackSync` = `NoOpRemoteTrackSync`, GPS nem megy fel szerverre.
- FGS típus `location` + `FOREGROUND_SERVICE_LOCATION`.
- OSM katalógus HTTPS, `OsmMapFile` Mapsforge header + méret a 28. offseten.
- GPX 1.1 név-escape, KML CDATA; Kalman / DP / egységváltás a szokásos GPS-tartományban helyes.
- A `bearing == 0` mint „ismeretlen” a Kalmanban szándékos (tesztek).

---

## Javasolt sorrend

1. **K1 + K2** — felvétel közbeni adatvesztés. *(✅ mindkettő javítva)*
2. **M4, M5, M6, M9, M12** — a track pontjainak igazsága. *(✅ mind javítva)*
3. **M2, M3, Kz6** — üres fájl, hamis GNSS. *(✅ mind javítva)*
4. **M1** — Tuhu HTTP. *(✅ javítva: HTTPS, redirect, méret, path-allowlist, rendertheme)*
5. **M7, M8, M10, M11** — beállítás / magasság / baro. *(✅ mind a négy javítva)*
6. A közepes és alacsony tételek a következő hullámban. *(Kz1–Kz4, Kz6, A2 javítva; Kz5, A1, A3 nyitott)*

---

## Módszer

Forrásolvasás a `app/` és `engine/` modulon, a GPS-lánc (LocationClient → FGS → FixAcceptance / Kalman → Room → export), a Tuhu/OSM letöltők, a manifest és a hálózati config mentén. A sorszámok a 2026-09-20-i fára vonatkoznak. Az állapot-frissítés (2026-09-21, majd 2026-09-22, a részben javított tételek lezárása után újra) a jelenlegi munkakönyvtár forráskódját ellenőrizte ugyanezen fájlok mentén.
