# Térképkereső javítások

A távolság- és keresőfunkció első változatát régi Android Kotlin gyakorlat szerint javítottuk. A `map-search.db` származtatott cache: a `.map` fájlból újraépíthető, a nyomvonal-adatbázist nem érinti. A séma a 2-es verzió, exportálva az `app/schemas` alá. Ha egy helyi debug-példányon még az 1-es séma van, a Room azt eldobja és üres cache-ből indul, majd a WorkManager újraolvassa a térképet.

## 1. Az index túléli a folyamatot

**Hol:** `MapSearchRepository`, `MapSearchIndexWorker`, `MapIndexStateEntity`, `IndexResume`.

**Mi volt a hiba:** A bejárás egy soha le nem állított `CoroutineScope`-ban futott az alkalmazás folyamatában. A `done` jelző csak a teljes bejárás végén lett igaz. Ha addig a rendszer megölte a folyamatot, a következő indulás törölte a már kiírt helyeket, és elölről olvasta a csempéket. Nagy fájlon ez soha nem ért véget, és minden hideg indulás újra nekifutott.

**Mit csinál a javítás:** A hosszú munka `MapSearchIndexWorker`, ugyanazzal a WorkManager-mintával, mint az OSM-letöltés. Egyedi work név a fájlútvonalra, `ExistingWorkPolicy.KEEP`, exponenciális backoff 30 másodperctől. Feltétel: az akkumulátor és a tárhely nem alacsony. A `map_index_state` őrzi a következő csempét (`subIndex`, `tileX`, `tileY`), a `truncated` jelzőt és a következő próbálkozás idejét. Kész indexnél a worker nem olvas csempét. Félbemaradt indexnél a helyek megmaradnak, és a kurzor folytatódik. Kivételkor a sorok nem törlődnek, az `attempt` nő, és a worker `Result.retry()`-t ad. Az `IndexResume.action` tiszta függvény dönt: kész, folytatás, várakozás vagy csonka. Erre unit teszt van.

**Miért ez a gyakorlat:** A percekig tartó, a folyamat halálát túlélő munka WorkManager-feladat, mentett kurzorral. A döntés az engine modulban van, Room és Mapsforge nélkül tesztelhető.

**Mit lát a felhasználó:** A kereső a már beírt helyeket adja, akkor is, ha az app közben kilépett. Alacsony töltöttségnél vagy kevés szabad tárhelynél az indexelés vár. Ismétlődő olvasási hiba után a felület jelzi, hogy a térkép most nem kereshető, és a munka később újrapróbálkozik.

**Maradó kockázat:** közepes. A WorkManager a Doze alatt elhalaszthatja a futást, ezért egy frissen választott nagy térkép nem azonnal kereshető végig. A kurzor csempénkénti, egy megszakított csempe újraolvasódik; az egyedi index a duplikátumot eldobja.

## 2. Memória a bejárás alatt

**Hol:** `MapSearchIndexer`.

**Mi volt a hiba:** Egy `HashSet` a futás végéig őrizte az összes már látott aliast, akár 250 000 helyig. Ugyanerre a `.map` fájlra a térképnézet is nyitva tartott egy Mapsforge `MapFile`-t, az indexelő pedig egy másodikat a teljes bejárás idejére.

**Mit csinál a javítás:** A memóriabeli készlet kikerült. A duplikátumot az egyedi index (`mapKey`, `nameFold`, `kind`, `gridLat`, `gridLon`) és az `OnConflictStrategy.IGNORE` tartja. A `MapFile` 32 csempénként nyílik és zárul. Sikeres köteg után a kurzor az adatbázisba kerül, a helyek 400-as adagokban íródnak. A nézet belső `MapFile`-ját nem osztjuk meg: a Mapsforge nem ad rá stabil, szálbiztos fogantyút.

**Miért ez a gyakorlat:** A deduplikáció az adatbázis feladata. A natív leképezés csak egy köteg idejére él, nem a teljes országfájl bejárásáig.

**Mit lát a felhasználó:** Semmit közvetlenül. A térkép húzása közben kisebb az esély, hogy az indexelés kifogyasztja a folyamat memóriáját.

**Maradó kockázat:** közepes. Egy köteg alatt a nézet és az indexelő egyszerre tarthatja nyitva a fájlt. A csúcs rövidebb, mint a teljes bejárás, de egy nagyon sűrű csempe még mindig nagy köteget adhat, amíg a 400-as írás le nem fut.

## 3. A keresőállapot nem számolja újra a nyomvonalat

**Hol:** `GtlViewModel`, `MapSearchSlot` a `MapPane`-ben.

**Mi volt a hiba:** A kereső `MapSearchUi` a nagy `combine`-ban ült, amely minden találatra és indexállapotra újraépítette a teljes képernyő-állapotot a főszálon: pontok, Douglas–Peucker, fájlolvasás.

**Mit csinál a javítás:** A `mapSearch` külön `StateFlow`. Csak a `MapSearchSlot` collectálja, és csak amíg a kereső nyitva van. A térkép és a nyomvonal ága ettől nem fut újra.

