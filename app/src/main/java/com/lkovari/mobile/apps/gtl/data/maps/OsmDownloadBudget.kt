package com.lkovari.mobile.apps.gtl.data.maps

object OsmDownloadBudget {
    const val MaxBytes = 4L * 1024L * 1024L * 1024L
    const val ReserveBytes = 64L * 1024L * 1024L
    const val SpaceCheckStride = 8L * 1024L * 1024L
    const val ReasonTooLarge = "too_large"
    const val ReasonNoSpace = "no_space"
    const val ReasonOther = "other"

    fun rejection(contentLength: Long, usableSpace: Long, alreadyOnDisk: Long = 0L): String? {
        if (usableSpace <= ReserveBytes) {
            return ReasonNoSpace
        }
        if (contentLength > MaxBytes) {
            return ReasonTooLarge
        }
        if (contentLength >= 0L) {
            val kept = alreadyOnDisk.coerceIn(0L, contentLength)
            val remaining = contentLength - kept
            if (remaining > usableSpace - ReserveBytes) {
                return ReasonNoSpace
            }
        }
        return null
    }

    fun blockBeforeStart(knownBytes: Long?, usableSpace: Long, alreadyOnDisk: Long = 0L): String? {
        if (knownBytes == null) {
            return if (usableSpace <= ReserveBytes) ReasonNoSpace else null
        }
        return rejection(knownBytes, usableSpace, alreadyOnDisk)
    }

    fun canStart(contentLength: Long, usableSpace: Long): Boolean {
        return rejection(contentLength, usableSpace) == null
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
