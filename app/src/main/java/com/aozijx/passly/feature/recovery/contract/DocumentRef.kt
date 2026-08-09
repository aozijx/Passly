package com.aozijx.passly.feature.recovery.contract

/**
 * 文档引用，用于 Contract 层替代 [android.net.Uri]，
 * 避免 MVI Contract 直接依赖 Android 平台类型。
 */
@JvmInline
value class DocumentRef(val value: String)