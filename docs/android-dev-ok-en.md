# Modern Android development

An opinionated field guide for production Android apps in Kotlin.

The samples match the stable toolchain of September 2026: Kotlin 2.4.20, Android Gradle Plugin 9.4.x, Gradle 9.6, JDK 17, Jetpack Compose (strong skipping on by default), Android Studio Quail (2026.1.x), target Android 16 (API 36). Version numbers move. Put the current stable release from the release notes into the catalog.

## How to read this

Each chapter answers three questions:

1. What is the correct approach today?
2. What does the bad version look like, and how do you spot it in review?
3. How do you prevent the whole class of bug, preferably with a tool?

Markers:

- **Bad practice** — the anti-pattern and the signs that give it away.
- **Good practice** — the replacement.
- **Pitfall** — it compiles and appears to work, then becomes a leak, an ANR, jank, or data loss.
- **How to catch it** — a linter, a test, or a heuristic that flags it mechanically.

### What not to choose in new code

Most of these still exist on the platform. New code does not build on them.

| Old tool | Where it belongs now |
|---|---|
| Java as the app language, Eclipse + ADT | Kotlin, Android Studio, Gradle Kotlin DSL |
| One Activity per screen, `findViewById` | One Activity, Jetpack Compose, type-safe navigation |
| XML layout as the primary UI, `AsyncTask` | Compose, coroutines + Flow |
| `SharedPreferences` for new state | DataStore |
| Raw `SQLiteOpenHelper` | Room 3 |
| `HttpURLConnection` and hand-written JSON | Retrofit or Ktor, kotlinx.serialization |
| Background `Service` and polling `AlarmManager` | WorkManager, a typed foreground service, push |
| `android.hardware.Camera` | The system camera contract, or CameraX |
| `LocationManager` polling for ordinary location | Fused Location Provider |
| Manual wiring, no architecture | Hilt, layers, unidirectional data flow |
| `startActivityForResult` | Activity Result API |
| `LocalBroadcastManager` | A shared Flow or a repository |

---

# 1. The platform

## 1.1 You do not own the lifecycle

Android is a Linux-based, sandboxed, component system. The system, not your `main()`, creates, pauses, recreates, and kills your process and your components. Leaked `Context`, lost state, and most ANRs come from that fact.

## 1.2 compileSdk, targetSdk, minSdk

Three different numbers. Do not mix them up with the marketing version.

| Marketing version | API level |
|---|---|
| Android 14 | 34 |
| Android 15 | 35 |
| Android 16 | 36 |
| Android 17 | 37 |

- **`compileSdk`** — the API you compile against. By itself it does not opt the app into new runtime behavior. Use the latest stable level your toolchain can compile.
- **`targetSdk`** — the level whose behavior changes you accept. Raising it is its own change: read that release's behavior-changes page and test. From 31 August 2026, a new app or an app update on phones, tablets, foldables, and Android Auto must target API 36 to ship on Play. An existing app below API 35 disappears for new users on newer OS versions. Wear OS and Android Automotive OS use API 35 as the update floor; Android TV and Android XR use API 34. An extension can be requested through 1 November 2026. Form-factor exceptions live in the current Play target API policy.
- **`minSdk`** — the oldest device you support. In 2026, `24` (Android 7) is still defensible when reach matters. `26` (Android 8) removes the pre-notification-channel branch. The floor comes from your users' API distribution, not from a generic percentage.

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

Always set `targetSdk`. If it is missing, AGP 9 copies `compileSdk` into `targetSdk`. Older AGP defaulted a missing `targetSdk` to `minSdk`. The implicit copy opts the app into behavior changes without a deliberate bump.

**Pitfall — treating targetSdk as cosmetic.** Each bump can change runtime behavior: scoped storage, the notification permission, foreground-service types, exact alarms, forced edge-to-edge, orientation limits ignored on large screens. Bump it in its own change, after reading the behavior-changes page.

**Good practice.** A version-gated call gets `@RequiresApi` or a `Build.VERSION.SDK_INT` guard, not an empty `try/catch`. Look at the AndroidX compat wrapper first (`ContextCompat`, `NotificationManagerCompat`, window insets).

### What target API 35 and 36 actually change

- **Edge-to-edge.** From API 35 the system draws content behind the system bars. The API 35 temporary opt-out is not a release plan. Call `enableEdgeToEdge()` and consume insets.
- **Large screens.** On API 36, displays with a smallest width of at least 600 dp ignore the app's orientation, resizability, and aspect-ratio limits. A phone-locked portrait layout breaks on tablets and foldables if you never test it there.
- **Predictive back.** Back is a system gesture with an animation. `onBackPressed()` is obsolete. Use `BackHandler` in Compose and `OnBackPressedDispatcher` elsewhere. `android:enableOnBackInvokedCallback` in the manifest has to match the navigation you actually implement.
- **16 KB memory pages.** An app targeting API 35 or higher that ships native `.so` libraries must be aligned for 16 KB pages on 64-bit devices. Pure Kotlin and Java, including their dependencies, already comply. NDK r28 and current AGP align native code you compile. A prebuilt `.so` inside an AAR can only be fixed by whoever built it. The enforcement date shown in Play Console for that app is the date that applies; do not trust an old blog post.

## 1.3 Gradle Kotlin DSL and the version catalog

**Bad practice.** Hard-coded versions scattered through Groovy scripts, and a `def libVersion` copied per module.

**Good practice.** One `gradle/libs.versions.toml`, Kotlin DSL, and a convention plugin for the shared setup.

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

AGP 9 compiles Kotlin in Android application and library modules by default. Applying `org.jetbrains.kotlin.android` fails. The Compose compiler plugin and the serialization plugin stay, and they track your Kotlin version. `kotlinCompilerExtensionVersion` and the `android.kotlinOptions` block are gone. Compiler options belong in `kotlin.compilerOptions`.

Since KSP 2.3 the version is no longer `<kotlin>-<ksp>`. 2.3.11 is the September 2026 stable, and it lines up loosely with Kotlin 2.4. KSP1 is not compatible with AGP 9. In an ordinary Android module, `ksp(...)` feeds the main source set. Test sources need `kspTest` or `kspAndroidTest`. In KMP the catch-all `ksp` configuration is obsolete: each target gets `kspAndroid`, `kspJvm`, and so on. New modules have no KAPT. The `org.jetbrains.kotlin.kapt` plugin is incompatible with AGP 9 built-in Kotlin.

Room's Kotlin extensions live in the runtime artifact. A separate `room-ktx` dependency is unnecessary; that artifact has been empty since Room 2.6.

