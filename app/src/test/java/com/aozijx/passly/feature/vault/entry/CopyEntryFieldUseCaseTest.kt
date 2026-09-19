package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
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
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyEntryFieldUseCaseTest {
    @Test
    fun usernameIsReadAndWrittenInsideGlobalCopyAuthorization() = runTest {
        val fixture = fixture(entry = entry(username = "alice"))

        val result = fixture.useCase(ENTRY_ID, EntryType.ACCOUNT, FieldKey.USERNAME)

        assertEquals(CopyEntryFieldResult.Copied, result)
        assertEquals(AuthorizationScope.Global(AuthenticationPurpose.COPY_SECRET), fixture.gate.scope)
        assertEquals(listOf("alice"), fixture.clipboard.values)
        assertEquals(1, fixture.query.reads)
        assertEquals(listOf(ENTRY_ID.value to ActivityType.COPY_USERNAME), fixture.activityRecorder.activities)
    }

    @Test
    fun passwordUsesExactCopyScopeAndWipesRevealedValue() = runTest {
        val value = OwnedChars.fromString("secret")
        val fixture = fixture(entry = entry(), revealed = value)

        val result = fixture.useCase(ENTRY_ID, EntryType.LOGIN, FieldKey.PASSWORD)

        assertEquals(CopyEntryFieldResult.Copied, result)
        assertEquals(
            AuthorizationScope.SensitiveFields(
                entryId = ENTRY_ID,
                fieldKeys = setOf(SensitiveFieldKey.PASSWORD),
                action = SensitiveAccessAction.COPY,
            ),
            fixture.gate.scope,
        )
        assertEquals(SensitiveAccessAction.COPY, fixture.sensitiveRepository.action)
        assertEquals(listOf("secret"), fixture.clipboard.values)
        assertTrue(value.isEmpty)
        assertEquals(0, fixture.query.reads)
        assertEquals(listOf(ENTRY_ID.value to ActivityType.COPY_PASSWORD), fixture.activityRecorder.activities)
    }

    @Test
    fun cancelledAuthorizationDoesNotReadOrCopy() = runTest {
        val fixture = fixture(entry = entry(), allowed = false)

        val result = fixture.useCase(ENTRY_ID, EntryType.ACCOUNT, FieldKey.USERNAME)

        assertEquals(CopyEntryFieldResult.NotAuthorized, result)
        assertEquals(0, fixture.query.reads)
        assertTrue(fixture.clipboard.values.isEmpty())
        assertTrue(fixture.activityRecorder.activities.isEmpty())
    }

    @Test
    fun otpValueIsResolvedOnlyAfterAuthorization() = runTest {
        val gate = RecordingGate(allowed = true)
        val clipboard = RecordingClipboard()
        val activityRecorder = RecordingActivityRecorder()
        var reads = 0

        val result = CopyOtpCodeUseCase(gate, clipboard, activityRecorder).invoke(ENTRY_ID) {
            reads++
            "123456"
        }

        assertEquals(CopyEntryFieldResult.Copied, result)
        assertEquals(AuthorizationScope.Global(AuthenticationPurpose.COPY_SECRET), gate.scope)
        assertEquals(1, reads)
        assertEquals(listOf("123456"), clipboard.values)
        assertEquals(listOf(ENTRY_ID.value to ActivityType.COPY_PASSWORD), activityRecorder.activities)
    }

    @Test
    fun cancelledOtpAuthorizationDoesNotResolveValue() = runTest {
        val gate = RecordingGate(allowed = false)
        val clipboard = RecordingClipboard()
        val activityRecorder = RecordingActivityRecorder()
        var reads = 0

        val result = CopyOtpCodeUseCase(gate, clipboard, activityRecorder).invoke(ENTRY_ID) {
            reads++
            "123456"
        }

        assertEquals(CopyEntryFieldResult.NotAuthorized, result)
        assertEquals(0, reads)
        assertTrue(clipboard.values.isEmpty())
        assertTrue(activityRecorder.activities.isEmpty())
    }

    private fun fixture(
        entry: Entry?,
        revealed: OwnedChars? = null,
        allowed: Boolean = true,
    ): Fixture {
        val gate = RecordingGate(allowed)
        val query = RecordingQuery(entry)
        val sensitiveRepository = RecordingSensitiveRepository(revealed)
        val clipboard = RecordingClipboard()
        val activityRecorder = RecordingActivityRecorder()
        return Fixture(
            useCase = CopyEntryFieldUseCase(
                authorizationGate = gate,
                entryQueryRepository = query,
                entryFieldReader = UsernameReader,
                sensitiveFieldRepository = sensitiveRepository,
                clipboardWriter = clipboard,
                activityRecorder = activityRecorder,
            ),
            gate = gate,
            query = query,
            sensitiveRepository = sensitiveRepository,
            clipboard = clipboard,
            activityRecorder = activityRecorder,
        )
    }

    private data class Fixture(
        val useCase: CopyEntryFieldUseCase,
        val gate: RecordingGate,
        val query: RecordingQuery,
        val sensitiveRepository: RecordingSensitiveRepository,
        val clipboard: RecordingClipboard,
        val activityRecorder: RecordingActivityRecorder,
    )

    private class RecordingGate(private val allowed: Boolean) : AuthorizationGate {
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
                AuthorizationResult.Cancelled
            }
        }
    }

    private class RecordingQuery(private val entry: Entry?) : EntryQueryRepository {
        var reads = 0
        override suspend fun getById(entryId: EntryId): Entry? = entry.also { reads++ }
        override suspend fun findEntriesWithCustomIcons() = emptyList<Entry>()
        override suspend fun count() = 0
    }

    private object UsernameReader : EntryFieldReader {
        override fun getFieldValue(entry: Entry, key: FieldKey): String? =
            entry.username.takeIf { key == FieldKey.USERNAME }
    }

    private class RecordingSensitiveRepository(
        private val revealed: OwnedChars?,
    ) : SensitiveFieldRepository {
        var action: SensitiveAccessAction? = null
        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField? {
            this.action = action
            return revealed?.let { RevealedSensitiveField(entryId, key, it) }
        }
        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ) = emptyList<RevealedSensitiveField>()
        override suspend fun getPresence(entryId: EntryId) = SensitiveFieldPresence(entryId, emptySet())
        override suspend fun readBundle(entryId: EntryId) = EntrySecret()
        override suspend fun readAll(entryId: EntryId) = EntrySecret()
    }

    private class RecordingClipboard : SensitiveClipboardWriter {
        val values = mutableListOf<String>()
        override suspend fun writeSensitive(text: String) { values += text }
    }

    private class RecordingActivityRecorder : ActivityRecorder {
        val activities = mutableListOf<Pair<String, ActivityType>>()

        override suspend fun recordUsage(entryId: String, type: ActivityType): AppResult<Unit> {
            activities += entryId to type
            return AppResult.Success(Unit)
        }

        override suspend fun deleteByEntryId(entryId: String) = Unit
        override suspend fun deleteBefore(timestamp: Long) = Unit
    }

    private fun entry(username: String = "") = Entry(
        identity = EntryIdentity(ENTRY_ID, EntryType.ACCOUNT, timestamps = EntryTimestamps(1L)),
        profile = EntryProfile(title = "Entry", username = username),
    )

    private companion object {
        val ENTRY_ID = EntryId("entry-id")
    }
}
