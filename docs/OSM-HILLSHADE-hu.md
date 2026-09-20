# OSM hillshade (HGT) — tervezet

[README-hu.md](../README-hu.md) · [dev-roadmap-hu.md](dev-roadmap-hu.md)

**Állapot:** nincs implementálva. Döntés és brief, ha később elkészül.  
**Nem kódspec:** ez a *miért* és a GTL-re szabott *hogyan*. Implementáció előtt a kiválasztott hullámra külön tesztlista kell.

A GTL OSM-térképe hivatalos Mapsforge `.map` (vektor). A **Domborzat** kapcsoló hillshade-et akar. Az nem a `.map` része: külön SRTM **HGT** kell.

---

## A nagy kép

Két adat, két letöltés:

| Mi | Forrás ma | Mi van benne |
| --- | --- | --- |
| OSM vektor | `https://download.mapsforge.org/maps/v5/.../{ország}.map` — `OsmCatalog` + `OsmDownloadWorker` | utak, víz, park, POI |
| Domborzat | **nincs a GTL-ben** | 1°×1° magasságcsempe, `.hgt` |

A Mapsforge 0.25 a hillshade-et **nem** a `.map`-ből olvassa. Kell `HillsRenderConfig` + `DemFolder` (SRTM HGT mappa), és ezt a `TileRendererLayer` konstruktorába kell adni.

A Domborzat kapcsoló (`settings_osm_hillshading`) csak a téma `hillshading` kategóriáját kapcsolja. Adat nélkül nincs árnyék.

---

## Honnan jön a HGT

A hivatalos, Mapsforge-hoz illő forrás a saját DEM tükre (Viewfinder Panoramas / SRTM, 3″ ≈ 90 m):

- https://download.mapsforge.org/maps/dem/dem3/
- tükör (gyorsabb): https://ftp-stud.hs-esslingen.de/Mirrors/download.mapsforge.org/maps/dem/dem3/

Példa (Budapest):  
`https://download.mapsforge.org/maps/dem/dem3/N47/N47E019.hgt.zip`

A fájlnév a **délnyugati sarok**: `N47E019.hgt` = 47–48°N, 19–20°E. Zipben jön, kicsomagolva SRTM3 ≈ 2,8 MB (`1201 × 1201 × 2` bájt). Déli / nyugati félteke: `S14W077.hgt`.

Ugyanaz az adat máshonnan:

