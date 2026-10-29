package com.aozijx.passly.domain.entry.model

import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryActivityTest {
    @Test
    fun `trash restore is a lifecycle mutation while historical restore creates content`() {
        assertTrue(ActivityType.RESTORE.isMutation)
        assertFalse(ActivityType.RESTORE.isContentChange)
        assertTrue(ActivityType.VERSION_RESTORED.isContentChange)
        assertFalse(ActivityType.VERSION_RESTORED.isUsage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `content event cannot omit its actual revision`() {
        EntryActivity(entryId = "entry", activityType = ActivityType.UPDATE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `usage event cannot claim to create a content revision`() {
        EntryActivity(entryId = "entry", activityType = ActivityType.AUTOFILL,
            context = EntryActivityContext.ContentRevision(EntryVersion(2)))
    }
}
