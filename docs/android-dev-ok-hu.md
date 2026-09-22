# Modern Android-fejlesztés

Véleményes terepkalauz produkciós Android-alkalmazásokhoz Kotlinnal.

A példák 2026. szeptemberi stabil eszközkészletre illeszkednek: Kotlin 2.4.20, Android Gradle Plugin 9.4.x, Gradle 9.6, JDK 17, Jetpack Compose (strong skipping alapból), Android Studio Quail (2026.1.x), célszint Android 16 (API 36). A verziószámok mozognak. A katalógusba mindig a kiadási jegyzék szerinti aktuális stabil kerüljön.

## Hogyan olvasd

Minden fejezet három kérdés köré épül:

1. Mi a helyes, mai megoldás?
2. Hogyan néz ki a rossz verzió, és hogyan ismered fel review-ban?
3. Hogyan előzöd meg az egész hibaosztályt, lehetőleg eszközzel?

Jelölések:

- **Rossz gyakorlat** — anti-minta, az árulkodó jelekkel.
- **Jó gyakorlat** — a javasolt helyettesítés.
- **Csapda** — lefordul és látszólag működik, később memóriaszivárgás, ANR, akadás vagy adatvesztés lesz belőle.
- **Így kapod el** — linter, teszt vagy heurisztika, ami gépiesen jelzi.

### Amit új kódban már nem választunk

Ezek a platformon többnyire még léteznek. Új kódban nem ezekre építünk.

| Régi eszköz | Mai helye |
|---|---|
| Java mint alkalmazásnyelv, Eclipse + ADT | Kotlin, Android Studio, Gradle Kotlin DSL |
| Képernyőnkénti Activity, `findViewById` | Egy Activity, Jetpack Compose, típusos navigáció |
| XML layout mint elsődleges UI, `AsyncTask` | Compose, coroutine + Flow |
| `SharedPreferences` új állapot tárolására | DataStore |
| Nyers `SQLiteOpenHelper` | Room 3 |
| `HttpURLConnection` és kézi JSON | Retrofit vagy Ktor, kotlinx.serialization |
| Háttér-`Service` és pollozó `AlarmManager` | WorkManager, típusos foreground service, push |
| `android.hardware.Camera` | Rendszerkamera-szerződés vagy CameraX |
| `LocationManager` pollozás általános helyzethez | Fused Location Provider |
| Kézi összedrótozás, nincs architektúra | Hilt, rétegek, egyirányú adatfolyam |
| `startActivityForResult` | Activity Result API |
| `LocalBroadcastManager` | Megosztott Flow vagy repository |

---

# 1. A platform

## 1.1 Az életciklus nem a tiéd

Az Android Linux-kernelre épül, sandboxolt, és komponens-alapú. A rendszert nem a `main()` függvényed vezérli: a folyamatot és a komponenseket a rendszer hozza létre, függeszti fel, hozza létre újra, és öli meg. Ebből ered a szivárgó `Context`, az elveszett állapot és az ANR nagy része.

## 1.2 compileSdk, targetSdk, minSdk

Három különböző szám. Ne keverd a marketing verzióval.

| Marketing verzió | API-szint |
|---|---|
| Android 14 | 34 |
| Android 15 | 35 |
| Android 16 | 36 |
| Android 17 | 37 |

- **`compileSdk`** — az API, ami ellen fordítasz. Önmagában nem kapcsolja be az új futásidejű viselkedést. A legfrissebb stabil szintet használd, amivel az eszközkészleted tud fordítani.
- **`targetSdk`** — erre a szint viselkedésváltozásaira iratkozol fel. Az emelése külön változás: olvasd el az adott kiadás behavior changes oldalát, és teszteld. 2026. augusztus 31-től telefonon, tableten, hajtogatható eszközön és Android Autón az új app és az appfrissítés a Playen API 36-ot kell célozzon. A már közzétett app API 35 alatt az újabb rendszerű eszközök új felhasználói elől kiesik. Wear OS és Android Automotive OS esetén a friss küszöb API 35, Android TV és Android XR esetén API 34. Haladék 2026. november 1-ig kérhető. A kivételek a Play aktuális target API szabályában vannak.
- **`minSdk`** — a legrégebbi támogatott eszköz. 2026-ban a `24` (Android 7) még védhető, ha az elérés számít. A `26` (Android 8) kiveszi a notification channel előtti ágat. A konkrét padlót a saját felhasználóid API-eloszlása dönti el, nem egy általános százalék.

```kotlin
android {
    namespace = "com.example.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

A `targetSdk`-t mindig írd ki. Az AGP 9, ha hiányzik, a `compileSdk` értékét teszi `targetSdk`-nak. Régebben a hiányzó `targetSdk` a `minSdk`-ra esett vissza. A hallgatólagos másolás viselkedésváltozást kapcsol be anélkül, hogy szándékosan felemelted volna.

**Csapda — a targetSdk-t kozmetikának venni.** Minden emelés megváltoztathatja a futást: scoped storage, értesítési engedély, foreground service típusok, exact alarm, kötelező edge-to-edge, nagy kijelzőn figyelmen kívül hagyott tájolási korlát. Emeld külön változásban, a behavior changes oldal elolvasása után.

**Jó gyakorlat.** Verziófüggő híváshoz `@RequiresApi` vagy `Build.VERSION.SDK_INT` őr tartozik, nem üres `try/catch`. Először az AndroidX compat burkolót nézd (`ContextCompat`, `NotificationManagerCompat`, WindowInsets).

### Amit az API 35 és 36 célszint konkrétan jelent

- **Edge-to-edge.** API 35-től a rendszer a tartalmat a rendszersávok alá rajzolja. Az API 35-ös átmeneti opt-out nem kiadási stratégia. `enableEdgeToEdge()`, és a tartalom fogyasztja az inseteket.
- **Nagy kijelző.** API 36-on a legalább 600 dp smallest width kijelzők figyelmen kívül hagyják az alkalmazás tájolási, átméretezési és képarány-korlátait. A telefonra rögzített álló layout tableten és hajtogatható eszközön szétesik, ha nem teszteled.
- **Predictive back.** A vissza művelet rendszerszintű, animált gesztus. Az `onBackPressed()` elavult. Compose-ban `BackHandler`, máshol `OnBackPressedDispatcher`. A manifestben az `android:enableOnBackInvokedCallback` legyen összhangban a navigációddal.
- **16 KB-os memórialap.** API 35+ alkalmazás, amely natív `.so` könyvtárat csomagol, 64 bites eszközön 16 KB-ra igazított kell legyen. A tiszta Kotlin- és Java-kód, a függőségeivel együtt, eleve megfelel. NDK r28 és a mai AGP a nálad fordított natív kódot alapból igazítja. Az AAR-ból érkező, előre lefordított `.so`-t csak a kiadója tudja rendbe tenni. A kikényszerítés dátuma a Play Console-on az adott appnál látszik; ezt a dátumot nézd, ne egy régi blogbejegyzést.

## 1.3 Gradle Kotlin DSL és version catalog

**Rossz gyakorlat.** Bedrótozott verziók szétszórva Groovy scriptekben, modulonként másolt `def libVersion`.

**Jó gyakorlat.** Egy `gradle/libs.versions.toml`, Kotlin DSL, convention plugin a közös beállításra.

```toml
[versions]
kotlin = "2.4.20"
agp = "9.4.1"
composeBom = "2026.08.00"
ksp = "2.3.11"
hilt = "2.59.2"
room = "3.0.3"
coroutines = "1.11.0"

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
room-runtime = { module = "androidx.room3:room3-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room3:room3-compiler", version.ref = "room" }
kotlinx-coroutines = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
}
```

Az AGP 9 alapból adja a Kotlin-fordítást Android application és library modulban. Az `org.jetbrains.kotlin.android` plugin alkalmazása hibát dob. A Compose compiler plugin és a serialization plugin megmarad, és a Kotlin-verziódhoz igazodik. A régi `kotlinCompilerExtensionVersion` és az `android.kotlinOptions` blokk nem használandó. A compiler beállítás a `kotlin.compilerOptions` blokkba megy.

KSP 2.3 óta a verzió nem `<kotlin>-<ksp>` alakú. A 2.3.11 a 2026. szeptemberi stabil, és lazán illeszkedik a Kotlin 2.4-hez. KSP1 nem kompatibilis az AGP 9-cel. Egy közönséges Android modulban a `ksp(...)` a main forráshalmazt eteti. A tesztforrásokhoz `kspTest` vagy `kspAndroidTest` kell. KMP-ben a catch-all `ksp` konfiguráció elavult: targetenként `kspAndroid`, `kspJvm` és társaik kellenek. KAPT új modulban nincs. Az `org.jetbrains.kotlin.kapt` plugin az AGP 9 beépített Kotlinjával nem fér össze.

A Room Kotlin-bővítményei a runtime artifactben vannak. Külön `room-ktx` függőség nem kell, Room 2.6 óta az az artifact üres.

**Így kapod el.** A Ben Manes `dependencyUpdates` riportolja az elérhető frissítéseket. Nem ez bukik el, ha a katalógusban duplikált verzió van: azt a Gradle és a review kapja el. CI-ban a lint, a teszt és a dependency-analysis plugin legyen a kapu. A verzióemelés maradjon tudatos változás, ne egy plugin, ami csendben felülírja a katalógust.

## 1.4 Modulváz

```
app/
 ├─ src/main/
 │   ├─ AndroidManifest.xml
 │   ├─ kotlin/com/example/app/
 │   │   ├─ MyApplication.kt
 │   │   ├─ MainActivity.kt
 │   │   ├─ ui/
 │   │   ├─ domain/
 │   │   └─ data/
 │   └─ res/
 └─ build.gradle.kts
