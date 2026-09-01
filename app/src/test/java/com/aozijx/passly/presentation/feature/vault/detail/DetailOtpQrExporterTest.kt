package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailOtpQrExporterTest {
    @Test
    fun authenticatesBeforeRevealingSecretAndBuildingQrPayload() = runTest {
        val events = mutableListOf<String>()
        val revealed = OwnedChars.fromString("JBSWY3DPEHPK3PXP")
        val exporter = DetailOtpQrExporter(
            authorizationGate = RecordingAuthorizationGate(events),
            sensitiveFieldRepository = RecordingSensitiveFieldRepository(events, revealed),
            formatUri = { config, _ ->
                events += "format"
                "otpauth://totp/Example?secret=${config.secret}"
            },
        )

        val uri = requireNotNull(exporter.export(otpEntry()))

        assertEquals(listOf("authenticate", "reveal", "format"), events)
        assertTrue(uri.startsWith("otpauth://totp/"))
        assertTrue(uri.contains("secret=JBSWY3DPEHPK3PXP"))
        assertTrue(revealed.isEmpty)
    }

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
            permit: AuthorizationPermit,
        ) = emptyList<RevealedSensitiveField>()

        override suspend fun readBundle(entryId: EntryId) = EntrySecret()
        override suspend fun readAll(entryId: EntryId) = EntrySecret()
    }

    private fun otpEntry() = Entry(
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
                    issuer = "Issuer",
                    accountName = "alice",
                ),
            ),
        ),
    )
}
