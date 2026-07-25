package com.aozijx.passly.core.platform

import android.content.Context
import java.io.File

object CacheUtils {

    fun calculateSize(dir: File): String {
        val bytes = dir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
        return formatFileSize(bytes)
    }

    fun calculateTotalCacheSize(context: Context): String {
        val cacheDir = context.cacheDir
        val cacheBytes = if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
        val vaultImagesDir = File(context.filesDir, "vault_images")
        val vaultBytes = if (vaultImagesDir.exists()) {
            vaultImagesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
        return formatFileSize(cacheBytes + vaultBytes)
    }

    fun clearDir(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                clearDir(file)
            }
            file.delete()
        }
    }

    fun clearAllCache(context: Context) {
        clearDir(context.cacheDir)
        clearDir(File(context.filesDir, "vault_images"))
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