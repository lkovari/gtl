package com.lkovari.mobile.apps.gtl.tuhu

import java.net.URL

object TuhuDownloadPolicy {
    const val MaxDownloadBytes = 500L * 1024L * 1024L
    const val MaxRedirects = 5
    const val AllowedHost = "turistautak.elte.hu"
    const val AllowedPath = "/tuhu/tuhu_mapsforge.zip"

    fun isAllowedUrl(url: URL): Boolean {
        if (!url.userInfo.isNullOrEmpty()) {
            return false
        }
        if (!url.query.isNullOrEmpty()) {
            return false
        }
        return url.protocol.equals("https", ignoreCase = true) &&
            url.host.equals(AllowedHost, ignoreCase = true) &&
            url.path == AllowedPath
    }
}
