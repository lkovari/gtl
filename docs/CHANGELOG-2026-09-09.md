# GTL 2.0.4 — 2026-09-09

Play production track **22 (2.0.4)**. What’s-new for Play Console: [play-console/whatsnew.txt](play-console/whatsnew.txt). Full history: [CHANGELOGS.md](../CHANGELOGS.md). How logging writes the Map polyline: [README.md — How logging works](../README.md#how-logging-works).

---

## English

Runner mode records like a sports watch. Small on-road loops stay on the map.

### Added

- Settings **Use GNSS only**: `GPS_PROVIDER` (GPS, Galileo, GLONASS, BeiDou chip). Fused HIGH_ACCURACY is the fallback if that provider is disabled. Runner preset turns it on; vehicles stay fused.
- README: Runner sports-watch logging, and **How logging works** (Start → Room → Map; the red polyline is the stored tracklog).

### Changed

- Runner preset: GNSS only on, Smooth recorded track **off**, Every good, map simplify off. Vehicles keep fused + Kalman + Smart.
- Every-good duplicate floor is **0.5 m** for runner / pedestrian and **1 m** for vehicles.
- If GPS bearing is 0, curve detection can use heading from consecutive positions.
- Pedestrian Kalman (if smoothing is on) adds extra position process noise so a 5 m road loop is not pulled onto the street.
- Settings has no vertical scrollbar. Help Settings / Track logging cover the switch and Runner preset (EN/HU).

### Fixed

- Street-scale runner loops were flattened by fused location plus constant-velocity Kalman even at Low strength.

---

## Magyar

Futó módban sportóra-szerű naplózás. A kis úttest-hurkok megmaradnak a térképen.

### Új

- Beállítások **Csak GNSS**: `GPS_PROVIDER` (GPS, Galileo, GLONASS, BeiDou chip). Ha ez a provider ki van, fused HIGH_ACCURACY a tartalék. A futó előbeállítás bekapcsolja; a járművek fused-ön maradnak.
- README: futó sportóra-naplózás, és **How logging works** (Start → Room → Térkép; a piros vonal a letárolt tracklog).

### Változott

- Futó előbeállítás: GNSS be, simítás **ki**, minden jó fix, térkép-egyszerűsítés ki. Járművek: fused + Kalman + Okos.
- Minden-jó ismétlődésküszöb **0,5 m** futónál / gyalogosnál, **1 m** járműveknél.
- Ha a GPS bearing 0, a kanyardetekció a pozíciókból számolt headinget is használhatja.
- Gyalogos Kalman (ha a simítás be van) extra folyamat-zajt kap, hogy egy 5 m-es úthurok ne simítson az aszfaltra.
- A Beállításoknak nincs függőleges görgetősávja. Súgó Beállítások / Naplózás: kapcsoló és futó előbeállítás (HU/EN).

### Javítva

- Az utcai léptékű futóhurkokat a fused hely + állandó sebességű Kalman még Alacsony erősségnél is ellapította.
