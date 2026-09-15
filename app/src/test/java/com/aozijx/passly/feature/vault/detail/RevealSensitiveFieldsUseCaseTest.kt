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
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RevealSensitiveFieldsUseCaseTest {

    @Test
    fun allowedRevealReturnsValuesByDomainKeyAndRecordsOneView() = runTest {
        val recorder = RecordingActivityRecorder()
        val useCase = RevealSensitiveFieldsUseCase(
            authorizationGate = FixedAuthorizationGate(allowed = true),
            sensitiveFieldRepository = RevealingRepository(),
            activityRecorder = recorder,
        )

        val result = useCase.reveal(
            entryId = ENTRY_ID,
            requestedFields = linkedSetOf(SensitiveFieldKey.PASSWORD, SensitiveFieldKey.CARD_CVV),
        )

        assertEquals("PASSWORD", String(result.getValue(SensitiveFieldKey.PASSWORD).toCharArray()))
        assertEquals("CARD_CVV", String(result.getValue(SensitiveFieldKey.CARD_CVV).toCharArray()))
        result.values.forEach { it.wipe() }
        assertEquals(listOf(ActivityType.VIEW), recorder.activities)
    }

    @Test
    fun deniedOrEmptyRevealReturnsNoValuesAndRecordsNothing() = runTest {
        val recorder = RecordingActivityRecorder()
        val denied = RevealSensitiveFieldsUseCase(
            authorizationGate = FixedAuthorizationGate(allowed = false),
            sensitiveFieldRepository = RevealingRepository(),
            activityRecorder = recorder,
        )

        assertTrue(denied.reveal(ENTRY_ID, setOf(SensitiveFieldKey.PASSWORD)).isEmpty())
        assertTrue(denied.reveal(ENTRY_ID, emptySet()).isEmpty())
        assertTrue(recorder.activities.isEmpty())
    }

    private class FixedAuthorizationGate(
        private val allowed: Boolean,
    ) : AuthorizationGate {
        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> = if (allowed) {
            AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
        } else {
            AuthorizationResult.Denied(
                AuthenticationFailure(AuthenticationFailureCode.HOST_UNAVAILABLE),
            )
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

    private companion object {
        val ENTRY_ID = EntryId("entry-1")
    }
}
