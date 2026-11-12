package com.aozijx.passly.data.codec

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
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
                ActivityType.REVEAL_FIELDS to "REVEAL_FIELDS",
                ActivityType.COPY_FIELD to "COPY_FIELD",
                ActivityType.COPY_OTP_CODE to "COPY_OTP_CODE",
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
            FieldKey.entries,
            mapOf(
                FieldKey.TITLE to "TITLE",
                FieldKey.USERNAME to "USERNAME",
                FieldKey.EMAIL to "EMAIL",
                FieldKey.PASSWORD to "PASSWORD",
                FieldKey.NOTES to "NOTES",
                FieldKey.TAGS to "TAGS",
                FieldKey.PRIMARY_URL to "PRIMARY_URL",
                FieldKey.DOMAINS to "DOMAINS",
                FieldKey.APPLICATION_IDS to "APPLICATION_IDS",
                FieldKey.EXPIRES_AT to "EXPIRES_AT",
                FieldKey.FAVORITE to "FAVORITE",
                FieldKey.CUSTOM_FIELDS to "CUSTOM_FIELDS",
                FieldKey.OTP_TYPE to "OTP_TYPE",
                FieldKey.OTP_SECRET to "OTP_SECRET",
                FieldKey.OTP_ISSUER to "OTP_ISSUER",
                FieldKey.OTP_ACCOUNT_NAME to "OTP_ACCOUNT_NAME",
                FieldKey.OTP_PERIOD to "OTP_PERIOD",
                FieldKey.OTP_COUNTER to "OTP_COUNTER",
                FieldKey.OTP_DIGITS to "OTP_DIGITS",
                FieldKey.OTP_ALGORITHM to "OTP_ALGORITHM",
                FieldKey.OTP_ENCODING to "OTP_ENCODING",
                FieldKey.PASSKEY_CREDENTIAL_ID to "PASSKEY_CREDENTIAL_ID",
                FieldKey.PASSKEY_RELYING_PARTY_ID to "PASSKEY_RELYING_PARTY_ID",
                FieldKey.PASSKEY_USER_HANDLE to "PASSKEY_USER_HANDLE",
                FieldKey.PASSKEY_DATA to "PASSKEY_DATA",
                FieldKey.RECOVERY_CODES to "RECOVERY_CODES",
                FieldKey.HARDWARE_INFO to "HARDWARE_INFO",
                FieldKey.SSH_KEY to "SSH_KEY",
                FieldKey.SSH_PUBLIC_KEY to "SSH_PUBLIC_KEY",
                FieldKey.SSH_PASSPHRASE to "SSH_PASSPHRASE",
                FieldKey.SEED_PHRASE to "SEED_PHRASE",
                FieldKey.CARD_TYPE to "CARD_TYPE",
                FieldKey.CARD_NUMBER to "CARD_NUMBER",
                FieldKey.CARD_EXPIRATION to "CARD_EXPIRATION",
                FieldKey.CARD_CVV to "CARD_CVV",
                FieldKey.CARD_HOLDER to "CARD_HOLDER",
                FieldKey.CARD_ISSUING_BANK to "CARD_ISSUING_BANK",
                FieldKey.CARD_BANK_BRANCH to "CARD_BANK_BRANCH",
                FieldKey.CARD_PRODUCT to "CARD_PRODUCT",
                FieldKey.CARD_CURRENCY to "CARD_CURRENCY",
                FieldKey.CARD_BANK_PHONE to "CARD_BANK_PHONE",
                FieldKey.PAYMENT_PIN to "PAYMENT_PIN",
                FieldKey.PAYMENT_PLATFORM to "PAYMENT_PLATFORM",
                FieldKey.BILLING_ADDRESS to "BILLING_ADDRESS",
                FieldKey.SECURITY_QUESTION to "SECURITY_QUESTION",
                FieldKey.SECURITY_ANSWER to "SECURITY_ANSWER",
                FieldKey.ID_NUMBER to "ID_NUMBER",
                FieldKey.WIFI_SSID to "WIFI_SSID",
                FieldKey.WIFI_SECURITY to "WIFI_SECURITY",
                FieldKey.WIFI_HIDDEN to "WIFI_HIDDEN",
            ),
            FieldKey::toStorageKey,
            ::fieldKeyFromStorageKey,
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
            fieldKeyFromStorageKey("FUTURE_OR_CORRUPT")
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
