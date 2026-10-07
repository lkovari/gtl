package com.lkovari.mobile.apps.gtl.engine

data class BaroFreshnessState(
    val lastHpa: Float? = null,
    val lastEventNanos: Long? = null,
    val lastChangeNanos: Long? = null
)

enum class BaroFeedStatus {
    Waiting,
    Live,
    Stale,
    Frozen
}

object BaroFreshness {
    const val MaxEventAgeNanos = 5_000_000_000L
    const val MaxFrozenNanos = 120_000_000_000L
    const val FirstSampleGraceNanos = 10_000_000_000L

    fun observe(state: BaroFreshnessState, hpa: Float, eventNanos: Long): BaroFreshnessState {
        val changed = state.lastHpa == null || state.lastHpa != hpa
        return BaroFreshnessState(
            lastHpa = hpa,
            lastEventNanos = eventNanos,
            lastChangeNanos = if (changed) eventNanos else state.lastChangeNanos ?: eventNanos
        )
    }

    fun status(state: BaroFreshnessState, nowNanos: Long, listeningSinceNanos: Long): BaroFeedStatus {
        val lastEvent = state.lastEventNanos
        if (lastEvent == null) {
            return if (nowNanos - listeningSinceNanos > FirstSampleGraceNanos) {
                BaroFeedStatus.Stale
            } else {
                BaroFeedStatus.Waiting
            }
        }
        if (nowNanos - lastEvent > MaxEventAgeNanos) {
            return BaroFeedStatus.Stale
        }
        val lastChange = state.lastChangeNanos ?: lastEvent
        if (lastEvent - lastChange > MaxFrozenNanos) {
            return BaroFeedStatus.Frozen
        }
        return BaroFeedStatus.Live
    }
}
