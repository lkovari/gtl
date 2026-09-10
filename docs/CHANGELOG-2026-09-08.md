# GTL 2.0.3 — 2026-09-08

Play production track **21 (2.0.3)**. What’s-new for Play Console: [play-console/whatsnew.txt](play-console/whatsnew.txt). Full history: [CHANGELOGS.md](../CHANGELOGS.md). How logging writes the Map polyline (including later GNSS-only work): [README-en.md — How logging works](../README-en.md#how-logging-works) / [README-hu.md](../README-hu.md#hogyan-működik-a-naplózás).

---

## English

Kalman smoothing is now in the recording pipeline. Settings sliders replace the old chips. Usage presets (including aircraft → ICAO units) write the sliders in one step.

### Added

- Constant-velocity **Kalman** filter (`KalmanTrackFilter` in `:engine`) runs **after** accuracy / satellite gates and **before** a point is stored. Route totals, Map (if simplify is off), and KMZ share that path. The HUD accuracy circle stays on the **raw** fused GPS fix.
- Settings: **Smooth recorded track**, continuous **Low–High** smoothing strength, **Hold still when stopped**, continuous **Smart–Every good** recording density.
- Usage presets set those controls plus map-simplify in one DataStore write:
  - Runner: Low strength, every good, simplify off (2 m if you turn it on)
  - Motorbike (app default): Medium, Smart, simplify 6 m
  - Car / watercraft: Medium, Smart, simplify 8 m
  - Aircraft: High, Smart, simplify 15 m, **ICAO** units (knots, nautical miles, feet)
- Help → Settings describes the sliders, presets, and ICAO default (English and Hungarian).
- Engine tests: highway RMSE, roundabout, figure-8, zigzag, stationary lock, jump re-init, usage table, slider interpolation.

### Changed

- **Simplify track on map** is a 1–20 m slider (1 m steps), not chips. It still thins **only the drawn Map line**. SQLite, odometer, and KMZ keep every stored point.
- Recording density between Smart and Every good interpolates spacing (and can use the min-time / curve gate).
- Standing min-distance preset is **2 m** (same as the Smart standing band). **1 m** remains the Every-good duplicate drop.
- Settings fits the safe drawing area without a vertical scrollbar. The read-only Fix filters row is gone; accuracy (30 m, runner 45 m) and 4-satellite gates still run in the background.
- Old **19.5 m** simplify sentinel migrates once to the usage table when Kalman keys are first written.

### Fixed

- Standing GPS wander is pinned when Hold still is on (no 10 m scribble at a stop).
- Poor-accuracy / low-satellite fixes never enter the Kalman filter.

### Effect on the stored tracklog

| Control | Stored in SQLite / KMZ / Route? | What you see |
|---|---|---|
| Smooth recorded track | Yes | On: Kalman moves lat/lon before save. Off: raw fused GPS. |
| Smoothing strength | Yes (if smoothing on) | Low follows GPS (figure-8). High draws smoother arcs, more lag on hairpins. |
| Hold still when stopped | Yes (if smoothing on) | Below pause speed the stored point does not wander. |
| Recording density | Yes | Smart writes fewer points at speed. Every good writes about once per min time (or sooner in a curve), never closer than 1 m. |
| Simplify track on map | **No** | Fewer vertices on Map only. |
| Show accuracy marker | **No** | Purple circle on the raw GPS fix. |
| Units | Display / KMZ labels | Aircraft preset is ICAO; others metric. Points are still stored in WGS-84 metres internally. |

---

## Magyar

A Kalman-simítás a rögzítési lánc része. A chippeket csúszkák váltják. A usage előbeállítás (repülőnél ICAO) egy lépésben állítja a csúszkákat. A térképvonal = a letárolt log (későbbi GNSS-only viselkedéssel): [README-en.md — How logging works](../README-en.md#how-logging-works) / [README-hu.md](../README-hu.md#hogyan-működik-a-naplózás).

### Új

- Állandó sebességű **Kalman**-szűrő (`KalmanTrackFilter` a `:engine` modulban) a pontosság / műhold kapu **után** és a letárolás **előtt** fut. Az Útvonal összesítők, a Térkép (ha az egyszerűsítés ki van) és a KMZ ugyanazt az útvonalat látja. A HUD pontossági köre a **nyers** fused GPS-fixen marad.
- Beállítások: **Rögzített útvonal simítása**, folyamatos **Alacsony–Magas** erősség, **Álláskor ne vándoroljon a pont**, folyamatos **Okos–Minden jó** rögzítési sűrűség.
- A usage előbeállítás ezeket és a térkép-egyszerűsítést egy DataStore írásban állítja:
  - Futó: alacsony erősség, minden jó, egyszerűsítés ki (2 m, ha bekapcsolod)
  - Motor (alkalmazás-alap): közepes, okos, egyszerűsítés 6 m
  - Autó / hajó: közepes, okos, egyszerűsítés 8 m
  - Repülő: magas, okos, egyszerűsítés 15 m, **ICAO** mértékegység (csomó, tengeri mérföld, láb)
- Súgó → Beállítások: csúszkák, előbeállítások, ICAO alap (angol és magyar).
- Engine tesztek: autópálya RMSE, körforgalom, nyolcas, zigzag, álló lock, ugrás újraindítás, usage tábla, csúszka-interpoláció.

### Változott

- **Útvonal egyszerűsítése a térképen**: 1–20 m csúszka (1 m-es lépés), nem chippek. Továbbra is **csak a kirajzolt Térkép-vonalat** ritkítja. Az SQLite, az odométer és a KMZ minden letárolt pontot megtart.
- A rögzítés sűrűsége az Okos és a Minden jó között keveri a távolságot (és használhatja a min-idő / kanyar kaput).
- Az álló min-távolság előbeállítás **2 m** (az Okos álló sáv). Az **1 m** a Minden jó ismétlődő-pont küszöbe marad.
- A Beállítások elfér a safe drawing területen, függőleges görgetősáv nélkül. A csak olvasható Fixszűrők sor kikerült; a pontosság (30 m, futónál 45 m) és a 4 műhold kapu a háttérben továbbra is él.
- A régi **19,5 m** egyszerűsítési őrszem egyszer migrál a usage táblára, amikor a Kalman kulcsok először íródnak.

### Javítva

- Álláskor a GPS kóborlása rögzül, ha a lock be van kapcsolva (nincs 10 m-es firkálás megálláskor).
- Gyenge pontosságú / kevés műholdas fix nem megy be a Kalman-szűrőbe.

### Hatás a letárolt tracklogra

| Vezérlő | SQLite / KMZ / Útvonal? | Mit látsz |
|---|---|---|
| Rögzített útvonal simítása | Igen | Be: Kalman mozgatja a szélességet/hosszúságot mentés előtt. Ki: nyers fused GPS. |
| Simítás erőssége | Igen (ha a simítás be van) | Alacsony követi a GPS-t (nyolcas). Magas simább ívek, több késés hajtűkanyarban. |
| Álláskor ne vándoroljon | Igen (ha a simítás be van) | Pauza-küszöb alatt a letárolt pont nem kóborol. |
| Rögzítés sűrűsége | Igen | Okos: kevesebb pont nagy sebességnél. Minden jó: kb. a min. időközönként (kanyarban hamarabb), 1 m-nél közelebb soha. |
| Útvonal egyszerűsítése a térképen | **Nem** | Kevesebb csúcs csak a Térképen. |
| Pontossági jelzés | **Nem** | Lila kör a nyers GPS-fixen. |
| Mértékegység | Megjelenítés / KMZ feliratok | Repülő előbeállítás: ICAO; más usage: metrikus. A pontok belül WGS-84 méterben tárolódnak. |
