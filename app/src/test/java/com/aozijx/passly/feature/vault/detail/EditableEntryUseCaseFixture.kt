package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.flow.MutableStateFlow

internal class EditableEntryUseCaseFixture(
    initialEntry: Entry,
    private val presentKeys: Set<SensitiveFieldKey> = emptySet(),
    revealedValues: Map<SensitiveFieldKey, String> = emptyMap(),
) {
    val access = TestAccess()
    val query = TestQuery(initialEntry)
    private val sensitiveFields = TestSensitiveFields(initialEntry.id, presentKeys, revealedValues)
    private val command = TestCommand(query)
    private val authorization = TestAuthorization()
    private val activity = TestActivity()

    val loader = LoadEditableEntryUseCase(query, sensitiveFields, authorization, access, activity)
    val editor = EditDetailEntryUseCase(query, command, access, testLogger())

    companion object {
        fun unsupportedEntry() = EditableEntryUseCaseFixture(
            Entry(
                identity = EntryIdentity(
                    EntryId("unsupported-fixture"),
                    EntryType.NOTE,
                    timestamps = EntryTimestamps(1),
                ),
                profile = EntryProfile("Unsupported"),
            ),
        )
    }

    internal class TestAccess : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1))
        override fun isUnlocked() = true
    }

    internal class TestQuery(var current: Entry?) : EntryQueryRepository {
        override suspend fun getById(entryId: EntryId) = current
        override suspend fun findEntriesWithCustomIcons() = emptyList<Entry>()
        override suspend fun count() = if (current == null) 0 else 1
    }

    private class TestAuthorization : AuthorizationGate {
        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> = AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
    }

    private class TestSensitiveFields(
        private val entryId: EntryId,
        private val presentKeys: Set<SensitiveFieldKey>,
        private val values: Map<SensitiveFieldKey, String>,
    ) : SensitiveFieldRepository {
        override suspend fun getPresence(entryId: EntryId) = SensitiveFieldPresence(entryId, presentKeys)

        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): List<RevealedSensitiveField> = keys.mapNotNull { key ->
            values[key]?.let { RevealedSensitiveField(entryId, key, OwnedChars.fromString(it)) }
        }

        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField? = null

        override suspend fun readBundle(entryId: EntryId) = EntrySecret()
        override suspend fun readAll(entryId: EntryId) = EntrySecret()
    }

    private class TestCommand(private val query: TestQuery) : EntryCommandRepository {
        override suspend fun updateEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
            changes: EntryUpdate,
        ): AppResult<Unit> {
            val current = requireNotNull(query.current)
            query.current = current.copy(
                identity = current.identity.copy(version = current.version.next()),
                profile = changes.profile ?: current.profile,
                secret = changes.secret ?: current.secret,
            )
            return AppResult.Success(Unit)
        }

        override suspend fun createEntry(entry: Entry) = AppResult.Success(entry.id)
        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }

    private class TestActivity : ActivityRecorder {
        override suspend fun recordUsage(entryId: String, type: ActivityType) = AppResult.Success(Unit)
        override suspend fun deleteByEntryId(entryId: String) = Unit
        override suspend fun deleteBefore(timestamp: Long) = Unit
    }
}