```

A manifest egy Activityből, az engedélyekből és a valóban szükséges service, receiver, provider bejegyzésekből áll. Több Activity indokolt lehet: más app által hívott belépési pont, külön task, Android TV, widget-konfiguráció.

**Csapda — exportált komponens.** API 31 óta minden intent filterrel rendelkező komponensen kötelező az `android:exported`. Ami nem a launcher és nem tudatos külső belépési pont, az `false`.

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

## 1.5 Terjesztés

- A Play feltöltési formátuma az Android App Bundle (`.aab`). A Play állítja elő az eszközre szabott APK-kat.
- Új alkalmazásnál a Play App Signing kötelező út. Az app signing kulcs a Google-nél van. Nálad az upload kulcs van. Az upload kulcs elvesztése a Play Console-ból helyreállítható. A signing kulcsot nem te őrzöd, és nem mindkettő a te titkod.
- A `versionCode` szigorúan növekvő egész. A `versionName` embereknek szól.
- Fokozatos közzététel és a Play vitals (összeomlás, ANR, túlzott ébresztés) a produkciós visszajelzés. A mapping fájl nélkül az R8-as stack trace olvashatatlan.

---

# 2. Kotlin

## 2.1 A nullázhatóság invariáns

A `T` és a `T?` különböző típus. A `!!` azt állítja, hogy a fordító téved. Review-ban minden `!!` kérdés: mi garantálja a nem-null értéket?

```kotlin
val user = repository.currentUser() ?: return navigateToLogin()
showProfile(user)
```

**Így kapod el.** Detekt `UnsafeCallOnNullableType`.

**Csapda — platformtípus.** A Javából és az Android SDK egy részéből érkező érték `String!`: a fordító nem kényszeríti a nullázhatóságot. A határon egyszer döntsd el, hogy `String` vagy `String?`, és onnantól Kotlin-típus menjen tovább.

## 2.2 Lezárt állapot és értékosztály

```kotlin
@JvmInline value class UserId(val value: String)

data class User(val id: UserId, val name: String, val email: String)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Failure(val error: AppError) : UiState<Nothing>
}
```

A `Throwable` a UI-állapotban gyenge modell: nem stabil Compose szemmel, és érzékeny részletet vihet a felületre. A felhasználónak szánt hiba legyen saját típus.

**Rossz gyakorlat.** Külön `isLoading`, `isError`, `data` és `errorMessage`. Ezek egyszerre is igazak lehetnek. Egy sealed állapot ezt kizárja.

Az értékosztály futásidőben gyakran eltűnik, de boxolódhat. Arra való, hogy egy `String` azonosítót ne lehessen összetéveszteni egy másik `String` azonosítóval.

## 2.3 Scope-függvények

Mindegyiknek van egy szokásos szerepe. Ne ágyazd őket egymásba.

- `apply` — az objektumot konfigurálod, és őt adod vissza.
- `also` — mellékhatás, a receiver megy tovább.
- `let` — nullázható érték átalakítása, vagy szűk ideiglenes scope.
- `run` és `with` — eredményt számolsz egy receiverből.

Ha az `it` nem egyértelmű, a blokk legyen nevesített függvény.

## 2.4 Coroutine

Az `AsyncTask` elavult. A platform API-jában még megvan. Új kódban coroutine van helyette.

1. Minden coroutine scope-hoz kötött: `viewModelScope`, `lifecycleScope`, vagy egy scope, amit te hozol létre és te szakítasz meg. `GlobalScope` nincs.
2. A suspend függvény hívható a fő szálról. A blokkoló munkát ő viszi le `withContext(Dispatchers.IO)` mögé, a hívó nem.
3. A megszakítás kooperatív. Hosszú ciklusban kell suspend pont vagy `ensureActive()`. A `CancellationException` továbbterjed.

```kotlin
fun load() {
    viewModelScope.launch {
        _state.value = UiState.Loading
        _state.value = try {
            UiState.Success(repo.user())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiState.Failure(e.toAppError())
        }
    }
}
```

**Rossz gyakorlat.** `GlobalScope.launch`. Túléli a képernyőt, és nincs, aki megszakítsa.

**Csapda — `runCatching` és a széles `catch (e: Exception)`.** Mindkettő elkapja a `CancellationException`-t, mert az az `Exception` leszármazottja. A `kotlin.runCatching` nem dobja tovább. Megszakítható munka körül konkrét kivételt kapj el, és a cancellationt dobd tovább. `ensureActive()` a széles catch után ugyanerre való.

**Csapda — a repository ismeri a fő szálat.** `withContext(Dispatchers.Main)` egy repositoryban a réteget fordítja meg. A dispatcher a szélén dől el, injektált `AppDispatchers` mögött, hogy a teszt `TestDispatcher`-t tehessen a helyére.

| Dispatcher | Mire való |
|---|---|
| `Main` | UI. `Main.immediate` elkerüli a felesleges újraküldést, ha már a fő szálon vagy. |
| `IO` | Blokkoló I/O. Nagy pool. |
| `Default` | CPU-munka: parse, rendezés, képfeldolgozás. |

A Retrofit és a Ktor suspend hívása már nem blokkolja a fő szálat. Plusz `withContext(Dispatchers.IO)` körülöttük ártalmatlan, de nem ettől lesznek main-safe.

## 2.5 Flow

A `Flow` hideg. A `StateFlow` forró, és mindig van értéke. Új képernyő-állapotnál ez a `LiveData` helye. A `LiveData` a platformon megvan; új kódban nem ezt választjuk.

```kotlin
val results: StateFlow<UiState<List<Item>>> =
    query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(UiState.Success(emptyList()))
            else repo.search(q)
                .map<List<Item>, UiState<List<Item>>> { UiState.Success(it) }
                .catch { emit(UiState.Failure(it.toAppError())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )
```

A Flow `catch` operátora nem nyeli el a `CancellationException`-t. A `WhileSubscribed(5_000)` képernyő-állapotra való: a forgatást túléli, tartós háttérben elengedi az upstreamet. Az `Eagerly` és a `Lazily` olyan folyamat-szintű streamre való, amelynek akkor is élnie kell, ha nincs feliratkozó. Képernyő-állapotra ezek feleslegesen tartják életben a munkát.

Compose-ban a gyűjtés életciklushoz kötött:

```kotlin
val state by viewModel.results.collectAsStateWithLifecycle()
```

A `collectAsState()` a kompozícióval él, az Activity `STOPPED` állapotában is. A `collectAsStateWithLifecycle()` a `lifecycle-runtime-compose` artifactből jön, és `STARTED` alatt gyűjt.

Az egyszeri esemény (navigáció, snackbar) nem `StateFlow`. Forgatáskor és újrafeliratkozáskor újrajátszódna. A jobb modell: az esemény a UI-állapot része, és kezelés után törlődik. A `Channel` elveszítheti az eseményt, ha éppen nincs gyűjtő.

## 2.6 Extension, delegálás, lazy

Az extension a domain modell tisztaságára és a perem ragasztására való (`User.toUiModel()`). Property getterben nincs I/O és nincs nehéz számítás. A property olvasása olcsó.

A `by lazy` alapból szinkronizált, egyszeri inicializálásra való. Nem helyettesíti az életciklust. `by viewModels()` és `by hiltViewModel()` scope-olt ViewModelt ad.

## 2.7 Generikusok és inline

Producer oldalon `out`, consumer oldalon `in`. Az `inline` és a `reified` ott éri meg, ahol a típusnak futásidőben meg kell maradnia, vagy ahol egy magasabbrendű függvény lambda-allokációját veszed ki. A válogatás nélküli inline növeli a bytecode-ot.

## 2.8 Kotlin 2.4

- A K2 fordító a 2.0 óta az alapértelmezett. A Compose compiler plugin verziója egyezzen a Kotlin-verzióval.
- A context paraméterek 2.4.0 óta stabilak. Kivétel: az explicit context argumentum és a callable reference kísérleti, külön fordítókapcsoló kell hozzá. Ritka, explicit környezeti függőségre valók (`Logger`, `Clock`). Nem service locator, és nem arra valók, hogy minden paraméter eltűnjön a szignatúrából. Konstruktoron nem lehetnek.
- A `kotlinx.serialization` fordításkor generál kódot, reflection nélkül. Új JSON-határon ez a választás, nem a Gson.

---

# 3. Architektúra

## 3.1 Komponensek

| Komponens | Mai szerepe |
|---|---|
| Activity | Az app egy Activityt használ, Compose-t és navigációt hosztol. További Activity csak valódi belépési ponthoz. |
| Service | Ritka. Foreground service csak olyan munkához, amit a felhasználó éppen lát és vár. |
| BroadcastReceiver | Háttérben a legtöbb implicit broadcast nem jut el a manifestben regisztrált receiverhez. Futás közben regisztrált receiver, vagy WorkManager. |
| ContentProvider | `FileProvider` és más appal való megosztás. A saját adat Room és repository. |

Konfigurációváltáskor (forgatás, téma, nyelv, ablakméret) az Activity újra létrejön. A ViewModel ezt túléli. A folyamat halálát nem éli túl. Azt a `SavedStateHandle`, a `rememberSaveable` és a valódi tár (Room, DataStore) éli túl. Mindkét utat teszteled: forgatás, valamint „Don't keep activities” vagy a folyamat kilövése a háttérből.

**Rossz gyakorlat.** Állapot az Activity mezőjében vagy egy `companion object`-ben. A mező forgatáskor elveszik. A statikus mező szivárog, és folyamat-halálnál úgyis elveszik.

A `SavedStateHandle` ugyanazokat a típusokat tudja, mint a `Bundle`: primitívek, `String`, és a támogatott tömbök, `Parcelable` szűk körben. Egy tetszőleges `List<Item>` nem menthető bele. A handle-be azonosító és kis UI-állapot kerül. A kosár tartalma Roomba vagy DataStore-ba megy.

```kotlin
@HiltViewModel
class CartViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val repo: CartRepository,
) : ViewModel() {
    val selectedId: StateFlow<Long> = savedState.getStateFlow("selectedId", -1L)
    val cart: Flow<List<Item>> = repo.observe()
}
```

## 3.2 Rétegek és egyirányú adatfolyam

```
UI        Composable  ← állapot —  ViewModel
              │  — esemény →        │
