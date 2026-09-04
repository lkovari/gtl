package com.lkovari.mobile.apps.gtl.data.sync

import com.lkovari.mobile.apps.gtl.data.db.GpsEventEntity
import com.lkovari.mobile.apps.gtl.data.db.TrackSessionEntity

interface RemoteTrackSync {
    suspend fun uploadSession(session: TrackSessionEntity, events: List<GpsEventEntity>)
}

class NoOpRemoteTrackSync : RemoteTrackSync {
    override suspend fun uploadSession(session: TrackSessionEntity, events: List<GpsEventEntity>) {
    }
}
