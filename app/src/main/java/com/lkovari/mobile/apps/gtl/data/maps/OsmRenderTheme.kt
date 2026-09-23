package com.lkovari.mobile.apps.gtl.data.maps

import android.content.res.AssetManager
import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import com.lkovari.mobile.apps.gtl.engine.OsmRenderCategories
import com.lkovari.mobile.apps.gtl.engine.OsmRenderOptions
import com.lkovari.mobile.apps.gtl.engine.OsmRenderThemePath
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.util.HashSet

object OsmRenderTheme {
    fun create(assets: AssetManager, options: OsmRenderOptions): XmlRenderTheme {
        return try {
            val theme = AssetsRenderTheme(
                assets,
                OsmRenderThemePath.RelativePathPrefix,
                OsmRenderThemePath.ThemeFile,
                MenuCallback(options)
            )
            theme.renderThemeAsStream.close()
            theme
        } catch (error: Throwable) {
            AppErrorLog.record("osm.theme", error)
            MapsforgeThemes.DEFAULT
        }
    }

    private class MenuCallback(
        private val options: OsmRenderOptions
    ) : XmlRenderThemeMenuCallback {
        override fun getCategories(style: XmlRenderThemeStyleMenu): Set<String> {
            val base = style.getLayer(style.defaultValue)
            if (base == null) {
                return HashSet(options.categoryIds())
            }
            val overlayCategories = LinkedHashMap<String, Set<String>>()
            for (overlay in base.overlays) {
                overlayCategories[overlay.id] = overlay.categories
            }
            return HashSet(
                OsmRenderCategories.enabled(
                    baseCategories = base.categories,
                    overlayCategories = overlayCategories,
                    enabledOverlayIds = options.categoryIds()
                )
            )
        }
    }
}
