package com.aozijx.passly.domain.model.entry

import kotlinx.serialization.Serializable

@Serializable
data class VaultEntry(
    val metadata: VaultMetadata,
    val credential: VaultCredential,
    /** 乐观锁版本号，来自数据库实体，不存储在加密 JSON 中。 */
    val entryVersion: Int = 0,
    /** 创建时间，来自数据库实体，不存储在加密 JSON 中。 */
    val createdAt: Long = System.currentTimeMillis(),
    /** 最后更新时间，来自数据库实体，不存储在加密 JSON 中。 */
    val updatedAt: Long = System.currentTimeMillis(),
    /** 回收站时间，null 表示正常条目，来自数据库实体。 */
    val deletedAt: Long? = null
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
    val expiresAt: Long? get() = metadata.expiresAt
}
