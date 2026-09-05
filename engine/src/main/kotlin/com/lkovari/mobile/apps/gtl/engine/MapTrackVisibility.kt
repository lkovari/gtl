package com.lkovari.mobile.apps.gtl.engine

object MapTrackVisibility {
    fun visible(logging: Boolean, showLastTrackOnMap: Boolean, selectedSessionId: Long?): Boolean {
        return logging || showLastTrackOnMap || selectedSessionId != null
    }
}
