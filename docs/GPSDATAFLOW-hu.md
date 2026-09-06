# GPS eseményfigyelés és adatút

Hogyan lesz a fused location frissítésből SQLite sor, majd térkép, Route HUD és KMZ.

```mermaid
flowchart TD
    User["Felhasználó: Start"] --> VM["GtlViewModel.startLogging"]
    VM --> FGS["TrackingForegroundService<br/>foreground type location"]
    FGS --> Sess["TrackRepository<br/>nyitott session vagy új"]
    Sess --> DBSess[("gtl.db<br/>track_sessions")]

    FGS --> Loc["LocationClient<br/>FusedLocationProvider<br/>HIGH_ACCURACY min. 500 ms"]
    FGS --> Gnss["GnssStatusSource"]
    FGS --> Temp["AmbientTemperatureSource"]
    FGS --> Acc["AccelerometerSource"]
    FGS --> Comp["CompassSource"]

    Loc --> LiveLoc["LiveTrackingState.lastLocation"]
    Gnss --> LiveGnss["LiveTrackingState.gnss"]
    Temp --> LiveTemp["temperatureCelsius"]
    Acc --> LiveAcc["accel"]
    Comp --> LiveAz["azimuthDegrees csak HUD"]

    LiveLoc --> Fix["TrackFix<br/>lat lon mag. seb. irany pontossag muhold"]
    LiveGnss --> Fix
    Fix --> Gate{"FixAcceptance.shouldAccept"}

    Gate -->|"tul rossz pontossag<br/>vagy keves muhold"| Drop["Fix eldobva"]
    Gate -->|"a session elso pontja"| KindStart["eventKind START"]
    Gate -->|"tavolsag >= SpeedAdaptiveSpacing"| KindMove{"sebesseg vs. pauza kuszob"}
    Gate -->|"tul kozel, nem kanyar"| Drop

    KindMove -->|"pauza kuszob alatt"| KindPause["PAUSE"]
    KindMove -->|"mozog"| KindGo["MOVE"]

    KindStart --> Row["GpsEventEntity<br/>+ elerheto homerseklet es gyorsulas"]
    KindPause --> Row
    KindGo --> Row

    UserStop["Felhasználó: Stop"] --> StopRow["GpsEventEntity STOP<br/>isPlacemark true"]
    StopRow --> Close["stopSession<br/>stoppedAt beallitasa"]

    Row --> DBevt[("gtl.db<br/>gps_events")]
    Close --> DBevt
    Close --> DBSess

    DBevt --> Observe["observeEvents Flow"]
    Observe --> VM2["GtlViewModel"]
    VM2 --> Stats["TrackStatsCalculator"]
    VM2 --> Display{"Térképvonal egyszerűsítés<br/>és több mint 4 pont?"}
    Display -->|"igen"| Simpl["Douglas-Peucker<br/>Settings küszöb 1-40 m<br/>csak térkép"]
    Display -->|"nem"| RawPts["Room pontok változatlanul"]
    Simpl --> Vis{"MapTrackVisibility"}
    RawPts --> Vis
    Vis --> Map["Map tab vonal<br/>Google Maps vagy OSM"]
    Stats --> Route["Route tab"]
    Observe --> GPSUI["GPS tab elo mezok"]
    DBevt --> KMZ["KmlExportUseCase / KMZ megosztas"]
```

## Figyelők (a foreground service-ben)

| Forrás | Mit táplál |
|---|---|
| `LocationClient` | Play Services fused location, `PRIORITY_HIGH_ACCURACY`. Intervallum a beállításból, legalább 500 ms. A kérés `minDistance` értéke `0`; a sűrűséget később szűrjük. |
| `GnssStatusSource` | Műholdszám és SNR a GPS fülre; `satellitesInFix` a letárolt soron. |
| `AmbientTemperatureSource` | Opcionális; a sorra másolódik, ha van szenzor. |
| `AccelerometerSource` | Opcionális; utolsó XYZ a soron. |
| `CompassSource` | Csak HUD / Compass fül; **nem** kerül SQLite-ba. |

Mindez **látható** location foreground értesítéssel fut. Nincs `ACCESS_BACKGROUND_LOCATION`.

## Kapu: `FixAcceptance`

Egy fix csak akkor tárolódik, ha:

1. `accuracy` ≤ a beállítás szerinti minimális pontosság (m).
2. `satellitesInFix` ≥ a beállítás szerinti minimum (alapból 4).
3. Ez a session első pontja, **vagy** a haversine-távolság az utolsó **eltárolt** ponttól legalább a `SpeedAdaptiveSpacing` (sebesség-sávok; fél távolság, ha az irányszög változása &gt; 15°).

Az elutasított frissítések is frissítik a `lastLocation`-t a GPS/Map HUD-hoz.

## `eventKind`

| Kind | Mikor |
|---|---|
| `START` | Első elfogadott fix (vagy folytatás, ha még nincs pont). |
| `MOVE` | Elfogadott, és a sebesség ≥ a usage pauza-küszöbe. |
| `PAUSE` | Elfogadott, és a sebesség a küszöb alatt (alap 0,4 m/s; futónál 0,25 m/s). |
| `STOP` | A user Stop; az utolsó helyzet akkor is beíródik, ha a kapu eldobná. |

`isPlacemark` igaz START / PAUSE / STOP-nál (KMZ play / pause / stop ikonok).

## SQLite után

A `GtlViewModel` a `gps_events`-et figyeli: élő session, Saved tracks választás, vagy az utolsó session, ha a **Show last logged route on map** be van kapcsolva.

- **Route** összesítők: `TrackStatsCalculator`, logolás közben (nyers Room minták).
- **Map** polyline Room-ból; opcionális Douglas–Peucker (lent); csak logoláskor, last-track-nél vagy kijelölt sessionnél (`MapTrackVisibility`).
- **Megosztás** ugyanebből a KMZ-t építi (`gx:Track` + balloonok). A KMZ soha nem egyszerűsített.

Nincs feltöltés. A Stop utáni `RemoteTrackSync` no-op.

## Térképvonal: Douglas–Peucker

**Cél.** Kevesebb csúcspont a Map fülön, hogy egy hosszú track olcsón rajzolható maradjon. Az SQLite, a Route összesítők és a KMZ minden eltárolt pontot megtart.

**Mikor.** Beállítás: **Simplify track on map** / **Útvonal egyszerűsítése a térképen** (alapból bekapcsolva), és több mint 4 pont. A küszöb a Settings csúszka (**1,0–40,0 m**, **0,5 m** lépés, alap **19,5 m**, megjegyzett). A kapcsoló ki a csúszkát elrejti, a tárolt értéket megtartja. `GtlViewModel` → `DouglasPeucker.clampTolerance` → `simplify`.

**Hogyan.** A szakasz első és utolsó pontja mindig megmarad. A köztes pontok közül azt választjuk, amelynek a merőleges távolsága (méterben, helyi `111_320` m/fok vetület) a két végpontot összekötő húrhoz a legnagyobb. Ha ez a távolság a tolerancia fölött van, a pontot megtartjuk, és mindkét oldalon rekurzívan folytatjuk; különben minden köztes pontot eldobunk.

Ez a közel egyenes szakaszok zaját csökkenti. GPS-zajt nem simít — a megmaradó sarkok élesek maradnak. Részletes leírás: [README.md](../README.md#douglaspeucker-map-simplify).
