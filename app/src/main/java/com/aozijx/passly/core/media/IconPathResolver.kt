package com.aozijx.passly.core.media

/**
 * Icon path conventions:
 * - remote URL: waiting for background sync download
 * - local path: ready for direct UI rendering
 */
fun isRemoteIconPath(path: String?): Boolean {
    val value = path?.trim().orEmpty()
    return value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)
}

fun toLocalIconImageModel(path: String?): String? {
    val value = path?.trim().orEmpty()
    if (value.isEmpty() || isRemoteIconPath(value)) return null
    return if (value.startsWith("file://", ignoreCase = true)) value else "file://$value"
}

