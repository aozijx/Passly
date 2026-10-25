package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.entry.otp.OtpUriParseResult
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportOtpQrAuthorizationTest {
    @Test
    fun authenticatesBeforeRevealingSecretAndBuildingQrPayload() = runTest {
        val events = mutableListOf<String>()
        val revealed = OwnedChars.fromString("JBSWY3DPEHPK3PXP")
        val exporter = exporter(events, revealed)

        val result = exporter(otpEntry())
        assertTrue(result is EntryQrExportResult.Ready)
        result as EntryQrExportResult.Ready
        val uri = try {
            result.content.toPlainString()
        } finally {
            result.content.wipe()
        }

        assertEquals(EntryQrKind.Otp, result.kind)
        assertEquals(listOf("authenticate", "reveal", "record"), events)
        assertTrue(uri.startsWith("otpauth://totp/"))
        assertTrue(uri.contains("secret=JBSWY3DPEHPK3PXP"))
        assertTrue(revealed.isEmpty)
        val parsed = OtpAuthUriCodec.parse(uri) as OtpUriParseResult.Parsed
        assertEquals("Issuer", parsed.config.issuer)
        assertEquals("alice", parsed.config.accountName)
    }

    @Test
    fun invalidSecretIsWipedWithoutProducingQrOrRecordingUsage() = runTest {
        val events = mutableListOf<String>()
        val revealed = OwnedChars.fromString("MZ")

        assertEquals(EntryQrExportResult.Unavailable, exporter(events, revealed)(otpEntry()))
        assertEquals(listOf("authenticate", "reveal"), events)
        assertTrue(revealed.isEmpty)
    }

    @Test
    fun base64SecretIsExportedAsEquivalentBase32AfterAuthorization() = runTest {
        val events = mutableListOf<String>()
        val revealed = OwnedChars.fromString("MTIzNDU2Nzg5MDEyMzQ1Njc4OTA=")
        val result = exporter(events, revealed)(otpEntry(OtpSecretEncoding.BASE64)) as EntryQrExportResult.Ready

        try {
            val parsed = OtpAuthUriCodec.parse(result.content.toPlainString()) as OtpUriParseResult.Parsed
            assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", parsed.config.secret)
            assertEquals(OtpSecretEncoding.BASE32, parsed.config.encoding)
            assertTrue(revealed.isEmpty)
        } finally {
            result.content.wipe()
        }
    }

    private fun exporter(events: MutableList<String>, revealed: OwnedChars) = ExportEntryQrUseCase(
        authorizationGate = RecordingAuthorizationGate(events),
        sensitiveFieldRepository = RecordingSensitiveFieldRepository(events, revealed),
        activityRecorder = RecordingActivityRecorder(events),
        logger = testLogger(),
    )

    private class RecordingAuthorizationGate(
        private val events: MutableList<String>,
    ) : AuthorizationGate {
        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            events += "authenticate"
            return AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
        }
    }

    private class RecordingSensitiveFieldRepository(
        private val events: MutableList<String>,
        private val revealed: OwnedChars,
    ) : SensitiveFieldRepository {
        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField {
            events += "reveal"
            return RevealedSensitiveField(entryId, key, revealed)
        }

        override suspend fun getPresence(entryId: EntryId) =
            SensitiveFieldPresence(entryId, setOf(SensitiveFieldKey.OTP_SECRET))

        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ) = emptyList<RevealedSensitiveField>()

        override suspend fun readBundle(entryId: EntryId) = EntrySecret()
        override suspend fun readAll(entryId: EntryId) = EntrySecret()
    }

    private class RecordingActivityRecorder(
        private val events: MutableList<String>,
    ) : ActivityRecorder {
        override suspend fun recordUsage(entryId: String, type: ActivityType): AppResult<Unit> {
            assertEquals("otp-1", entryId)
            assertEquals(ActivityType.VIEW, type)
            events += "record"
            return AppResult.Success(Unit)
        }

        override suspend fun deleteByEntryId(entryId: String) = Unit
        override suspend fun deleteBefore(timestamp: Long) = Unit
    }

    private fun otpEntry(encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32) = Entry(
        identity = EntryIdentity(
            id = EntryId("otp-1"),
            type = EntryType.OTP,
            timestamps = EntryTimestamps(1),
        ),
        profile = EntryProfile(title = "Example", username = "alice"),
        secret = EntrySecret(
            credential = OtpCredential(
                OtpConfig(
                    secret = null,
                    encoding = encoding,
                    issuer = "Issuer",
                    accountName = "alice",
                ),
            ),
        ),
    )
}
