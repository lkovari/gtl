# White screen ("white crash") — root cause analysis

- [English](#english)
- [Magyar](#magyar)

---

## English

Analysis date: 2026-10-04. App version on the phone: 2.0.19 (code identical to `73c5bcd`; `HEAD` only changed docs and version). No code was changed during the analysis.

### 1. Symptom

- While recording a track, the user went to *Menu → Download Offline map*, switched the map from *OSM Hungary* to *Turistautak.hu*, and went back.
- The **whole screen** turned blank in the app background color (light, almost white). Only the system status bar was visible, with white icons.
- Nothing was on screen: no top bar, no Stop button, no bottom tabs, no map.
- Nothing brought it back: taps, waiting, zooming. The app had to be left with system Back or killed.
- There was **no crash**. Logcat had no `FATAL EXCEPTION`, the crash buffer was empty, and the app's own error log (`files/diagnostics`) did not exist, so no uncaught exception was ever recorded.

Earlier fixes aimed at map rendering (themes, tile cache, MapView lifecycle) did not help, because the map is not the cause.

### 2. Root cause

Every secondary screen's back arrow calls `NavController.popBackStack()` **unconditionally**:

```kotlin
// app/src/main/java/com/lkovari/mobile/apps/gtl/ui/GtlApp.kt
composable("settings") { SettingsScreen(state, viewModel) { nav.popBackStack() } }
composable("osm")      { OsmDownloadScreen(viewModel) { nav.popBackStack() } }
// same for tracks, help, location, about, diagnostics, and onShowOnMap
```

What happens on a fast double tap of the back arrow:

1. **First tap:** `popBackStack()` removes `osm` and Navigation Compose starts the exit transition. During the transition, the leaving screen is **still drawn and still receives touches**.
2. **Second tap**, on the same arrow of the leaving screen: `popBackStack()` runs again. The only entry left is the start destination `main`, so **`main` is popped too**.
3. The back stack is now **empty**. `NavHost` has nothing to compose, so only the `GtlTheme` background is drawn, which is the blank screen.
4. The Activity is still `RESUMED` and in the foreground, and the process is alive. Nothing throws, so nothing is logged. With nothing on screen, no tap can recover it. System Back finishes the Activity, because `NavHost` no longer intercepts Back.

**Why it looked like a map switch bug:** the map is switched on the *Offline map* screen and the user returns with its back arrow. During recording, and with the map being rebuilt for the new file, the main screen draws slower on the phone. So the first tap can look like it was ignored, and the user taps again. This part is a likely explanation and was not measured. The trigger itself is proven: the second tap on the back arrow, which happens **without any map switch** too (see the *Settings* test below).

### 3. Proof

Environment: Android emulator (`Pixel_10`), debug build of the current code, with the phone's exact DataStore settings and both map files copied from the phone (`tuhu.map`, `eu-hungary.map`). The emulator GPS was set to Dobogókő.

| # | Steps | Result |
|---|---|---|
| 1 | Offline map → back arrow **once** (control) | Normal main screen (45 text nodes in the UI dump) |
| 2 | Offline map → back arrow **twice** within ~150 ms, run 3 times from a cold start | **Blank screen every time**, 0 text nodes, `topResumedActivity = gtl/.MainActivity` |
| 3 | **Settings** → back arrow twice (**no map switch at all**) | **Blank screen**, 0 text nodes |
| 4 | Back arrow, then system BACK | Not blank; the app goes to background (correct) |
| 5 | System BACK twice | Not blank; the app goes to background (correct) |

The emulator screenshot of the blank state matches the phone screenshot: the same background color and white status bar icons.

Reproduce (emulator or phone, 1080 px wide screen, coordinates of the top-left back arrow):

```bash
# open Menu → Download Offline map in the app, then:
adb shell "input tap 74 226; sleep 0.15; input tap 74 226"
adb shell dumpsys activity activities | grep topResumedActivity   # still MainActivity
```

### 4. Hypotheses ruled out, with evidence

| Hypothesis | How it was checked | Result |
|---|---|---|
| App crash | Phone logcat, crash buffer, the app's error log | No crash, no recorded error |
| Corrupt map file | Pulled `tuhu.map` and `eu-hungary.map` from the phone | Both valid, readable |
| `tuhu.xml` theme does not match the map's tags | Rendered 7 tiles (Budapest, Pilis, Mátra) on the JVM with mapsforge 0.25.0 (the app's version) and the app's category logic | Renders correctly: forest, contours, trail blazes, thousands of colors per tile |
| Night Turistautak theme (`AUTOMATIC` mode in the evening) | Same render, with the app's own `NightRenderTheme.recolor` | Renders correctly |
| The map switch itself | Emulator: OSM Hungary → Turistautak, with and without recording | Map appears correctly |

### 5. Fix

Make the back action run only while the screen is the **current, resumed** destination. AndroidX Lifecycle (already in the project, `lifecycle-runtime-compose` 2.9.2) provides `dropUnlessResumed` for exactly this case:

```kotlin
import androidx.lifecycle.compose.dropUnlessResumed

composable("settings") {
    SettingsScreen(state, viewModel, onBack = dropUnlessResumed { nav.popBackStack() })
}
composable("osm") {
    OsmDownloadScreen(viewModel, onBack = dropUnlessResumed { nav.popBackStack() })
}
// apply the same to tracks (onBack and onShowOnMap), help, location, about, diagnostics
```

Inside `composable { }`, `LocalLifecycleOwner` is that destination's `NavBackStackEntry`, so `dropUnlessResumed` checks the state of the screen that owns the arrow.

An equivalent guard without the helper:

```kotlin
fun NavController.popIfCurrent(entry: NavBackStackEntry) {
    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) popBackStack()
}
// composable("osm") { entry -> OsmDownloadScreen(viewModel) { nav.popIfCurrent(entry) } }
```

Optional defense in depth: never pop the start destination, for example `if (nav.previousBackStackEntry != null) nav.popBackStack()`.

### 6. Why the fix works

- When `popBackStack()` runs, Navigation immediately moves the popped `NavBackStackEntry` **out of `RESUMED`**: it is lowered while the exit transition runs and is destroyed after it. The entry being revealed (`main`) becomes `RESUMED` only when the transition settles.
- On the second tap, the leaving screen's entry is no longer `RESUMED`, so `dropUnlessResumed` **drops the click** and `popBackStack()` is not called a second time.
- `main` therefore can never be popped by a back arrow, so the back stack can never become empty, so `NavHost` always has a screen to draw.
- The fix does not depend on timing, device speed, recording, or the map. It removes the second `popBackStack()` itself, the event that causes the blank screen. A single tap still works exactly as before, because at that moment the screen is `RESUMED`.
- System Back is not affected: it goes through `NavHost`'s own back handler, which already behaves correctly (tests 4 and 5).

### 7. Verifying the fix

1. Repeat test 2 (double tap within ~150 ms) on every secondary screen. Expected: the main screen appears, with more than 0 text nodes.
2. Repeat test 1 (single tap). Expected: unchanged behavior.
3. Repeat the original scenario on the phone: recording on, switch OSM Hungary → Turistautak, back arrow tapped several times quickly.
4. Recommended: an instrumented Compose UI test that calls the back callback twice, then asserts that a main screen node is displayed and `nav.currentDestination?.route == "main"`.

### 8. Fix (implemented)

Status: implemented on 2026-10-04 and listed under `[Unreleased]` in [CHANGELOGS.md](CHANGELOGS.md).

#### What changed

| File | Change |
|---|---|
| [app/src/main/java/com/lkovari/mobile/apps/gtl/ui/NavigationGuards.kt](app/src/main/java/com/lkovari/mobile/apps/gtl/ui/NavigationGuards.kt) | New. `rememberGuardedPop(nav)` is `dropUnlessResumed { nav.popBackStack() }`. `dropUnlessResumedWith { value -> … }` is the one-argument version, for callbacks that take a value. |
| [app/src/main/java/com/lkovari/mobile/apps/gtl/ui/GtlApp.kt](app/src/main/java/com/lkovari/mobile/apps/gtl/ui/GtlApp.kt) | Every secondary screen's back callback uses `rememberGuardedPop(nav)`: settings, osm, tracks, help, location, about, diagnostics. *Show on map* in Saved tracks uses `dropUnlessResumedWith`, so it cannot pop twice either. The main screen's menu entries use `dropUnlessResumed { nav.navigate(…) }`, so a double tap cannot push the same screen twice. |
| [app/src/test/java/com/lkovari/mobile/apps/gtl/ui/NavigationGuardsTest.kt](app/src/test/java/com/lkovari/mobile/apps/gtl/ui/NavigationGuardsTest.kt) | New regression test (Robolectric + Compose UI test). |
| [app/build.gradle.kts](app/build.gradle.kts) | `testImplementation` of the Compose BOM and `ui-test-junit4`, so Compose UI tests run as JVM unit tests. |

The screens did not change: they still receive a plain `onBack: () -> Unit`. The guard sits where navigation happens, in `GtlApp`, inside each destination's `composable { }`. There `LocalLifecycleOwner` is the destination's own `NavBackStackEntry`.

Why `dropUnlessResumed`: it is the AndroidX Lifecycle API for exactly this case, it is already a dependency (`lifecycle-runtime-compose` 2.9.2), and it does not depend on timing. A debounce (“ignore taps for 500 ms”) would only shrink the window. The window is the exit transition, and how long it lasts depends on the device and its load.

#### Tests

`NavigationGuardsTest` builds a real `NavHost` with `main` and a second destination, opens the second, then taps its back button twice. The Compose clock is paused, so the second tap lands for certain during the exit transition (150 ms after the first).

| Test | Expected | Result |
|---|---|---|
| `unguardedDoubleBack_emptiesBackStack_reproducesBug` | Plain `popBackStack()`: the back stack is empty and `main` is not on screen (the bug) | Passed: the test reproduces the bug |
| `guardedDoubleBack_keepsStartDestination` | `rememberGuardedPop`: the current destination is `main`, and `main` is on screen | Passed |
| `guardedWithArgumentDoubleBack_keepsStartDestination` | Same with `dropUnlessResumedWith` | Passed |
| `guardedSingleBack_stillNavigatesBack` | A single guarded tap still goes back | Passed |

The first test proves the test setup really produces the bug; otherwise the guarded tests could pass for the wrong reason. Full suite: `./gradlew test` passed with 410 tests, 0 failures, 0 errors.

#### Verification on the emulator

The fixed debug build was installed on the same emulator, with the same settings and map files as in section 3. Each run started from a cold start.

| Steps | Before the fix | After the fix |
|---|---|---|
| Offline map → back arrow twice, 3 runs | Blank (0 text nodes) | Main screen (45 text nodes) every time |
| Settings → back arrow twice | Blank | Main screen |
| Offline map → back arrow three times within ~100 ms | Not tested | Main screen |
| Offline map → back arrow once | Main screen | Main screen (unchanged) |

Still to do: repeat the original scenario on the phone with a release build (recording on, OSM Hungary → Turistautak, back arrow tapped several times quickly).

---

## Magyar

Elemzés dátuma: 2026-10-04. Az app verziója a telefonon: 2.0.19 (a kód megegyezik a `73c5bcd`-vel; a `HEAD` csak dokumentációt és verziót változtatott). Az elemzés alatt kód nem változott.

### 1. Tünet

- Rögzítés közben a felhasználó a *Menü → Offline térkép letöltése* képernyőn átváltott *OSM Hungary*-ról *Turistautak.hu*-ra, majd visszalépett.
- A **teljes képernyő** üres lett, az app háttérszínével (világos, majdnem fehér). Csak a rendszer status bar látszott, fehér ikonokkal.
- Semmi nem volt a képernyőn: se felső sáv, se Stop gomb, se alsó fülek, se térkép.
- Semmi nem hozta vissza: se koppintás, se várakozás, se nagyítás. Az appot rendszer-visszával kellett elhagyni, vagy kilőni.
- **Crash nem volt.** A logcatben nincs `FATAL EXCEPTION`, a crash buffer üres, és az app saját hibanaplója (`files/diagnostics`) nem is létezett, tehát kezeletlen kivétel sosem keletkezett.

A térképrenderelésre irányuló korábbi javítások (téma, csempe-cache, MapView életciklus) nem segítettek, mert nem a térkép az ok.

### 2. Root cause

Minden másodlagos képernyő vissza-nyila **feltétel nélkül** hívja a `NavController.popBackStack()`-et:

```kotlin
// app/src/main/java/com/lkovari/mobile/apps/gtl/ui/GtlApp.kt
composable("settings") { SettingsScreen(state, viewModel) { nav.popBackStack() } }
composable("osm")      { OsmDownloadScreen(viewModel) { nav.popBackStack() } }
// ugyanígy: tracks, help, location, about, diagnostics és onShowOnMap
```

Mi történik, ha a vissza-nyilat gyorsan kétszer megnyomják:

1. **Első koppintás:** a `popBackStack()` leveszi az `osm` képernyőt, és a Navigation Compose elindítja a kilépő animációt. Az animáció alatt a távozó képernyő **még látszik, és még fogadja az érintést**.
2. **Második koppintás**, a távozó képernyő ugyanazon nyilára: a `popBackStack()` újra lefut. Már csak a kezdő `main` célpont van a backstacken, így **a `main` is lekerül**.
3. A backstack **üres**. A `NavHost`-nak nincs mit rajzolnia, csak a `GtlTheme` háttere látszik: ez az üres képernyő.
4. Az Activity közben `RESUMED`, előtérben van, a folyamat él. Nem dobódik kivétel, ezért nincs napló. Mivel semmi nincs a képernyőn, koppintással nem lehet kijutni. A rendszer-vissza bezárja az Activity-t, mert a `NavHost` már nem kezeli a Back-et.

**Miért tűnt térképváltási hibának:** a térképet az *Offline térkép* képernyőn váltják, és onnan a vissza-nyíllal jönnek vissza. Rögzítés közben, az új fájlra újraépülő térképpel a főképernyő lassabban rajzolódik ki a telefonon. Így az első koppintás „nem reagálónak” tűnhet, és jön a második. Ez valószínű magyarázat, nem mértük. A kiváltó ok viszont bizonyított: a vissza-nyíl második koppintása, ami **térképváltás nélkül is** előjön (lásd a *Beállítások* próbát).

### 3. Bizonyíték

Környezet: Android emulátor (`Pixel_10`), a jelenlegi kód debug buildje. A telefonról átmásolva: a pontos DataStore beállítások és mindkét térképfájl (`tuhu.map`, `eu-hungary.map`). Az emulátor GPS-e Dobogókőre állítva.

| # | Lépések | Eredmény |
|---|---|---|
| 1 | Offline térkép → vissza-nyíl **egyszer** (kontroll) | Normál főképernyő (45 szöveges elem a UI dumpban) |
| 2 | Offline térkép → vissza-nyíl **kétszer**, ~150 ms-on belül, 3 futás hidegindításból | **Mindháromszor üres képernyő**, 0 szöveges elem, `topResumedActivity = gtl/.MainActivity` |
| 3 | **Beállítások** → vissza-nyíl kétszer (**semmilyen térképváltás nélkül**) | **Üres képernyő**, 0 szöveges elem |
| 4 | Vissza-nyíl, majd rendszer BACK | Nem üres; az app háttérbe megy (helyes) |
| 5 | Rendszer BACK kétszer | Nem üres; az app háttérbe megy (helyes) |

Az emulátoron készült képernyőkép megegyezik a telefonossal: ugyanaz a háttérszín, fehér status bar ikonok.

Reprodukálás (emulátor vagy telefon, 1080 px széles kijelző, a bal felső vissza-nyíl koordinátái):

```bash
# az appban: Menü → Offline térkép letöltése, majd:
adb shell "input tap 74 226; sleep 0.15; input tap 74 226"
adb shell dumpsys activity activities | grep topResumedActivity   # továbbra is MainActivity
```

### 4. Kizárt hipotézisek, bizonyítékkal

| Hipotézis | Ellenőrzés módja | Eredmény |
|---|---|---|
| Az app összeomlott | Telefon logcat, crash buffer, az app hibanaplója | Nincs crash, nincs rögzített hiba |
| Sérült térképfájl | A telefonról lehúzott `tuhu.map` és `eu-hungary.map` | Mindkettő ép, olvasható |
| A `tuhu.xml` téma nem illik a térkép tagjeihez | 7 csempe renderelése (Budapest, Pilis, Mátra) JVM-en, mapsforge 0.25.0-val (az app verziója) és az app kategórialogikájával | Rendesen kirajzolódik: erdő, szintvonalak, jelzések, csempénként több ezer szín |
| Éjszakai Turistautak-téma (`AUTOMATIC` mód este) | Ugyanez a renderelés, az app saját `NightRenderTheme.recolor` kódjával | Rendesen kirajzolódik |
| Maga a térképváltás | Emulátor: OSM Hungary → Turistautak, rögzítéssel és anélkül | A térkép rendben megjelenik |

### 5. Javítás

A visszalépés csak akkor fusson le, ha a képernyő a **jelenlegi, `RESUMED`** célpont. Az AndroidX Lifecycle (már benne van a projektben, `lifecycle-runtime-compose` 2.9.2) pontosan erre adja a `dropUnlessResumed`-ot:

```kotlin
import androidx.lifecycle.compose.dropUnlessResumed

composable("settings") {
    SettingsScreen(state, viewModel, onBack = dropUnlessResumed { nav.popBackStack() })
}
composable("osm") {
    OsmDownloadScreen(viewModel, onBack = dropUnlessResumed { nav.popBackStack() })
}
// ugyanígy: tracks (onBack és onShowOnMap), help, location, about, diagnostics
```

A `composable { }` blokkon belül a `LocalLifecycleOwner` az adott célpont `NavBackStackEntry`-je, így a `dropUnlessResumed` annak a képernyőnek az állapotát nézi, amelyiké a nyíl.

Ugyanez segédfüggvény nélkül:

```kotlin
fun NavController.popIfCurrent(entry: NavBackStackEntry) {
    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) popBackStack()
}
// composable("osm") { entry -> OsmDownloadScreen(viewModel) { nav.popIfCurrent(entry) } }
```

Opcionális második védelmi vonal: a kezdő célpontot soha ne vegyük le, például `if (nav.previousBackStackEntry != null) nav.popBackStack()`.

### 6. Miért fog működni a javítás

- A `popBackStack()` hívásakor a Navigation azonnal **kiveszi a `RESUMED` állapotból** a levett `NavBackStackEntry`-t: a kilépő animáció alatt lejjebb kerül, utána megszűnik. A felbukkanó bejegyzés (`main`) csak az animáció végén lesz `RESUMED`.
- A második koppintáskor a távozó képernyő bejegyzése már nem `RESUMED`, ezért a `dropUnlessResumed` **eldobja a kattintást**, és a `popBackStack()` nem fut le másodszor.
- Így a `main`-t vissza-nyíl sosem veheti le, a backstack sosem ürülhet ki, és a `NavHost`-nak mindig van mit rajzolnia.
- A javítás nem függ időzítéstől, eszközsebességtől, rögzítéstől vagy a térképtől. Magát a második `popBackStack()` hívást szünteti meg, vagyis azt az eseményt, ami az üres képernyőt okozza. Az egyszeri koppintás pontosan úgy működik, mint eddig, mert akkor a képernyő `RESUMED`.
- A rendszer-vissza nem érintett: az a `NavHost` saját back handlerén megy át, ami már most is helyesen viselkedik (4. és 5. próba).

### 7. A javítás ellenőrzése

1. A 2. próba (dupla koppintás ~150 ms-on belül) minden másodlagos képernyőn. Elvárt: megjelenik a főképernyő, a szöveges elemek száma nagyobb 0-nál.
2. Az 1. próba (egyszeri koppintás). Elvárt: változatlan viselkedés.
3. Az eredeti forgatókönyv a telefonon: rögzítés be, váltás OSM Hungary → Turistautak, vissza-nyíl gyorsan többször.
4. Javasolt: műszeres Compose UI teszt, ami kétszer hívja a vissza-callbacket, majd ellenőrzi, hogy egy főképernyős elem látszik, és `nav.currentDestination?.route == "main"`.

### 8. Javítás (megvalósítva)

Állapot: 2026-10-04-én megvalósítva, a [CHANGELOGS.md](CHANGELOGS.md) `[Unreleased]` részében.

#### Mi változott

| Fájl | Változás |
|---|---|
| [app/src/main/java/com/lkovari/mobile/apps/gtl/ui/NavigationGuards.kt](app/src/main/java/com/lkovari/mobile/apps/gtl/ui/NavigationGuards.kt) | Új. A `rememberGuardedPop(nav)` egyenlő a `dropUnlessResumed { nav.popBackStack() }` hívással. A `dropUnlessResumedWith { value -> … }` az egyparaméteres változat, értéket kapó callbackekhez. |
| [app/src/main/java/com/lkovari/mobile/apps/gtl/ui/GtlApp.kt](app/src/main/java/com/lkovari/mobile/apps/gtl/ui/GtlApp.kt) | Minden másodlagos képernyő vissza-callbackje `rememberGuardedPop(nav)`-ot használ: settings, osm, tracks, help, location, about, diagnostics. A Mentett útvonalak *Megjelenítés a térképen* művelete `dropUnlessResumedWith`-et kap, így az sem léphet vissza kétszer. A főképernyő menüpontjai `dropUnlessResumed { nav.navigate(…) }` hívást kapnak, így dupla koppintás nem nyitja meg kétszer ugyanazt a képernyőt. |
| [app/src/test/java/com/lkovari/mobile/apps/gtl/ui/NavigationGuardsTest.kt](app/src/test/java/com/lkovari/mobile/apps/gtl/ui/NavigationGuardsTest.kt) | Új regressziós teszt (Robolectric + Compose UI teszt). |
| [app/build.gradle.kts](app/build.gradle.kts) | `testImplementation`: Compose BOM és `ui-test-junit4`, hogy a Compose UI tesztek JVM unit tesztként fussanak. |

A képernyők nem változtak: továbbra is sima `onBack: () -> Unit`-ot kapnak. A védelem ott van, ahol a navigáció történik: a `GtlApp`-ban, az egyes célpontok `composable { }` blokkjában. Ott a `LocalLifecycleOwner` a célpont saját `NavBackStackEntry`-je.

Miért `dropUnlessResumed`: ez az AndroidX Lifecycle pontosan erre szolgáló API-ja, már függőség (`lifecycle-runtime-compose` 2.9.2), és nem függ időzítéstől. Egy debounce („500 ms-ig nem figyelünk a koppintásra”) csak szűkítené az ablakot. Az ablak a kilépő animáció, aminek a hossza az eszköztől és a terheléstől függ.

#### Tesztek

A `NavigationGuardsTest` valódi `NavHost`-ot épít `main` és egy második célponttal, megnyitja a másodikat, majd kétszer megnyomja a vissza-gombját. A Compose órája meg van állítva, így a második koppintás biztosan a kilépő animáció alatt érkezik (150 ms-mal az első után).

| Teszt | Elvárt | Eredmény |
|---|---|---|
| `unguardedDoubleBack_emptiesBackStack_reproducesBug` | Sima `popBackStack()`: a backstack üres, a `main` nincs a képernyőn (a hiba) | Zöld: a teszt előidézi a hibát |
| `guardedDoubleBack_keepsStartDestination` | `rememberGuardedPop`: az aktuális célpont a `main`, és a `main` látszik | Zöld |
| `guardedWithArgumentDoubleBack_keepsStartDestination` | Ugyanez `dropUnlessResumedWith`-tel | Zöld |
| `guardedSingleBack_stillNavigatesBack` | Az egyszeri, védett koppintás továbbra is visszalép | Zöld |

Az első teszt bizonyítja, hogy a tesztkörnyezet tényleg előállítja a hibát; különben a védett tesztek rossz okból is lehetnének zöldek. Teljes készlet: a `./gradlew test` zöld, 410 teszt, 0 hiba, 0 error.

#### Ellenőrzés emulátoron

A javított debug build ugyanarra az emulátorra került, a 3. fejezet beállításaival és térképfájljaival. Minden futás hidegindításból indult.

| Lépések | Javítás előtt | Javítás után |
|---|---|---|
| Offline térkép → vissza-nyíl kétszer, 3 futás | Üres (0 szöveges elem) | Mindig főképernyő (45 szöveges elem) |
| Beállítások → vissza-nyíl kétszer | Üres | Főképernyő |
| Offline térkép → vissza-nyíl háromszor, ~100 ms-on belül | Nem teszteltük | Főképernyő |
| Offline térkép → vissza-nyíl egyszer | Főképernyő | Főképernyő (változatlan) |

Még hátra van: az eredeti forgatókönyv megismétlése a telefonon, release builddel (rögzítés be, OSM Hungary → Turistautak, vissza-nyíl gyorsan többször).
