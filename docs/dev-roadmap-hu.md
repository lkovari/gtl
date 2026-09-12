# GTL fejlesztési roadmap

[English](dev-roadmap-en.md) · [Magyar](dev-roadmap-hu.md)

**Állapot:** termékterv a 2.0.6 (versionCode 24) után. A fában már benne van a térkép HUD, a GPX 1.1, a skyplot, a magasságprofil, a QNH, a GPS-magasság választás és az OSM fájl/kamera védelem.  
**Nem kódspec:** ez a sorrend *miértjét* és a hullámokat rögzíti. Implementáció előtt a kiválasztott hullámra külön brief / tesztlista kell.  
**Effort:** egy, a kódbázist ismerő fejlesztő naptári napja (nem emberhónap, nem naptári hét csapatra).

Kapcsolódó: [README-hu.md](../README-hu.md), [CHANGELOGS.md](../CHANGELOGS.md), [RENEWAL-REPORT.md](RENEWAL-REPORT.md), [DBSTRUCT-en.md](DBSTRUCT-en.md), [GPSDATAFLOW-hu.md](GPSDATAFLOW-hu.md), [all-gps-systems-hu.md](all-gps-systems-hu.md).

---

## Hogyan olvasd

A GTL (GPS Track Logger) 2014-es Eclipse-app Kotlin + Compose újraírása. A 2.0.x kiadások a **naplózási láncot** rakták helyre: Room az egyetlen igazságforrás, Kalman a letárolt pontokon, GNSS-only futó/kerékpár, KMZ, OSM, pontfelhő. A 2.0.6 utáni fában a felvétel közbeni **térkép HUD**, a **GPX**, a GPS **skyplot**, a **magasságprofil** (QNH-s baro vonallal), a **GPS-magasság** választás (MSL, majd GNSS, a fused szemét eldobva) és az OSM **fájlellenőrzés** is megvan (a Használ nem crash-loop; a kamera a letöltött régión marad).

A következő hiány **éjszakai használat, archívum és a második eye-catcher**: a térkép nappali marad, az értesítés statikus, a mentett lista dátum, a vonal egy színű. A Play feature graphic sötét cockpitet és izzó tracket ígér; a HUD és a skyplot már egyezik, a sötét csempe és a sebesség-szín még nem.

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
3. GNSS HUD (konstelláció, SNR, pontfelhő / CEP95, skyplot)
4. KMZ balloonok Earth-höz; GPX a többi eszközhöz

Minden új feature-nek ezt kell erősítenie, vagy **kibontania** (sötét térkép: éjjel is látod; kártyák: a saját logod olvasható), nem helyettesítenie közösségi feeddel.

---

## Hol tartunk ma

### Ami erős

- Előtér-szolgáltatás, látható értesítés, nincs `ACCESS_BACKGROUND_LOCATION`
- Használati előbeállítások (repülő, hajó, autó, motor, kerékpár, futó) egy DataStore-szerkesztésben
- Szűrőlánc: pontosság / műhold → opcionális Kalman → sűrűség → Room → Térkép / Útvonal / KMZ / GPX
- Térkép HUD (nagy sebesség, pontosság, GNSS used/in view; naplózáskor út, idő, pulzáló REC); keep-screen-on beállítás
- GPS fül: L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC, SNR, polar skyplot; magasság a `GpsAltitude.pick`-ből; baro, ha van nyomásszenzor
- KMZ terepre feszített `LineString` (látható, magasság 0) plusz rejtett `gx:Track` az idősorhoz; Start / Pause / Stop balloon a letárolt vonalon (a Stop az utolsó elfogadott pont)
- GPX 1.1 megosztás (egy fájl, több `trk`; START/PAUSE/STOP `wpt`)
- Magasságprofil (GPS × táv; szaggatott baro, QNH 900–1100 hPa a Beállításokból; tengely legalább 50 m)
- OSM Mapsforge régióletöltés `OsmMapFile` ellenőrzéssel; sikertelen nyitás kikapcsolja a **Letöltött OSM térkép használatát**; a kamera a `.map`-en marad, ha a GPS azon kívül van. Google Maps, ha van `MAPS_API_KEY`
- Zöld **S** / piros **E** a kirajzolt tracken (a vég rejtve naplózáskor)
- Compose paletta: világos sage/papír, sötét **Cockpit** (`Theme.kt`); a sötét a rendszer témáját követi

