package com.lkovari.mobile.apps.gtl.ui

internal const val DumpTapGoal = 3
internal const val DumpTapWindowMillis = 500L

internal data class DumpTapState(
    val count: Int,
    val opened: Boolean
)

internal fun dumpTap(
    count: Int,
    lastTapMillis: Long,
    nowMillis: Long,
    windowMillis: Long = DumpTapWindowMillis
): DumpTapState {
    val continued = lastTapMillis != 0L && nowMillis - lastTapMillis <= windowMillis
    val next = if (continued) count + 1 else 1
    return if (next >= DumpTapGoal) {
        DumpTapState(count = 0, opened = true)
    } else {
        DumpTapState(count = next, opened = false)
    }
}
