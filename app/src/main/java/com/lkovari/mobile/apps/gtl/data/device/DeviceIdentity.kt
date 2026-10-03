package com.lkovari.mobile.apps.gtl.data.device

import android.os.Build

object DeviceIdentity {
    fun displayName(): String {
        return format(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            androidRelease = Build.VERSION.RELEASE.orEmpty()
        )
    }

    fun format(manufacturer: String, model: String, androidRelease: String): String {
        val brand = manufacturer.trim()
        val hardware = model.trim()
        val release = androidRelease.trim()
        val device = when {
            brand.isNotEmpty() && hardware.isNotEmpty() && !hardware.contains(brand, ignoreCase = true) ->
                "$brand $hardware"
            hardware.isNotEmpty() -> hardware
            brand.isNotEmpty() -> brand
            else -> "Android"
        }
        return if (release.isEmpty()) device else "$device · Android $release"
    }
}
