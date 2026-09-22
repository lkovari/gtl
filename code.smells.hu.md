# GTL – Code smell és best practice audit — 2026-09-21

**Projekt:** GPS Track Logger (GTL), Kotlin + Jetpack Compose, `versionName` 2.0.11, `versionCode` 29
**Alap:** a mellékelt "Modern Android Development" útmutató (Kotlin/Compose/Coroutines/Room/WorkManager best practice katalógusa, ld. a dokumentum 15. fejezetét: *"Recognizing Bad Practices — A Code-Smell Catalogue"*).
**Hatáskör:** `app/` és `engine/` modul, forráskód + build konfiguráció.
**Módszer:** a guide smell-katalógusán (concurrency, lifecycle/leaks, state/architecture, UI/Compose, data/storage, security/platform, tooling) végigmenve grep + forrásolvasás a teljes fán.
**Megjegyzés:** ez a dokumentum *nem* a `gtl-review-20260920-hu.md` funkcionális hibalistáját ismétli meg (K1/K2/M1–M12 stb. ott vannak) — itt kizárólag a guide-ban katalogizált **kódstílus / architektúra / best-practice** eltéréseket gyűjtöttem, azok is, amik nem okoznak azonnali hibát, csak technikai adósságot.

---

## Rövid rangsor

| # | Súly | Tétel |
|---|------|--------|
| S1 | Magas | `CoroutineWorker` elnyeli a `CancellationException`-t (`catch (_: Exception)`) |
| S2 | Magas | Blokkoló fájl I/O a fő szálon KMZ/GPX exportnál |
| S3 | Közepes | "God ViewModel" — `GtlViewModel` GPS/GNSS/szenzor/DB/DataStore/WorkManager/export mindent maga vezérel |
| S4 | Közepes | Nincs DI keretrendszer — kézi service locator, konkrét osztályok, nehezen tesztelhető |
| S5 | Közepes | Room `exportSchema = false`, nincs séma-history, nincs migrációs teszt |
| S6 | Közepes | `collectAsState()` `collectAsStateWithLifecycle()` helyett két képernyőn |
| S7 | Alacsony (megelőző) | `LazyColumn` `items()` stabil `key` nélkül — ellenőrzés után: ma nincs igazolt aktív hiba belőle |
| S8 | Alacsony | Cleartext HTTP kivétel a `network_security_config`-ban |
| S9 | Alacsony (stílus) | Hardcode-olt szín a témán kívül — ellenőrzés után: nem dark-mode kontraszthiba |
| S10 | Alacsony | Nincs CI/CD, statikus elemzés (Detekt/ktlint/Lint-as-error), LeakCanary, StrictMode |

**Frissítés (2026-09-21):** kódellenőrzés után az S7 és S9 indoklását pontosítottam — a bennük leírt konkrét hibamechanizmus a jelenlegi kódból nem következik, mindkettő megelőző/stílus jellegű tétel maradt, súlyuk ennek megfelelően lejjebb került.

---

## Magas

### S1. `CoroutineWorker` elnyeli a `CancellationException`-t

- **Forrás:**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/tuhu/TuhuDownloadWorker.kt`, **27–56**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/data/maps/OsmDownloadWorker.kt`, **24–42**
- **Probléma:** Mindkét `doWork()` a teljes letöltés+kicsomagolás+fájlműveletet egy `try { … } catch (_: Exception) { … Result.failure() }` blokkba csomagolja. A `catch (_: Exception)` a Kotlin coroutine-okban dobott `CancellationException`-t is elkapja (az az `Exception` leszármazottja), mert nincs külön ágra bontva.
- **Miért probléma:** A guide 2.4.1 pontja explicit szabályként mondja ki: *"CancellationException must propagate… `catch (e: Exception)` around suspend code — swallows CancellationException, breaks structured concurrency"* (15.2, Concurrency). Ha a WorkManager megszakítja a munkát (user törli a letöltést, constraint már nem teljesül, `WorkManager.cancelWorkById`), a coroutine belső `CancellationException`-jét a worker elnyeli, lefuttatja a `purgeFailedAttempt`/`temp.delete()` cleanupot és **`Result.failure()`-t** ad vissza ahelyett, hogy a cancellation természetesen propagálna és a munka `CANCELLED` állapotba kerülne. Ez azt jelenti, hogy egy user-kezdeményezett megszakítás a WorkManager szemében sikertelen letöltésként jelenik meg (téves retry-logika, téves UI-visszajelzés `download.failed`-en keresztül).
- **Javítás:** A guide ajánlása szerint (`if (e is CancellationException) throw e`, vagy specifikus típusra szűkített catch): a `catch` ágban külön kezelni a `CancellationException`-t (rethrow), és csak a valódi I/O-hibákat (`IOException`, `IllegalStateException` a saját `error(...)` hívásokból) elkapni.
  ```kotlin
  } catch (e: CancellationException) {
      TuhuDownloadCleanup.purgeFailedAttempt(mapsDir, applicationContext.cacheDir)
      throw e
  } catch (e: Exception) {
      TuhuDownloadCleanup.purgeFailedAttempt(mapsDir, applicationContext.cacheDir)
      Result.failure()
  }
  ```
