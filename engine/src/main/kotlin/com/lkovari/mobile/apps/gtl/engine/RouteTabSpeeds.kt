package com.lkovari.mobile.apps.gtl.engine

object RouteTabSpeeds {
    fun instantMps(logging: Boolean, liveSpeedMps: Float?): Float? {
        return if (logging) liveSpeedMps else 0f
    }

    fun averageMps(logging: Boolean, sessionAverageMps: Float): Float {
        return if (logging) sessionAverageMps else 0f
    }
}
