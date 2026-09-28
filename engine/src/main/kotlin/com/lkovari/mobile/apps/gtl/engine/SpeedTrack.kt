package com.lkovari.mobile.apps.gtl.engine

data class TrackVertex(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val speedMps: Float? = null
)

data class SpeedRun(
    val band: SpeedBand,
    val points: List<GeoPoint>
)

object SpeedTrack {
    fun runs(
        vertices: List<TrackVertex>,
        keep: BooleanArray,
        usage: UsageType,
        system: MeasurementSystem
    ): List<SpeedRun> {
        if (keep.size != vertices.size) {
            return emptyList()
        }
        val kept = ArrayList<Int>()
        for (index in vertices.indices) {
            if (keep[index]) {
                kept.add(index)
            }
        }
        if (kept.size < 2) {
            return emptyList()
        }
        val chords = ArrayList<Chord>(kept.size - 1)
        for (pair in 1 until kept.size) {
            val start = kept[pair - 1]
            val end = kept[pair]
            chords.add(
                Chord(SpeedBands.of(chordSpeedMps(vertices, start, end), usage, system), start, end)
            )
        }
        val merged = ArrayList<Chord>()
        for (chord in chords) {
            val last = merged.lastOrNull()
            if (last != null && last.band == chord.band && last.end == chord.start) {
                merged[merged.lastIndex] = last.copy(end = chord.end)
            } else {
                merged.add(chord)
            }
        }
        return merged.map { chord ->
            val points = ArrayList<GeoPoint>()
            for (index in chord.start..chord.end) {
                if (keep[index]) {
                    val vertex = vertices[index]
                    points.add(GeoPoint(vertex.latitude, vertex.longitude, vertex.altitude))
                }
            }
            SpeedRun(chord.band, points)
        }
    }

    private fun chordSpeedMps(vertices: List<TrackVertex>, start: Int, end: Int): Float? {
        if (end == start + 1) {
            return finiteSpeed(vertices[end].speedMps)
        }
        var weighted = 0.0
        var weight = 0.0
        for (index in start + 1..end) {
            val speed = finiteSpeed(vertices[index].speedMps) ?: continue
            val previous = vertices[index - 1]
            val current = vertices[index]
            val distance = FixAcceptance.haversineMeters(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            )
            val sampleWeight = distance.coerceAtLeast(1.0)
            weighted += speed * sampleWeight
            weight += sampleWeight
        }
        if (weight == 0.0) {
            return null
        }
        return (weighted / weight).toFloat()
    }

    private fun finiteSpeed(speedMps: Float?): Float? {
        if (speedMps == null || !speedMps.isFinite()) {
            return null
        }
        return speedMps
    }

    private data class Chord(
        val band: SpeedBand,
        val start: Int,
        val end: Int
    )
}
