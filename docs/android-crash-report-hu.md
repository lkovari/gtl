# Korrekt globális hibajelentés Androidon

Egy folyamatban futó Android-alkalmazás minden Java- és Kotlin-hibáját, valamint a folyamat leállását egyetlen helyen kell tudni elolvasni. A platform nem ad ehhez egy hookot. Három csatorna fedi le, ami egyáltalán látható, és mind a három ugyanabba a helyi fájlba ír.

Külső könyvtár ehhez nem kell. Natív stack visszafejtéséhez vagy egy felhős gyűjtőhöz lent van két ingyenes lehetőség. Fizetős SDK-ra nincs szükség.

A leírás általános. Nincs hozzákötve egy konkrét alkalmazás osztályaihoz.

## Mit jelent a teljes lefedés

| Csatorna | Mit lát | Mikor ír |
|---|---|---|
| Nem kezelt Java/Kotlin kivétel | Minden szál, amelynek nincs saját kezelője. Ide tartozik a főszál, a saját executor, és a coroutine `launch`, ha a kivétel kijut a szálra | Azonnal, a folyamat halála előtt, szinkronban, `fsync`-kel |
| Szándékosan elkapott hiba | Csak az a `catch`, amely meghívja a közös `report` függvényt | A folyamat életben marad, az írás mehet háttérszálon, ugyanabba a fájlba |
| Előző futás kilépése (API 30+) | Natív összeomlás, ANR, inicializálási hiba, és az a Java-összeomlás is, amelynek a saját írása nem ért lemezre | A következő hideg induláskor, a rendszer `ApplicationExitInfo` rekordjából |

Ami ezeken kívül esik, azt ez a megoldás sem látja. Elkapott és eldobott kivétel, nem várt `async` eredmény, más folyamat, és a kezelő telepítése előtti statikus inicializálás mind ilyen. Ezeket lent, a korlátoknál bontjuk ki.

## 1. Nem kezelt kivétel

A belépési pont a `Thread.setDefaultUncaughtExceptionHandler`. A JVM ezt hívja, ha egy szálon a kivétel kifut a hívási verem tetejére, és annak a szálnak nincs saját `UncaughtExceptionHandler`-e.

Az Android a folyamat indulásakor, az alkalmazáskód előtt, már beállít egy kezelőt. Ez állítja le a folyamatot, és ez jeleníti meg a rendszer összeomlási párbeszédét. A saját kezelő ezt a példányt köteles megjegyezni, és a lemezre írás után meghívni. Enélkül a főszál beragad, a folyamat nem hal meg, a UI pedig hibás állapotban marad.

A kezelő `Throwable`-t kap, tehát az `Error` ág is ide tartozik: `OutOfMemoryError`, `StackOverflowError`, `NoClassDefFoundError`. A szignatúra emiatt `Throwable`, nem `Exception`.

### Telepítés

A kezelőnek az első saját kód előtt a helyén kell lennie. A sorrend a folyamatban:

1. A rendszer kezelője (a folyamat indításakor).
2. `Application.attachBaseContext`.
3. A manifest `ContentProvider`-jei, `android:initOrder` szerint. A nagyobb szám fut előbb.
4. `Application.onCreate`.

A `ContentProvider.onCreate` tehát előbb fut, mint az `Application.onCreate`. Összeomlás egy providerben, vagy két provider között, csak akkor kerül fájlba, ha a saját provider már lefutott. A provider egy dolgot csinál: beállítja a kezelőt. Hálózat, adatbázis és lemezolvasás nincs benne.

```xml
<provider
    android:name=".crash.CrashInitProvider"
    android:authorities="${applicationId}.crash-init"
    android:exported="false"
    android:initOrder="999" />
```

Az `initOrder` legyen nagyobb, mint a merge-elt manifest többi providerjéé. Kiadás előtt a merge-elt manifestben ellenőrizni kell a sorrendet. A kisebb számmal futó provider összeomlását ez a kezelő még nem látja.