### Ami gyenge a listinghez és a használathoz

- **Térkép éjjel:** HUD van, a csempe nappali. A listing `docs/screenshots/` képei (2026-09-12) a GPS skyplotot, az Útvonal magasságprofilt, az Iránytű MAG rózsát, a Beállítások QNH-ját, a Mentett track Magasságot és a Térkép S/E-t mutatják. A HUD-os Térkép még hiányzik a listingről (a HUD rejtve, ha mentett track látszik).
- **Útvonal fül:** 2×4 `HudMetric` kártya plusz magasságprofil. A sebesség nem *a* szám.
- **Mentett útvonalak:** dátum + nyers `usageType` enum + `METRIC`. Van Magasság, törlés-megerősítés, KMZ/GPX. Nincs név, táv, mini-térkép.
- **Téma:** a cockpit paletta kész, a **térkép nappali marad**, nincs in-app Rendszer / Világos / Sötét, a `themes.xml` status bar light.
- **Értesítés:** statikus cím + szöveg + Leállít (`TrackingForegroundService.buildNotification`). Nincs élő sebesség / út.
- **Play feature graphic** (`docs/play-console/feature-graphic.png`): sötét műszerfal, izzó track, skyplot. HUD és skyplot megvan; a sötét csempe és a sebesség-szín még hiányzik.

### Szándékosan nincs (és maradjon így)

Lásd a renewal jelentést: IMEI, élő lat/lng feltöltés, follow-me web, távoli feloldás, Google Directions, app által kapcsolt GPS, boot auto-start. A `RemoteTrackSync` no-op csonk; **ne töltsd meg** backenddel, amíg a termék helyi logger.

---

## Ami ne kerüljön a backlog elejére

| Ötlet | Miért ne most |
| ----- | ------------- |
| Élő megosztás / saját szerver / `RemoteTrackSync` feltöltés | Szemben a privacy-politikával és a 2.0 ígérettel |
| Utcára pattintás (OSM/Google map-matching) | Szemben a futó GNSS-trackkel; a Kalman szándékosan nem ezt csinálja |
| Strava-szerű közösség, kudos, szegmensek | Más termék |
| Wear OS | Hetek, külön store, tesztmátrix; a telefonos HUD megvan |
| GPX import | Az app logger, nem archívum-kezelő |
| FIT / TCX | Ha valaki Garmin Connect-re kér |
| Turn-by-turn | Play / API / figyelemelterelés; a 2014-es Directions szándékosan kimaradt |
| Indítósáv-widget | A Quick Settings tile olcsóbb; widget később |

---

## Eye-catcher elv

Ne „fésüld át Material 3-mal”. A paletta (teal, carmine, magenta, cockpit) már megkülönböztet. A HUD és a skyplot megvan. A gond a **hierarchia** a Route-on, a **nappali térkép éjjel**, és hogy a vonal **egy színű**.

Ami a feature graphicot igazzá teszi:

1. Élő **térkép HUD** — kész
2. **Skyplot** a GPS fülön — kész
3. **Sebesség-színezett** vonal sötét térképen — következő látvány

A Play listing (2026-09-12) GPS skyplotot, Útvonal magasságprofilt, Iránytűt, Beállítások QNH-ját, Mentett trackeket és Térkép S/E-t mutat — nem a HUD-os Térképet (a HUD rejtve a kirajzolt mentett tracken). Vegyél HUD-os Térképet naplózás közben, és cseréld a feature graphicot **valódi UI-kivágásra**, ha a sötét csempe és a színezett vonal már egyezik.

Motor az alap usage: az eye-catchernek **nappal és éjjel, kesztyűben, villantásra** is működnie kell (nagy szám, kevés koppintás, sötét térkép).

---

## Prioritás (érték szerint)

Az effort egy fejlesztő napja. A „fájlok” a természetes belépők, nem kimerítő lista.

### 1. Sötét térkép + in-app téma

**Érték:** magas — brand, éjszakai motor, listing-egyezés  
**Effort:** 2–3 nap  
**Hullám:** 1

**Miért.** A Compose már tud cockpit sötétet (`isSystemInDarkTheme()`). A Google Maps és a Mapsforge **nappali** csempe marad. Éjjel a fehér térkép vakít, a magenta cím + sötét top bar + világos térkép szétesik. A README „világos és sötét téma” azért van még a teendőkben, mert a **térkép és a rendszerchrome** nincs kész, csak a kártyák. A HUD sötét stílusa a csempével együtt áll.