Domain    use case-ek, tiszta Kotlin, Android-import nélkül
Data      repository → Room, hálózat, DataStore
```

- Az állapot lefelé folyik, az esemény felfelé. A UI nem ír megosztott, mutable állapotot.
- A függőség befelé mutat. A domain modul Android-import nélkül, közönséges JVM-tesztben fut.
- A repository az adat egyetlen forrása. A UI nem hív API-t és nem nyit adatbázist.
- A ViewModel nem tart Activityt, View-t, navigációs kontrollert. `Application` context jogilag belefér, és mégis szag: a contextet igénylő hívás maradjon a data rétegben, a ViewModel repositoryt kapjon.
- A domain réteg opcionális. Akkor éri meg, ha ugyanaz a szabály több képernyőn fut, vagy a szabályt Android nélkül akarod tesztelni. Egyetlen repository-hívás becsomagolása üres use case-be nem architektúra.

**Rossz gyakorlat.** Egy osztályban hálózat, adatbázis, JSON és formázás. Áruló jel: a fájl importál `android.*`-ot, `okhttp3.*`-ot és `androidx.room3.*`-ot is.

## 3.3 ViewModel

A képernyő állapota egy immutable `StateFlow`, ami sealed állapotot vagy egyetlen, ellentmondásmentes data class-t hordoz. A mutable tartály privát. A frissítés `update { copy(...) }`.

```kotlin
@HiltViewModel
class TodoViewModel @Inject constructor(
    private val getTodos: GetTodosUseCase,
    private val toggleTodo: ToggleTodoUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TodoUiState>(TodoUiState.Loading)
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTodos().collect { todos ->
                _uiState.value = TodoUiState.Ready(todos)
            }
        }
    }

    fun onToggle(id: TodoId) {
        viewModelScope.launch { toggleTodo(id) }
    }
}

sealed interface TodoUiState {
    data object Loading : TodoUiState
    data class Ready(val todos: List<Todo>) : TodoUiState
    data class Failed(val error: AppError) : TodoUiState
}
```

**Rossz gyakorlat.** Publikus `MutableStateFlow` vagy `MutableLiveData`. A UI ettől kezdve maga írja az állapotot.

## 3.4 Hilt

A Hilt a szokásos választás egy Android-appban. A Koin könnyebb, futásidejű, és KMP-ben gyakori. A kettő keverése egy modulban rosszabb, mint bármelyik önmagában.

```kotlin
@HiltAndroidApp
class MyApplication : Application()

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder<AppDatabase>(context, "app.db")
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideTodoDao(db: AppDatabase): TodoDao = db.todoDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds
    abstract fun bindTodoRepo(impl: TodoRepositoryImpl): TodoRepository
}
```

A képernyő `hiltViewModel()`-t használ. Az aktuális artifact az `androidx.hilt:hilt-lifecycle-viewmodel-compose`. A régi `hilt-navigation-compose` név ne legyen az egyetlen fogódzó: nézd meg, melyik artifact exportálja a függvényt a választott Hilt-vonalon.

**Jó gyakorlat.** A domain a `TodoRepository` interfészt látja. Az implementáció a data modulban van, `@Binds` köti. A teszt `FakeTodoRepository`-t ad.

**Csapda — minden `@Singleton`.** A singleton a folyamatig él, és magával viszi, amit tart. Activity context singletonban klasszikus szivárgás. A szűkebb scope (`@ViewModelScoped`) csak akkor kell, ha egy ViewModel-példányon belül osztod meg az objektumot. A semmire sem kötött provider minden kérésre új példányt ad.

A Hilt workerhez kell az `androidx.hilt:hilt-work` és a hozzá tartozó compiler is, nem csak a `hilt-android-compiler`.

## 3.5 Modulok

Egy modul elég, amíg a fordítás és a tulajdonlás bírja. Utána:

- `:app` — Application, Activity, navigációs gyökér, DI-összeszerelés.
- `:feature:*` — egy feature. Más feature belsejétől nem függ. Ha két feature beszél, a szerződés a `:core`-ban vagy egy külön API-modulban van.
- `:core:ui`, `:core:data`, `:core:domain`, `:core:designsystem`.

A convention plugin a `build-logic`-ban van, nem másolt `build.gradle.kts` darabokban. A határsértést a dependency-analysis plugin vagy egy explicit Gradle-szabály bukja a CI-ban.

---

# 4. Jetpack Compose

## 4.1 UI = f(állapot)

A composable leírja, mit kell mutatni. Nem tartasz `TextView`-t, és nem hívsz `setText`-et. A három fázis: composition, layout, drawing. A recomposition csak az érintett részt hívja újra, és csak akkor olcsó, ha a kihagyás működik.

## 4.2 remember és state hoisting

```kotlin
@Composable
fun Counter() {
    var count by rememberSaveable { mutableIntStateOf(0) }
    Button(onClick = { count++ }) { Text("Count: $count") }
}
```

A `remember` a recompositiont éli túl, és a kompozícióval együtt megszűnik. Konfigurációváltást nem él túl. A `rememberSaveable` a konfigurációváltást és a folyamat halálát is túléli, ha az érték `Saver`-rel menthető. A forgatás konfigurációváltás. A folyamat kilövése process death. A ViewModel a konfigurációváltást éli túl, a folyamat halálát nem.

**Jó gyakorlat.** Az állapot a hívónál van. A levél composable állapotot kap, és eseményt küld.

```kotlin
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(value = query, onValueChange = onQueryChange)
}
```

**Csapda.** `remember { mutableStateOf(param) }` nem követi a `param` későbbi változását. Kulcsolj: `remember(param) { ... }`, vagy számold `derivedStateOf`-fal, ha a számítás drága.

## 4.3 Route és Screen

```kotlin
@Composable
fun TodoRoute(viewModel: TodoViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodoScreen(state = state, onToggle = viewModel::onToggle)
}

