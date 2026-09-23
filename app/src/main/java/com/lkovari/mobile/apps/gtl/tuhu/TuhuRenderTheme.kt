package com.lkovari.mobile.apps.gtl.tuhu

import android.content.res.AssetManager
import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import com.lkovari.mobile.apps.gtl.engine.OsmRenderCategories
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu
import org.mapsforge.map.rendertheme.ExternalRenderTheme
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File
import java.util.HashSet

object TuhuRenderTheme {
    fun create(assets: AssetManager, options: TuhuRenderOptions, externalTheme: File?): XmlRenderTheme {
        return try {
            val callback = MenuCallback(options)
            if (externalTheme != null && externalTheme.isFile) {
                ExternalRenderTheme(externalTheme, callback)
            } else {
                val theme = AssetsRenderTheme(
                    assets,
                    "",
                    TuhuCatalog.THEME_ASSET,
                    callback
                )
                theme.renderThemeAsStream.close()
                theme
            }
        } catch (error: Throwable) {
            AppErrorLog.record("tuhu.theme", error)
            MapsforgeThemes.DEFAULT
        }
    }

    private class MenuCallback(
        private val options: TuhuRenderOptions
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
