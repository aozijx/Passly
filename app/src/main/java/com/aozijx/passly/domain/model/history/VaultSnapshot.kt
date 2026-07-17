package com.aozijx.passly.domain.model.history

import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * 历史快照 —— 保存每个版本完整的 VaultEntry。
 *
 * 数据库存的是整个 Entry 的加密快照，不是逐字段 diff。
 * 如需按字段展示变更历史，应另建 VaultActivity / VaultChange。
 */
data class VaultSnapshot(
    val snapshotId: String,
    val entryId: String,
    val version: Int,
    val createdAt: Long,
    val changeType: SnapshotType,
    val entry: VaultEntry? = null
)

enum class SnapshotType(val value: String) {
    VALUE_CHANGED("value_changed"),
    VERSION_RESTORED("version_restored");

    companion object {
        fun fromValue(value: String): SnapshotType =
            entries.find { it.value == value } ?: VALUE_CHANGED
    }
}
