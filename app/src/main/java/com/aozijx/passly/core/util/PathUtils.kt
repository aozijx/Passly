package com.aozijx.passly.core.util

import androidx.core.net.toUri

object PathUtils {
    fun formatPath(
        rawUri: String?,
        maxLength: Int = 32,
    ): String? {
        if (rawUri.isNullOrBlank()) return null

        // 1. 尝试提取可读文件名
        val fileName = extractFileName(rawUri) ?: rawUri

        // 2. 如果文件名本身就不长，直接返回
        if (fileName.length <= maxLength) return fileName

        // 3. 智能截断
        return truncateFileName(fileName, maxLength.coerceAtLeast(8))
    }

    /**
     * 从 URI 字符串中提取文件名（最后一段路径，并去除冒号前缀）
     */
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
            // 扩展名最多保留 8 个字符
            ".${extension.take(8)}"
        }
        val headBudget = (maxLength - suffix.length - ellipsis.length).coerceAtLeast(8)
        val head = fileName.take(headBudget)
        return "$head$ellipsis$suffix"
    }
}