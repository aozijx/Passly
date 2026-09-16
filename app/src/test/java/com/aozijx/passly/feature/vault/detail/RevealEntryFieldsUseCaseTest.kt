package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
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
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RevealEntryFieldsUseCaseTest {

    @Test
    fun `wifi password is read as low sensitivity without field authorization`() = runTest {
        val gate = RecordingAuthorizationGate(allowed = true)
        val recorder = RecordingActivityRecorder()
        val reader = RecordingFieldReader("wifi-secret")
        val useCase = RevealEntryFieldsUseCase(
            authorizationGate = gate,
            entryFieldReader = reader,
            sensitiveFieldRepository = RevealingRepository(),
            activityRecorder = recorder,
        )

        val result = useCase.reveal(
            entry = entry(EntryType.WIFI),
            requestedFields = setOf(FieldKey.PASSWORD),
            recordLowSensitivityAccess = false,
        )

        assertEquals("wifi-secret", String(result.getValue(FieldKey.PASSWORD).toCharArray()))
        result.values.forEach { it.wipe() }
        assertNull(gate.scope)
        assertEquals(listOf(FieldKey.PASSWORD), reader.keys)
        assertTrue(recorder.activities.isEmpty())
    }

    @Test
    fun `login password uses exact reveal scope and records one view`() = runTest {
        val gate = RecordingAuthorizationGate(allowed = true)
        val recorder = RecordingActivityRecorder()
        val reader = RecordingFieldReader("must-not-read")
        val useCase = RevealEntryFieldsUseCase(
            authorizationGate = gate,
            entryFieldReader = reader,
            sensitiveFieldRepository = RevealingRepository(),
            activityRecorder = recorder,
        )

        val result = useCase.reveal(
            entry = entry(EntryType.LOGIN),
            requestedFields = setOf(FieldKey.PASSWORD),
            recordLowSensitivityAccess = false,
        )

        assertEquals("PASSWORD", String(result.getValue(FieldKey.PASSWORD).toCharArray()))
        result.values.forEach { it.wipe() }
        assertEquals(
            AuthorizationScope.SensitiveFields(
                entryId = ENTRY_ID,
                fieldKeys = setOf(SensitiveFieldKey.PASSWORD),
                action = SensitiveAccessAction.REVEAL,
            ),
            gate.scope,
        )
        assertTrue(reader.keys.isEmpty())
        assertEquals(listOf(ActivityType.VIEW), recorder.activities)
    }

    @Test
    fun `denied high sensitivity reveal returns no plaintext`() = runTest {
        val useCase = RevealEntryFieldsUseCase(
            authorizationGate = RecordingAuthorizationGate(allowed = false),
            entryFieldReader = RecordingFieldReader("must-not-read"),
            sensitiveFieldRepository = RevealingRepository(),
            activityRecorder = RecordingActivityRecorder(),
        )

        assertTrue(
            useCase.reveal(
                entry = entry(EntryType.LOGIN),
                requestedFields = setOf(FieldKey.PASSWORD),
                recordLowSensitivityAccess = false,
            ).isEmpty(),
        )
    }

    private class RecordingAuthorizationGate(
        private val allowed: Boolean,
    ) : AuthorizationGate {
        var scope: AuthorizationScope? = null

        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            this.scope = scope
            return if (allowed) {
                AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
            } else {
                AuthorizationResult.Denied(
                    AuthenticationFailure(AuthenticationFailureCode.HOST_UNAVAILABLE),
                )
            }
        }
    }

    private class RecordingFieldReader(
        private val value: String,
    ) : EntryFieldReader {
        val keys = mutableListOf<FieldKey>()

        override fun getFieldValue(entry: Entry, key: FieldKey): String {
            keys += key
            return value
        }
    }

    private class RevealingRepository : SensitiveFieldRepository {
        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): List<RevealedSensitiveField> = keys.map { key ->
            RevealedSensitiveField(entryId, key, OwnedChars.fromString(key.name))
        }

        override suspend fun getPresence(entryId: EntryId) =
            SensitiveFieldPresence(entryId, emptySet())

        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField? = null

        override suspend fun readBundle(entryId: EntryId) = EntrySecret()
        override suspend fun readAll(entryId: EntryId) = EntrySecret()
    }

    private class RecordingActivityRecorder : ActivityRecorder {
        val activities = mutableListOf<ActivityType>()

        override suspend fun recordUsage(entryId: String, type: ActivityType): AppResult<Unit> {
            activities += type
            return AppResult.Success(Unit)
        }

        override suspend fun deleteByEntryId(entryId: String) = Unit
        override suspend fun deleteBefore(timestamp: Long) = Unit
    }

    private fun entry(type: EntryType) = Entry(
        identity = EntryIdentity(
            id = ENTRY_ID,
            type = type,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(title = "Entry"),
        secret = EntrySecret(
            credential = when (type) {
                EntryType.LOGIN -> LoginCredential()
                EntryType.WIFI -> WifiCredential(ssid = "network", password = "wifi-secret")
                else -> error("Unsupported fixture type: $type")
            },
        ),
    )

    private companion object {
        val ENTRY_ID = EntryId("entry-1")
    }
}
