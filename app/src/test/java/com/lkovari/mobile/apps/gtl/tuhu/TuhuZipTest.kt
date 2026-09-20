package com.lkovari.mobile.apps.gtl.tuhu

import com.lkovari.mobile.apps.gtl.engine.OsmMapFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TuhuZipTest {
    @Test
    fun extractsReadableMapFromZip() {
        val dir = kotlin.io.path.createTempDirectory("tuhu-zip").toFile()
        try {
            val zip = File(dir, "tuhu.zip")
            writeZip(zip, mapOf("tuhu.map" to fakeMapsforgeBytes(4096)))
            val dest = File(dir, "out")
            val result = TuhuZip.extract(zip, dest)
            assertTrue(OsmMapFile.isReadable(result.mapFile))
            assertEquals("tuhu.map", result.mapFile.name)
            assertNull(result.themeFile)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun findsMapInNestedFolderAndOptionalTheme() {
        val dir = kotlin.io.path.createTempDirectory("tuhu-nested").toFile()
        try {
            val zip = File(dir, "tuhu.zip")
            writeZip(
                zip,
                mapOf(
                    "pack/readme.txt" to "hello".toByteArray(),
                    "pack/maps/tuhu.map" to fakeMapsforgeBytes(4096),
                    "pack/tuhu.xml" to "<rendertheme/>".toByteArray()
                )
            )
            val result = TuhuZip.extract(zip, File(dir, "out"))
            assertTrue(OsmMapFile.isReadable(result.mapFile))
            assertNotNull(result.themeFile)
            assertEquals("tuhu.xml", result.themeFile?.name)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun zipWithoutMapFails() {
        val dir = kotlin.io.path.createTempDirectory("tuhu-nomap").toFile()
        try {
            val zip = File(dir, "tuhu.zip")
            writeZip(zip, mapOf("notes.txt" to "no map".toByteArray()))
            try {
                TuhuZip.extract(zip, File(dir, "out"))
                fail("expected missing map")
            } catch (_: IllegalStateException) {
            }
            val leftover = File(dir, "out").listFiles().orEmpty()
            assertTrue(leftover.none { it.extension == "map" })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun truncatedMapInZipFailsAndLeavesNoMap() {
        val dir = kotlin.io.path.createTempDirectory("tuhu-bad").toFile()
        try {
            val zip = File(dir, "tuhu.zip")
            writeZip(zip, mapOf("tuhu.map" to "mapsforge binary OSM".toByteArray()))
            val dest = File(dir, "out")
            try {
                TuhuZip.extract(zip, dest)
                fail("expected unreadable map")
            } catch (_: IllegalStateException) {
            }
            assertFalse(dest.walkTopDown().any { it.extension == "map" && OsmMapFile.isReadable(it) })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun notAZipFails() {
        val dir = kotlin.io.path.createTempDirectory("tuhu-plain").toFile()
        try {
            val zip = File(dir, "tuhu.zip")
            zip.writeText("not a zip")
            try {
                TuhuZip.extract(zip, File(dir, "out"))
                fail("expected zip failure")
            } catch (_: Exception) {
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun truncatedZipFails() {
        val dir = kotlin.io.path.createTempDirectory("tuhu-trunczip").toFile()
        try {
            val zip = File(dir, "tuhu.zip")
            writeZip(zip, mapOf("tuhu.map" to fakeMapsforgeBytes(4096)))
            zip.writeBytes(zip.readBytes().copyOf(zip.length().toInt() / 2))
            try {
                TuhuZip.extract(zip, File(dir, "out"))
                fail("expected truncated zip")
            } catch (_: Exception) {
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun zipPathEscapeFails() {
        val dir = kotlin.io.path.createTempDirectory("tuhu-slip").toFile()
        try {
            val zip = File(dir, "tuhu.zip")
            writeZip(zip, mapOf("../escape.map" to fakeMapsforgeBytes(2048)))
            try {
                TuhuZip.extract(zip, File(dir, "out"))
                fail("expected zip slip")
            } catch (_: Exception) {
            }
            assertFalse(File(dir, "escape.map").exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun ignoresUnreadableMapWhenAnotherMapIsValid() {
        val dir = kotlin.io.path.createTempDirectory("tuhu-mixed").toFile()
        try {
            val zip = File(dir, "tuhu.zip")
            writeZip(
                zip,
                mapOf(
                    "junk.map" to ByteArray(16),
                    "good/tuhu.map" to fakeMapsforgeBytes(2048)
                )
            )
            val result = TuhuZip.extract(zip, File(dir, "out"))
            assertTrue(OsmMapFile.isReadable(result.mapFile))
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun writeZip(zip: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(zip.outputStream()).use { out ->
            entries.forEach { (name, bytes) ->
                out.putNextEntry(ZipEntry(name))
                out.write(bytes)
                out.closeEntry()
            }
        }
    }

    private fun fakeMapsforgeBytes(actualSize: Int): ByteArray {
        val bytes = ByteArray(actualSize)
        val magic = "mapsforge binary OSM".toByteArray(Charsets.US_ASCII)
        magic.copyInto(bytes)
        ByteBuffer.wrap(bytes, 28, 8).order(ByteOrder.BIG_ENDIAN).putLong(actualSize.toLong())
        return bytes
    }
}
