package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
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
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportEntryQrUseCaseTest {
    @Test
    fun `wifi authorization precedes a fresh secret read and usage recording`() = runTest {
        val fixture = Fixture()
        val result = fixture.exporter(fixture.entry())

        assertTrue(result is EntryQrExportResult.Ready)
        result as EntryQrExportResult.Ready
        try {
            assertEquals(EntryQrKind.Wifi, result.kind)
            assertEquals("WIFI:T:WPA;S:Office;P:fresh-password;H:true;;", result.content.toPlainString())
            assertEquals(listOf("authorize", "readBundle", "record"), fixture.events)
        } finally {
            result.content.wipe()
        }
    }

    @Test
    fun `denied authorization never reads wifi secrets`() = runTest {
        val fixture = Fixture().apply { outcome = Outcome.Denied }

        assertEquals(EntryQrExportResult.Denied, fixture.exporter(fixture.entry()))
        assertEquals(listOf("authorize"), fixture.events)
    }

    @Test
    fun `cancelled authorization never reads wifi secrets`() = runTest {
        val fixture = Fixture().apply { outcome = Outcome.Cancelled }

        assertEquals(EntryQrExportResult.Cancelled, fixture.exporter(fixture.entry()))
        assertEquals(listOf("authorize"), fixture.events)
    }

    @Test
    fun `invalid saved password produces no qr or usage record`() = runTest {
        val fixture = Fixture().apply { password = "short" }

        assertEquals(EntryQrExportResult.Unavailable, fixture.exporter(fixture.entry()))
        assertEquals(listOf("authorize", "readBundle"), fixture.events)
    }

    @Test
    fun `open networks omit any stored password`() = runTest {
        val fixture = Fixture().apply { securityType = WifiSecurityType.Open }
        val result = fixture.exporter(fixture.entry()) as EntryQrExportResult.Ready

        try {
            assertEquals("WIFI:T:nopass;S:Office;H:true;;", result.content.toPlainString())
        } finally {
            result.content.wipe()
        }
    }

    @Test
    fun `coroutine cancellation propagates through secret reads`() = runTest {
        val fixture = Fixture().apply { cancelRead = true }
        try {
            fixture.exporter(fixture.entry())
            error("Expected cancellation")
        } catch (_: CancellationException) {
            assertEquals(listOf("authorize", "readBundle"), fixture.events)
        }
    }

    private enum class Outcome { Allowed, Denied, Cancelled }

    private class Fixture {
        val events = mutableListOf<String>()
        var outcome = Outcome.Allowed
        var password = "fresh-password"
        var securityType = WifiSecurityType.WpaPersonal
        var cancelRead = false

        val exporter = ExportEntryQrUseCase(
            authorizationGate = object : AuthorizationGate {
                override suspend fun <T> authorize(
                    scope: AuthorizationScope,
                    input: AuthInput,
                    block: suspend (AuthorizationPermit) -> T,
                ): AuthorizationResult<T> {
                    events += "authorize"
                    assertEquals(AuthorizationScope.Global(AuthenticationPurpose.REVEAL_SECRET), scope)
                    return when (outcome) {
                        Outcome.Allowed -> AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
                        Outcome.Denied -> AuthorizationResult.Denied(
                            AuthenticationFailure(AuthenticationFailureCode.HOST_UNAVAILABLE),
                        )
                        Outcome.Cancelled -> AuthorizationResult.Cancelled
                    }
                }
            },
            sensitiveFieldRepository = object : SensitiveFieldRepository {
                override suspend fun readBundle(entryId: EntryId): EntrySecret {
                    events += "readBundle"
                    if (cancelRead) throw CancellationException("cancelled")
                    return EntrySecret(credential = WifiCredential("Office", password, securityType, true))
                }
                override suspend fun readAll(entryId: EntryId) = error("Unexpected full read")
                override suspend fun getPresence(entryId: EntryId) = SensitiveFieldPresence(entryId, emptySet())
                override suspend fun reveal(
                    entryId: EntryId,
                    key: SensitiveFieldKey,
                    action: com.aozijx.passly.domain.access.model.SensitiveAccessAction,
                    permit: AuthorizationPermit,
                ): RevealedSensitiveField? = error("Unexpected high sensitivity read")
                override suspend fun revealMany(
                    entryId: EntryId,
                    keys: Set<SensitiveFieldKey>,
                    action: com.aozijx.passly.domain.access.model.SensitiveAccessAction,
                    permit: AuthorizationPermit,
                ): List<RevealedSensitiveField> = error("Unexpected high sensitivity read")
            },
            activityRecorder = object : ActivityRecorder {
                override suspend fun recordUsage(entryId: String, type: ActivityType): AppResult<Unit> {
                    events += "record"
                    assertEquals(ActivityType.VIEW, type)
                    return AppResult.Success(Unit)
                }
                override suspend fun deleteByEntryId(entryId: String) = Unit
                override suspend fun deleteBefore(timestamp: Long) = Unit
            },
            logger = testLogger(),
            formatOtpUri = { _, _ -> error("Unexpected OTP format") },
        )

        fun entry() = Entry(
            identity = EntryIdentity(id = EntryId("wifi"), type = EntryType.WIFI, timestamps = EntryTimestamps(1L)),
            profile = EntryProfile(title = "Office"),
            secret = EntrySecret(credential = WifiCredential("Office", "stale-password")),
        )
    }
}