**How to catch it.** The Ben Manes `dependencyUpdates` plugin reports available upgrades. It does not fail the build when the catalog has a duplicate version: Gradle and review do. The CI gate is lint, tests, and the dependency-analysis plugin. A version bump stays a deliberate change, not a plugin that rewrites the catalog on its own.

## 1.4 Module shape

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

The manifest is one Activity, the permissions, and the service, receiver, and provider entries you actually need. More Activities are justified for an entry point another app calls, a separate task, Android TV, or a widget configuration screen.

**Pitfall — exported components.** Since API 31 every component with an intent filter must set `android:exported`. Anything that is not the launcher and not a deliberate external entry point is `false`.

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

## 1.5 Shipping

- Play's upload format is the Android App Bundle (`.aab`). Play generates the device-specific APKs.
- New apps use Play App Signing. Google holds the app signing key. You hold the upload key. A lost upload key can be reset from Play Console. You do not custody the signing key, and the two keys are not both your secret.
- `versionCode` is a strictly increasing integer. `versionName` is for humans.
- Staged rollout and Play vitals (crash rate, ANR rate, excessive wakeups) are the production signal. Without the mapping file an R8 stack trace is unreadable.

---

# 2. Kotlin

## 2.1 Nullability is an invariant

`T` and `T?` are different types. `!!` claims the compiler is wrong. In review, every `!!` is a question: what guarantees the value is non-null?

```kotlin
val user = repository.currentUser() ?: return navigateToLogin()
showProfile(user)
```

**How to catch it.** Detekt `UnsafeCallOnNullableType`.

**Pitfall — platform types.** Values coming from Java and from parts of the Android SDK arrive as `String!`: the compiler does not enforce nullability. Decide `String` or `String?` once, at the boundary, and pass a Kotlin type inward from there.

## 2.2 Sealed state and value classes

```kotlin
@JvmInline value class UserId(val value: String)

data class User(val id: UserId, val name: String, val email: String)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Failure(val error: AppError) : UiState<Nothing>
}
```

A `Throwable` is a weak UI-state model: Compose does not treat it as stable, and it can carry sensitive detail onto the screen. A user-facing error is its own type.

**Bad practice.** Separate `isLoading`, `isError`, `data`, and `errorMessage`. They can all be true at once. A sealed state makes that combination unrepresentable.

A value class often disappears at runtime, but it can be boxed. Its job is to stop one `String` id from being passed where another `String` id belongs.

## 2.3 Scope functions

Each one has a usual job. Do not nest them.

- `apply` — configure the object and return it.
- `also` — a side effect, and the receiver continues.
- `let` — transform a nullable, or open a short temporary scope.
- `run` and `with` — compute a result from a receiver.

If `it` is ambiguous, the block becomes a named function.

## 2.4 Coroutines

`AsyncTask` is obsolete. It is still in the platform API. New code uses coroutines.

1. Every coroutine belongs to a scope: `viewModelScope`, `lifecycleScope`, or a scope you create and cancel. There is no `GlobalScope`.
2. A suspend function is safe to call from the main thread. If it does blocking work, it moves itself with `withContext(Dispatchers.IO)`. The caller does not.
3. Cancellation is cooperative. A long loop needs a suspend point or `ensureActive()`. `CancellationException` propagates.

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

**Bad practice.** `GlobalScope.launch`. It outlives the screen, and nothing cancels it.

**Pitfall — `runCatching` and a broad `catch (e: Exception)`.** Both catch `CancellationException`, because it extends `Exception`. `kotlin.runCatching` does not rethrow it. Around cancellable work, catch the specific failure and rethrow cancellation. `ensureActive()` after a broad catch does the same job.

**Pitfall — the repository knows about the main thread.** `withContext(Dispatchers.Main)` inside a repository inverts the layers. Dispatcher choice stays at the edge, behind an injected `AppDispatchers`, so a test can install a `TestDispatcher`.

| Dispatcher | Use |
|---|---|
| `Main` | UI. `Main.immediate` avoids a pointless redispatch when you are already on the main thread. |
| `IO` | Blocking I/O. Large pool. |
| `Default` | CPU work: parsing, sorting, image processing. |

Retrofit and Ktor suspend calls already do not block the main thread. An extra `withContext(Dispatchers.IO)` around them is harmless and is not what makes them main-safe.

## 2.5 Flow

`Flow` is cold. `StateFlow` is hot and always has a value. For new screen state, this is the place `LiveData` used to occupy. `LiveData` is still on the platform; new code does not choose it.

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

The Flow `catch` operator does not swallow `CancellationException`. `WhileSubscribed(5_000)` is for screen state: it survives rotation and drops the upstream after a real background stay. `Eagerly` and `Lazily` are for a process-wide stream that must stay alive with no subscribers. On screen state they keep work running for no reason.

In Compose, collection follows the lifecycle:

```kotlin
val state by viewModel.results.collectAsStateWithLifecycle()
```

`collectAsState()` lives with composition, including while the Activity is `STOPPED`. `collectAsStateWithLifecycle()` comes from `lifecycle-runtime-compose` and collects only at `STARTED` and above.

A one-shot event (navigation, snackbar) is not a `StateFlow`. It would replay on rotation and on a new subscriber. The better model: the event is part of UI state and is cleared after it is handled. A `Channel` can drop the event when nothing is collecting.

## 2.6 Extensions, delegation, lazy

Extensions keep a domain model clean and hold edge glue (`User.toUiModel()`). A property getter does no I/O and no heavy work. Reading a property is cheap.

`by lazy` is synchronized by default and is for one-time initialization. It does not replace a lifecycle. `by viewModels()` and `by hiltViewModel()` provide a scoped ViewModel.

## 2.7 Generics and inline

`out` on producers, `in` on consumers. `inline` and `reified` pay off when the type must survive to runtime, or when you remove a lambda allocation from a higher-order function. Inline everywhere grows bytecode.

## 2.8 Kotlin 2.4

- The K2 compiler has been the default since 2.0. The Compose compiler plugin version matches the Kotlin version.
- Context parameters have been stable since 2.4.0. Explicit context arguments and callable references are still experimental and need their own compiler flag. Context parameters are for a rare, explicit ambient dependency (`Logger`, `Clock`). They are not a service locator, and they are not a way to delete every parameter from a signature. Constructors cannot declare them.
- `kotlinx.serialization` generates code at compile time, without reflection. New JSON boundaries use it, not Gson.

---

# 3. Architecture

## 3.1 Components

