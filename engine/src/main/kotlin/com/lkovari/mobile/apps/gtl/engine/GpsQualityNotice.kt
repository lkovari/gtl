package com.lkovari.mobile.apps.gtl.engine

object GpsQualityNotice {
    const val PoorAfterMillis = 20_000L

    fun isPoor(elapsedSinceLastAcceptMillis: Long): Boolean {
        return elapsedSinceLastAcceptMillis >= PoorAfterMillis
    }
}
