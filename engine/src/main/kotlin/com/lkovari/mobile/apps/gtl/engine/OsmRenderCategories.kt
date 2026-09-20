package com.lkovari.mobile.apps.gtl.engine

object OsmRenderCategories {
    fun enabled(
        baseCategories: Set<String>,
        overlayCategories: Map<String, Set<String>>,
        enabledOverlayIds: Set<String>
    ): Set<String> {
        return buildSet {
            addAll(baseCategories)
            enabledOverlayIds.forEach { id ->
                val cats = overlayCategories[id]
                if (cats != null) {
                    addAll(cats)
                } else {
                    add(id)
                }
            }
        }
    }
}