| Component | Role today |
|---|---|
| Activity | The app has one Activity, hosting Compose and navigation. Another Activity only for a real entry point. |
| Service | Rare. A foreground service only for work the user can see and is waiting on. |
| BroadcastReceiver | In the background, most implicit broadcasts never reach a manifest-registered receiver. Use a receiver registered while running, or WorkManager. |
| ContentProvider | `FileProvider` and sharing with another app. Your own data is Room and a repository. |

On a configuration change (rotation, theme, language, window size) the Activity is recreated. The ViewModel survives that. It does not survive process death. `SavedStateHandle`, `rememberSaveable`, and a real store (Room, DataStore) do. Test both: rotate, and use "Don't keep activities" or kill the process from the background.

**Bad practice.** State in an Activity field or a `companion object`. The field disappears on rotation. The static field leaks, and process death drops it anyway.

`SavedStateHandle` stores the same kinds of values as a `Bundle`: primitives, `String`, and a narrow set of supported arrays and `Parcelable`s. An arbitrary `List<Item>` does not belong there. The handle holds an id and small UI state. Cart contents go to Room or DataStore.

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

## 3.2 Layers and unidirectional data flow

```
UI        Composable  ← state —  ViewModel
              │  — event →       │
Domain    use cases, plain Kotlin, no Android imports
Data      repository → Room, network, DataStore
```

- State flows down, events flow up. The UI does not write shared mutable state.
- Dependencies point inward. The domain module has no Android imports and runs in a plain JVM test.
- The repository is the single source of the data. The UI does not call an API or open a database.
- A ViewModel does not hold an Activity, a View, or a navigation controller. An `Application` context is legal and still a smell: the call that needs a context stays in the data layer, and the ViewModel receives a repository.
- The domain layer is optional. It earns its place when the same rule runs on more than one screen, or when you want to test the rule without Android. Wrapping a single repository call in an empty use case is not architecture.

**Bad practice.** Network, database, JSON, and formatting in one class. The tell: the file imports `android.*`, `okhttp3.*`, and `androidx.room3.*`.

## 3.3 ViewModel

Screen state is one immutable `StateFlow` of a sealed state, or of a single data class that cannot contradict itself. The mutable holder is private. Updates go through `update { copy(...) }`.

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

**Bad practice.** A public `MutableStateFlow` or `MutableLiveData`. The UI then writes state itself.

## 3.4 Hilt

Hilt is the usual choice in an Android app. Koin is lighter, runtime-based, and common in KMP. Mixing the two in one module is worse than either one alone.

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

A screen uses `hiltViewModel()`. The current artifact is `androidx.hilt:hilt-lifecycle-viewmodel-compose`. The old `hilt-navigation-compose` name is not the only handle: check which artifact exports the function on the Hilt line you use.

**Good practice.** Domain code sees the `TodoRepository` interface. The implementation lives in the data module and is bound with `@Binds`. A test supplies a `FakeTodoRepository`.

**Pitfall — everything is `@Singleton`.** A singleton lives for the process and keeps whatever it holds. An Activity context inside a singleton is a classic leak. A narrower scope (`@ViewModelScoped`) is only needed when one ViewModel instance shares the object. An unscoped provider creates a new instance on every request.

A Hilt worker also needs `androidx.hilt:hilt-work` and its compiler, not only `hilt-android-compiler`.

## 3.5 Modules

One module is enough while build time and ownership still work. After that:

- `:app` — Application, Activity, navigation root, DI assembly.
- `:feature:*` — one feature. It does not depend on another feature's internals. If two features talk, the contract lives in `:core` or in a separate API module.
- `:core:ui`, `:core:data`, `:core:domain`, `:core:designsystem`.

Convention plugins live in `build-logic`, not in copied `build.gradle.kts` fragments. The dependency-analysis plugin, or an explicit Gradle rule, fails CI when a boundary is crossed.

---

# 4. Jetpack Compose

## 4.1 UI = f(state)

A composable describes what to show. You do not hold a `TextView` and you do not call `setText`. The three phases are composition, layout, and drawing. Recomposition reruns only the affected part, and it is cheap only when skipping works.

## 4.2 remember and state hoisting

```kotlin
@Composable
fun Counter() {
    var count by rememberSaveable { mutableIntStateOf(0) }
    Button(onClick = { count++ }) { Text("Count: $count") }
}
```

`remember` survives recomposition. `rememberSaveable` also survives a configuration change and process death, when the value has a `Saver`. They are not the same thing. Rotation is a configuration change. Killing the process is process death. `rememberSaveable` covers both. `remember` dies with composition. A ViewModel survives the configuration change and does not survive the process.

**Good practice.** State lives at the caller. A leaf composable receives state and emits events.

```kotlin
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(value = query, onValueChange = onQueryChange)
}
```

**Pitfall.** `remember { mutableStateOf(param) }` does not follow later changes of `param`. Key it: `remember(param) { ... }`, or compute it with `derivedStateOf` when the calculation is expensive.

## 4.3 Route and Screen

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

`Route` knows the ViewModel. `Screen` does not. Preview and Compose tests feed `Screen`.

## 4.4 Lists

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

`key` is stable and unique. The list index lies across reorder and insert. A long or unbounded list uses `LazyColumn`, `LazyRow`, or `LazyVerticalGrid`. `Column` plus `verticalScroll` composes every item.

## 4.5 Side effects

A composable body can run many times, in an unpredictable order. Side effects live in an effect.

- `LaunchedEffect(key)` — a coroutine tied to composition. It restarts when the key changes and is cancelled on leave.
- `rememberCoroutineScope()` — a coroutine started from a callback, with the composition's lifetime.
- `DisposableEffect` — registration, with `onDispose` as its pair.
- `rememberUpdatedState` — a long-lived effect sees the latest value without changing its key.
- `derivedStateOf` — recomputes only when the output actually changes. Typical for a scroll threshold.

**Bad practice.** `vm.load()` directly in the composable body. Every recomposition starts it. Use `LaunchedEffect(Unit)`, or start the load in the ViewModel `init` when it belongs to entering the screen.

## 4.6 Material 3

Material 3, `MaterialTheme`, and dynamic color from Android 12 (API 31) when the product wants it.

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

Color and size come from the theme. Scattered `Color(0xFF...)` and a magic `16.dp` turn dark theme and rebranding into a search problem.

Call `enableEdgeToEdge()` in `onCreate`, before `setContent`. Content uses `Modifier.safeDrawingPadding()`, Scaffold insets, or window insets you handle on purpose. A button drawn under the system bar, where it cannot be tapped, is a bug.

