package com.aozijx.passly.domain.model.core

import java.io.Serializable

/**
 * 领域模型：条目变更历史
 */
data class VaultHistory(
    val historyId: Int = 0,
    val entryId: Int,
    val fieldName: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val changeType: HistoryType = HistoryType.UPDATE,
    val deviceName: String? = null,
    val changedAt: Long = System.currentTimeMillis()
) : Serializable {
    enum class HistoryType(val value: Int) {
        UPDATE(0),   // 内容更新
        ACCESS(1),   // 访问/查看
        COPY(2),     // 复制
        AUTOFILL(3), // 自动填充
        CREATE(4)    // 初始创建
    }
}