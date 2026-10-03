package com.lkovari.mobile.apps.gtl.engine

import java.util.Locale

fun formatDownloadSize(bytes: Long, locale: Locale = Locale.US): String {
    val gigabyte = 1024.0 * 1024.0 * 1024.0
    if (bytes >= gigabyte) {
        return String.format(locale, "%.1f GB", bytes / gigabyte)
    }
    val megabyte = 1024L * 1024L
    val rounded = (bytes + megabyte - 1L) / megabyte
    return "$rounded MB"
}
