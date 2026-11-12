package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationMaterial
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerator
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.domain.entry.otp.generation.toGenerationConfig
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.mapper.KeyedEntryArchiveMapper
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OtpArchiveValidationTest {
    private val mapper = KeyedEntryArchiveMapper

    private fun generateOtp(
        config: OtpConfig,
        timestamp: Long,
    ): OtpResult {
        val material = OtpGenerationMaterial(config.toGenerationConfig(), config.secret?.toByteArray() ?: byteArrayOf())
        return material.use { OtpGenerator.generate(it, timestamp = timestamp) }
    }

    @Test
    fun validLongPeriodCanBeBackedUpRestoredAndGenerateTheSameCode() {
        val record = otp("long-period", field("otp_period", BackupFieldValue.integer(600)))
        BackupBundleValidator.validate(bundle(record), requireResourceData = false)
        val config =
            requireNotNull(
                mapper
                    .toEntry(record)
                    .secret.otp
                    ?.config,
            )

        assertEquals(600, (config as OtpConfig.Totp).periodSeconds)
        assertEquals(
            generateOtp(OtpConfig.Totp(secret = SECRET, periodSeconds = 600), 1234567),
            generateOtp(config, 1234567),
        )
    }

    @Test
    fun optionalTimedDefaultsAreIdenticalInValidationAndRestoration() {
        val record = otp("defaults")
        BackupBundleValidator.validate(bundle(record), requireResourceData = false)
        val config =
            requireNotNull(
                mapper
                    .toEntry(record)
                    .secret.otp
                    ?.config,
            )

        assertEquals(OtpConfig.Totp(secret = SECRET), config)

        val steam = otp("steam-defaults", field("otp_type", BackupFieldValue.text("steam")))
        BackupBundleValidator.validate(bundle(steam), requireResourceData = false)
        assertEquals(
            OtpConfig.Steam(secret = SECRET),
            mapper
                .toEntry(steam)
                .secret.otp
                ?.config,
        )
    }

    @Test
    fun strictImportRejectsOtpConfigurationsThatCannotBeUsed() {
        for (record in invalidRecords()) {
            assertThrows(IllegalArgumentException::class.java) {
                BackupImportPlanner.plan(bundle(record), BackupImportStrategy.STRICT)
            }
        }
    }

    @Test
    fun compatibleImportQuarantinesInvalidOtpBeforeRestoration() {
        val known =
            BackupEntryRecord(
                id = "keep-note",
                type = "note",
                revision = 1,
                createdAt = 1,
                updatedAt = 1,
                fields = listOf(field("title", BackupFieldValue.text("Keep"))),
            )
        val invalid = invalidRecords()
        val plan = BackupImportPlanner.plan(bundle(known, *invalid.toTypedArray()), BackupImportStrategy.COMPATIBLE)

        assertEquals(listOf(known), plan.bundle.document.entries)
        assertEquals(invalid.size, plan.summary.skippedEntryCount)
    }

    @Test
    fun nativeRestoreKeepsValidSecretRepresentationAndIdentityMetadata() {
        val record =
            otp("preserved").copy(
                fields =
                    listOf(
                        field("title", BackupFieldValue.text("OTP")),
                        field("otp_secret", BackupFieldValue.text("my======")),
                        field("otp_period", BackupFieldValue.integer(30)),
                        field("otp_issuer", BackupFieldValue.text(" Bank ")),
                        field("otp_account_name", BackupFieldValue.text(" alice ")),
                    ),
            )
        BackupBundleValidator.validate(bundle(record), requireResourceData = false)
        val config =
            requireNotNull(
                mapper
                    .toEntry(record)
                    .secret.otp
                    ?.config,
            )

        assertEquals("my======", config.secret)
        assertEquals(" Bank ", config.issuer)
        assertEquals(" alice ", config.accountName)
    }

    private fun invalidRecords(): List<BackupEntryRecord> =
        listOf(
            otp("digits-5", field("otp_digits", BackupFieldValue.integer(5))),
            otp("digits-9", field("otp_digits", BackupFieldValue.integer(9))),
            otp("digits-10", field("otp_digits", BackupFieldValue.integer(10))),
            otp("algorithm", field("otp_algorithm", BackupFieldValue.text("unknown"))),
            otp("encoding", field("otp_encoding", BackupFieldValue.text("unknown"))),
            otp("bad-secret").copy(
                fields =
                    listOf(
                        field("title", BackupFieldValue.text("OTP")),
                        field("otp_secret", BackupFieldValue.text("INVALID!")),
                    ),
            ),
            otp("steam-digits", field("otp_type", BackupFieldValue.text("steam")), field("otp_digits", BackupFieldValue.integer(6))),
            otp(
                "steam-algorithm",
                field("otp_type", BackupFieldValue.text("steam")),
                field("otp_digits", BackupFieldValue.integer(5)),
                field("otp_algorithm", BackupFieldValue.text("sha256")),
            ),
            otp(
                "steam-period",
                field("otp_type", BackupFieldValue.text("steam")),
                field("otp_digits", BackupFieldValue.integer(5)),
                field("otp_period", BackupFieldValue.integer(60)),
            ),
        )

    private fun otp(
        id: String,
        vararg fields: BackupFieldRecord,
    ) = BackupEntryRecord(
        id = id,
        type = "otp",
        revision = 1,
        createdAt = 1,
        updatedAt = 1,
        fields = listOf(field("title", BackupFieldValue.text("OTP")), field("otp_secret", BackupFieldValue.text(SECRET))) + fields,
    )

    private fun field(
        key: String,
        value: BackupFieldValue,
    ) = BackupFieldRecord(key, value)

    private fun bundle(vararg entries: BackupEntryRecord) =
        BackupBundle(
            BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = 1,
                entries = entries.toList(),
            ),
        )

    private companion object {
        const val SECRET = "JBSWY3DPEHPK3PXP"
    }
}
