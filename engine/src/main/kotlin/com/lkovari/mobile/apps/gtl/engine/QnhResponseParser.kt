package com.lkovari.mobile.apps.gtl.engine

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object QnhResponseParser {
    fun modelHpa(body: String): Double? {
        val root = parseObject(body) ?: return null
        val timeseries = root.optJSONObject("properties")?.optJSONArray("timeseries") ?: return null
        val first = timeseries.optJSONObject(0) ?: return null
        val details = first.optJSONObject("data")
            ?.optJSONObject("instant")
            ?.optJSONObject("details")
            ?: return null
        return finite(details, "air_pressure_at_sea_level")
    }

    fun metars(body: String): List<QnhMetar> {
        val array = parseArray(body) ?: return emptyList()
        val reports = mutableListOf<QnhMetar>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val observed = item.optLong("obsTime", Long.MIN_VALUE)
            reports.add(
                QnhMetar(
                    altimHpa = finite(item, "altim"),
                    observedAtMillis = if (observed == Long.MIN_VALUE) {
                        null
                    } else {
                        toMillis(observed)
                    },
                    latitude = finite(item, "lat") ?: Double.NaN,
                    longitude = finite(item, "lon") ?: Double.NaN,
                    icao = item.optString("icaoId", "")
                )
            )
        }
        return reports
    }

    private fun finite(obj: JSONObject, key: String): Double? {
        if (!obj.has(key) || obj.isNull(key)) {
            return null
        }
        val value = obj.optDouble(key, Double.NaN)
        if (!value.isFinite()) {
            return null
        }
        return value
    }

    private fun toMillis(obsTime: Long): Long {
        return if (obsTime >= 1_000_000_000_000L) obsTime else obsTime * 1_000L
    }

    private fun parseObject(body: String): JSONObject? {
        return try {
            JSONObject(body)
        } catch (_: JSONException) {
            null
        }
    }

    private fun parseArray(body: String): JSONArray? {
        return try {
            JSONArray(body)
        } catch (_: JSONException) {
            null
        }
    }
}
