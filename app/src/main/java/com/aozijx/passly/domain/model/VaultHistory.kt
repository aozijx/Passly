package com.aozijx.passly.domain.model

import java.io.Serializable

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
        UPDATE(0),
        ACCESS(1),
        COPY(2),
        AUTOFILL(3),
        CREATE(4)
    }
}