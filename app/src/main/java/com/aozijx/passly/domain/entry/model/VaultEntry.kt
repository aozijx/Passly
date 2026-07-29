package com.aozijx.passly.domain.entry.model

/**
 * 保险库条目的完整聚合。
 *
 * 包含三层结构：
 * - [header]：身份标识、类型、版本、时间戳（来自数据库实体）
 * - [summary]：展示摘要（标题、用户名、网站、图标、收藏、标签）
 * - [secret]：类型相关的敏感凭据数据
 */
data class VaultEntry(
    val header: EntryHeader,
    val summary: EntrySummary,
    val secret: EntrySecret
) : VaultIconable {
    override val entryType: EntryType get() = header.entryType
    override val iconName: String? get() = summary.icon
    override val iconCustomPath: String? get() = summary.iconCustomPath
    override val associatedAppPackage: String? get() = summary.website?.packageNames?.firstOrNull()
    override val associatedDomain: String? get() = summary.website?.primaryUrl

    // --- 便捷委托 ---
    val id: String get() = header.entryId
    val title: String get() = summary.title
    val username: String get() = summary.username
    val vaultId: String get() = header.vaultId
    val parentEntryId: String? get() = header.parentEntryId
    val favorite: Boolean get() = summary.favorite
    val tags: List<String> get() = summary.tags
    val website: WebsiteInfo? get() = summary.website
    val expiresAt: Long? get() = summary.expiresAt
    val createdAt: Long get() = header.createdAt
    val updatedAt: Long get() = header.updatedAt
    val deletedAt: Long? get() = header.deletedAt
    val entryVersion: Int get() = header.version.value
}
