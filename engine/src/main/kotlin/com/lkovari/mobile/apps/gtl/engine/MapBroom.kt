package com.lkovari.mobile.apps.gtl.engine

object MapBroom {
    fun visible(viewingSaved: Boolean, importedTrack: Boolean): Boolean {
        return viewingSaved || importedTrack
    }

    fun clearImported(importedTrack: Boolean): Boolean {
        return importedTrack
    }

    fun clearSaved(viewingSaved: Boolean): Boolean {
        return viewingSaved
    }
}