- **Miért oldja meg:** A structured concurrency szabálya helyreáll: a megszakítás megszakítás marad, a WorkManager helyesen jelöli a munkát `CANCELLED`-nek, a takarítás továbbra is lefut mindkét ágon.

---

### S2. Blokkoló fájl I/O a fő szálon KMZ/GPX exportnál

- **Forrás:**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/domain/KmlExportUseCase.kt`, **20–87** (`write()`, nem `suspend`, nincs dispatcher-váltás)
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/domain/GpxExportUseCase.kt`, **25–73** (ugyanaz)
  - Hívó: `app/src/main/java/com/lkovari/mobile/apps/gtl/viewmodel/GtlViewModel.kt`, **894–923** (`shareSessions`, `viewModelScope.launch { … exporter.write(...) … }`)
- **Probléma:** A `write()` metódusok szinkron, blokkoló módon írnak fájlt (`file.writeBytes(...)`, `file.writeText(...)`), ZIP-et csomagolnak (`KmzExporter.pack`), assetből olvasnak (`context.assets.open(...).use { it.readBytes() }`). Nincs `suspend` módosító, nincs `withContext(Dispatchers.IO)` sem a use case-ben, sem a hívó oldalon. A `viewModelScope.launch { }` alapértelmezett dispatchere `Dispatchers.Main.immediate`, tehát ez a teljes munka a UI szálon fut.
- **Miért probléma:** A guide 6.2 pontja kifejezetten kimondja: *"All file reads/writes belong on Dispatchers.IO inside a suspend function."* A 14.2 fejezet ("Jank & rendering performance") és 14.4 ("ANRs") szerint main-thread I/O a jank/ANR egyik fő oka. Egy több órás, sok ezer pontos track exportja (ZIP-csomagolás + fájlírás) érezhető UI-fagyást vagy akár ANR-t okozhat a "Megosztás" gombra kattintás után.
- **Javítás:** A `write()` metódusokat tegyük `suspend fun`-ná és csomagoljuk `withContext(Dispatchers.IO) { … }`-ba (vagy a hívó oldalon: `withContext(Dispatchers.IO) { exporter.write(...) }` a `GtlViewModel.shareSessions`-ben). Guide 8.2/13.2 mintája szerint dispatchert injektálva (ne hardcode-olva) a tesztelhetőség is megmarad.
- **Miért oldja meg:** A fájlrendszer- és ZIP-műveletek IO-poolra kerülnek, a UI szál szabad marad, nincs jank/ANR-kockázat nagy trackeknél.

---

## Közepes

