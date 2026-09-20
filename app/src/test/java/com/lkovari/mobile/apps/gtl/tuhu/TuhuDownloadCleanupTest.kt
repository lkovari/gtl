package com.lkovari.mobile.apps.gtl.tuhu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TuhuDownloadCleanupTest {
    @Test
    fun failedAttemptRemovesPartStagingAndExtractButKeepsReadableMap() {
        val dir = kotlin.io.path.createTempDirectory("tuhu-cleanup").toFile()
        try {
            val mapsDir = File(dir, "maps")
            val cacheDir = File(dir, "cache")
            mapsDir.mkdirs()
            cacheDir.mkdirs()
            val kept = File(mapsDir, "${TuhuCatalog.REGION_ID}.map")
            kept.writeBytes(fakeMapsforgeBytes(2048))
            File(mapsDir, "${TuhuCatalog.REGION_ID}.zip.part").writeText("partial-zip")
            File(mapsDir, "${TuhuCatalog.REGION_ID}.map.staging").writeText("half-map")
            File(cacheDir, "tuhu-extract").mkdirs()
            File(cacheDir, "tuhu-extract/junk.map").writeText("junk")
            TuhuDownloadCleanup.purgeFailedAttempt(mapsDir, cacheDir)
            assertFalse(File(mapsDir, "${TuhuCatalog.REGION_ID}.zip.part").exists())
            assertFalse(File(mapsDir, "${TuhuCatalog.REGION_ID}.map.staging").exists())
            assertFalse(File(cacheDir, "tuhu-extract").exists())
            assertTrue(kept.exists())
        } finally {
            dir.deleteRecursively()
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
