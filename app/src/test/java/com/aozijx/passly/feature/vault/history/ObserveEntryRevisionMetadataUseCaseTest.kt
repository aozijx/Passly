package com.aozijx.passly.feature.vault.history

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class ObserveEntryRevisionMetadataUseCaseTest {
    @Test
    fun `initializes history before requesting the entry metadata flow`() =
        runTest {
            val calls = mutableListOf<String>()
            val useCase = ObserveEntryRevisionMetadataUseCase(repository(calls))

            val result = useCase(ENTRY_ID)

            assertEquals(listOf("initialize", "observe:${ENTRY_ID.value}"), calls)
            assertEquals(emptyList<EntryRevisionMetadata>(), (result as AppResult.Success).data.first())
        }

    @Test
    fun `initialization failure does not subscribe to metadata`() =
        runTest {
            val failure = NotFound()
            val calls = mutableListOf<String>()
            val useCase = ObserveEntryRevisionMetadataUseCase(repository(calls, AppResult.Failure(failure)))

            val result = useCase(ENTRY_ID)

            assertEquals(AppResult.Failure(failure), result)
            assertEquals(listOf("initialize"), calls)
        }

    @Test
    fun `initialization cancellation propagates unchanged`() {
        val cancellation = CancellationException("cancel history initialization")
        val calls = mutableListOf<String>()
        val repository =
            object : EntryRevisionRepository by repository(calls) {
                override suspend fun initializeHistory(): AppResult<Unit> {
                    calls += "initialize"
                    throw cancellation
                }
            }

        val propagated =
            assertThrows(CancellationException::class.java) {
                runTest { ObserveEntryRevisionMetadataUseCase(repository)(ENTRY_ID) }
            }

        assertSame(cancellation, propagated)
        assertEquals(listOf("initialize"), calls)
    }

    private fun repository(
        calls: MutableList<String>,
        initialization: AppResult<Unit> = AppResult.Success(Unit),
    ): EntryRevisionRepository =
        object : EntryRevisionRepository by UnusedRevisionRepository {
            override suspend fun initializeHistory(): AppResult<Unit> {
                calls += "initialize"
                return initialization
            }

            override fun observeMetadata(entryId: EntryId): Flow<List<EntryRevisionMetadata>> {
                calls += "observe:${entryId.value}"
                return flowOf(emptyList())
            }
        }

    private object UnusedRevisionRepository : EntryRevisionRepository {
        override suspend fun initializeHistory(): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeMetadata(entryId: EntryId): Flow<List<EntryRevisionMetadata>> = flowOf(emptyList())

        override suspend fun loadRedacted(
            entryId: EntryId,
            revisionId: EntryRevisionId,
        ): AppResult<RedactedEntryRevision> = error("Unused")

        override suspend fun reveal(
            entryId: EntryId,
            revisionId: EntryRevisionId,
            keys: Set<SensitiveFieldKey>,
            permit: AuthorizationPermit,
        ): AppResult<List<RevealedRevisionSensitiveField>> = error("Unused")

        override suspend fun restore(
            entryId: EntryId,
            revisionId: EntryRevisionId,
            expectedVersion: EntryVersion,
            permit: AuthorizationPermit?,
        ): AppResult<EntryVersion> = error("Unused")
    }

    private companion object {
        val ENTRY_ID = EntryId("entry-1")
    }
}