### S3. "God ViewModel" — `GtlViewModel` mindent maga vezérel

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/viewmodel/GtlViewModel.kt`, teljes fájl (**117–949**, kb. 830 sor egyetlen osztályban)
- **Probléma:** Egyetlen `ViewModel` közvetlenül kezeli: GPS/GNSS location listenereket (**161–236**), szenzorokat (kompasz, gravitáció, hőmérséklet, nyomás, **193–248**), fix-cloud statisztikát (**250–305**), Room-lekérdezéseket és session-életciklust (**482–582**), DataStore-beállítások mind a ~35 setterét (**584–743**), OSM és Tuhu WorkManager letöltéseket (**777–888**), fájl-exportot (**890–923**). A fájl `android.location`, `androidx.room`, `androidx.datastore`, saját `engine` csomag és UI-only típusok importjait egyaránt tartalmazza.
- **Miért probléma:** A guide 3.2 pontja ezt nevesíti: *"Bad practice — the 'God Activity'/'God ViewModel'. Networking, DB access, JSON parsing, and formatting all inside one class. Smell: the file is 1,000+ lines… Split by responsibility into repository/use case/VM."* Ilyen méretű, sok felelősségű osztály nehezen tesztelhető izoláltan (egy unit teszthez az összes almodult — LocationClient, GnssStatusSource, TrackRepository, GtlPreferences, OsmMapStore, TuhuMapStore — fel kell építeni), és minden új feature ide kerül be, tovább növelve a csatolást.
- **Javítás:** A guide 3.2/3.3 rétegzett architektúrája szerint bontsuk use case-ekre / kisebb, fókuszált ViewModel-social-kollaborátorokra: pl. `LocationPreviewController` (GNSS/szenzor preview a 161–248 sorokból), `TrackSessionUseCases` (session lifecycle), `MapDownloadCoordinator` (OSM+Tuhu). A `GtlViewModel` csak ezeket orchestrálja és a `uiState`-et építi.
- **Miért oldja meg:** Minden felelősség önállóan, framework-mentesen (vagy legalább izoláltan) tesztelhetővé válik, és a jövőbeli funkciók nem egy már túlterhelt fájlba kerülnek.

---

### S4. Nincs DI keretrendszer — kézi service locator, konkrét osztályok

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/GtlApplication.kt`, teljes fájl (**20–71**)
- **Probléma:** Az `Application.onCreate()`-ben 13 `lateinit var` mezőt manuálisan példányosítunk (`database`, `trackRepository`, `preferences`, `gnssStatusSource`, `osmMapStore`, `tuhuMapStore`, …), mindegyik konkrét osztály (nincs `interface TrackRepository` / `interface GtlPreferences` absztrakció). A `GtlViewModel` és a `TrackingForegroundService` ezekhez `application as GtlApplication`-ön keresztül fér hozzá.
- **Miért probléma:** A guide 3.4 pontja ("Depend on interfaces, bind implementations… Tests inject a `FakeTodoRepository` with no framework") és a 17. fejezet migrációs táblázata ("Manual DI / singletons → Hilt") ezt nevesíti mint elhagyandó mintát. Mivel nincsenek interfészek, egy `GtlViewModel` unit teszthez nem lehet egyszerű fake-eket becsatolni — vagy a teljes `GtlApplication`-t kell mockolni/valódi Android környezetet indítani. Ez magyarázza, hogy az `app/src/test` alatt mindössze 5 tesztfájl van, miközben az `engine` modul (framework-mentes, tiszta Kotlin — ez helyesen van csinálva) egyetlen, kiterjedt tesztfájllal rendelkezik.
- **Javítás:** Vagy Hilt bevezetése (guide 3.4 mintája: `@Module`/`@Binds`/`@Singleton`), vagy — kisebb lépésként — legalább interfészek kivezetése (`interface TrackRepository`) úgy, hogy a `GtlApplication` az implementációt adja, de a fogyasztók (ViewModel, Service) az interfészre hivatkoznak, és konstruktor-paraméterként kapják meg (nem `application as GtlApplication`-ön keresztül).
- **Miért oldja meg:** Tesztekben fake implementációk köthetők be keretrendszer nélkül is, a modulok közötti csatolás csökken.

---