- eredeti: [viewfinderpanoramas.org/dem3.html](https://www.viewfinderpanoramas.org/dem3.html)
- másik tükör: [bailu.ch/dem3](https://bailu.ch/dem3/)

A GTL letöltéséhez a Mapsforge / Esslingen út a cél. USGS Earthdata (login) és Copernicus GeoTIFF nem kellenek: a Mapsforge HGT-t vár.

Méretek (3″, kicsomagolva):

- egy csempe ≈ 2,8 MB
- Magyarország (~N45–N48 × E016–E022) ≈ 28 csempe ≈ 80 MB
- Németország ennek a többszöröse

1″ HGT (~25 MB/fok) telefonra, országos letöltésnek túl nehéz.

A Mapsforge DEM oldal szerint a tár **nem** tömeges letöltésre való. Országonként, felhasználói kérésre: rendben. Világ-scrape: nem.

---

## Mi van ma a fában

- Téma: `app/src/main/assets/mapsforge/gtl.xml` — `<hillshading cat="hillshading" zoom-min="9" zoom-max="17" />`
- Kapcsoló: Beállítások / Térkép rétegmenü; `OsmRenderOptions.hillshading`; alapból **ki**
- Kapu: `OsmHillshading.available(mapFile)` — akkor enged, ha a `.map` olvasható **és** a szülőkönyvtárban (vagy `hills/` alatta) van `.hgt` / `.hf2` / `.hgt.zip`
- `OsmRenderOptions.forMap(hillshadingAvailable)` — adat nélkül a kategória leesik
- Tárolás: minden régió `filesDir/maps/{regionId}.map` — **egy közös mappa**
- Rajzolás: `MapPane.attachOsmLayers` a `TileRendererLayer`-t **DEM nélkül** hozza létre
- OSM `AndroidView` kulcsa csak `filePath` — DEM-revízió nem építi újra a réteget

A hint szövege (HGT a `.map` mellett) a sideload-modellt írja. Play-felhasználónak ez nem elég: a hivatalos Mapsforge `.map`-ben nincs hillshade.

A teszt (`hgtBesideAnotherMapDoesNotEnableThisMap`) **külön** mappákat feltételez. Az app nem így tárol. Ha a HGT a közös `maps/` alá kerül, egy Budapest-csempe az Ausztria-térképet is „késznek” mutatja.

---

## Amit a GTL-ben célszerű

A termék: helyben futó logger, explicit letöltés, nincs saját szerver, nincs meglepetés-adatforgalom. Az OSM-lista már ilyen: a felhasználó nyomja a **Letöltés**t.

Ezért: **második, kézi letöltés** ugyanazon a soron, nem a `.map` mellé sütés, nem húzás közbeni csempefogás.

### 1. Külön gomb az OSM-listán

Csak ha a `.map` már megvan: **Domborzat letöltése**. Nem indul magától a térképpel.

Magyarország ~80 MB, Németország többszöröse — a felhasználó döntse el. Ugyanaz a WorkManager-minta, mint most (`CONNECTED`, unique work, progress). Külön work név, pl. `osm-dem-{regionId}`. A térkép-letöltéssel ne fusson egyszerre ugyanarra a régióra.

Egy soros hint a méretről (csempeszám × ~2,8 MB).

### 2. Közös csempetár, nem a `.map` mellett

```
filesDir/maps/eu-hungary.map
filesDir/dem/N47E019.hgt
filesDir/dem/N47E018.hgt
```

A csempe **földrajzi**, nem országos. HU / AT / SK ugyanazt a `N47E016`-ot használja; másodszor ne töltse. A hiányzó zip: Esslingen, fallback `download.mapsforge.org`. Kicsomagolás, SRTM3 méret ellenőrzése. Már megvan a fájl: skip.

Ne a `maps/` alá, ne `maps/hills/` alá. Az `OsmHillshading.available` ma a szülőt nézi — ezt a kaput a `dem/` + a **kiválasztott** térkép bboxára kell cserélni.

Sideload (`adb` a `dem/`-be) maradhat fejlesztői menekülőút. Play-út a második gomb.

### 3. Melyik csempék

A letöltött `MapFile` bounding boxából (a kamera már ezt olvassa `attachOsmLayers`-ben). Katalógusba nem kell bbox.

```
lat: floor(minLat) … floor(maxLat − ε)
lon: floor(minLon) … floor(maxLon − ε)
név: N47E019 / S14W077
```

Az engine tiszta: bounds → csempeazonosítók. Az app / worker a Mapsforge `MapFile.boundingBox()`-szal adja a számokat, miután a `.map` megvan.

A kamera a `.map` határán marad; pan közben nem kell extra csempe.

### 4. Kapcsoló

Akkor enged, ha a **kiválasztott** térkép bboxát lefedő csempék megvannak. Ne „van bármilyen HGT a `maps/` alatt”.

Alapból maradjon **ki** (ma is így van; extra GPU).

Részleges készlet: a Mapsforge a hiányzó csempéknél nem árnyékol. A kapcsolót akkor érdemes engedni, ha a régió csempekészlete **kész** — különben Nyugat-Magyarország lapos, a felhasználó azt hiszi, elromlott. Progress: `12/28 csempe`.

### 5. Törlés

Térképtörléskor **csak** az a DEM menjen, amit már egyetlen megmaradt `.map` sem használ. A közös csempék maradnak. Ha nem marad térkép, a `dem/` ürülhet.

### 6. Rajzolás

Mapsforge minta (`HillshadingMapViewer`):

- `DemFolderFS(demDir)`
- `AdaptiveClasyHillShading` (adaptive zoom)
- `MemoryCachingHgtReaderTileSource`
- `HillsRenderConfig` → `indexOnThread()`
- a config a `TileRendererLayer` konstruktorába (belső hillshade, nem külön overlay réteg)

A `HillsRenderConfig` konstruktor-paraméter. Téma-csere (`setXmlRenderTheme`) nem elég. Az OSM `AndroidView` ma `key(filePath)`: DEM-revízió nélkül a letöltés után üres marad a domborzat, amíg a réteg újra nem épül. Kulcs: `filePath` + DEM-revízió (ugyanaz a minta, mint `osmMapStore.downloadedRevision`).

---

## Amit ne

| Ötlet | Miért ne |
| --- | --- |
| HGT a `.map` letöltésbe | más URL, Németország százas MB, a felhasználó nem kérte |
| A `.map` URL cseréje | a v5 térkép vektor; hillshade nincs benne |
| Húzás közbeni csempeletöltés | meglepetés-adat, Mapsforge „not mass downloads”, a GTL explicit |
| APK-ba sütés | méret |
| 1″ HGT | ~25 MB/fok, országosan túl sok |
| Saját szerver / tükör | privacy, üzemeltetés; a Mapsforge tükör megvan |
| USGS / Copernicus GeoTIFF | login vagy más formátum |
| HGT a közös `maps/` alá | minden `.map` „késznek” látszik |

---

## Licenc / Névjegy

A DEM Viewfinder Panoramas, eredetileg SRTM (2000). A Mapsforge a saját csomagolását adja. Az OSM-vektor ODbL; a Térkép fülön már van **© OpenStreetMap**. Domborzatnál a Névjegy / súgó egy sora elég (forrás + SRTM / Viewfinder), ha a letöltés bekerül.

---

## Implementációs sorrend, ha elkészül

1. Engine: bounds → HGT csempenevek; `OsmHillshading.available` a `dem/` + kiválasztott bbox szerint (ne a `.map` szülője).
2. `OsmDemStore` / worker: zip → `.hgt`, skip ha megvan, Esslingen + fallback, progress csempeszám szerint.
3. OSM-lista: második gomb, mérethint, `12/28`.
4. `attachOsmLayers`: `HillsRenderConfig`, ha van adat; `AndroidView` kulcs + DEM-revízió.
5. Törlés: fel nem használt csempék.
6. Szövegek (EN/HU), súgó, Névjegy; a hint ne a „`.map` mellett”-et mondja.
7. Play what’s-new, ha a kiadásba bekerül.

A Domborzat kapcsoló és a téma már megvan. A hiány az adatút, a tár és a renderer config.
