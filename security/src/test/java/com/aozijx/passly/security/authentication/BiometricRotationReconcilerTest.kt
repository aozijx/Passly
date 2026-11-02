package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationCredentialSnapshot
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.BiometricRotationPhase
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.AuthenticationRuntimeMaintenanceResult
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricRotationReconcilerTest {
    @Test
    fun `committed cleanup failure preserves active binding and metadata for retry`() =
        runTest {
            val store =
                MemoryStore(
                    BiometricBootstrapState(
                        binding = BiometricBinding("new", true),
                        rotation = BiometricRotationJournal(BiometricRotationPhase.COMMITTED, "old", "new"),
                        cleanupAliases = setOf("old"),
                    ),
                )

            val result = reconcileBiometricRotationState(store) { alias -> alias != "old" }

            assertEquals(AuthenticationRuntimeMaintenanceResult.RETRY_REQUIRED, result)
            assertEquals("new", store.state.binding?.activeAlias)
            assertEquals(BiometricRotationPhase.COMMITTED, store.state.rotation?.phase)
            assertEquals(setOf("old"), store.state.cleanupAliases)
        }

    @Test
    fun `retry removes obsolete alias and clears committed journal`() =
        runTest {
            val store = committedStore()
            val deleted = mutableListOf<String>()

            val result =
                reconcileBiometricRotationState(store) { alias ->
                    deleted.add(alias)
                    true
                }

            assertEquals(AuthenticationRuntimeMaintenanceResult.COMPLETE, result)
            assertEquals(listOf("old"), deleted)
            assertEquals("new", store.state.binding?.activeAlias)
            assertEquals(null, store.state.rotation)
            assertTrue(store.state.cleanupAliases.isEmpty())
        }

    @Test
    fun `prepared candidate deletion failure preserves journal for retry`() =
        runTest {
            val journal = BiometricRotationJournal(BiometricRotationPhase.PREPARED, "old", "candidate")
            val store =
                MemoryStore(
                    BiometricBootstrapState(
                        binding = BiometricBinding("old", true),
                        rotation = journal,
                        cleanupAliases = emptySet(),
                    ),
                )

            val result = reconcileBiometricRotationState(store) { false }

            assertEquals(AuthenticationRuntimeMaintenanceResult.RETRY_REQUIRED, result)
            assertEquals(journal, store.state.rotation)
            assertEquals("old", store.state.binding?.activeAlias)
        }

    @Test
    fun `prepared candidate that became active is never deleted and stale journal is cleared`() =
        runTest {
            val store =
                MemoryStore(
                    BiometricBootstrapState(
                        binding = BiometricBinding("candidate", true),
                        rotation = BiometricRotationJournal(BiometricRotationPhase.PREPARED, "old", "candidate"),
                        cleanupAliases = emptySet(),
                    ),
                )
            var deleteCalls = 0

            val result =
                reconcileBiometricRotationState(store) {
                    deleteCalls++
                    true
                }

            assertEquals(AuthenticationRuntimeMaintenanceResult.COMPLETE, result)
            assertEquals(0, deleteCalls)
            assertEquals(null, store.state.rotation)
            assertEquals("candidate", store.state.binding?.activeAlias)
        }

    @Test
    fun `ordinary store exception remains available to maintenance failure owner`() =
        runTest {
            val failure = IllegalStateException("datastore unavailable")
            val store = committedStore().apply { loadFailure = failure }
            try {
                reconcileBiometricRotationState(store) { true }
                error("store failure was swallowed")
            } catch (actual: IllegalStateException) {
                assertTrue(actual === failure)
            }
        }

    @Test
    fun `cancellation from store propagates without being treated as retryable failure`() =
        runTest {
            val cancellation = CancellationException("startup cancelled")
            val store = committedStore().apply { loadFailure = cancellation }

            try {
                reconcileBiometricRotationState(store) { true }
                error("cancellation was swallowed")
            } catch (actual: CancellationException) {
                assertTrue(actual === cancellation)
            }
        }

    @Test
    fun `fatal store error propagates`() =
        runTest {
            val fatal = AssertionError("datastore fatal")
            val store = committedStore().apply { loadFailure = fatal }

            try {
                reconcileBiometricRotationState(store) { true }
                error("fatal error was swallowed")
            } catch (actual: AssertionError) {
                assertTrue(actual === fatal)
            }
        }

    @Test
    fun `failed metadata update preserves cleanup alias for idempotent retry`() =
        runTest {
            val store = committedStore().apply { cleanupUpdateFailure = IllegalStateException("datastore write failed") }
            val deleted = mutableListOf<String>()

            try {
                reconcileBiometricRotationState(store) { alias ->
                    deleted.add(alias)
                    true
                }
                error("metadata update failure was swallowed")
            } catch (expected: IllegalStateException) {
                assertEquals("datastore write failed", expected.message)
            }

            assertEquals(listOf("old"), deleted)
            assertEquals(setOf("old"), store.state.cleanupAliases)
            assertEquals(BiometricRotationPhase.COMMITTED, store.state.rotation?.phase)
        }

    private fun committedStore() =
        MemoryStore(
            BiometricBootstrapState(
                binding = BiometricBinding("new", true),
                rotation = BiometricRotationJournal(BiometricRotationPhase.COMMITTED, "old", "new"),
                cleanupAliases = setOf("old"),
            ),
        )

    private class MemoryStore(
        var state: BiometricBootstrapState,
    ) : VaultBootstrapStore {
        var loadFailure: Throwable? = null
        var cleanupUpdateFailure: Exception? = null

        override suspend fun loadBiometricState(): BiometricBootstrapState {
            loadFailure?.let { throw it }
            return state
        }

        override suspend fun clearBiometricRotationJournal() {
            state = state.copy(rotation = null)
        }

        override suspend fun clearBiometricCleanupAlias(alias: String) {
            cleanupUpdateFailure?.let { throw it }
            state = state.copy(cleanupAliases = state.cleanupAliases - alias)
        }

        override suspend fun prepareBiometricRotation(journal: BiometricRotationJournal) = error("unused")

        override suspend fun commitBiometricRotation(
            envelope: KeyEnvelope,
            binding: BiometricBinding,
            obsoleteAlias: String?,
        ) = error("unused")

        override suspend fun disableBiometric(activeAlias: String) = error("unused")

        override suspend fun save(envelope: KeyEnvelope) = error("unused")

        override suspend fun load(type: EnvelopeType) = error("unused")

        override suspend fun loadAll() = error("unused")

        override suspend fun loadAuthenticationCredentialSnapshot() = AuthenticationCredentialSnapshot()

        override suspend fun delete(type: EnvelopeType) = error("unused")

        override suspend fun saveVerificationTag(tag: ByteArray) = error("unused")

        override suspend fun loadVerificationTag(): ByteArray? = error("unused")

        override suspend fun saveSensitiveKeyEnvelope(envelope: ByteArray) = error("unused")

        override suspend fun loadSensitiveKeyEnvelope(): ByteArray? = error("unused")

        override suspend fun saveAttachmentKeyEnvelope(envelope: ByteArray) = error("unused")

        override suspend fun loadAttachmentKeyEnvelope(): ByteArray? = error("unused")
    }
}
