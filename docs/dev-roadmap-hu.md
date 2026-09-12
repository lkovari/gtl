# GTL fejlesztési roadmap

[English](dev-roadmap-en.md) · [Magyar](dev-roadmap-hu.md)

**Állapot:** 2.0.5 (versionCode 23) utáni termékterv.  
**Nem kódspec:** ez a sorrend *miértjét* és a hullámokat rögzíti. Implementáció előtt a kiválasztott hullámra külön brief / tesztlista kell.  
**Effort:** egy, a kódbázist ismerő fejlesztő naptári napja (nem emberhónap, nem naptári hét csapatra).

Kapcsolódó: [README-hu.md](../README-hu.md), [CHANGELOGS.md](../CHANGELOGS.md), [RENEWAL-REPORT.md](RENEWAL-REPORT.md), [DBSTRUCT-en.md](DBSTRUCT-en.md), [GPSDATAFLOW-hu.md](GPSDATAFLOW-hu.md).

---

## Hogyan olvasd

A GTL (GPS Track Logger) 2014-es Eclipse-app Kotlin + Compose újraírása. A 2.0.x kiadások a **naplózási láncot** rakták helyre: Room az egyetlen igazságforrás, Kalman a letárolt pontokon, GNSS-only futó/kerékpár, KMZ, OSM, pontfelhő.

A következő lépés nem új szűrőalgoritmus. A hiány **termékélmény és interoperabilitás**: a felvétel közbeni térkép üresnek hat, az export csak Google Earth-höz barátságos, a Play feature graphic olyan cockpitet ígér, amit a valódi UI még nem ad.

A sorrend **érték szerint** van (megtartás × Play-konverzió × a már tárolt adat kiaknázása), nem könnyű győzelem szerint. Az effort másodlagos, de ahol két tétel közel azonos értékű, az olcsóbb előrébb kerül a hullámban.

---

## Termékpozíció

A GTL **helyben futó, precíziós GPS útvonalnapló**. Semmi nem kerül fel a mi szerverünkre. A térképvonal **maga** a SQLite tracklog, nem map-matching, nem második vázlat.

Célfelhasználó (a Beállítások usage sorrendje és az alap **motor** szerint):

- motoros / autós, aki utána Google Earth-ben vagy saját archívumban nézi az utat
- futó / kerékpáros, aki sportóra-szerű GNSS-tracket akar, utcára pattintás nélkül
- hajós / repülős, ICAO egységekkel, ritkább, de a usage modell már kezeli

A verseny **nem** a Strava, Komoot vagy Google Maps Navigation. Azok közösség, edzésterv, turn-by-turn. A GTL moatja:

1. adat a telefonon marad
2. a vonal az, amit a chip / Kalman tényleg rögzített
3. GNSS HUD (konstelláció, SNR, pontfelhő / CEP95)
4. KMZ balloonok Earth-höz

Minden új feature-nek ezt kell erősítenie, vagy **kibontania** (GPX: ki tudod vinni; HUD: látod felvétel közben), nem helyettesítenie közösségi feeddel.

---

## Hol tartunk ma (2.0.5)

### Ami erős

- Előtér-szolgáltatás, látható értesítés, nincs `ACCESS_BACKGROUND_LOCATION`
- Használati előbeállítások (repülő, hajó, autó, motor, kerékpár, futó) egy DataStore-szerkesztésben
- Szűrőlánc: pontosság / műhold → opcionális Kalman → sűrűség → Room → Térkép / Útvonal / KMZ
- GPS fül: L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC, SNR
- KMZ terepre feszített `LineString` (látható, magasság 0) plusz rejtett `gx:Track` az idősorhoz; Start / Pause / Stop balloon a letárolt vonalon (a Stop az utolsó elfogadott pont)
- OSM Mapsforge régióletöltés, Google Maps ha van `MAPS_API_KEY`
- Compose paletta: világos sage/papír, sötét **Cockpit** (`Theme.kt`); a sötét a rendszer témáját követi

