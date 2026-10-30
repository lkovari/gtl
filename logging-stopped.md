# Recording stopped with the screen off — root cause analysis

- [English](#english)
- [Magyar](#magyar)

---

## English

Analysis date: 2026-10-10. Phone: Xever 7 Pro, Android 16. App on the phone: 2.0.22 (versionCode 40, installed from Google Play).

### 1. Symptom

- Run/Hike recording, screen off, phone in a pocket. Start 08:48:36, Stop 09:26:19 (local time, CEST).
- The exported `GTL_20261010_084828.gpx` has **no point between 09:00:15 and 09:26:08**: a 26-minute gap.
- The two points around the gap are 492 m apart, and the track joins them with a straight line. The whole recording is one `<trkseg>` with a single Start.
- Nothing on screen said that recording had stopped.

### 2. Evidence from the phone

`adb shell dumpsys activity exit-info com.lkovari.mobile.apps.gtl`:

```
timestamp=2026-10-10 09:00:15.617 pid=15123 reason=13 (OTHER KILLS BY SYSTEM) subreason=12 (KILL PID)
importance=125 rss=375MB description=by PriScreenOffKiller (5009)
```

`importance=125` is a foreground service. The kill time matches the last GPX point to the second.

Logcat:

```
09:00:15.617 ActivityManager: Killing 15123:com.lkovari.mobile.apps.gtl (adj 200): by PriScreenOffKiller (5009)
09:00:15.740 ActivityManager: Scheduling restart of crashed service .service.TrackingForegroundService in 20964ms
09:01:07.362 ActivityManager: Unable to launch app com.lkovari.mobile.apps.gtl for service ...: process is bad
09:25:22     Start proc ... for top-activity          (the user opened the app)
09:26:04     am_foreground_service_start               (the user tapped Start)
09:26:19     am_foreground_service_stop                (the user tapped Stop)
```

- `PriScreenOffKiller` is `com.pri.screenoff.killer`, a system app of this ROM with `FORCE_STOP_PACKAGES`. It is not Android's low-memory killer.
- In the whole log buffer it killed only GTL and DuckDuckGo (both several times). Signal, which was also running, was never killed by it. `dumpsys deviceidle whitelist` lists Signal on the battery optimization exemption list; GTL and DuckDuckGo were not on it.
- All 16 GTL exits Android kept (since 2026-10-08 18:33) came from this killer: 15 kills and 1 force stop. 10 of them hit a running foreground service, i.e. a recording.

### 3. Root cause

Two things together:

1. **Trigger (vendor):** the ROM's screen-off killer stops non-exempt apps even when they run a `location` foreground service. How it works, from its own log (`adb logcat -s PriScreenOffKiller`):
   - `screen_off_three_min`, 3 minutes after the screen goes off: kills processes at `adj` 900 and above (cached apps).
   - `screen_off_fifteen_min`, 15 minutes after the screen goes off: kills down to `adj` 200 and even `adj` 0, so a recording foreground service too. It logs `in use location package: [com.lkovari.mobile.apps.gtl]` and kills it anyway.
   - `morning.kill`, daily at 03:01: force stop (the `FORCE STOP … from pid 5009` exit at 03:01).
   - Before killing, it reads `DozeWhiteList[…]`, which is the battery optimization exemption list, and skips those packages (`skip filter process: …`).
   
   After a kill Android may restart the sticky service: in the test below it did so after 1 s. At 09:01:07 in the field run it refused (`process is bad`), and after a force stop it never restarts. So the recording either breaks for good, or (with the old code) the next Start appended across the gap.
2. **Gaps in the app:**
   - GTL never asked to be exempt from battery optimization (no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, no `isIgnoringBatteryOptimizations` check), so it stayed a target.
   - Recovery relied only on `START_STICKY`. The **Recording stopped** notice and the closing of the session ran only when the service *was* restarted and its foreground start was refused (`abandonInterruptedStart`). When no restart happened at all, nothing ran: the session stayed open with no writer, and the UI just showed "not logging".
   - Start resumed whatever session was still open (`openSession()`), so the next Start appended to the old session. That is the straight 492 m line.

### 4. Fix

| Part | Where | What |
|---|---|---|
| Prevent | `MainTrackerScreen`, `BatteryOptimization`, manifest | While GTL is not exempt, Start shows **Keep recording with the screen off** with **Allow** (system `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` dialog, falling back to the list screen) and **Not now**. Recording starts either way; back or tap outside cancels. |
| Recover | `InterruptedRecordingRecovery`, `GtlViewModel` | On launch, an open session that no service in this process owns is closed at its last saved point (`STOP` marker, `stoppedAt`). The existing **Recording stopped** dialog shows once, with **Battery settings** while not exempt. The next Start begins a new session. |
| Diagnose | `ProcessExitDiagnostics` | The previous exit (`getHistoricalProcessExitReasons`, Android 11+) goes to the in-app error log as `track.interrupted`, e.g. `description=by PriScreenOffKiller (5009)`. |
| Restart | `TrackingForegroundService` | A `START_STICKY` restart (null intent) only resumes an open session; with nothing to resume it stops instead of starting a recording nobody asked for. |
| Concurrency | `RecordingSessionGate` | The service claims the session as soon as it accepts a start; service and recovery share one process-wide `Mutex`, so a system restart and the launch-time recovery never act on the same session. |

Not changed on purpose: the logging pipeline, filters, exports, statistics, and the behaviour when the service *is* restarted (it still resumes the same session).

### 5. Verification

Same phone, debug build of this fix installed over the debug build already on the phone (same signature, no uninstall). Screen off, USB power reported as unplugged (`dumpsys battery unplug`), nobody touching the phone. DuckDuckGo was not a usable control (it was not in the killer's target list during B2), so the killer's own log lines are the evidence.

| # | Setup | Result |
|---|---|---|
| B1 | Not exempt, Start | The new **Keep recording with the screen off** dialog appeared; **Not now** started the recording (foreground service, `types=0x8`). |
| B2 | Not exempt, recording, screen off 10:19:25 | `screen_off_fifteen_min` at 10:34:26, GTL killed at 10:34:46 (`by PriScreenOffKiller`, `importance=125`). Android restarted the service at 10:34:47; the null-intent restart **resumed session 1**: 197 points from 10:19:08 to 10:36:11, no gap over 20 s. |
| B3 | Force stop while recording (what `morning.kill` does), no restart | Session 1 stayed open with no process. Opening the app (behind the PIN lock screen) closed it: `stoppedAt` 10:37:35, a `STOP` at the last point (10:36:53, same coordinates), error log `track.interrupted … reason=10 … importance=125 description=[FORCE STOP]`, notice flag set for the **Recording stopped** dialog. The dialog itself was not seen, because the PIN lock screen was on top. |
| A0 | Not exempt, GTL process alive, screen off 10:38:17 | 3-minute round skipped it (most recent task); **15-minute round killed it** at 10:53:04 (`adj 0`). |
| A1 | Exempt (`deviceidle whitelist +`), screen off 10:53:35 | 15-minute round: `DozeWhiteList[com.lkovari.mobile.apps.gtl]`, `skip filter process: com.lkovari.mobile.apps.gtl`. Process alive (same pid). |
| A0b | Not exempt, screen off 11:09:32 | **Killed** at 11:22:23 (`killProcess [28402, com.lkovari.mobile.apps.gtl] true`). |
| A1b | Exempt, screen off 11:22:32 | 15-minute round 11:37:53: `skip filter process: com.lkovari.mobile.apps.gtl`. Process alive (same pid). |
| B4 | After unlock: the **Recording stopped** dialog from B3 | Shown with **Battery settings**; the user allowed the exemption in the system dialog, and GTL appeared on `dumpsys deviceidle whitelist`. |
| A2 | Exempt, **recording**, Start 14:20:19, screen off 14:20:22 | Start went straight to recording; the user did not notice the dialog, consistent with recording starting right after Start. 15-minute round 14:35:21: `DozeWhiteList[com.lkovari.mobile.apps.gtl]`, `skip filter process: com.lkovari.mobile.apps.gtl`. Same pid, foreground service running, session 2 has 194 points from 14:20:24 to 14:36:46 with no gap over 20 s. |

Rounds A0–A1b ran without a recording because the phone was locked with a PIN while nobody was there; A2 repeated the exemption case with a real recording. Not exercised on the phone: the **Allow** button of the Start dialog itself. It opens the same system dialog as **Battery settings** (B4) through the same code path.

Unit tests: `InterruptedRecordingRecoveryTest` (5) and `ProcessExitDiagnosticsTest` (2). The full `:engine:test` and `:app:testDebugUnitTest` suites pass, `lintDebug` shows 0 errors and no new warnings (85 before, 85 after).

### 6. Limits

- Android offers no API for vendor background or auto-start lists. The dialog text asks the user to allow GTL there too.
- Google Play restricts `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to apps whose core function needs it; continuous track recording is that function, but a Play policy declaration may be requested.
- A recording interrupted by a killer is still lost for the time the process was dead. The fix prevents the kill where the exemption is honoured, and makes an interruption visible instead of silent.

---

## Magyar

Elemzés dátuma: 2026-10-10. Telefon: Xever 7 Pro, Android 16. App a telefonon: 2.0.22 (versionCode 40, Google Play-ből telepítve).

### 1. Tünet

- Fut/túra felvétel, kikapcsolt képernyő, telefon zsebben. Start 08:48:36, Stop 09:26:19 (helyi idő, CEST).
- Az exportált `GTL_20261010_084828.gpx`-ben **09:00:15 és 09:26:08 között nincs pont**: 26 perces rés.
- A rés két szélén lévő pont 492 m-re van egymástól, a track egyenes vonallal köti össze őket. Az egész felvétel egy `<trkseg>`, egyetlen Starttal.
- A képernyőn semmi nem jelezte, hogy a rögzítés leállt.

### 2. Bizonyíték a telefonról

`adb shell dumpsys activity exit-info com.lkovari.mobile.apps.gtl`:

```
timestamp=2026-10-10 09:00:15.617 pid=15123 reason=13 (OTHER KILLS BY SYSTEM) subreason=12 (KILL PID)
importance=125 rss=375MB description=by PriScreenOffKiller (5009)
```

Az `importance=125` foreground service-t jelent. A kill ideje másodpercre egyezik az utolsó GPX-ponttal.

Logcat:

```
09:00:15.617 ActivityManager: Killing 15123:com.lkovari.mobile.apps.gtl (adj 200): by PriScreenOffKiller (5009)
09:00:15.740 ActivityManager: Scheduling restart of crashed service .service.TrackingForegroundService in 20964ms
09:01:07.362 ActivityManager: Unable to launch app com.lkovari.mobile.apps.gtl for service ...: process is bad
09:25:22     Start proc ... for top-activity          (a felhasználó megnyitotta az appot)
09:26:04     am_foreground_service_start               (a felhasználó Startot nyomott)
09:26:19     am_foreground_service_stop                (a felhasználó Stopot nyomott)
```

- A `PriScreenOffKiller` a `com.pri.screenoff.killer`, ennek a ROM-nak a rendszerappja, `FORCE_STOP_PACKAGES` jogosultsággal. Nem az Android memóriakezelője.
- A teljes log-pufferben csak a GTL-t és a DuckDuckGo-t lőtte le (mindkettőt többször). A szintén futó Signalt soha nem bántotta. A `dumpsys deviceidle whitelist` szerint a Signal az akkumulátor-optimalizálás alóli kivétellistán van, a GTL és a DuckDuckGo nem volt rajta.
- Az Android által megőrzött mind a 16 GTL-kilépés (2026-10-08 18:33 óta) ettől a killertől jött: 15 kill és 1 force stop. Ebből 10 futó foreground service-t, vagyis felvételt talált el.

### 3. Root cause

Két dolog együtt:

1. **Kiváltó ok (gyártói):** a ROM képernyő-kikapcsolási killere a kivétellistán nem szereplő appokat akkor is leállítja, ha `location` foreground service-t futtatnak. A saját naplója szerint (`adb logcat -s PriScreenOffKiller`) így működik:
   - `screen_off_three_min`, 3 perccel a képernyő kikapcsolása után: a 900-as vagy nagyobb `adj` értékű (gyorsítótárazott) folyamatokat állítja le.
   - `screen_off_fifteen_min`, 15 perccel a kikapcsolás után: 200-as, sőt 0-s `adj`-ig mindent leállít, így a felvételt futtató foreground service-t is. Naplózza, hogy `in use location package: [com.lkovari.mobile.apps.gtl]`, és mégis leállítja.
   - `morning.kill`, naponta 03:01-kor: force stop (a 03:01-es `FORCE STOP … from pid 5009` kilépés).
   - Leállítás előtt beolvassa a `DozeWhiteList[…]`-et, ami az akkumulátor-optimalizálás alóli kivétellista, és ezeket a csomagokat kihagyja (`skip filter process: …`).
   
   Leállítás után az Android újraindíthatja a sticky service-t: az alábbi tesztben 1 s után megtette. A terepi futásban 09:01:07-kor elutasította (`process is bad`), force stop után pedig soha nem indítja újra. A felvétel így vagy végleg megszakad, vagy (a régi kóddal) a következő Start a résen át hozzáfűzött.
2. **Hiányok az appban:**
   - A GTL soha nem kérte, hogy kivétel legyen az akkumulátor-optimalizálás alól (nincs `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, nincs `isIgnoringBatteryOptimizations` ellenőrzés), így célpont maradt.
   - A helyreállítás csak a `START_STICKY`-re épült. **A felvétel leállt** értesítés és a session lezárása csak akkor futott, ha a service *újraindult*, és a foreground indítását elutasították (`abandonInterruptedStart`). Ha újraindítás egyáltalán nem történt, semmi nem futott: a session írás nélkül nyitva maradt, a UI pedig csak „nem naplóz” állapotot mutatott.
   - A Start a még nyitott sessiont folytatta (`openSession()`), így a következő Start a régi sessionhöz fűzött hozzá. Ez a 492 m-es egyenes vonal.

### 4. Javítás

| Rész | Hol | Mit |
|---|---|---|
| Megelőzés | `MainTrackerScreen`, `BatteryOptimization`, manifest | Amíg a GTL nem kivétel, a Start megjeleníti a **Rögzítés kikapcsolt képernyővel** ablakot: **Engedélyezés** (rendszerablak: `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, ha nincs, a lista képernyője) és **Most nem**. A felvétel mindkét esetben elindul; vissza gomb vagy kívülre koppintás megszakítja. |
| Helyreállítás | `InterruptedRecordingRecovery`, `GtlViewModel` | Megnyitáskor a nyitott, de ebben a folyamatban egyetlen service által sem birtokolt session az utolsó mentett pontnál lezárul (`STOP` jelölő, `stoppedAt`). A meglévő **A felvétel leállt** ablak egyszer megjelenik, **Akkumulátor-beállítás** gombbal, amíg a GTL nem kivétel. A következő Start új sessiont kezd. |
| Diagnosztika | `ProcessExitDiagnostics` | Az előző kilépés (`getHistoricalProcessExitReasons`, Android 11+) `track.interrupted` néven az app hibanaplójába kerül, pl. `description=by PriScreenOffKiller (5009)`. |
| Újraindítás | `TrackingForegroundService` | A `START_STICKY` újraindítás (null intent) csak nyitott sessiont folytat; ha nincs mit, leáll, és nem indít olyan felvételt, amit senki nem kért. |
| Párhuzamosság | `RecordingSessionGate` | A service a start elfogadásakor azonnal lefoglalja a sessiont; a service és a helyreállítás egy közös, folyamatszintű `Mutex`-et használ, így a rendszer általi újraindítás és az induláskori helyreállítás soha nem nyúl egyszerre ugyanahhoz a sessionhöz. |

Szándékosan nem változott: a naplózási lánc, a szűrők, az exportok, a statisztikák, és az a viselkedés, amikor a service *újraindul* (továbbra is ugyanazt a sessiont folytatja).

### 5. Ellenőrzés

Ugyanaz a telefon, a javítás debug buildje a telefonon már fent lévő debug buildre telepítve (ugyanaz az aláírás, eltávolítás nélkül). Kikapcsolt képernyő, USB-táp kihúzottnak jelentve (`dumpsys battery unplug`), a telefonhoz senki nem nyúlt. A DuckDuckGo itt nem volt használható kontroll (a B2 alatt nem szerepelt a killer céllistáján), ezért a bizonyíték a killer saját naplója.

| # | Felállás | Eredmény |
|---|---|---|
| B1 | Nincs kivétel, Start | Megjelent az új **Rögzítés kikapcsolt képernyővel** ablak; a **Most nem** elindította a felvételt (foreground service, `types=0x8`). |
| B2 | Nincs kivétel, felvétel, képernyő ki 10:19:25 | `screen_off_fifteen_min` 10:34:26-kor, a GTL leállítva 10:34:46-kor (`by PriScreenOffKiller`, `importance=125`). Az Android 10:34:47-kor újraindította a service-t; a null intentes újraindítás **folytatta az 1-es sessiont**: 197 pont 10:19:08 és 10:36:11 között, 20 s-nál hosszabb rés nélkül. |
| B3 | Force stop felvétel közben (amit a `morning.kill` csinál), újraindítás nincs | Az 1-es session folyamat nélkül nyitva maradt. Az app megnyitása (a PIN-es zárképernyő mögött) lezárta: `stoppedAt` 10:37:35, `STOP` az utolsó ponton (10:36:53, ugyanazok a koordináták), hibanapló: `track.interrupted … reason=10 … importance=125 description=[FORCE STOP]`, és beállt a jelzés **A felvétel leállt** ablakhoz. Magát az ablakot nem láttam, mert a PIN-es zárképernyő volt felül. |
| A0 | Nincs kivétel, a GTL folyamat él, képernyő ki 10:38:17 | A 3 perces kör kihagyta (legutóbbi feladat); **a 15 perces kör leállította** 10:53:04-kor (`adj 0`). |
| A1 | Kivétel (`deviceidle whitelist +`), képernyő ki 10:53:35 | 15 perces kör: `DozeWhiteList[com.lkovari.mobile.apps.gtl]`, `skip filter process: com.lkovari.mobile.apps.gtl`. A folyamat él (ugyanaz a pid). |
| A0b | Nincs kivétel, képernyő ki 11:09:32 | **Leállítva** 11:22:23-kor (`killProcess [28402, com.lkovari.mobile.apps.gtl] true`). |
| A1b | Kivétel, képernyő ki 11:22:32 | 15 perces kör 11:37:53: `skip filter process: com.lkovari.mobile.apps.gtl`. A folyamat él (ugyanaz a pid). |
| B4 | Feloldás után: a B3-ból jövő **A felvétel leállt** ablak | Megjelent **Akkumulátor-beállítás** gombbal; a felhasználó a rendszerablakban engedélyezte a kivételt, és a GTL megjelent a `dumpsys deviceidle whitelist` listán. |
| A2 | Kivétel, **felvétel**, Start 14:20:19, képernyő ki 14:20:22 | A Start azonnal felvételt indított; a felhasználó nem vett észre ablakot, ami egyezik azzal, hogy a felvétel rögtön a Start után indult. 15 perces kör 14:35:21: `DozeWhiteList[com.lkovari.mobile.apps.gtl]`, `skip filter process: com.lkovari.mobile.apps.gtl`. Ugyanaz a pid, a foreground service fut, a 2-es sessionben 194 pont 14:20:24 és 14:36:46 között, 20 s-nál hosszabb rés nélkül. |

Az A0–A1b körök felvétel nélkül futottak, mert a telefon PIN-nel zárolt volt, és senki nem volt mellette; az A2 valódi felvétellel ismételte meg a kivételes esetet. A telefonon nem próbáltuk ki: magát a Start-ablak **Engedélyezés** gombját. Ez ugyanazt a rendszerablakot nyitja, mint az **Akkumulátor-beállítás** (B4), ugyanazon a kódúton.

Unit tesztek: `InterruptedRecordingRecoveryTest` (5) és `ProcessExitDiagnosticsTest` (2). A teljes `:engine:test` és `:app:testDebugUnitTest` zöld, a `lintDebug` 0 hibát és új figyelmeztetést nem mutat (előtte 85, utána 85).

### 6. Korlátok

- A gyártói háttér- és autostart-listákhoz az Android nem ad API-t. Az ablak szövege kéri, hogy a felhasználó ott is engedélyezze a GTL-t.
- A Google Play a `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` engedélyt azokra az appokra korlátozza, amelyek alapfunkciójához kell; a folyamatos track-rögzítés ilyen, de a Play kérhet hozzá szabályzati nyilatkozatot.
- Ha a killer mégis leállítja a felvételt, a halott folyamat ideje alatti szakasz elvész. A javítás ott megelőzi a killt, ahol a kivételt figyelembe veszik, és a megszakadást láthatóvá teszi a csendes hiba helyett.
