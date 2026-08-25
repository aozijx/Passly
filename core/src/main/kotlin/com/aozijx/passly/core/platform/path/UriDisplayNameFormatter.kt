package com.aozijx.passly.core.platform.path

import java.net.URI

object UriDisplayNameFormatter {
    fun format(rawUri: String?, maxLength: Int = 32): String? {
        if (rawUri.isNullOrBlank()) return null
        val fileName = extractFileName(rawUri) ?: rawUri
        if (fileName.length <= maxLength) return fileName
        return truncateFileName(fileName, maxLength.coerceAtLeast(8))
    }

    private fun extractFileName(rawUri: String): String? =
        runCatching { URI(rawUri).path }.getOrNull()
            ?.substringAfterLast('/')
            ?.let { segment -> segment.substringAfterLast(':', missingDelimiterValue = segment) }
            ?.takeIf { it.isNotBlank() }

    private fun truncateFileName(
        fileName: String,
        maxLength: Int,
        ellipsis: String = "..."
    ): String {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        val suffix = if (extension.isBlank()) "" else ".${extension.take(8)}"
        val headBudget = (maxLength - suffix.length - ellipsis.length).coerceAtLeast(8)
        return "${fileName.take(headBudget)}$ellipsis$suffix"
    }
}