### Ami gyenge a listinghez és a használathoz

- **Térkép naplózáskor:** HUD (nagy sebesség, út, REC) a Google- és OSM-térkép fölött. A listinghez még a régi screenshotok lehetnek a `docs/screenshots/`-ban.
- **Útvonal fül:** 2×4 `HudMetric` kártya plusz magasságprofil, ha van session.
- **Mentett útvonalak:** dátum + nyers `usageType` enum + `METRIC`. Nincs név, táv, mini-térkép.
- **Export:** KMZ és GPX 1.1. FIT / TCX / GPX import nincs.
- **Téma:** a cockpit paletta kész, a **térkép nappali marad**, nincs in-app Rendszer / Világos / Sötét, a `themes.xml` status bar light.
- **Értesítés:** statikus cím + szöveg + Leállít (`TrackingForegroundService.buildNotification`). Nincs élő sebesség / út.
- **Play feature graphic** (`docs/play-console/feature-graphic.png`): sötét műszerfal, izzó piros track, skyplot. Az app ezt a kompozíciót még nem adja. Ez a legnagyobb eye-catcher-rés.

### Szándékosan nincs (és maradjon így)

Lásd a renewal jelentést: IMEI, élő lat/lng feltöltés, follow-me web, távoli feloldás, Google Directions, app által kapcsolt GPS, boot auto-start. A `RemoteTrackSync` no-op csonk; **ne töltsd meg** backenddel, amíg a termék helyi logger.

---

## Ami ne kerüljön a backlog elejére

| Ötlet | Miért ne most |
| ----- | ------------- |
| Élő megosztás / saját szerver / `RemoteTrackSync` feltöltés | Szemben a privacy-politikával és a 2.0 ígérettel |
| Utcára pattintás (OSM/Google map-matching) | Szemben a futó GNSS-trackkel; a Kalman szándékosan nem ezt csinálja |
| Strava-szerű közösség, kudos, szegmensek | Más termék |
| Wear OS | Hetek, külön store, tesztmátrix; a telefonos HUD előbb |
| GPX import | Az app logger, nem archívum-kezelő |
| FIT / TCX | GPX után, ha valaki Garmin Connect-re kér |
| Turn-by-turn | Play / API / figyelemelterelés; a 2014-es Directions szándékosan kimaradt |
| Indítósáv-widget | A Quick Settings tile olcsóbb; widget később |

---

## Eye-catcher elv

Ne „fésüld át Material 3-mal”. A paletta (teal, carmine, magenta, cockpit) már megkülönböztet. A gond a **hierarchia** és a **térkép üressége**.

Három látvány, ami a feature graphicot igazzá teszi:

1. Élő **térkép HUD** (nagy sebesség, út, REC) — hullám 1
2. **Sebesség-színezett** vonal sötét térképen — hullám 2
3. **Skyplot** a GPS fülön — hullám 2

A Play screenshot innentől a Térkép HUD-os állapota, nem a számkártyás Útvonal. A feature graphicot a hullám 1 után cseréld **valódi UI-kivágásra**, ne 3D műhold-montázsra, ha a kettő már egyezik.

Motor az alap usage: az eye-catchernek **nappal és éjjel, kesztyűben, villantásra** is működnie kell (nagy szám, kevés koppintás, sötét térkép).

---

## Prioritás (érték szerint)

Az effort egy fejlesztő napja. A „fájlok” a természetes belépők, nem kimerítő lista.

### 1. Térkép HUD overlay — kész 2026-09-12

**Érték:** nagyon magas — eye-catcher és használat egyszerre  
**Effort:** 3–5 nap  
**Hullám:** 1

**Miért.** Felvétel közben a felhasználó a Térkép fület nézi. Ma ott egy piros vonal van egy nappali Google-térképen, számok nélkül. A GPS és Útvonal fülekre kell lapozni sebességért — ez vezetés közben veszélyes, és a listingen „üres térképnek” néz ki. A feature graphic HUD-ot ígér; a rés itt a legnagyobb.

