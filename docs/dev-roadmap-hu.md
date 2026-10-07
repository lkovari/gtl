# GTL fejlesztési roadmap

[English](dev-roadmap-en.md) · [Magyar](dev-roadmap-hu.md)

**Állapot:** termékterv a 2.0.22 (versionCode 40) után, frissítve 2026-10-07. A fában már benne van a térkép HUD, a GPX 1.1, a skyplot, a magasságprofil, a QNH, a GPS-magasság választás, az OSM fájl/kamera védelem, a usage szerinti sebességsávokkal színezett térképvonal (jelmagyarázattal), az Útvonal nagy élő sebessége sparkline-nal, a mentett-útvonal kártyák, a sötét térkép helyzet szerinti témával (1.) és az álló sebesség a HUD-on (2.).  
**Nem kódspec:** ez a sorrend *miértjét* és a hullámokat rögzíti. Implementáció előtt a kiválasztott hullámra külön brief / tesztlista kell.  
**Effort:** egy, a kódbázist ismerő fejlesztő naptári napja (nem emberhónap, nem naptári hét csapatra).

Kapcsolódó: [README-hu.md](../README-hu.md), [CHANGELOGS.md](../CHANGELOGS.md), [DBSTRUCT-en.md](DBSTRUCT-en.md), [GPSDATAFLOW-hu.md](GPSDATAFLOW-hu.md), [all-gps-systems-hu.md](all-gps-systems-hu.md).

---

## Hogyan olvasd

A GTL (GPS Track Logger) 2014-es Eclipse-app Kotlin + Compose újraírása. A 2.0.x kiadások a **naplózási láncot** rakták helyre: Room az egyetlen igazságforrás, Kalman a letárolt pontokon, GNSS-only Fut/túra és kerékpár, KMZ, OSM, pontfelhő. A 2.0.22 utáni fában a felvétel közbeni **térkép HUD**, a **GPX**, a GPS **skyplot**, a **magasságprofil** (QNH-s baro vonallal), a **GPS-magasság** választás, az OSM **fájlellenőrzés**, a **sebesség szerint színezett** térképvonal, a **mentett-útvonal kártyák**, a **sötét térkép** helyzet szerinti témával és az **álló sebesség** a HUD-on is megvan.

A következő hiány **egy élő, menettel forduló térkép, az élő értesítés és a motornapló**: a térkép mindig északra néz, a minden ponton tárolt dőlésből a felület egy fokot mutat, az értesítés statikus. A Play feature graphic sötét cockpitet és izzó tracket ígér; a HUD, a skyplot, a sebesség-szín és a sötét csempe a kódban megvan, a listing képe még a régi kivágás.

A sorrend **érték szerint** van (megtartás × Play-konverzió × a már tárolt adat kiaknázása), nem könnyű győzelem szerint. Az effort másodlagos, de ahol két tétel közel azonos értékű, az olcsóbb előrébb kerül a hullámban. A lista eleje az, amit **minden felvételen** látsz; a vége az, amit egy túra után egyszer nézel meg.

---

## Termékpozíció

A GTL **helyben futó, precíziós GPS útvonalnapló**. Semmi nem kerül fel a mi szerverünkre. A térképvonal **maga** a SQLite tracklog, nem map-matching, nem második vázlat.

Célfelhasználó (a Beállítások usage sorrendje és az alap **motor** szerint):

- motoros / autós, aki utána Google Earth-ben vagy saját archívumban nézi az utat
- futó, túrázó / kerékpáros, aki sportóra-szerű GNSS-tracket akar, utcára pattintás nélkül
- hajós / repülős, ICAO egységekkel, ritkább, de a usage modell már kezeli

A verseny **nem** a Strava, Komoot vagy Google Maps Navigation. Azok közösség, edzésterv, turn-by-turn. A GTL moatja:

1. adat a telefonon marad
2. a vonal az, amit a chip / Kalman tényleg rögzített
3. GNSS HUD (konstelláció, SNR, pontfelhő / CEP95, skyplot)
4. KMZ balloonok Earth-höz; GPX a többi eszközhöz

Minden új feature-nek ezt kell erősítenie, vagy **kibontania** (sötét térkép: éjjel is látod; dőlésszalag: a szerpentin motornapló lesz), nem helyettesítenie közösségi feeddel.

---

## Hol tartunk ma

### Ami erős