### S5. Room `exportSchema = false`, nincs séma-history, nincs migrációs teszt

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/data/db/GtlDatabase.kt`, **10–14**
- **Probléma:** `@Database(…, version = 4, exportSchema = false)`. Három kézzel írt `Migration` van (1→2, 2→3, 3→4), de nincs `schemas/` JSON export a repóban, és nincs `MigrationTestHelper`-alapú teszt egyikhez sem.
- **Miért probléma:** A guide 7.3 pontja: *"Best practice — write explicit Migrations and test them. Keep `exportSchema = true`, commit the generated JSON schemas, and add a Room migration test that opens an old schema and migrates forward."* `exportSchema = false` mellett a build-idejű ellenőrzés hiányzik: egy hibásan megírt jövőbeli migráció (rossz oszlopnév, hiányzó `ALTER`) csak futásidőben, a felhasználó eszközén bukik ki (`IllegalStateException` Room-tól), nem CI-ben.
- **Javítás:** `exportSchema = true`, a generált `schemas/` JSON-okat commitoljuk, és a guide 13.3 mintája szerint (`MigrationTestHelper`) minden migrációhoz írjunk tesztet, ami egy régi sémájú DB-t nyit meg és migrál előre.
- **Miért oldja meg:** A következő oszlop-/tábla-változtatás gépileg ellenőrizhető, mielőtt éles usereket érne.

---

### S6. `collectAsState()` `collectAsStateWithLifecycle()` helyett

- **Forrás:**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/ui/screens/SecondaryScreens.kt`, **784, 787, 788** (`OsmRow`: `observeDownload(...).collectAsState(...)`, `observeDownloadedRevision().collectAsState()`, `viewModel.settings.collectAsState()`)
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/tuhu/TuhuDownloadRow.kt`, **38, 41, 42** (ugyanez a minta a Tuhu-sornál)
- **Probléma:** Ugyanabban a fájlban (`SecondaryScreens.kt`) máshol helyesen `collectAsStateWithLifecycle()`-t használnak (**878, 895, 896** — `inspectDump`, `savedElevationId`, `savedElevation`), tehát a projekt ismeri és alkalmazza a helyes mintát, csak az OSM/Tuhu letöltés-sorokon nem konzisztens.
- **Miért probléma:** A guide 4.7.1: *"Bad practice — `collectAsState()` instead of `collectAsStateWithLifecycle()`. Plain `collectAsState` keeps collecting while the app is in the background, wasting work and risking updates to a stopped UI."* Ez a képernyő (OSM/Tuhu térképletöltés lista) pont olyan flow-kat figyel (letöltési progress, DataStore settings), amik háttérben is emittálhatnak — feleslegesen tartja élve a collectort a képernyő elhagyása/backgroundolás után.
- **Javítás:** Mind a 6 helyen `collectAsStateWithLifecycle()`-re cserélni (a szükséges import — `androidx.lifecycle.compose.collectAsStateWithLifecycle` — már használatban van a projektben máshol).
- **Miért oldja meg:** A collectorok automatikusan leállnak, amikor a képernyő nincs `STARTED` állapotban, nincs felesleges munka/battery-költség háttérben.

---

## Alacsony

### S7. `LazyColumn` `items()` stabil `key` nélkül — megelőző jellegű, nincs igazolt aktív hiba

- **Forrás:**
  - `app/src/main/java/com/lkovari/mobile/apps/gtl/ui/screens/SecondaryScreens.kt`, **775** (`OsmDownloadScreen`: `items(viewModel.regions()) { region -> … }`, elérhető stabil azonosító: `region.id`)
  - ugyanott, **921** (`TracksScreen`: `items(state.sessions) { session -> … }`, elérhető stabil azonosító: `session.id`)
- **Probléma:** Egyik `items(...)` hívás sem ad meg `key = { … }` paramétert, holott mindkét listaelem-típusnak van stabil, egyedi azonosítója.
- **Miért probléma (pontosítva, kódellenőrzés után):** Guide 4.4: *"Bad practice — `LazyColumn` items without a `key`. Without stable keys, Compose can't match items across updates; scroll position jumps, animations glitch, and recomposition counts inflate."* Az eredeti feltételezés — hogy a checkbox- vagy progress-állapot "átugorhat" egy másik sorra — **ellenőrzés után nem igazolható ebből a kódból**: a `TracksScreen`-ben (889–938. sor) a checkbox-állapot (`selectedIds`), a törlés-azonosító (`pendingDeleteId`) és a share-picker (`sharePicker`) mind képernyő-szintű, `session.id`-alapú állapotok, nincs a sor belsejében `remember`, ami pozícióhoz (slothoz) tapadna — minden recompositionkor újraszámolódik az azonosítóból. Az OSM-listánál (`OsmDownloadScreen`) a `viewModel.regions()` az `OsmCatalog.regions` statikus, futásidőben soha nem újrarendeződő katalógusa, tehát a pozíció- és azonosító-alapú identitás mindig egybeesik. Ami ténylegesen megmarad: az `OsmRow`-ban *van* egy helyi `var pendingDelete by rememberSaveable { … }` (795. sor), ami — ha a lista valaha dinamikussá/átrendezhetővé válna — `key` nélkül tényleg rossz sorra tapadhatna; ma, a statikus lista mellett ártalmatlan. Összességében ez a tétel ma **megelőző jellegű** (a hivatalos Compose-ajánlásnak való megfelelés, jövőbeli regresszió elleni védelem), nem egy jelenleg fennálló hiba.
- **Javítás:**
  ```kotlin
  items(viewModel.regions(), key = { it.id }) { region -> … }
  items(state.sessions, key = { it.id }) { session -> … }
  ```
- **Miért oldja meg:** Nem egy meglévő hibát szüntet meg, hanem kizárja ezt a hibaosztályt a jövőre nézve (pl. ha egy sorba később helyi `remember`/`rememberSaveable` kerül), és megfelel a Compose hivatalos ajánlásának.

---

### S8. Cleartext HTTP kivétel a `network_security_config`-ban

- **Forrás:** `app/src/main/res/xml/network_security_config.xml`, **4–6**; felhasznált URL: `app/src/main/java/com/lkovari/mobile/apps/gtl/tuhu/TuhuCatalog.kt` (`http://turistautak.elte.hu/…`)
- **Probléma:** A globális `cleartextTrafficPermitted="false"` mellett egy explicit domain-kivétel engedi a cleartext HTTP-t a `turistautak.elte.hu` hosztra, mert a Tuhu térkép-zip csak HTTP-n érhető el.
- **Miért probléma:** Guide 8.5 / 15.2: *"`cleartextTrafficPermitted="true"` to dodge a cert issue → MITM exposure → fix the cert; HTTPS only."* Ez itt nem "cert-hiba megkerülése", hanem egy harmadik fél (egyetemi/civil) szerver, aminek nincs TLS-e — de a smell-kategória ugyanaz: a letöltött ZIP tartalma (térkép + illesztett render-téma) hálózati útközben cserélhető. (A részletes kockázatelemzést és javaslatot — redirect-korlátozás, méretplafon — a `gtl-review-20260920-hu.md` M1 tétele tartalmazza; itt csak a guide-minta egyezését jelzem.)
- **Javítás:** Ld. a másik review M1 javaslata: ha a forrás sosem kap TLS-t, a kivétel dokumentált, tudatos kompromisszum marad — de host+path allowlist és redirect-tiltás nélkül a guide checklistája szerint még mindig hiányos.
- **Miért oldja meg:** (ld. ott)

