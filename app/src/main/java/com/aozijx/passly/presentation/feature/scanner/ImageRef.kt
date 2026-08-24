package com.aozijx.passly.presentation.feature.scanner

/**
 * 图片引用，用于 Contract 层替代 [android.net.Uri]，
 * 避免 MVI Contract 直接依赖 Android 平台类型。
 */
@JvmInline
value class ImageRef(val value: String)