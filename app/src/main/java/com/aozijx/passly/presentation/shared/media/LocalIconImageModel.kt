package com.aozijx.passly.presentation.shared.media

@JvmInline
value class LocalIconImageModel internal constructor(
    val uri: String,
)

fun toLocalIconImageModel(path: String?): LocalIconImageModel? {
    val value = path?.trim().orEmpty()
    if (value.isEmpty() || value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) return null
    return LocalIconImageModel(if (value.startsWith("file://", ignoreCase = true)) value else "file://$value")
}
