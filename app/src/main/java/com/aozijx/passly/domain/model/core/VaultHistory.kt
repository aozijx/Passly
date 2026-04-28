package com.aozijx.passly.domain.model.core

import java.io.Serializable

/**
 * 领域模型：条目变更历史（与存储实现解耦）
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
        ACCESS(1),   // 访问/查看敏感字段
        COPY(2),     // 复制操作
        AUTOFILL(3)  // 自动填充使用
    }
}