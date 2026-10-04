# GTL fejlesztési roadmap

[English](dev-roadmap-en.md) · [Magyar](dev-roadmap-hu.md)

**Állapot:** termékterv a 2.0.19 (versionCode 37) után, frissítve 2026-10-04. A fában már benne van a térkép HUD, a GPX 1.1, a skyplot, a magasságprofil, a QNH, a GPS-magasság választás, az OSM fájl/kamera védelem, a usage szerinti sebességsávokkal színezett térképvonal (jelmagyarázattal), az Útvonal nagy élő sebessége sparkline-nal és a mentett-útvonal kártyák.  
**Nem kódspec:** ez a sorrend *miértjét* és a hullámokat rögzíti. Implementáció előtt a kiválasztott hullámra külön brief / tesztlista kell.  
**Effort:** egy, a kódbázist ismerő fejlesztő naptári napja (nem emberhónap, nem naptári hét csapatra).

Kapcsolódó: [README-hu.md](../README-hu.md), [CHANGELOGS.md](../CHANGELOGS.md), [DBSTRUCT-en.md](DBSTRUCT-en.md), [GPSDATAFLOW-hu.md](GPSDATAFLOW-hu.md), [all-gps-systems-hu.md](all-gps-systems-hu.md).

---

## Hogyan olvasd

A GTL (GPS Track Logger) 2014-es Eclipse-app Kotlin + Compose újraírása. A 2.0.x kiadások a **naplózási láncot** rakták helyre: Room az egyetlen igazságforrás, Kalman a letárolt pontokon, GNSS-only Fut/túra és kerékpár, KMZ, OSM, pontfelhő. A 2.0.19 utáni fában a felvétel közbeni **térkép HUD**, a **GPX**, a GPS **skyplot**, a **magasságprofil** (QNH-s baro vonallal), a **GPS-magasság** választás, az OSM **fájlellenőrzés**, a **sebesség szerint színezett** térképvonal és a **mentett-útvonal kártyák** is megvan.

A következő hiány **éjszakai használat, egy élő, menettel forduló térkép és a motornapló**: a csempe nappali marad, a térkép mindig északra néz, a minden ponton tárolt dőlésből a felület egy fokot mutat, az értesítés statikus. A Play feature graphic sötét cockpitet és izzó tracket ígér; a HUD, a skyplot és a sebesség-szín már egyezik, a sötét csempe még nem.

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
- Compose paletta: világos sage/papír, sötét **Cockpit** (`Theme.kt`); a sötét a rendszer témáját követi

### Ami gyenge a listinghez és a használathoz

- **Térkép éjjel:** HUD van, a csempe nappali. Sötét csempén a sebességszín még nem látszott; fehéren a világos sávok kiégnek.
- **A térkép nem fordul:** a Google sziluett `rotation = 0f`, az OSM rétegek a `draw(..., _rotation)` paramétert eldobják. Menet közben a kanyar a képernyőn oldalra fut, nem előre. A fix `bearing`-je minden ponton megvan.
- **Álló sebesség:** a térkép HUD a chip nyers Dopplerét kerekíti egész km/h-ra. Bent, mozdulatlan pin mellett is kijön ~5 km/h. A sebességpontosság nincs kiolvasva.
- **Dőlés:** a `leanAngle` minden ponton tárolódik, a felület egy fokot mutat. Ráadásul a forrás `TYPE_GRAVITY` (tartalék: gyorsulásmérő), ami egyenletes kanyarban a motor síkjába eső látszólagos gravitációt méri, ezért **kanyarban ~0°** közelébe húz. Lásd 5.
- **Mentett útvonalak:** kártya van. Hátravan az opcionális fájlnév; az átlag és a max minden megnyitáskor a pontokból számol.
- **Téma:** nincs in-app Rendszer / Világos / Sötét, a `themes.xml` status bar light.
- **Értesítés:** statikus cím + szöveg + Leállít (`TrackingForegroundService.buildNotification`). Nincs élő sebesség / út.
- **Play feature graphic** (`docs/play-console/feature-graphic.png`): sötét műszerfal, izzó track, skyplot. A sötét csempe még hiányzik.

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

Ne „fésüld át Material 3-mal”. A paletta (teal, carmine, magenta, cockpit) már megkülönböztet. A HUD, a skyplot és a sebesség-színezett vonal megvan. A gond a **nappali térkép éjjel**, a **mindig északra néző** térkép menet közben, és hogy a tárolt dőlés **nem látszik**.

Ami a feature graphicot igazzá teszi:

1. Élő **térkép HUD** — kész
2. **Skyplot** a GPS fülön — kész
3. **Sebesség-színezett** vonal — kész
4. Ugyanez **sötét csempén**, menettel forduló kamerával és üstökösfarokkal — következő látvány

