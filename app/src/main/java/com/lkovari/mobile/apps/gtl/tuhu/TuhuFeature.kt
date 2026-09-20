package com.lkovari.mobile.apps.gtl.tuhu

object TuhuFeature {
    const val enabled = true

    fun showDownloadRow(): Boolean = TuhuUi.showDownloadRow(enabled)

    fun showSettings(inUse: Boolean): Boolean {
        return TuhuUi.showSettings(enabled, inUse)
    }

    fun showAbout(): Boolean = TuhuUi.showAbout(enabled)

    fun showHelp(mapDownloaded: Boolean): Boolean {
        return TuhuUi.showHelp(enabled, mapDownloaded)
    }

    fun showMapControls(selectedPath: String): Boolean {
        return TuhuUi.showMapControls(enabled, selectedPath)
    }

    fun isActive(selectedPath: String): Boolean {
        return TuhuUi.isActive(enabled, selectedPath)
    }

    fun isTuhuMap(path: String): Boolean = TuhuUi.isTuhuMap(path)
}