@Composable
fun TodoScreen(state: TodoUiState, onToggle: (TodoId) -> Unit) {
    when (state) {
        TodoUiState.Loading -> LoadingIndicator()
        is TodoUiState.Failed -> ErrorView(state.error)
        is TodoUiState.Ready -> TodoList(state.todos, onToggle)
    }
}
```

A `Route` ismeri a ViewModelt. A `Screen` nem. A preview és a Compose-teszt a `Screen`-t eteti.

## 4.4 Lista

```kotlin
@Composable
fun TodoList(todos: List<Todo>, onToggle: (TodoId) -> Unit) {
    LazyColumn {
        items(items = todos, key = { it.id.value }) { todo ->
            TodoRow(todo, onToggle)
        }
    }
}
```

A `key` stabil és egyedi. A lista indexe rendezéskor és beszúráskor hazudik. Hosszú vagy korlátlan listához `LazyColumn`, `LazyRow` vagy `LazyVerticalGrid` való. A `Column` plusz `verticalScroll` minden elemet felépít.

## 4.5 Mellékhatás

A composable törzse többször és kiszámíthatatlan sorrendben futhat. Mellékhatás csak effectben van.

- `LaunchedEffect(key)` — coroutine a kompozícióhoz kötve. A kulcs változására újraindul, kilépéskor megszakad.
- `rememberCoroutineScope()` — callbackből indított coroutine, a kompozíció élettartamával.
- `DisposableEffect` — regisztráció, és `onDispose` a párja.
- `rememberUpdatedState` — hosszú effect a friss értéket látja anélkül, hogy a kulcs változna.
- `derivedStateOf` — csak akkor számol újra, ha a kimenet tényleg változik. Görgetési küszöbhöz tipikus.

**Rossz gyakorlat.** `vm.load()` közvetlenül a composable törzsében. Minden recomposition indítja. `LaunchedEffect(Unit)`, vagy a betöltés a ViewModel `init`-jében van, ha a képernyő belépéséhez tartozik.

## 4.6 Material 3

Material 3, `MaterialTheme`, és Android 12-től (API 31) dynamic color, ha a termék kéri.

```kotlin
@Composable
fun AppTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark ->
            dynamicDarkColorScheme(context)
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
```

A szín és a méret a témából jön. A szétszórt `Color(0xFF...)` és a mágikus `16.dp` a sötét témát és a újramárkázást kereséssé teszi.

`enableEdgeToEdge()` az `onCreate`-ben, `setContent` előtt. A tartalom `Modifier.safeDrawingPadding()`, `Scaffold` inset, vagy tudatosan kezelt `WindowInsets`. A rendszer sávja mögé rajzolt, kattinthatatlan gomb hiba.

## 4.7 Stabilitás

A Compose akkor hagy ki egy composable-t, ha a paraméterei változatlanok. Erős skipping mellett (ez az alap) az instabil paraméterű composable is kihagyható, ha minden argumentum **referencia szerint** ugyanaz. A stabil típusnál az `equals` számít. A `List`, a `Set` és a `Map` instabil, mert az interfész mögött lehet mutable implementáció. Egy data class csak akkor stabil, ha minden property-je stabil.

Az `@Immutable` és az `@Stable` ezért nem felesleges. Azt ígéred velük, hogy az `equals` megbízható, és a Compose eszerint hagyhat ki. Rossz ígéret csendes UI-hibát okoz. A `kotlinx.collections.immutable` `ImmutableList`-je csak akkor stabil a fordító szemében, ha a modulban fordul, vagy stability configuration mondja annak. A wrapper data class `@Immutable` annotációja ugyanez az ígéret.

**Rossz gyakorlat.** `Flow`, `LiveData` vagy mutable kollekció átadása levél composable-nek. A flow-t egyszer, a route-ban gyűjtsd, és immutable állapotot adj tovább.

Tipikus recomposition-hiba:

1. Instabil paraméter, ami minden körben új példány.
2. Nehéz objektum létrehozása a compositionben.
3. Gyorsan változó állapot túl magasan olvasva (görgetés). Olvasd a legkisebb scope-ban, vagy `derivedStateOf` és lambda-`Modifier` (`Modifier.offset { }`) mögött, hogy layout vagy draw fusson, ne az egész composition.
4. Hiányzó `key`.
5. `java.util.Date` és `Calendar` a UI-állapotban. Helyettük `kotlinx.datetime` vagy egy saját immutable érték.

Mérés:

- Layout Inspector recomposition counts.
- Compose compiler riport: `reportsDestination` a `composeCompiler` blokkban. A régi, szétszórt compiler-flag nem a mai belépési pont.
- Macrobenchmark és Baseline Profile, release builden, R8-cal. Debug buildből teljesítményt nem olvashatsz.

**Így kapod el.** Detekt, Android Lint, és a Compose szabálykészlet (`io.nlopez.compose.rules`, a korábbi Twitter-szabályok karbantartott vonala).

## 4.8 Preview és akadálymentesség

Az állapotmentes `Screen` `@Preview` és `@PreviewParameter` alatt eszköz nélkül renderelhető.

Az akadálymentesség a felület része. A jelentéssel bíró ikon kap `contentDescription`-t. A dekoratív ikon `contentDescription`-je `null`, különben a felolvasó kétszer mondja. Érintési cél legalább 48 dp; a Material 3 `minimumInteractiveComponentSize()` ezt a komponens köré teszi. Kontraszt és szemantika (`Modifier.semantics`) a nem szöveges vezérlőn kell.

---

# 5. Intent, navigáció, eredmény

## 5.1 Explicit és implicit intent

```kotlin
startActivity(Intent(this, DetailActivity::class.java))

val intent = Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
try {
    startActivity(intent)
} catch (_: ActivityNotFoundException) {
    showNoHandler()
}
```

**Csapda — package visibility.** API 30 óta a `resolveActivity` `null`-t adhat olyan appra is, ami telepítve van, ha a manifest `<queries>` eleme nem fedi az intentet. A `resolveActivity != null` ellenőrzés ezért önmagában hamis biztonság. A nem látható, de valós kezelőt a `try/catch` éri el. A saját magad által indított, ismert célt `<queries>`-ben deklarálod.

**Csapda — PendingIntent.** API 31 óta kötelező a `FLAG_IMMUTABLE` vagy a `FLAG_MUTABLE`. Az alap a `FLAG_IMMUTABLE`, gyakran `FLAG_UPDATE_CURRENT`-tel együtt. Mutable flag csak ott, ahol a fogadó app tölti ki az intentet (például egyes értesítési válaszmezők).

## 5.2 Navigáció az appon belül

Két támogatott, típusos út van.

**Navigation Compose 2** (2.10.x) típusos úti céllal. A back stack, a deep link és a `NavController` a könyvtáré. Akkor erős, ha App Link, több back stack és kész navigációs viselkedés kell.

```kotlin
@Serializable data object Home
@Serializable data class Profile(val userId: String)

@Composable
fun AppNav(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = Home) {
        composable<Home> {
            HomeScreen(onOpenProfile = { id -> navController.navigate(Profile(id)) })
        }
        composable<Profile> { entry ->
            val profile = entry.toRoute<Profile>()
            ProfileScreen(userId = profile.userId)
        }
    }
}
```

Kell hozzá a Kotlin serialization plugin és a `kotlinx-serialization` függőség. A stringes route és a kézi `navArgument` új gráfban nem kell.

**Navigation 3** Compose-ra készült, stabil vonal. A back stack egy lista, ami a tiéd. Az `entryProvider` típusos kulcsból composable-t csinál, a `NavDisplay` rajzol. `compileSdk` 36 kell hozzá. A deep link és a bonyolult, többmodulos gráf több saját kód, mint Navigation 2-ben. Új, Compose-only appban jó alap, ha a csapat birtokolja ezt az állapotot. Meglévő, deep linkes gráfot először típusos Navigation 2-re hozz, és csak utána nézd a Navigation 3-at.

Mindkét úton azonosító megy át, nem egész objektum és nem bitmap. Az argumentum saved state-en utazik. A nagy csomag `TransactionTooLargeException`.

**Csapda.** `NavController` a ViewModelben a UI-hoz köti a modellt, és szivárog. A ViewModel eseményt vagy állapotot ad. A composable navigál, mert ő tartja a kontrollert vagy a back stacket.

## 5.3 Activity Result

A `startActivityForResult` elavult, a platform API-jából nincs kivágva. Új kód az Activity Result szerződéseket használja. Ezek a folyamat halálát is átvészelik.

Képválasztáshoz a photo picker való, tárolási engedély nélkül:

```kotlin
val pickImage = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia(),
) { uri ->
    uri?.let(viewModel::onImagePicked)
}

