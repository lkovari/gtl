package com.lkovari.mobile.apps.gtl.engine

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object OsmMapFile {
    const val Magic = "mapsforge binary OSM"
    const val MinReadableBytes = 1024L

    fun isReadable(file: File): Boolean {
        if (!file.isFile) {
            return false
        }
        val length = file.length()
        if (length < MinReadableBytes) {
            return false
        }
        val header = ByteArray(36)
        file.inputStream().use { input ->
            if (input.read(header) != header.size) {
                return false
            }
        }
        val magic = String(header, 0, Magic.length, Charsets.US_ASCII)
        if (magic != Magic) {
            return false
        }
        val declared = ByteBuffer.wrap(header, 28, 8).order(ByteOrder.BIG_ENDIAN).long
        return declared == length && declared >= MinReadableBytes
    }
}
