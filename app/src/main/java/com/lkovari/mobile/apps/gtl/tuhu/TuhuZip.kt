package com.lkovari.mobile.apps.gtl.tuhu

import com.lkovari.mobile.apps.gtl.engine.OsmMapFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream

data class TuhuZipExtract(
    val mapFile: File,
    val themeFile: File?
)

object TuhuZip {
    const val MaxZipEntries = 4000
    const val MaxUncompressedBytes = 1024L * 1024L * 1024L
    const val MaxThemeProbeBytes = 8 * 1024

    fun extract(
        zip: File,
        destDir: File,
        maxEntries: Int = MaxZipEntries,
        maxUncompressedBytes: Long = MaxUncompressedBytes
    ): TuhuZipExtract {
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }
        destDir.mkdirs()
        unzip(zip, destDir, maxEntries, maxUncompressedBytes)
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
            .firstOrNull { isRenderTheme(it) }
        return TuhuZipExtract(mapFile = map, themeFile = theme)
    }

    internal fun isRenderTheme(file: File): Boolean {
        return file.inputStream().use { input ->
            firstStartTagLocalName(input)?.equals("rendertheme", ignoreCase = true) == true
        }
    }

    private fun unzip(
        zip: File,
        destDir: File,
        maxEntries: Int,
        maxUncompressedBytes: Long
    ) {
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            var entries = 0
            var uncompressed = 0L
            var entry = input.nextEntry
            while (entry != null) {
                entries += 1
                if (entries > maxEntries) {
                    error("zip has too many entries")
                }
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
                        uncompressed += copyCapped(input, output, maxUncompressedBytes - uncompressed)
                    }
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }
    }

    private fun copyCapped(input: InputStream, output: OutputStream, remaining: Long): Long {
        val buffer = ByteArray(64 * 1024)
        var copied = 0L
        var read = input.read(buffer)
        while (read >= 0) {
            if (copied + read > remaining) {
                error("zip uncompressed size too large")
            }
            output.write(buffer, 0, read)
            copied += read
            read = input.read(buffer)
        }
        return copied
    }

    private fun firstStartTagLocalName(input: InputStream): String? {
        var used = 0
        var pushed = -2
        fun readByte(): Int {
            if (pushed != -2) {
                val value = pushed
                pushed = -2
                return value
            }
            if (used >= MaxThemeProbeBytes) {
                return -1
            }
            val value = input.read()
            if (value >= 0) {
                used += 1
            }
            return value
        }
        fun unread(value: Int) {
            pushed = value
        }
        fun skipUntil(first: Int, second: Int): Boolean {
            var previous = -1
            while (true) {
                val current = readByte()
                if (current < 0) {
                    return false
                }
                if (previous == first && current == second) {
                    return true
                }
                previous = current
            }
        }
        val bom = readByte()
        if (bom == 0xEF) {
            if (readByte() != 0xBB || readByte() != 0xBF) {
                return null
            }
        } else if (bom >= 0) {
            unread(bom)
        }
        while (true) {
            val current = readByte()
            if (current < 0) {
                return null
            }
            if (current == ' '.code || current == '\t'.code || current == '\n'.code || current == '\r'.code) {
                continue
            }
            if (current != '<'.code) {
                return null
            }
            val next = readByte()
            if (next < 0) {
                return null
            }
            if (next == '?'.code) {
                if (!skipUntil('?'.code, '>'.code)) {
                    return null
                }
                continue
            }
            if (next == '!'.code) {
                val mark = readByte()
                if (mark == '-'.code && readByte() == '-'.code) {
                    var first = readByte()
                    var second = if (first >= 0) readByte() else -1
                    var closed = false
                    while (first >= 0 && second >= 0) {
                        val third = readByte()
                        if (third < 0) {
                            return null
                        }
                        if (first == '-'.code && second == '-'.code && third == '>'.code) {
                            closed = true
                            break
                        }
                        first = second
                        second = third
                    }
                    if (!closed) {
                        return null
                    }
                } else {
                    var seen = mark
                    while (seen >= 0 && seen != '>'.code) {
                        seen = readByte()
                    }
                    if (seen != '>'.code) {
                        return null
                    }
                }
                continue
            }
            val name = StringBuilder()
            var ch = next
            while (ch >= 0 && isXmlNameChar(ch)) {
                name.append(ch.toChar())
                ch = readByte()
            }
            if (name.isEmpty()) {
                return null
            }
            return name.toString().substringAfter(':')
        }
    }

    private fun isXmlNameChar(value: Int): Boolean {
        val ch = value.toChar()
        return ch.isLetterOrDigit() || ch == '_' || ch == ':' || ch == '-' || ch == '.'
    }
}