Button(onClick = {
    pickImage.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
    )
}) {
    Text("Pick image")
}
```

A `GetContent` és a nyers `ACTION_GET_CONTENT` régebbi út. Egy saját kamera-UI nélkül készített fotóhoz `TakePicture` való, de az egy előre létrehozott cél-`Uri`-t vár (tipikusan `FileProvider`), és `Boolean`-t ad vissza: sikerült-e a mentés. A `TakePicturePreview` csak egy apró bitmapet ad, nem egy elmentett fájlt.

Az engedélykérés is szerződés: `RequestPermission`, `RequestMultiplePermissions`.

## 5.4 Deep link és App Link

Az intent filter vagy a navigációs deep link egy URL-t vagy értesítést a megfelelő képernyőre visz. Az Android App Link (`https`, `autoVerify`, Digital Asset Links) választó nélkül nyitja az appot, ha a domain igazolása megvan. Az igazolatlan `https` link választót mutat, vagy böngészőben marad.

## 5.5 Broadcast

API 26 óta a manifestben regisztrált receiver a legtöbb implicit broadcastot nem kapja meg, ha az app háttérben van. Kivételek vannak (`BOOT_COMPLETED` és a dokumentált lista). Appon belüli eseményre broadcast nem való: Flow vagy repository. A `LocalBroadcastManager` elavult.

A `RECEIVER_EXPORTED` és a `RECEIVER_NOT_EXPORTED` API 33 óta létezik. API 34 célszinttől a futásidőben regisztrált receivernek kötelező valamelyik. Az alap a `RECEIVER_NOT_EXPORTED`, hacsak nem más app broadcastját várod tudatosan.

Rendszerállapotra (hálózat, töltés) adott, halasztható reakció WorkManager-constraint, nem egy receiver, ami a `onReceive`-ben dolgozik.

---

# 6. Tárolás

## 6.1 DataStore

A `SharedPreferences` első olvasása a hívó szálon, lemezről tölt. A `commit()` a fő szálon ír. Új beállításhoz nem ezt választjuk. A típus megvan a platformon.

A Preferences DataStore kis kulcs-értékhez való. A delegate egyetlen, top-level példány. Osztályon vagy függvényen belül létrehozott második DataStore ugyanarra a fájlra kivételt dob.

```kotlin
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")

    val darkMode: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[darkModeKey] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[darkModeKey] = enabled
        }
    }
}
```

Strukturált beállításhoz custom `Serializer` való, gyakran kotlinx.serializationnel. Ez nem Proto DataStore. A Proto DataStore Protocol Buffers sémát használ. A név keverése rossz függőséget húz be.

Meglévő prefekhez van `SharedPreferencesMigration`. Egyszer fut, az első olvasáskor. Utána egy tároló van, nem kettő.

## 6.2 Fájlok

- **Belső tár** (`filesDir`, `cacheDir`): privát, engedély nélkül. Eltávolításkor megy. A `cacheDir` nyomás alatt ürülhet.
- **Photo picker és MediaStore**: a felhasználó képei. Új kódban a picker az első választás, engedély nélkül. A teljes médiatár olvasása (`READ_MEDIA_IMAGES` és társai) külön, felülvizsgált engedély. API 34 óta a felhasználó részleges hozzáférést adhat (`READ_MEDIA_VISUAL_USER_SELECTED`).
- **Storage Access Framework**: a felhasználó egy dokumentumot vagy fát választ. A tartós jog `takePersistableUriPermission`.
- **FileProvider**: a saját fájlod `content://` URI-ja, ideiglenes granttal. A `file://` URI `FileUriExposedException` (API 24+).

**Rossz gyakorlat.** `MANAGE_EXTERNAL_STORAGE`, hogy ne kelljen megtanulni a scoped storage-ot. A Play ezt fájlkezelőre és backup eszközre korlátozza.

**Csapda.** Fájl I/O a fő szálon. Suspend függvény, `Dispatchers.IO`. Debugban a StrictMode jelzi.

Biztonsági mentés: API 31 óta a `android:dataExtractionRules` és a `android:fullBackupContent` mondja meg, mi kerülhet felhőbe és eszközök közötti átvitelbe. Az alapértelmezés olyan adatot is vihet, amit a felhasználó nem vár (token, adatbázis). A szabály tudatos.

## 6.3 Kis, átmeneti állapot

| Hely | Mit él túl | Mit tegyél bele |
|---|---|---|
| `remember` | recomposition | olcsó, efemer UI |
| `rememberSaveable` | konfigurációváltás és process death | kis, menthető UI (görgetés, szöveg) |
| `SavedStateHandle` | process death, a ViewModel újralétrehozása után | kis kulcsok |
| Room, DataStore | minden | ami a felhasználónak számít |

Nagy blob a saved state-ben `TransactionTooLargeException`.

---

# 7. Room

Új adatbázis Room 3: `androidx.room3`, KSP, coroutine-first DAO, kötelező `SQLiteDriver`. A Room 2.8 karbantartási vonal. A WorkManager a saját, belső Room 2 függőségét hozza; a csomagneve különbözik, ezért a kettő egy classpathon megfér. Egy adatbázisfájlt a kettővel közösen kezelni nem cél.

## 7.1 Séma

```kotlin
@Entity(tableName = "todos", indices = [Index("createdAt")])
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    val createdAt: Long,
)

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Upsert
    suspend fun upsert(todo: TodoEntity)

    @Query("UPDATE todos SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)
}

@Database(entities = [TodoEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}
```

A `@Query` SQL-je fordításkor ellenőrizve van. Az `@Insert(onConflict = REPLACE)` nem upsert: törli a sort, és újat szúr, ami kaszkádot és új rowid-t okozhat. Arra az `@Upsert` való.

A Room Gradle plugin (a Room főverziójához illő artifact) a sémaexportot reprodukálhatóvá és cache-elhetővé teszi. Az exportált JSON-t commitold.

## 7.2 Olvasás és leképezés

A DAO `Flow`-ja a tábla invalidálásakor újra emitál. Az entitás a data rétegben marad. A repository domain típusra képez.

A blokkoló DAO-metódus új kódban nincs. A Room a főszálas szinkron hívást alapból hibának veszi.

## 7.3 Migráció

**Rossz gyakorlat.** `fallbackToDestructiveMigration()` produkcióban. Séma változásakor eldobja az adatot.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            "ALTER TABLE todos ADD COLUMN priority INTEGER NOT NULL DEFAULT 0",
        )
    }
}
```

Room 3-ban a `migrate` suspend, és `SQLiteConnection`-t kap, nem `SupportSQLiteDatabase`-t. A driver nélkül a Room 3 adatbázis nem épül. Android-appban `BundledSQLiteDriver` (egyező SQLite minden eszközön) vagy `AndroidSQLiteDriver` (a rendszer SQLite-ja). A driver beállítása után a régi `SupportSQLiteDatabase` hívások nem élnek, hacsak nem használod tudatosan a `room3-sqlite-wrapper` hidat.

Minden migrációs utat tesztelj `MigrationTestHelper`-rel, az exportált sémán.

## 7.4 Lekérdezés

- A `@Relation` nem egy SQL JOIN. A szülőt egy lekérdezés olvassa, a gyerekeket egy másik, összegyűjtve. Ez N+1-et kerül, de két lekérdezés. Egyetlen lekérdezéshez írj `JOIN`-t a `@Query`-ben.
- A kötegelt írás `@Transaction` vagy `withTransaction`. Egy commit, és vagy minden sor megvan, vagy egyik sem.
- Index arra az oszlopra, amire szűrsz vagy rendezel. `EXPLAIN QUERY PLAN` mondja meg, hogy az index él-e.
- Titok nem kerül sima oszlopba. A kulcs a Keystore-ban vagy a Tink alatt van. A Room 3 titkosítása a választott SQLite-driver képessége, nem egy `SupportFactory` a régi SupportSQLite úton.

Nagy, végtelen lista Paging 3: `PagingSource` a DAO-ból, `RemoteMediator`, ha a hálózat és a Room együtt lapoz. A Compose-oldal `collectAsLazyPagingItems()`.

---

# 8. Hálózat

## 8.1 Kliens

Retrofit + OkHttp, vagy Ktor. A határ suspend függvény. A JSON kotlinx.serialization.

```kotlin
@Serializable
data class UserDto(val id: String, val name: String, val email: String)

interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto
}
```

A kotlinx.serialization Retrofit-konvertere külön artifact (a Jake Wharton-féle converter a szokásos). A `Json { ignoreUnknownKeys = true }` az előre kompatibilitás: az ismeretlen mező nem dönti el az appot. Az `explicitNulls = false` tudatos kódolási döntés, nem alapértelmezés, amit gondolkodás nélkül másolunk.

## 8.2 Hibamodell

Az API-t a repository hívja, nem a ViewModel és nem a composable. A kimenet típusos eredmény. A `CancellationException` továbbmegy.

```kotlin
sealed interface ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>
    data class Failure(val error: AppError) : ApiResult<Nothing>
}

override suspend fun user(id: String): ApiResult<User> =
    try {
        ApiResult.Ok(api.getUser(id).toDomain())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        ApiResult.Failure(AppError.Http(e.code()))
    } catch (e: IOException) {
        ApiResult.Failure(AppError.Network)
    }
```

A Retrofit nem-2xx válaszra `HttpException`-t dob. A Ktornak saját kivételtípusa van. A kettőt ne keverd egy `catch` ágban.

**Csapda.** Timeout nélkül a hívás a Doze és a rossz hálózat alatt lóg. Az OkHttp `connectTimeout` és `readTimeout` be van állítva. Újrapróbálás csak idempotens kérésre, backoff-fal, korlátozva. A tokenfrissítés single-flight, különben egy lejárt token tucatnyi párhuzamos refresh-t indít.

## 8.3 Offline

A UI a Roomot figyeli. A frissítés külön suspend hívás. Ha a refresh a flow `onStart` blokkjában fut, a cache-ből jövő első érték megvárja a hálózatot, és offline-first helyett offline-last lesz.

```kotlin
fun observeUser(id: String): Flow<User?> =
    dao.observeUser(id).map { entity -> entity?.toDomain() }

