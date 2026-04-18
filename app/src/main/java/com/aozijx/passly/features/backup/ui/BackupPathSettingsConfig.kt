package com.aozijx.passly.features.backup.ui

import androidx.core.net.toUri

internal object BackupPathSettingsConfig {
    const val NOT_SET_TEXT = "未设置"
    private const val RECENT_FILE_MAX_LEN = 32

    fun displayValue(rawUri: String?): String {
        if (rawUri.isNullOrBlank()) return NOT_SET_TEXT
        val parsed = runCatching { rawUri.toUri() }.getOrNull() ?: return rawUri
        val segment = parsed.lastPathSegment?.substringAfterLast(':')
        return segment?.takeIf { it.isNotBlank() } ?: rawUri
    }

    fun displayRecentFileName(fileName: String?): String {
        if (fileName.isNullOrBlank()) return NOT_SET_TEXT
        if (fileName.length <= RECENT_FILE_MAX_LEN) return fileName

        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        val suffix = if (extension.isBlank()) "" else ".${extension.take(8)}"
        val headBudget = (RECENT_FILE_MAX_LEN - suffix.length - 3).coerceAtLeast(8)
        val head = fileName.take(headBudget)
        return "$head...$suffix"
    }
}