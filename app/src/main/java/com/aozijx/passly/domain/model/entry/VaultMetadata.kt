package com.aozijx.passly.domain.model.entry

import kotlinx.serialization.Serializable

@Serializable
data class VaultMetadata(
    val entryId: String,
    val entryType: EntryType,
    val title: String,
    val username: String,
    val icon: String?,
    val iconCustomPath: String? = null,
    val website: WebsiteInfo? = null,
    val favorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val expiresAt: Long? = null,
    val lastUsedAt: Long? = null,
    val usageCount: Int = 0,

    /** 乐观锁版本号，每次更新自动递增。用于防冲突并发写。 */
    val entryVersion: Int = 0
)