Az `Application.onCreate` végén a telepítés újra lefut. Ekkor az a kezelő van becsomagolva, amelyet egy könyvtár az `onCreate` közben tett a helyére. A függvény idempotens: ha a jelenlegi kezelő már a saját osztály, nincs második csomagolás, és nincs kétszeres naplósor.

Más folyamat (`android:process`) nem futtatja a fő folyamat providerjét. Az `Application.onCreate` minden folyamatban lefut, ezért a második telepítés ezeket is lefedi. A provider a fő folyamat korai ablakát fogja meg.

```kotlin
class CrashInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { CrashLog.install(it) }
        return true
    }

    override fun query(u: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?) = null
    override fun getType(u: Uri) = null
    override fun insert(u: Uri, v: ContentValues?) = null
    override fun delete(u: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(u: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
```

```kotlin
fun install(context: Context) {
    store = FileStore(File(context.applicationContext.filesDir, "crash"))
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    if (previous is CrashHandler) return
    Thread.setDefaultUncaughtExceptionHandler(CrashHandler(previous))
}
```

A `filesDir` a folyamat halála után megmarad, és a rendszer cache-ürítése nem viszi el. A `cacheDir` erre a naplóra alkalmatlan.

Direct Boot alatt, feloldás előtt, a hitelesítéshez kötött `filesDir` még nincs meg. Ilyen alkalmazásban a provider a `createDeviceProtectedStorageContext().filesDir` alá ír. Közönséges alkalmazásban marad a sima `filesDir`.

### A kezelő teste

A lemezírás és a továbbhívás két külön lépés. Az írás hibája nem nyeli el az eredeti kivételt, és nem akadályozza a rendszer kezelőjét.

```kotlin
class CrashHandler(
    private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, error: Throwable) {
        try {
            CrashLog.writeFatal(thread, error)
        } catch (_: Throwable) {
        }
        if (previous != null) {
            previous.uncaughtException(thread, error)
        } else {
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }
}
```

A `previous == null` ág eszközön gyakorlatilag nem fut le, mert a rendszer kezelője már a helyén van. Teszten és egy lecsupaszított runtime-on ez az ág állítja le a folyamatot, hogy a szál ne menjen tovább hibás állapotban.

A kezelőből tilos hálózatot hívni, Roomot vagy SQLite-ot nyitni, és coroutine-t indítani. A folyamat a továbbhívás után meghal. A `Dispatchers.IO`-ra tett írás a puffert a lemez előtt elveszíti. A hívás beragadhat azon a záron vagy executoron, amelyik éppen összeomlott. Az SQLite maga is lehet az összeomlás oka.

### Írás a halál előtt

Az írás szinkron, rövid, és a saját zárján kívül mást nem fog. Egy rekord után `FileOutputStream.fd.sync()`, hogy a `killProcess` ne vágja le a page cache-ből a sort.

Ugyanaz a szál, amelyik a napló zárját tartja, nem várhat újra ugyanerre a zárra: az `OutOfMemoryError` a naplóírás közben különben örökre a kezelőben marad, és a rendszer kezelője nem fut le. A mélység számláló ezt vágja el. Más szál rövid ideig várhat a zárra. Ha nem kapja meg, a sor kimarad, a továbbhívás akkor is megtörténik. A kimaradt Java-összeomlást a következő indulás kilépési oka még jelzi, stack nélkül.

```kotlin
private val lock = ReentrantLock()
private val depth = ThreadLocal.withInitial { 0 }

fun writeFatal(thread: Thread, error: Throwable) {
    if (depth.get() > 0) return
    depth.set(1)
    try {
        if (!lock.tryLock(50, TimeUnit.MILLISECONDS)) return
        try {
            appendAndSync(formatFatal(thread, error))
        } finally {
            lock.unlock()
        }
    } finally {
        depth.set(0)
    }
}
```

Egy rekord felső határa legyen kötött, például 64 KiB. A `StackOverflowError` és a hosszú `cause` lánc különben másodszor is kifut a memóriából, miközben a naplót állítja elő. A formázás `Throwable`-t kap el, és hiba esetén egy egysoros jelzést ír: időbélyeg, `kind=fatal`, az eredeti osztály neve. A részletes stack elmaradhat. A tény, hogy a folyamat meghalt, megmarad.

