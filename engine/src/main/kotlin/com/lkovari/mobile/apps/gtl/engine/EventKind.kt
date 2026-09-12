package com.lkovari.mobile.apps.gtl.engine

enum class EventKind {
    START,
    MOVE,
    PAUSE,
    STOP;

    fun kmlPlacemarkName(): String {
        return when (this) {
            START -> "Start"
            MOVE -> "Move"
            PAUSE -> "Pause"
            STOP -> "Stop"
        }
    }

    fun kmlDrawOrder(): Int {
        return when (this) {
            STOP -> 10
            START -> 5
            PAUSE -> 1
            MOVE -> 0
        }
    }
}
