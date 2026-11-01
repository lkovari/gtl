# QNH lekérés induláskor

Implementáld a GTL-ben a tengerszinti légnyomás automatikus beállítását. A viselkedés, a források és az adatvédelmi szöveg kötött. Új Android-engedély, API-kulcs és követőszerver nem kell.

A `docs/play-console/privacy-policy.html` már leírja ezt a lekérést. Ne írd vissza a régit. Az élő oldal (`https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html`) közzététele a kiadó kézi lépése; Play-kiadás előtt a helyi HTML-nek és az élő oldalnak egyeznie kell. A Súgó továbbra is csak ezt a külső címet nyissa.

## Viselkedés

- Processzinduláskor, ha van `TYPE_PRESSURE` (`PressureSource.isAvailable`), megvan az `ACCESS_FINE_LOCATION`, és nincs nyitott rögzítés, egy pozícióból kérd le a QNH-t, és írd a meglévő Settings QNH értékbe (`GtlPreferences.setQnhHpa`).
- A hely még nincs meg `Application.onCreate`-ben. Várd meg az első használható fixet. Egy processzben egy lekérés legyen, ne minden fixre.
- Ha a felhasználó a Beállítások csúszkáját elengedi, az kézi érték. Tartsd meg a DataStore-ban, és többé ne írd felül automatikusan, sem induláskor, sem rögzítés közben.
- Kell egy külön vezérlő, ami törli a kézi jelzőt és azonnal újra lekér. Enélkül egyetlen csúszkaérintés örökre a régi nyomáson hagyja a barót.
- Futó vagy helyreállított rögzítés közben ne cseréld a QNH-t. A QNH élő: a szaggatott baro vonal, a HUD és a KMZ is elmozdulna, az offset pedig a régi QNH-hoz készült.
- Ha a lekérés még tart, és a felhasználó Indít, a `TrackingForegroundService.maybeAutoCalibrateBaro` várja meg a lekérést vagy annak timeoutját, és csak utána kalibráljon. Az offset abszolút (`BaroAltitude.calibrationOffsetHpa`): ha előbb 1013,25-re kalibrálsz, majd utána írsz 1025-öt, az offsetben bent marad a különbség.
- Ha a felhasználó a lekérés közben elhúzza a csúszkát, a válasz már nem írhatja a QNH-t. Írás előtt olvasd újra a kézi jelzőt.
- Hiba, timeout, 403 vagy üres válasz: maradjon a tárolt QNH. Ne ess vissza 1013,25-re, ha már volt valós érték.
- Koordinátát, URL-t és állomáskódot ne tegyél az `AppErrorLog` üzenetébe. Crashlyticsre pozíció nem mehet.

## Forrás

Két HTTPS végpont, kulcs nélkül. A kapcsolat mintája a `MapFileDownloader`: `HttpURLConnection`, cleartext tilos (`network_security_config`). Timeout 4 másodperc connect és read. Egy saját User-Agent mindkét kérésen:

`GPS Track Logger (com.lkovari.mobile.apps.gtl; laszlo.kovary@gmail.com)`

A MET Norway User-Agent nélkül 403-at ad. Az Aviation Weather saját User-Agentet kér.

A pozíciót a kérésben 3 tizedesre kerekítsd (kb. 100 m). Ezt a tájékoztató is így írja.

1. Pontbeli modellnyomás, ez a világ bármely koordinátáján a baro bemenete:

`https://api.met.no/weatherapi/locationforecast/2.0/compact?lat={lat}&lon={lon}`

A mező: `properties.timeseries[0].data.instant.details.air_pressure_at_sea_level` (hPa). Csak az első időszelet kell. Ez meteorológiai tengerszinti nyomás, nem repülőtéri QNH.

2. Mért QNH, ha van közeli friss METAR:

`https://aviationweather.gov/api/data/metar?format=json&bbox={minLat},{minLon},{maxLat},{maxLon}`

