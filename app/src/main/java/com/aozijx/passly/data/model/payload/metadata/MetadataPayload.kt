package com.aozijx.passly.data.model.payload.metadata

import kotlinx.serialization.Serializable

/**
 * 非敏感元数据 Payload —— 列表展示/搜索/自动填充只需解密此 Blob。
 *
 * 对应表: vault_metadata
 *
 * 热数据模型（Hot Data）：列表滚动、搜索过滤、自动填充匹配仅解密此 Blob。
 * 与 [CredentialPayload] 分离 —— 查看/编辑凭据内容时额外解密 credentialBlob。
 */
@Serializable
data class MetadataPayload(
    val title: String,
    val category: String,
    val iconName: String? = null,
    val iconCustomPath: String? = null,

    val associatedAppPackage: String? = null,
    val associatedDomain: String? = null,
    val uriList: List<String>? = null,
    val matchType: Int = 0,
    val autoSubmit: Boolean = false,

    val favorite: Boolean = false,
    val tags: List<String>? = null,

    val lastUsedAt: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val expiresAt: Long? = null
)
