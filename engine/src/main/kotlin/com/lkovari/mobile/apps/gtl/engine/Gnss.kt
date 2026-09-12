package com.lkovari.mobile.apps.gtl.engine

enum class GnssConstellation {
    GPS,
    SBAS,
    GLONASS,
    QZSS,
    BEIDOU,
    GALILEO,
    IRNSS,
    UNKNOWN
}

enum class GpsBand {
    L1,
    L5,
    OTHER
}

data class SatelliteSample(
    val constellation: GnssConstellation,
    val svid: Int,
    val usedInFix: Boolean,
    val cn0DbHz: Float,
    val carrierFrequencyHz: Float?,
    val azimuthDegrees: Float,
    val elevationDegrees: Float
)

data class ConstellationCount(
    val inView: Int,
    val usedInFix: Int
)

enum class SignalQuality {
    NONE,
    POOR,
    FAIR,
    GOOD,
    EXCELLENT
}

data class GnssSnapshot(
    val byConstellation: Map<GnssConstellation, ConstellationCount>,
    val gpsL1: ConstellationCount,
    val gpsL5: ConstellationCount,
    val averageCn0: Double,
    val usedAverageCn0: Double,
    val maxCn0: Double,
    val satellitesInView: Int,
    val satellitesInFix: Int,
    val signalQuality: SignalQuality,
    val satellites: List<SatelliteSample>
)

object GnssClassifier {
    const val GPS_L1_HZ = 1_575_420_000f
    const val GPS_L5_HZ = 1_176_450_000f
    private const val BAND_TOLERANCE_HZ = 2_000_000f

    fun constellationFromAndroid(value: Int): GnssConstellation {
        return when (value) {
            1 -> GnssConstellation.GPS
            2 -> GnssConstellation.SBAS
            3 -> GnssConstellation.GLONASS
            4 -> GnssConstellation.QZSS
            5 -> GnssConstellation.BEIDOU
            6 -> GnssConstellation.GALILEO
            7 -> GnssConstellation.IRNSS
            else -> GnssConstellation.UNKNOWN
        }
    }

    fun gpsBand(carrierFrequencyHz: Float?): GpsBand {
        if (carrierFrequencyHz == null || carrierFrequencyHz <= 0f) {
            return GpsBand.OTHER
        }
        val l1Delta = kotlin.math.abs(carrierFrequencyHz - GPS_L1_HZ)
        val l5Delta = kotlin.math.abs(carrierFrequencyHz - GPS_L5_HZ)
        return when {
            l5Delta <= BAND_TOLERANCE_HZ -> GpsBand.L5
            l1Delta <= BAND_TOLERANCE_HZ -> GpsBand.L1
            else -> GpsBand.OTHER
        }
    }

    fun snapshot(samples: List<SatelliteSample>): GnssSnapshot {
        val grouped = GnssConstellation.entries.associateWith { constellation ->
            val matching = samples.filter { it.constellation == constellation }
            ConstellationCount(
                inView = matching.size,
                usedInFix = matching.count { it.usedInFix }
            )
        }
        val gps = samples.filter { it.constellation == GnssConstellation.GPS }
        val gpsL1 = gps.filter { gpsBand(it.carrierFrequencyHz) == GpsBand.L1 }
        val gpsL5 = gps.filter { gpsBand(it.carrierFrequencyHz) == GpsBand.L5 }
        val cn0Values = samples.map { it.cn0DbHz.toDouble() }.filter { it > 0.0 }
        val usedCn0 = samples.filter { it.usedInFix }.map { it.cn0DbHz.toDouble() }.filter { it > 0.0 }
        val average = if (cn0Values.isEmpty()) 0.0 else cn0Values.average()
        val usedAverage = if (usedCn0.isEmpty()) average else usedCn0.average()
        val qualitySource = if (usedAverage > 0.0) usedAverage else average
        return GnssSnapshot(
            byConstellation = grouped,
            gpsL1 = ConstellationCount(gpsL1.size, gpsL1.count { it.usedInFix }),
            gpsL5 = ConstellationCount(gpsL5.size, gpsL5.count { it.usedInFix }),
            averageCn0 = average,
            usedAverageCn0 = usedAverage,
            maxCn0 = cn0Values.maxOrNull() ?: 0.0,
            satellitesInView = samples.size,
            satellitesInFix = samples.count { it.usedInFix },
            signalQuality = qualityFromCn0(qualitySource, samples.size),
            satellites = samples
        )
    }

    fun qualityFromCn0(averageCn0: Double, satellitesInView: Int): SignalQuality {
        if (satellitesInView == 0 || averageCn0 <= 0.0) {
            return SignalQuality.NONE
        }
        return when {
            averageCn0 >= 35.0 -> SignalQuality.EXCELLENT
            averageCn0 >= 25.0 -> SignalQuality.GOOD
            averageCn0 >= 18.0 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
    }
}
