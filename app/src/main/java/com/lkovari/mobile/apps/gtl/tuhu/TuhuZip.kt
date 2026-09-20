package com.lkovari.mobile.apps.gtl.tuhu

import com.lkovari.mobile.apps.gtl.engine.OsmMapFile
import java.io.File
import java.util.zip.ZipInputStream

data class TuhuZipExtract(
    val mapFile: File,
    val themeFile: File?
)

object TuhuZip {
    fun extract(zip: File, destDir: File): TuhuZipExtract {
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }
        destDir.mkdirs()
        unzip(zip, destDir)
        val maps = destDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("map", ignoreCase = true) }
            .filter { OsmMapFile.isReadable(it) }
            .toList()
        val map = maps.firstOrNull()
        if (map == null) {
            destDir.deleteRecursively()
            error("zip has no readable mapsforge map")
        }
        val theme = destDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .firstOrNull()
        return TuhuZipExtract(mapFile = map, themeFile = theme)
    }

    private fun unzip(zip: File, destDir: File) {
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                val destRoot = destDir.canonicalFile
                val target = File(destDir, entry.name).canonicalFile
                val destPath = destRoot.path
                val inside = target == destRoot || target.path.startsWith(destPath + File.separator)
                if (!inside) {
                    error("zip path escapes destination")
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }
    }
}
