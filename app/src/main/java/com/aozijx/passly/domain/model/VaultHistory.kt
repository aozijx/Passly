package com.aozijx.passly.domain.model

import java.io.Serializable

/**
 * 领域模型：条目变更历史（与存储实现解耦）
 */
data class VaultHistory(
    val historyId: Int = 0,
    val entryId: Int,
    val fieldName: String,
    val oldValue: String?,
    val newValue: String?,
    val changeType: Int = 0,
    val deviceName: String? = null,
    val changedAt: Long = System.currentTimeMillis()
) : Serializable
