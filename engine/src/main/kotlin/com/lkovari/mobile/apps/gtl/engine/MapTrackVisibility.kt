package com.lkovari.mobile.apps.gtl.engine

object MapTrackVisibility {
    fun visible(
        logging: Boolean,
        showLastTrackOnMap: Boolean,
        selectedSessionId: Long?,
        mapCleared: Boolean = false
    ): Boolean {
        if (logging) {
            return true
        }
        if (mapCleared) {
            return false
        }
        return showLastTrackOnMap || selectedSessionId != null
    }
}
