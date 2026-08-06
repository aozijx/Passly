package com.aozijx.passly.core.util

import androidx.core.net.toUri

object PathDisplayFormatter {
    fun format(
        rawUri: String?,
        maxLength: Int = 32,
    ): String? {
        if (rawUri.isNullOrBlank()) return null

        val fileName = extractFileName(rawUri) ?: rawUri
        if (fileName.length <= maxLength) return fileName

        return truncateFileName(fileName, maxLength.coerceAtLeast(8))
    }

    private fun extractFileName(rawUri: String): String? {
        return runCatching { rawUri.toUri() }
            .getOrNull()
            ?.lastPathSegment
            ?.substringAfterLast(':', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * 按指定长度截断文件名，保留扩展名和头部
     */
    private fun truncateFileName(
        fileName: String,
        maxLength: Int,
        ellipsis: String = "..."
    ): String {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        val suffix = if (extension.isBlank()) {
            ""
        } else {
            ".${extension.take(8)}"
        }
        val headBudget = (maxLength - suffix.length - ellipsis.length).coerceAtLeast(8)
        val head = fileName.take(headBudget)
        return "$head$ellipsis$suffix"
    }
}
