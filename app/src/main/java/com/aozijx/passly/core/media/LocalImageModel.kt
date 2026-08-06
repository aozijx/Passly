package com.aozijx.passly.core.media

fun isRemoteImagePath(path: String?): Boolean {
    val value = path?.trim().orEmpty()
    return value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
}

fun toLocalIconImageModel(path: String?): String? {
    val value = path?.trim().orEmpty()
    if (value.isEmpty() || isRemoteImagePath(value)) return null
    return if (value.startsWith("file://", ignoreCase = true)) value else "file://$value"
}
