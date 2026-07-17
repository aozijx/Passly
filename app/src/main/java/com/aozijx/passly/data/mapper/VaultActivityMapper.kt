package com.aozijx.passly.data.mapper

import com.aozijx.passly.data.model.entity.VaultActivityEntity
import com.aozijx.passly.domain.model.activity.VaultActivity

fun VaultActivityEntity.toDomain(): VaultActivity = VaultActivity(
    activityId = activityId,
    entryId = entryId,
    activityType = activityType,
    source = source,
    createdAt = createdAt
)

fun VaultActivity.toEntity(): VaultActivityEntity = VaultActivityEntity(
    activityId = activityId,
    entryId = entryId,
    activityType = activityType,
    source = source,
    createdAt = createdAt
)
