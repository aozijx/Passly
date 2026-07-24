package com.aozijx.passly.domain.entry.model

/**
 * 条目更新变更集。
 *
 * 表示对 [VaultEntry] 中 [EntrySummary] 和/或 [EntrySecret] 的更新。
 * 字段为 null 表示该组件没有变更，写入时将保留数据库中的现有值。
 */
data class EntryChanges(
    val summary: EntrySummary? = null,
    val secret: EntrySecret? = null
) {
    val hasChanges: Boolean get() = summary != null || secret != null
}
