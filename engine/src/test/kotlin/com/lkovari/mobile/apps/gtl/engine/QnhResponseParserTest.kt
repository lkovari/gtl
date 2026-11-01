package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QnhResponseParserTest {
    @Test
    fun readsTheFirstModelPressure() {
        val body = """
            {
              "properties": {
                "timeseries": [
                  {
                    "data": {
                      "instant": {
                        "details": { "air_pressure_at_sea_level": 1018.4 }
                      }
                    }
                  },
                  {
                    "data": {
                      "instant": {
                        "details": { "air_pressure_at_sea_level": 900.0 }
                      }
                    }
                  }
                ]
              }
            }
        """.trimIndent()
        assertEquals(1018.4, QnhResponseParser.modelHpa(body)!!, 0.0)
    }

    @Test
    fun readsMetarAltimAndIgnoresSlp() {
        val body = """
            [
              {
                "icaoId": "LHNY",
                "obsTime": 1700000000,
                "lat": 47.983,
                "lon": 21.692,
                "altim": 1024,
                "slp": 1010
              }
            ]
        """.trimIndent()
        val report = QnhResponseParser.metars(body).single()
        assertEquals("LHNY", report.icao)
        assertEquals(1024.0, report.altimHpa!!, 0.0)
        assertEquals(1_700_000_000_000L, report.observedAtMillis)
        assertEquals(47.983, report.latitude, 0.0)
        assertEquals(21.692, report.longitude, 0.0)
    }

    @Test
    fun missingAltimStaysMissing() {
        val body = """
            [{ "icaoId": "XXXX", "obsTime": 1700000000, "lat": 47.0, "lon": 19.0, "slp": 1020 }]
        """.trimIndent()
        assertNull(QnhResponseParser.metars(body).single().altimHpa)
    }

    @Test
    fun brokenJsonIsEmpty() {
        assertNull(QnhResponseParser.modelHpa("not-json"))
        assertTrue(QnhResponseParser.metars("{").isEmpty())
    }
}
