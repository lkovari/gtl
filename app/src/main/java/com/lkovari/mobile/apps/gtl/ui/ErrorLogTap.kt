package com.lkovari.mobile.apps.gtl.ui

internal const val ErrorLogTapGoal = 7
internal const val ErrorLogTapWindowMillis = 2_000L

internal data class ErrorLogTapState(
    val count: Int,
    val opened: Boolean
)

internal fun errorLogTap(
    count: Int,
    lastTapMillis: Long,
    nowMillis: Long,
    windowMillis: Long = ErrorLogTapWindowMillis
): ErrorLogTapState {
    val continued = lastTapMillis != 0L && nowMillis - lastTapMillis <= windowMillis
    val next = if (continued) count + 1 else 1
    return if (next >= ErrorLogTapGoal) {
        ErrorLogTapState(count = 0, opened = true)
    } else {
        ErrorLogTapState(count = next, opened = false)
    }
}
