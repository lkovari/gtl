package com.lkovari.mobile.apps.gtl.tuhu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URL

class TuhuNetworkSecurityTest {
    @Test
    fun cleartextIsDeniedAndZipIsHttps() {
        val xml = configFile().readText()
        assertTrue(xml.contains("cleartextTrafficPermitted=\"false\""))
        assertFalse(xml.contains("cleartextTrafficPermitted=\"true\""))
        assertFalse(xml.contains("turistautak.elte.hu"))
        assertTrue(TuhuCatalog.ZIP_URL.startsWith("https://"))
        assertFalse(TuhuCatalog.ZIP_URL.startsWith("http://"))
        assertTrue(TuhuDownloadPolicy.isAllowedUrl(URL(TuhuCatalog.ZIP_URL)))
        assertFalse(TuhuDownloadPolicy.isAllowedUrl(URL("http://turistautak.elte.hu/tuhu/tuhu_mapsforge.zip")))
        assertFalse(TuhuDownloadPolicy.isAllowedUrl(URL("https://example.com/tuhu.zip")))
        assertFalse(TuhuDownloadPolicy.isAllowedUrl(URL("https://turistautak.elte.hu/other/tuhu_mapsforge.zip")))
        assertFalse(TuhuDownloadPolicy.isAllowedUrl(URL("https://user@turistautak.elte.hu/tuhu/tuhu_mapsforge.zip")))
        assertFalse(TuhuDownloadPolicy.isAllowedUrl(URL("https://turistautak.elte.hu/tuhu/tuhu_mapsforge.zip?dl=1")))
    }

    private fun configFile(): File {
        val candidates = listOf(
            File("src/main/res/xml/network_security_config.xml"),
            File("app/src/main/res/xml/network_security_config.xml")
        )
        return candidates.first { it.isFile }
    }
}