- Előtér-szolgáltatás, látható értesítés, nincs `ACCESS_BACKGROUND_LOCATION`
- Használati előbeállítások (repülő, hajó, autó, motor, kerékpár, Fut/túra) egy DataStore-szerkesztésben
- Szűrőlánc: pontosság / műhold → opcionális Kalman → sűrűség → Room → Térkép / Útvonal / KMZ / GPX
- Térkép HUD (nagy sebesség, pontosság, GNSS used/in view; naplózáskor út, idő, pulzáló REC); keep-screen-on beállítás
- Sebesség szerint színezett vonal a usage sávjaival (`SpeedBands`, `SpeedTrack`), sarok-jelmagyarázat (nyitva vagy összecsukott pöttyök)
- Útvonal fül: nagy élő sebesség, alatta sparkline; mentett sessionnél átlag
- GPS fül: L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC, SNR, polar skyplot; magasság a `GpsAltitude.pick`-ből; baro, ha van nyomásszenzor
- KMZ `LineString` a letárolt GPS-magasságon (`absolute`; `clampToGround` csak ha a tracken nincs magasság) plusz rejtett `gx:Track` magasság 0-val az idősorhoz; Start / Pause / Stop balloon
- GPX 1.1 megosztás (egy fájl, több `trk`; START/PAUSE/STOP `wpt`)
- Magasságprofil (GPS × táv; szaggatott baro, QNH 900–1100 hPa; 1500 m GPS-őr)
- OSM Mapsforge és Turistautak; `OsmMapFile` ellenőrzés; Google Maps, ha van `MAPS_API_KEY`
- Zöld **S** / piros **E** a kirajzolt tracken; usage-sziluett a helyeden
- Compose paletta: világos sage/papír, sötét **Cockpit** (`Theme.kt`); Automatikus módban a sötét a helyzet szerinti polgári szürkületet követi
- Sötét térkép (1.): Beállítások **Téma** — Automatikus / Világos / Sötét. Google night JSON a normál és terep rétegen; OSM és Turistautak Mapsforge átszínezés. Sötét sebességsáv, világosabb pontossági és CEP vonal. Splash fekete
- Álló sebesség (2.): `DisplaySpeed` a térkép HUD-on és az Útvonal pillanatnyi sebességén. Sebességpontosság vagy elmozdulás, két mintás hiszterézis. A letárolt track változatlan
- Dőlésszög (5.): javítva 2026-10-04. Az új pont és az Útvonal száma `atan(v · ω / g)`. A szalag nincs meg

### Ami gyenge a listinghez és a használathoz

- **A térkép nem fordul:** a Google sziluett `rotation = 0f`, az OSM rétegek a `draw(..., _rotation)` paramétert eldobják. Menet közben a kanyar a képernyőn oldalra fut, nem előre. A fix `bearing`-je minden ponton megvan.
- **Dőlés:** a kanyarban hazug gravitációs forrás **javítva**. A letárolt `leanAngle` és az Útvonal egy foka `atan(v · ω / g)`. A szalag, a max bal/jobb kártya és a színezés még nincs. Lásd 5.
- **Mentett útvonalak:** kártya van. Hátravan az opcionális fájlnév; az átlag és a max minden megnyitáskor a pontokból számol.
- **Értesítés:** statikus cím + szöveg + Leállít (`TrackingForegroundService.buildNotification`). Nincs élő sebesség / út. A 2. kapuja a HUD-on megvan, az értesítés még nem hívja.
- **Play feature graphic** (`docs/play-console/feature-graphic.png`): sötét műszerfal, izzó track, skyplot. A sötét csempe a kódban megvan; a listing képe még a régi kivágás.

### Szándékosan nincs (és maradjon így)

IMEI, élő lat/lng feltöltés, follow-me web, távoli feloldás, Google Directions, app által kapcsolt GPS, boot auto-start. A `RemoteTrackSync` no-op csonk; **ne töltsd meg** backenddel, amíg a termék helyi logger.

---

## Ami ne kerüljön a backlog elejére

| Ötlet | Miért ne most |
| ----- | ------------- |
| Élő megosztás / saját szerver / `RemoteTrackSync` feltöltés | Szemben a privacy-politikával és a 2.0 ígérettel |
| Utcára pattintás (OSM/Google map-matching) | Szemben a Fut/túra GNSS-trackkel; a Kalman szándékosan nem ezt csinálja |
| Strava-szerű közösség, kudos, szegmensek | Más termék |
| Wear OS | Hetek, külön store, tesztmátrix; a telefonos HUD megvan |
| GPX import | Az app logger, nem archívum-kezelő |
| FIT / TCX | Ha valaki Garmin Connect-re kér |
| Turn-by-turn | Play / API / figyelemelterelés; a 2014-es Directions szándékosan kimaradt |
| Indítósáv-widget | A Quick Settings tile olcsóbb; widget később |
| **Naiv headingUp a sziluetten**, alapból be, kikapcsolva minden frissítés `heading = 0` | Pirosnál pörög (álló Doppler-irány zaj), a befoglaló téglalap levágja a tracket, a kétujjas forgatást a következő pont visszapörgeti, és az OSM ettől még nem fordul. Helyette a kapuzott menetirányú követés (3.) |
| **Hőszalag** (track színezése hőmérséklet szerint) | Az `ambientTemperature` sok telefonon nincs; a funkció a felhasználók többségénél üres lenne |

