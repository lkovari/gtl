package com.lkovari.mobile.apps.gtl.data.device

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceIdentity {
    fun displayName(context: Context): String {
        val assigned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        } else {
            null
        }
        if (!assigned.isNullOrBlank()) {
            return assigned
        }
        val brand = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        return when {
            brand.isNotEmpty() && model.isNotEmpty() && !model.contains(brand, ignoreCase = true) -> "$brand $model"
            model.isNotEmpty() -> model
            brand.isNotEmpty() -> brand
            else -> "Android"
        }
    }
}