## 4.7 Stability

Compose skips a composable when its parameters are unchanged. With strong skipping (the default), a composable with unstable parameters can still be skipped when every argument is the **same instance**. For a stable type, `equals` is what counts. `List`, `Set`, and `Map` are unstable because the interface can hide a mutable implementation. A data class is stable only when every property is stable.

`@Immutable` and `@Stable` are therefore not redundant. You are promising that `equals` is trustworthy and that Compose may skip on that basis. A false promise becomes a silent UI bug. An `ImmutableList` from `kotlinx.collections.immutable` is stable in the compiler's eyes only when it is compiled in the module or named in a stability configuration. `@Immutable` on a wrapper data class is the same kind of promise.

**Bad practice.** Passing a `Flow`, `LiveData`, or a mutable collection into a leaf composable. Collect the flow once, in the route, and pass immutable state down.

Typical recomposition failures:

1. An unstable parameter that is a new instance on every pass.
2. Allocating a heavy object during composition.
3. Reading fast-changing state too high in the tree (scroll). Read it in the smallest scope, or behind `derivedStateOf` and a lambda `Modifier` (`Modifier.offset { }`), so layout or draw reruns instead of the whole composition.
4. A missing `key`.
5. `java.util.Date` and `Calendar` in UI state. Use `kotlinx.datetime` or your own immutable value.

Measurement:

- Layout Inspector recomposition counts.
- Compose compiler reports: `reportsDestination` on the `composeCompiler` block. The old scattered compiler flag is not the current entry point.
- Macrobenchmark and Baseline Profiles, on a release build, with R8. You cannot read performance from a debug build.

**How to catch it.** Detekt, Android Lint, and the Compose rule set (`io.nlopez.compose.rules`, the maintained line of the old Twitter rules).

## 4.8 Preview and accessibility

A stateless `Screen` renders under `@Preview` and `@PreviewParameter` without a device.

Accessibility is part of the UI. An icon that means something gets a `contentDescription`. A decorative icon uses `null`, or the screen reader says it twice. Touch targets are at least 48 dp; Material 3 `minimumInteractiveComponentSize()` puts that size around the component. Contrast and semantics (`Modifier.semantics`) belong on a control that is not already text.

---

# 5. Intents, navigation, results

## 5.1 Explicit and implicit intents

```kotlin
startActivity(Intent(this, DetailActivity::class.java))

val intent = Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
try {
    startActivity(intent)
} catch (_: ActivityNotFoundException) {
    showNoHandler()
}
```

**Pitfall — package visibility.** Since API 30, `resolveActivity` can return `null` for an app that is installed, when the manifest `<queries>` element does not cover the intent. A `resolveActivity != null` check is therefore a false sense of safety on its own. `try/catch` still reaches a real handler you cannot see. A target you know you launch is declared in `<queries>`.

**Pitfall — PendingIntent.** Since API 31, `FLAG_IMMUTABLE` or `FLAG_MUTABLE` is required. The default is `FLAG_IMMUTABLE`, often together with `FLAG_UPDATE_CURRENT`. A mutable flag is only for the case where the receiving app fills in the intent (some notification reply fields, for example).

## 5.2 Navigation inside the app

There are two supported, type-safe paths.

**Navigation Compose 2** (2.10.x) with a type-safe destination. The library owns the back stack, deep links, and `NavController`. It is the strong choice when you need App Links, multiple back stacks, and navigation behavior that is already built.

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

This needs the Kotlin serialization plugin and the `kotlinx-serialization` dependency. A new graph does not need string routes or hand-written `navArgument`s.

**Navigation 3** is the stable line built for Compose. The back stack is a list you own. `entryProvider` turns a typed key into a composable, and `NavDisplay` draws it. It requires `compileSdk` 36. Deep links and a complicated multi-module graph are more of your own code than in Navigation 2. It is a good base for a new Compose-only app when the team is willing to own that state. An existing graph with deep links moves to type-safe Navigation 2 first, and only then looks at Navigation 3.

Both paths pass an id, not a whole object and not a bitmap. Arguments travel through saved state. A large payload throws `TransactionTooLargeException`.

**Pitfall.** A `NavController` inside a ViewModel ties the model to the UI and leaks. The ViewModel exposes an event or state. The composable navigates, because it holds the controller or the back stack.

## 5.3 Activity Result

`startActivityForResult` is obsolete. It has not been removed from the platform API. New code uses the Activity Result contracts. They also survive process death.

Picking an image uses the photo picker, with no storage permission:

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

`GetContent` and raw `ACTION_GET_CONTENT` are the older path. A photo taken without your own camera UI uses `TakePicture`, but that contract expects a destination `Uri` you created first (typically through `FileProvider`) and returns a `Boolean`: whether the save succeeded. `TakePicturePreview` returns only a small bitmap, not a saved file.

Permission requests are contracts too: `RequestPermission`, `RequestMultiplePermissions`.

## 5.4 Deep links and App Links

An intent filter, or a navigation deep link, takes a URL or a notification to the right screen. An Android App Link (`https`, `autoVerify`, Digital Asset Links) opens the app with no disambiguation dialog once the domain is verified. An unverified `https` link shows a chooser, or stays in the browser.

## 5.5 Broadcasts

Since API 26, a manifest-registered receiver does not get most implicit broadcasts while the app is in the background. There are exceptions (`BOOT_COMPLETED` and the documented list). An in-app event is not a broadcast: it is a Flow or a repository. `LocalBroadcastManager` is obsolete.

`RECEIVER_EXPORTED` and `RECEIVER_NOT_EXPORTED` have existed since API 33. From target API 34, a receiver registered at runtime must set one of them. The default is `RECEIVER_NOT_EXPORTED`, unless you deliberately want another app's broadcast.

A deferrable reaction to a system condition (network, charging) is a WorkManager constraint, not a receiver that does the work inside `onReceive`.

---

# 6. Storage

## 6.1 DataStore

The first `SharedPreferences` read loads from disk on the calling thread. `commit()` writes on the main thread. New settings do not use it. The type is still on the platform.

Preferences DataStore is for a small key-value set. The delegate is a single top-level instance. A second DataStore created inside a class or a function, pointed at the same file, throws.

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

Structured settings use a custom `Serializer`, often with kotlinx.serialization. That is not Proto DataStore. Proto DataStore uses a Protocol Buffers schema. Mixing the names pulls in the wrong dependency.

Existing prefs have `SharedPreferencesMigration`. It runs once, on the first read. After that there is one store, not two.

