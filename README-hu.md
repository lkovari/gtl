# GPS Track Logger

[English](README-en.md) · [Magyar](README-hu.md)

Helyben futó GPS útvonalnapló. Az útpontok SQLite-ban maradnak a telefonon. KMZ-t (KML + ikonok) oszthatsz meg Google Earth-tel, vagy GPX 1.1 fájlt OsmAnd, Komoot, Garmin Connect, QGIS és más appokkal. A mi szerverünkre semmi nem kerül fel.

A 2014-es Eclipse-app (`gtl-e`) Kotlin + Jetpack Compose újraírása. Alkalmazásazonosító: `com.lkovari.mobile.apps.gtl`.

**Verzió:** 2.0.15 (versionCode 33)  
**SDK:** minSdk 24 · targetSdk 36 · compileSdk 36  
**UI:** angol és magyar, Material 3, álló (portrait)

Adatvédelmi tájékoztató: [https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html](https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html)

---

## Tartalomjegyzék

- [Funkciók](#funkciók)
  - [Naplózás](#naplózás)
  - [GPS fül](#gps-fül)
  - [Útvonal fül](#útvonal-fül)
  - [Térkép fül](#térkép-fül)
  - [Iránytű fül](#iránytű-fül)
  - [Mentett útvonalak](#mentett-útvonalak)
  - [KMZ export](#kmz-export)
  - [GPX export](#gpx-export)
  - [Beállítások](#beállítások)
  - [További képernyők](#további-képernyők)
- [Architektúra](#architektúra)
  - [Adatok](#adatok)
  - [Hogyan működik a naplózás](#hogyan-működik-a-naplózás)
  - [GNSS skyplot](#gnss-skyplot)
  - [Barometrikus magasság (Baro)](#barometrikus-magasság-baro)
  - [Hogyan naplóz a Fut/túra, mint egy sportóra](#hogyan-naplóz-a-futtúra-mint-egy-sportóra)
  - [Kalman-szűrő (hogyan simulnak a letárolt pontok)](#kalman-szűrő-hogyan-simulnak-a-letárolt-pontok)
  - [A beállítások hatása a tracklogra](#a-beállítások-hatása-a-tracklogra)
  - [Rögzítés sűrűsége](#rögzítés-sűrűsége)
  - [Douglas–Peucker (térkép-egyszerűsítés)](#douglaspeucker-térkép-egyszerűsítés)
  - [Engedélyek](#engedélyek)
- [Beüzemelés](#beüzemelés)
  - [Fordítás](#fordítás)
  - [Tesztek](#tesztek)
  - [Stack](#stack)
- [Technikai dokumentumok](#technikai-dokumentumok)
- [Play listing képernyőképek](#play-listing-képernyőképek)
- [Következő teendők](#következő-teendők)
- [Ami nincs ebben az appban](#ami-nincs-ebben-az-appban)

---

## Funkciók

### Naplózás

- **Indít / Leállít** a munkamenetet látható előtér-szolgáltatásként rögzíti, értesítéssel.
- A fixek csak akkor tárolódnak, ha átmennek a pontossági és műholdszám-kapun. Opcionális **Kalman**-simítás utána elmozdítja a pontot. Az **Okos** vagy **Minden jó fix** sűrűség dönti el, hogy beíródik-e (lásd Beállítások). Fut/túránál az alap: **Csak GNSS** (műholdchip, nem fused hely) simítás nélkül, hogy a kis úttest-alakzatok megmaradjanak a tracklogban. Teljes lánc: [Hogyan működik a naplózás](#hogyan-működik-a-naplózás).
- Eseménytípusok: `START`, `MOVE`, `PAUSE` (a usage pauza-sebesség alatt), `STOP`.
- Használati módok: repülő, hajó, autó, motor (alap), kerékpár, Fut/túra. A használat választása egy teljes előbeállítást ír (szűrők, csak GNSS, simítás, sűrűség, térkép-egyszerűsítés). A Fut/túra és a kerékpár lazább pontossági szűrőt és alacsonyabb pauza-küszöböt használ.
- Opcionális környezeti hőmérséklet (`TYPE_AMBIENT_TEMPERATURE`), barometrikus magasság (`TYPE_PRESSURE`; lásd [Barometrikus magasság (Baro)](#barometrikus-magasság-baro)), gyorsulásmérő-minták és dőlésszög (gravitáció, tankra szerelve) minden letárolt ponton.



### GPS fül

- Élő műholdszámok: GPS L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC. A chippek színe megegyezik a skyplotéval.
- Polar **skyplot** az SNR alatt (észak fent, Használatban vs Látható, L5 gyűrű). Lásd [GNSS skyplot](#gnss-skyplot). Rendszerek és sávok: [docs/all-gps-systems-hu.md](docs/all-gps-systems-hu.md).
- SNR minőség (kiváló / jó / közepes / gyenge / nincs jel).
- Szélesség, hosszúság, pontosság, forrás, magasság, naplózási állapot. **Baro**, ha van nyomásszenzor (QNH a Beállításokban; [hogyan számolódik a Baro](#barometrikus-magasság-baro)). Környezeti hőmérséklet.
- A **magasság** először MSL, aztán GNSS ellipszoid, aztán fused ellipszoid (`GpsAltitude.pick`). A −430…20000 m-en kívüli érték (néhány telefonon fused −1800 m körüli szemét) hiányzik.
- Ha a **Pontfelhő** be van: n, RMS, CEP95, medián jelentett pontosság, plusz álló / mozgás / várakozás felirat (ugyanaz a memóriabeli ablak, mint a térkép pöttyei; a CEP95-höz 8 minta kell).



### Útvonal fül

Az Indítás utáni összesítők (és a mentett / utolsó sessionre a Térképen): eltelt idő, út, mozgás ideje, várakozás ideje, magasság, irány, dőlésszög (telefon síkban a motortankon), hőmérséklet-tartomány, ha van szenzor, és GPS magasságprofil (szaggatott baro vonal, ha van nyomásminta). Idle-ben a sebesség és az átlagsebesség 0; Indítás után az élő GPS-sebesség és a session átlaga. A tengely min/max a GPS és a baro együtt, legalább 50 m. A jelmagyarázat az utolsó GPS- és baro-értéket mutatja. A baro a Beállítások QNH-ját és a [Barometrikus magasság (Baro)](#barometrikus-magasság-baro) szabályait használja.

### Térkép fül

- A jelenlegi helyre centrál, amikor a fület nyitod. Idle-ben elhúzhatod (Google és OSM). Indítás után a kamera követ. A **Teljes útvonal a képernyőn** minden GPS-frissítés után a teljes nyomvonalat a képernyőre illeszti (a nagyítás és mozgatás a következő fixig megengedett). A **Saját hely** gomb (bal felső: cián GPS-kereszt, ugyanolyan kör, mint a seprő) a GPS-fixre centrál, a zoomot nem változtatja.
- Piros polyline a Room-ból (élő munkamenet, utoljára mentett track, vagy a Mentett útvonalakban választott track). A térképvonal **maga** a letárolt log; nincs külön vázlat. Lásd [Hogyan működik a naplózás](#hogyan-működik-a-naplózás).
- **Google Maps**, ha a `MAPS_API_KEY` be van állítva; különben a telefonon megjelenő üzenet.
- **OSM Mapsforge**, ha letöltöttél egy régiót (vagy Turistautakot) és **Használatban** van az **Offline térkép letöltése** listán (vagy be van a **Letöltött OSM térkép használata**). Ugyanaz a polyline és pontossági gyűrű rajzolódik az OSM-re. Hiányzó vagy nem Mapsforge fájl üzenetet mutat, és kikapcsolja a kapcsolót, hogy a következő indítás ne crash-loop legyen. A letöltés csak `mapsforge binary OSM` mágiájú, egyező header-méretű fájlt tart meg. A kamera a `.map` start/bounds pontját használja, ha a GPS a fájlon kívül van; élő követés csak a fájlon belül. Az OSM `MapView` mérete megmarad, ha elhagyod a Térkép fület.
- Világos lila pontossági kör (sugár = GPS pontosság méterben). Beállításokban kapcsolható. A kör a **nyers** helyet követi (GNSS chip vagy fused), nem a Kalman-simított letárolt tracket.
- **HUD** mindkét térképmotor fölött: nagy sebesség (a Beállítások mértékegysége), pontosság, GNSS used/in view. Naplózáskor: út, eltelt idő, pulzáló REC. Idle GPS-fixszel: halkított panel bal lent. Mentett tracknél, ha nincs naplózás, rejtve.
- **Koppintás** a Google, az OSM és a Turistautak térképen ugyanazt a menüt nyitja a ponton. A menü a földrajzi ponthoz van kötve, és a kamera mozgatásakor követi. Új koppintás a régit leváltja. A húzás és a zoom megmarad. A menü és a távolságcél a Térkép fülön él, ezért Google és offline térkép között váltva a HUD-távolság megmarad. A három művelet külön fut: a cím nem indít HUD-ot, a távolság nem ír ki címet.
  - **Távolság** — a pont lesz a cél. Minden élő GPS-fixnél légvonal, távolodáskor is. A sor a HUD alatt van, akkor is, ha a sebesség-HUD rejtett. Nincs fix: gondolatjel. Új célpont felülírja a régit. A sor megnyomása törli. Szóköz nélkül: előtag **T** (magyar) vagy **D** (angol); mérték a Beállításokból — Metric `km` (10 alatt egy tizedes, attól felfelé egész: `T2.7km`, `T655km`), Imperial `mi`, ICAO `NM`.
  - **Mutató** — ugyanazon a soron, ha van cél és GPS-fix. A tű a fix és a cél kezdő főkör-irányszöge, mínusz az előre irány, 0–360°-ra tekerve. A felfelé álló tű (0°) azt jelenti, hogy a pont előtted van. 1 m/s-tól, ha a fixnek van GPS-pályája, az a haladási irány (igaz észak). 1 m/s alatt, vagy ha nincs pálya, a telefon iránytűje az előre: mágneses azimut plusz a fix deklinációja, tehát a telefon teteje az előre. Az Iránytű fül MAG/TRUE kapcsolója a mutatót nem változtatja; mindig a földrajzi irányszögre céloz. Alacsony magnetométer-pontosság csak akkor halványítja a tűt, ha az iránytű adja az irányt. 20 m-en belül, vagy a jelentett GPS-pontosságon belül, ha az nagyobb, a tű eltűnik és a tárcsa marad, hogy a sor szélessége ne ugorjon; a távolság tovább frissül. Zsebben az iránytű a telefont követi, nem a tested; amint mozogsz, a GPS-pálya váltja fel. A keresés **Igen** ugyanezt a célt állítja, ezért a mutató ott is megjelenik. Másik `.map` fájl a célt és a mutatót együtt törli. A sor megnyomása mindkettőt törli. Nincs fix: a gondolatjel, tárcsa nélkül.
  - **GPS koordináta** — kártya, nem a HUD. A szélesség és a hosszúság egymás alatt, hat tizedes, pont a tizedesjel: `Lat: 47.497913`, alatta `Lon: 19.040236`. A másolás ikon a vágólapra `47.497913, 19.040236` formát tesz (előbb a szélesség), hogy egy térképkereső egyből megtalálja. A következő koppintásig vagy **Bezárás**ig marad.
  - **Cím** — csak akkor látszik, ha a térképformátum együtt visszaadja az irányítószámot, az országot, a várost, az utcát és a házszámot. Választásra ezek a mezők jönnek fel egy kártyán, a HUD változatlan. A mostani három formátum ezt nem tudja, ezért a pont rejtve marad: a Google koppintás csak koordináta; az OSM `.map`-ben van közeli utcanév és házszám, de nincs hozzájuk kötött irányítószám, ország és város; a Turistautak `.map`-ben házszám nincs.
- Kis piros sziluett a helyeden (repülő, hajó, autó, motor, kerékpár, Fut/túra — ugyanaz, mint a Beállításokban). Élő GPS-fixnél cián GNSS-retikuluson ül. Álló portrén vízszintesen marad. Az északjelző mindig a térképen van.
- Zöld **S** a kirajzolt track elején; piros **E** a végén, ha nincs naplózás (naplózáskor a sziluett a most).
- Ha mentett track látszik és nincs naplózás, a bal felső seprő leveszi a vonalat a térképről, a logot nem törli. Indítás vagy Mentett útvonalak → Térképen újra kirajzol. A **Saját hely** a seprő alatt van (vagy egyedül bal fent, ha nincs mentett track).
- A **Keresés** (nagyító a Saját hely alatt) csak akkor látszik, ha OSM vagy Turistautak térkép van használatban. A `.map` fájlban tárolt névvel bíró helyeket keresi: város, kisváros, falu, település, városrész, csúcs, szobor, emlékmű, nevezetesség, ház (utca és házszám), épület, utca és minden más névvel bíró hely. A rétegkapcsolók nem rejtik el, ami a fájlban névvel bent van. Legalább 3 karakter; az ékezet nem számít (`szobor` megtalálja a `Szobor`t). Legfeljebb 5 találat: előbb az erősebb név, azonos névnél a közelebbi. A távolság az élő GPS-fixhez képest, fix nélkül a térkép start pontjához. Egy sor: **Navigáljak ide: &lt;név&gt;?** mellette a fajta és a légvonal. Az **Igen** oda viszi a kamerát (a zoom a fajtától függ) és beállítja a HUD távolságcélját. A **Nem** nem mozdítja a térképet. Naplózás közben, vagy ha a teljes útvonal a képernyőn marad, a kamera a helyen marad, amíg a **Saját hely** vagy az első húzás újra a GPS-követést engedi. A HUD légvonala közben frissül. Google Térképen nincs keresés, mert ezek a nevek nincsenek letöltött fájlban. A használatban lévő térkép a háttérben indexelődik, akkor is, ha az app közben kilép; a már beírt helyek kereshetők, és a következő indulás onnan folytatja, ahol abbahagyta. Ha a 250 000 helyes plafon betelik, a kereső megmondja, hogy csak egy rész van beolvasva. Alacsony akkumulátornál vagy kevés tárhelynél az indexelés vár. Másik `.map` fájlra váltáskor a koppintás menüje és a HUD-távolság törlődik; Google és offline között a távolság megmarad.
- Douglas–Peucker egyszerűsítés a kirajzolt vonalon, ha az **Útvonal egyszerűsítése a térképen** be van (lásd lent). Az SQLite, az Útvonal összesítők és a KMZ soha nem egyszerűsödik.



### Iránytű fül

Mágneses heading (MAG) a forgásérzékelőből, vagy TRUE (földrajzi észak = MAG + a last GPS-fix deklinációja). MAG / TRUE ezen a fülön, alap MAG, nem kötődik a usage-hez. GPS-fix nélkül a TRUE MAG marad, Nincs GPS. Alacsony magnetométer-pontosságnál „8-as a levegőben” a dial alatt. Forgó rózsa, rögzített lubber, MAG vagy TRUE plusz háromjegyű heading középen. Indítás nélkül is működik.

### Mentett útvonalak

- Munkamenetek listája dátummal, használattal, mértékegységgel.
- **Térképen** a Térkép fület nyitja azon a munkameneten (Google Maps vagy OSM), a sessionben tárolt használati módot beírja a Beállításokba, és azzal rajzolja. Utána a usage vagy a csúszkák váltása más módban mutatja ugyanazt a logot. A következő Indít a kiválasztott Beállításokat követi. A seprő leveszi a vonalat, a munkamenetet nem törli.
- **Magasság** GPS-magasság × távolság chartot nyit (szaggatott baro vonal, ha van minta; ugyanazok a Baro-szabályok, mint a [Barometrikus magasság (Baro)](#barometrikus-magasság-baro) alatt).
- **Törlés** minden sessionnél (keskeny kijelzőn a Magasság alá tör). Megerősítés után cascade-törli a SQLite sessiont és a pontjait.
- Jelölőnégyzetek, **Összes kijelölése**, **Kijelöltek megosztása** → KMZ vagy GPX:
  - egy munkamenet → `GTL_yyyyMMdd_HHmmss.kmz` vagy `.gpx`
  - több munkamenet → egy KMZ trackenként mappával, vagy egy GPX több `<trk>`-kel



### KMZ export

- Csomagolt play (indítás), pause és stop ikonok (`IconStyle` scale **0.8**); a térképfeliratok rejtettek (`LabelStyle` scale 0). A **látható** vonal KML `LineString`, `tessellate` és `clampToGround`, magasság 0, hogy a Google Earth a terepre feszítse (a `gx:Track` GPS-magassággal a 3. `gx:coord`-on közeli zoomnál az utca mellé emelkedik, és a kamera alá tűnhet). A Start / Pause / Stop Point magassága is 0. Egy rejtett `gx:Track` tárolja a `when`, speed, odometer, GPS `alt` és `baro` adatot.
- A vonal a letárolt log; a session végét jelölő STOP sor nem lesz extra horog. A Stop ikon az utolsó path-csúcson van. A Pause ikon a pauza-csúcson van (állásonként egy; Start/Stop átfedésnél elmarad).
- START / PAUSE / STOP balloonok (a Google Earth play, pause vagy stop ikonjára koppintva). A placemark neve **Start**, **Pause**, **Stop**. A leírás HTML (`<br/>`), hogy az Earth details minden mezőt mutasson. Az idő UTC, nincs `time=` előtag és nincs `UTC` utótag. A mértékegység a Beállításokat követi (metrikus: km/h, m / km, °C; angolszász: mph, ft / mi, °F; ICAO: kt, ft / NM, °C). A balloonban **nincs** `usage=` és `lean=`.
  - **Start:** `YYYY:MM:DD HH:MM:SS`, `temp=` (`N/A`, ha nincs szenzorminta), `lon=`, `lat=`, `Altitude:` (GPS), `Baro:` (a letárolt `pressureHpa` a **jelenlegi** Beállítások QNH-jával és GPS-kalibrációs offsettel megosztáskor, ugyanaz, mint a magasságprofil szaggatott vonala, vagy `-`; kimarad, ha több mint 1500 m-re van a pont GPS-magasságától). Nincs Speed / Avg. Speed / Max speed / duration / distance.
  - **Pause:** ugyanazok a sorok, plusz `Speed:` (pillanatnyi GPS-sebesség a pauza-soron), `duration=` (másodperc, ha 60 s vagy kevesebb, egész perc 60 perc alatt, különben `HH:MM:SS` a Starttól), és `distance=` az addigi út a kiválasztott mértékegységben. Nincs Avg. Speed / Max speed.
  - **Stop:** ugyanazok a sorok, plusz `Avg. Speed:` és `Max speed:` (egy tizedes) a `TrackStatsCalculator`-ból a pathon, majd `duration=` és `distance=` a teljes sessionre. Nincs pillanatnyi `Speed:`.
- Minden rejtett `gx:Track` pont ExtendedData: `speed` (m/s), `odometer` (m), `alt` (GPS méter), `baro` (méter a `pressureHpa`-ból a megosztáskori QNH-val és offsettel, `-` ha nincs minta, vagy ha az érték több mint 1500 m-re van a GPS-magasságtól). A `gx:coord` magasság 0. QNH- vagy Kalibrálás GPS-ből változtatás után oszd meg újra a KMZ-t. Képlet: [Barometrikus magasság (Baro)](#barometrikus-magasság-baro).
- MIME `application/vnd.google-earth.kmz`. Nyisd meg Google Earth-tel (ha kell, telepítsd a Play Áruházból).
- A súgó **KMZ és GPX megosztása** felsorolja a balloon mezőket (EN/HU) és a SQLite `gps_events` mezőit.

### GPX export

- GPX 1.1 mag: sessionenként egy `<trk>` / egy `<trkseg>` (az auto-PAUSE nem darabolja a vonalat). A záró STOP marker nem lesz extra `<trkpt>`.
- Minden letárolt pont `<trkpt>`: `lat`, `lon`, `<ele>` (GPS-magasság), `<time>` (UTC). Nincs speed-kiterjesztés, hogy az OsmAnd, Komoot, Garmin Connect, Relive és QGIS be tudja olvasni. A baro nem kerül a GPX-be; SQLite-ban és KMZ-ben marad. Lásd [Barometrikus magasság (Baro)](#barometrikus-magasság-baro).
- START / PAUSE / STOP `<wpt>` neve Start, Pause, Stop. A Stop waypoint az utolsó path-pont (ugyanaz a pattinás, mint a KMZ).
- Több kijelölt session → egy `.gpx` több `<trk>`-kel. Fájlnév `GTL_yyyyMMdd_HHmmss.gpx`. MIME `application/gpx+xml`.
- Mentett útvonalak → Kijelöltek megosztása → KMZ vagy GPX.



### Beállítások

A **használat** választása egy DataStore-szerkesztésben felülírja a kapcsolódó alapértékeket. Utána bármelyik vezérlő külön is állítható.


| Használat        | Mértékegység | Csak GNSS | Rögzített útvonal simítása | Erősség | Álláskor ne vándoroljon | Sűrűség    | Egyszerűsítés a térképen | Tűrés |
| ---------------- | ------------ | --------- | -------------------------- | ------- | ----------------------- | ---------- | ------------------------ | ----- |
| Fut/túra         | Metrikus     | be        | ki                         | Alacsony | be                      | Minden jó  | ki                       | 2 m   |
| Kerékpár         | Metrikus     | be        | ki                         | Alacsony | be                      | Minden jó  | ki                       | 3 m   |
| Motor (alap)     | Metrikus     | ki        | be                         | Közepes | be                      | Okos       | be                       | 6 m   |
| Autó             | Metrikus     | ki        | be                         | Közepes | be                      | Okos       | be                       | 8 m   |
| Hajó             | ICAO         | ki        | be                         | Közepes | be                      | Okos       | be                       | 8 m   |
| Repülő           | ICAO         | ki        | be                         | Magas   | be                      | Okos       | be                       | 15 m  |


**Mit csinál az egyes vezérlő**

- **Használat** — tevékenység típusa. Újratölti a fenti táblát és a 2017-es pontossági / műhold kapukat (Fut/túra és kerékpár 45 m, többiek 30 m). Repülőnél és hajónál a mértékegység ICAO-ra vált; a többi használat metrikusra.
- **Mértékegység** — metrikus, angolszász vagy ICAO az Útvonalon (km/h és méter; mph és láb/mérföld; csomó, tengeri mérföld és láb). A letárolt koordinátákat nem mozgatja.
- **QNH** — tengerszinti nyomás a barométerhez, **900–1100 hPa** (alap `PRESSURE_STANDARD_ATMOSPHERE` 1013,25). Csak akkor látszik, ha a telefonnak van nyomásszenzora. Az élő baro, a magasságprofil szaggatott vonala és a KMZ `Baro:` / ExtendedData `baro` a `getAltitude(QNH, nyomás − offset)` (KMZ **megosztáskor**). Ha ez a magasság több mint 1500 m-re van a pont GPS-magasságától, a letárolt íráskori `baroAltitude` marad, vagy a baro kimarad. A letárolt `pressureHpa` nyers; a `baroAltitude` íráskor az akkor érvényes QNH-t és offsetet használja. Valós tengerszinti QNH-t METAR-ból, ATIS-ból vagy reptéri időjárásból nézz (nem állomásnyomás). **Kalibrálás GPS-ből** (állj, jó GPS-magasság) a chip offsetjét a DataStore-ba írja (±10 hPa), a QNH csúszkát nem; **Baro visszaállítás** törli. **Automatikus kalibrálás induláskor** (alapból be) ugyanezt a kalibrálást futtatja le automatikusan, amint minden felvétel indulása után két egymást követő GPS-fix 15 méteren belül egyezik a magasságban, hogy ezt ne neked kelljen megnyomnod. Részletek: [Barometrikus magasság (Baro)](#barometrikus-magasság-baro).
- **Letöltött OSM térkép használata** — letöltésig ki van kapcsolva és nem állítható. Bekapcsolva a **Használatban** lévő Mapsforge fájl; kikapcsolva a Térkép Google Térképet mutat. Hiányzó vagy érvénytelen `.map` kikapcsolja a kapcsolót. A Beállításokban az **OSM térkép** kártya csak OSM-régió **Használatban** állapotánál látszik (nem Turistautak), a **Turistautak.hu** kártya csak annak **Használatban** állapotánál. A Térkép fül réteg gombja (ugyanott, ahol a Google rétegek) a megfelelő kapcsolókat nyitja. OSM: Épületek (alapból be), **POI** (ki; boltok, éttermek, parkolók, kutak 14-es zoomtól — nem buszmegálló), Tömegközlekedés (ki; vasút/villamos/állomás és buszmegálló), Kerékpárutak kiemelése (Kerékpár usage-nél be; magenta overlay 12-es zoomtól; a külön `highway=cycleway` kék marad; usage-váltás visszaállítja), Védett terület / park (be), Domborzat (ki). A kapcsoló a csempét újrarajzolja, a kamera nem mozog. A hivatalos Mapsforge fájlban bármely országnál csak a külön `highway=cycleway` van, úttesti sáv nincs. A Domborzat ki marad, kivéve ha HGT fájlok vannak a `.map` mellett vagy a `hills/` mappában (a hivatalos Mapsforge-letöltésekben általában nincs).
- **Útvonal egyszerűsítése a térképen** — kevesebb csúcs csak a Térképen. A kapcsoló bekapcsolva **1–20 m** csúszka (1 m-es lépés). A KMZ és az odométer minden letárolt pontot megtart.
- **Utolsó naplózott útvonal a térképen** — Leállítás után az utolsó (vagy kijelölt) track a Térképen marad. A seprő leveszi a kirajzolt mentett tracket, a logot nem törli.
- **Teljes útvonal a képernyőn** — naplózáskor minden GPS-frissítés a teljes nyomvonalat a képernyőre illeszti. A nagyítás és mozgatás a következő fixig megengedett.
- **Képernyő bekapcsolva naplózáskor** — alapból ki. Csak felvétel alatt tartja ébren a kijelzőt (tankra szerelt telefon).
- **Pontossági jelzés megjelenítése** — világos lila kör; a sugár a GPS pontossága. A HUD a nyers helyen marad (chip vagy fused).
- **Pontfelhő** — pasztell magenta pöttyök a nyers GPS-fixekből, amíg állsz, plusz magenta CEP95-kör a felhő centroidján. Alapból ki. Bekapcsoláskor a pontossági jelzés is bekapcsol; kikapcsoláskor csak a felhő tűnik el. Mozgás közben szünetel. Nem íródik a naplóba és a KMZ-be.
- **Csak GNSS** — műholdchip-pozíciók fused hely helyett. Fut/túránál és kerékpárnál be; járműveknél ki.
- **Rögzített útvonal simítása**, **Simítás erőssége**, **Álláskor ne vándoroljon a pont**, **Rögzítés sűrűsége** — ezek azt változtatják, ami **a tracklogba íródik**. Részletek lent.

A meglévő telepítések, amelyeknél még a régi **19,5 m** egyszerűsítési alap van, a usage táblára migrálnak, amikor a Kalman-kulcsok először íródnak. A 19,5-től eltérő egyedi tűrés megmarad.

### További képernyők

- Biztonságos vezetés nyilatkozat a telepítés utáni első indításkor. Az **Elfogadom** a választ a telefon DataStore-jába írja; a későbbi indítások a GPS-re mennek, a nyilatkozat rejtve marad. Az **Elutasítom** bezárja az appot, és nem ment elfogadást, ezért a következő indítás megint kérdez. Az eltávolítás törli az app adatait (a mentés ki van kapcsolva), ezért az új telepítés megint kérdez. A splash addig marad, amíg ez a mentett választás be nem olvasható, így az elfogadott nyilatkozat nem villan fel a GPS előtt.
- Offline térkép letöltése (először Turistautak.hu, aztán Mapsforge v5 OSM-régiók). A letöltött térkép **Használható** vagy **Használatban**; egyszerre csak egy lehet Használatban. A Használatban gomb Google Térképre vált. A letöltött régiót onnan törölheted. Opcionális túratérkép kódban kapuzva (`TuhuFeature.enabled`, alapból be); részletek: `docs/tuhu-hu.md`.
- Helymeghatározás beállításai (megnyitja a rendszer GPS-panelét).
- Súgó: harmonika (egyszerre egy szakasz nyitva). Használat, **Beállítások** (előbeállítások, QNH, OSM térképrétegek és minden vezérlő), Útvonalnaplózás (Kalman vs Douglas–Peucker vs sűrűség), GPS (skyplot, magasságválasztás, baro), Útvonal (Idle-ben sebesség és átlagsebesség 0), Térkép (OSM fájl, S/E), **OSM térkép opciók**, Turistautak opciók ha az a térkép le van töltve, Iránytű, KMZ/KML megtekintése, adatvédelmi tájékoztató, letárolt trackpont mezőtábla. Angol és magyar.
- Adatvédelmi tájékoztató hivatkozás.
- Névjegy: harmonika (egyszerre egy téma nyitva). Alkalmazás adatai (verzió, csomag, ez a készülék), helyi GPS útvonalnapló, OSM (ODbL és weboldal), Turistautak, eredeti tároló, szerzői jog. Az OSM Térkép fülön **© OpenStreetMap**. A verziószám hétszeri érintése, két másodpercen belül, megnyitja a telefonon lévő hibanaplót: UTC időbélyeg, a sikertelen művelet, és a teljes hívási verem az okkal együtt. A **Törlés** törli az `errors.log` és az `errors.log.1` fájlt.

---



## Architektúra

Két Gradle-modul:


| Modul     | Szerep                                                                                                                                                                              |
| --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `:engine` | Tiszta JVM: GNSS-osztályozás, skyplot-projekció, GPS-magasság választás, baro/QNH, Kalman trackszűrő, fix-elfogadás, sebességadaptív térköz, Douglas–Peucker, trackstatisztika, KML/KMZ, GPX 1.1, térkép-HUD láthatóság, koppintásos légvonal- és koordinátafelirat, célmutató a koppintásos légvonalhoz, Útvonal-fül idle sebesség, iránytű MAG/TRUE heading, magasságprofil, OSM fájl/kamera/újrarajzolás, OSM megjelenítési kategóriák, térkép-láthatósági szabályok, track végpontok, pontfelhő-buffer. A JUnit tesztek itt vannak. |
| `:app`    | Android: Compose UI, Room, DataStore, hely/GNSS/szenzorok, előtér-szolgáltatás, Google Maps, Mapsforge, WorkManager OSM-letöltés, FileProvider megosztás.                            |


```
app/     Compose, Room, szolgáltatások, térképek
engine/  Domain-algoritmusok (nincs Android SDK)
docs/    Adatvédelmi tájékoztató, Play-anyagok
```



### Adatok

- **Room:** `track_sessions` + `gps_events` (kaszkád törlés). A Map polyline mindig a Room-ból olvasódik, nem memóriabeli vázlatból. Ezért a látott vonal az a log, amit eltároltál.
- **DataStore:** nyilatkozat, használat, mértékegység, QNH, baro nyomás-offset, szűrők, OSM-fájlútvonal, OSM rétegkapcsolók, térképbeállítások, Kalman / sűrűség / csak GNSS / térkép-egyszerűsítés / pontfelhő.
- **Fájlok:** OSM `.map` letöltések; KMZ a `files/gtltracklogs/` alatt (FileProvider). Hibanapló a `files/diagnostics/` alatt (`errors.log`, az előző fájl `errors.log.1`, ha az aktuális átlépi a kb. 256 KB-ot).
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
  → STOP placemark (utolsó **elfogadott** letárolt pont, hogy az ikon a tracklog végén legyen)
  → Térkép / Útvonal / KMZ mind a Room-ot olvassa
```

**Két helyfolyam.** Amíg az app nyitva van és nem naplóz, a `GtlViewModel` kb. másodpercenként figyel, hogy a GPS és Térkép HUD az Indítás előtt is frissüljön. Indítás után a ViewModel leállítja ezt a preview-t, hogy a HUD a service `lastLocation`-jével egyezzen. Csak a `TrackingForegroundService` ír. Legalább 500 ms-enként kér frissítést (`minTimeMillis`, minimális távolság `0`). A térközt később a `FixAcceptance` alkalmazza, nem az Android.

**Forrás.** **Csak GNSS** be → Android `GPS_PROVIDER` (a műholdchip: GPS, Galileo, GLONASS, BeiDou, QZSS, NavIC — a szolgáltató neve történeti). Ki → Play Services fused `PRIORITY_HIGH_ACCURACY` (műholdak Wi-Fi-vel, cellával és IMU-val keverve). Ha a Csak GNSS be van és a GPS ki, nincs fused tartalék; a HUD kéri, hogy kapcsold be a GPS-t. Fut/túránál az alap a csak GNSS, hogy egy 5–10 m-es úttest-hurkot ne lapítson el a telefon „hol van a felhasználó?” szűrője, mielőtt a GTL egyáltalán látná.

**HUD vs letárolt track.** Minden frissítés a **nyers** `Location`-t másolja a `lastLocation`-be. A világos lila pontossági kör, az élő szélesség/hosszúság, a forrás és a pontosság ez a nyers fix. A **magasság** ezen az objektumon már `GpsAltitude.pick` (GNSS MSL, fused MSL, GNSS ellipszoid, fused ellipszoid; −430…20000 m-en kívül eldobva). A **Pontfelhő** ugyanezt a `lastLocation`-t mintavételezi egy memóriabeli ablakba (centroid RMS / CEP95), és nem ír SQLite-ot. Bekapcsoláskor a pontossági jelzés is bekapcsol; kikapcsoláskor csak a felhő tűnik el. A piros polyline az, ami **a Roomba bekerült** (Kalman-simítva, ha az a kapcsoló be van). Szándékosan lehetnek pár méterre egymástól.

**1. kapu — pontosság és műholdak.** A usage pontosságánál rosszabb (30 m, Fut/túránál és kerékpárnál 45 m) vagy 4-nél kevesebb műholdas fix eldobódik. Nem megy Kalmanba, és nem lesz sor. A HUD ettől még frissül.

**2. kapu — opcionális Kalman.** Ha a **Rögzített útvonal simítása** be van, a `KalmanTrackFilter.observe` minden pontosságon átment fixen lefut (egy szűrőpéldány Indít→Leállít munkamenetenként; folytatásnál az utolsó letárolt pontból magoz). Új lat/lon-t ad. Az időbélyeg, magasság, pontosság és műholdszám a GPS-fixé marad. A sebesség és az irányszög a szűrő sebességéből jön, ha az legalább 0,3 m/s; különben a GPS-fixé (vagy az utolsó irányszög). A Kalman **mozgatja** a pontokat, és **ugyanannyi jelöltet tart**. Nem map-matching, és nem dob el csúcsokat. Járműveknél ez az alap, hogy a körforgalom kerek legyen, a cruise tiszta vonal. Fut/túránál **ki**, hogy egy kis nyolcas ne számítson mérési zajnak.

**3. kapu — rögzítés sűrűsége.** A `FixAcceptance.shouldAccept` dönti el, hogy az (esetleg simított) pont SQLite-sor legyen-e. A Kalman akkor is frissül, ha a pont kimarad.

- A munkamenet első fixe → mindig `START`-ként tárolódik.
- **Okos** (járművek): ír, ha a haversine-távolság az utolsó **eltárolt** ponttól eléri a 2014-es sebességsávot; kanyarban annak a fele (irányszög-változás > 15°). Fut/túra Okos ezt a sávot újra felezi (min. 1 m).
- **Minden jó** (Fut/túra alap): ír, ha eltelt a `minTimeMillis` (500 ms) **vagy** az irányszög kanyarban van, és a távolság legalább **1 m** (jármű) vagy **0,5 m** (Fut/túra és kerékpár).
- A csúszka köztes állásai az Okos térközt keverik a Minden-jó padlóval; a min-idő / kanyar út akkor is elfogadhat egy pontot.
- Ha a GPS `bearing` 0 (kocogáskor gyakori), a kanyardetekció a szomszédos pozíciókból számolt irányszöget is használhatja.

**Eseménytípus.** Írás után: `START` az első ponton; `PAUSE`, ha a sebesség a usage pauza-küszöb alatt van (0,25 m/s Fut/túra, 0,4 m/s járművek); különben `MOVE`. Opcionális környezeti hőmérséklet, utolsó gyorsulásmérő XYZ, dőlésszög (gravitáció, tankra szerelve), nyers `pressureHpa` és íráskori `baroAltitude` a sorra másolódik. Az iránytű azimutja csak HUD, nem tárolódik. Lásd [Barometrikus magasság (Baro)](#barometrikus-magasság-baro).

**Leállítás.** Mindig ír egy `STOP` sort (`isPlacemark` true), még ha a sűrűség eldobná is a pontot. A koordináta az utolsó **elfogadott** letárolt fix (nem a nyers HUD-fix, ami pár méterre lehet a logtól). A KMZ/GPX a Stop ikont erre az utolsó path-csúcsra teszi.

**Térképrajzolás.** A `GtlViewModel` a Room-sorokat `displayPoints`-re képezi. A `MapTrackVisibility` akkor mutatja a vonalat, ha naplózás megy, ha az **Utolsó naplózott útvonal a térképen** be van, vagy ha Mentett útvonalak-munkamenet van kiválasztva — hacsak a seprő `mapCleared`-et nem állított (csak idle; naplózáskor akkor is rajzol). Ha az **Útvonal egyszerűsítése a térképen** be van, és több mint 4 pont van, a Douglas–Peucker **csak ezeket a megjelenítési csúcsokat** ritkítja az 1–20 m csúszkán. Az SQLite, az Útvonal-odométer és a KMZ soha nem megy DP-n. Egyszerűsítés **ki** (Fut/túra és kerékpár alap) esetén minden letárolt csúcs a térképen van — ezért marad látható egy kis úttest-hurok.

**Miért néz ki a térkép a logodnak**


| Réteg                | Mit csinál                    | Hatás a térképen                                                                              |
| -------------------- | ----------------------------- | --------------------------------------------------------------------------------------------- |
| GNSS chip vs fused   | Ki válaszol a „hol vagyok?”-ra | Fut/túra és kerékpár: chip-track, utcai léptékű alak megmarad. Járművek: fused, kevesebb Wi-Fi/cella-ugrás.    |
| Pontosság / műhold   | Szemét eldobása Kalman előtt  | Nincs 200 m-es teleport-tüske a vonalon.                                                      |
| Kalman (opcionális)  | Pontok mozgatása, darabszám megmarad | Jármű-körforgalom és cruise sima; álló zár megállítja a 10 m-es firkát.                  |
| Sűrűség              | Hány pont tárolódik           | Okos: autópályán kevesebb pont. Minden jó: ~2 Hz, szűk hurkok megtartják a csúcsokat.         |
| Room                 | Egyetlen igazságforrás        | Térkép, Útvonal és KMZ ugyanaz az út.                                                         |
| Douglas–Peucker      | Csak megjelenítés ritkítása   | Hosszú járműtrack olcsón rajzolható; Fut/túránál ki, hogy a térkép = SQLite.                      |


Semmi nem kerül fel. A Stop utáni `RemoteTrackSync` no-op.

Pipeline mermaid (ugyanaz a folyamat, több dobozzal): [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md) / [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md).

### GNSS skyplot

A GPS fül polar plotja **az égbolt térképe, ahogy a chip látja**, nem 3D földgömb és nem második tracklog. Az SNR alatt van, a konstelláció-chippek után. A chippek maradnak: ezek a villantható `used/in view` számok (GPS L1, GPS L5, Galileo, GLONASS, BeiDou, QZSS, NavIC). A skyplot azt mutatja, **hol** vannak ezek a műholdak. Mi a különbség a rendszerek és sávok között: [docs/all-gps-systems-hu.md](docs/all-gps-systems-hu.md). Adatút: [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md#skyplot-körök-gps-fül).

Kétféle kör van a skyploton: a **rács** (az ég geometriája) és a **műholdjelölők**.

**Rács (nagy koncentrikus körök).** Polar térkép, észak fent. A közép a zenit (90° eleváció, műhold a fejed fölött). A külső vastag gyűrű a **horizon** (0°). A két vékonyabb gyűrű **30°** és **60°**. Minél közelebb van egy pont a középhez, annál magasabban van a műhold. A 12 óra észak (azimut 0°); kelet, dél, nyugat óramutató szerint. A plot **nem** forog a telefonnal — azt az Iránytű fül csinálja. A horizon alatti műhold kimarad.

**Műholdjelölők (kis körök).** Mindegyik egy műhold (ugyanannak az SVID-nek az L1+L5 sora egy pont).

| Jelölés | Jelentés |
|---|---|
| **Kitöltött** korong | **Használatban** — benne van a jelenlegi helyfixben |
| **Üres** kör | **Látható** — a chip látja, de nincs a fixben |
| **Belső gyűrű** a korongban | **L5** — L5-osztályú vivő (~1176,45 MHz; GPS L5, Galileo E5a is) |

A **szín** a konstelláció, ugyanaz, mint a fenti chipeken: GPS kék, Galileo lime, GLONASS carmine, BeiDou borostyán, QZSS magenta, NavIC cián; SBAS/ismeretlen halk. A marker **mérete fix**; a jelerősség (SNR) a felette lévő sávon van, nem a kör nagyságán.

**Sarkok.** Bal fent **SKYPLOT**. Jobb fent **Látható** (üres kör). Bal lent **Használatban** (kitöltött). Jobb lent **L5** (kitöltött + belső gyűrű). Az égtájak a horizon-gyűrűn: **N** carmine 12 óránál, majd **E**, **S**, **W**.

**Kétfrekvenciás.** Az Android ugyanannak az SVID-nek az L1 és L5 sorát két `GnssStatus` sorként adja, azonos azimuttal/elevációval. A plot egy ponttá vonja össze, hogy ne legyen két egymásra tett pötty. A chippek ezeket a sorokat továbbra is külön számolják (`satellitesInView` a nyers sorszám, mint a Térkép HUD `used/in view`).

**Adatút.** `GnssStatus.Callback` → műholdankénti `SatelliteSample` (azimut, eleváció, CN0, used, konstelláció, vivő) → `GnssSnapshot.satellites` memóriában → polar canvas. Sem a `gps_events`, sem a KMZ, sem a GPX nem kapja. A Kalman, a sűrűség és a **Csak GNSS** azt változtatja, *honnan jön a fix*, nem ezt a plotot. A skyplot a chip aktuális egét mutatja, fused-től függetlenül.

**Mikor él.** Amint van helyengedély, mint az iránytű és a GPS számok. Indítás nem kell. Üres gyűrűk, amíg nincs műhold (vagy ha nincs engedély). Alagútban nem cache-eli az utolsó „szép” eget.

Engine: `Gnss.kt` (minta + snapshot) és `Skyplot.kt` (projekció + L1/L5 összevonás). UI: `GnssSkyplot` a GPS fülön. A Térkép HUD változatlan.

### Barometrikus magasság (Baro)

**Mi ez.** Magasság a telefon légnyomás-szenzorából (`TYPE_PRESSURE`), nem GPS-magasság és nem a Google Earth terep-DEM-je. A szám repülős **QNH-magasság**: tengerszinti nyomás plusz a Nemzetközi Standard Atmoszféra (ISA), hogy az eredmény közelítő méter legyen közepes tengerszint felett (MSL). Az időjárás, a chip hibája és a rossz QNH a GPS-hez képest eltolhatja.

**Hogyan számolódik.** Az élő és a letárolt átszámítás Android `SensorManager.getAltitude(qnhHpa, pressureHpa − offsetHpa)`. A `:engine` másolat (`BaroAltitude.metersFromPressureHpa`) ugyanaz az ISA-képlet:

`h = 44330 × (1 − (p_corr / QNH)^(1 / 5.255))`

ahol `p_corr` a nyers hektopascal mínusz a GPS-kalibrációs offset. A QNH **900–1100 hPa**, alap `PRESSURE_STANDARD_ATMOSPHERE` **1013,25**. Az offset **±10 hPa**. Tengerszinti QNH-t METAR-ból, ATIS-ból vagy reptéri időjárásból nézz — nem állomásnyomást (QFE).

**Kalibrálás GPS-ből.** Állj, megbízható GPS-magassággal. Az app kiszámolja, milyen állomásnyomást várna az ISA ezen a magasságon és QNH-n (`expectedStationHpa`), majd a `pressureHpa − expected` értéket DataStore offsetként tárolja. A QNH csúszka nem mozdul. A **Baro visszaállítás** törli az offsetet.

**Automatikus kalibrálás induláskor.** Beállítások → Baro → **Automatikus kalibrálás induláskor** (alapból **be**, csak akkor látszik, ha a telefonnak van nyomásszenzora). Indítás után minden GPS-fix, amely átmegy a szokásos pontosság-/műholdszűrőn, összehasonlításra kerül az előzővel; amint két **egymást követő** fix 15 méteren (`MaxAltitudeJitterMeters`) belül egyezik a magasságban, a szolgáltatás lefuttatja pontosan ugyanazt a számítást, mint a **Kalibrálás GPS-ből**, ennek a fixnek a magasságát és az aktuális nyomásértéket használva, majd eltárolja a kapott offsetet — session-enként egyszer. Az, hogy két fixnek kell egyeznie ahelyett, hogy az elsőben megbíznánk, a gyakorlatban számít: közvetlenül a helymeghatározás indulása után egyetlen GPS-fix magassága akár több tíz métert is tévedhet (a függőleges pontosság lassabban áll be, mint a vízszintes, és az Android saját pontosság-értéke csak a vízszintes hibát írja le) — a funkció egy korábbi verziója közvetlenül az első átmenő fixről kalibrált, és egy session teljes hosszára ~41 m-es hibát zárt be, miután az első fix magassága egyetlen mintára ~144 m-ről 185 m-re ugrott. Nem kell többé megállnod és megnyomni a Kalibrálást minden túra előtt: az offset egyből az adott session valós időjárásához igazodik ahelyett, hogy az ISA 1013,25 hPa alapértéken maradna (ami a GPS-hez képest jellemzően 40–90 m eltérést okoz, lásd az 1500 m-es őrnél lentebb). Ha még nincs nyomásminta, hihető magasság, vagy megerősítő előző fix, az app egyszerűen vár, és a következő fixnél próbálja újra — csak sikerig, akkor is csak egyszer. A kapcsoló kikapcsolása visszaállítja a régi működést: az offset csak akkor változik, ha te magad megnyomod a **Kalibrálás GPS-ből** vagy a **Baro visszaállítás** gombot. Ha a szaggatott baro vonal menet közben hirtelen, nagyot ugrik (ettől a funkciótól függetlenül), az általában a barométer valós, de irreleváns nyomásváltozása — zseb, táska, autóajtó, légkondi —, nem hiba; ha ez megnyugszik, a **Kalibrálás GPS-ből** vagy a **Baro visszaállítás** helyreállítja. Engine: `BaroAltitude.autoCalibrateEligible`. App: `TrackingForegroundService.maybeAutoCalibrateBaro`.

**Íráskor vs képernyőn.** Minden `gps_events` sor nyers `pressureHpa`-t és `baroAltitude`-ot tárol, az **akkor** érvényes QNH-val és offsettel. A GPS-fül Baro, az Útvonal / Mentett útvonalak szaggatott magasságvonala és a KMZ `Baro:` / ExtendedData `baro` a `pressureHpa`-ból számol a **jelenlegi** QNH-val és offsettel (`displayedMeters`; KMZ **megosztáskor**). A QNH utólagos változtatása ezeket a megjelenítéseket frissíti, a SQLite-ot nem írja újra. QNH- vagy Kalibrálás-változtatás után oszd meg újra a KMZ-t.

**1500 m-es GPS-őr.** Ha az újraszámolt magasság több mint `MaxGpsDeltaMeters` (**1500 m**) a pont GPS-magasságától, a `pickDisplayed` a letárolt `baroAltitude`-ot használja, ha az 1500 m-en belül van a GPS-től; különben a baro kimarad (`Baro: -` / nincs szaggatott minta). Néhány tíz méter az ISA 1013,25 és egy valódi METAR között (például LHBP ~1022 hPa, kb. 70 m az ISA-hoz képest, GPS ~140 m) érvényes, megmarad. Kb. 2000 m a 140 m-es GPS mellett elutasítva (hamis ~800 hPa minta).

**GPX.** A `<ele>` csak GPS-magasság. A baro SQLite-ban és a KMZ balloon / ExtendedData mezőben marad.

Engine: `BaroAltitude.kt`. App: `AndroidBaroAltitude.kt`.

**Pontosság — ez becslés, nem mérés.** A `TYPE_PRESSURE` csak a környezeti légnyomást méri; a „magasság” ennek a nyomásnak a számított átalakítása az ISA-modellen keresztül, nem közvetlen mérés. Pontos helyi tengerszinti nyomást (QNH) igényel, és elcsúszik, ahogy a valós időjárás eltávolodik ettől a referenciától — semmilyen szenzorpontosság nem javítja ki a rossz vagy elavult QNH-t. Ezt maga az Android dokumentálja, nem csak ebben az appban megfigyelt viselkedés.

**Hivatalos dokumentáció.** A `SensorManager.getAltitude(p0, p)` fölötti Javadoc az AOSP-ben (`frameworks/base/core/java/android/hardware/SensorManager.java`):

> „A tengerszinti nyomást ismerni kell [...] Ha ismeretlen, a `PRESSURE_STANDARD_ATMOSPHERE` közelítésként használható, de az abszolút magasságok nem lesznek pontosak.”

— és két magasság közti *különbség* számítására ajánlja a függvényt, nem az abszolút érték bizalmára. A metódusban (és ennek az appnak a `BaroAltitude.metersFromPressureHpa` függvényében) szereplő `44330` skálamagasság és `1/5,255` kitevő nem Android-specifikus: a Nemzetközi Standard Atmoszféra (ISA) hipszometrikus képletét valósítja meg, amelyet az ICAO szabványosított, ezzel egyenértékű az U.S. Standard Atmosphere, 1976 is.

**Hivatkozások:**
- Android API-referencia: [`SensorManager.getAltitude(float, float)`](https://developer.android.com/reference/android/hardware/SensorManager#getAltitude(float,%20float))
- AOSP forrás, szó szerinti Javadoc és implementáció: [`SensorManager.java`](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/hardware/SensorManager.java)
- ICAO Doc 7488, *Manual of the ICAO Standard Atmosphere* — a barometrikus képlet konstansainak forrása
- U.S. Standard Atmosphere, 1976 (NOAA / NASA / USAF) — ezzel egyenértékű standard atmoszféra modell

### Hogyan naplóz a Fut/túra, mint egy sportóra

Egy dedikált óra, például a Suunto Ambit 3 Peak, a **GNSS chipet** kb. másodpercenként rögzíti. Nem pattintja a vonalat utcára, és nem futtat telefonos „fused” szűrőt. A **GNSS** a műholdrendszerek családja (GPS, Galileo, GLONASS, BeiDou, QZSS, NavIC). A GPS egy konstelláció; a chip mindet használja.

A telefon **fused** hely API-ja más kérdésre válaszol: „hol van a felhasználó?” Műholdakat kever Wi-Fi-vel, cellával és IMU-val, majd simít. Egy 5–10 m-es hurok, amit tényleg az úton futottál, gyalogos zajnak tűnik, és **mielőtt** a GTL tárolná, ellapul.

A **Csak GNSS** az Android `GPS_PROVIDER`-t kéri (a chip, minden konstelláció — a név történeti). A letárolt pontok ezek a chip-pozíciók, ugyanaz az elv, mint az óra 1 s-os GPS-trackje. (A Suunto FusedSpeed tempó, nem a polyline. Ez az app nem valósít meg FusedTrack IMU-hézagkitöltést.)

A Fut/túra előbeállítás többi része ezt az alakot tartja meg SQLite-ban, a Térképen és a KMZ-ben:

- **Rögzített útvonal simítása ki** — nincs második, állandó sebességű Kalman, ami a kis kört mérési zajnak venné. A Kalman vissza is kapcsolható; Fut/túránál akkor extra folyamat-zaj járul hozzá, hogy egy 5 m-es hurok ne húzódjon az utcára.
- **Minden jó** — tárolás kb. 500 ms-enként (beállítás min. idő), vagy kanyarban hamarabb. Ha a GPS irányszög 0 (kocogáskor gyakori), az irány jöhet a szomszédos pozíciókból.
- **0,5 m** ismétlődés-eldobás (járműveknél 1 m marad), hogy a szűk hurok megtartsa a csúcsokat.
- **Útvonal egyszerűsítése a térképen ki** — a kirajzolt vonal minden letárolt pont.

A pontossági és műhold kapuk továbbra is eldobják a rossz fixeket. Ez **nem** kevesebb műholdas fixet tárol; a jókból többet tárol, és leállítja a Wi-Fi/cella/fused találgatást track-forrásként.

### Kalman-szűrő (hogyan simulnak a letárolt pontok)

**Cél.** GPS-jitter vágása autós vagy repülős tracken (körforgalom kerek, cruise tiszta vonal) anélkül, hogy a Fut/túra nyolcasa ellapulna, és anélkül, hogy állás közben 10 m-es firka keletkezne. A Kalman **zajszűrő**: az elfogadott pontokat **mozgatja**, és **ugyanannyit tart**. Nem map-matching (nincs OSM/Google utcára pattintás), és nem Douglas–Peucker (a DP **eldob** csúcsokat, és csak a Térkép fülön).

**Hol ül a láncban.** Egy `KalmanTrackFilter` Indít→Leállít munkamenetenként, a `:engine`-ben. A `TrackingForegroundService` minden helyfrissítésre ezt csinálja:

1. A fixet a HUD-ra másolja (`lastLocation`). A világos lila pontossági kör mindig ezt a **nyers** pontot követi.
2. Eldobja a fixet, ha a pontosság a usage kapunál rosszabb (30 m, Fut/túránál és kerékpárnál 45 m), vagy a fixben lévő műholdak száma 4 alatt van. Az elutasított fixek nem jutnak Kalmanba és SQLite-ba.
3. Ha a **Rögzített útvonal simítása** be van, lefut a `KalmanTrackFilter.observe`. A szűrő új lat/lon-t ad. Az időbélyeg, magasság, pontosság és műholdszám a GPS-fixé marad. A sebesség és az irányszög a szűrő sebességéből jön, ha az legalább 0,3 m/s. Fut/túra és gyalogos extra helyzet-folyamat-zajt kap, hogy egy 5 m-es hurok ne húzódjon a húrra.
4. A **Rögzítés sűrűsége** (`FixAcceptance`) dönti el, hogy ezt az (esetleg simított) pontot **beírja-e**. Ha a hézag túl kicsi, a Kalman-állapot ettől még frissül, de a Room nem kap sort.
5. Leállításkor a STOP sor az utolsó **elfogadott** letárolt pont, hogy a Térkép, a KMZ és a GPX a logon végződjön.

Tehát a Kalman azt változtatja, **hol** ülnek a letárolt pontok. A sűrűség azt, **hány** van belőlük. A térkép-egyszerűsítés **egyiket sem** — csak a Térképen kirajzolt polyline-t ritkítja.

**Hogyan működik a szűrő.** Állandó sebességű modell helyi méterben (`GeoProjection`, ugyanaz a `111_320` m/fok, mint a DP). Az állapot `[east, north, vEast, vNorth]`. A GPS-mérés **csak helyzet** (nincs sebesség/irányszög-update). Minden lépés:

1. **Predikció** — az állapotot `dt`-vel előrelépteti (kis tartományra szorítva, hogy a GNSS-szünet ne robbanja a kovarianciát).
2. **Folyamat-zaj** `q` (m²/s⁴) = `baseQ(usage) × strengthMultiplier(slider)`. Majd `× turnBoost(usage)`, ha az irányszög az előző **kimeneti** irányszöghöz képest (vagy a szomszédos pozíciókból számolt irány, ha a GPS irányszög 0) több mint 15°-ot változik. **Magas** `q` **= jobban bízik a GPS-ben = kevesebb simítás.** Alacsony `q` = jobban bízik a mozgásmodellben = simább ívek, több késés, amikor tényleg fordulsz.
3. **Update** — Joseph-formájú Kalman-update, mérési σ = max(GPS pontosság, 2 m).
4. **Ugrás** — ha az innováció nagyobb, mint `max(50 m, 8 × pontosság)` (alagút-kijárat, GPS-teleport), újrainicializál az új fixre. A hézagot **nem** interpolálja.
5. **Álló zár** (ha be van) — ha a GPS-sebesség vagy a prediktált sebesség a usage pauza-küszöb alatt van (0,25 m/s Fut/túra, 0,4 m/s járművek), és az elmozdulás 1,5 m alatt, befagyasztja az utolsó kimenetet, nullázza a sebességet, zsugorítja a helyzet-kovarianciát.

Alap `q` a csúszka közepén (régi Közepes): Fut/túra 8,0, kerékpár 6,0, motor 2,5, autó/hajó 1,5, repülő 0,8. Fordulási boost: Fut/túra 10, kerékpár 8, motor 5, autó/víz 3, repülő 2. Erősségcsúszka `t` a `[0, 1]`-ben (Alacsony→Magas) a `q`-t `4^(1 − 2t)`-vel szorozza: Alacsony ×4, közép ×1, Magas ×0,25.

**Szándékosan nincs implementálva.** OSM/Google utcára pattintás, RTS előre–hátra simító, IMU holtpontszámítás / Suunto FusedTrack hézagkitöltés, megjelenítési spline-ok.

### A beállítások hatása a tracklogra

Ezek a vezérlők változtatják a SQLite `gps_events` táblát, az Útvonal odométert / sebességeket és a megosztott KMZ-t. Minden más csak megjelenítés.


| Beállítás                                                              | Beíródik a tracklogba?          | Hatás                                                                                                                                                                                                                                                                                                                                                                                                |
| ---------------------------------------------------------------------- | ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Csak GNSS**                                                          | Igen (forrás)                   | **Be:** `GPS_PROVIDER` műholdchip-pozíciók (minden GNSS-konstelláció). **Ki:** Play Services fused HIGH_ACCURACY. Ha a Csak GNSS be van és a GPS ki, nincs fused tartalék.                                                                                                                                                                                                                            |
| **Rögzített útvonal simítása**                                         | Igen                            | **Be:** minden jelölt pont Kalman-simított a sűrűség előtt. Útvonal, Térkép (nyers polyline) és KMZ a simított utat mutatja. **Ki:** a helyforrás a pontosság/műhold kapu után változatlanul tárolódik (Fut/túra alap).                                                                                                                                                                                  |
| **Simítás erőssége** (Alacsony–Magas csúszka; csak ha a simítás be van) | Igen                            | **Alacsony:** a GPS-jitter megmarad, a nyolcas és a zigzag is. **Magas** (repülő alap): körforgalom és cruise tisztább; a hajtű kicsit késik. Közép a motor/autó/víz.                                                                                                                                                                                                                                |
| **Álláskor ne vándoroljon a pont**                                     | Igen (csak ha a simítás be van) | Pauza-sebesség alatt a letárolt koordináta nem kóborol. Egy 6 m-es GPS-csoport pirosnál egy pont felé omlik. Magában nem dob el sorokat — a sűrűség dönti el az írást.                                                                                                                                                                                                                                |
| **Rögzítés sűrűsége** (Okos–Minden jó csúszka)                         | Igen                            | **Okos:** ír, ha a távolság az utolsó **eltárolt** ponttól eléri a 2014-es sebességsávot (kanyarban fele; Fut/túra Okos újra fele, min. 1 m). Autópályán kevesebb pont; gyalogláskor több. **Minden jó:** ír kb. `minTime`-onként (500 ms), vagy kanyarban hamarabb. Járműveknél az **1 m**-nél közelebbi halmok hullanak; Fut/túránál **0,5 m**. A csúszka közepe a két szabályt keveri.                    |
| **Útvonal egyszerűsítése a térképen** (1–20 m csúszka)                 | **Nem**                         | Kevesebb csúcs csak a Térkép fülön. A letárolt pontok, az odométer és a KMZ változatlan.                                                                                                                                                                                                                                                                                                             |
| **Pontossági jelzés megjelenítése**                                    | **Nem**                         | Világos lila kör a **nyers** GPS-fixen, akkor is, ha a Kalman be van.                                                                                                                                                                                                                                                                                                                                     |
| **Pontfelhő**                                                          | **Nem**                         | Pasztell magenta pöttyök a nyers HUD-fixekből állva, CEP95 a centroid körül. Alapból ki. Bekapcsoláskor a pontossági jelzés is bekapcsol; kikapcsoláskor csak a felhő tűnik el. Mozgás közben szünetel. Nem tárolódik.                                                                                                                                                                          |
| **Mértékegység**                                                       | Csak címkék                     | Metrikus / angolszász / ICAO formázza az Útvonalat és a KMZ balloonokat (metrikus km/h, m, °C; angolszász mph, ft, °F; ICAO kt, ft, °C). A koordináták WGS-84 maradnak. A repülő és hajó előbeállítás ICAO-t választ.                                                                                                                                                                                                                                                |
| **QNH** (900–1100 hPa)                                                 | Baro íráskor; KMZ megosztáskor  | Az élő baro, a magasságprofil szaggatott vonala és a KMZ balloon / ExtendedData `baro` a `getAltitude(QNH, nyomás − offset)` (KMZ megosztáskor). Ha ez több mint 1500 m-re van a GPS-magasságtól, a letárolt íráskori `baroAltitude` marad, vagy a baro kimarad. A sorra írt `baroAltitude` az akkor érvényes QNH és offset; a nyers `pressureHpa` változatlan. A **Kalibrálás GPS-ből** DataStore offsetet ír (±10 hPa), a csúszkát nem. Alap ISA 1013,25. |
| Pontossági / műhold kapuk                                              | Igen (elutasítás)               | A 30 m-nél (Fut/túránál és kerékpárnál 45 m) rosszabb, vagy 4-nél kevesebb műholdas fix Kalman előtt eldobódik. Nincs Settings-csúszkaként megjelenítve.                                                                                                                                                                                                                                                                |


**Gyakorlati eredmény.** Motor alap: fused + simított utcai track, Okos térköz, térképvonal 6 m-en ritkítva. Fut/túra és kerékpár alap: GNSS chip, nincs Kalman, majdnem minden jó fix tárolódik (0,5 m padló), a Térkép minden letárolt csúcsot mutat, hogy a kis úttest-hurok látható maradjon. Repülő alap: erősebb simítás, Okos térköz, 15 m térkép-ritkítás, sebesség/távolság csomóban és tengeri mérföldben.

### Rögzítés sűrűsége

Független a Kalmantól. A Kalman simítás bekapcsolva minden pontosságon átment fixet lát; a sűrűség csak a **tárolást** kapuzza.

- **Okos** (járművek): pontot ír, ha a haversine-távolság az utolsó **eltárolt** fixtől eléri a 2014-es sebességsávot; kanyarban annak a fele. Fut/túra Okos ezt a sávot újra felezi (a gyaloglás/kocogás túl durva volt egy kis nyolcashoz).
- **Minden jó** (Fut/túra és kerékpár alap): elfogad, ha eltelt a `minTimeMillis`, vagy az irányszög kanyarban van. Járműveknél az 1 m-nél közelebbi halmok hullanak; Fut/túránál és kerékpárnál 0,5 m.
- **A csúszka közepe:** a szükséges távolság az Okos sáv és a Minden-jó padló keveréke (1 m jármű, 0,5 m Fut/túra és kerékpár); a min-idő / kanyar út is elfogadhat egy pontot.



### Douglas–Peucker (térkép-egyszerűsítés)

**Cél.** Kevesebb csúcsot kelljen a Térkép fülnek rajzolnia. Egy hosszú munkamenetben több ezer letárolt fix lehet; ezek nagy része majdnem egyenesre esik. A köztesek eldobása reszponzívvá tartja a térképet anélkül, hogy a felvétel megváltozna.

Ez **csak megjelenítés**. A `gps_events`, az Útvonal odométer / sebességek és a KMZ export mindig a Room-sorokat használja (már Kalman-simítva, ha az a beállítás be van). A Douglas–Peucker nem simítja a GPS-zajt: a megmaradó sarkok élesek maradnak. Csak azokat a pontokat dobja el, amelyek elég közel vannak egy húrhoz.

**Mikor fut.** Beállítások → **Útvonal egyszerűsítése a térképen** (`optimizationActive`; járműveknél be, Fut/túránál és kerékpárnál ki). Naplózáskor a `GtlViewModel` ezt a kapcsolót használja. A **Térképen** először a session usage-ét írja a Beállításokba, utána a kirajzolt vonal a jelenlegi csúszkákat követi, ezért a usage váltása más módban mutatja ugyanazt a logot. A tűrés **1–20 m** csúszka (1 m-es lépés; motor alap 6 m, autó 8 m). A `DouglasPeucker.clampTolerance` íráskor továbbra is pattint és szorít.

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

### Tesztek

Az eszközön tárolt hibanapló alkalmazástesztjei (`./gradlew :app:testDebugUnitTest`):

- `ErrorLogStoreTest` — egy bejegyzésben megvan az UTC időbélyeg, a művelet, egy veremkeret és a `Caused by` sor. A plafon fölötti írás `errors.log.1` névre nevezi a fájlt, és újat kezd; az olvasás előbb a régebbi fájlt adja. A **Törlés** mindkét fájlt törli. A sikertelen írás nem dob kivételt.
- `ErrorLogExceptionsTest` — az `IOException`, az `SQLException`, az üzenet nélküli `IllegalArgumentException`, az `IllegalStateException` okozati lánca (`IOException`, alatta `IllegalArgumentException`) és az `OutOfMemoryError` megtartja az üzenetet és a teljes vermet. Több bejegyzés az írási sorrendben marad. Az aszinkron `record` út kiírja az `IOException`t.
- `ErrorLogTapTest` — a hetedik érintés két másodpercen belül megnyitja a naplót; a hosszabb szünet nullázza a számlálót.

### Stack

Kotlin 2.2 · AGP 9.2 · Compose BOM 2025.12 · Room 2.7 · DataStore · Navigation Compose · Play Services Location / Maps · Maps Compose · Mapsforge 0.25 · WorkManager · KSP

---



## Technikai dokumentumok


| Dokumentum                                                                     | Mi ez                                                                                                               |
| ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------- |
| [CHANGELOGS.md](CHANGELOGS.md)                                                 | Kanonikus verzióelőzmény (2.0.0 újraírás → Unreleased, angol és magyar)                                            |
| [docs/play-console/whatsnew.txt](docs/play-console/whatsnew.txt)               | Play Console kiadásnév és EN/HU what’s-new szöveg                                                                   |
| [docs/play-console/privacy-policy.html](docs/play-console/privacy-policy.html) | Adatvédelmi tájékoztató (az élő KLHome-oldal helyi másolata)                                                        |
| [docs/play-console/feature-graphic.png](docs/play-console/feature-graphic.png) | Play Áruház feature graphic                                                                                         |
| [docs/screenshots/](docs/screenshots/)                                         | Play listing képernyőképek (GPS, útvonal, térkép/tracking, iránytű, beállítások, mentett útvonalak, súgó, névjegy, Google Earth KMZ) |
| [docs/DBSTRUCT-en.md](docs/DBSTRUCT-en.md)                                     | SQLite séma: nyomvonal `gtl.db` és hely-cache `map-search.db` (mikor jön létre, mikor ürül, mikor indexelődik)     |
| [docs/GPSDATAFLOW-en.md](docs/GPSDATAFLOW-en.md)                               | GPS figyelés → szűrés → Room → UI / KMZ (EN); a naplózási lánc mermaidje                                           |
| [docs/GPSDATAFLOW-hu.md](docs/GPSDATAFLOW-hu.md)                               | GPS figyelés → szűrés → Room → UI / KMZ (HU)                                                                        |
| [docs/all-gps-systems-hu.md](docs/all-gps-systems-hu.md)                       | GNSS rendszerek és sávok: GPS L1/L5, Galileo, BeiDou, GLONASS, QZSS, NavIC (HU, webes referencia)                   |
| [docs/dp-kalman-smoothing-en.md](docs/dp-kalman-smoothing-en.md)               | Eredeti Kalman implementációs brief; **as-built jegyzetek felül** (a jelenlegi viselkedés ez a README)              |


Érdemes elolvasni ezeket az engine belépési pontokat:

- `engine/.../FixAcceptance.kt` — pontosság / műhold / Okos vagy Minden jó sűrűség
- `engine/.../KalmanTrackFilter.kt` — állandó sebességű simító (letárolt pontok)
- `engine/.../UsageSmoothingDefaults.kt` — usage előbeállítás (csak GNSS, Kalman, sűrűség, térkép-egyszerűsítés)
- `engine/.../SpeedAdaptiveSpacing.kt` — méter a pontok között km/h és kanyar szerint
- `engine/.../DouglasPeucker.kt` — csak térképes polyline-egyszerűsítés (méter, helyi vetület)
- `engine/.../TrackStats.kt` — odométer, mozgás vs várakozás
- `engine/.../KmlExporter.kt` + `KmzExporter.kt` — KMZ helyi ikonokkal (`IconStyle` scale 0.8), clampToGround, HTML balloon
- `engine/.../KmlDescriptions.kt` — Start / Pause / Stop Earth details (dátumidő, temp, lon/lat, Altitude, Baro, Speed / Avg. Speed / Max speed, duration, distance)
- `engine/.../TrackLogExport.kt` — path vs Start/Pause/Stop markerek KMZ-hez és GPX-hez; KMZ baro a `pressureHpa`-ból a megosztáskori QNH-val (`displayedMeters`, 1500 m GPS-őr)
- `engine/.../GpxExporter.kt` — GPX 1.1 `trk` / `trkseg` / `trkpt` + Start/Pause/Stop `wpt`
- `engine/.../Gnss.kt` — konstelláció / L1 vs L5 / SNR / műholdlista
- `engine/.../Skyplot.kt` — polar projekció / kétfrekvenciás összevonás
- `engine/.../GpsAltitude.kt` — MSL, majd GNSS, majd fused; −430…20000 m-en kívül eldobva
- `engine/.../BaroAltitude.kt` — ISA / QNH méter a `pressureHpa`-ból; `displayedMeters` / `pickDisplayed` (1500 m a GPS-hez képest)
- `engine/.../OsmMapFile.kt` — Mapsforge mágia + header fájlméret
- `engine/.../OsmMapCamera.kt` — OSM közép/zoom a `.map` boundsön belül; locate cél
- `engine/.../MapCameraMode.kt` — idle szabad húzás, követés naplózáskor, mentett/teljes track illesztés
- `engine/.../OsmMapViewRedraw.kt` — mikor kell a Compose-nak OSM csempét invalidálni
- `engine/.../OsmRenderOptions.kt` — OSM rétegkategóriák a Mapsforge theme-hez
- `engine/.../MapFitZoom.kt` — zoom szorítás / fit méretellenőrzés
- `engine/.../TrackEndpoints.kt` — zöld S / piros E (vég rejtve naplózáskor)
- `engine/.../FixCloud.kt` — memóriabeli állóhelyi pontfelhő / CEP95
- `engine/.../MapDisplayUsage.kt` — melyik usage és egyszerűsítés szerint rajzol a térkép
- `engine/.../MapTrackVisibility.kt` — mikor kell a térképnek tracket rajzolnia (naplózáskor mindig; különben last-track vagy kijelölt session, hacsak nem ürítették)
- `app/.../LocationClient.kt` — fused HIGH_ACCURACY vagy `GPS_PROVIDER`, ha a Csak GNSS be van; fused mellett a `GPS_PROVIDER` a magassághoz is figyel

---



## Play listing képernyőképek

`docs/screenshots/`

- `gps-idle.png` — GPS fül: konstelláció-chippek, SNR, polar skyplot, GPS / Baro magasság, alsó fülek (idle, GNSS-fixre vár). Újra véve 2026-09-12.
- `gps-logging.png` — régebbi GPS fül naplózás közben (skyplot előtti elrendezés, 460×1024)
- `route.png` — Útvonal összesítők, dőlés, GPS/baro magasságprofil, alsó fülek. Újra véve 2026-09-12.
- `map.png` — Teljes telefonkép: Térkép kirajzolt trackkel, zöld S / piros E, idle Map HUD (sebesség, hely, pontosság, GNSS used/in view), Google Maps, alsó fülek. Újra véve 2026-09-13 (1080×2160).
- `tracking.png` — régebbi Térkép felvétel közben (S/E előtt, 460×1024)
- `googleearth.png` — megosztott KMZ a Google Earth-ben
- `compass.png` — Iránytű MAG / TRUE rózsa, alsó fülek. Újra véve 2026-09-12.
- `about.png`
- `settings.png` — Teljes telefonkép: hat használati mód (Hajó), QNH 1023 hPa Calibrate / Reset, OSM, egyszerűsítés, Csak GNSS, simítás, álláskor ne vándoroljon, rögzítés sűrűsége. Újra véve 2026-09-13 (1080×2160).
- `saved-tracks.png` — Mentett útvonalak: Térképen, Magasság, Törlés, GPS/baro profil. Újra véve 2026-09-12.
- `settings-density.png` — Régebbi Beállítások elrendezés egyszerűsítő / simító csúszkákkal (2.0.3)
- `help.png` — Súgótémák (2.0.3)
- `app-icon.png`

Telefon listing: 24 bites PNG, nincs alfa. A korábbi képek (`gps-idle.png`, `route.png`, `compass.png`, `saved-tracks.png`) 1080×1920 (9:16, chrome levágva). A `map.png` és `settings.png` a teljes eszközkép 1080×2160-ra skálázva (Play: a hosszú oldal = 2× a rövid; UI nincs vágva). Ezzel a kiadással töltsd fel ezt a hatot. A naplózás közbeni HUD-os Térkép és a feature graphic a sötét csempére vár (lásd a roadmapet).

---



## Következő teendők

- Világos és sötét téma

---



## Ami nincs ebben az appban

Szándékosan nem került át 2014-ből (szabály vagy halott API): IMEI / `READ_PHONE_STATE`, élő lat/lng feltöltés, follow-me weboldal, távoli feloldás, Google Directions, app által kapcsolt GPS/Wi-Fi, boot auto-start.
