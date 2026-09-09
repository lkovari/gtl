package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.atan2

object BikeLeanAngle {
    fun fromGravity(ax: Float, ay: Float, az: Float): Float {
        return Math.toDegrees(atan2(-ax.toDouble(), az.toDouble())).toFloat()
    }
}