A `bbox` sorrendje `minLat,minLon,maxLat,maxLon`. A doboz a kerekített pont köré essen, elég nagy legyen egy állomáshoz (kb. ±0,5°), és ha üres, egyszer növeld kb. ±1°-ra. A JSON mező a QNH: `altim` (hPa). A `slp` tengerszinti légnyomás, azt ne használd QNH-nak. A nyers szövegben az európai `Q1024` és az amerikai `Axxxx` is az `altim`-ban van.

Döntés a `:engine` tiszta függvényében, hálózat nélkül:

- METAR csak akkor jelölt, ha van `altim`, 90 percnél nem régebbi, és a `lat`/`lon` véges.
- 30 km-en belül a legközelebbi ilyen METAR nyer.
- `UsageType.AIRCRAFT` esetén ez a sugár 100 km. A pilóta a környék repülőterének QNH-ját állítja. 1013,25-re átszámolni nem kell: a képlet QNH-magasságot ad, nem repülési szintet.
- Egyébként a modellnyomás nyer. Sátoraljaújhelyen (48,395, 21,656) a Kassa (LZKZ) és a Nyíregyháza (LHNY) is kb. 45 km, mindkettő kívül esik a 30 km-en, tehát földön a modell értéke a beállítás.
- Ha modell nincs és a legközelebbi METAR 100 km-en belül van, az maradhat. Ha egyik sincs, ne írj QNH-t.

Kerekítés a csúszka rácsára: `BaroAltitude.clampQnh` után egész hPa (`900…1100`, a slider `steps` értéke 199). 1 hPa kb. 8 m.

## Kód

A döntés, a kerekítés, a távolság és a „szabad-e felülírni” az `engine` modulba kerül, JUnit alá. A HTTP, a DataStore és a felület az `app` modulba. Forráskódba kommentet ne írj. Új Kotlin `as` castot ne vezess be; a JSON-t `org.json` `opt*` olvasóival parse-old.

DataStore, a meglévő `qnh_hpa` mellé:

- kézi jelző, alapból hamis
- forrás: modell, METAR vagy kézi
- lekérés ideje
- állomás ICAO, ha METAR
- a lekéréshez használt koordináta, csak a feliratnak

A csúszka `onValueChangeFinished` útja (`GtlViewModel.setQnhHpa`) állítsa a kézi jelzőt. Az automatikus írás ne ezen az úton menjen, különben saját magát kézinek jelöli.

Felület, csak ha `pressureAvailable`, a meglévő QNH sor alatt (`SecondaryScreens`, `settings_qnh`):

- honnan jött: modell, vagy ICAO és km
- a lekérés kora
- kézi állapot
- egy chip, ami visszaadja az automatikus lekérést

Szövegek `values/strings.xml` és `values-hu/strings.xml`. A `settings_qnh_hint` ne állítsa, hogy a QNH csak METAR; modell is lehet.

A Beállítások súgó és a `README-en.md` / `README-hu.md` QNH bekezdése mondja el ugyanezt: induláskor töltődik, a kézi csúszka marad, a baro továbbra is `h = 44330 × (1 − (p / QNH)^(1/5.255))`. Az `INTERNET` zárójel a README-ben egészüljön ki a QNH-lekéréssel. A tájékoztató mondatát ne másold ki harmadszor a Súgóba; a Súgó a meglévő privacy linket nyitja.

Nyomás alatt álló kabinban a telefon a kabinmagasságot méri. Ezt a repülés súgójában egy mondatban írd le. QNH ezt nem javítja.

## Ellenőrzés

Engine tesztek, hálózat nélkül:

- 30 km-en belüli friss METAR nyer a modell ellenében
- 45 km-es METAR földön veszít, repülésen nyer
- 90 percnél régebbi vagy `altim` nélküli jelentés kiesik
- kézi jelző mellett az automatikus érték nem ír
- kerekítés és `clampQnh` a 900–1100 határon
- a kérés koordinátája 3 tizedes

A parserhez tegyél egy rövid, rögzített JSON mintát mindkét válaszból. Éles hálózat ne legyen a unit tesztben.