A fájl mérete is kötött, például 256 KiB. Tele fájl esetén a jelenlegi fájl egyetlen előző példányba kerül (`crash.log` → `crash.log.1`), és az új rekord üres fájlba kerül. Így az összeomlás sora nem esik le egy rotáció miatt. Régebbi előzmény nem kell: két fájl elég egy eszközön megnézhető naplóhoz.

### Rekord

Egyszerű szöveg, hogy egy képernyő meg tudja mutatni és a vágólapra tudja tenni.

```text
---
time=2026-10-01T19:13:04.120Z
kind=fatal
thread=main
exception=java.lang.IllegalStateException
message=boom
at com.example.MainActivity.onCreate(MainActivity.kt:40)
at android.app.Activity.performCreate(Activity.java:1)
caused by:
exception=java.lang.IllegalArgumentException
message=missing id
at com.example.Repo.load(Repo.kt:12)
suppressed:
exception=java.io.IOException
message=close failed
at com.example.Repo.close(Repo.kt:30)
```

Az idő UTC, `Instant` szövegként. A `cause` lánc és a `suppressed` tömb is bekerül, mindkettő a méretplafonig. A rekord tartalmazza a szál nevét. Verzió, `versionCode`, folyamatnév és API-szint egy fejlécsorban elfér, egyszer, a fájl elején, nem minden rekordban.

A kivétel üzenetébe nem való token, helyadat, és felhasználói tartalom. A napló azt a szöveget őrzi, amit a `Throwable` már eleve hordoz.

## 2. Elkapott hiba

A `catch` nem jut el a szál kezelőjéig. A JVM nem ad hookot az elkapott kivételre. Bytecode-szövés nélkül a teljes elkapott halmaz egyetlen módon látszik: minden helyreállító `catch` meghívja ugyanazt a `report` függvényt.

```kotlin
private val reportScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun report(op: String, error: Throwable) {
    if (error is CancellationException) throw error
    reportScope.launch {
        writeNonFatal(op, error)
    }
}
```

A `CancellationException` nem hiba. A coroutine lemondása, a kompozíció elhagyása és a `Job` megszakítása mind ezt dobja. Naplózni és lenyelni tilos: a `report` első sora továbbdobja. Ugyanez a szabály a kézi `catch (Exception)` ágakra. Előbb a `CancellationException` megy tovább, utána jön a `report`, utána a helyreállítás.

A nem végzetes írás mehet háttérszálon, mert a folyamat életben marad. Ugyanaz a fájl, ugyanaz a zár, `kind=nonfatal`, plusz egy stabil `op` név, amely megmondja, melyik művelet állt helyre. A név fix sztring a híváshelyen (`map.load`, `db.insert`), nem az osztály `toString()`-je.

A helyreállítás marad a híváshelyen. A `report` nem dönti el, hogy a folyamat folytatódjon-e. Ha a hiba után nincs értelmes folytatás, a `catch` ne nyelje el a kivételt: menjen tovább, és a végzetes csatorna naplózza.

`runCatching` és a csupasz `catch (Exception)` a `CancellationException`-t is megeszi, mert az `Exception` leszármazottja. A `report` továbbdobása ezt egy helyen kijavítja, feltéve hogy minden ilyen ág a `report`-on keresztül megy.

## 3. Coroutines

A `launch` egy `SupervisorJob` alatt, saját `CoroutineExceptionHandler` nélkül, a kivételt a szál nem kezelt kezelőjére viszi. A `viewModelScope` és a `lifecycleScope` ilyen supervisor. A kezeletlen `launch` tehát összeomlasztja a folyamatot, és a végzetes csatorna naplózza. Ez a kívánt viselkedés egy programhibánál.