suspend fun refreshUser(id: String) {
    val dto = api.getUser(id)
    dao.upsert(dto.toEntity())
}
```

A ViewModel gyűjti az `observeUser`-t, és külön `launch`-csal hívja a `refreshUser`-t. A képernyő a cache-t azonnal mutatja, majd frissül.

Az OkHttp HTTP-cache a `Cache-Control`-t tisztelő GET-hez való. Képet Coil 3 tölt (`coil3.compose.AsyncImage`): életciklus, memóriakache, lemezkache, mintavételezés a célméretre. Kézzel összerakott bitmap-betöltés a klasszikus `OutOfMemoryError`.

## 8.4 Vezeték

- Csak HTTPS. A cleartext API 28 célszint óta alapból tiltott. A `cleartextTrafficPermitted` nem gyógymód egy hibás tanúsítványra.
- Certificate pinning csak ott, ahol a fenyegetés indokolja, tartalék pinnel és lejárati tervvel. Egyetlen, lejáró pin kiüti az appot a felhasználóknál.
- Token és személyes adat nem kerül logba. Az OkHttp logging interceptor csak debug variánsban van, és az `Authorization` fejléc takarva van.

---

# 9. Háttérmunka

A képernyőhöz kötött munka coroutine a `viewModelScope`-ban. Ami a képernyő és a folyamat után is be kell fejeződjön, az WorkManager. A kettő cseréje a tipikus hiba: a feltöltés a ViewModelben meghal, amikor a felhasználó elnavigál; a pollozó service pedig olyankor akar futni, amikor a rendszer nem engedi.

## 9.1 Korlátok

API 26 óta:

- Háttérből indított háttér-service `IllegalStateException`.
- Az implicit broadcastok nagy része nem ébreszti fel a manifest receivert.
- Az exact alarm külön engedély és Play-felülvizsgálat. Ébresztő, naptár, felhasználó által látott időzítés. Szinkronizálásra nem.
- A Doze és az App Standby elhalasztja a halasztható munkát.

Nem akkor futsz, amikor akarsz. Leírod a munkát és a feltételt, a rendszer ütemez.

## 9.2 WorkManager

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: SyncRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        try {
            repo.sync()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            Result.retry()
        }
}
```

A periodikus munka legrövidebb intervalluma 15 perc. A 6 órás, tölteni kell és nem mért hálózat kell feltétel reális. Az `enqueueUniquePeriodicWork` a `KEEP` policyval nem ütemez másodpéldányt. Az `UPDATE` akkor kell, ha a constraint változott, és a meglévő kérést cserélni akarod. A `Result.retry()` exponenciális backoffot kap.

Az expedited munka kvótás, és nem foreground service. Arra való, hogy egy felhasználó által éppen kért, rövid munka előrébb kerüljön. Végtelen szinkronra nem.

**Rossz gyakorlat.** Háttér-`Service` és `while (true)` egy szerver pollozására. Az OS leállítja, az akkut eszi. WorkManager vagy push való helyette.

## 9.3 Foreground service

Akkor, ha a felhasználó éppen ezt a munkát látja: lejátszás, navigáció, edzés rögzítése, hívás, általa indított fájlátvitel.

- API 34 célszinttől a manifestben és a `startForeground` hívásban kötelező a típus (`location`, `mediaPlayback`, `dataSync`, `camera`, és a többi dokumentált típus), plusz a típushoz tartozó engedély.
- A `startForegroundService` után rövid időn belül hívd a `startForeground`-ot. Enélkül a rendszer leállítja a service-t.
- API 31 óta háttérből foreground service-t indítani csak a dokumentált kivételekkel lehet.
- API 35-ön a `dataSync` típusú foreground service napi kerete véges (6 óra 24 órán belül). Hosszú, láthatatlan szinkronra nem való.
- A Play felülvizsgálja, hogy a felhasználónak van-e látható oka. Nincs oka: WorkManager.

Az értesítés, amit a foreground service posztol, mentes a `POST_NOTIFICATIONS` engedély alól. Minden más értesítés API 33-tól kéri ezt az engedélyt.

## 9.4 Push

Szerver által indított frissítés Firebase Cloud Messaging vagy más push, nem periodikus ébresztés. A magas prioritású FCM felhasználó által azonnal látható, sürgős üzenetre való, és kvótás. Háttér-szinkron magas prioritással a kvótát és a felhasználó akkuját égeti.

---

# 10. Helyzet és térkép

## 10.1 Fused Location Provider

Általános helyzethez a Fused Location Provider való, nem a `LocationManager` pollozása. A `LocationManager` speciális GNSS- és passzív esetekre marad.

A `Task.await()` a `kotlinx-coroutines-play-services` artifact. Folyamatos figyelés `callbackFlow`-val, és `awaitClose` a `removeLocationUpdates`-hez. A callbackben nincs nehéz munka: a main looperon jön.

A `PRIORITY_HIGH_ACCURACY` GPS-t tart fel. Balanced vagy low power, hacsak nem kell méteres pontosság. A frissítés leáll, amikor a collector leáll.

## 10.2 Engedély

- `ACCESS_COARSE_LOCATION` és `ACCESS_FINE_LOCATION` futásidőben. Android 12-től a felhasználó közelítő pontosságot adhat akkor is, ha fine-t kértél. Kezeld ezt az állapotot.
- `ACCESS_BACKGROUND_LOCATION` külön, második lépés (API 29+). Android 11-től nem kérhető ugyanabban a dialógusban, mint az előtér-helyzet. A Play ezt szigorúan nézi. Csak akkor kéred, ha a feature háttérben, felhasználói indoklással helyzetet igényel.
- Előre, „biztos, ami biztos” fine és background együtt: a felhasználó elutasítja, a Play visszadobhatja.

Az Activity Result engedélyszerződés kezeli a megtagadást, a „ne kérdezd újra” állapotot és a közelítő grantet. Megtagadáskor a feature-nek van működő, szűkebb útja.

## 10.3 Térkép

A Google-térkép deklaratív felülete a Maps SDK és a Maps Compose. A kulcs az alkalmazásra és a csomagnévre korlátozott. A `Geocoder.getFromLocation` blokkol. API 33-tól van listeneres, aszinkron változata, és nem minden eszközön van geokódoló.

Offline vagy nehéz térképhez a MapLibre és a hasonló motorok valós alternatívák. A választás termékdöntés, nem egyetlen helyes SDK.

---

# 11. Kamera, lejátszás, kép

## 11.1 Kamera

Egy fotóhoz a rendszerkamera és a `TakePicture` szerződés elég. CameraX akkor kell, ha saját előnézet, egyedi expozíció vagy élő elemzés (ML) van.

A CameraX use case-eket egy `LifecycleOwner`-höz köti, és a leálláskor elengedi a kamerát. Az aktuális suspend belépő a `ProcessCameraProvider.awaitInstance(context)`. Az `ImageAnalysis` use case táplálja az ML-t. Az előnézethez a surface provider be van kötve, különben a use case él, kép nincs.

## 11.2 Media3

Hang és videó: Media3, ExoPlayer, `MediaSession`. Adaptív stream (DASH, HLS), és háttérlejátszáshoz `MediaSessionService` a `mediaPlayback` foreground service típussal.

Az `ExoPlayer` codecet és surface-t tart. `release()` a `DisposableEffect.onDispose`-ban, vagy a service lebontásakor. Elfelejtett player nehéz szivárgás.

## 11.3 Kép

Coil 3 tölti a képet a célméretre mintavételezve. Teljes felbontású bitmap egy bélyegkép kedvéért az `OutOfMemoryError` szokásos útja.

---

# 12. Engedély, telefónia, szenzor, natív kód

## 12.1 SMS és hívásnapló

A `READ_SMS`, a `SEND_SMS`, a hívásnapló és a telefonállapot olyan engedély, amit a Play csak akkor fogad el, ha az app alapfunkciója SMS- vagy tárcsázó-alkalmazás. Kényelmi feature-höz elutasítás jár.

SMS küldése a felhasználó appján át: `ACTION_SENDTO` és `smsto:`. Engedély nem kell. OTP-hez az SMS Retriever API való, SMS-engedély nélkül.

## 12.2 Szenzor

A listenert a láthatósághoz kötöd, és a párjában leveszed. Compose-ban `DisposableEffect`, máshol `DefaultLifecycleObserver`. A le nem vett `SensorEventListener` életben tartja az Activityt, és a szenzort bekapcsolva hagyja.

