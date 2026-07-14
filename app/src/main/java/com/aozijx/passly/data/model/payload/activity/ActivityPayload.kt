package com.aozijx.passly.data.model.payload.activity

import kotlinx.serialization.Serializable

@Serializable
data class ActivityPayload(
    val activityId: String,
    val entryId: String,
    val activityType: String,
    val source: String? = null,
    val createdAt: Long
)