`CoroutineExceptionHandler` a supervisor gyökerén elnyeli a kivételt: a szál kezelője már nem fut le, a folyamat nem hal meg. Kezelőt csak oda szabad tenni, ahol a folytatás szándékos. A kezelő törzse a `report`. A végzetes út ettől még a szál kezelője marad.

Az `async` a hibát a `Deferred`-ben tartja, és az `await` dobja újra. A szál kezelője és a `CoroutineExceptionHandler` nem látja. Amit senki nem `await`-el, az elveszik. A híváshely vagy `await`-el, vagy `launch`-ot használ, ha a műveletnek nincs eredménye.

A `coroutineScope` és a `withContext` a kivételt a hívónak adja. Onnan vagy továbbmegy a végzetes útra, vagy egy `report`-tal helyreáll.

## 4. Natív összeomlás, ANR, elveszett írás

A szál kezelője Java és Kotlin kivételt lát. A natív összeomlás (`SIGSEGV`, `SIGABRT`, `SIGBUS`) és az ANR nem kivétel. A főszál blokkolása közben a folyamat nem fut le egy Java `catch`-ig. Ezeket a rendszer a következő induláskor, API 30-tól, `ActivityManager.getHistoricalProcessExitReasons` alatt adja vissza.

Ezt a lekérdezést a folyamat elején, háttérszálon, egyszer kell lefuttatni. A saját csomag olvasásához nincs külön engedély. `pid = 0` és egy kis `maxNum` (például 16) elég.

Naplózandó okok:

| Ok | Jelentése |
|---|---|
| `REASON_CRASH` | Java/Kotlin összeomlás. Ha a saját fájlban megvan a stack, ez a sor csak megerősítés. Ha a `fsync` nem ért véget, ez az egyetlen nyoma, stack nélkül |
| `REASON_CRASH_NATIVE` | Natív összeomlás |
| `REASON_ANR` | A főszál nem válaszolt |
| `REASON_INITIALIZATION_FAILURE` | A folyamat az indulás közben halt meg |

A felhasználói kilépés, a saját `exitProcess`, a csomagfrissítés és a jogosultságváltás nem hiba. Nem kerül a naplóba. Az `LOW_MEMORY` és az `EXCESSIVE_RESOURCE_USAGE` folyamatleállás, de nem kivétel. Külön döntés, ha ezek is kellenek. Az alap készlet a fenti négy ok.

A rekord: `kind=exit`, az ok neve, a rendszer időbélyege, a fontosság, a `description`, a `status`. A `getTraceInputStream()` ANR-nél a traces szöveget, natív összeomlásnál tombstone-részletet adhat. A folyamot le kell zárni, a szöveget ugyanazzal a rekordplafonnal kell vágni. Sok eszközön Java-összeomlásnál a folyam `null`: a Java stack csak a saját végzetes rekordban van meg.

Ugyanaz a kilépés minden hideg induláskor újra megjelenne. Egy külön kurzor fájl őrzi az utolsó feljegyzett `timestamp`-et. A naplósor után kerül lemezre, `fsync`-kel. Ha a folyamat a kurzor írása előtt meghal, a következő indulás újra feljegyzi ugyanazt a kilépést. A dupla sor ártalmatlanabb, mint a csendben elveszett ANR.

API 29 és alatta nincs `ApplicationExitInfo`. Ott a natív összeomlás és az ANR eszközön, külső könyvtár nélkül, nem rekonstruálható. A Java-összeomlás a saját kezelőn keresztül megvan.

## 5. Mikor kell könyvtár

A fenti három csatorna a platform API-ja. Függősége nincs.

Könyvtár két feladatnál éri meg, és mindkettő megoldható ingyenes eszközzel.