**Ma.** `MapPane.kt`: Google Maps Compose + Mapsforge `AndroidView`. Overlay: pontossági kör, pontfelhő, usage-sziluett, északjelző, seprő. Nincs telemetria. Az Útvonal összesítők a `GtlViewModel` / `TrackStatsCalculator` élő mintáiból jönnek, de csak a Route fülön.

**Mit építs.** Egy **közös Compose HUD** a térkép tetején (mindkét motor fölött, ne két overlay-implementáció):

- nagy, olvasható **sebesség** (metrikus / angolszász / ICAO a Beállításokból)
- **út** és **eltelt idő**
- **pontosság** méterben + GNSS used / in view
- pulzáló **REC**, ha `live.logging`
- opcionális **képernyő bekapcsolva** naplózáskor (tankra szerelt telefon)

A HUD nyers HUD-fixet mutasson sebességre / pontosságra (ugyanaz a filozófia, mint a lila kör), az utat a Room-statból. Ne takarja el a vonalat: alsó vagy felső sáv, félig átlátszó, cockpit színek sötétben.

**Függőség.** Nincs. A sötét térkép (3.) utána jobban áll.

**Teszt.** Naplózás Google-on és letöltött OSM-en; idle + mentett track (HUD halkul vagy eltűnik, ha nincs logging — döntsd el egy helyen); mértékegység-váltás; keep-screen-on csak logging alatt.

---

### 2. GPX export — kész 2026-09-12

**Érték:** nagyon magas — az adat kikerül a szigetről  
**Effort:** 1,5–2,5 nap  
**Hullám:** 1

**Miért.** A KMZ Google Earth-höz ideális (`gx:Track`, play/pause/stop ikon, balloon). A tracklog-világ többi része **GPX 1.1**-et vár: OsmAnd, Komoot, Garmin Connect, Relive, QGIS, sok sportóra-web. Enélkül a GTL zárt formátumú napló. A README ezt már listázza.

**Ma.** `KmlExportUseCase` → `KmlExporter` + `KmzExporter`, FileProvider, share sheet. Mentett útvonalak: egy session egy KMZ, több session mappánként egy KMZ-ben.

**Mit építs.**

- `:engine` `GpxExporter`: `trk` / `trkseg` / `trkpt` (`lat`, `lon`, `ele`, `time`; opcionális `speed` GPX-kiterjesztésben vagy elhagyva — először a mag GPX, hogy minden importer nyeljen)
- START/PAUSE/STOP: `wpt`, vagy egy `trkseg` szakaszonként, ha később jön a kézi szünet
- Share: **KMZ vagy GPX** (rendszerchooser vagy in-app két gomb). Több kijelölés: egy `.gpx` több `trk`-kel, vagy több fájl — az egy fájl, több track egyszerűbb
- MIME `application/gpx+xml`, fájlnév `GTL_yyyyMMdd_HHmmss.gpx`
- Súgó EN/HU, engine unit teszt fix koordinátákkal

**Ne most.** FIT, TCX, GPX import.

**Függőség.** Nincs. A session-név (6.) később beírható a `<name>`-be.

---

### 3. Sötét térkép + in-app téma

**Érték:** magas — brand, éjszakai motor, listing-egyezés  
**Effort:** 2–3 nap  
**Hullám:** 1

**Miért.** A Compose már tud cockpit sötétet (`isSystemInDarkTheme()`). A Google Maps és a Mapsforge **nappali** csempe marad. Éjjel a fehér térkép vakít, a magenta cím + sötét top bar + világos térkép szétesik. A README „világos és sötét téma” azért van még a teendőkben, mert a **térkép és a rendszerchrome** nincs kész, csak a kártyák.

**Ma.** `GtlTheme(darkTheme = isSystemInDarkTheme())`. `gtlWash` gradient. `values/themes.xml`: teal status bar, paper nav bar, light. Nincs DataStore-kulcs a témára.

**Mit építs.**

