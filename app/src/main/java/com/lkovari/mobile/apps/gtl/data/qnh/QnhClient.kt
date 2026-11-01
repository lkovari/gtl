package com.lkovari.mobile.apps.gtl.data.qnh

import com.lkovari.mobile.apps.gtl.engine.QnhMetar
import com.lkovari.mobile.apps.gtl.engine.QnhResponseParser
import com.lkovari.mobile.apps.gtl.engine.QnhSelection
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

data class QnhLookup(
    val modelHpa: Double?,
    val metars: List<QnhMetar>,
    val latitude: Double,
    val longitude: Double,
    val failure: String?
)

class QnhClient {
    fun lookup(latitude: Double, longitude: Double): QnhLookup {
        val roundedLat = QnhSelection.roundCoordinate(latitude)
        val roundedLon = QnhSelection.roundCoordinate(longitude)
        val lat = QnhSelection.formatCoordinate(latitude)
        val lon = QnhSelection.formatCoordinate(longitude)
        val model = get("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$lat&lon=$lon")
        val modelHpa = model.body?.let { QnhResponseParser.modelHpa(it) }
        val near = QnhSelection.bbox(roundedLat, roundedLon, QnhSelection.BboxDeltaDegrees).query()
        var metar = get("https://aviationweather.gov/api/data/metar?format=json&bbox=$near")
        var reports = metar.body?.let { QnhResponseParser.metars(it) }.orEmpty()
        if (reports.isEmpty() && metar.failure == null) {
            val wide = QnhSelection.bbox(roundedLat, roundedLon, QnhSelection.BboxExpandedDeltaDegrees).query()
            metar = get("https://aviationweather.gov/api/data/metar?format=json&bbox=$wide")
            reports = metar.body?.let { QnhResponseParser.metars(it) }.orEmpty()
        }
        val failure = when {
            modelHpa != null || reports.isNotEmpty() -> null
            else -> prefer(model.failure, metar.failure) ?: "empty"
        }
        return QnhLookup(modelHpa, reports, roundedLat, roundedLon, failure)
    }

    private fun prefer(first: String?, second: String?): String? {
        val rank = listOf("timeout", "unreachable", "http", "empty")
        return listOfNotNull(first, second).minByOrNull { reason ->
            val index = rank.indexOf(reason)
            if (index < 0) rank.size else index
        }
    }

    private fun get(url: String): HttpBody {
        val opened = try {
            URL(url).openConnection()
        } catch (_: SocketTimeoutException) {
            return HttpBody(null, "timeout")
        } catch (_: UnknownHostException) {
            return HttpBody(null, "unreachable")
        } catch (_: IOException) {
            return HttpBody(null, "unreachable")
        }
        if (opened !is HttpURLConnection) {
            return HttpBody(null, "http")
        }
        opened.setRequestProperty("User-Agent", USER_AGENT)
        opened.connectTimeout = TIMEOUT_MILLIS
        opened.readTimeout = TIMEOUT_MILLIS
        opened.instanceFollowRedirects = true
        return try {
            opened.connect()
            val code = opened.responseCode
            if (code !in 200..299) {
                HttpBody(null, "http")
            } else {
                val text = opened.inputStream.use { input -> readLimited(input) }
                if (text.isNullOrBlank()) HttpBody(null, "empty") else HttpBody(text, null)
            }
        } catch (_: SocketTimeoutException) {
            HttpBody(null, "timeout")
        } catch (_: UnknownHostException) {
            HttpBody(null, "unreachable")
        } catch (_: IOException) {
            HttpBody(null, "unreachable")
        } finally {
            opened.disconnect()
        }
    }

    private fun readLimited(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(4096)
        var total = 0
        while (true) {
            val read = input.read(chunk)
            if (read < 0) {
                break
            }
            total += read
            if (total > MAX_BYTES) {
                return null
            }
            buffer.write(chunk, 0, read)
        }
        return buffer.toString(Charsets.UTF_8.name())
    }

    private data class HttpBody(val body: String?, val failure: String?)

    companion object {
        const val USER_AGENT = "GPS Track Logger (com.lkovari.mobile.apps.gtl; laszlo.kovary@gmail.com)"
        private const val TIMEOUT_MILLIS = 4_000
        private const val MAX_BYTES = 1_048_576
    }
}
