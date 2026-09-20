package com.lkovari.mobile.apps.gtl.engine

enum class MapCameraMode {
    FitTrack,
    FollowLive,
    Free;

    companion object {
        fun of(logging: Boolean, keepWholeTrack: Boolean, viewingSaved: Boolean): MapCameraMode {
            if (keepWholeTrack || viewingSaved) {
                return FitTrack
            }
            if (logging) {
                return FollowLive
            }
            return Free
        }
    }
}
