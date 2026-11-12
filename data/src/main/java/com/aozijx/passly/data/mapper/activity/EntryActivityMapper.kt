package com.aozijx.passly.data.mapper.activity

import com.aozijx.passly.data.codec.fieldKeyFromStorageKey
import com.aozijx.passly.data.codec.toStorageKey
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext

internal fun EntryActivityEntity.toDomain(): EntryActivity =
    EntryActivity(
        activityId = activityId,
        entryId = entryId,
        activityType = activityType,
        context =
            when {
                revisionVersion != null -> EntryActivityContext.ContentRevision(EntryVersion(revisionVersion))
                activityType.isUsage -> EntryActivityContext.Usage
                else -> EntryActivityContext.Lifecycle
            },
        createdAt = createdAt,
        affectedFields = affectedFieldKeys.map(::fieldKeyFromStorageKey).toSet(),
    )

internal fun EntryActivity.toEntity(): EntryActivityEntity =
    EntryActivityEntity(
        activityId = activityId,
        entryId = entryId,
        activityType = activityType,
        revisionVersion =
            when (val value = context) {
                is EntryActivityContext.ContentRevision -> value.version.value
                EntryActivityContext.Usage, EntryActivityContext.Lifecycle -> null
            },
        affectedFieldKeys = affectedFields.map(FieldKey::toStorageKey).toSet(),
        createdAt = createdAt,
    )
