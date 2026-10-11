package com.aozijx.passly.data.codec

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.attachment.AttachmentStatus
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StorageEnumMappingsTest {
    @Test
    fun `all persisted enums round trip through stable keys`() {
        EntryType.entries.forEach { value ->
            assertEquals(value, entryTypeFromStorageKey(value.toStorageKey()))
        }
        ActivityType.entries.forEach { value ->
            assertEquals(value, activityTypeFromStorageKey(value.toStorageKey()))
        }
        EntryRelationType.entries.forEach { value ->
            assertEquals(value, entryRelationTypeFromStorageKey(value.toStorageKey()))
        }
        SensitiveFieldKey.entries.forEach { value ->
            assertEquals(value, sensitiveFieldKeyFromStorageKey(value.toStorageKey()))
        }
        AttachmentStatus.entries.forEach { value ->
            assertEquals(value, attachmentStatusFromStorageKey(value.toStorageKey()))
        }
        RevisionChange.entries.forEach { value ->
            assertEquals(value, revisionChangeFromStorageKey(value.toStorageKey()))
        }
        OtpType.entries.forEach { value ->
            assertEquals(value, otpTypeFromStorageKey(value.toStorageKey()))
        }
    }

    @Test
    fun `unknown persisted values fail instead of becoming a valid default`() {
        assertThrows(IllegalArgumentException::class.java) {
            activityTypeFromStorageKey("FUTURE_OR_CORRUPT")
        }
        assertThrows(IllegalArgumentException::class.java) {
            sensitiveFieldKeyFromStorageKey("FUTURE_OR_CORRUPT")
        }
        assertThrows(IllegalArgumentException::class.java) {
            otpTypeFromStorageKey("FUTURE_OR_CORRUPT")
        }
    }
}
