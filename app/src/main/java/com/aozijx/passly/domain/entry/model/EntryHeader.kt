package com.aozijx.passly.domain.entry.model

data class EntryHeader(
    val id: EntryId,
    val entryType: EntryType,
    val version: EntryVersion,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    /** Logical vault ownership. This is not an account grouping key. */
    val vaultId: String = DEFAULT_VAULT_ID,
    /** Optional ACCOUNT entry that owns this independently stored credential. */
    val parentEntryId: String? = null
) {
    /** 条目的唯一字符串标识（快捷访问）。 */
    val entryId: String get() = id.value

    companion object {
        /** 版本号的唯一真相源是数据库实体，不存储在加密 JSON 中。 */
        const val VERSION_SOURCE = "database"
        const val DEFAULT_VAULT_ID = "default"
    }
}