- Beállítás: **Rendszer / Világos / Sötét** (DataStore)
- Google Maps `MapStyleOptions` night JSON, ha a téma sötét
- Mapsforge: sötét render theme (beépített vagy saját XML), ugyanarra a kapcsolóra
- `Theme.Gtl` status/nav bar a témához; splash maradhat fekete
- HUD, polyline, pontfelhő kontrasztja sötét csempén (a carmine megmaradhat, a lila körhöz világosabb stroke)

**Ne.** Harmadik „high contrast” paletta. Elég a két scheme, ami a `Color.kt`-ban van.

**Függőség.** A HUD (1.) sötét stílusát ezzel együtt csiszold, ha ugyanabban a kiadásban mennek.

---

### 4. Élő előtér-értesítés

**Érték:** közepes–magas — második HUD, zsebben  
**Effort:** 1–2 nap  
**Hullám:** 1

**Miért.** A naplózás foreground service. Motoros/futó nem nézi a képernyőt. Az értesítés ma „megy a naplózás” szöveg. Ugyanazok a számok, mint a térkép HUD-on, lock screenen / shade-en.

**Ma.** `NOTIFICATION_ID = 17`, `IMPORTANCE_LOW`, Stop action, statikus stringek.

**Mit építs.** Periodikus `notify()` frissítés: sebesség, út, pontosság (rövid `contentText` vagy `BigText`). Meglévő Stop. Ne legyen hang/rezgés (LOW marad). `FLAG_UPDATE_CURRENT`.

**Függőség.** Ugyanaz a formázó, mint a HUD (`Units`). Érdemes a HUD után vagy vele párhuzamosan, hogy ne legyen kétféle kerekítés.

---

### 5. Sebesség szerint színezett track + Route cockpit

**Érték:** magas — második eye-catcher, a számkártyák helyett  
**Effort:** 4–6 nap  
**Hullám:** 2

**Miért.** Egyetlen carmine polyline pontos, de a listingen „piros firkának” hat. Sebesség-szín (lassú teal → közép borostyán → gyors carmine) azonnal mesél: város vs autópálya, emelkedő vs lejtő. Az Útvonal fülön a sebesség legyen **a** szám, ne nyolc egyenlő csempe egyike.

**Ma.** `Polyline` / Mapsforge polyline egy szín, `CarmineTrack`. `RoutePane`: `HudMetric` rács. `TrackStats`: odometer, moving/waiting, max/avg speed, min/max altitude — nincs idősor-rajz.

**Mit építs.**

- Engine: szakaszok `speedMps` szerint (küszöb usage-hez vagy percentilis a sessionhöz — az első verziónál **fix sávok** mértékegység szerint, hogy a jelmagyarázat stabil legyen)
- Google: több rövid polyline vagy `span`; OSM: szegmens overlay. DP egyszerűsítés **után** színezz, különben a ritkított vonal hamis sebességet kap a húron
- Jelmagyarázat a térkép sarkában
- Route: nagy sebesség, alatt sparkline (sebesség vagy magasság), a többi metrika másodlagos

**Ne elsőre.** Magasság-szín és sebesség-szín egyszerre (választó később). Interpolált gradiens minden méterre — a szegmens elég.

**Függőség.** Sötét térkép (3.), hogy a színek ne égjenek ki a fehér csempén. HUD (1.) maradhat egy színű „élő” fej, a múlt színezett.

---

### 6. GNSS skyplot

**Érték:** magas a márkához, közepes a napi motoroshoz  
**Effort:** 3–4 nap  
**Hullám:** 2

**Miért.** A konstelláció-chippek egyediek, de a feature graphic **polar plotot** mutat. GPSTest / nerd loggerek ezt várják. A GTL GNSS-hitelessége itt válik láthatóvá: used vs in view, L5, Galileo.

**Ma.** `GnssStatusSource` mintavételez, de a `SatelliteSample` **nem** tárol azimutot és elevációt, pedig a `GnssStatus.getAzimuthDegrees` / `getElevationDegrees` megvan. A `GnssClassifier.snapshot` összesít, egyedi holdak nincsenek a UI-on.