**Natív stack és ANR-trace az eszközön, fiók nélkül.** Az [xCrash](https://github.com/iqiyi/xCrash) Apache-2.0. Java-összeomlást, natív tombstone-t és ANR-trace-t a saját könyvtárába ír. A saját `UncaughtExceptionHandler`-t lecseréli, ezért a telepítés után a mi kezelőnknek kívül kell maradnia, és tovább kell hívnia az xCrash kezelőjét, különben a tombstone vagy a saját fájl egyike elmarad. Bevezetés előtt a kiadás frissességét ellenőrizni kell. Verziót a leírás nem rögzít.

**Gyűjtés a fejlesztő gépe nélkül, szimbolizált natív stackkel.** A Firebase Crashlytics ingyenes. A Java-összeomlást, az ANR-t és a `recordException` nem végzetes hibát a következő induláskor tölti fel. A natív stack a szintén ingyenes `firebase-crashlytics-ndk` modullal megy fel. A feltöltés az SDK dolga, a saját kezelőből továbbra sem indul hálózat. Ha a Crashlytics és a saját fájl együtt él, a saját kezelő a Crashlytics kezelőjét hívja tovább.

A fizetős hibagyűjtők (Bugsnag, Instabug és társaik) ugyanezt a platformrést töltik ki. A fenti két ingyenes út lefedi.

## 6. Amit a megoldás nem lát

- **Elkapott és nem jelentett kivétel.** Nincs `report`, nincs sor.
- **Nem várt `async`.** A hiba a `Deferred`-ben marad.
- **Saját kezelővel rendelkező szál**, ha az a kezelő nem hívja a `Thread.getDefaultUncaughtExceptionHandler()`-t. Egyes executorok és HTTP-kliensek ilyen szálat indítanak. A saját executor a gyári alap kezelőt hagyja meg, vagy továbbhívja.
- **A provider telepítése előtti kód.** `attachBaseContext`, az `Application` statikus inicializálója, és a kisebb `initOrder`-ű providerek.
- **Másik folyamat**, amíg az `install` abban a folyamatban le nem futott.
- **Natív összeomlás és ANR API 29 alatt**, könyvtár nélkül.
- **A rendszer által elemésztett trace.** Az `getTraceInputStream()` lehet `null`, és a tombstone csonka lehet. A rekord akkor is megmarad, az ok kódjával.
- **Force stop.** A felhasználó által kikényszerített leállás nem kivétel, és gyakran kilépési trace sincs hozzá.

A főszál `Looper.loop()` köré tett `catch`, amely naplóz és újra belép a ciklusba, nem hibajelentés. Az Activity hibás állapotban marad a képernyőn. A végzetes út naplóz, majd a rendszer kezelője leállítja a folyamatot.

A csak `Log.e` sor a logcat gyűrűjébe kerül. A folyamat halála és a felhasználó eszköze után ez a szöveg nincs meg. A fájl a megmaradó másolat. Fejlesztés közben a logcat `AndroidRuntime` sora ettől még hasznos.

## 7. Ellenőrzés

JVM-teszt, eszköz nélkül:

- A formázó kiírja a `cause` láncot és a `suppressed` kivételt.
- A plafon fölötti stack levágódik, a rekord fejléce megmarad.
- Tele fájl után a régi tartalom a `.1` fájlban van, az új rekord a friss fájlban, és az új rekord nincs eldobva.
- Az írás közben dobott kivétel után a továbbhívott kezelő pontosan egyszer fut.
- A második `install` nem csomagolja kétszer a saját kezelőt.
- A `report(CancellationException)` továbbdob, és nem ír sort.
- Ugyanazon a szálon, a zár tartása közben induló második `writeFatal` visszatér, és nem deadlockol.

Eszközön, debug buildben, egy csak debugban elérhető gombbal:

- Kivétel a főszálon: a fájlban `kind=fatal`, utána a folyamat meghal, a rendszer párbeszéd megjelenik.
- Kivétel egy háttérszálon: ugyanaz.
- Kezeletlen `launch` a `viewModelScope`-ban: ugyanaz.
- Helyreállító `catch` a `report`-tal: `kind=nonfatal`, a folyamat életben marad.
- API 30+ eszközön egy szándékos natív abort vagy egy mesterséges ANR után a következő hideg indulás `kind=exit` sort ír.

A tesztcsomag és az instrumentált teszt ne hívja a `report`-ot. A szándékos `catch` ott a teszthez tartozik.
