package com.aozijx.passly.feature.backup.internal.archive.mapper

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.CustomField
import com.aozijx.passly.domain.entry.model.credential.CustomFieldKind
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.SteamDefinition
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyedEntryArchiveRoundTripTest {
    private val mapper = KeyedEntryArchiveMapper

    @Test
    fun bankCardRoundTripPreservesBusinessFieldsAndStableWireKeys() {
        val entry =
            Entry(
                identity =
                    EntryIdentity(
                        id = EntryId("card-1"),
                        type = EntryType.BANK_CARD,
                        version = EntryVersion(4),
                        timestamps = EntryTimestamps(10L, 20L, 30L),
                    ),
                profile =
                    EntryProfile(
                        title = "Primary card",
                        username = "owner",
                        favorite = true,
                        tags = setOf("finance"),
                        expiresAtMs = 40L,
                    ),
                secret =
                    EntrySecret(
                        credential =
                            CardCredential(
                                cardType = "credit",
                                cardNumber = "4111111111111111",
                                cardExpiry = "12/30",
                                cardCvv = "123",
                                cardHolder = "Ada Lovelace",
                                paymentPin = "9876",
                                paymentPlatform = "visa",
                                billingAddress = "1 Example Road",
                                issuingBank = "Example Bank",
                                bankBranch = "Central Branch",
                                cardProduct = "Travel Platinum",
                                currency = "CNY / USD",
                                bankPhone = "+86 123456",
                            ),
                        notes = "travel",
                        customFields =
                            listOf(
                                CustomField("support", "secret", CustomFieldKind.HIDDEN),
                            ),
                    ),
            )

        val record = mapper.toRecord(entry)
        val serialized = BackupJson.encodeToString(record)
        val decoded = BackupJson.decodeFromString<BackupEntryRecord>(serialized)

        assertEquals("bank_card", record.type)
        assertEquals(4, record.revision)
        assertEquals(
            "4111111111111111",
            record.fields
                .single { it.key == "card_number" }
                .value.text,
        )
        assertEquals(
            true,
            record.fields
                .single { it.key == "favorite" }
                .value.booleanValue,
        )
        assertEquals(
            "Example Bank",
            record.fields
                .single { it.key == "card_issuing_bank" }
                .value.text,
        )
        assertEquals(
            "Central Branch",
            record.fields
                .single { it.key == "card_bank_branch" }
                .value.text,
        )
        assertEquals(
            "Travel Platinum",
            record.fields
                .single { it.key == "card_product" }
                .value.text,
        )
        assertEquals(
            "CNY / USD",
            record.fields
                .single { it.key == "card_currency" }
                .value.text,
        )
        assertEquals(
            "+86 123456",
            record.fields
                .single { it.key == "card_bank_phone" }
                .value.text,
        )
        assertEquals(entry, mapper.toEntry(decoded))
    }

    @Test
    fun otpRoundTripPreservesSeparatedSecretAndConfiguration() {
        val entry =
            Entry(
                identity =
                    EntryIdentity(
                        id = EntryId("otp-1"),
                        type = EntryType.OTP,
                        version = EntryVersion(2),
                        timestamps = EntryTimestamps(100L, 200L),
                    ),
                profile = EntryProfile(title = "Production OTP", username = "alice"),
                secret =
                    EntrySecret(
                        credential =
                            OtpCredential(
                                OtpConfig.Totp(
                                    secret = "JBSWY3DPEHPK3PXP",
                                    algorithm = OtpHashAlgorithm.SHA256,
                                    digits = 8,
                                    periodSeconds = 60,
                                    encoding = OtpSecretEncoding.BASE32,
                                    issuer = "Example",
                                    accountName = "alice@example.com",
                                ),
                            ),
                        notes = "primary token",
                    ),
            )

        val record = mapper.toRecord(entry)

        assertEquals(
            "JBSWY3DPEHPK3PXP",
            record.fields
                .single { it.key == "otp_secret" }
                .value.text,
        )
        assertEquals(entry, mapper.toEntry(record))
    }

    @Test
    fun otpRoundTripPreservesEveryTypeAlgorithmAndEncoding() {
        val configs =
            buildList {
                OtpHashAlgorithm.entries.forEach { algorithm ->
                    OtpSecretEncoding.entries.forEach { encoding ->
                        add(OtpConfig.Totp(secret = "JBSWY3DPEHPK3PXP", algorithm = algorithm, encoding = encoding))
                        add(OtpConfig.Hotp(secret = "JBSWY3DPEHPK3PXP", algorithm = algorithm, counter = 7L, encoding = encoding))
                        if (algorithm == OtpHashAlgorithm.SHA1) {
                            add(SteamDefinition.configuration(secret = "JBSWY3DPEHPK3PXP", encoding = encoding))
                        }
                    }
                }
            }
        configs.forEachIndexed { index, config ->
            val entry =
                Entry(
                    identity =
                        EntryIdentity(
                            id = EntryId("otp-$index"),
                            type = EntryType.OTP,
                            timestamps = EntryTimestamps(1L),
                        ),
                    profile = EntryProfile(title = "OTP"),
                    secret = EntrySecret(credential = OtpCredential(config)),
                )

            val record = mapper.toRecord(entry)
            assertEquals(config.type.toArchiveKey(), record.fieldText("otp_type"))
            val algorithm =
                when (config) {
                    is OtpConfig.Totp -> config.algorithm
                    is OtpConfig.Hotp -> config.algorithm
                }
            assertEquals(algorithm.toArchiveKey(), record.fieldText("otp_algorithm"))
            assertEquals(config.encoding.toArchiveKey(), record.fieldText("otp_encoding"))
            assertEquals(entry, mapper.toEntry(record))
        }
    }

    private fun com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord.fieldText(key: String): String? =
        fields.single { it.key == key }.value.text

    private fun OtpType.toArchiveKey(): String =
        when (this) {
            OtpType.TOTP -> "totp"
            OtpType.HOTP -> "hotp"
            OtpType.STEAM -> "steam"
        }

    private fun OtpHashAlgorithm.toArchiveKey(): String =
        when (this) {
            OtpHashAlgorithm.SHA1 -> "sha1"
            OtpHashAlgorithm.SHA256 -> "sha256"
            OtpHashAlgorithm.SHA512 -> "sha512"
        }

    private fun OtpSecretEncoding.toArchiveKey(): String =
        when (this) {
            OtpSecretEncoding.BASE32 -> "base32"
            OtpSecretEncoding.BASE64 -> "base64"
        }
}
