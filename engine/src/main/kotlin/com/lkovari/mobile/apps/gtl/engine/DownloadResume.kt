package com.lkovari.mobile.apps.gtl.engine

enum class DownloadBodyMode {
    Overwrite,
    Append
}

object DownloadResume {
    fun rangeHeader(existingBytes: Long): String? {
        if (existingBytes <= 0L) {
            return null
        }
        return "bytes=$existingBytes-"
    }

    fun bodyMode(responseCode: Int, existingBytes: Long): DownloadBodyMode {
        if (responseCode == 206 && existingBytes > 0L) {
            return DownloadBodyMode.Append
        }
        return DownloadBodyMode.Overwrite
    }

    fun contentRangeTotal(header: String?): Long? {
        if (header.isNullOrBlank()) {
            return null
        }
        val slash = header.lastIndexOf('/')
        if (slash < 0 || slash == header.lastIndex) {
            return null
        }
        val total = header.substring(slash + 1).trim()
        if (total == "*") {
            return null
        }
        return total.toLongOrNull()
    }

    fun fullSize(
        existingBytes: Long,
        responseCode: Int,
        contentLength: Long,
        contentRangeTotal: Long?
    ): Long {
        if (responseCode == 206) {
            if (contentRangeTotal != null && contentRangeTotal >= 0L) {
                return contentRangeTotal
            }
            if (contentLength >= 0L) {
                return existingBytes + contentLength
            }
            return -1L
        }
        if (contentLength >= 0L) {
            return contentLength
        }
        return -1L
    }
}
