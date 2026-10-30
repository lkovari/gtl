# GTL Android — az iOS-jegyzetből mi valós ezen a projekten

Átnézés: 2026. október 3. Forrás: a `gtl-ios` tároló `gtl-ios-decline-fix-plan-hu.md` jegyzéke. Ez a fájl azt mondja meg, melyik pont használati vagy technológiai hibaként ezen az Android-projekten is megvan.

A munkapéldány állapota: `OsmCatalog.kt` staged változása a nyolc hibás letöltési utat már a szerver szerinti névre cseréli. A méretplafon, a felület és a rögzítés kódja ettől nem változik. A letöltési címeket ma HEAD kéréssel ellenőriztem (`User-Agent: GPS Track Logger`). Futást telefonon nem néztem; a zárolt képernyő és a Play Console űrlapjai nincsenek eszközön igazolva.

Az Apple-irányelvek (2.1, 2.3, MapKit, Mindig engedély, iPad kompatibilitási mód) ide nem illenek. Ahol a jelenség a kódban ugyanaz, azt leírom. Ahol csak az iOS-felépítés miatt létezik, azt röviden kizárom.

## Összefoglaló

| iOS pont | Ezen a projekten | Röviden |
| --- | --- | --- |
| C1 katalógus | Részben valós | A nyolc 404-es út a munkapéldányban javítva. Németország (3,23 GB) és Franciaország (3,50 GB) a 2 GiB-os plafon miatt nem tölthető le. A lista a koppintás előtt nem mutat méretet. Nincs hálózati katalógusteszt. |
| C2 mockup képernyőkép | Nem ez | A `docs/screenshots/` képek a futó appot mutatják. Több kép régebbi felületet őriz. |
| H1 megtagadott hely, néma Start | Valós, szűkebb | Végleges elutasítás után a Start nem ír semmit, és nem visz az app engedélyképernyőjére. A helyzetgomb ilyenkor ki van kapcsolva, nem halott koppintás. |
| H2 Mindig engedély, zárolás leállítja | Nem ez | Nincs háttér-helyengedély. A rögzítés előtér-szolgáltatás, értesítéssel. |
| M1 rejtett képernyők | Valós | Hétszeres koppintás a verzióra: hibanapló. Háromszoros koppintás a mentett útvonal dátumára: nyers `gps_events`. |
| M2 Mapsforge főszerver | Valós | Az OSM-régiók továbbra is a `download.mapsforge.org` címről jönnek. |
| M3 logó takarása | Részben valós | Google-térképen a HUD a bal alsó sarokban van, tartalommargó nélkül. Üres felirat-chip online módban nincs. |
| M4 iPad / iOS 27 | Nem ez | Nincs iPad-kompatibilitási mód. A GPS hardver kötelező, a tájolás álló. |
| L1 adatvédelmi címke | Nem igazolható | A helyi tájékoztató és a kód egy irányba mutat. A Play Data safety űrlap nincs a repóban. |
| L2 Start/Stop és mértékegység | Részben valós | Az Indít / Leállít magyar. A mértékegység-chipek `Metric`, `Imperial`, `Icao` mindkét nyelven. |
| L3 készüléknév | Más jelenség, valós | A névjegy a felhasználó által adott készüléknevet olvassa, nem egy fix „Android” szót. |
| L4 „EU” előtag | Valós | Ugyanazok a feliratok, nem EU-tag országokon is. |
| L5 AccentColor, bundle név | Nem ez | A launcher név be van állítva, magyarul is. |
| N1 háttérletöltés elveszik | Nem ez a hiba | A WorkManager a folyamat halála után is tartja a munkát. A nagy fájl elölről indul, értesítés nélkül. |
| N2 HTTP-státusz | Nem ez | A 2xx-en kívüli válasz hiba, a szemétfájl törlődik, a sor „A letöltés sikertelen”. |
| N3 export hibája csendben | Valós | A GPX/KMZ írás nincs elkapva, hibaüzenet nincs. |
| N4 adatbázis nélkül „megy” a rögzítés | Nem ez | A Room nyitása az induláskor van. Ha elhasal, az app nem úgy tesz, mintha rögzítene. |
| N5 első Start csak engedélyt kér | Nem ez | Az engedély megadása után a rögzítés azonnal indul. |
| N6 foglalt áruházi név | Nem ez | A Play a csomagnevet köti, nem egy egyedi címet. |
| N7 akkumulátor-mondat | Nem a repóból | A `whatsnew.txt`-ben nincs ilyen mondat. A teljes áruházi leírás nincs a tárolóban. |
| N8 Google Fonts a tájékoztatón | Valós | A `privacy-policy.html` a Google Fontsot tölti. |

## Valós vagy részben valós

