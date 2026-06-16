package com.aozijx.passly.core.platform

import java.io.File

object CacheUtils {

    fun calculateSize(dir: File): String {
        val bytes = dir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
        return formatFileSize(bytes)
    }

    fun clearDir(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                clearDir(file)
            }
            file.delete()
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024 * 1024))} GB"
        }
    }
}