---

## Eye-catcher elv

Ne „fésüld át Material 3-mal”. A paletta (teal, carmine, magenta, cockpit) már megkülönböztet. A HUD, a skyplot, a sebesség-színezett vonal és a sötét csempe megvan. A gond a **mindig északra néző** térkép menet közben, és hogy a tárolt dőlés **nem látszik**.

Ami a feature graphicot igazzá teszi:

1. Élő **térkép HUD** — kész
2. **Skyplot** a GPS fülön — kész
3. **Sebesség-színezett** vonal — kész
4. Ugyanez **sötét csempén** — kész
5. Menettel forduló kamera és üstökösfarok — következő látvány

Vegyél HUD-os Térképet naplózás közben, és cseréld a feature graphicot **valódi UI-kivágásra**. A sötét csempe a fában megvan.

Motor az alap usage: az eye-catchernek **nappal és éjjel, kesztyűben, villantásra** is működnie kell (nagy szám, kevés koppintás, sötét térkép, előre néző út).

---

## Prioritás (érték szerint)

Az effort egy fejlesztő napja. A „fájlok” a természetes belépők, nem kimerítő lista. Elöl, ami minden felvételen látszik; hátul, amit ritkán nézel, vagy kevés telefonon működik.

### 1. Sötét térkép + in-app téma

**Állapot:** kész (2026-10-04, a fában)  
**Érték:** magas — brand, éjszakai motor, listing-egyezés, a sebességszín és az üstökösfarok sötéten ad látványt  
**Effort:** 2–3 nap  
**Hullám:** 1

**Engedély:** nincs új. Sem a manifest `uses-permission` listája, sem a Play Console engedélyűrlapja nem bővül.

**Miért.** Éjjel a fehér térkép vakít, a sebességszín kiég. Kevésbé egyedi, mint a többi tétel, a használatnak mégis erős, és a 3., 5., 6. tétel látványa erre ül.

**Ma.** `ThemeMode` a DataStore-ban (`AUTOMATIC` / `LIGHT` / `DARK`), hiányzó kulcs = Automatikus. A sötét a helyzet szerinti polgári szürkület (−6°), nem a rendszer téma és nem egy fix óra. Google `MapStyleOptions` night JSON a normál és terep rétegen. Mapsforge éjjel átszínezve (OSM és Turistautak). Splash fekete.

**Megvan.**

- Beállítás: **Automatikus / Világos / Sötét** (DataStore). Az automatikus a helyzet szerinti polgári szürkület, nem egy fix óra
- Két stílus: Google Maps `MapStyleOptions` night JSON, Mapsforge sötét render theme (OSM és Turistautak), ugyanarra a kapcsolóra
- `Theme.Gtl` status/nav bar a témához; splash maradhat fekete
- HUD, polyline, pontfelhő kontrasztja sötét csempén (a lila körhöz világosabb stroke); a sebességsávok sötét változata, ha a mélyzöld / fekete sáv eltűnik

**Ne.** Harmadik „high contrast” paletta.

**Teszt.** Automatikus / Világos / Sötét; Google, OSM és Turistautak; HUD, sebességsáv és pontfelhő olvasható sötét csempén.

---

### 2. Álló sebesség a HUD-on

**Állapot:** kész (2026-10-04, a fában)  
**Érték:** magas — a műszer állva is mozogni mutat; ugyanez a kapu kell a 3. irányához  
**Effort:** ~1 nap  
**Hullám:** 1

**Engedély:** nincs új. A `Location.getSpeedAccuracyMetersPerSecond` a meglévő `ACCESS_FINE_LOCATION` fixjén van. `ACTIVITY_RECOGNITION` ne kerüljön be.

**Miért.** Bent, mozdulatlan pin mellett a térkép HUD ~5 km/h-t írt (nyers Doppler, `Units.hudSpeedNumber`). A Route lap idle-ben már 0 (`RouteTabSpeeds`). Fix km/h-küszöb a lassú gyaloglást vágná, egy nagyobb benti tüskét átengedne.

**Ma.** `DisplaySpeed` az előnézet és a naplózás `acceptFix` útján. A térkép HUD (`displaySpeedMps`) és az Útvonal pillanatnyi sebessége ezt mutatja. Az értesítés (4.) még a statikus szöveget írja.

**Megvan.** A térkép HUD és a Route pillanatnyi sebesség ezt hívja. Az értesítés (4.) még nem.

