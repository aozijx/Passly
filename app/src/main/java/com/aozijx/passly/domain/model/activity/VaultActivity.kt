package com.aozijx.passly.domain.model.activity

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
    val activityId: String = "",
    val entryId: String,
    val activityType: ActivityType,
    val source: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
