package com.aozijx.passly.data.mapper.activity

import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import com.aozijx.passly.domain.entry.model.EntryVersion

fun EntryActivityEntity.toDomain(): EntryActivity = EntryActivity(
    activityId = activityId,
    entryId = entryId,
    activityType = activityType,
    context = when {
        revisionVersion != null -> EntryActivityContext.ContentRevision(EntryVersion(revisionVersion))
        activityType.isUsage -> EntryActivityContext.Usage
        else -> EntryActivityContext.Lifecycle
    },
    createdAt = createdAt
)

fun EntryActivity.toEntity(): EntryActivityEntity = EntryActivityEntity(
    activityId = activityId,
    entryId = entryId,
    activityType = activityType,
    revisionVersion = when (val value = context) {
        is EntryActivityContext.ContentRevision -> value.version.value
        EntryActivityContext.Usage, EntryActivityContext.Lifecycle -> null
    },
    createdAt = createdAt
)