- Nincs sebességmező: „—”.
- Van sebességpontosság (`getSpeedAccuracyMetersPerSecond`; minSdk 24-en `LocationCompat`): ha a sebesség ≤ a pontossága, a kijelző **0**.
- Nincs sebességpontosság: ha az előző fix óta az elmozdulás ≤ a vízszintes pontosság, **0**.
- Hiszterézis: két szignifikáns minta a 0 elhagyásához, kettő a visszatéréshez.
- A letárolt track marad Kalman + `pauseSpeedMps()`; ez kijelzőszabály.

**Ne.** Fix km/h-küszöb. Sebesség a koordináta-különbségből. Kalman az idle előnézeten.

**Teszt.** Álló benti fix: HUD 0. Szabad égi gyaloglás: a szám megjön. Nincs 0/5 villogás.

---

### 3. Kapuzott menetirányú követés (+ sziluett a pálya felé)

**Érték:** magas — minden felvételen látszik, az út előre fut a képernyőn  
**Effort:** 3–4 nap (Google ~0,5 nap; az OSM a munka)  
**Hullám:** 2

**Engedély:** nincs új. A pálya a meglévő GPS-fix `bearing`-je. Iránytű-engedély Androidon nincs; `BODY_SENSORS` ne kerüljön be.

**Miért.** Élő követéskor a kamera a pályával fordul, így a kanyar előre néz, nem oldalra. A bearing már a fixen van, új adat nem kell.

**Ma.** Google: `MarkerComposable(rotation = 0f)`, a kamera bearing 0. OSM: a `UsagePositionLayer` és a trackvég-rétegek `draw(..., _rotation: Rotation)` paraméterét eldobják. Az északjelző (`NorthIndicator`) már a kamera bearingjével forog.

**Mit építs.**

- Engine: tiszta `heading` kapu. **1 m/s alatt marad az utolsó jó irány** (pirosnál nem pörög); fölötte a fix `bearing`-je, ha `hasBearing()`. Hiszterézis és kis simítás (pl. 2–3 fix), hogy egy zajos fix ne rántsa a kamerát. A 2. sebességkapu ugyanaz a döntés.
- Kamera mód: **élő követés** = menetirányú; **teljes útvonal a képernyőn** és **mentett track** = északra illeszt (`TrackCameraBounds` változatlan). Kétujjas forgatás vagy húzás kilép a menetirányúból a következő Saját hely koppintásig, nem pörgeti vissza a következő fix.
- Google: `CameraPosition.bearing`.
- OSM: Mapsforge kamera rotáció; a sziluett, az S/E, a pontossági kör és a tap-menü horgonya megkapja a `Rotation`-t.
- **Sziluett a pálya felé, ha a térkép északra néz:** a sziluett a kapuzott irányba fordul. Menetirányú kameránál az ikon a képernyő teteje felé marad, különben a kanyar kétszer fordul.
- Beállítás: Menetirány / Észak fent kapcsoló, motoron és autón alapból Menetirány.

**Ne.** Iránytű-heading menet közben (a telefon a tankon nem a menetirány). A naiv headingUp (lásd a „ne” táblát).

**Teszt.** Pirosnál álló kamera nem forog; kanyarban a kamera követ; mentett track és teljes útvonal északra áll; kétujjas forgatás nem pattan vissza; OSM-en a sziluett és az S/E a forgatással jó helyen van.

---

### 4. Élő előtér-értesítés

**Érték:** közepes–magas — második HUD, zsebben  
**Effort:** 1–2 nap  
**Hullám:** 1

**Engedély:** a sima frissülő értesítéshez nincs új. `POST_NOTIFICATIONS` és a `foregroundServiceType="location"` már a manifestben van; az Indít a runtime kérést megteszi. Kiemelt, folyamatban lévő stílushoz (Live Update; a promotion API 36.1) és csak ahhoz kell új, install-time `uses-permission`: `android.permission.POST_PROMOTED_NOTIFICATIONS`. Ehhez a compileSdk 36.1. Nem dangerous, nincs külön rendszerdialógus. A Play a manifestből látja. Külön érzékeny-engedély nyilatkozat nem kell, a Data safety nem változik: ugyanaz a helyi sebesség és út, feltöltés nélkül. Új foreground-service típus nem kell.

**Miért.** Motoros, futó, túrázó nem nézi a képernyőt. Ugyanazok a számok, mint a térkép HUD-on, lock screenen / shade-en.

**Ma.** `NOTIFICATION_ID = 17`, `IMPORTANCE_LOW`, Stop action, statikus stringek.

**Mit építs.** Periodikus `notify()`: sebesség, út, eltelt idő. Meglévő Stop. Nincs hang/rezgés. Android 16-on (targetSdk 36) a kiemelt, folyamatban lévő értesítés-stílus megfontolható; régebbin sima frissülő értesítés.

**Függőség.** A 2. kapuzott sebessége.

---

### 5. Dőlésszalag (motornapló)