**Ma.** `GtlTheme(darkTheme = isSystemInDarkTheme())`. `gtlWash` gradient. `values/themes.xml`: teal status bar, paper nav bar, light. Nincs DataStore-kulcs a témára.

**Mit építs.**

- Beállítás: **Rendszer / Világos / Sötét** (DataStore)
- Google Maps `MapStyleOptions` night JSON, ha a téma sötét
- Mapsforge: sötét render theme (beépített vagy saját XML), ugyanarra a kapcsolóra
- `Theme.Gtl` status/nav bar a témához; splash maradhat fekete
- HUD, polyline, pontfelhő kontrasztja sötét csempén (a carmine megmaradhat, a lila körhöz világosabb stroke)

**Ne.** Harmadik „high contrast” paletta. Elég a két scheme, ami a `Color.kt`-ban van.

**Függőség.** Nincs. A HUD már megy mindkét map motor fölött.

**Teszt.** Rendszer / Világos / Sötét a Beállításokban; Google és letöltött OSM; HUD és pontfelhő olvasható sötét csempén; status bar a témához.

---

### 2. Élő előtér-értesítés

**Érték:** közepes–magas — második HUD, zsebben  
**Effort:** 1–2 nap  
**Hullám:** 1

**Miért.** A naplózás foreground service. Motoros/futó nem nézi a képernyőt. Az értesítés ma „megy a naplózás” szöveg. Ugyanazok a számok, mint a térkép HUD-on, lock screenen / shade-en.

**Ma.** `NOTIFICATION_ID = 17`, `IMPORTANCE_LOW`, Stop action, statikus stringek.

**Mit építs.** Periodikus `notify()` frissítés: sebesség, út, pontosság (rövid `contentText` vagy `BigText`). Meglévő Stop. Ne legyen hang/rezgés (LOW marad). `FLAG_UPDATE_CURRENT`.

**Függőség.** Ugyanaz a formázó, mint a HUD (`Units`). A HUD már megvan, ne legyen kétféle kerekítés.

---

### 3. Sebesség szerint színezett track + Route cockpit

**Érték:** magas — második eye-catcher, a számkártyák helyett  
**Effort:** 4–6 nap  
**Hullám:** 2

**Miért.** Egyetlen carmine polyline pontos, de a listingen „piros firkának” hat. Sebesség-szín (lassú teal → közép borostyán → gyors carmine) azonnal mesél: város vs autópálya, emelkedő vs lejtő. Az Útvonal fülön a sebesség legyen **a** szám, ne nyolc egyenlő csempe egyike. A magasságprofil a kártyák alatt már megvan.

**Ma.** `Polyline` / Mapsforge polyline egy szín, `CarmineTrack`. `RoutePane`: `HudMetric` rács + magasságprofil. `TrackStats`: odometer, moving/waiting, max/avg speed, min/max altitude — nincs sebesség-idősor.

**Mit építs.**

- Engine: szakaszok `speedMps` szerint (küszöb usage-hez vagy percentilis a sessionhöz — az első verziónál **fix sávok** mértékegység szerint, hogy a jelmagyarázat stabil legyen)
- Google: több rövid polyline vagy `span`; OSM: szegmens overlay. DP egyszerűsítés **után** színezz, különben a ritkított vonal hamis sebességet kap a húron
- Jelmagyarázat a térkép sarkában
- Route: nagy sebesség, alatt sparkline (sebesség vagy magasság), a többi metrika másodlagos

**Ne elsőre.** Magasság-szín és sebesség-szín egyszerre (választó később). Interpolált gradiens minden méterre — a szegmens elég.

**Függőség.** Sötét térkép (1.), hogy a színek ne égjenek ki a fehér csempén. A HUD maradhat egy színű „élő” fej, a múlt színezett.

---

### 4. Mentett útvonalak: kártya, név, statok

**Érték:** közepes–magas — a saját archívum használhatóvá válik  
**Effort:** 3–4 nap  
**Hullám:** 2

**Miért.** Leállítás után a lista dátum. Két szombati motoros kör megkülönböztethetetlen. Nincs táv, nincs usage-ikon, a `TWO_WHEELERS` nyers enum a UI-on. Megosztáskor a fájlnév időbélyeg.

