package com.aozijx.passly.app.database.backup

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
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomBackupSnapshotMapperTest {
    private val mapper = RoomBackupSnapshotMapper()

    @Test
    fun bankCardRoundTripPreservesEveryBusinessField() {
        val entry = Entry(
            identity = EntryIdentity(
                id = EntryId("card-1"),
                type = EntryType.BANK_CARD,
                version = EntryVersion(4),
                timestamps = EntryTimestamps(10L, 20L, 30L),
            ),
            profile = EntryProfile(
                title = "Primary card",
                username = "owner",
                favorite = true,
                tags = setOf("finance"),
                expiresAtMs = 40L,
            ),
            secret = EntrySecret(
                credential = CardCredential(
                    cardType = "credit",
                    cardNumber = "4111111111111111",
                    cardExpiry = "12/30",
                    cardCvv = "123",
                    cardHolder = "Ada Lovelace",
                    paymentPin = "9876",
                    paymentPlatform = "visa",
                    billingAddress = "1 Example Road",
                ),
                notes = "travel",
                customFields = listOf(
                    CustomField("support", "secret", CustomFieldKind.HIDDEN),
                ),
            ),
        )

        assertEquals(entry, mapper.toEntry(mapper.toRecord(entry)))
    }

    @Test
    fun otpRoundTripPreservesSeparatedSecretAndConfiguration() {
        val entry = Entry(
            identity = EntryIdentity(
                id = EntryId("otp-1"),
                type = EntryType.OTP,
                version = EntryVersion(2),
                timestamps = EntryTimestamps(100L, 200L),
            ),
            profile = EntryProfile(title = "Production OTP", username = "alice"),
            secret = EntrySecret(
                credential = OtpCredential(
                    OtpConfig(
                        type = OtpType.TOTP,
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

        assertNull(record.secret.otp?.config?.secret)
        assertEquals(
            "JBSWY3DPEHPK3PXP",
            record.sensitiveFields.single { it.key == SensitiveFieldKey.OTP_SECRET.name }.value,
        )
        assertEquals(entry, mapper.toEntry(record))
    }
}
