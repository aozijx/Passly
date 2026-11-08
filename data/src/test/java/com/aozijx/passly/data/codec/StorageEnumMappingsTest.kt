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
    private fun <T> assertMapping(
        values: List<T>,
        expected: Map<T, String>,
        encode: (T) -> String,
        decode: (String) -> T,
    ) {
        assertEquals(values.toSet(), expected.keys)
        assertEquals(expected.values.size, expected.values.toSet().size)
        assertEquals(expected, values.associateWith(encode))
        expected.forEach { (value, key) -> assertEquals(value, decode(key)) }
    }

    @Test
    fun `all persisted enums round trip through stable keys`() {
        assertMapping(
            EntryType.entries,
            mapOf(
                EntryType.ACCOUNT to "ACCOUNT",
                EntryType.LOGIN to "LOGIN",
                EntryType.NOTE to "NOTE",
                EntryType.BANK_CARD to "BANK_CARD",
                EntryType.ID_CARD to "ID_CARD",
                EntryType.PASSPORT to "PASSPORT",
                EntryType.DRIVER_LICENSE to "DRIVER_LICENSE",
                EntryType.SSH_KEY to "SSH_KEY",
                EntryType.WIFI to "WIFI",
                EntryType.PASSKEY to "PASSKEY",
                EntryType.OTP to "OTP",
                EntryType.DATABASE_CREDENTIAL to "DATABASE_CREDENTIAL",
                EntryType.SERVER_CREDENTIAL to "SERVER_CREDENTIAL",
                EntryType.API_KEY to "API_KEY",
                EntryType.CRYPTO_WALLET to "CRYPTO_WALLET",
                EntryType.SEED_PHRASE to "SEED_PHRASE",
                EntryType.RECOVERY_CODE to "RECOVERY_CODE",
            ),
            EntryType::toStorageKey,
            ::entryTypeFromStorageKey,
        )
        assertMapping(
            ActivityType.entries,
            mapOf(
                ActivityType.VIEW to "VIEW",
                ActivityType.COPY_USERNAME to "COPY_USERNAME",
                ActivityType.COPY_PASSWORD to "COPY_PASSWORD",
                ActivityType.AUTOFILL to "AUTOFILL",
                ActivityType.EXPORT to "EXPORT",
                ActivityType.IMPORT to "IMPORT",
                ActivityType.CREATE to "CREATE",
                ActivityType.UPDATE to "UPDATE",
                ActivityType.VERSION_RESTORED to "VERSION_RESTORED",
                ActivityType.DELETE to "DELETE",
                ActivityType.RESTORE to "RESTORE",
            ),
            ActivityType::toStorageKey,
            ::activityTypeFromStorageKey,
        )
        assertMapping(
            EntryRelationType.entries,
            mapOf(
                EntryRelationType.MEMBER_OF_ACCOUNT to "MEMBER_OF_ACCOUNT",
                EntryRelationType.OTP_FOR to "OTP_FOR",
                EntryRelationType.RECOVERY_FOR to "RECOVERY_FOR",
                EntryRelationType.RELATED_TO to "RELATED_TO",
            ),
            EntryRelationType::toStorageKey,
            ::entryRelationTypeFromStorageKey,
        )
        assertMapping(
            SensitiveFieldKey.entries,
            mapOf(
                SensitiveFieldKey.PASSWORD to "PASSWORD",
                SensitiveFieldKey.CARD_NUMBER to "CARD_NUMBER",
                SensitiveFieldKey.CARD_CVV to "CARD_CVV",
                SensitiveFieldKey.CARD_PAYMENT_PIN to "CARD_PAYMENT_PIN",
                SensitiveFieldKey.IDENTITY_NUMBER to "IDENTITY_NUMBER",
                SensitiveFieldKey.SEED_PHRASE to "SEED_PHRASE",
                SensitiveFieldKey.RECOVERY_CODES to "RECOVERY_CODES",
                SensitiveFieldKey.SSH_PRIVATE_KEY to "SSH_PRIVATE_KEY",
                SensitiveFieldKey.SSH_PASSPHRASE to "SSH_PASSPHRASE",
                SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE to "PASSKEY_PRIVATE_REFERENCE",
                SensitiveFieldKey.OTP_SECRET to "OTP_SECRET",
            ),
            SensitiveFieldKey::toStorageKey,
            ::sensitiveFieldKeyFromStorageKey,
        )
        assertMapping(
            RevisionChange.entries,
            mapOf(
                RevisionChange.VALUE_CHANGED to "VALUE_CHANGED",
                RevisionChange.VERSION_RESTORED to "VERSION_RESTORED",
            ),
            RevisionChange::toStorageKey,
            ::revisionChangeFromStorageKey,
        )
        assertMapping(
            OtpType.entries,
            mapOf(
                OtpType.TOTP to "TOTP",
                OtpType.HOTP to "HOTP",
                OtpType.STEAM to "STEAM",
            ),
            OtpType::toStorageKey,
            ::otpTypeFromStorageKey,
        )
        assertMapping(
            OtpHashAlgorithm.entries,
            mapOf(
                OtpHashAlgorithm.SHA1 to "SHA1",
                OtpHashAlgorithm.SHA256 to "SHA256",
                OtpHashAlgorithm.SHA512 to "SHA512",
            ),
            OtpHashAlgorithm::toStorageKey,
            ::otpHashAlgorithmFromStorageKey,
        )
        assertMapping(
            OtpSecretEncoding.entries,
            mapOf(
                OtpSecretEncoding.BASE32 to "BASE32",
                OtpSecretEncoding.BASE64 to "BASE64",
            ),
            OtpSecretEncoding::toStorageKey,
            ::otpSecretEncodingFromStorageKey,
        )
    }

    @Test
    fun `unknown persisted values fail instead of becoming a valid default`() {
        assertThrows(IllegalArgumentException::class.java) {
            entryTypeFromStorageKey("FUTURE_OR_CORRUPT")
        }
        assertThrows(IllegalArgumentException::class.java) {
            activityTypeFromStorageKey("FUTURE_OR_CORRUPT")
        }
        assertThrows(IllegalArgumentException::class.java) {
            entryRelationTypeFromStorageKey("FUTURE_OR_CORRUPT")
        }
        assertThrows(IllegalArgumentException::class.java) {
            sensitiveFieldKeyFromStorageKey("FUTURE_OR_CORRUPT")
        }
        assertThrows(IllegalArgumentException::class.java) {
            revisionChangeFromStorageKey("FUTURE_OR_CORRUPT")
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
