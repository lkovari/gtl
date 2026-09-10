package com.lkovari.mobile.apps.gtl.engine

object MapDisplayUsage {
    fun followsSettings(logging: Boolean, selectedSessionId: Long?): Boolean {
        return logging || selectedSessionId != null
    }

    fun of(
        logging: Boolean,
        followSettings: Boolean,
        settingsUsage: UsageType,
        sessionUsageName: String?
    ): UsageType {
        if (logging || followSettings) {
            return settingsUsage
        }
        if (sessionUsageName.isNullOrBlank()) {
            return settingsUsage
        }
        return runCatching { UsageType.valueOf(sessionUsageName) }.getOrNull() ?: settingsUsage
    }

    fun simplify(
        usage: UsageType,
        logging: Boolean,
        followSettings: Boolean,
        settingsActive: Boolean,
        settingsTolerance: Double
    ): Pair<Boolean, Double> {
        if (logging || followSettings) {
            return settingsActive to DouglasPeucker.clampTolerance(settingsTolerance)
        }
        val defaults = usage.defaultSmoothing()
        return defaults.optimizationActive to defaults.optimizationToleranceMeters
    }
}
