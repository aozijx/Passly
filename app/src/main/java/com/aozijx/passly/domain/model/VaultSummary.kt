package com.aozijx.passly.domain.model

/**
 * 列表展示用的轻量模型（不依赖 Room）。
 * 仅保留 UI 与交互需要的字段。
 */
data class VaultSummary(
    val id: Int,
    val title: String,
    val category: String,
    val entryType: Int = 0,
    val username: String,
    val iconName: String? = null,
    val iconCustomPath: String? = null,
    val associatedAppPackage: String? = null,
    val associatedDomain: String? = null,
    val totpSecret: String? = null,
    val totpPeriod: Int = 30,
    val totpDigits: Int = 6,
    val totpAlgorithm: String = "SHA1",
    val favorite: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)
