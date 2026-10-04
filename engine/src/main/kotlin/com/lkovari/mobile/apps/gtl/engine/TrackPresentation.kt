package com.lkovari.mobile.apps.gtl.engine

data class TrackPresentationKey(
    val eventCount: Int,
    val lastEventMillis: Long,
    val lastLatitude: Double,
    val lastLongitude: Double,
    val usageName: String,
    val toleranceMeters: Double,
    val optimizationActive: Boolean,
    val showLastTrackOnMap: Boolean,
    val selectedSessionId: Long?,
    val viewingSessionId: Long?,
    val logging: Boolean,
    val mapCleared: Boolean,
    val measurementSystemName: String,
    val sessionUsageName: String?
)

object TrackPresentation {
    fun key(
        eventCount: Int,
        lastEventMillis: Long,
        lastLatitude: Double,
        lastLongitude: Double,
        usageName: String,
        toleranceMeters: Double,
        optimizationActive: Boolean,
        showLastTrackOnMap: Boolean,
        selectedSessionId: Long?,
        viewingSessionId: Long?,
        logging: Boolean,
        mapCleared: Boolean,
        measurementSystemName: String,
        sessionUsageName: String?
    ): TrackPresentationKey {
        return TrackPresentationKey(
            eventCount = eventCount,
            lastEventMillis = lastEventMillis,
            lastLatitude = lastLatitude,
            lastLongitude = lastLongitude,
            usageName = usageName,
            toleranceMeters = toleranceMeters,
            optimizationActive = optimizationActive,
            showLastTrackOnMap = showLastTrackOnMap,
            selectedSessionId = selectedSessionId,
            viewingSessionId = viewingSessionId,
            logging = logging,
            mapCleared = mapCleared,
            measurementSystemName = measurementSystemName,
            sessionUsageName = sessionUsageName
        )
    }

    fun same(left: TrackPresentationKey, right: TrackPresentationKey): Boolean {
        return left == right
    }
}
