package com.aozijx.passly.data.codec

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
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
        RevisionChange.entries.forEach { value ->
            assertEquals(value, revisionChangeFromStorageKey(value.toStorageKey()))
        }
        OtpType.entries.forEach { value ->
            assertEquals(value, otpTypeFromStorageKey(value.toStorageKey()))
        }
        OtpHashAlgorithm.entries.forEach { value ->
            assertEquals(value, otpHashAlgorithmFromStorageKey(value.toStorageKey()))
        }
        OtpSecretEncoding.entries.forEach { value ->
            assertEquals(value, otpSecretEncodingFromStorageKey(value.toStorageKey()))
        }
        assertEquals("TOTP", OtpType.TOTP.toStorageKey())
        assertEquals("SHA1", OtpHashAlgorithm.SHA1.toStorageKey())
        assertEquals("BASE32", OtpSecretEncoding.BASE32.toStorageKey())
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
        assertThrows(IllegalArgumentException::class.java) {
            otpHashAlgorithmFromStorageKey("FUTURE_OR_CORRUPT")
        }
        assertThrows(IllegalArgumentException::class.java) {
            otpSecretEncodingFromStorageKey("FUTURE_OR_CORRUPT")
        }
    }
}
