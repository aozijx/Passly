package com.aozijx.passly.data.local.database.model

import androidx.room.Embedded
import com.aozijx.passly.data.local.database.entity.EntryEntity

data class EntryPagingRow(
    @Embedded val entry: EntryEntity,
    val usageCount: Int,
    val lastUsedAt: Long?,
    val accountId: String?,
)