### A nagy OSM-fájlok nem férnek a plafon alá (C1 maradéka)

A katalógus a `https://download.mapsforge.org/maps/v5` alatt van, az `OsmCatalog` 36 régiója plusz a Turistautak. A munkapéldány útjai a ma élő fájlnevek:

- Csehország: `europe/czech-republic.map` (200, 0,61 GB)
- Anglia, Skócia, Wales: `europe/united-kingdom/…`
- Írország: `europe/ireland-and-northern-ireland.map`
- Kalifornia, New York, Texas: `north-america/us/…`

Ezek 200-at adnak. Az utolsó commitban még a régi, 404-es utak vannak; a javítás staged, commit nélkül.

A plafon az `OsmDownloadBudget.MaxBytes`: 2 GiB. A mai HEAD:

| Régió | Méret | A 2 GiB-os plafon |
| --- | --- | --- |
| Németország | 3,23 GB | nem fér be |
| Franciaország | 3,50 GB | nem fér be |
| Brazília | 2,14 GB | még belefér, kevés tartalékkal |

A letöltő a `Content-Length` alapján még a másolás előtt elutasítja a túl nagy fájlt (`download too large`). A felület ezt nem mondja ki: a sor „A letöltés sikertelen” szöveget kapja, ugyanazzal a szöveggel, mint egy hálózati hibát. A méret csak akkor jelenik meg, ha a letöltés már fut és a `totalBytes` pozitív. Németországnál és Franciaországnál a felhasználó a koppintás előtt nem látja, hogy több gigabájtos fájlt indít, és utána sem látja, hogy a plafon állította meg.

Nincs teszt, ami a katalógus minden címére HEAD kérést küldene. A szerver átnevezése vagy egy fájl kinövése csak élesben derül ki.

A mobilhálózatra nincs külön megerősítés. A WorkManager feltétele csak az, hogy legyen hálózat (`NetworkType.CONNECTED`).

### Megtagadott helyengedélynél a Start néma marad (H1)

A Start a felső sávban van, minden fülön. Ha a precíz hely még nincs megadva, a rendszerablak megjelenik, és az engedély megadása után a `startLogging()` lefut. Ez rendben van.

Ha a felhasználó mindkét helyengedélyt elutasítja, a visszahívás nem ír ki semmit. Üzenet csak akkor van, ha a durva hely megvan, a precíz nincs: „A naplózáshoz precíz helymeghatározás kell”. Végleges elutasításnál („ne kérdezd újra”) a következő Start azonnal visszatér, párbeszéd és szöveg nélkül.

A menü „Helyzet beállításai” pontja a rendszer GPS-paneljét nyitja (`ACTION_LOCATION_SOURCE_SETTINGS`): a helymeghatározás ki- és bekapcsolását. Nem az app engedélyképernyőjét. Megtagadott engedélynél ez a panel nem adja vissza a jogot.

A Saját hely gomb fix nélkül le van tiltva, tehát nem tűnik működő, de néma gombnak.

### Két rejtett képernyő (M1)

A névjegyben a verziósor hétszeres koppintása, két másodpercen belül, megnyitja a hibanaplót. A képernyőnek van magyar címe („Hibanapló”), a súgó és a menü nem említi, külön sora nincs.

A mentett útvonal kártyáján a dátum háromszoros koppintása, fél másodpercen belül, megnyitja az adott menet pontjait. A címsor a nyers `gps_events` szöveget írja, a másolás gombja angolul „Copy”. A koppintás a dátum során van; a kártya többi része a magasságprofilt nyitja. A súgó „Egy letárolt trackpont” blokkja elmondja, hogy a pont a `gps_events` táblában van, azt nem, hogy a tábla hogyan nyílik meg.

### A Mapsforge főszerver (M2)

Az OSM-régiók alapcíme `https://download.mapsforge.org/maps/v5`. A szerver saját szövege szerint ez nem tömeges letöltésre való. A domborzat-jegyzet (`docs/OSM-HILLSHADE-hu.md`) az Esslingen-tükröt a HGT-fájloknál említi; a `.map` katalógus nem azt használja. Ha a főszerver korlátozza a forgalmat, minden régióletöltés egyszerre áll meg.

A tájékoztató „Mapsforge letöltőhelyet” ír, hosztnév nélkül. A kiszolgáló cseréjekor ezt a szakaszt akkor is követni kell.

### A Google-logó és a HUD ugyanabban a sarokban van (M3)

Online módban a térkép a Maps SDK `GoogleMap` nézete, `contentPadding` nélkül. A Google-logó alapból a bal alsó sarokban van. A HUD ugyanoda kerül, 12 dp margóval, amint van GPS-fix vagy fut a rögzítés. A `docs/screenshots/map.png` képen a logó még látszik a panel mellett; a kód nem tolja feljebb. A Maps SDK feltétele a látható, nem takart logó.