**Mit építs.**

- `SatelliteSample` + snapshot lista: azimut, eleváció, CN0, used, konstelláció, L1/L5
- Canvas polar: 0° = észak, gyűrűk 0/30/60° eleváció; szín konstellációnként; kitöltött = used-in-fix
- GPS fül: skyplot fent vagy az SNR alatt, a chippek maradnak
- Idle-ben is él (mint az iránytű) — nem kell naplózás

**Ne.** 3D földgömb, AR. 2D polar + a meglévő chippek.

**Függőség.** Nincs a HUD-hoz. Screenshot: GPS fül skyplottal a listingre.

---

### 7. Mentett útvonalak: kártya, név, statok

**Érték:** közepes–magas — a saját archívum használhatóvá válik  
**Effort:** 3–4 nap  
**Hullám:** 2

**Miért.** Leállítás után a lista dátum. Két szombati motoros kör megkülönböztethetetlen. Nincs táv, nincs usage-ikon, a `TWO_WHEELERS` nyers enum a UI-on. Megosztáskor a fájlnév időbélyeg.

**Ma.** `track_sessions`: `startedAt`, `stoppedAt`, `usageType`, `measurementSystem`. Nincs `displayName`. `TracksScreen`: checkbox, Térképen, Törlés, kijelöltek megosztása.

**Mit építs.**

- Opcionális `displayName` (Room migráció 3→4). Üres = dátum, mint most
- Lista kártya: usage ikon, név/dátum, út, időtartam, max/átlag (a `TrackStatsCalculator` sessionenként — cache-eld a listához, ne minden scrollra a teljes `gps_events`-et)
- Mini-polyline opcionális (drágább; elsőre statok + ikon is sokat visz)
- Megosztás KMZ **vagy** GPX; a `<name>` / KMZ folder a displayName
- Törlés megerősítés, ha még nincs

**Függőség.** GPX (2.), ha a választó itt jelenik meg. A színezett track (5.) a Térképen-nézetet szépíti, a listát nem blokkolja.

---

### 8. Magasságprofil (GPS először, baro később) — GPS-profil kész 2026-09-12; QNH később

**Érték:** közepes  
**Effort:** 2–3 nap a profilra; +2–3 nap baróra  
**Hullám:** 3 (profil), később baro

**Miért.** Futó, kerékpár, repülő nézi a szintet. A `gps_events.altitude` GPS-magasság, zajos, de van. A DBSTRUCT a barót (`TYPE_PRESSURE`) planned-ként említi, ICAO ft-hez.

**Ma (2026-09-12).** Mentett útvonalak **Magasság** gombja és a Route fül: GPS-magasság × táv canvas. `baroAltitude` / `pressureHpa` a `gps_events`-en (Room 4); ISA, nincs QNH. Ha van legalább két baro minta, szaggatott második vonal.

**Baro később.** QNH / tengerszint kalibráció, repülő usage. Az oszlop megvan; ne keverd a GPS alt-tal jelmagyarázat nélkül (a profil már külön vonal).

**Függőség.** Route cockpit (5.) ad helyet a sparkline-nak; a teljes profil lehet a kártya alatt.

---

### 9. Kézi szünet és kör / lap

**Érték:** közepes  
**Effort:** 2–3 nap  
**Hullám:** 3

**Miért.** A `PAUSE` ma sebességküszöb (0,25 m/s gyalogos, 0,4 jármű). A felhasználó nem tudja megállítani a felvételt pirosnál anélkül, hogy Leállítana (új session). Futó kör, motoros benzinkút: kézi szünet + folytatás ugyanabban a sessionben. Opcionális lap (köridő) a `eventKind` vagy külön split táblával.

**Ma.** `EventKind`: START, MOVE, PAUSE, STOP. STOP lezárja a sessiont (`stoppedAt`). Nincs user-pause a UI-on, csak Indít / Leállít.