## 6.2 Files

- **Internal storage** (`filesDir`, `cacheDir`): private, no permission. Removed with the app. `cacheDir` can be wiped under pressure.
- **Photo picker and MediaStore**: the user's pictures. In new code the picker is the first choice, with no permission. Reading the whole media library (`READ_MEDIA_IMAGES` and its siblings) is a separate, reviewed permission. Since API 34 the user can grant partial access (`READ_MEDIA_VISUAL_USER_SELECTED`).
- **Storage Access Framework**: the user picks a document or a tree. Persist the grant with `takePersistableUriPermission`.
- **FileProvider**: your own file as a `content://` URI, with a temporary grant. A `file://` URI throws `FileUriExposedException` (API 24+).

**Bad practice.** `MANAGE_EXTERNAL_STORAGE` so you can skip learning scoped storage. Play limits this to file managers and backup tools.

**Pitfall.** File I/O on the main thread. A suspend function and `Dispatchers.IO`. StrictMode flags it in debug.

Backup: since API 31, `android:dataExtractionRules` and `android:fullBackupContent` say what may enter cloud backup and device transfer. The default can take data the user does not expect (a token, a database). The rules are deliberate.

## 6.3 Small, transient state

| Place | What it survives | What you put there |
|---|---|---|
| `remember` | recomposition | cheap, ephemeral UI |
| `rememberSaveable` | configuration change and process death | small, saveable UI (scroll, text) |
| `SavedStateHandle` | process death, after the ViewModel is recreated | small keys |
| Room, DataStore | everything | anything the user cares about |

A large blob in saved state throws `TransactionTooLargeException`.

---

# 7. Room

A new database is Room 3: `androidx.room3`, KSP, coroutine-first DAOs, and a required `SQLiteDriver`. Room 2.8 is the maintenance line. WorkManager brings its own internal Room 2 dependency; the package names differ, so both can sit on one classpath. Sharing one database file between them is not the goal.

## 7.1 Schema

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

`@Query` SQL is checked at compile time. `@Insert(onConflict = REPLACE)` is not an upsert: it deletes the row and inserts a new one, which can cascade and change the row id. `@Upsert` is the operation that means upsert.

The Room Gradle plugin (the artifact that matches the Room major version) makes schema export reproducible and cacheable. Commit the exported JSON.

## 7.2 Reads and mapping

A DAO `Flow` emits again when the table is invalidated. The entity stays in the data layer. The repository maps it to a domain type.

New code has no blocking DAO methods. Room treats a synchronous call on the main thread as an error by default.

## 7.3 Migrations

**Bad practice.** `fallbackToDestructiveMigration()` in production. A schema change drops the data.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            "ALTER TABLE todos ADD COLUMN priority INTEGER NOT NULL DEFAULT 0",
        )
    }
}
```

In Room 3, `migrate` is suspend and receives a `SQLiteConnection`, not a `SupportSQLiteDatabase`. Without a driver a Room 3 database does not build. An Android app uses `BundledSQLiteDriver` (the same SQLite on every device) or `AndroidSQLiteDriver` (the system SQLite). After the driver is set, old `SupportSQLiteDatabase` calls no longer work unless you deliberately use the `room3-sqlite-wrapper` bridge.

Test every migration path with `MigrationTestHelper`, against the exported schema.

## 7.4 Queries

- `@Relation` is not a SQL JOIN. One query reads the parents, another reads the children in a batch. That avoids N+1 and it is still two queries. A single query is a `JOIN` you write in `@Query`.
- Batched writes use `@Transaction` or `withTransaction`. One commit, and either every row is there or none of them are.
- Index the column you filter or sort on. `EXPLAIN QUERY PLAN` tells you whether the index is used.
- A secret does not go into a plain column. The key lives in Keystore or under Tink. Room 3 encryption is a capability of the SQLite driver you chose, not a `SupportFactory` on the old SupportSQLite path.

A large, unbounded list uses Paging 3: a `PagingSource` from the DAO, a `RemoteMediator` when the network and Room page together. The Compose side is `collectAsLazyPagingItems()`.

---

# 8. Networking

## 8.1 Client

Retrofit + OkHttp, or Ktor. The boundary is a suspend function. JSON is kotlinx.serialization.

```kotlin
@Serializable
data class UserDto(val id: String, val name: String, val email: String)

interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto
}
```

The kotlinx.serialization Retrofit converter is a separate artifact (the Jake Wharton converter is the usual one). `Json { ignoreUnknownKeys = true }` is forward compatibility: an unknown field does not crash the app. `explicitNulls = false` is a deliberate encoding choice, not a default to copy without thinking.

## 8.2 Error model

The repository calls the API, not the ViewModel and not the composable. The output is a typed result. `CancellationException` propagates.

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

Retrofit throws `HttpException` for a non-2xx response. Ktor has its own exception type. Do not mix the two in one `catch` branch.

**Pitfall.** Without a timeout the call hangs under Doze and on a bad network. OkHttp `connectTimeout` and `readTimeout` are set. Retry only idempotent requests, with backoff, and with a cap. Token refresh is single-flight, or one expired token starts a dozen parallel refreshes.

## 8.3 Offline

The UI observes Room. Refresh is a separate suspend call. If refresh runs inside the flow's `onStart` block, the first value from cache waits for the network, and offline-first becomes offline-last.

```kotlin
fun observeUser(id: String): Flow<User?> =
    dao.observeUser(id).map { entity -> entity?.toDomain() }

