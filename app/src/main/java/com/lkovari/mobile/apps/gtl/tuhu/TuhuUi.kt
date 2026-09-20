package com.lkovari.mobile.apps.gtl.tuhu

import java.io.File

object TuhuUi {
    const val REGION_ID = "tuhu"

    fun isTuhuMap(path: String): Boolean {
        if (path.isBlank()) {
            return false
        }
        return File(path).name.equals("$REGION_ID.map", ignoreCase = true)
    }

    fun showDownloadRow(enabled: Boolean): Boolean = enabled

    fun showSettings(enabled: Boolean, inUse: Boolean): Boolean {
        return enabled && inUse
    }

    fun showAbout(enabled: Boolean): Boolean = enabled

    fun showHelp(enabled: Boolean, mapDownloaded: Boolean): Boolean {
        return enabled && mapDownloaded
    }

    fun showMapControls(enabled: Boolean, selectedPath: String): Boolean {
        return enabled && isTuhuMap(selectedPath)
    }

    fun isActive(enabled: Boolean, selectedPath: String): Boolean {
        return enabled && isTuhuMap(selectedPath)
    }
}

object TuhuDeletePolicy {
    fun forceGoogle(deletingSelectedTuhu: Boolean): Boolean = deletingSelectedTuhu
}
