package com.lkovari.mobile.apps.gtl.engine

object OsmRenderThemePath {
    const val RelativePathPrefix = ""
    const val ThemeFile = "mapsforge/gtl.xml"

    fun assetOpenPath(
        relativePathPrefix: String = RelativePathPrefix,
        themeFile: String = ThemeFile
    ): String {
        return relativePathPrefix + themeFile
    }
}