**Ma.** `track_sessions`: `startedAt`, `stoppedAt`, `usageType`, `measurementSystem`. Nincs `displayName`. `TracksScreen`: checkbox, Térképen, Magasság, Törlés megerősítéssel, kijelöltek megosztása KMZ vagy GPX.

**Mit építs.**

- Opcionális `displayName` (Room migráció 4→5). Üres = dátum, mint most
- Lista kártya: usage ikon, név/dátum, út, időtartam, max/átlag (a `TrackStatsCalculator` sessionenként — cache-eld a listához, ne minden scrollra a teljes `gps_events`-et)
- Mini-polyline opcionális (drágább; elsőre statok + ikon is sokat visz)
- A `<name>` / KMZ folder a displayName (a KMZ/GPX választó megvan)

**Függőség.** A színezett track (3.) a Térképen-nézetet szépíti, a listát nem blokkolja.

---

### 5. Kézi szünet és kör / lap

**Érték:** közepes  
**Effort:** 2–3 nap  
**Hullám:** 3

**Miért.** A `PAUSE` ma sebességküszöb (0,25 m/s gyalogos, 0,4 jármű). A felhasználó nem tudja megállítani a felvételt pirosnál anélkül, hogy Leállítana (új session). Futó kör, motoros benzinkút: kézi szünet + folytatás ugyanabban a sessionben. Opcionális lap (köridő) a `eventKind` vagy külön split táblával.

**Ma.** `EventKind`: START, MOVE, PAUSE, STOP. STOP lezárja a sessiont (`stoppedAt`). Nincs user-pause a UI-on, csak Indít / Leállít.

**Mit építs.** Harmadik gomb naplózáskor: Szünet / Folytat. Szünetben a service futhat, de ne írjon MOVE-ot (vagy írjon PAUSE placemarkot és hagyja a sűrűséget). Folytatáskor ne legyen új `track_sessions` sor. KMZ: meglévő pause ikon. GPX: `trkseg` a szünetnél természetes.

**Kör.** Lehet a szünet után; elsőre a kézi szünet a 80%.

**Függőség.** HUD: Szünet állapot a REC helyett.

---

### 6. Fekvő / tank HUD mód

**Érték:** közepes a default motorhoz, effort magas  
**Effort:** 5–8 nap  
**Hullám:** 3 vagy később

**Miért.** Az app `portrait`. Tankra rakva a nagy számjegy fekvőben olvasható. A portrait HUD a haszon ~80%-át adja.

**Mit építs, ha jön.** Landscape activity vagy a fő képernyő lock-feloldása naplózáskor; óriás sebesség; térkép keskeny sáv; mindkét map motor. Figyelem: Mapsforge `MapView` + Compose rotáció.

**Függőség.** A sötét térkép (1.) kész legyen, különben kétszer rakod a HUD-ot éjjelre.

---

### 7. Track-kép / képeslap megosztás

**Érték:** közepes — social eye-catcher szerver nélkül  
**Effort:** 4–6 nap  
**Hullám:** 3+

**Miért.** Sötét alapon izzó vonal, táv, idő, GTL pecsét, PNG a share sheetre. A privacy megmarad (nincs feltöltés). Jó listing-screenshot forrás.

**Drága, mert:** statikus térkép-snapshot (Google Static / OSM render / saját polyline sötét háttéren). Az utolsó a legegyszerűbb és offline: nem csempe, csak vonal + statok. Kezdd azzal, ne Static Maps API-val.

**Függőség.** 3. (szín) és 4. (név/stat) a képeslapot tartalommal tölti.

---

### 8. Quick Settings tile (Indít / Leállít)

**Érték:** alacsony–közepes  
**Effort:** ~1 nap  
**Hullám:** 1 végétől bármikor, vagy 3

**Miért.** Shade-ből indítás kesztyűben. `TileService`, ugyanazok az engedélyek, mint az Indít gomb. Nem listing-téma.

**Függőség.** Nincs. Az értesítés Stop actionje már ad egy vezérlőt.

---

## Kiadási hullámok

A verziószámok **javaslatok**. A 2.0.6 patch maradhat hotfixnek; a következő minor a hullám 1 maradéka.

### Hullám 1 — „éjjel is látod” (kb. 3,5–5,5 nap)