Vegyél HUD-os Térképet naplózás közben, és cseréld a feature graphicot **valódi UI-kivágásra**, ha a sötét csempe kész.

Motor az alap usage: az eye-catchernek **nappal és éjjel, kesztyűben, villantásra** is működnie kell (nagy szám, kevés koppintás, sötét térkép, előre néző út).

---

## Prioritás (érték szerint)

Az effort egy fejlesztő napja. A „fájlok” a természetes belépők, nem kimerítő lista. Elöl, ami minden felvételen látszik; hátul, amit ritkán nézel, vagy kevés telefonon működik.

### 1. Sötét térkép + in-app téma

**Érték:** magas — brand, éjszakai motor, listing-egyezés, a sebességszín és az üstökösfarok sötéten ad látványt  
**Effort:** 2–3 nap  
**Hullám:** 1

**Miért.** Éjjel a fehér térkép vakít, a sebességszín kiég. Kevésbé egyedi, mint a többi tétel, a használatnak mégis erős, és a 3., 5., 6. tétel látványa erre ül.

**Ma.** `GtlTheme(darkTheme = isSystemInDarkTheme())`. `gtlWash` gradient. `values/themes.xml`: teal status bar, paper nav bar, light. Nincs DataStore-kulcs a témára. Nincs `MapStyleOptions`.

**Mit építs.**

- Beállítás: **Rendszer / Világos / Sötét** (DataStore)
- Két stílus: Google Maps `MapStyleOptions` night JSON, Mapsforge sötét render theme (OSM és Turistautak), ugyanarra a kapcsolóra
- `Theme.Gtl` status/nav bar a témához; splash maradhat fekete
- HUD, polyline, pontfelhő kontrasztja sötét csempén (a lila körhöz világosabb stroke); a sebességsávok sötét változata, ha a mélyzöld / fekete sáv eltűnik

**Ne.** Harmadik „high contrast” paletta.

**Teszt.** Rendszer / Világos / Sötét; Google, OSM és Turistautak; HUD, sebességsáv és pontfelhő olvasható sötét csempén.

---

### 2. Álló sebesség a HUD-on

**Érték:** magas — a műszer állva is mozogni mutat; ugyanez a kapu kell a 3. irányához  
**Effort:** ~1 nap  
**Hullám:** 1

**Miért.** Bent, mozdulatlan pin mellett a térkép HUD ~5 km/h-t ír (nyers Doppler, `Units.hudSpeedNumber`). A Route lap idle-ben már 0 (`RouteTabSpeeds`). Fix km/h-küszöb a lassú gyaloglást vágná, egy nagyobb benti tüskét átengedne.

**Mit építs.** Egy tiszta engine-függvény; a térkép HUD, a Route pillanatnyi sebesség és az értesítés (4.) ezt hívja.

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

**Miért.** Motoros, futó, túrázó nem nézi a képernyőt. Ugyanazok a számok, mint a térkép HUD-on, lock screenen / shade-en.

**Ma.** `NOTIFICATION_ID = 17`, `IMPORTANCE_LOW`, Stop action, statikus stringek.

**Mit építs.** Periodikus `notify()`: sebesség, út, eltelt idő. Meglévő Stop. Nincs hang/rezgés. Android 16-on (targetSdk 36) a kiemelt, folyamatban lévő értesítés-stílus megfontolható; régebbin sima frissülő értesítés.

**Függőség.** A 2. kapuzott sebessége.

---

### 5. Dőlésszalag (motornapló)

**Érték:** magas a motoros alap usage-nek — a szerpentin ettől motornapló  
**Effort:** 3–4 nap  
**Hullám:** 2

**Miért.** A dőlés minden ponton a telefonon van, a felület egy fokot mutat. Bal/jobb dőlés szerinti szalag a vonal mentén, élő dőlésmérő, max bal / max jobb a kártyán.

**Kockázat — a mostani forrás kanyarban hazudik.** A `BikeLeanAngle.fromGravity` a `TYPE_GRAVITY` (tartalék: gyorsulásmérő) vektorából számol. Egyenletes, koordinált kanyarban a látszólagos gravitáció (g + centripetális) a motor síkjába esik, tehát egy tankra szerelt telefon **~0°-ot** lát. A gyro-fúziós gravitáció rövid ideig mutat valamit, hosszabb kanyarban visszahúz. Zsebben a gravitáció végképp hazudik.

**Mit építs.**

- Engine: dőlés a **kinematikából**: `lean ≈ atan(v · ω / g)`, ahol `v` a sebesség, `ω` a forduló szögsebessége. Mentett tracken `ω` a letárolt `bearing` változásából / idő, tehát **visszamenőleg minden meglévő motoros trackre** működik, és nem függ a telefon rögzítésétől. Élőben `ω` a giroszkópból (rotation vector yaw rate), ha van.
- Alacsony sebességen (pl. < 3 m/s) és ritka pontoknál nincs szalag (a bearing-zaj dominál).
- Rajzolás mindkét motoron: szalag a vonal mellett vagy választható színezés (sebesség / dőlés), bal és jobb külön árnyalattal; jelmagyarázat fokban.
- Mentett kártya: max bal / max jobb.
- **Fut/túrán rejtve**, kerékpáron opcionális.