**Mit építs.** Harmadik gomb naplózáskor: Szünet / Folytat. Szünetben a service futhat, de ne írjon MOVE-ot (vagy írjon PAUSE placemarkot és hagyja a sűrűséget). Folytatáskor ne legyen új `track_sessions` sor. KMZ: meglévő pause ikon.

**Kör.** Lehet a szünet után; elsőre a kézi szünet a 80%.

**Függőség.** GPX `trkseg` a szünetnél természetes. HUD: Szünet állapot a REC helyett.

---

### 10. Fekvő / tank HUD mód

**Érték:** közepes a default motorhoz, effort magas  
**Effort:** 5–8 nap  
**Hullám:** 3 vagy később

**Miért.** Az app `portrait`. Tankra rakva a nagy számjegy fekvőben olvasható. A 1. tétel portrait HUD-ja a haszon ~80%-át adja.

**Mit építs, ha jön.** Landscape activity vagy a fő képernyő lock-feloldása naplózáskor; óriás sebesség; térkép keskeny sáv; mindkét map motor. Figyelem: Mapsforge `MapView` + Compose rotáció.

**Függőség.** 1. és 3. kész legyen, különben kétszer rakod a HUD-ot.

---

### 11. Track-kép / képeslap megosztás

**Érték:** közepes — social eye-catcher szerver nélkül  
**Effort:** 4–6 nap  
**Hullám:** 3+

**Miért.** Sötét alapon izzó vonal, táv, idő, GTL pecsét, PNG a share sheetre. A privacy megmarad (nincs feltöltés). Jó listing-screenshot forrás.

**Drága, mert:** statikus térkép-snapshot (Google Static / OSM render / saját polyline sötét háttéren). Az utolsó a legegyszerűbb és offline: nem csempe, csak vonal + statok. Kezdd azzal, ne Static Maps API-val.

**Függőség.** 5. (szín) és 7. (név/stat) a képeslapot tartalommal tölti.

---

### 12. Quick Settings tile (Indít / Leállít)

**Érték:** alacsony–közepes  
**Effort:** ~1 nap  
**Hullám:** 1 végétől bármikor, vagy 3

**Miért.** Shade-ből indítás kesztyűben. `TileService`, ugyanazok az engedélyek, mint az Indít gomb. Nem listing-téma.

**Függőség.** Nincs. Az értesítés (4.) Stop actionje már ad egy vezérlőt.

---

## Kiadási hullámok

A verziószámok **javaslatok**. A 2.0.5 patch maradhat hotfixnek; a következő minor a hullám 1.

### Hullám 1 — „látod és ki tudod vinni” (kb. 1–1,5 hét)

Cél: a térkép felvétel közben műszer, az adat GPX-ben elmegy, éjjel nem vakít.

| # | Tétel | Effort |
| - | ----- | ------ |
| 1 | Térkép HUD + keep-screen-on | 3–5 nap |
| 2 | GPX export a megosztásban | 1,5–2,5 nap |
| 3 | Sötét térkép + Rendszer/Világos/Sötét | 2–3 nap |
| 4 | Értesítés élő számokkal | 1–2 nap |
| — | Play screenshot + feature graphic frissítés a **valódi** HUD-os térképről | 0,5 nap |

Párhuzamosítható: GPX (engine teszt) a HUD UI mellett. A téma és a HUD vizuálisan egybeér.

**Kész, ha:** OsmAnd megnyit egy GTL GPX-et; naplózáskor a Térkép fülön nagy sebesség látszik Google-on és OSM-en; sötét módban a csempe sötét; az értesítésben van km/h és km; a listing új 9:16 képe nem a régi üres térkép.

### Hullám 2 — „az archívum és a GNSS látszik” (kb. 1,5–2 hét)

| # | Tétel | Effort |
| - | ----- | ------ |
| 5 | Színezett polyline + Route nagy sebesség / sparkline | 4–6 nap |
| 6 | Skyplot | 3–4 nap |
| 7 | Mentett track kártyák + displayName | 3–4 nap |