Cél: éjjel nem vakít, az értesítésben ugyanazok a számok, a listing a valódi HUD-os térkép.

| # | Tétel | Effort |
| - | ----- | ------ |
| 1 | Sötét térkép + Rendszer/Világos/Sötét | 2–3 nap |
| 2 | Értesítés élő számokkal | 1–2 nap |
| — | Play screenshot + feature graphic frissítés a **valódi** HUD-os térképről | 0,5 nap |

GPS / Útvonal / Iránytű / Beállítások / Mentett trackek / Térkép S/E listing képek újra véve 2026-09-12 (1080×1920, alsó fülek megmaradnak). Hátravan: HUD-os Térkép naplózás közben, és a feature graphic.

**Kész, ha:** sötét módban a csempe sötét; az értesítésben van km/h és km; a listing új 9:16 képe a HUD-os Térkép, nem az idle S/E track.

### Hullám 2 — „az archívum és a vonal mesél” (kb. 7–10 nap)

| # | Tétel | Effort |
| - | ----- | ------ |
| 3 | Színezett polyline + Route nagy sebesség / sparkline | 4–6 nap |
| 4 | Mentett track kártyák + displayName | 3–4 nap |

**Kész, ha:** egy autópályás szakasz nem azonos színű, mint a város; két session névvel megkülönböztethető; a Térképen a színezés a session usage sávjait használja.

### Hullám 3 — mélyítés (később, darabolva)

| # | Tétel | Effort |
| - | ----- | ------ |
| 5 | Kézi szünet | 2–3 nap |
| 6 | Fekvő HUD | 5–8 nap |
| 7 | Képeslap PNG | 4–6 nap |
| 8 | Quick Settings tile | ~1 nap |

A 6. és 7. a drágák: csak akkor, ha a hullám 1–2 után még a „tankra raknám” / „megosztanám képként” a panasz.

---

## Összesítő tábla

| Rang | Feature | Érték | Effort | Hullám |
| ---- | ------- | ----- | ------ | ------ |
| 1 | Sötét térkép + téma-választó | magas | 2–3 nap | 1 |
| 2 | Élő értesítés | közepes–magas | 1–2 nap | 1 |
| 3 | Színezett track + Route cockpit | magas | 4–6 nap | 2 |
| 4 | Mentett track kártyák + név | közepes–magas | 3–4 nap | 2 |
| 5 | Kézi szünet / lap | közepes | 2–3 nap | 3 |
| 6 | Fekvő tank HUD | közepes | 5–8 nap | 3+ |
| 7 | Képeslap megosztás | közepes | 4–6 nap | 3+ |
| 8 | Quick Settings tile | alacsony–közepes | ~1 nap | bármikor |

Hullám 1 maradék: **kb. 3,5–5,5 nap** + listing asset.  
Hullám 2: **kb. 7–10 nap**.  
Ha csak **kettőt** lehet: **sötét térkép + színezett track**. Ez zárja a „éjjel vakít” és a „piros firka a listingen” rést.

---

## Dokumentáció és Play, minden hullám végén

Nem opcionális toldalék:

- [CHANGELOGS.md](../CHANGELOGS.md) EN + Magyar
- [docs/play-console/whatsnew.txt](play-console/whatsnew.txt) (500 karakter / nyelv)
- Súgó EN/HU a új vezérlőkre
- README funkciólista, ha a felhasználó látja
- Screenshot 1080×1920, 24 bit, nincs alfa; GPS skyplot, Útvonal magasságprofil, Iránytű, Beállítások QNH, Mentett trackek és Térkép S/E újra véve 2026-09-12. HUD-os Térkép (naplózás) és feature graphic a sötét csempe után.

A [GPSDATAFLOW](GPSDATAFLOW-hu.md) csak akkor változik, ha a lánc írása változik. Session név: [DBSTRUCT](DBSTRUCT-en.md) migráció 4→5.

---

## Nyitott döntések (implementáció előtt, hullámonként)

Hullám 1:

- Google night JSON: beépített stílus vagy saját, a cockpit teal/carmine köré?

Hullám 2:

- Sebességsávok globálisak (0–30 / 30–70 / 70+ km/h) vagy usage-enként (futó más skála)?

Ezeket a hullám briefjében rögzítsd; a roadmap szándékosan nem fagyasztja a pixel-layoutot.