**Miért ez a gyakorlat:** A ritkán változó, drága UI-állapot és a gépelés közben változó lista két folyam. A Compose csak azt a levelet rajzolja újra, amelyik a kereső állapotát olvassa.

**Mit lát a felhasználó:** A találatlista frissül, a térképvonala és a kamera nem ugrik egy karakter leütésére.

**Maradó kockázat:** alacsony. A kereső megnyitásakor a slot az aktuális állapotot olvassa; ha a lista közben változik, csak a panel komponálódik újra.

## 4. A fájl olvashatósága nem a főszálon dől el

**Hol:** `GtlViewModel.observeActiveMapSearch`, `MapSearchRepository.activate`.

**Mi volt a hiba:** A `combine` átalakítója a főszálon hívta az `OsmMapFile.isReadable`-t (fájlhossz és fejléc), majd a kulcsból `substringBefore('|')` szedte vissza az útvonalat.

**Mit csinál a javítás:** A folyam csak az útvonalat és azt adja, hogy offline térkép van-e használatban. Az olvashatóság `Dispatchers.IO`-n fut. Az `activate` `File`-t kap.

**Miért ez a gyakorlat:** A lemez a háttérszálon van. A főszál csak a kész döntést látja. Az útvonal nem egy összerakott kulcs szétszedése.

**Mit lát a felhasználó:** Az indulás nem akad meg a `.map` fejlécének olvasásán.

**Maradó kockázat:** alacsony. Lassú tárnál az IO-szál vár, a felület addig a korábbi keresőállapotot mutatja.

## 5. Keresés: egy origó, hiba nem ragad be, rangsor a háttérben, FTS

**Hol:** `MapOrigin`, `MapSearchRepository.search`, `GtlViewModel.searchPlaces`, `MapPlaceFts`, `MapSearch.ftsMatch`.

**Mi volt a hiba:** A szélesség és a hosszúság két külön volatile mező volt, egy pillanatra keveredhetett. A `searching` zászló csak a sikeres ág végén ment le. A rangsorolás a Room után a főszálon futott. A második lekérdezés `LIKE '% token%'` volt, ami a 250 000 soros táblát végigolvasta.

**Mit csinál a javítás:** Az origó egy `@Volatile` `MapOrigin`. A `searchPlaces` `try/finally`: a `CancellationException` továbbmegy, minden más hiba leveszi a zászlót és üríti a listát. A `rank` `Dispatchers.Default`-on fut. A `nameFold` Room FTS4 tartalomtábla. A lekérdezés egy prefix (`"token"*`), a `mapKey` szűrés SQL-ben van. A redundáns `(mapKey, nameFold)` index kikerült. Az FTS-be csak betű és szám kerül, hogy a `%`, `_` és az FTS operátorok ne legyenek a kifejezésben.

**Miért ez a gyakorlat:** Egy referencia olvasható szétszakadás nélkül. A felület állapota hiba után is konzisztens. Az FTS a tokent indexből adja, nem táblabejárással. A haversine nem a főszálon számol.

**Mit lát a felhasználó:** A találatok ugyanúgy jönnek: erősebb név, azon belül közelebbi. Indexelés közben a már beírt sorok kereshetők. Hibás lekérdezés után a „keresés” állapot nem ragad be.

**Maradó kockázat:** közepes. Az FTS a leghosszabb tokent használja, ahogy a korábbi `LIKE` is. Két egyforma hosszú szó közül az első kerül a prefixbe. A többi szó a memóriabeli rangsorban számít, a jelöltek között.

## 6. A 250 000-es plafon nem „kész”, a régi térkép indexe nem marad bent

**Hol:** `MapSearchIndexer` a plafonnál `truncated = true`. `MapPlaceDao.deleteExcept` egy `@Transaction`. A fájl törlése leállítja a workot, majd tranzakcióban töröl.

**Mi volt a hiba:** A plafon elérésekor `done = true` lett, a felület késznek mutatta a csonka indexet, és a hátralévő csempék soha nem kerültek be. Térképváltáskor a régi fájl sorai bent maradtak.

**Mit csinál a javítás:** A plafon `truncated`, a kereső szövege: „A térképnek csak egy része van beolvasva” / „Only part of this map is indexed”. Másik térképre váltáskor minden más `path` törlődik egy tranzakcióban. A térkép törlése a workot is leállítja.

**Miért ez a gyakorlat:** A kész állapot csak a végigjárt fájlra igaz. A cache a használatban lévő térképhez kötött. A két törlés egy SQLite-tranzakció: vagy mindkettő megvan, vagy egyik sem.

**Mit lát a felhasználó:** Részleges indexnél a kereső megmondja. A nem használt térkép helyei nem gyűlnek a `map-search.db`-ben.

**Maradó kockázat:** alacsony. A plafon után a hiányzó nevek nem kereshetők, amíg a fájl cseréje vagy törlése új bejárást nem indít. A csonka index szándékosan nem folytatódik.