**Állapot:** a kanyarbeli dőlésszög javítva (2026-10-04, a fában). A letárolt `leanAngle` és az Útvonal száma `atan(v · ω / g)`, nem a gravitáció. A szalag, a max bal/jobb kártya és a színezés nincs meg.  
**Érték:** magas a motoros alap usage-nek — a szerpentin ettől motornapló  
**Effort:** 3–4 nap  
**Hullám:** 2

**Engedély:** nincs új. A giroszkóp, a gravitáció és a gyorsulásmérő nem engedélyköteles. `BODY_SENSORS`, `ACTIVITY_RECOGNITION` és `HIGH_SAMPLING_RATE_SENSORS` ne kerüljön be. Opcionális manifest-jelölés, ha az élő ω giroszkópból jön: `uses-feature` `android.hardware.sensor.gyroscope` `required="false"`. Ez hardver, nem Play-engedély, és a bearingből számolt mentett szalag nélküle is megvan.

**Miért.** A dőlés minden ponton a telefonon van, a felület egy fokot mutat. Bal/jobb dőlés szerinti szalag a vonal mentén, élő dőlésmérő, max bal / max jobb a kártyán.

**Kockázat — javítva.** A `TYPE_GRAVITY` dőlés kanyarban ~0°-ot rögzített. Az új pont `leanAngle` értéke és az Útvonal száma most `atan(v · ω / g)`. A sor ω-ja a letárolt bearing változása / idő. Élőben ω a rotation-vector yaw rate, ha van giroszkóp, különben a bearing. 3 m/s alatt, 0,2 s-nél sűrűbb vagy 5 s-nél ritkább bearingnél nincs érték. A már mentett sorok a régi gravitációs számot őrzik; a szalag majd a bearingből számol, nem a régi oszlopból.

**Mit építs.** A kinematikai dőlés kész. Hátra a szalag, nem a szám.

- Alacsony sebességen (pl. < 3 m/s) és ritka pontoknál nincs szalag (a bearing-zaj dominál). A szám ugyanezt a kaput használja.
- Rajzolás mindkét motoron: szalag a vonal mellett vagy választható színezés (sebesség / dőlés), bal és jobb külön árnyalattal; jelmagyarázat fokban.
- Mentett kártya: max bal / max jobb.
- **Fut/túrán rejtve**, kerékpáron opcionális.

**Ne elsőre.** A `TYPE_GRAVITY` dőlés szalagra rajzolása. Kalibrációs varázsló a tartóhoz.

**Teszt.** Engine **kész**: szintetikus körív adott sebességgel → ismert dőlés. Egyenes: ~0°. Álló: nincs érték. Szintetikus szerpentin: bal/jobb előjel helyes. Valódi szerpentin track a szalaggal együtt marad.

---

### 6. Üstökösfarok

**Érték:** közepes — olcsó, menet közben mozgást ad  
**Effort:** ~1 nap  
**Hullám:** 2

**Engedély:** nincs új. Sem a manifest `uses-permission` listája, sem a Play Console engedélyűrlapja nem bővül.

**Miért.** Naplózáskor az utolsó perc vastagabb és teljes színű, a régebbi vonal halkabb. A meglévő színezett vonalra ül, új adat nem kell.

**Mit építs.** A `SpeedTrack` szegmensek kapnak egy „kor” mezőt; szélesség és alfa a kor szerint (2–3 lépcső elég, nem méterenkénti gradiens). Csak élő sessionnél; mentett tracken egyenletes vonal. Google-on ügyelj a polyline-darabszámra.

**Függőség.** Sötét csempe (1.) mellett mutat igazán.

---

### 7. Repülés Google Earth-ben (KMZ `gx:Tour`)

**Érték:** közepes — nagy látvány, olcsó, pont a KMZ-közönségnek  
**Effort:** 1–2 nap  
**Hullám:** 3

**Engedély:** nincs új. A megosztás a meglévő `FileProvider`en megy. Tároló-engedély ne kerüljön be.

**Miért.** A KMZ már a letárolt GPS-magasságon adja át a vonalat; egy `gx:Tour` a vonal mentén végigrepíti a kamerát a Google Earth-ben. A „helyi flyover” (12.) látványának nagy része, a költsége töredékéért.

**Mit építs.** `KmzExporter`: `gx:Tour` / `gx:Playlist` `gx:FlyTo` lépésekkel a ritkított pályán (heading a pálya irányából, tilt fix, range a sebességhez), opcionálisan a megosztási menüben. Tiszta engine-teszt a KML-re.

---

### 8. Mentett útvonalak: fájlnév és tárolt sebesség

**Érték:** közepes — a kártya megvan; a név és a sessionben tárolt sebesség hiányzik  
**Effort:** 1–2 nap  
**Hullám:** 3

