package com.aozijx.passly.domain.model.entry

data class EntryHeader(
    val id: EntryId,
    val entryType: EntryType,
    val version: EntryVersion,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
) {
    /** 条目的唯一字符串标识（快捷访问）。 */
    val entryId: String get() = id.value

    companion object {
        /** 版本号的唯一真相源是数据库实体，不存储在加密 JSON 中。 */
        const val VERSION_SOURCE = "database"
    }
}