---

### S9. Hardcode-olt szín a témán kívül — stílus-konzisztencia, nem dark-mode kontraszthiba

- **Forrás:** `app/src/main/java/com/lkovari/mobile/apps/gtl/ui/screens/MapPane.kt`, **176–184** (`color = Color(0xFF333333)` a 184. sorban)
- **Probléma:** A projekt egyébként következetesen téma-tokeneket használ (`StartBlue`, `TrackingOrange`, `TitleMagenta`, `AmberFix` a `ui/theme` csomagból, ld. `MainTrackerScreen.kt` importjai) — ez az egy hely egy nyers `Color(0xFF…)` literált tartalmaz.
- **Miért probléma (pontosítva, kódellenőrzés után):** Guide 4.6: *"Bad practice — hard-coded colors/dimensions… scattered in UI code. Centralize in the theme; otherwise dark mode and rebranding become a find-and-replace nightmare."* Az eredeti feltételezés — hogy ez dark módban kontraszt-problémát okozhat — **ellenőrzés után téves**: a felirat (181. sor) egy explicit `Color.White.copy(alpha = 0.78f)` hátterű, lekerekített chipen ül, amit a térkép fölé rajzolunk; ez egy térkép-overlay attribution-címke, ami szándékosan **nem** követi az app rendszertémáját, hanem mindig fehéres alapon jelenik meg. A sötétszürke szöveg emiatt sötét módban is ugyanazon a fehér alapon marad — a kontraszt nem sérül. Ami ténylegesen megmarad: guide 4.6 szerint ez az egyetlen nyers `Color(0xFF…)` literál a `ui/theme` csomagon kívül — tisztán **stílus-konzisztencia** kérdése (ha egyszer a márkaszíneket vagy az attribution-chip stílusát módosítod, ezt az egy helyet külön kell megkeresni), nem funkcionális vagy olvashatósági hiba.
- **Javítás:** Vegyük fel a `ui/theme` csomagba névvel ellátott tokenként (pl. `MapAttributionText`), és onnan hivatkozzuk.
- **Miért oldja meg:** Nem hibát javít, hanem kozmetikai konzisztenciát ad: egy helyen módosítható, nincs elszórt "mágikus szín".

