package com.aozijx.passly.data.mapper.history

import com.aozijx.passly.data.model.entity.VaultHistoryEntity
import com.aozijx.passly.domain.model.VaultHistory

fun VaultHistoryEntity.toDomain(): VaultHistory = VaultHistory(
    historyId = historyId,
    entryId = entryId,
    version = version,
    createdAt = createdAt
)
