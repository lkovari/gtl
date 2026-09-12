package com.lkovari.mobile.apps.gtl.engine

object OsmMapViewRedraw {
    fun shouldRedrawLayers(width: Int, height: Int, oldWidth: Int, oldHeight: Int): Boolean {
        if (width <= 0 || height <= 0) {
            return false
        }
        return oldWidth == 0 || oldHeight == 0 || oldWidth != width || oldHeight != height
    }

    fun shouldRequestTiles(width: Int, height: Int): Boolean {
        return width > 0 && height > 0
    }

    fun mustPostAncestorInvalidate(calledOnMainThread: Boolean): Boolean {
        return !calledOnMainThread
    }
}