**Engedély:** nincs új. Room-migráció, a Play Data safety típusai nem bővülnek.

**Hátravan.**

- Opcionális `displayName` a sessionön. Üres = dátum. A KMZ/GPX fájlnév és a `<name>` / KMZ folder ezt használja; a tiltott karaktereket cseréld.
- `track_sessions` bővítés (Room migráció 6→7): `avgSpeed`, `maxSpeed` (m/s), és ha az 5. kész, `maxLeanLeft` / `maxLeanRight`, Leállításkor számolva. A lista ezeket olvassa.

---

### 9. Track-kép / képeslap megosztás

**Érték:** közepes — social eye-catcher szerver nélkül  
**Effort:** 3–5 nap  
**Hullám:** 3

**Engedély:** nincs új. A PNG a cache-be megy, a meglévő `FileProvider` osztja a share sheeten. `READ_MEDIA_IMAGES`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` és `WRITE_EXTERNAL_STORAGE` ne kerüljön be: a Play fotó- és videószabálya ezekre külön nyilatkozatot kér.

**Miért.** Sötét alapon izzó, sebesség-színezett vonal, táv, idő, magasságcsík, GTL pecsét, PNG a share sheetre. Nincs feltöltés. Jó listing-forrás.

**Mit építs.** Csempe nélkül: saját polyline sötét Canvasra (a `TrackRouteThumbnail` rajzolója nagyrészt újrahasznosítható) → Bitmap → share. Ne Static Maps API-val kezdd.

**Függőség.** 8. (név, statok); 5. ha a dőlés is rákerül.

---

### 10. Kanyargaléria

**Érték:** közepes motoron, önmagában egy lista  
**Effort:** 2–3 nap  
**Hullám:** 3

**Engedély:** nincs új. Sem a manifest `uses-permission` listája, sem a Play Console engedélyűrlapja nem bővül.

**Miért.** „Bal 38°, 72 km/h”: a mentett track legerősebb kanyarjai a bearing ugrásából és a dőlésből, koppintásra a térkép oda ugrik. A szalag (5.) után érdemes, mert ugyanazt az engine-számítást használja.

---

### 11. Kézi szünet és kör / lap

**Érték:** közepes  
**Effort:** 2–3 nap  
**Hullám:** 4

**Engedély:** nincs új. Sem a manifest `uses-permission` listája, sem a Play Console engedélyűrlapja nem bővül.

**Miért.** A `PAUSE` ma sebességküszöb. Pirosnál, benzinkútnál nem lehet szüneteltetni Leállítás (új session) nélkül.

**Mit építs.** Szünet / Folytat gomb naplózáskor; szünetben nincs MOVE; folytatáskor nincs új `track_sessions` sor. KMZ: meglévő pause ikon; GPX: `trkseg` a szünetnél. Kör utána.

---

### 12. Helyi flyover / visszajátszás

**Érték:** közepes — erős film, de a túra után egyszer nézed meg  
**Effort:** 5–8 nap  
**Hullám:** 4

**Engedély:** nincs új. Sem a manifest `uses-permission` listája, sem a Play Console engedélyűrlapja nem bővül.

**Miért.** A mentett track kirajzolódik, a dőlő sziluett végigmegy rajta, a HUD az adott pont számait mutatja. Jó Play-videó.

**Drága, mert:** csúszka, tempó (1× / 10× / 60×), dőlő ikon, kamera mindkét motoron. A 7. (`gx:Tour`) a látvány nagy részét olcsóbban adja; ez csak akkor, ha a felhasználók appon belül kérik.

**Függőség.** 3. (forgó kamera és sziluett), 5. (dőlő ikon).

---

### 13. Fekvő / tank HUD mód

**Érték:** közepes a default motorhoz, effort magas  
**Effort:** 5–8 nap  
**Hullám:** 4

**Engedély:** nincs új. Ha a fekvő mód éles, az activity `android:screenOrientation="portrait"` jön le a manifestből. Ez orientáció, nem permission, és a Play engedélyűrlapját nem érinti.

**Miért.** Az app `portrait`. Tankra rakva a nagy számjegy fekvőben olvasható. A portrait HUD a haszon ~80%-át adja.

**Figyelem.** Mapsforge `MapView` + Compose rotáció; a 3. menetirányú kamerával együtt tesztelendő.

---

### 14. Quick Settings tile (Indít / Leállít)

**Érték:** alacsony–közepes  
**Effort:** ~1 nap  
**Hullám:** bármikor

**Engedély:** nincs új `uses-permission`, és a Play érzékeny-engedély űrlapja nem bővül. A csempe service-én `android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"`, `android:exported="true"`, és az `android.service.quicksettings.action.QS_TILE` intent-filter. Ezt a `uses-permission` listába ne másold: signature engedély, az app nem kapja meg, a Play pedig fölöslegesen kiírná. Az Indít továbbra is a meglévő helyengedélyt és a `FOREGROUND_SERVICE_LOCATION` típust használja. Új foreground-service típus nem kell.

