package com.aozijx.passly.domain.model.entry

/**
 * 条目更新变更集。
 *
 * 表示对 [VaultEntry] 中 [VaultMetadata] 和/或 [VaultCredential] 的更新。
 * 字段为 null 表示该组件没有变更，写入时将保留数据库中的现有值。
 */
data class EntryChanges(
    val metadata: VaultMetadata? = null,
    val credential: VaultCredential? = null
) {
    val hasChanges: Boolean get() = metadata != null || credential != null
}