A `getSystemService` nullát adhat, ha a szenzor nincs az eszközön. `!!` helyett korai visszatérés. A leglassabb `SENSOR_DELAY_*`, ami a feature-nek még elég.

## 12.3 NDK

Natív kód (C, C++ vagy Rust az NDK-n át) indok: mért CPU-szűk keresztmetszet, meglévő natív könyvtár, vagy kemény valós idejű hang. Minden más Kotlin a `Dispatchers.Default`-on. A natív határ vékony, Kotlin-API mögött van. A 16 KB-os lapigazítás a saját `.so`-dra és minden szállított előre fordított `.so`-ra vonatkozik.

## 12.4 Engedély, általában

- A használat pillanatában kéred, indoklással (`shouldShowRequestPermissionRationale`).
- A megtagadás normál út, nem kivétel.
- A photo picker, az SAF és az SMS Retriever azért jobb, mert engedély nélkül oldja meg ugyanazt.
- A `POST_NOTIFICATIONS` API 33-tól kell a közönséges értesítéshez. A foreground service saját értesítése kivétel. A felhasználó az értesítést így is elnémíthatja.
- Minden manifestbeli engedélyt a Play és a felhasználó lát. A nem használt engedély kockázat.

## 12.5 Bejelentkezés

Új bejelentkezés Credential Managerrel (`androidx.credentials`) megy: jelszó, passkey, és a Google-fiók is ezen a kapun. A régi `GoogleSignInClient` és a WebView-ba rejtett jelszóűrlap nem a mai út. A token a szervernek szól. A kliensen tartott session a Keystore-ral védett tárolóba kerül, nem egy logoló `SharedPreferences`-be, és nem a forrásba égetve.

---

# 13. Teszt

A tesztpiramis:

- **Unit**, a legtöbb. ViewModel, use case, mapper, repository fake-kel. JVM, eszköz nélkül.
- **Integráció**, kevesebb. Room in-memory vagy teszt-driverrel, DataStore, repository és fake hálózat.
- **UI**, kevés. Compose UI-teszt a állapotmentes képernyőre. Instrumentált end-to-end csak a kritikus útra. Legacy View-hoz Espresso.

A `viewModelScope` a `Dispatchers.Main`-t használja. A `runTest` ezt nem cseréli le. Kell egy szabály, ami `Dispatchers.setMain`-t hív, és a teszt végén `resetMain`-t.

```kotlin
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@Test
fun loadEmitsSuccess() = runTest {
    val viewModel = ProfileViewModel(FakeUserRepository(ApiResult.Ok(user)))
    viewModel.load()
    advanceUntilIdle()
    assertEquals(UiState.Success(user), viewModel.uiState.value)
}
```

A `StandardTestDispatcher` virtuális időt ad, és `advanceUntilIdle` kell hozzá. Az `UnconfinedTestDispatcher` mohón futtat. Flow-asserthez Turbine való. A dispatchert az éles kód injektálja, ha a repository maga választ I/O-t. A `Dispatchers.IO` közvetlen hívása a tesztelt osztályban nem cserélhető.

A Room-migrációt a `MigrationTestHelper` viszi végig az exportált sémán, ugyanazzal a driverrel, amit az app használ. A destruktív migráció adatvesztését ez a teszt fogja meg, nem a felhasználó.

A Compose-teszt a `Screen`-t eteti állapottal, és a szemantikán assertál (`onNodeWithText`, `onNodeWithContentDescription`). Screenshot-regresszióhoz Paparazzi vagy a Compose screenshot teszt való.

**Rossz gyakorlat.** Minden függőség mock, hívási sorrendre kötve. A suspend és a Flow mockolása törik. Kézzel írt fake, in-memory állapottal, újrahasználható és olvasható. Mock ott, ahol az interakció a lényeg (analitika elküldve).

CI-kapu: unit teszt, `lint`, Detekt, formázás (ktlint vagy Spotless), és a screenshot, amit megengedhetsz magadnak. Az új szabálysértés piros. A meglévő adósság baseline-on van, és nem nő.

---

# 14. Memória és teljesítmény

## 14.1 Szivárgás

Androidon a szivárgás tipikusan egy Activity, Fragment, View vagy Activity-`Context`, amit egy tovább élő objektum tart. Az Activity viszi a view-fát. Ismételt forgatásnál ez `OutOfMemoryError`.

1. Statikus vagy singleton Activity context vagy View. Ami túléli a képernyőt, `applicationContext`-et kap, vagy még jobb: nem tart contextet.
2. Nem statikus belső osztály, listener vagy coroutine, ami elkapja az Activity `this`-ét, és túléli. A coroutine `viewModelScope` vagy `lifecycleScope`.
3. `register` / `add` / `observe` nélkül a pár `unregister` / `remove`. Szenzor, receiver, location callback, `ContentObserver`, player, `Handler`.
4. `Handler.postDelayed`, ami View-t fog. Lebontáskor `removeCallbacksAndMessages(null)`, vagy lifecycle-scope és `delay`.
5. ViewModel, ami Activityt vagy View-t tart. Forgatáskor a ViewModel él, az Activitynek mennie kellene.
6. Teljes felbontású bitmap és korlátlan cache. Coil, mintavételezés, korlátozott cache, `onTrimMemory`.
7. `CoroutineScope(SupervisorJob())`, amit senki nem `cancel`-el. A job és a befoglalt referencia él. A framework scope-ja az alap. Saját scope-nak gazdája van.

A LeakCanary debug implementáció. Megtartott Activityre, Fragmentre és ViewModelre hívási láncot ad. Minden jelentés hiba, nem zaj. Produkcióban a Play vitals memória- és OOM-aránya, valamint a Memory Profiler a vizsgálat.

## 14.2 Akadás

60 Hz-en körülbelül 16,7 ms egy képkocka. 90 Hz-en 11 ms, 120 Hz-en 8,3 ms. A fő szálon parse, adatbázis, lemez és nagy allokáció ezt lépi túl.

Debugban a StrictMode `penaltyLog`-gal jelzi a főszálas lemezt és hálózatot. A `penaltyDeath` könyvtárak miatt zajos appban többet zavar, mint amennyit véd. A `detectAll()` szintén zajos; a konkrét detektor (lemez, hálózat, lezáratlan erőforrás) olvashatóbb.

A többi szokásos ok: instabil Compose-paraméter, túl magasan olvasott görgetés, hiányzó lista-`key`, mély és felesleges beágyazás, allokáció a draw útvonalon.

Mérés: JankStats, Perfetto, Macrobenchmark `FrameTimingMetric`. Release, R8-cal.

## 14.3 Indulás

- Baseline Profile a hideg indulás és a fő út forró kódjára. Az ART telepítéskor ahead-of-time fordítja. A nyereség appfüggő; a gyakran idézett 20–30% mérés eredménye volt egyes mintákon, nem ígéret. A sajátodat `StartupTimingMetric` méri, release-en.
- Az App Startup könyvtár rendezetten, lustán inicializál. A nehéz SDK-inicializálás az `Application.onCreate`-ben minden indulást lassít, akkor is, ha a felhasználó azt a feature-t nem nyitja meg.

## 14.4 ANR

A bemenet körülbelül 5 másodperc főszál-blokkolás után ANR. A broadcast és a service más, dokumentált limitet kap, háttérben rövidebbet. Tipikus ok: főszálas I/O, `runBlocking` a fő szálon, deadlock, hosszú szinkron binder-hívás. A `runBlocking` produkciós UI-kódban review-blokk. Az ANR-arány a Play láthatóságát is befolyásolja.

## 14.5 R8

Release-ben `isMinifyEnabled = true` és `isShrinkResources = true`. Az R8 full mode, ha a projektben még nincs bekapcsolva, külön property. A reflectiont használó könyvtár keep szabályt kap. A mapping fájl felmegy a Playre vagy a crash-eszközbe. Debug builden a kihagyás és a futásidő nem a felhasználó élménye.

## 14.6 Ellenőrzőlista merge előtt

- Nincs főszálas I/O.
- A LeakCanary tiszta az érintett képernyőn.
- A lazy listának stabil `key`-e van.
- Nincs `GlobalScope`, nincs `runBlocking` a fő szálon, nincs gazda nélküli scope.
- A listener, az observer és a player regisztrációja páros.
- A kép Coilön megy, a célméretre mintavételezve.
- A forró képernyő Compose-riportjában nincs meglepetés-instabil paraméter.
- Ami számít, release-en, Macrobenchmarkkal van mérve.

---

# 15. Szagok

## 15.1 Eszköz, egyszer beállítva

- Android Lint, a mai `lint { }` blokkal. A `lintOptions` a régi DSL. Új figyelmeztetés a CI-ban hiba, vagy baseline-ról indulva csak az új nőhet.
- Detekt: `!!`, üres catch, komplexitás, coroutine-hiba. Baseline-nal az adósság nem nő.
- ktlint vagy Spotless: a stílus nem review-téma.
- Compose-szabályok.
- Compose compiler riport a forró képernyőre.
- dependency-analysis: nem használt és rossz modulba tett függőség.