**Ne elsőre.** A `TYPE_GRAVITY` dőlés szalagra rajzolása. Kalibrációs varázsló a tartóhoz.

**Teszt.** Engine: szintetikus körív adott sebességgel → ismert dőlés. Egyenes: ~0°. Álló: nincs szalag. Valódi szerpentin track: bal/jobb előjel helyes.

---

### 6. Üstökösfarok

**Érték:** közepes — olcsó, menet közben mozgást ad  
**Effort:** ~1 nap  
**Hullám:** 2

**Miért.** Naplózáskor az utolsó perc vastagabb és teljes színű, a régebbi vonal halkabb. A meglévő színezett vonalra ül, új adat nem kell.

**Mit építs.** A `SpeedTrack` szegmensek kapnak egy „kor” mezőt; szélesség és alfa a kor szerint (2–3 lépcső elég, nem méterenkénti gradiens). Csak élő sessionnél; mentett tracken egyenletes vonal. Google-on ügyelj a polyline-darabszámra.

**Függőség.** Sötét csempe (1.) mellett mutat igazán.

---

### 7. Repülés Google Earth-ben (KMZ `gx:Tour`)

**Érték:** közepes — nagy látvány, olcsó, pont a KMZ-közönségnek  
**Effort:** 1–2 nap  
**Hullám:** 3

**Miért.** A KMZ már a letárolt GPS-magasságon adja át a vonalat; egy `gx:Tour` a vonal mentén végigrepíti a kamerát a Google Earth-ben. A „helyi flyover” (12.) látványának nagy része, a költsége töredékéért.

**Mit építs.** `KmzExporter`: `gx:Tour` / `gx:Playlist` `gx:FlyTo` lépésekkel a ritkított pályán (heading a pálya irányából, tilt fix, range a sebességhez), opcionálisan a megosztási menüben. Tiszta engine-teszt a KML-re.

---

### 8. Mentett útvonalak: fájlnév és tárolt sebesség

**Érték:** közepes — a kártya megvan; a név és a sessionben tárolt sebesség hiányzik  
**Effort:** 1–2 nap  
**Hullám:** 3

**Hátravan.**

- Opcionális `displayName` a sessionön. Üres = dátum. A KMZ/GPX fájlnév és a `<name>` / KMZ folder ezt használja; a tiltott karaktereket cseréld.
- `track_sessions` bővítés (Room migráció 6→7): `avgSpeed`, `maxSpeed` (m/s), és ha az 5. kész, `maxLeanLeft` / `maxLeanRight`, Leállításkor számolva. A lista ezeket olvassa.

---

### 9. Track-kép / képeslap megosztás

**Érték:** közepes — social eye-catcher szerver nélkül  
**Effort:** 3–5 nap  
**Hullám:** 3

**Miért.** Sötét alapon izzó, sebesség-színezett vonal, táv, idő, magasságcsík, GTL pecsét, PNG a share sheetre. Nincs feltöltés. Jó listing-forrás.

**Mit építs.** Csempe nélkül: saját polyline sötét Canvasra (a `TrackRouteThumbnail` rajzolója nagyrészt újrahasznosítható) → Bitmap → share. Ne Static Maps API-val kezdd.

**Függőség.** 8. (név, statok); 5. ha a dőlés is rákerül.

---

### 10. Kanyargaléria

**Érték:** közepes motoron, önmagában egy lista  
**Effort:** 2–3 nap  
**Hullám:** 3

**Miért.** „Bal 38°, 72 km/h”: a mentett track legerősebb kanyarjai a bearing ugrásából és a dőlésből, koppintásra a térkép oda ugrik. A szalag (5.) után érdemes, mert ugyanazt az engine-számítást használja.

---

### 11. Kézi szünet és kör / lap

**Érték:** közepes  
**Effort:** 2–3 nap  
**Hullám:** 4

**Miért.** A `PAUSE` ma sebességküszöb. Pirosnál, benzinkútnál nem lehet szüneteltetni Leállítás (új session) nélkül.

**Mit építs.** Szünet / Folytat gomb naplózáskor; szünetben nincs MOVE; folytatáskor nincs új `track_sessions` sor. KMZ: meglévő pause ikon; GPX: `trkseg` a szünetnél. Kör utána.

---

### 12. Helyi flyover / visszajátszás

**Érték:** közepes — erős film, de a túra után egyszer nézed meg  
**Effort:** 5–8 nap  
**Hullám:** 4

