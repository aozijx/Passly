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
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpAlgorithm
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpEncoding
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun otpRoundTripPreservesEveryTypeAlgorithmAndEncoding() {
        OtpType.entries.forEach { type ->
            OtpHashAlgorithm.entries.forEach { algorithm ->
                OtpSecretEncoding.entries.forEach { encoding ->
                    val config = OtpConfig(
                        type = type,
                        secret = "JBSWY3DPEHPK3PXP",
                        algorithm = algorithm,
                        digits = if (type == OtpType.STEAM) 5 else 6,
                        periodSeconds = if (type == OtpType.HOTP) null else 30,
                        counter = if (type == OtpType.HOTP) 7L else null,
                        encoding = encoding,
                    )
                    val entry = Entry(
                        identity = EntryIdentity(
                            id = EntryId("otp-${type.ordinal}-${algorithm.ordinal}-${encoding.ordinal}"),
                            type = EntryType.OTP,
                            timestamps = EntryTimestamps(1L),
                        ),
                        profile = EntryProfile(title = "OTP"),
                        secret = EntrySecret(credential = OtpCredential(config)),
                    )

                    val record = mapper.toRecord(entry)
                    val backupConfig = requireNotNull(record.secret.otp?.config)

                    assertEquals(type.toExpectedBackupType(), backupConfig.type)
                    assertEquals(algorithm.toExpectedBackupAlgorithm(), backupConfig.algorithm)
                    assertEquals(encoding.toExpectedBackupEncoding(), backupConfig.encoding)
                    assertEquals(entry, mapper.toEntry(record))
                }
            }
        }
    }

    @Test
    fun otpMappingsDoNotDependOnEnumNames() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val source = sourceRoot.resolve(
            "com/aozijx/passly/app/database/backup/RoomBackupSnapshotMapper.kt",
        ).readText()

        listOf(
            "BackupOtpType.valueOf",
            "BackupOtpAlgorithm.valueOf",
            "BackupOtpEncoding.valueOf",
            "OtpType.valueOf",
            "OtpHashAlgorithm.valueOf",
            "OtpSecretEncoding.valueOf",
        ).forEach { bridge -> assertFalse("Mapper still uses $bridge", source.contains(bridge)) }
    }

    private fun OtpType.toExpectedBackupType(): BackupOtpType = when (this) {
        OtpType.TOTP -> BackupOtpType.TOTP
        OtpType.HOTP -> BackupOtpType.HOTP
        OtpType.STEAM -> BackupOtpType.STEAM
    }

    private fun OtpHashAlgorithm.toExpectedBackupAlgorithm(): BackupOtpAlgorithm = when (this) {
        OtpHashAlgorithm.SHA1 -> BackupOtpAlgorithm.SHA1
        OtpHashAlgorithm.SHA256 -> BackupOtpAlgorithm.SHA256
        OtpHashAlgorithm.SHA512 -> BackupOtpAlgorithm.SHA512
    }

    private fun OtpSecretEncoding.toExpectedBackupEncoding(): BackupOtpEncoding = when (this) {
        OtpSecretEncoding.BASE32 -> BackupOtpEncoding.BASE32
        OtpSecretEncoding.BASE64 -> BackupOtpEncoding.BASE64
    }
}