**Miért.** Shade-ből indítás kesztyűben. `TileService`. A runtime engedély ugyanaz, mint az Indít gombon. Nem listing-téma.

---

### 15. Magasság-szobor

**Érték:** alacsony — hágón és repülőúton szép, városi motoron lapos  
**Effort:** 3–5 nap  
**Hullám:** később

**Engedély:** nincs új. Sem a manifest `uses-permission` listája, sem a Play Console engedélyűrlapja nem bővül. A domborzat-HGT, ha később mégis letöltés, a meglévő `INTERNET` alá esik.

**Miért ilyen hátul.** A KMZ ezt a Google Earthnek már átadja (`absolute` magasság), a 7. pedig repülést is ad hozzá. Appon belüli 2,5D rajz csak hegyi / repülős usage-nél ér valamit.

---

### 16. Két út egymáson

**Érték:** alacsony — archívum  
**Effort:** 2–3 nap  
**Hullám:** később

**Engedély:** nincs új. Sem a manifest `uses-permission` listája, sem a Play Console engedélyűrlapja nem bővül.

**Miért ilyen hátul.** Két kijelölt mentett track egy térképen, eltérő színnel. A szem a különbségen akad meg, de a menet közbeni kép nem változik, és ritkán használt.

---

## Kiadási hullámok

A verziószámok **javaslatok**.

### Hullám 1 — „éjjel is látod” (kb. 4–6 nap)

| # | Tétel | Effort |
| - | ----- | ------ |
| 1 | Sötét térkép + Automatikus/Világos/Sötét — **kész** | 2–3 nap |
| 2 | Álló sebesség a HUD-on — **kész** | ~1 nap |
| 4 | Értesítés élő számokkal | 1–2 nap |
| — | Play screenshot + feature graphic a **valódi** HUD-os, sötét térképről | 0,5 nap |

**Ma:** az 1. és a 2. a fában megvan. Hátra az élő értesítés és a listing kép.  
**Kész, ha:** sötét módban a csempe sötét (Google, OSM, Turistautak); álló benti fixen a HUD 0 km/h; az értesítésben ugyanaz a szám, mint a HUD-on; a listing új képe a sötét HUD-os Térkép.

### Hullám 2 — „menet közben él a térkép” (kb. 7–9 nap)

| # | Tétel | Effort |
| - | ----- | ------ |
| 3 | Kapuzott menetirányú követés + sziluett a pálya felé | 3–4 nap |
| 5 | Dőlésszalag — a kinematikai dőlés **kész**, a szalag nincs | 3–4 nap |
| 6 | Üstökösfarok | ~1 nap |

**Kész, ha:** pirosnál a kamera nem pörög, kanyarban előre néz; mentett track északra áll; egy szerpentin mentett tracken bal/jobb dőlés látszik, régi trackeken is; naplózáskor az utolsó perc kiemelt.

### Hullám 3 — „az archívum mesél” (kb. 7–12 nap)

| # | Tétel | Effort |
| - | ----- | ------ |
| 7 | KMZ `gx:Tour` | 1–2 nap |
| 8 | Fájlnév + átlag/max (+ max dőlés) a session sorában | 1–2 nap |
| 9 | Képeslap PNG | 3–5 nap |
| 10 | Kanyargaléria | 2–3 nap |

### Hullám 4 — mélyítés (később, darabolva)

| # | Tétel | Effort |
| - | ----- | ------ |
| 11 | Kézi szünet / kör | 2–3 nap |
| 12 | Helyi flyover | 5–8 nap |
| 13 | Fekvő HUD | 5–8 nap |
| 14 | Quick Settings tile | ~1 nap |
| 15 | Magasság-szobor | 3–5 nap |
| 16 | Két út egymáson | 2–3 nap |

A 12. és 13. a drágák: csak akkor, ha a hullám 1–3 után még ez a panasz.

---

## Engedélyek

A 16 pont közül egyetlen új Play-látható engedély van, és az is feltételes.