**Miért.** A mentett track kirajzolódik, a dőlő sziluett végigmegy rajta, a HUD az adott pont számait mutatja. Jó Play-videó.

**Drága, mert:** csúszka, tempó (1× / 10× / 60×), dőlő ikon, kamera mindkét motoron. A 7. (`gx:Tour`) a látvány nagy részét olcsóbban adja; ez csak akkor, ha a felhasználók appon belül kérik.

**Függőség.** 3. (forgó kamera és sziluett), 5. (dőlő ikon).

---

### 13. Fekvő / tank HUD mód

**Érték:** közepes a default motorhoz, effort magas  
**Effort:** 5–8 nap  
**Hullám:** 4

**Miért.** Az app `portrait`. Tankra rakva a nagy számjegy fekvőben olvasható. A portrait HUD a haszon ~80%-át adja.

**Figyelem.** Mapsforge `MapView` + Compose rotáció; a 3. menetirányú kamerával együtt tesztelendő.

---

### 14. Quick Settings tile (Indít / Leállít)

**Érték:** alacsony–közepes  
**Effort:** ~1 nap  
**Hullám:** bármikor

**Miért.** Shade-ből indítás kesztyűben. `TileService`, ugyanazok az engedélyek, mint az Indít gomb. Nem listing-téma.

---

### 15. Magasság-szobor

**Érték:** alacsony — hágón és repülőúton szép, városi motoron lapos  
**Effort:** 3–5 nap  
**Hullám:** később

**Miért ilyen hátul.** A KMZ ezt a Google Earthnek már átadja (`absolute` magasság), a 7. pedig repülést is ad hozzá. Appon belüli 2,5D rajz csak hegyi / repülős usage-nél ér valamit.

---

### 16. Két út egymáson

**Érték:** alacsony — archívum  
**Effort:** 2–3 nap  
**Hullám:** később

**Miért ilyen hátul.** Két kijelölt mentett track egy térképen, eltérő színnel. A szem a különbségen akad meg, de a menet közbeni kép nem változik, és ritkán használt.

---

## Kiadási hullámok

A verziószámok **javaslatok**.

### Hullám 1 — „éjjel is látod” (kb. 4–6 nap)

| # | Tétel | Effort |
| - | ----- | ------ |
| 1 | Sötét térkép + Rendszer/Világos/Sötét | 2–3 nap |
| 2 | Álló sebesség a HUD-on | ~1 nap |
| 4 | Értesítés élő számokkal | 1–2 nap |
| — | Play screenshot + feature graphic a **valódi** HUD-os, sötét térképről | 0,5 nap |

**Kész, ha:** sötét módban a csempe sötét (Google, OSM, Turistautak); álló benti fixen a HUD 0 km/h; az értesítésben ugyanaz a szám, mint a HUD-on; a listing új képe a sötét HUD-os Térkép.

### Hullám 2 — „menet közben él a térkép” (kb. 7–9 nap)

| # | Tétel | Effort |
| - | ----- | ------ |
| 3 | Kapuzott menetirányú követés + sziluett a pálya felé | 3–4 nap |
| 5 | Dőlésszalag (kinematikai dőlés) | 3–4 nap |
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

## Összesítő tábla

| Rang | Feature | Érték | Effort | Hullám |
| ---- | ------- | ----- | ------ | ------ |
| 1 | Sötét térkép + téma-választó | magas | 2–3 nap | 1 |
| 2 | Álló sebesség a HUD-on | magas | ~1 nap | 1 |
| 3 | Kapuzott menetirányú követés + sziluett a pálya felé | magas | 3–4 nap | 2 |
| 4 | Élő értesítés | közepes–magas | 1–2 nap | 1 |
| 5 | Dőlésszalag (kinematikai) | magas (motor) | 3–4 nap | 2 |
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

Ha csak **kettőt** lehet: **sötét térkép + kapuzott menetirányú követés**. Az első éjjel, a második minden felvételen látszik.  
Ha a motoros közönségre lősz: a harmadik a **dőlésszalag**, kinematikai dőléssel.

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

## Nyitott döntések (implementáció előtt, hullámonként)

Hullám 1:

- Google night JSON: beépített stílus vagy saját, a cockpit teal/carmine köré?
- Mapsforge sötét téma: saját XML vagy a beépített téma átszínezése? A Turistautak témához külön kell?

Hullám 2:

- Menetirányú kamera alapértéke usage-enként (motor/autó be, Fut/túra ki?)
- Dőlés: szalag a vonal mellett, vagy sebesség / dőlés színezés-választó?
- Élő dőlés: giroszkóp yaw rate vagy csak a GPS bearing változása (késik ~1 fixet)?

Ezeket a hullám briefjében rögzítsd; a roadmap szándékosan nem fagyasztja a pixel-layoutot.
