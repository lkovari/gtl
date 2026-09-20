package com.lkovari.mobile.apps.gtl.engine

object OfflineMapUse {
    fun isInUse(useOffline: Boolean, selectedPath: String, candidatePath: String): Boolean {
        if (!useOffline || selectedPath.isBlank() || candidatePath.isBlank()) {
            return false
        }
        return selectedPath == candidatePath
    }
}
