package com.aozijx.passly.domain.model.entry

import kotlinx.serialization.Serializable

@Serializable
data class VaultEntry(
    val metadata: VaultMetadata,
    val credential: VaultCredential
) : VaultIconable {
    override val category: String get() = metadata.entryType.name
    override val iconName: String? = metadata.icon
    override val iconCustomPath: String? get() = metadata.iconCustomPath
    override val associatedAppPackage: String? = metadata.website?.packageNames?.firstOrNull()
    override val associatedDomain: String? = metadata.website?.primaryUrl

    // --- 聚合入口 ---
    val website: WebsiteInfo? get() = metadata.website

    // --- Metadata 委托（Hot） ---
    val id: String get() = metadata.entryId
    val title: String get() = metadata.title
    val username: String get() = metadata.username
    val entryType: EntryType get() = metadata.entryType
    val favorite: Boolean get() = metadata.favorite
    val tags: List<String> get() = metadata.tags
    val createdAt: Long get() = metadata.createdAt
    val updatedAt: Long get() = metadata.updatedAt
    val deletedAt: Long? get() = metadata.deletedAt
    val expiresAt: Long? get() = metadata.expiresAt
    val lastUsedAt: Long? get() = metadata.lastUsedAt
    val usageCount: Int get() = metadata.usageCount
}
