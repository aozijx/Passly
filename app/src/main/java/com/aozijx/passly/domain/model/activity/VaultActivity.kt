package com.aozijx.passly.domain.model.activity

import com.github.f4b6a3.uuid.UuidCreator

enum class ActivityType {
    VIEW,
    COPY_USERNAME,
    COPY_PASSWORD,
    AUTOFILL,
    EXPORT,
    IMPORT,
    CREATE,
    UPDATE,
    DELETE,
    RESTORE
}

data class VaultActivity(
    val activityId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val entryId: String,
    val activityType: ActivityType,
    val source: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
