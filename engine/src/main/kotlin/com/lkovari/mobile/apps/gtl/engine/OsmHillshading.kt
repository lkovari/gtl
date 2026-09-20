package com.lkovari.mobile.apps.gtl.engine

import java.io.File

object OsmHillshading {
    fun available(mapFile: File): Boolean {
        if (!OsmMapFile.isReadable(mapFile)) {
            return false
        }
        val dir = mapFile.parentFile ?: return false
        return hasElevationFiles(dir) || hasElevationFiles(File(dir, "hills"))
    }

    fun isElevationFileName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".hgt") || lower.endsWith(".hf2") || lower.endsWith(".hgt.zip")
    }

    private fun hasElevationFiles(dir: File): Boolean {
        if (!dir.isDirectory) {
            return false
        }
        val files = dir.listFiles() ?: return false
        return files.any { it.isFile && isElevationFileName(it.name) }
    }
}