## 7. Húzás, kamera, koppintás

**Hol:** `TapAnchor`, `TapMenuHost`, `GtlOsmMapView`.

**Mi volt a hiba:** Minden `onMoveEvent` Compose-állapotot írt, amit a `MapPane` olvasott. Emiatt az `AndroidView` `update` blokkja is lefutott, és a végén újra csempét kért. Az `update` ráadásul kompozíció közben hívta a `publishAnchor()`-t. A keresés utáni `cameraHold` csak a Saját hely gombra oldódott. Térképfájl-váltáskor a koppintás menüje és a cél megmaradhatott.

**Mit csinál a javítás:** A képernyőpontot csak a `TapMenuHost` olvassa, ezért a húzás nem indítja újra a térkép `update` blokkját. A `publishAnchor()` kikerült az `update`-ből; a Mapsforge mozgás- és zoomeseménye, valamint a Google vetület frissíti a levelet. A menü továbbra is követi a pontot. Az első felhasználói húzás feloldja a `cameraHold`-ot. A programozott `setCenter` és a Saját hely körül egy elnyomó zászló van, és a következő képkockán engedjük el, hogy a saját kameraállítás ne számítson húzásnak. Másik `.map` útvonalra váltáskor törlődik a koppintás, a menü, a koordináta, a cím és a HUD-távolság. Google és offline között a távolság megmarad, mert ott a fájlútvonal nem cserélődik.

**Miért ez a gyakorlat:** A gyakran változó pont csak azt a kompozáblist invalidálja, amelyik olvassa. A térkép nézet frissítése a térkép saját eseménye, nem egy Compose-állapot írása a kompozícióban.

**Mit lát a felhasználó:** A menü követi a pontot, a húzás közben a térkép nem kér újra csempét minden lépésre. Navigálás után az első húzás vagy a Saját hely visszaadja a követést. Másik letöltött térképre váltva a régi cél és menü eltűnik.

**Maradó kockázat:** alacsony. Ha a Mapsforge a `setCenter` után, a zászló feloldása után küld még egy mozgáseseményt, az feloldhatja a holdot. A zászló a következő képkockáig tart, ez a szokásos ablak.

## 8. Felület

**Hol:** `MapSearchOverlay`, `MapHud`, `MapTapOverlay`, `MapTapLayer`, `values/strings.xml`, `values-hu/strings.xml`.

**Mi volt a hiba:** A fókusz kérése az első képkockán, mielőtt a mező a fában van, `IllegalStateException` lehet. A távolságsor 16 sp volt, tartalomleírás nélkül. A „Lon:”, „Lat:” és a gondolatjel fix sztring volt. A billentyűzet keresőgombja nem csinált semmit. A koppintásréteg akkor is elnyelte az érintést, ha nem volt figyelő.

**Mit csinál a javítás:** `withFrameNanos` után jön a `requestFocus()`. A távolságsor legalább 48 dp, a TalkBack a „Távolság törlése” / „Clear distance” címkét olvassa. A feliratok string erőforrások. A keresőgomb leveszi a fókuszt; a lekérdezés továbbra is gépelésre fut. Az `onTap` csak akkor ad `true`-t, ha a menü figyelője megvan.

**Miért ez a gyakorlat:** A fókusz a csatolt node-ra megy. A Material minimum célméret és a content description ugyanazt a műveletet adja ujjnak és felolvasónak. A két nyelv a meglévő erőforrás-páron megy.

**Mit lát a felhasználó:** A keresőmező megnyitáskor fókuszt kap. A HUD-távolság sort könnyebb eltalálni. A koordináta felirata továbbra is `Lon:` és `Lat:`, mindkét nyelven, de fordítható erőforrás. Nincs fix: gondolatjel.

**Maradó kockázat:** alacsony. A 48 dp a sor magassága; keskeny szöveg mellett a vízszintes cél a szöveg szélessége marad.

A táblák és az életciklus (létrehozás, sortörlés, újraindexelés) kanonikus leírása: [DBSTRUCT-en.md](DBSTRUCT-en.md#map-searchdb). Magyarul: [GPSDATAFLOW-hu.md](GPSDATAFLOW-hu.md#a-map-searchdb-nem-ez-a-lánc).

## 9. Séma

**Hol:** `MapSearchDatabase` version 2, `exportSchema = true`, KSP `room.schemaLocation` = `app/schemas`. Erre a cache-re `fallbackToDestructiveMigration(true)`.

**Miért:** A keresőadatbázis újraépíthető. Az exportált séma a 2-es verzió diffje. Egy 1-es debug-példány induláskor nem omlik össze: a táblák törlődnek, az index a `.map`-ből újraépül. A nyomvonal Room-adatbázisa változatlan.

**Maradó kockázat:** alacsony. Sémafrissítéskor a helyi keresőindex egyszer újraépül. A felhasználó addig a már üres cache-t látja, amíg a worker újra nem olvas.
