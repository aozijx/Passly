package com.aozijx.passly.domain.entry.model.activity

import com.github.f4b6a3.uuid.UuidCreator
import com.aozijx.passly.domain.entry.model.EntryVersion

sealed interface EntryActivityContext {
    data object Usage : EntryActivityContext
    data object Lifecycle : EntryActivityContext
    data class ContentRevision(val version: EntryVersion) : EntryActivityContext
}

data class EntryActivity(
    val activityId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val entryId: String,
    val activityType: ActivityType,
    val context: EntryActivityContext = if (activityType.isUsage) EntryActivityContext.Usage else EntryActivityContext.Lifecycle,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(when (context) {
            EntryActivityContext.Usage -> activityType.isUsage
            EntryActivityContext.Lifecycle -> activityType.isMutation && !activityType.isContentChange
            is EntryActivityContext.ContentRevision -> activityType.isContentChange
        }) { "Activity context must match its event type" }
    }
}