Online módban nincs üres felirat-chip. Az OSM-hozzárendelés csak offline térképen jelenik meg, a jobb alsó sarok fölött.

### „EU” előtag nem EU-tagokon (L4)

A lista feliratai kódban fix angol szövegek, nem a `countryCode` lokalizált neve. Svájc, Anglia, Skócia, Wales, Norvégia, Ukrajna és Szerbia is „EU” előtagot kap. Az előtag a kontinenst jelenti, az Európai Uniót nem. Japán, India, Brazília és Ausztrália előtag nélkül van.

### A mértékegység chipjei angolul maradnak (L2 része)

Magyar rendszeren a fő gomb „Indít” és „Leállít”. A felső címsor (`brand_title`) mindkét nyelven „GPS Track Logger”; a launcher magyar neve „GTL GPS útvonal napló”.

A Beállítások mértékegység-chipjei az enum nevét írják ki: `Metric`, `Imperial`, `Icao`. A súgó ugyanezt „metrikus”, „angolszász” és „ICAO” szavakkal mondja, és egy helyen „Route fülön” szerepel, miközben a fül felirata „Útvonal”.

### A névjegy a valódi készüléknevet mutatja (L3, fordítva)

Az iOS-jegyzetben a sor mindig „iPhone”, mert a rendszer a felhasználó által adott nevet nem adja ki. Itt a `DeviceIdentity` a `Settings.Global.DEVICE_NAME` értéket írja ki, és csak ennek híján esik vissza a gyártóra és a modellre. A névjegy tehát azt a nevet mutatja, amit a felhasználó a készüléknek adott. A tájékoztató ezt a nevet nem említi. A repóban ez a név nem megy fel a fejlesztő szerverére; a képernyőn viszont személyes adatként megjelenik.

### Az export hibája nem látszik (N3)

A `shareSessions` a GPX- vagy KMZ-írást `try/catch` nélkül hívja, majd megnyitná a megosztólapot. Ha az írás elhasal (tele a tárhely), a felhasználó nem kap szöveget, a lap pedig nem nyílik meg. Üres mentett útvonalnál a függvény csendben visszatér, sikerüzenet nélkül.

### A tájékoztató Google Fontsot tölt (N8)

A `docs/play-console/privacy-policy.html` a `fonts.googleapis.com` és a `fonts.gstatic.com` címről kéri az Outfit és a Fraunces betűtípust. A lap látogatójának IP-címe így a Google-höz kerül. A tájékoztató szövege a Google-t a térképcsempék és a Mapsforge-letöltés kapcsán említi, a betűtípus-kérést nem.

## Ami ezen a projekten nem ez a hiba

**Háttérrögzítés és „Mindig” engedély (H2).** A manifestben nincs `ACCESS_BACKGROUND_LOCATION`. A rögzítés a `TrackingForegroundService`, típusa `location`, folyamatos értesítéssel. A zárolt képernyő az iOS-os „csak használat közben” ágtól eltérően nem egy külön, szándékosan kikapcsolt háttérmód. A szolgáltatás `START_STICKY`. Azt, hogy egy adott gyártó akkukímélése ezt mégis megöli, a kód nem bizonyítja és nem cáfolja.

**Képernyőképek (C2).** A `docs/screenshots/` a futó felületet mutatja, nem rajzolt mockupot. A README szerint a `saved-tracks.png` a kártyás lista előtti elrendezés, a `gps-logging.png` és a `tracking.png` pedig régebbi, kisebb képek. A listing és a mai UI emiatt szétcsúszhat; ez nem a mockup esete.

**iPad és a legújabb iOS (M4).** Androidon a GPS hardver `required="true"`, tehát GPS nélküli táblagép a Playen ezt a csomagot nem kapja meg. Az activity álló tájolású. Kompatibilitási mód, ami egy telefon-appot táblagépen kicsinyítve futtat, nincs.

**Adatvédelmi címke (L1).** A kódban nincs Crashlytics és nincs saját feltöltés. A tájékoztató azt írja, hogy a nyomvonal a telefonon marad, a Google a csempéknél IP-címet láthat, a Mapsforge-letöltésnél a hoszt látja a kért fájlt. A Play Console Data safety válaszai nincsenek a repóban, ezért az iOS-os „a címke gyűjtést mond, a szöveg nem” ellentmondás innen nem állapítható meg.

**Launcher név és hangsúlyszín (L5).** Az `app_name` angolul „GPS Track Logger”, magyarul „GTL GPS útvonal napló”. Hiányzó AccentColor-figyelmeztetés nincs, ez iOS-eszköz.