suspend fun refreshUser(id: String) {
    val dto = api.getUser(id)
    dao.upsert(dto.toEntity())
}
```

The ViewModel collects `observeUser` and calls `refreshUser` from a separate `launch`. The screen shows the cache immediately, then updates.

OkHttp's HTTP cache is for GETs that honor `Cache-Control`. Coil 3 loads images (`coil3.compose.AsyncImage`): lifecycle, memory cache, disk cache, and sampling to the target size. A hand-rolled bitmap loader is the classic `OutOfMemoryError`.

## 8.4 On the wire

- HTTPS only. Cleartext has been blocked by default since target API 28. `cleartextTrafficPermitted` is not the fix for a bad certificate.
- Certificate pinning only where the threat justifies it, with a backup pin and a rotation plan. A single expiring pin locks the app out for users.
- Tokens and personal data do not go into logs. The OkHttp logging interceptor exists only in the debug variant, and the `Authorization` header is redacted.

---

# 9. Background work

Work tied to a screen is a coroutine in `viewModelScope`. Work that must finish after the screen and the process are gone is WorkManager. Swapping the two is the typical bug: an upload inside the ViewModel dies when the user navigates away, and a polling service tries to run when the system will not let it.

## 9.1 Limits

Since API 26:

- Starting a background service from the background throws `IllegalStateException`.
- Most implicit broadcasts do not wake a manifest receiver.
- Exact alarms need a separate permission and a Play review. They are for alarms, calendars, and timing the user can see. They are not for sync.
- Doze and App Standby defer work that can wait.

You do not run whenever you want. You describe the work and the constraints, and the system schedules it.

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

The shortest interval for periodic work is 15 minutes. A 6-hour request that requires charging and an unmetered network is realistic. `enqueueUniquePeriodicWork` with `KEEP` does not schedule a second copy. `UPDATE` is for when the constraints changed and you want to replace the existing request. `Result.retry()` gets exponential backoff.

Expedited work is quota-limited, and it is not a foreground service. It moves a short piece of work, which the user just asked for, ahead in the queue. It is not an endless sync.

**Bad practice.** A background `Service` and `while (true)` polling a server. The OS stops it and it drains the battery. WorkManager or push belongs there instead.

## 9.3 Foreground services

Use one when the user can see this work right now: playback, navigation, recording a workout, a call, a file transfer they started.

- From target API 34 the type is required in the manifest and in the `startForeground` call (`location`, `mediaPlayback`, `dataSync`, `camera`, and the other documented types), plus the permission that belongs to that type.
- Call `startForeground` shortly after `startForegroundService`. Otherwise the system stops the service.
- Since API 31, starting a foreground service from the background is limited to the documented exemptions.
- On API 35 a `dataSync` foreground service has a finite daily budget (6 hours inside 24 hours). It is the wrong tool for a long, invisible sync.
- Play reviews whether the user has a visible reason. No reason: WorkManager.

The notification a foreground service posts is exempt from `POST_NOTIFICATIONS`. Every other notification needs that permission from API 33.

## 9.4 Push

A server-driven update is Firebase Cloud Messaging or another push, not a periodic wakeup. High-priority FCM is for an urgent message the user should see immediately, and it is quota-limited. Background sync on high priority burns the quota and the battery.

---

# 10. Location and maps

## 10.1 Fused Location Provider

Ordinary location uses the Fused Location Provider, not `LocationManager` polling. `LocationManager` remains for special GNSS and passive cases.

`Task.await()` comes from `kotlinx-coroutines-play-services`. Continuous updates use `callbackFlow`, and `awaitClose` calls `removeLocationUpdates`. The callback does no heavy work: it arrives on the main looper.

`PRIORITY_HIGH_ACCURACY` keeps GPS running. Use balanced or low power unless you need meter-level accuracy. Updates stop when the collector stops.

## 10.2 Permission

- `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION` are runtime permissions. From Android 12 the user can grant approximate accuracy even when you asked for fine. Handle that state.
- `ACCESS_BACKGROUND_LOCATION` is a separate, second step (API 29+). From Android 11 it cannot be requested in the same dialog as foreground location. Play reviews it strictly. Ask only when the feature needs location in the background and you can explain why.
- Asking for fine and background up front, "just in case", gets a denial from the user and can get a rejection from Play.

The Activity Result permission contract handles denial, "don't ask again", and an approximate grant. On denial the feature has a narrower path that still works.

## 10.3 Maps

The declarative Google map surface is the Maps SDK plus Maps Compose. Restrict the key to the app and the package name. `Geocoder.getFromLocation` blocks. From API 33 there is a listener-based async form, and not every device has a geocoder.

For offline or heavy maps, MapLibre and similar engines are real alternatives. The choice is a product decision, not a single correct SDK.

---

# 11. Camera, playback, images

## 11.1 Camera

One photo is the system camera and the `TakePicture` contract. CameraX is for your own preview, custom exposure, or live analysis (ML).

CameraX binds use cases to a `LifecycleOwner` and releases the camera when that lifecycle stops. The current suspend entry point is `ProcessCameraProvider.awaitInstance(context)`. An `ImageAnalysis` use case feeds ML. The preview's surface provider is attached, or the use case is alive and there is no image.

## 11.2 Media3

Audio and video: Media3, ExoPlayer, `MediaSession`. Adaptive streaming (DASH, HLS), and background playback through a `MediaSessionService` with the `mediaPlayback` foreground-service type.

An `ExoPlayer` holds codecs and surfaces. `release()` goes in `DisposableEffect.onDispose`, or when the service is torn down. A forgotten player is an expensive leak.

## 11.3 Images

Coil 3 loads the image sampled to the target size. A full-resolution bitmap for a thumbnail is the usual path to `OutOfMemoryError`.

---

# 12. Permissions, telephony, sensors, native code

## 12.1 SMS and call log

`READ_SMS`, `SEND_SMS`, call log, and phone state are permissions Play accepts only when the app's core function is an SMS or dialer app. A convenience feature is rejected.

Sending an SMS through the user's app: `ACTION_SENDTO` and `smsto:`. No permission. OTP uses the SMS Retriever API, with no SMS permission.

## 12.2 Sensors

Bind the listener to visibility and remove it in the pair. In Compose that is `DisposableEffect`; elsewhere a `DefaultLifecycleObserver`. A `SensorEventListener` you never remove keeps the Activity alive and leaves the sensor on.

`getSystemService` can return null when the device has no such sensor. Return early instead of `!!`. Use the slowest `SENSOR_DELAY_*` the feature can tolerate.

## 12.3 NDK

Native code (C, C++, or Rust through the NDK) needs a reason: a measured CPU bottleneck, an existing native library, or hard real-time audio. Everything else is Kotlin on `Dispatchers.Default`. The native boundary stays thin, behind a Kotlin API. 16 KB page alignment applies to your own `.so` files and to every prebuilt `.so` you ship.

## 12.4 Permissions in general

- Ask at the moment of use, with a rationale (`shouldShowRequestPermissionRationale`).
- Denial is a normal path, not an exception.
- The photo picker, SAF, and SMS Retriever are better because they solve the same job with no permission.
- `POST_NOTIFICATIONS` is required from API 33 for an ordinary notification. A foreground service's own notification is the exception. The user can still silence notifications.
- Play and the user see every permission in the manifest. An unused permission is risk.

## 12.5 Sign-in

New sign-in goes through Credential Manager (`androidx.credentials`): passwords, passkeys, and Google accounts on that same gate. The old `GoogleSignInClient` and a password form hidden in a WebView are not the current path. The token is for the server. A session kept on the client goes into storage protected by Keystore, not into a logging `SharedPreferences`, and not baked into source.

---

# 13. Tests

The pyramid:

- **Unit**, most of them. ViewModels, use cases, mappers, repositories with fakes. JVM, no device.
- **Integration**, fewer. Room in memory or with a test driver, DataStore, a repository and a fake network.
- **UI**, few. Compose UI tests against the stateless screen. Instrumented end-to-end only for the critical path. Espresso for a legacy View.

`viewModelScope` uses `Dispatchers.Main`. `runTest` does not replace it. You need a rule that calls `Dispatchers.setMain` and `resetMain` at the end of the test.

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

`StandardTestDispatcher` gives you virtual time, and it needs `advanceUntilIdle`. `UnconfinedTestDispatcher` runs eagerly. Turbine asserts Flow emissions. Inject the dispatcher when the code under test chooses I/O itself. A direct `Dispatchers.IO` call inside the class you are testing cannot be replaced.

`MigrationTestHelper` walks a Room migration against the exported schema, with the same driver the app uses. That test catches destructive-migration data loss, instead of a user catching it.

A Compose test feeds `Screen` with state and asserts on semantics (`onNodeWithText`, `onNodeWithContentDescription`). Screenshot regression uses Paparazzi or Compose screenshot testing.

**Bad practice.** Mocking every dependency and binding the test to call order. Mocking suspend and Flow breaks. A hand-written fake with in-memory state is reusable and readable. A mock belongs where the interaction is the point (an analytics event was sent).

CI gate: unit tests, `lint`, Detekt, formatting (ktlint or Spotless), and the screenshot tests you can afford. A new violation is red. Existing debt sits on a baseline and does not grow.

---

# 14. Memory and performance

## 14.1 Leaks

On Android a leak is typically an Activity, Fragment, View, or Activity `Context` held by something that lives longer. The Activity owns the view tree. Repeated rotation turns that into `OutOfMemoryError`.

1. A static field or a singleton holding an Activity context or a View. What outlives the screen gets `applicationContext`, or better: it does not hold a context.
2. A non-static inner class, listener, or coroutine that captures the Activity `this` and outlives it. The coroutine uses `viewModelScope` or `lifecycleScope`.
3. `register` / `add` / `observe` with no paired `unregister` / `remove`. Sensors, receivers, location callbacks, `ContentObserver`, players, `Handler`.
4. `Handler.postDelayed` capturing a View. On teardown, `removeCallbacksAndMessages(null)`, or a lifecycle scope and `delay`.
5. A ViewModel that holds an Activity or a View. On rotation the ViewModel lives and the Activity should be gone.
6. A full-resolution bitmap and an unbounded cache. Coil, sampling, a bounded cache, `onTrimMemory`.
7. `CoroutineScope(SupervisorJob())` that nobody `cancel`s. The job and the captured reference stay. A framework scope is the default. A scope you create has an owner.

LeakCanary is a debug implementation. It gives a reference chain for a retained Activity, Fragment, and ViewModel. Every report is a bug, not noise. In production, Play vitals memory and OOM rates, plus the Memory Profiler, are how you investigate.

## 14.2 Jank

At 60 Hz a frame is about 16.7 ms. At 90 Hz, 11 ms. At 120 Hz, 8.3 ms. Parsing, a database, disk, and a large allocation on the main thread miss that budget.

In debug, StrictMode with `penaltyLog` flags main-thread disk and network. `penaltyDeath` in an app whose libraries are noisy disturbs more than it protects. `detectAll()` is noisy too; a specific detector (disk, network, an unclosed resource) is easier to read.

The other usual causes: an unstable Compose parameter, scroll state read too high, a missing list `key`, deep unnecessary nesting, allocation on the draw path.

Measure with JankStats, Perfetto, and Macrobenchmark `FrameTimingMetric`. Release, with R8.

## 14.3 Startup

- A Baseline Profile covers the hot code of cold start and the main path. ART compiles it ahead of time at install. The gain depends on the app; the often-quoted 20–30% was a measurement on some samples, not a promise. Measure yours with `StartupTimingMetric`, on release.
- The App Startup library initializes in order, lazily. Heavy SDK initialization in `Application.onCreate` slows every start, including when the user never opens that feature.

## 14.4 ANR

Input becomes an ANR after about 5 seconds of main-thread blocking. Broadcasts and services get a different, documented limit, shorter in the background. Typical causes: main-thread I/O, `runBlocking` on the main thread, deadlock, a long synchronous binder call. `runBlocking` in production UI code is a review block. The ANR rate also affects Play visibility.

## 14.5 R8

In release, `isMinifyEnabled = true` and `isShrinkResources = true`. R8 full mode, if the project does not enable it yet, is a separate property. A library that uses reflection gets a keep rule. The mapping file uploads with the release to Play or to the crash tool. Skipping and runtime on a debug build are not the user's experience.

## 14.6 Checklist before merge

- No main-thread I/O.
- LeakCanary is clean on the screen you touched.
- The lazy list has a stable `key`.
- No `GlobalScope`, no `runBlocking` on the main thread, no scope without an owner.
- Listener, observer, and player registration is paired.
- Images go through Coil, sampled to the target size.
- The Compose report for a hot screen has no surprise unstable parameter.
- What matters is measured on release, with Macrobenchmark.

---

# 15. Smells

## 15.1 Tools, set up once

- Android Lint, with the current `lint { }` block. `lintOptions` is the old DSL. A new warning is a CI failure, or, starting from a baseline, only new findings may grow.
- Detekt: `!!`, empty catch, complexity, coroutine mistakes. With a baseline, debt does not grow.
- ktlint or Spotless: style is not a review topic.
- Compose rules.
- Compose compiler reports for hot screens.
- dependency-analysis: unused dependencies and dependencies in the wrong module.

The ratchet: a baseline today, and from tomorrow a new violation is red. A big-bang cleanup is not a prerequisite.

## 15.2 Catalog

**Concurrency**

- `GlobalScope` → `viewModelScope` or `lifecycleScope`.
- `runBlocking` on the main thread → the caller becomes suspend, or the work leaves the main thread.
- `catch (e: Exception)` and `runCatching` around suspend code → cancellation propagates.
- A hard-coded `Dispatchers.IO` in business code you need to test → an injected dispatcher.

**Lifecycle**

- An Activity, a View, or an Activity context in a static field, an `object`, or a ViewModel → a leak.
- `register` with no pair → a leak and battery drain.
- A `NavController` in the ViewModel → the event belongs to the composable.

**State**

- Public mutable state → a read-only `StateFlow`.
- Boolean flags that do not exclude each other → a sealed state.
- An `android.*` import in domain code → the framework stays at the edge.

**Compose**

- A side effect in the body → `LaunchedEffect` or ViewModel `init`.
- `collectAsState()` on a screen → `collectAsStateWithLifecycle()`.
- A lazy item with no `key`.
- A raw `Flow` or a mutable collection in a leaf.
- A hard-coded color and size → the theme.

**Data**

- `SharedPreferences.commit` or a main-thread read in new code → DataStore.
- `fallbackToDestructiveMigration()` in production → a migration and a test.
- Hand-written JSON or reflection-based Gson on a new boundary → kotlinx.serialization.
- Main-thread file or database access → a suspend DAO, `Dispatchers.IO`.
- `@Insert(REPLACE)` where you meant upsert → `@Upsert`.

**Security and platform**

- `exported="true"` without intent.
- A mutable `PendingIntent` without a reason → `FLAG_IMMUTABLE`.
- A token in a log.
- SMS, call log, background location, all-files access "in case we need it" → the minimum, and a permissionless alternative.
- `cleartextTrafficPermitted` to dodge a certificate failure.
- `resolveActivity` as the only guard, with no package visibility.

## 15.3 Security baseline

- Secret keys live in Keystore, with hardware backing where the device has it. Bulk encryption is Tink or an equivalent, with the key under Keystore. There is no product named "Encrypted DataStore": a custom serializer encrypts the bytes when the preference is a secret.
- An API secret inside the APK can be read out. What is secret stays on the server.
- Least privilege. `exported=false` unless it is an entry point. Scoped storage, the photo picker.
- A deep link, an Intent extra, and data from another app are untrusted. Validate them.
- HTTPS. Pinning only with a reason and a rotation plan.
- Authorization, price, and auth are decided on the server. A client check is UI, not a security boundary.
- R8 raises the cost of reversing the app. Play Integrity is for a high-value flow the server verifies. Neither is a wall.
- Check dependencies (a dependency checker, the Play SDK Index). A transitive vulnerability is yours.

## 15.4 Review questions

Does it block the main thread? Can this reference outlive the Activity? Does state come from one source, and can it contradict itself? Does the effect and the registration have a pair? What happens offline, on denial, on rotation, and after process death? Is the permission minimal? Is the native library 16 KB aligned? Is this the same behavior in release, with R8?

---

# 16. Build and release

## 16.1 Variants

The `debug` variant is debuggable, with StrictMode, LeakCanary, and logging. The `release` variant is built with R8, shrunk resources, a signature, and no token logging.

A product flavor (`dev`, `staging`, `prod`) selects the endpoint and the configuration. Secrets and the keystore are not in Git: a Gradle property, `local.properties`, a CI secret.

## 16.2 CI

On a pull request: compile, unit tests, lint, Detekt, formatting. On main: an App Bundle, instrumented tests on Gradle Managed Devices, a Baseline Profile if the team measures startup. At release the bundle goes to a Play track (internal, closed, open, production) with a staged percentage. At the vitals gate, crashes and ANRs halt the rollout.

The mapping file uploads with that same release.

## 16.3 Versions and switches

`versionCode` comes from CI and never decreases. A staged rollout (5%, 20%, 50%, 100%) only protects you if you can stop it at the gates.

Remote config and feature flags are for a dark launch and a fast rollback. They are not for bypassing a policy.

---

# 17. An existing codebase

Replace it incrementally. The app stays shippable after every step.

| What you have | Replace it with | How |
|---|---|---|
| Java | Kotlin | File by file, then fix nullability and data classes. Interop works. |
| `AsyncTask`, raw threads | Coroutines | `viewModelScope`, blocking libraries behind `withContext(IO)`. |
| `SharedPreferences` | DataStore | `SharedPreferencesMigration`, once. |
| `SQLiteOpenHelper` | Room 3 | The schema becomes entities. KSP, suspend DAOs, and the driver come before the package change. Room opens the existing file when the version and the migrations match. |
| `HttpURLConnection`, hand-written JSON | Retrofit or Ktor | A repository boundary, one screen at a time. |
| XML Views | Compose | `ComposeView` inside a View, `AndroidView` inside Compose. One screen at a time. |
| `Loader` | Flow + ViewModel | The repository Flow is the source. |
| Hand-written singletons | Hilt | From the Application inward, module by module. |
| Support Library, Jetifier | AndroidX | A full `androidx.*` migration. Jetifier off. |

The repository interface is the boundary between old and new. One feature is replaced behind it. What you touch gets a test in the same change.

---

# Appendix

## Snapshot, September 2026

| Area | Choice |
|---|---|
| Language | Kotlin 2.4.20 |
| Build | AGP 9.4.x, Gradle 9.6, JDK 17, Android Studio Quail 2026.1.x |
| Target | compileSdk 36, targetSdk 36, minSdk from your own reach |
| UI | Jetpack Compose, Material 3, Compose BOM 2026.08.00 |
| Compiler plugins | Compose compiler and serialization on the Kotlin version; KSP 2.3.11 |
| Async | Coroutines 1.11.x, Flow, StateFlow |
| DI | Hilt 2.59.x, or Koin in KMP |
| Database | Room 3.0.x, KSP, SQLite driver |
| Key-value | Preferences DataStore, or a custom Serializer |
| Network | Retrofit or Ktor, OkHttp, kotlinx.serialization |
| Images | Coil 3 |
| Background | WorkManager; a typed foreground service; FCM |
| Navigation | Navigation 3, or type-safe Navigation Compose 2.10.x |
| Location | Fused Location Provider |
| Camera, media | System camera contract or CameraX 1.6.x; Media3 |
| Tests | JUnit, coroutines-test, Turbine, Compose UI test, MigrationTestHelper |
| Leaks | LeakCanary, debug only |
| Performance | Macrobenchmark, Baseline Profile, R8, StrictMode |
| Quality | Android Lint, Detekt, ktlint or Spotless, Compose rules |

## Principles

1. The lifecycle belongs to the system. Leaks, lost state, and ANRs are what you get when you ignore that.
2. State flows down, events flow up. One source, a model that cannot contradict itself.
3. The framework stays at the edge. Domain code is testable on the JVM.
4. The main thread does not block. An Activity context does not go into a long-lived holder.
5. Measure first, on release, and optimize after that.
6. The permission is the minimum. What comes from outside the app is untrusted.
7. Lint, Detekt, LeakCanary, and CI enforce the above. Review looks at the design.
