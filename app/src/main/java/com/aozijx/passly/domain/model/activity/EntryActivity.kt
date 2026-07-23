package com.aozijx.passly.domain.model.activity

import com.github.f4b6a3.uuid.UuidCreator

data class EntryActivity(
    val activityId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val entryId: String,
    val activityType: ActivityType,
    val source: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