---

### S10. Nincs CI/CD, statikus elemzés, LeakCanary, StrictMode

- **Forrás:** repó gyökér (nincs `.github/` workflow), `app/build.gradle.kts` (nincs `detekt`/`ktlint`/`spotless` plugin), nincs `debug`-ra kötött LeakCanary dependency, nincs `StrictMode.setThreadPolicy(...)` hívás sehol a kódban.
- **Probléma:** A build csak `release` `buildTypes` blokkot definiál explicit módon (R8 minify+shrink, ami helyes), de nincs automatizált minőségi kapu: nincs lint-as-error CI lépés, nincs Detekt/ktlint, nincs debug-build LeakCanary, nincs StrictMode a fejlesztői build-ekben.
- **Miért probléma:** Guide 14.1 ("Detect leaks: LeakCanary… the single highest-ROI tool"), 14.2 (StrictMode a main-thread I/O detektálásához), 15.1 ("Android Lint / Detekt / ktlint — run in CI; treat new warnings as errors"), 16.3 (CI pipeline minimum: `assembleDebug`, unit tesztek, lint, Detekt, ktlint minden PR-en). Ezek hiányában pont az ebben a dokumentumban felsorolt smell-ek (S1–S9) egy része gépileg is elkapható lenne, mielőtt code review-ba kerül.
- **Javítás:** A guide 16.3 minimális pipeline-ja: PR-en `assembleDebug` + unit tesztek + `lint` + Detekt + ktlint; debug build-be LeakCanary; debug-only StrictMode a `GtlApplication.onCreate()`-ben (`if (BuildConfig.DEBUG) { StrictMode… }`).
- **Miért oldja meg:** A regressziók (leak, main-thread I/O, stílus-eltérés) a fejlesztő gépén/CI-ben buknak ki, nem a felhasználónál.

---

## Módszer

A mellékelt "Modern Android Development" guide 15.2 smell-katalógusát (concurrency, lifecycle/leaks, state/architecture, UI/Compose, data/storage, security/platform) végigmenve grep-alapú keresés (`GlobalScope`, `runBlocking`, `catch (.*Exception)`, `collectAsState(`, `LazyColumn`/`items(`, `exportSchema`, `Color(0x`, `SharedPreferences`, `fallbackToDestructiveMigration`, `FLAG_MUTABLE`, `exported=`, `!!`) + célzott forrásolvasás (`GtlViewModel.kt`, `GtlApplication.kt`, `GtlDatabase.kt`, export use case-ek, worker-ek, `SecondaryScreens.kt`, `TuhuDownloadRow.kt`, `GtlApp.kt`, `MapPane.kt`, build/gradle konfiguráció, `network_security_config.xml`). A `!!` operátor 16 találata mind az `engine/src/test` tesztfájlban van (asserteknél idiomatikus, nem smell) — production kódban egy előfordulás sem volt. `GlobalScope`, `runBlocking`, `SharedPreferences`, `fallbackToDestructiveMigration`, mutable `PendingIntent` egyáltalán nem fordul elő a kódbázisban — ezek a guide szempontjából tisztán állnak.
