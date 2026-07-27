package com.aozijx.passly.data.mapper.activity

import com.aozijx.passly.data.model.entity.EntryActivityEntity
import com.aozijx.passly.domain.entry.model.activity.EntryActivity

fun EntryActivityEntity.toDomain(): EntryActivity = EntryActivity(
    activityId = activityId,
    entryId = entryId,
    activityType = activityType,
    source = source,
    createdAt = createdAt
)

fun EntryActivity.toEntity(): EntryActivityEntity = EntryActivityEntity(
    activityId = activityId,
    entryId = entryId,
    activityType = activityType,
    source = source,
    createdAt = createdAt
)
