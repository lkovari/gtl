package com.lkovari.mobile.apps.gtl.engine

data class TrackLogEvent(
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speedMps: Float,
    val kind: EventKind,
    val tempCelsius: Float? = null,
    val leanAngle: Float? = null,
    val usageType: String? = null,
    val baroAltitude: Double? = null
) {
    fun point(): GeoPoint {
        return GeoPoint(latitude, longitude, altitude)
    }
}

data class TrackLogMarker(
    val kind: EventKind,
    val event: TrackLogEvent
)

object TrackLogExport {
    const val MARKER_OVERLAP_METERS = 1.0

    fun path(events: List<TrackLogEvent>): List<TrackLogEvent> {
        if (events.isEmpty()) {
            return emptyList()
        }
        val last = events.last()
        return if (last.kind == EventKind.STOP && events.size > 1) {
            events.dropLast(1)
        } else {
            events
        }
    }

    fun markers(events: List<TrackLogEvent>): List<TrackLogMarker> {
        val path = path(events)
        if (path.isEmpty()) {
            return emptyList()
        }
        val startEvent = path.first().copy(kind = EventKind.START)
        val stopSource = events.lastOrNull { it.kind == EventKind.STOP }
        val stopEvent = if (stopSource != null) {
            path.last().copy(
                kind = EventKind.STOP,
                timestampMillis = stopSource.timestampMillis,
                speedMps = stopSource.speedMps,
                tempCelsius = stopSource.tempCelsius,
                leanAngle = stopSource.leanAngle,
                usageType = stopSource.usageType,
                baroAltitude = stopSource.baroAltitude
            )
        } else {
            null
        }
        val pauses = firstOfEachPauseRun(path)
            .filter { pause -> !near(pause, startEvent) }
            .filter { pause -> stopEvent == null || !near(pause, stopEvent) }
        val result = mutableListOf(TrackLogMarker(EventKind.START, startEvent))
        pauses.forEach { pause ->
            result.add(TrackLogMarker(EventKind.PAUSE, pause))
        }
        if (stopEvent != null) {
            result.add(TrackLogMarker(EventKind.STOP, stopEvent))
        }
        return result
    }

    fun samplesForStats(events: List<TrackLogEvent>): List<TrackSample> {
        val path = path(events)
        if (path.isEmpty()) {
            return emptyList()
        }
        val stop = events.lastOrNull { it.kind == EventKind.STOP }
        val forStats = if (stop != null) {
            path + path.last().copy(
                timestampMillis = stop.timestampMillis,
                kind = EventKind.STOP,
                speedMps = 0f
            )
        } else {
            path
        }
        return forStats.map { event ->
            TrackSample(
                timestampMillis = event.timestampMillis,
                latitude = event.latitude,
                longitude = event.longitude,
                altitude = event.altitude,
                speedMps = event.speedMps,
                bearing = 0f,
                ambientTemperature = event.tempCelsius,
                eventKind = event.kind
            )
        }
    }

    private fun firstOfEachPauseRun(path: List<TrackLogEvent>): List<TrackLogEvent> {
        val firsts = mutableListOf<TrackLogEvent>()
        var previousWasPause = false
        path.forEach { event ->
            val isPause = event.kind == EventKind.PAUSE
            if (isPause && !previousWasPause) {
                firsts.add(event)
            }
            previousWasPause = isPause
        }
        return firsts
    }

    private fun near(left: TrackLogEvent, right: TrackLogEvent): Boolean {
        return FixAcceptance.haversineMeters(
            left.latitude,
            left.longitude,
            right.latitude,
            right.longitude
        ) < MARKER_OVERLAP_METERS
    }
}

object KmlTrackBuilder {
    fun build(
        name: String,
        events: List<TrackLogEvent>,
        system: MeasurementSystem
    ): KmlTrack {
        val stats = TrackStatsCalculator.compute(TrackLogExport.samplesForStats(events))
        val path = TrackLogExport.path(events)
        val odometers = TrackStatsCalculator.cumulativeOdometerMeters(path.map { it.point() })
        val points = path.mapIndexed { index, event ->
            KmlVertex(
                point = event.point(),
                timestampMillis = event.timestampMillis,
                speedMps = event.speedMps,
                odometerMeters = odometers[index],
                baroAltitude = event.baroAltitude
            )
        }
        val placemarks = TrackLogExport.markers(events).map { marker ->
            KmlPlacemark(
                name = marker.kind.kmlPlacemarkName(),
                kind = marker.kind,
                point = marker.event.point(),
                description = KmlDescriptions.balloon(
                    kind = marker.kind,
                    timestampMillis = marker.event.timestampMillis,
                    latitude = marker.event.latitude,
                    longitude = marker.event.longitude,
                    altitude = marker.event.altitude,
                    baroAltitude = marker.event.baroAltitude,
                    speedMps = marker.event.speedMps,
                    tempCelsius = marker.event.tempCelsius,
                    maxSpeedMps = if (marker.kind == EventKind.STOP) stats.maxSpeedMps else null,
                    averageSpeedMps = if (marker.kind == EventKind.STOP) stats.averageSpeedMps else null,
                    elapsedMillis = elapsedFor(marker, path),
                    odometerMeters = odometerFor(marker, path, odometers),
                    system = system
                ),
                drawOrder = marker.kind.kmlDrawOrder()
            )
        }
        return KmlTrack(name = name, points = points, placemarks = placemarks)
    }

    private fun elapsedFor(marker: TrackLogMarker, path: List<TrackLogEvent>): Long {
        if (marker.kind == EventKind.START || path.isEmpty()) {
            return 0L
        }
        return (marker.event.timestampMillis - path.first().timestampMillis).coerceAtLeast(0L)
    }

    private fun odometerFor(
        marker: TrackLogMarker,
        path: List<TrackLogEvent>,
        odometers: List<Double>
    ): Double {
        if (odometers.isEmpty()) {
            return 0.0
        }
        return when (marker.kind) {
            EventKind.START -> odometers.first()
            EventKind.STOP -> odometers.last()
            else -> {
                val index = path.indexOfFirst { event ->
                    event.timestampMillis == marker.event.timestampMillis &&
                        event.latitude == marker.event.latitude &&
                        event.longitude == marker.event.longitude
                }
                if (index >= 0) odometers[index] else odometers.last()
            }
        }
    }
}
