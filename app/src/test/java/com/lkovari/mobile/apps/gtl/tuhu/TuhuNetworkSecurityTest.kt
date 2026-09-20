package com.lkovari.mobile.apps.gtl.tuhu

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TuhuNetworkSecurityTest {
    @Test
    fun cleartextIsLimitedToTuristautakHost() {
        val xml = configFile().readText()
        assertTrue(xml.contains("turistautak.elte.hu"))
        assertTrue(xml.contains("cleartextTrafficPermitted=\"true\""))
        assertTrue(xml.contains("cleartextTrafficPermitted=\"false\""))
    }

    private fun configFile(): File {
        val candidates = listOf(
            File("src/main/res/xml/network_security_config.xml"),
            File("app/src/main/res/xml/network_security_config.xml")
        )
        return candidates.first { it.isFile }
    }
}
