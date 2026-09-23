package com.lkovari.mobile.apps.gtl.data.search

import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import com.lkovari.mobile.apps.gtl.engine.MapSearch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tag
import org.mapsforge.core.model.Tile
import org.mapsforge.map.datastore.Way
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.reader.header.SubFileParameter
import java.io.File
import kotlin.coroutines.coroutineContext

enum class IndexOutcome {
    Done,
    Truncated,
    Cancelled
}

class MapSearchIndexer(
    private val dao: MapPlaceDao,
    private val gate: Mutex
) {
    suspend fun index(
        file: File,
        mapKey: String,
        isActive: () -> Boolean,
        onOrigin: (Double, Double) -> Unit
    ): IndexOutcome {
        batch.clear()
        val opened = openHeader(file)
        val origin = opened.origin
        onOrigin(origin.latitude, origin.longitude)
        val existing = gate.withLock { dao.state(mapKey) }
        var stored = gate.withLock { dao.count(mapKey) }
        var next = resumeCursor(opened.subs, existing)
        if (!isActive()) {
            return IndexOutcome.Cancelled
        }
        while (next != null) {
            coroutineContext.ensureActive()
            if (!isActive()) {
                return IndexOutcome.Cancelled
            }
            if (stored >= MapSearch.MaxIndexedPlaces) {
                save(file, mapKey, origin, next, done = false, truncated = true)
                return IndexOutcome.Truncated
            }
            val chunk = ArrayList<TileAddress>(TileBatch)
            var upcoming = next
            var taken = 0
            while (upcoming != null && taken < TileBatch) {
                chunk.add(upcoming)
                upcoming = advance(opened.subs, upcoming)
                taken += 1
            }
            if (chunk.isEmpty()) {
                break
            }
            val mapFile = MapFile(file)
            try {
                for (tile in chunk) {
                    coroutineContext.ensureActive()
                    if (!isActive()) {
                        batch.clear()
                        return IndexOutcome.Cancelled
                    }
                    if (stored >= MapSearch.MaxIndexedPlaces) {
                        flush(isActive)
                        save(file, mapKey, origin, tile, done = false, truncated = true)
                        return IndexOutcome.Truncated
                    }
                    stored += readTile(
                        mapFile,
                        file.absolutePath,
                        mapKey,
                        opened.subs[tile.subIndex],
                        tile.x,
                        tile.y,
                        opened.tileSize,
                        isActive
                    )
                }
            } finally {
                mapFile.close()
            }
            stored += flush(isActive)
            if (!isActive()) {
                batch.clear()
                return IndexOutcome.Cancelled
            }
            if (stored >= MapSearch.MaxIndexedPlaces) {
                save(file, mapKey, origin, upcoming, done = false, truncated = true)
                return IndexOutcome.Truncated
            }
            if (upcoming == null) {
                break
            }
            save(file, mapKey, origin, upcoming, done = false, truncated = false)
            next = upcoming
            yield()
        }
        if (!isActive()) {
            return IndexOutcome.Cancelled
        }
        flush(isActive)
        save(file, mapKey, origin, null, done = true, truncated = false)
        return IndexOutcome.Done
    }

    private fun openHeader(file: File): OpenedMap {
        val mapFile = MapFile(file)
        try {
            val start = mapFile.startPosition()
            val info = mapFile.mapFileInfo
            val header = mapFile.mapFileHeader
            val subs = ArrayList<SubFileParameter>()
            val seen = HashSet<Byte>()
            var zoom = info.zoomLevelMin
            while (zoom <= info.zoomLevelMax) {
                val sub = header.getSubFileParameter(zoom.toInt())
                if (sub != null && seen.add(sub.baseZoomLevel)) {
                    subs.add(sub)
                }
                if (zoom == Byte.MAX_VALUE) {
                    break
                }
                zoom = (zoom + 1).toByte()
            }
            subs.sortBy { it.baseZoomLevel }
            return OpenedMap(
                origin = LatLong(start.latitude, start.longitude),
                subs = subs,
                tileSize = info.tilePixelSize
            )
        } finally {
            mapFile.close()
        }
    }

    private fun resumeCursor(subs: List<SubFileParameter>, existing: MapIndexStateEntity?): TileAddress? {
        val first = firstTile(subs) ?: return null
        if (existing == null || existing.tileX == UNSET) {
            return first
        }
        val saved = TileAddress(existing.subIndex, existing.tileX, existing.tileY)
        if (contains(subs, saved)) {
            return saved
        }
        return first
    }

    private suspend fun save(
        file: File,
        mapKey: String,
        origin: LatLong,
        next: TileAddress?,
        done: Boolean,
        truncated: Boolean
    ) {
        if (!done && !truncated && next == null) {
            return
        }
        gate.withLock {
            val previous = dao.state(mapKey)
            dao.upsertState(
                MapIndexStateEntity(
                    mapKey = mapKey,
                    path = file.absolutePath,
                    done = done,
                    truncated = truncated,
                    attempt = if (done || truncated) 0 else previous?.attempt ?: 0,
                    nextAttemptAtMillis = if (done || truncated) 0L else previous?.nextAttemptAtMillis ?: 0L,
                    subIndex = next?.subIndex ?: previous?.subIndex ?: 0,
                    tileX = next?.x ?: previous?.tileX ?: UNSET,
                    tileY = next?.y ?: previous?.tileY ?: UNSET,
                    originLatitude = origin.latitude,
                    originLongitude = origin.longitude
                )
            )
        }
    }

    private suspend fun readTile(
        mapFile: MapFile,
        path: String,
        mapKey: String,
        sub: SubFileParameter,
        tileX: Long,
        tileY: Long,
        tileSize: Int,
        isActive: () -> Boolean
    ): Int {
        if (tileX < 0L || tileY < 0L || tileX > Int.MAX_VALUE || tileY > Int.MAX_VALUE) {
            return 0
        }
        val result = try {
            val tile = Tile(tileX.toInt(), tileY.toInt(), sub.baseZoomLevel, tileSize)
            mapFile.readNamedItems(tile)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: RuntimeException) {
            AppErrorLog.record("map.search.index", error)
            null
        } ?: return 0
        var inserted = 0
        for (poi in result.pois) {
            inserted += collect(
                path,
                mapKey,
                tagsOf(poi.tags),
                poi.position.latitude,
                poi.position.longitude,
                isActive
            )
        }
        for (way in result.ways) {
            val point = pointOf(way) ?: continue
            inserted += collect(
                path,
                mapKey,
                tagsOf(way.tags),
                point.latitude,
                point.longitude,
                isActive
            )
        }
        return inserted
    }

    private suspend fun collect(
        path: String,
        mapKey: String,
        tags: Map<String, String>,
        latitude: Double,
        longitude: Double,
        isActive: () -> Boolean
    ): Int {
        val record = MapSearch.record(tags) ?: return 0
        val gridLat = MapSearch.cell(latitude)
        val gridLon = MapSearch.cell(longitude)
        var inserted = 0
        for (alias in record.foldedAliases) {
            batch.add(
                MapPlaceEntity(
                    mapKey = mapKey,
                    path = path,
                    name = record.displayName,
                    nameFold = alias,
                    kind = record.kind.name,
                    latitude = latitude,
                    longitude = longitude,
                    gridLat = gridLat,
                    gridLon = gridLon
                )
            )
            if (batch.size >= BatchSize) {
                inserted += flush(isActive)
            }
        }
        return inserted
    }

    private suspend fun flush(isActive: () -> Boolean): Int {
        if (batch.isEmpty()) {
            return 0
        }
        return gate.withLock {
            if (!isActive()) {
                batch.clear()
                return@withLock 0
            }
            val ids = dao.insertAll(batch.toList())
            batch.clear()
            ids.count { it > 0L }
        }
    }

    private fun tagsOf(tags: List<Tag>): Map<String, String> {
        val map = LinkedHashMap<String, String>(tags.size)
        for (tag in tags) {
            val key = tag.key ?: continue
            val value = tag.value ?: continue
            map[key] = value
        }
        return map
    }

    private fun pointOf(way: Way): LatLong? {
        way.labelPosition?.let { return it }
        val ring = way.latLongs.firstOrNull() ?: return null
        if (ring.isEmpty()) {
            return null
        }
        var latitude = 0.0
        var longitude = 0.0
        for (point in ring) {
            latitude += point.latitude
            longitude += point.longitude
        }
        val count = ring.size.toDouble()
        return LatLong(latitude / count, longitude / count)
    }

    private fun firstTile(subs: List<SubFileParameter>): TileAddress? {
        val sub = subs.firstOrNull() ?: return null
        return TileAddress(0, sub.boundaryTileLeft, sub.boundaryTileTop)
    }

    private fun contains(subs: List<SubFileParameter>, tile: TileAddress): Boolean {
        val sub = subs.getOrNull(tile.subIndex) ?: return false
        return tile.x in sub.boundaryTileLeft..sub.boundaryTileRight &&
            tile.y in sub.boundaryTileTop..sub.boundaryTileBottom
    }

    private fun advance(subs: List<SubFileParameter>, current: TileAddress): TileAddress? {
        val sub = subs.getOrNull(current.subIndex) ?: return null
        val nextX = current.x + 1
        if (nextX <= sub.boundaryTileRight) {
            return TileAddress(current.subIndex, nextX, current.y)
        }
        val nextY = current.y + 1
        if (nextY <= sub.boundaryTileBottom) {
            return TileAddress(current.subIndex, sub.boundaryTileLeft, nextY)
        }
        val nextIndex = current.subIndex + 1
        val next = subs.getOrNull(nextIndex) ?: return null
        return TileAddress(nextIndex, next.boundaryTileLeft, next.boundaryTileTop)
    }

    private data class OpenedMap(
        val origin: LatLong,
        val subs: List<SubFileParameter>,
        val tileSize: Int
    )

    private data class TileAddress(
        val subIndex: Int,
        val x: Long,
        val y: Long
    )

    private val batch = ArrayList<MapPlaceEntity>(BatchSize)

    private companion object {
        const val BatchSize = 400
        const val TileBatch = 32
        const val UNSET = Long.MIN_VALUE
    }
}
