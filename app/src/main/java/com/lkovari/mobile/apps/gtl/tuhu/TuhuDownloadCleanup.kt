package com.lkovari.mobile.apps.gtl.tuhu

import java.io.File

object TuhuDownloadCleanup {
    fun purgeFailedAttempt(mapsDir: File, cacheDir: File) {
        File(mapsDir, "${TuhuCatalog.REGION_ID}.zip.part").delete()
        File(mapsDir, "${TuhuCatalog.REGION_ID}.map.staging").delete()
        File(cacheDir, "tuhu-extract").deleteRecursively()
    }
}