A racsni: baseline ma, holnaptól az új sértés piros. Nincs big-bang takarítás előfeltételként.

## 15.2 Katalógus

**Konkurrencia**

- `GlobalScope` → `viewModelScope` vagy `lifecycleScope`.
- `runBlocking` a fő szálon → a hívó legyen suspend, vagy a munka menjen háttérbe.
- `catch (e: Exception)` és `runCatching` suspend kód körül → a cancellation továbbmegy.
- Bedrótozott `Dispatchers.IO` a tesztelendő üzleti kódban → injektált dispatcher.

**Életciklus**

- Activity, View vagy Activity-context statikus mezőben, `object`-ben vagy ViewModelben → szivárgás.
- `register` pár nélkül → szivárgás és akku.
- `NavController` a ViewModelben → az esemény a composable-é.

**Állapot**

- Publikus mutable state → csak olvasható `StateFlow`.
- Egymást nem kizáró boolean flag-ek → sealed állapot.
- `android.*` import a domainben → a framework a peremen marad.

**Compose**

- Mellékhatás a törzsben → `LaunchedEffect` vagy ViewModel `init`.
- `collectAsState()` képernyőn → `collectAsStateWithLifecycle()`.
- Lazy elem `key` nélkül.
- Nyers `Flow` vagy mutable kollekció a levélben.
- Beégetett szín és méret → téma.

**Adat**

- `SharedPreferences.commit` vagy főszálas olvasás új kódban → DataStore.
- `fallbackToDestructiveMigration()` produkcióban → migráció és teszt.
- Kézi JSON vagy reflectionös Gson új határon → kotlinx.serialization.
- Főszálas fájl vagy adatbázis → suspend DAO, `Dispatchers.IO`.
- `@Insert(REPLACE)` upsert helyett → `@Upsert`.

**Biztonság és platform**

- `exported="true"` szándék nélkül.
- Mutable `PendingIntent` ok nélkül → `FLAG_IMMUTABLE`.
- Token a logban.
- SMS, hívásnapló, háttér-helyzet, all-files access „hátha kell” → a minimum, engedély nélküli alternatíva.
- `cleartextTrafficPermitted` egy cert hiba megkerülésére.
- `resolveActivity` package visibility nélkül, egyetlen őrként.

## 15.3 Biztonsági alap

- A titkos kulcs a Keystore-ban van, hardveres tárolóval, ahol az eszköz tudja. A tömeges titkosítás Tink vagy ezzel egyenértékű, kulccsal a Keystore alatt. „Encrypted DataStore” nevű termék nincs: custom serializer titkosítja a byte-okat, ha a pref titok.
- API-titok az APK-ban kiolvasható. Ami titok, a szerveren van.
- Legkisebb engedély. `exported=false`, hacsak nem belépési pont. Scoped storage, photo picker.
- A deep link, az Intent extra és a más appból jött adat nem megbízható. Validálod.
- HTTPS. Pinning csak indokkal és rotációval.
- Jogosultság, ár és auth a szerveren dől el. A kliens ellenőrzése felület, nem biztonsági határ.
- Az R8 emeli a visszafejtés árát. A Play Integrity a magas értékű, szerver által ellenőrzött folyamatokra való. Egyik sem fal.
- A függőséget ellenőrizd (dependency check, Play SDK Index). A tranzitív sebezhetőség a tiéd.

## 15.4 Review-kérdések

Blokkolja a fő szálat? Túlélheti a referencia az Activityt? Egy forrásból jön, és ellentmondásmentes az állapot? Az effectnek és a regisztrációnak van párja? Mi történik offline, megtagadáskor, forgatáskor és process death után? Minimális az engedély? A natív könyvtár 16 KB-ra igazított? Release-ben, R8-cal is ez a viselkedés?

---

# 16. Build és kiadás

## 16.1 Variánsok

A `debug` variáns debuggolható, StrictMode-dal, LeakCanaryval, logolással. A `release` R8-cal, zsugorított erőforrással, aláírással, token-log nélkül készül.

A product flavor (`dev`, `staging`, `prod`) a végpontot és a konfigurációt választja. A titok és a keystore nincs a Gitben: Gradle property, `local.properties`, CI-secret.

## 16.2 CI

Pull requesten: fordítás, unit teszt, lint, Detekt, formázás. A mainre kerüléskor: App Bundle, instrumentált teszt Gradle Managed Devices-on, Baseline Profile, ha a csapat méri az indulást. Kiadáskor a bundle egy Play-sávra megy (internal, closed, open, production), fokozatos százalékkal. A vitals kapuján az összeomlás és az ANR megállítja a görgetést.

A mapping fájl ugyanazzal a kiadással megy fel.

## 16.3 Verzió és kapcsoló

A `versionCode` a CI-ból jön, és soha nem csökken. A fokozatos közzététel (5%, 20%, 50%, 100%) csak akkor véd, ha a kapukon megállítható.

A remote config és a feature flag sötét indításra és gyors visszavonásra való. Szabály megkerülésére nem.

---

# 17. Meglévő kódbázis

A csere inkrementális. Az app minden lépés után kiadható.

| Meglévő | Helyette | Hogyan |
|---|---|---|
| Java | Kotlin | Fájlonként, utána a nullázhatóság és a data class rendbetétele. Az interop működik. |
| `AsyncTask`, nyers szál | Coroutine | `viewModelScope`, a blokkoló könyvtár `withContext(IO)` mögött. |
| `SharedPreferences` | DataStore | `SharedPreferencesMigration`, egyszer. |
| `SQLiteOpenHelper` | Room 3 | A séma entitás lesz. Előbb a KSP, a suspend DAO és a driver, utána a csomagcsere. A meglévő fájlt a Room megnyitja, ha a verzió és a migráció stimmel. |
| `HttpURLConnection`, kézi JSON | Retrofit vagy Ktor | Repository-határ, képernyőnként. |
| XML View | Compose | `ComposeView` egy View-ban, `AndroidView` Compose-ban. Képernyőnként. |
| `Loader` | Flow + ViewModel | A repository Flow-ja a forrás. |
| Kézi singleton | Hilt | Az Applicationtől befelé, modulonként. |
| Support Library, Jetifier | AndroidX | Teljes `androidx.*` migráció. A Jetifier kikapcsolva. |

A repository interfész a határ a régi és az új között. Egy feature mögötte cserélődik. Amit megérintesz, arra teszt kerül, ugyanabban a változásban.

---

# Függelék

## Pillanatkép, 2026. szeptember

| Terület | Választás |
|---|---|
| Nyelv | Kotlin 2.4.20 |
| Build | AGP 9.4.x, Gradle 9.6, JDK 17, Android Studio Quail 2026.1.x |
| Célszint | compileSdk 36, targetSdk 36, minSdk a saját elérésed szerint |
| UI | Jetpack Compose, Material 3, Compose BOM 2026.08.00 |
| Fordítóplugin | Compose compiler és serialization a Kotlin-verzióval; KSP 2.3.11 |
| Aszinkron | Coroutine 1.11.x, Flow, StateFlow |
| DI | Hilt 2.59.x, vagy Koin KMP-ben |
| Adatbázis | Room 3.0.x, KSP, SQLite driver |
| Kulcs-érték | Preferences DataStore, vagy custom Serializer |
| Hálózat | Retrofit vagy Ktor, OkHttp, kotlinx.serialization |
| Kép | Coil 3 |
| Háttér | WorkManager; típusos foreground service; FCM |
| Navigáció | Navigation 3, vagy típusos Navigation Compose 2.10.x |
| Helyzet | Fused Location Provider |
| Kamera, média | Rendszerkamera-szerződés vagy CameraX 1.6.x; Media3 |
| Teszt | JUnit, coroutines-test, Turbine, Compose UI test, MigrationTestHelper |
| Szivárgás | LeakCanary, csak debug |
| Teljesítmény | Macrobenchmark, Baseline Profile, R8, StrictMode |
| Minőség | Android Lint, Detekt, ktlint vagy Spotless, Compose-szabályok |

## Vezérelvek

1. Az életciklus a rendszeré. A szivárgás, az elveszett állapot és az ANR ennek a figyelmen kívül hagyása.
2. Az állapot lefelé folyik, az esemény felfelé. Egy forrás, ellentmondásmentes modell.
3. A framework a peremen marad. A domain JVM-en tesztelhető.
4. A fő szál nem blokkol. Activity context nem kerül hosszú életű tartóba.
5. Előbb mérsz, release-en, és csak utána optimalizálsz.
6. Az engedély a minimum. Ami az appon kívülről jön, nem megbízható.
7. A lint, a Detekt, a LeakCanary és a CI kényszeríti a fentieket. A review a tervezést nézi.
