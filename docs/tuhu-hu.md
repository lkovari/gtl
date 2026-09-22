# Turistautak.hu térkép (tuhu.map) — opcionális feature

[README-hu.md](../README-hu.md) · [OSM-HILLSHADE-hu.md](OSM-HILLSHADE-hu.md)

**Állapot:** implementálva, kódban kapuzva. Alapból be (`TuhuFeature.enabled = true`). Play-release nélkül: a konstanst `false`-ra állítod, rebuild.

A Mapsforge **motor** ugyanaz, mint az OSM-nél (`MapFile` / `TileRendererLayer`). A Turistautak **termék** külön csomag: `app/.../tuhu/`, `assets/mapsforge/tuhu.xml`, `res/values/tuhu_strings.xml`.

---

## Bekapcsolás / kikapcsolás

A kapu `TuhuFeature.enabled` (`app/.../tuhu/TuhuFeature.kt`). Alapból `true`. Kikapcsolás egy release-hez: `false`, majd rebuild. Nincs `local.properties` kulcs.

Globális `usesCleartextTraffic` nincs. A letöltés csak HTTPS: `cleartextTrafficPermitted="false"`, HTTP tartalék nincs.

---

## Letöltés

URL: `https://turistautak.elte.hu/tuhu/tuhu_mapsforge.zip`  
User-Agent: `GPS Track Logger` (ugyanaz, mint az OSM worker).

`TuhuDownloadWorker`: zip → `filesDir/maps/tuhu.zip.part` → kicsomagolás `cacheDir/tuhu-extract` → első olvasható Mapsforge `.map` → `filesDir/maps/tuhu.map`. Ha a zipben van render XML, `filesDir/tuhu/theme.xml`; különben az asset `mapsforge/tuhu.xml`.

Sikertelen magic/méret: zip.part, staging és extract törlődik; a korábbi érvényes `tuhu.map` megmarad.

Törlés: OSM letöltő lista **fölött** Turistautak sor. Ha a törölt fájl volt a kiválasztott térkép → `selectedMapFile=""` és Google Maps (`useOfflineMap=false`). Más OSM régiók a lemezen maradhatnak. **Használható** / **Használatban**: egyszerre csak egy térkép lehet Használatban (egy OSM-régió vagy Turistautak). Használatban megnyomása Google Térképre vált.

---

## Láthatóság

| Felület | Flag false | Flag true, nincs fájl | Flag true, tuhu.map megvan |
|---|---|---|---|
| OSM letöltő | csak OSM régiók | + Turistautak sor felül | + Használható / Használatban / Törlés |
| Settings overlay | nincs TUHU blokk | nincs | TUHU accordion, ha Turistautak **Használatban** |
| Map Layers | OSM, ha OSM aktív | OSM, ha OSM aktív | Tuhu kapcsolók, ha tuhu a kiválasztott map |
| Help OSM térkép opciók | van | van | van |
| Help Turistautak opciók | nincs | nincs | van |
| About | csak OSM szekció | OSM + TUHU | OSM + TUHU |

A TUHU About **nem** a letöltéstől függ. Nincs Turistautak szó az `OsmCatalog`-ban, a `help_map_body`-ban vagy az OSM accordionban. Nincs kötés a `WALKING_HIKE` usage-hez.

---

## Nyolc kapcsoló

Saját DataStore: `tuhu_settings` (`tuhu_blazes` …). Nem keverednek az `osm_*` kulcsokkal.

| Kapcsoló | Cat | Alap | Mit csinál |
|---|---|---|---|
| Turistajelzések | `blazes` | be | `ref=(B)/(R)/(Y)/(G)` és társaik |
| Ösvénykiemelés | `paths` | be | `highway=path/track` magenta kiemelés (`#C4007A` / `#FF4FBF`, ugyanaz mint az OSM kerékpárút-kiemelés) |
| Szintvonal | `contours` | be | `contour=elevation` + major |
| Mellék szintvonal | `contours_minor` | ki | `contour_ext=elevation_minor` |
| Túra-POI | `hike_poi` | be | peak, spring, hut, shelter, cave, viewpoint |
| Védett terület | `parks` | ki | `boundary=national_park` |
| Városi POI | `urban_poi` | ki | shop, restaurant, parking, bus_stop |
| Domborzat | `hillshading` | ki | ugyanaz a HGT/`hills/` pipeline, mint OSM; adat nélkül disabled |

A `gtl.xml` OSM téma ezeket a Garmin/ref/contour tageket nem rajzolja jelzésnek. A tuhu térkép `tuhu.xml`-t kap.

Domborzat: nincs külön DEM-zip ebben a feature-ben. Ha a meglévő OSM hillshade-észlelő HGT-t talál a `.map` mellett vagy `hills/`-ben, a kapcsoló enabled.

---

## Kidobás

1. Flag false → UI eltűnik.
2. Teljes törlés: `app/.../tuhu/` mappa, `assets/mapsforge/tuhu.xml`, `tuhu_strings.xml` (en/hu), `docs/tuhu-hu.md`, `network_security_config` tuhu host, és a host `if (TuhuFeature…)` ágak (`GtlApplication`, `GtlViewModel`, `OsmDownloadScreen`, Settings, `MapPane`, Help, About).

Host tapintási pontok szándékosan kevesek. A ViewModel csak delegál a `TuhuPreferences` / `TuhuMapStore` felé.