**Háttérletöltés újraindítás után (N1).** Az OSM- és a Turistautak-letöltés WorkManager `OneTimeWorkRequest`, egyedi névvel. A folyamat kilövése nem ejti el a feladatot úgy, ahogy az iOS-os, lustán létrehozott `URLSession` teszi. A worker nem folytatja a félbemaradt bájtokat, és nincs saját előtér-értesítése. Egy több gigabájtos letöltést a rendszer elhalaszthat vagy megszakíthat, és a következő futás elölről tölti.

**HTTP-státusz (N2).** Az `OsmDownloadWorker` a 200–299-en kívüli választ hibának veszi, a `.part` fájlt törli, és `Result.failure` lesz. Nem Mapsforge tartalomnál az `OsmMapFile.isReadable` is eldobja a fájlt. A felhasználó a sikertelen sort látja.

**Adatbázis nélkül futó rögzítés (N4).** A `GtlDatabase.create` az `Application.onCreate` elején fut, `try?` nélkül. Ha a nyitás elszáll, az app az induláskor áll meg, nem vált a gomb Leállít-ra üres menettel. Rögzítés közben egy `SQLException` a `loggingError` állapotot állítja, és az Útvonal fülön megjelenik.

**Első Start (N5).** A helyengedély megadása a Start visszahívásában azonnal elindítja a rögzítést. Nincs olyan ág, ahol az első koppintás csak az ablakot nyitná, a másodikat pedig a felhasználóra bízná.

**Áruházi név (N6).** A telepített azonosító `com.lkovari.mobile.apps.gtl`. A Play nem követeli meg, hogy a „GPS Track Logger” cím egyedül a tiéd legyen.

**Akkumulátor-mondat (N7).** A repó Play-szövege a `docs/play-console/whatsnew.txt`. Ebben nincs mondat a háttérben futó GPS fogyasztásáról. A teljes áruházi leírás a Console-ban lehet, a tárolóból nem olvasható.

## Javasolt sorrend

A sorrend a felhasználói kárt követi: előbb az, ami a fő műveletet elnémítja vagy egy látható gombot hibára futtat, utána az, ami adatot vagy bizalmat érint, a végén a felirat és a listing.

1. **Megtagadott helyengedély.** A Start minden fülön ott van, és végleges elutasítás után nem válaszol. A menü a GPS-panelt nyitja, nem az app engedélyeit. Ez a rögzítés, az app fő funkciója.
2. **Németország és Franciaország, plusz a letöltés visszajelzése.** A 2 GiB-os plafon két nagy országot kizár, a sor pedig csak „A letöltés sikertelen”. Ugyanebben a lépésben a méret jelenjen meg a koppintás előtt, és a túl nagy fájl kapjon saját szöveget. A nyolc javított út már a munkapéldányban van; a plafon nincs emelve. Brazília szorosan a határ alatt van.
3. **A nagy letöltés viselkedése.** Értesítés, mobilhálózati megerősítés, és hogy egy megszakadt fájl ne elölről kezdődjön. A 2. pont nélkül a felhasználó még mindig vakon indít több gigabájtot.
4. **GPX és KMZ hiba.** Tele tárhelynél az írás elszáll, üzenet nélkül. A felhasználó azt hiheti, hogy a mentett útvonal nem osztható meg.
5. **A két rejtett képernyő.** A hibanapló és a `gps_events` tábla legyen menüpont, magyar címmel. A tábla véletlen háromszoros koppintással is megnyílik, angol „Copy” gombbal.
6. **Google-logó.** A HUD a bal alsó sarkot foglalja, tartalommargó nélkül. A térkép a mindennapi képernyő.
7. **Mapsforge-kiszolgáló.** A régiók a tömeges letöltésre nem szánt főszerverről jönnek. Amíg ez így marad, egy szerveroldali korlát az összes OSM-letöltést egyszerre állítja le. A tájékoztatót a választott hoszthoz kell igazítani.
8. **Készüléknév.** A névjegy a felhasználó által adott nevet mutatja, a tájékoztató nem szól róla. Vagy modell és rendszerverzió kerüljön ki, vagy a lap említse a nevet.
9. **Feliratok.** „EU” előtag a nem EU-tagokon; a chipek `Metric`, `Imperial`, `Icao`; a súgó egy helyen „Route fül”.
10. **Adatvédelmi lap betűtípusa.** A Google Fonts kérését rendszerbetűre cserélni, és a lap szövegét ehhez igazítani.
11. **Elavult képernyőképek.** A mentett útvonalak, a naplózós GPS és a régi tracking kép a mai felület előtt készült. Akkor kell újra venni, amikor a Play-lista frissül.