| Pont | Új `uses-permission` | Play Console | Manifest, ami nem engedély |
| ---- | -------------------- | ------------ | -------------------------- |
| 1–3, 6–8, 10–12, 15–16 | nincs | az űrlap nem bővül | — |
| 4 sima `notify()` | nincs | nem bővül | a meglévő `POST_NOTIFICATIONS` és `location` FGS elég |
| 4 kiemelt Live Update | `POST_PROMOTED_NOTIFICATIONS` (install-time; promotion API 36.1) | a manifestből látszik; érzékeny-engedély nyilatkozat nincs | compileSdk 36.1 |
| 5 | nincs | nem bővül | opcionális `uses-feature` giroszkóp, `required="false"` |
| 9 | nincs; média-engedélyt ne adj hozzá | fotó/videó nyilatkozat csak akkor, ha mégis hozzáadod | meglévő `FileProvider` |
| 13 | nincs | nem bővül | `screenOrientation="portrait"` levétele |
| 14 | nincs; `BIND_QUICK_SETTINGS_TILE` ne legyen `uses-permission` | nem bővül | a service `android:permission` attribútuma és a `QS_TILE` filter |

A Data safety típusai egyik pontnál sem bővülnek: nincs új szerver, nincs háttérhely (`ACCESS_BACKGROUND_LOCATION`), nincs SMS, hívásnapló vagy összes-fájl hozzáférés. Az elvetett naiv headingUp és a hőszalag sem kér engedélyt.

## Összesítő tábla

| Rang | Feature | Érték | Effort | Hullám |
| ---- | ------- | ----- | ------ | ------ |
| 1 | Sötét térkép + automatikus szürkület — **kész** | magas | 2–3 nap | 1 |
| 2 | Álló sebesség a HUD-on — **kész** | magas | ~1 nap | 1 |
| 3 | Kapuzott menetirányú követés + sziluett a pálya felé | magas | 3–4 nap | 2 |
| 4 | Élő értesítés | közepes–magas | 1–2 nap | 1 |
| 5 | Dőlésszalag — kinematikai dőlés **kész**, szalag nincs | magas (motor) | 3–4 nap | 2 |
| 6 | Üstökösfarok | közepes | ~1 nap | 2 |
| 7 | KMZ `gx:Tour` Google Earth-höz | közepes | 1–2 nap | 3 |
| 8 | Mentett track fájlnév + tárolt statok | közepes | 1–2 nap | 3 |
| 9 | Képeslap megosztás | közepes | 3–5 nap | 3 |
| 10 | Kanyargaléria | közepes (motor) | 2–3 nap | 3 |
| 11 | Kézi szünet / kör | közepes | 2–3 nap | 4 |
| 12 | Helyi flyover / visszajátszás | közepes | 5–8 nap | 4 |
| 13 | Fekvő tank HUD | közepes | 5–8 nap | 4 |
| 14 | Quick Settings tile | alacsony–közepes | ~1 nap | bármikor |
| 15 | Magasság-szobor | alacsony | 3–5 nap | később |
| 16 | Két út egymáson | alacsony | 2–3 nap | később |
| — | Naiv headingUp | elvetve | — | — |
| — | Hőszalag | elvetve | — | — |

A sötét térkép és az álló sebesség kész. Ha a hátralévőből csak **kettőt** lehet: **kapuzott menetirányú követés + élő értesítés**. Az első minden felvételen látszik, a második zárolt képernyőn.  
Ha a motoros közönségre lősz: a következő a **dőlésszalag**, kinematikai dőléssel.

---

## Dokumentáció és Play, minden hullám végén

Nem opcionális toldalék:

- [CHANGELOGS.md](../CHANGELOGS.md) EN + Magyar
- [docs/play-console/whatsnew.txt](play-console/whatsnew.txt) (500 karakter / nyelv)
- Súgó EN/HU az új vezérlőkre
- README funkciólista, ha a felhasználó látja
- Screenshot 24 bites PNG, nincs alfa. HUD-os Térkép (naplózás, sötét csempe) és feature graphic az 1. hullám után; dőlésszalagos szerpentin a 2. után.

A [GPSDATAFLOW](GPSDATAFLOW-hu.md) csak akkor változik, ha a lánc írása változik (az 5. kinematikai dőlése számítás, nem írás). Session név, statok, max dőlés: [DBSTRUCT](DBSTRUCT-en.md) migráció 6→7.

---

## Döntések

### Hullám 1 — lezárva

- Google night JSON: saját stílus, `res/raw/map_style_night.json`, csak a normál és a terep rétegen. Műhold és hibrid fotó marad.
- Mapsforge: a meglévő `gtl.xml` és `tuhu.xml` átszínezése (`NightRenderTheme`), nem második kézi téma. A Turistautak ugyanezt kapja, a külső `theme.xml` is.

### Nyitott (implementáció előtt)

Hullám 2:

- Menetirányú kamera alapértéke usage-enként (motor/autó be, Fut/túra ki?)
- Dőlés: szalag a vonal mellett, vagy sebesség / dőlés színezés-választó?
- Élő dőlés: **lezárva** — giroszkóp yaw rate, ha van; különben a GPS bearing változása. A letárolt sor mindig a bearing.

Ezeket a hullám briefjében rögzítsd; a roadmap szándékosan nem fagyasztja a pixel-layoutot.
