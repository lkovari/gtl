# GPS Track Logger

[English](README-en.md) · [Magyar](README-hu.md)

Helyben futó GPS útvonalnapló. Az útpontok SQLite-ban maradnak a telefonon. KMZ-t (KML + ikonok) oszthatsz meg Google Earth-tel vagy más térképalkalmazással. A mi szerverünkre semmi nem kerül fel.

A 2014-es Eclipse-app (`gtl-e`) Kotlin + Jetpack Compose újraírása. Alkalmazásazonosító: `com.lkovari.mobile.apps.gtl`.

**Verzió:** 2.0.4 (versionCode 22)  
**SDK:** minSdk 24 · targetSdk 36 · compileSdk 36  
**UI:** angol és magyar, Material 3, álló (portrait)

Adatvédelmi tájékoztató: [https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html](https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html)

---

## Funkciók

### Naplózás

- **Indít / Leállít** a munkamenetet látható előtér-szolgáltatásként rögzíti, értesítéssel.
- A fixek csak akkor tárolódnak, ha átmennek a pontossági és műholdszám-kapun. Opcionális **Kalman**-simítás utána elmozdítja a pontot. Az **Okos** vagy **Minden jó fix** sűrűség dönti el, hogy beíródik-e (lásd Beállítások). Futónál az alap: **Csak GNSS** (műholdchip, nem fused hely) simítás nélkül, hogy a kis úttest-alakzatok megmaradjanak a tracklogban. Teljes lánc: [Hogyan működik a naplózás](#hogyan-működik-a-naplózás).
- Eseménytípusok: `START`, `MOVE`, `PAUSE` (a usage pauza-sebesség alatt), `STOP`.
- Használati módok: repülő, hajó, autó, motor (alap), kerékpár, futó. A használat választása egy teljes előbeállítást ír (szűrők, csak GNSS, simítás, sűrűség, térkép-egyszerűsítés). A futó és a kerékpár lazább pontossági szűrőt és alacsonyabb pauza-küszöböt használ.
- Opcionális környezeti hőmérséklet (`TYPE_AMBIENT_TEMPERATURE`), gyorsulásmérő-minták és dőlésszög (gravitáció, tankra szerelve) minden letárolt ponton.



### GPS fül

- Élő műholdszámok: GPS L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC.
- SNR minőség (kiváló / jó / közepes / gyenge / nincs jel).
- Szélesség, hosszúság, pontosság, forrás, magasság, környezeti hőmérséklet, naplózási állapot.
- Ha a **Pontfelhő** be van: n, RMS, CEP95, medián jelentett pontosság, plusz álló / mozgás / várakozás felirat (ugyanaz a memóriabeli ablak, mint a térkép pöttyei; a CEP95-höz 8 minta kell).



### Útvonal fül

Az Indítás utáni összesítők: eltelt idő, út, mozgás ideje, várakozás ideje, sebesség, átlagsebesség, magasság, irány, dőlésszög (telefon síkban a motortankon), hőmérséklet-tartomány, ha van szenzor.

### Térkép fül

- A jelenlegi helyre centrál; naplózáskor követ. A **Teljes útvonal a képernyőn** minden GPS-frissítés után a teljes nyomvonalat a képernyőre illeszti (a nagyítás és mozgatás a következő fixig megengedett).
- Piros polyline a Room-ból (élő munkamenet, utoljára mentett track, vagy a Mentett útvonalakban választott track). A térképvonal **maga** a letárolt log; nincs külön vázlat. Lásd [Hogyan működik a naplózás](#hogyan-működik-a-naplózás).
- **Google Maps**, ha a `MAPS_API_KEY` be van állítva; különben a telefonon megjelenő üzenet.
- **OSM Mapsforge**, ha letöltöttél egy régiót, és bekapcsoltad a **Letöltött OSM térkép használata** kapcsolót. Ugyanaz a polyline és pontossági gyűrű rajzolódik az OSM-re.
- Világos lila pontossági kör (sugár = GPS pontosság méterben). Beállításokban kapcsolható. A kör a **nyers** helyet követi (GNSS chip vagy fused), nem a Kalman-simított letárolt tracket.
- Kis piros sziluett a helyeden (repülő, hajó, autó, motor, kerékpár, futó — ugyanaz, mint a Beállításokban). Álló portrén vízszintesen marad. Az északjelző mindig a térképen van.
- Ha mentett track látszik és nincs naplózás, a bal felső seprő leveszi a vonalat a térképről, a logot nem törli. Indítás vagy Mentett útvonalak → Térképen újra kirajzol.
- Douglas–Peucker egyszerűsítés a kirajzolt vonalon, ha az **Útvonal egyszerűsítése a térképen** be van (lásd lent). Az SQLite, az Útvonal összesítők és a KMZ soha nem egyszerűsödik.



### Iránytű fül

Mágneses irány és élő tárcsa a forgásérzékelőből. Indítás nélkül is működik.

### Mentett útvonalak

- Munkamenetek listája dátummal, használattal, mértékegységgel.
- **Térképen** a Térkép fület nyitja azon a munkameneten (Google Maps vagy OSM), a sessionben tárolt használati módot beírja a Beállításokba, és azzal rajzolja. Utána a usage vagy a csúszkák váltása más módban mutatja ugyanazt a logot. A következő Indít a kiválasztott Beállításokat követi. A seprő leveszi a vonalat, a munkamenetet nem törli.
- Törlés.
- Jelölőnégyzetek, **Összes kijelölése**, **Kijelöltek megosztása**:
  - egy munkamenet → egy KMZ, neve `GTL_yyyyMMdd_HHmmss.kmz`
  - több munkamenet → egy KMZ, trackenként egy mappával



### KMZ export

- Csomagolt play (indítás), pause és stop ikonok; a térképfeliratok rejtettek (`LabelStyle` scale 0).
- Minden letárolt GPS-pont egy `gx:Track`-en van (`when`, lon/lat/alt, speed).
- START / PAUSE / STOP balloonok (a Google Earth play, pause vagy stop ikonjára koppintva):
  - Mindhárom: `time=` (UTC), `usage=` (Aircraft, Watercraft, Car, Motorbike, Bicycle vagy Runner), `lat=`, `lon=`, `speed=`, `temp=`, és `lean=` ha volt dőlésszög.
  - Pause és stop `speed=0`-t kényszerít.
  - Stop-nál még: `Duration:` (`20 s`, ha 60 másodperc vagy kevesebb, egész perc 60 perc alatt, különben `HH:MM:SS`), `Avg. speed:` és `Max. speed:` egész számként a `TrackStatsCalculator`-ból (metrikus `km/h`, angolszász `mile/h`, ICAO `kt`).
- MIME `application/vnd.google-earth.kmz`. Nyisd meg Google Earth-tel (ha kell, telepítsd a Play Áruházból).
- A súgó **KMZ/KML megtekintése** felsorolja ezeket a balloon mezőket (EN/HU) és a SQLite `gps_events` mezőit.



### Beállítások

A **használat** választása egy DataStore-szerkesztésben felülírja a kapcsolódó alapértékeket. Utána bármelyik vezérlő külön is állítható.


| Használat        | Mértékegység | Csak GNSS | Rögzített útvonal simítása | Erősség | Álláskor ne vándoroljon | Sűrűség    | Egyszerűsítés a térképen | Tűrés |
| ---------------- | ------------ | --------- | -------------------------- | ------- | ----------------------- | ---------- | ------------------------ | ----- |
| Futó             | Metrikus     | be        | ki                         | Alacsony | be                      | Minden jó  | ki                       | 2 m   |
| Kerékpár         | Metrikus     | be        | ki                         | Alacsony | be                      | Minden jó  | ki                       | 3 m   |
| Motor (alap)     | Metrikus     | ki        | be                         | Közepes | be                      | Okos       | be                       | 6 m   |
| Autó             | Metrikus     | ki        | be                         | Közepes | be                      | Okos       | be                       | 8 m   |
| Hajó             | ICAO         | ki        | be                         | Közepes | be                      | Okos       | be                       | 8 m   |
| Repülő           | ICAO         | ki        | be                         | Magas   | be                      | Okos       | be                       | 15 m  |


**Mit csinál az egyes vezérlő**

- **Használat** — tevékenység típusa. Újratölti a fenti táblát és a 2017-es pontossági / műhold kapukat (futó és kerékpár 45 m, többiek 30 m). Repülőnél és hajónál a mértékegység ICAO-ra vált; a többi használat metrikusra.
- **Mértékegység** — metrikus, angolszász vagy ICAO az Útvonalon (km/h és méter; mph és láb/mérföld; csomó, tengeri mérföld és láb). A letárolt koordinátákat nem mozgatja.
- **Letöltött OSM térkép használata** — Mapsforge fájl a Google Maps helyett.
- **Útvonal egyszerűsítése a térképen** — kevesebb csúcs csak a Térképen. A kapcsoló bekapcsolva **1–20 m** csúszka (1 m-es lépés). A KMZ és az odométer minden letárolt pontot megtart.
- **Utolsó naplózott útvonal a térképen** — Leállítás után az utolsó (vagy kijelölt) track a Térképen marad. A seprő leveszi a kirajzolt mentett tracket, a logot nem törli.
- **Teljes útvonal a képernyőn** — naplózáskor minden GPS-frissítés a teljes nyomvonalat a képernyőre illeszti. A nagyítás és mozgatás a következő fixig megengedett.
- **Pontossági jelzés megjelenítése** — világos lila kör; a sugár a GPS pontossága. A HUD a nyers helyen marad (chip vagy fused).
- **Pontfelhő** — pasztell magenta pöttyök a nyers GPS-fixekből, amíg állsz, plusz magenta CEP95-kör a felhő centroidján. Alapból ki. Bekapcsoláskor a pontossági jelzés is bekapcsol; kikapcsoláskor csak a felhő tűnik el. Mozgás közben szünetel. Nem íródik a naplóba és a KMZ-be.
- **Csak GNSS** — műholdchip-pozíciók fused hely helyett. Futónál és kerékpárnál be; járműveknél ki.
- **Rögzített útvonal simítása**, **Simítás erőssége**, **Álláskor ne vándoroljon a pont**, **Rögzítés sűrűsége** — ezek azt változtatják, ami **a tracklogba íródik**. Részletek lent.

A meglévő telepítések, amelyeknél még a régi **19,5 m** egyszerűsítési alap van, a usage táblára migrálnak, amikor a Kalman-kulcsok először íródnak. A 19,5-től eltérő egyedi tűrés megmarad.

### További képernyők

- Első indításkori biztonságos vezetés nyilatkozat.
- OSM térkép letöltése (Mapsforge v5 régiók: Európa, válogatott Ázsia / Amerika / Ausztrália).
- Helymeghatározás beállításai (megnyitja a rendszer GPS-panelét).
- Súgó: harmonika (egyszerre egy szakasz nyitva). Használat, **Beállítások** (előbeállítások és minden vezérlő), Útvonalnaplózás (Kalman vs Douglas–Peucker vs sűrűség), GPS, Útvonal, Térkép, Iránytű, KMZ/KML megtekintése, adatvédelmi tájékoztató, letárolt trackpont mezőtábla. Angol és magyar.
- Adatvédelmi tájékoztató hivatkozás.

---



## Architektúra

Két Gradle-modul:


| Modul     | Szerep                                                                                                                                                                              |
| --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `:engine` | Tiszta JVM: GNSS-osztályozás, Kalman trackszűrő, fix-elfogadás, sebességadaptív térköz, Douglas–Peucker, trackstatisztika, KML/KMZ, térkép-láthatósági szabályok, pontfelhő-buffer. A JUnit tesztek itt vannak. |
| `:app`    | Android: Compose UI, Room, DataStore, hely/GNSS/szenzorok, előtér-szolgáltatás, Google Maps, Mapsforge, WorkManager OSM-letöltés, FileProvider megosztás.                            |


```
app/     Compose, Room, szolgáltatások, térképek
engine/  Domain-algoritmusok (nincs Android SDK)
docs/    Adatvédelmi tájékoztató, Play-anyagok, renewal jegyzetek
```



### Adatok

- **Room:** `track_sessions` + `gps_events` (kaszkád törlés). A Map polyline mindig a Room-ból olvasódik, nem memóriabeli vázlatból. Ezért a látott vonal az a log, amit eltároltál.
- **DataStore:** nyilatkozat, használat, mértékegység, szűrők, OSM-fájlútvonal, térképbeállítások, Kalman / sűrűség / csak GNSS / térkép-egyszerűsítés / pontfelhő.
- **Fájlok:** OSM `.map` letöltések; KMZ a `files/gtltracklogs/` alatt (FileProvider).
- **RemoteTrackSync:** no-op csonk egy későbbi backendhez. Nincs élő helyfeltöltés.



### Hogyan működik a naplózás

A Térképen a piros vonal a letárolt tracklog, nem egy második vázlat. A `GtlViewModel` a Room `gps_events` tábláját figyeli, és ezeket a koordinátákat rajzolja (Google Maps polyline vagy Mapsforge overlay). Az Útvonal összesítők és a megosztott KMZ ugyanazokat a sorokat olvassa. Ha a térképvonal úgy néz ki, ahogy mentél, az azért van, mert a naplózó ezeket a pontokat írta — nem azért, mert a térkép utcára pattintotta.

```
Indít
  → előtér-szolgáltatás (látható helymeghatározási értesítés)
  → helyfrissítések (GNSS chip vagy fused)
  → a HUD mindig a nyers fixet kapja (világos lila pontossági kör)
  → opcionális Pontfelhő (csak memória: pasztell magenta pöttyök + CEP95 állva)
  → rossz pontosság / túl kevés műhold eldobása
  → opcionális Kalman (mozgatja a lat/lon-t; nem dobja el a pontot)
  → sűrűségkapu (Okos / Minden jó / keverék) — ez ír vagy kihagy
  → SQLite gps_events (START / MOVE / PAUSE)
Leállít
  → STOP placemark (utolsó Kalman-pont, ha a simítás be van, különben az utolsó nyers fix)
  → Térkép / Útvonal / KMZ mind a Room-ot olvassa
```

**Két helyfolyam.** Amíg az app nyitva van, a `GtlViewModel` kb. másodpercenként figyel, hogy a GPS és Térkép HUD az Indítás előtt is frissüljön. Indítás után csak a `TrackingForegroundService` ír. Legalább 500 ms-enként kér frissítést (`minTimeMillis`, minimális távolság `0`). A térközt később a `FixAcceptance` alkalmazza, nem az Android.

**Forrás.** **Csak GNSS** be → Android `GPS_PROVIDER` (a műholdchip: GPS, Galileo, GLONASS, BeiDou, QZSS, NavIC — a szolgáltató neve történeti). Ki → Play Services fused `PRIORITY_HIGH_ACCURACY` (műholdak Wi-Fi-vel, cellával és IMU-val keverve). Ha a GPS-szolgáltató ki van kapcsolva, fused megy mindkét esetben. Futónál az alap a csak GNSS, hogy egy 5–10 m-es úttest-hurkot ne lapítson el a telefon „hol van a felhasználó?” szűrője, mielőtt a GTL egyáltalán látná.

**HUD vs letárolt track.** Minden frissítés a **nyers** `Location`-t másolja a `lastLocation`-be. A világos lila pontossági kör, az élő szélesség/hosszúság, a forrás és a pontosság ez a nyers fix. A **Pontfelhő** ugyanezt a `lastLocation`-t mintavételezi egy memóriabeli ablakba (centroid RMS / CEP95), és nem ír SQLite-ot. Bekapcsoláskor a pontossági jelzés is bekapcsol; kikapcsoláskor csak a felhő tűnik el. A piros polyline az, ami **a Roomba bekerült** (Kalman-simítva, ha az a kapcsoló be van). Szándékosan lehetnek pár méterre egymástól.

**1. kapu — pontosság és műholdak.** A usage pontosságánál rosszabb (30 m, futónál és kerékpárnál 45 m) vagy 4-nél kevesebb műholdas fix eldobódik. Nem megy Kalmanba, és nem lesz sor. A HUD ettől még frissül.

**2. kapu — opcionális Kalman.** Ha a **Rögzített útvonal simítása** be van, a `KalmanTrackFilter.observe` minden pontosságon átment fixen lefut (egy szűrőpéldány Indít→Leállít munkamenetenként; folytatásnál az utolsó letárolt pontból magoz). Új lat/lon-t ad. Az időbélyeg, magasság, pontosság és műholdszám a GPS-fixé marad. A sebesség és az irányszög a szűrő sebességéből jön, ha az legalább 0,3 m/s; különben a GPS-fixé (vagy az utolsó irányszög). A Kalman **mozgatja** a pontokat, és **ugyanannyi jelöltet tart**. Nem map-matching, és nem dob el csúcsokat. Járműveknél ez az alap, hogy a körforgalom kerek legyen, a cruise tiszta vonal. Futónál **ki**, hogy egy kis nyolcas ne számítson mérési zajnak.

**3. kapu — rögzítés sűrűsége.** A `FixAcceptance.shouldAccept` dönti el, hogy az (esetleg simított) pont SQLite-sor legyen-e. A Kalman akkor is frissül, ha a pont kimarad.

- A munkamenet első fixe → mindig `START`-ként tárolódik.
- **Okos** (járművek): ír, ha a haversine-távolság az utolsó **eltárolt** ponttól eléri a 2014-es sebességsávot; kanyarban annak a fele (irányszög-változás > 15°). Futó Okos ezt a sávot újra felezi (min. 1 m).
- **Minden jó** (futó alap): ír, ha eltelt a `minTimeMillis` (500 ms) **vagy** az irányszög kanyarban van, és a távolság legalább **1 m** (jármű) vagy **0,5 m** (futó és kerékpár).
- A csúszka köztes állásai az Okos térközt keverik a Minden-jó padlóval; a min-idő / kanyar út akkor is elfogadhat egy pontot.
- Ha a GPS `bearing` 0 (kocogáskor gyakori), a kanyardetekció a szomszédos pozíciókból számolt irányszöget is használhatja.

**Eseménytípus.** Írás után: `START` az első ponton; `PAUSE`, ha a sebesség a usage pauza-küszöb alatt van (0,25 m/s futó, 0,4 m/s járművek); különben `MOVE`. Opcionális környezeti hőmérséklet, utolsó gyorsulásmérő XYZ és dőlésszög (gravitáció, tankra szerelve) a sorra másolódik. Az iránytű azimutja csak HUD, nem tárolódik.

**Leállítás.** Mindig ír egy `STOP` placemarkot (`isPlacemark` true), még ha a sűrűség eldobná is a pontot. Simítás bekapcsolva az a sor az utolsó Kalman-kimenetet használja, hogy a track vége a simított vonallal egyezzen.

**Térképrajzolás.** A `GtlViewModel` a Room-sorokat `displayPoints`-re képezi. A `MapTrackVisibility` akkor mutatja a vonalat, ha naplózás megy, ha az **Utolsó naplózott útvonal a térképen** be van, vagy ha Mentett útvonalak-munkamenet van kiválasztva — hacsak a seprő `mapCleared`-et nem állított (csak idle; naplózáskor akkor is rajzol). Ha az **Útvonal egyszerűsítése a térképen** be van, és több mint 4 pont van, a Douglas–Peucker **csak ezeket a megjelenítési csúcsokat** ritkítja az 1–20 m csúszkán. Az SQLite, az Útvonal-odométer és a KMZ soha nem megy DP-n. Egyszerűsítés **ki** (futó és kerékpár alap) esetén minden letárolt csúcs a térképen van — ezért marad látható egy kis úttest-hurok.

**Miért néz ki a térkép a logodnak**


| Réteg                | Mit csinál                    | Hatás a térképen                                                                              |
| -------------------- | ----------------------------- | --------------------------------------------------------------------------------------------- |
| GNSS chip vs fused   | Ki válaszol a „hol vagyok?”-ra | Futó és kerékpár: chip-track, utcai léptékű alak megmarad. Járművek: fused, kevesebb Wi-Fi/cella-ugrás.    |
| Pontosság / műhold   | Szemét eldobása Kalman előtt  | Nincs 200 m-es teleport-tüske a vonalon.                                                      |
| Kalman (opcionális)  | Pontok mozgatása, darabszám megmarad | Jármű-körforgalom és cruise sima; álló zár megállítja a 10 m-es firkát.                  |
| Sűrűség              | Hány pont tárolódik           | Okos: autópályán kevesebb pont. Minden jó: ~2 Hz, szűk hurkok megtartják a csúcsokat.         |
| Room                 | Egyetlen igazságforrás        | Térkép, Útvonal és KMZ ugyanaz az út.                                                         |
| Douglas–Peucker      | Csak megjelenítés ritkítása   | Hosszú járműtrack olcsón rajzolható; futónál ki, hogy a térkép = SQLite.                      |


Semmi nem kerül fel. A Stop utáni `RemoteTrackSync` no-op.

Pipeline mermaid (ugyanaz a folyamat, több dobozzal): [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md) / [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md).

### Hogyan naplóz a Futó, mint egy sportóra

Egy dedikált óra, például a Suunto Ambit 3 Peak, a **GNSS chipet** kb. másodpercenként rögzíti. Nem pattintja a vonalat utcára, és nem futtat telefonos „fused” szűrőt. A **GNSS** a műholdrendszerek családja (GPS, Galileo, GLONASS, BeiDou, QZSS, NavIC). A GPS egy konstelláció; a chip mindet használja.

A telefon **fused** hely API-ja más kérdésre válaszol: „hol van a felhasználó?” Műholdakat kever Wi-Fi-vel, cellával és IMU-val, majd simít. Egy 5–10 m-es hurok, amit tényleg az úton futottál, gyalogos zajnak tűnik, és **mielőtt** a GTL tárolná, ellapul.

A **Csak GNSS** az Android `GPS_PROVIDER`-t kéri (a chip, minden konstelláció — a név történeti). A letárolt pontok ezek a chip-pozíciók, ugyanaz az elv, mint az óra 1 s-os GPS-trackje. (A Suunto FusedSpeed tempó, nem a polyline. Ez az app nem valósít meg FusedTrack IMU-hézagkitöltést.)

A Futó előbeállítás többi része ezt az alakot tartja meg SQLite-ban, a Térképen és a KMZ-ben:

- **Rögzített útvonal simítása ki** — nincs második, állandó sebességű Kalman, ami a kis kört mérési zajnak venné. A Kalman vissza is kapcsolható; Futónál akkor extra folyamat-zaj járul hozzá, hogy egy 5 m-es hurok ne húzódjon az utcára.
- **Minden jó** — tárolás kb. 500 ms-enként (beállítás min. idő), vagy kanyarban hamarabb. Ha a GPS irányszög 0 (kocogáskor gyakori), az irány jöhet a szomszédos pozíciókból.
- **0,5 m** ismétlődés-eldobás (járműveknél 1 m marad), hogy a szűk hurok megtartsa a csúcsokat.
- **Útvonal egyszerűsítése a térképen ki** — a kirajzolt vonal minden letárolt pont.

A pontossági és műhold kapuk továbbra is eldobják a rossz fixeket. Ez **nem** kevesebb műholdas fixet tárol; a jókból többet tárol, és leállítja a Wi-Fi/cella/fused találgatást track-forrásként.

### Kalman-szűrő (hogyan simulnak a letárolt pontok)

**Cél.** GPS-jitter vágása autós vagy repülős tracken (körforgalom kerek, cruise tiszta vonal) anélkül, hogy a futó nyolcasa ellapulna, és anélkül, hogy állás közben 10 m-es firka keletkezne. A Kalman **zajszűrő**: az elfogadott pontokat **mozgatja**, és **ugyanannyit tart**. Nem map-matching (nincs OSM/Google utcára pattintás), és nem Douglas–Peucker (a DP **eldob** csúcsokat, és csak a Térkép fülön).

**Hol ül a láncban.** Egy `KalmanTrackFilter` Indít→Leállít munkamenetenként, a `:engine`-ben. A `TrackingForegroundService` minden helyfrissítésre ezt csinálja:

1. A fixet a HUD-ra másolja (`lastLocation`). A világos lila pontossági kör mindig ezt a **nyers** pontot követi.
2. Eldobja a fixet, ha a pontosság a usage kapunál rosszabb (30 m, futónál és kerékpárnál 45 m), vagy a fixben lévő műholdak száma 4 alatt van. Az elutasított fixek nem jutnak Kalmanba és SQLite-ba.
3. Ha a **Rögzített útvonal simítása** be van, lefut a `KalmanTrackFilter.observe`. A szűrő új lat/lon-t ad. Az időbélyeg, magasság, pontosság és műholdszám a GPS-fixé marad. A sebesség és az irányszög a szűrő sebességéből jön, ha az legalább 0,3 m/s. Futó/gyalogos extra helyzet-folyamat-zajt kap, hogy egy 5 m-es hurok ne húzódjon a húrra.
4. A **Rögzítés sűrűsége** (`FixAcceptance`) dönti el, hogy ezt az (esetleg simított) pontot **beírja-e**. Ha a hézag túl kicsi, a Kalman-állapot ettől még frissül, de a Room nem kap sort.
5. Leállításkor az utolsó Kalman-kimenet a STOP pont, ha a simítás be van.

Tehát a Kalman azt változtatja, **hol** ülnek a letárolt pontok. A sűrűség azt, **hány** van belőlük. A térkép-egyszerűsítés **egyiket sem** — csak a Térképen kirajzolt polyline-t ritkítja.

**Hogyan működik a szűrő.** Állandó sebességű modell helyi méterben (`GeoProjection`, ugyanaz a `111_320` m/fok, mint a DP). Az állapot `[east, north, vEast, vNorth]`. A GPS-mérés **csak helyzet** (nincs sebesség/irányszög-update). Minden lépés:

1. **Predikció** — az állapotot `dt`-vel előrelépteti (kis tartományra szorítva, hogy a GNSS-szünet ne robbanja a kovarianciát).
2. **Folyamat-zaj** `q` (m²/s⁴) = `baseQ(usage) × strengthMultiplier(slider)`. Majd `× turnBoost(usage)`, ha az irányszög az előző **kimeneti** irányszöghöz képest (vagy a szomszédos pozíciókból számolt irány, ha a GPS irányszög 0) több mint 15°-ot változik. **Magas** `q` **= jobban bízik a GPS-ben = kevesebb simítás.** Alacsony `q` = jobban bízik a mozgásmodellben = simább ívek, több késés, amikor tényleg fordulsz.
3. **Update** — Joseph-formájú Kalman-update, mérési σ = max(GPS pontosság, 2 m).
4. **Ugrás** — ha az innováció nagyobb, mint `max(50 m, 8 × pontosság)` (alagút-kijárat, GPS-teleport), újrainicializál az új fixre. A hézagot **nem** interpolálja.
5. **Álló zár** (ha be van) — ha a GPS-sebesség vagy a prediktált sebesség a usage pauza-küszöb alatt van (0,25 m/s futó, 0,4 m/s járművek), és az elmozdulás 1,5 m alatt, befagyasztja az utolsó kimenetet, nullázza a sebességet, zsugorítja a helyzet-kovarianciát.

Alap `q` a csúszka közepén (régi Közepes): futó 8,0, kerékpár 6,0, motor 2,5, autó/hajó 1,5, repülő 0,8. Fordulási boost: futó 10, kerékpár 8, motor 5, autó/víz 3, repülő 2. Erősségcsúszka `t` a `[0, 1]`-ben (Alacsony→Magas) a `q`-t `4^(1 − 2t)`-vel szorozza: Alacsony ×4, közép ×1, Magas ×0,25.

**Szándékosan nincs implementálva.** OSM/Google utcára pattintás, RTS előre–hátra simító, IMU holtpontszámítás / Suunto FusedTrack hézagkitöltés, megjelenítési spline-ok.

### A beállítások hatása a tracklogra

Ezek a vezérlők változtatják a SQLite `gps_events` táblát, az Útvonal odométert / sebességeket és a megosztott KMZ-t. Minden más csak megjelenítés.


| Beállítás                                                              | Beíródik a tracklogba?          | Hatás                                                                                                                                                                                                                                                                                                                                                                                                |
| ---------------------------------------------------------------------- | ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Csak GNSS**                                                          | Igen (forrás)                   | **Be:** `GPS_PROVIDER` műholdchip-pozíciók (minden GNSS-konstelláció). **Ki:** Play Services fused HIGH_ACCURACY. Ha a GPS-szolgáltató ki van kapcsolva, fused megy mindkét esetben.                                                                                                                                                                                                                 |
| **Rögzített útvonal simítása**                                         | Igen                            | **Be:** minden jelölt pont Kalman-simított a sűrűség előtt. Útvonal, Térkép (nyers polyline) és KMZ a simított utat mutatja. **Ki:** a helyforrás a pontosság/műhold kapu után változatlanul tárolódik (futó alap).                                                                                                                                                                                  |
| **Simítás erőssége** (Alacsony–Magas csúszka; csak ha a simítás be van) | Igen                            | **Alacsony:** a GPS-jitter megmarad, a nyolcas és a zigzag is. **Magas** (repülő alap): körforgalom és cruise tisztább; a hajtű kicsit késik. Közép a motor/autó/víz.                                                                                                                                                                                                                                |
| **Álláskor ne vándoroljon a pont**                                     | Igen (csak ha a simítás be van) | Pauza-sebesség alatt a letárolt koordináta nem kóborol. Egy 6 m-es GPS-csoport pirosnál egy pont felé omlik. Magában nem dob el sorokat — a sűrűség dönti el az írást.                                                                                                                                                                                                                                |
| **Rögzítés sűrűsége** (Okos–Minden jó csúszka)                         | Igen                            | **Okos:** ír, ha a távolság az utolsó **eltárolt** ponttól eléri a 2014-es sebességsávot (kanyarban fele; futó Okos újra fele, min. 1 m). Autópályán kevesebb pont; gyalogláskor több. **Minden jó:** ír kb. `minTime`-onként (500 ms), vagy kanyarban hamarabb. Járműveknél az **1 m**-nél közelebbi halmok hullanak; futónál **0,5 m**. A csúszka közepe a két szabályt keveri.                    |
| **Útvonal egyszerűsítése a térképen** (1–20 m csúszka)                 | **Nem**                         | Kevesebb csúcs csak a Térkép fülön. A letárolt pontok, az odométer és a KMZ változatlan.                                                                                                                                                                                                                                                                                                             |
| **Pontossági jelzés megjelenítése**                                    | **Nem**                         | Világos lila kör a **nyers** GPS-fixen, akkor is, ha a Kalman be van.                                                                                                                                                                                                                                                                                                                                     |
| **Pontfelhő**                                                          | **Nem**                         | Pasztell magenta pöttyök a nyers HUD-fixekből állva, CEP95 a centroid körül. Alapból ki. Bekapcsoláskor a pontossági jelzés is bekapcsol; kikapcsoláskor csak a felhő tűnik el. Mozgás közben szünetel. Nem tárolódik.                                                                                                                                                                          |
| **Mértékegység**                                                       | Csak címkék                     | Metrikus / angolszász / ICAO formázza az Útvonalat és a KMZ balloonokat. A koordináták WGS-84 maradnak. A repülő és hajó előbeállítás ICAO-t választ.                                                                                                                                                                                                                                                |
| Pontossági / műhold kapuk                                              | Igen (elutasítás)               | A 30 m-nél (futónál és kerékpárnál 45 m) rosszabb, vagy 4-nél kevesebb műholdas fix Kalman előtt eldobódik. Nincs Settings-csúszkaként megjelenítve.                                                                                                                                                                                                                                                                |


**Gyakorlati eredmény.** Motor alap: fused + simított utcai track, Okos térköz, térképvonal 6 m-en ritkítva. Futó és kerékpár alap: GNSS chip, nincs Kalman, majdnem minden jó fix tárolódik (0,5 m padló), a Térkép minden letárolt csúcsot mutat, hogy a kis úttest-hurok látható maradjon. Repülő alap: erősebb simítás, Okos térköz, 15 m térkép-ritkítás, sebesség/távolság csomóban és tengeri mérföldben.

### Rögzítés sűrűsége

Független a Kalmantól. A Kalman simítás bekapcsolva minden pontosságon átment fixet lát; a sűrűség csak a **tárolást** kapuzza.

- **Okos** (járművek): pontot ír, ha a haversine-távolság az utolsó **eltárolt** fixtől eléri a 2014-es sebességsávot; kanyarban annak a fele. Futó Okos ezt a sávot újra felezi (a gyaloglás/kocogás túl durva volt egy kis nyolcashoz).
- **Minden jó** (futó és kerékpár alap): elfogad, ha eltelt a `minTimeMillis`, vagy az irányszög kanyarban van. Járműveknél az 1 m-nél közelebbi halmok hullanak; futónál és kerékpárnál 0,5 m.
- **A csúszka közepe:** a szükséges távolság az Okos sáv és a Minden-jó padló keveréke (1 m jármű, 0,5 m futó és kerékpár); a min-idő / kanyar út is elfogadhat egy pontot.



### Douglas–Peucker (térkép-egyszerűsítés)

**Cél.** Kevesebb csúcsot kelljen a Térkép fülnek rajzolnia. Egy hosszú munkamenetben több ezer letárolt fix lehet; ezek nagy része majdnem egyenesre esik. A köztesek eldobása reszponzívvá tartja a térképet anélkül, hogy a felvétel megváltozna.

Ez **csak megjelenítés**. A `gps_events`, az Útvonal odométer / sebességek és a KMZ export mindig a Room-sorokat használja (már Kalman-simítva, ha az a beállítás be van). A Douglas–Peucker nem simítja a GPS-zajt: a megmaradó sarkok élesek maradnak. Csak azokat a pontokat dobja el, amelyek elég közel vannak egy húrhoz.

**Mikor fut.** Beállítások → **Útvonal egyszerűsítése a térképen** (`optimizationActive`; járműveknél be, futónál és kerékpárnál ki). Naplózáskor a `GtlViewModel` ezt a kapcsolót használja. A **Térképen** először a session usage-ét írja a Beállításokba, utána a kirajzolt vonal a jelenlegi csúszkákat követi, ezért a usage váltása más módban mutatja ugyanazt a logot. A tűrés **1–20 m** csúszka (1 m-es lépés; motor alap 6 m, autó 8 m). A `DouglasPeucker.clampTolerance` íráskor továbbra is pattint és szorít.

**Hogyan működik.** Klasszikus Ramer–Douglas–Peucker, távolságok méterben helyi érintősíkon (`111_320` m szélességi fokonként; a hosszúság `cos(lat)`-tal skálázva):

1. A aktuális szakasz első és utolsó pontja mindig megmarad.
2. Minden köztes pontra mérjük a merőleges távolságot a kezdet–vég egyeneshez (húr).
3. Vesszük a legtávolabbit. Ha ez a távolság **nagyobb, mint** a tűrés, megtartjuk — ez valódi kanyar —, és rekurzió a két alszakaszon (kezdet→legtávolabbi, legtávolabbi→vég).
4. Ha a legtávolabbi pont **a tűrésen belül** van, minden köztes pontot eldobunk: mind elég közel van a húrhoz.

Így egy majdnem kollineáris szakasz két végpontra omlik, míg egy küszöbön túl kilógó sarok megmarad. Egy oda-vissza úthurok (~6–8 m széles) alacsony küszöböt igényel (kb. 2–8 m), különben magas tűrésnél egyetlen vonallá omlik. Implementáció: `engine/.../DouglasPeucker.kt`. Pipeline-kontextus: [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md) / [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md).

### Engedélyek

`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE` / `_LOCATION`, `POST_NOTIFICATIONS`, `INTERNET` / `ACCESS_NETWORK_STATE` (térképek + OSM-letöltés). GPS-hardver kötelező; iránytű és környezeti hőmérséklet opcionális. Nincs `ACCESS_BACKGROUND_LOCATION`, nincs telefonállapot / IMEI.

---



## Beüzemelés

1. Nyisd meg ezt a mappát Android Studio-ban (JDK 11 toolchain).
2. Másold a gitignored `keystore.properties`-t (ugyanaz az EKL release keystore, mint a sensors-s-nél).
3. Adj Maps SDK kulcsot a `local.properties`-hez:

```
sdk.dir=/path/to/Android/sdk
MAPS_API_KEY=your_key_here
```

A kulcsot korlátozd a `com.lkovari.mobile.apps.gtl` csomagra és az EKL keystore SHA-1-re. Amíg a kulcs nincs beállítva, a Térkép fül letöltött OSM-régióval akkor is működik.

### Fordítás

```bash
./gradlew :engine:test
./gradlew assembleDebug
./gradlew assembleRelease    # keystore.properties kell
./gradlew bundleRelease      # aláírt AAB a Playhez
```

Release APK: `app/build/outputs/apk/release/app-release.apk`  
Release AAB: `app/build/outputs/bundle/release/app-release.aab` (Play App Signing; upload key = EKL release keystore)

### Stack

Kotlin 2.2 · AGP 9.2 · Compose BOM 2025.12 · Room 2.7 · DataStore · Navigation Compose · Play Services Location / Maps · Maps Compose · Mapsforge 0.25 · WorkManager · KSP

---



## Technikai dokumentumok


| Dokumentum                                                                     | Mi ez                                                                                                               |
| ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------- |
| [CHANGELOGS.md](CHANGELOGS.md)                                                 | Kanonikus verzióelőzmény (2.0.0 újraírás → Unreleased, angol és magyar)                                            |
| [docs/play-console/whatsnew.txt](docs/play-console/whatsnew.txt)               | Play Console kiadásnév és EN/HU what’s-new szöveg                                                                   |
| [docs/RENEWAL-REPORT.md](docs/RENEWAL-REPORT.md)                               | Újraírási jelentés: mi készült újra, mi esett ki Play-szabály miatt, follow-up-ok                                  |
| [docs/play-console/privacy-policy.html](docs/play-console/privacy-policy.html) | Adatvédelmi tájékoztató (az élő KLHome-oldal helyi másolata)                                                        |
| [docs/play-console/feature-graphic.png](docs/play-console/feature-graphic.png) | Play Áruház feature graphic                                                                                         |
| [docs/screenshots/](docs/screenshots/)                                         | Play listing képernyőképek (GPS, útvonal, térkép/tracking, iránytű, beállítások, mentett útvonalak, súgó, névjegy, Google Earth KMZ) |
| [docs/DBSTRUCT-en.md](docs/DBSTRUCT-en.md)                                     | SQLite séma (`gtl.db`) mermaid                                                                                      |
| [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md)                               | GPS figyelés → szűrés → Room → UI / KMZ (EN); a naplózási lánc mermaidje                                           |
| [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md)                               | GPS figyelés → szűrés → Room → UI / KMZ (HU)                                                                        |
| [docs/dp-kalman-smoothing-en.md](docs/dp-kalman-smoothing-en.md)               | Eredeti Kalman implementációs brief; **as-built jegyzetek felül** (a jelenlegi viselkedés ez a README)              |


Érdemes elolvasni ezeket az engine belépési pontokat:

- `engine/.../FixAcceptance.kt` — pontosság / műhold / Okos vagy Minden jó sűrűség
- `engine/.../KalmanTrackFilter.kt` — állandó sebességű simító (letárolt pontok)
- `engine/.../UsageSmoothingDefaults.kt` — usage előbeállítás (csak GNSS, Kalman, sűrűség, térkép-egyszerűsítés)
- `engine/.../SpeedAdaptiveSpacing.kt` — méter a pontok között km/h és kanyar szerint
- `engine/.../DouglasPeucker.kt` — csak térképes polyline-egyszerűsítés (méter, helyi vetület)
- `engine/.../TrackStats.kt` — odométer, mozgás vs várakozás
- `engine/.../KmlExporter.kt` + `KmzExporter.kt` — KMZ helyi ikonokkal
- `engine/.../Gnss.kt` — konstelláció / L1 vs L5 / SNR
- `engine/.../FixCloud.kt` — memóriabeli állóhelyi pontfelhő / CEP95
- `engine/.../MapDisplayUsage.kt` — melyik usage és egyszerűsítés szerint rajzol a térkép
- `engine/.../MapTrackVisibility.kt` — mikor kell a térképnek tracket rajzolnia (naplózáskor mindig; különben last-track vagy kijelölt session, hacsak nem ürítették)
- `app/.../LocationClient.kt` — fused HIGH_ACCURACY vagy `GPS_PROVIDER`, ha a Csak GNSS be van

---



## Play listing képernyőképek

`docs/screenshots/`

- `gps-idle.png`, `gps-logging.png` — GPS fül
- `route.png` — Útvonal összesítők
- `map.png`, `tracking.png` — Térkép felvétel közben
- `googleearth.png` — megosztott KMZ a Google Earth-ben
- `compass.png`, `about.png`
- `settings.png` — Beállítások Futóval, Csak GNSS-szel, rögzítés sűrűsége Minden jónál
- `saved-tracks.png` — Mentett útvonalak (kijelölés, megosztás, térképen)
- `settings-density.png` — Régebbi Beállítások elrendezés egyszerűsítő / simító csúszkákkal (2.0.3)
- `help.png` — Súgótémák (2.0.3)
- `app-icon.png`

Telefon listing méret: 1080×1920, 24 bites PNG, nincs alfa (Play 9:16). Ezzel a kiadással töltsd fel a `settings.png`, `saved-tracks.png` és `help.png` fájlokat.

---



## Következő teendők

- GPX export megvalósítása. A GPX (GPS Exchange Format) a legelterjedtebb GPS tracklog-csereformátum.
- Világos és sötét téma

---



## Ami nincs ebben az appban

Szándékosan nem került át 2014-ből (szabály vagy halott API): IMEI / `READ_PHONE_STATE`, élő lat/lng feltöltés, follow-me weboldal, távoli feloldás, Google Directions, app által kapcsolt GPS/Wi-Fi, boot auto-start. Lásd [docs/RENEWAL-REPORT.md](docs/RENEWAL-REPORT.md).
