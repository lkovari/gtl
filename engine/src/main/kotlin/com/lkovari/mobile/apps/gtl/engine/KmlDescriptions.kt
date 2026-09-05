package com.lkovari.mobile.apps.gtl.engine

object KmlDescriptions {
    fun balloon(kind: EventKind, speedMps: Float, tempCelsius: Float?): String {
        val speed = if (kind == EventKind.PAUSE || kind == EventKind.STOP) {
            "0"
        } else {
            speedMps.toString()
        }
        val temp = tempCelsius?.toString() ?: "-"
        return "speed=$speed temp=$temp"
    }
}
