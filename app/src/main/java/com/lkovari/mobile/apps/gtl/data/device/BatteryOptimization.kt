package com.lkovari.mobile.apps.gtl.data.device

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Battery optimization exemption for background recording.
 *
 * A foreground service alone does not survive every vendor task killer: on the Xever 7 Pro
 * `com.pri.screenoff.killer` stopped GTL mid-recording while the screen was off, but left apps on
 * the `deviceidle` exemption list alone. Recording is the app's core function, so the exemption
 * is requested before Start.
 */
object BatteryOptimization {
    fun isIgnoring(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return true
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Direct system dialog for this app; needs `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. */
    @SuppressLint("BatteryLife")
    fun requestIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
    }

    /** Fallback list screen when a vendor ROM has no handler for [requestIntent]. */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
}
