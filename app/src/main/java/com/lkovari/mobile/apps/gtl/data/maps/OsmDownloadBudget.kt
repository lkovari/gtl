package com.lkovari.mobile.apps.gtl.data.maps

object OsmDownloadBudget {
    const val MaxBytes = 2L * 1024L * 1024L * 1024L
    const val ReserveBytes = 64L * 1024L * 1024L
    const val SpaceCheckStride = 8L * 1024L * 1024L

    fun canStart(contentLength: Long, usableSpace: Long): Boolean {
        if (usableSpace <= ReserveBytes) {
            return false
        }
        if (contentLength > MaxBytes) {
            return false
        }
        if (contentLength >= 0L && contentLength > usableSpace - ReserveBytes) {
            return false
        }
        return true
    }

    fun copiedAllowed(copied: Long): Boolean {
        return copied <= MaxBytes
    }

    fun shouldRecheckSpace(copied: Long, lastCheck: Long): Boolean {
        return copied - lastCheck >= SpaceCheckStride
    }

    fun spaceAllowsMore(usableSpace: Long): Boolean {
        return usableSpace > ReserveBytes
    }
}