**Kész, ha:** egy autópályás szakasz nem azonos színű, mint a város; a GPS fül polar plotot mutat idle-ben; két session névvel megkülönböztethető; a Térképen a színezés a session usage sávjait használja.

### Hullám 3 — mélyítés (később, darabolva)

| # | Tétel | Effort |
| - | ----- | ------ |
| 8 | Magasságprofil (GPS) | 2–3 nap |
| 9 | Kézi szünet | 2–3 nap |
| 10 | Fekvő HUD | 5–8 nap |
| 11 | Képeslap PNG | 4–6 nap |
| 12 | Quick Settings tile | ~1 nap |
| — | Baro magasság | +2–3 nap |

A 10. és 11. a drágák: csak akkor, ha a hullám 1–2 után még a „tankra raknám” / „megosztanám képként” a panasz.

---

## Összesítő tábla

| Rang | Feature | Érték | Effort | Hullám |
| ---- | ------- | ----- | ------ | ------ |
| 1 | Térkép HUD overlay | nagyon magas | 3–5 nap | 1 |
| 2 | GPX export | nagyon magas | 1,5–2,5 nap | 1 |
| 3 | Sötét térkép + téma-választó | magas | 2–3 nap | 1 |
| 4 | Élő értesítés | közepes–magas | 1–2 nap | 1 |
| 5 | Színezett track + Route cockpit | magas | 4–6 nap | 2 |
| 6 | GNSS skyplot | magas (márka) | 3–4 nap | 2 |
| 7 | Mentett track kártyák + név | közepes–magas | 3–4 nap | 2 |
| 8 | Magasságprofil | közepes | 2–3 nap | 3 |
| 9 | Kézi szünet / lap | közepes | 2–3 nap | 3 |
| 10 | Fekvő tank HUD | közepes | 5–8 nap | 3+ |
| 11 | Képeslap megosztás | közepes | 4–6 nap | 3+ |
| 12 | Quick Settings tile | alacsony–közepes | ~1 nap | bármikor |

Hullám 1 összeg: **kb. 8–13 nap** + listing asset.  
Hullám 2: **kb. 10–14 nap**.  
Ha csak **kettőt** lehet: **HUD + GPX**. Ez zárja a „ezt mutatom a boltban” és a „ki tudom vinni” rést.

---

## Dokumentáció és Play, minden hullám végén

Nem opcionális toldalék:

- [CHANGELOGS.md](../CHANGELOGS.md) EN + Magyar
- [docs/play-console/whatsnew.txt](play-console/whatsnew.txt) (500 karakter / nyelv)
- Súgó EN/HU a új vezérlőkre
- README funkciólista, ha a felhasználó látja
- Screenshot 1080×1920, 24 bit, nincs alfa; hullám 1-ben legalább `map.png` / `tracking.png` / egy GPS, ha skyplot később jön

A [GPSDATAFLOW](GPSDATAFLOW-hu.md) csak akkor változik, ha a lánc írása változik (GPX olvas Room-ot, mint a KMZ — általában elég a README export-bekezdés). Skyplot: `Gnss.kt` + GPSDATAFLOW HUD ág, ha a snapshot séma nő. Session név: [DBSTRUCT](DBSTRUCT-en.md) migráció.

---

## Nyitott döntések (implementáció előtt, hullámonként)

Hullám 1 (2026-09-12 lezárva):

- HUD idle: kompakt sáv GPS-fixnél; rejtve mentett track idle-ben; teljes sáv naplózáskor.
- Keep-screen-on alapból ki.
- GPX: egy fájl több `trk`.

Hullám 2:

- Sebességsávok globálisak (0–30 / 30–70 / 70+ km/h) vagy usage-enként (futó más skála)?
- Skyplot a GPS fül tetején (több scroll) vagy csukható?

Ezeket a hullám briefjében rögzítsd; a roadmap szándékosan nem fagyasztja a pixel-layoutot.
