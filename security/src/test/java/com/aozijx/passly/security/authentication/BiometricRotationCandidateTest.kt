package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.BiometricRotationPhase
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricRotationCandidateTest {
    @Test
    fun `coroutine cancellation removes uncommitted candidate and preserves old binding`() = runTest {
        val store = Store()
        val deleted = mutableListOf<String>()
        val job = launch {
            withBiometricRotationCandidate(store, "candidate", { deleted += it; true }) {
                store.prepareBiometricRotation(journal())
                awaitCancellation()
            }
        }
        runCurrent()
        job.cancel()
        job.join()
        assertEquals(listOf("candidate"), deleted)
        assertEquals(BiometricBinding("old", true), store.state.binding)
        assertEquals(null, store.state.rotation)
    }

    @Test
    fun `cancellation after atomic commit never deletes the active candidate`() = runTest {
        val store = Store()
        val deleted = mutableListOf<String>()
        try {
            withBiometricRotationCandidate(store, "candidate", { deleted += it; true }) {
                store.state = BiometricBootstrapState(
                    BiometricBinding("candidate", false), journal().copy(phase = BiometricRotationPhase.COMMITTED), setOf("old"),
                )
                throw CancellationException("synthetic cancellation after commit")
            }
            error("cancellation was swallowed")
        } catch (_: CancellationException) { }
        assertEquals(emptyList<String>(), deleted)
        assertEquals("candidate", store.state.binding?.activeAlias)
        assertEquals(BiometricRotationPhase.COMMITTED, store.state.rotation?.phase)
    }

    @Test
    fun `failed key deletion keeps recovery journal for retry`() = runTest {
        val store = Store()
        withBiometricRotationCandidate(store, "candidate", { false }) {
            store.prepareBiometricRotation(journal())
            AuthenticationResult.Cancelled(CancellationReason.USER)
        }
        assertEquals(journal(), store.state.rotation)
    }

    @Test
    fun `old candidate cleanup cannot clear another rotation journal`() = runTest {
        val store = Store()
        val nextJournal = journal().copy(candidateAlias = "next-candidate")
        store.state = store.state.copy(rotation = nextJournal)
        withBiometricRotationCandidate(store, "candidate", { true }) {
            AuthenticationResult.Cancelled(CancellationReason.USER)
        }
        assertEquals(nextJournal, store.state.rotation)
    }

    @Test
    fun `cleanup error cannot replace the original operation failure`() = runTest {
        val store = Store()
        val original = IllegalArgumentException("synthetic operation failure")
        val cleanup = IllegalStateException("synthetic cleanup failure")
        store.readFailure = { throw cleanup }
        try {
            withBiometricRotationCandidate(store, "candidate", { true }) { throw original }
            error("operation failure lost")
        } catch (error: IllegalArgumentException) {
            assertSame(original, error)
            assertEquals(1, error.suppressed.size)
            // Coroutine stacktrace recovery may wrap the cleanup exception.
            assertSame(cleanup, generateSequence(error.suppressed.single()) { it.cause }.last())
        }
    }

    private fun journal() = BiometricRotationJournal(BiometricRotationPhase.PREPARED, "old", "candidate")

    private class Store : VaultBootstrapStore {
        var state = BiometricBootstrapState(BiometricBinding("old", true), null, emptySet())
        var readFailure: () -> Unit = {}
        override suspend fun loadBiometricState(): BiometricBootstrapState { readFailure(); return state }
        override suspend fun prepareBiometricRotation(journal: BiometricRotationJournal) { state = state.copy(rotation = journal) }
        override suspend fun clearBiometricRotationJournal() { state = state.copy(rotation = null) }
        override suspend fun clearBiometricCleanupAlias(alias: String) { state = state.copy(cleanupAliases = state.cleanupAliases - alias) }
        override suspend fun commitBiometricRotation(envelope: KeyEnvelope, binding: BiometricBinding, obsoleteAlias: String?) = error("unused")
        override suspend fun disableBiometric(activeAlias: String) = error("unused")
        override suspend fun save(envelope: KeyEnvelope) = error("unused")
        override suspend fun load(type: EnvelopeType) = error("unused")
        override suspend fun loadAll() = error("unused")
        override suspend fun delete(type: EnvelopeType) = error("unused")
        override suspend fun saveVerificationTag(tag: ByteArray) = error("unused")
        override suspend fun loadVerificationTag() = error("unused")
        override suspend fun saveSensitiveKeyEnvelope(envelope: ByteArray) = error("unused")
        override suspend fun loadSensitiveKeyEnvelope() = error("unused")
        override suspend fun saveAttachmentKeyEnvelope(envelope: ByteArray) = error("unused")
        override suspend fun loadAttachmentKeyEnvelope() = error("unused")
        override suspend fun clear() = error("unused")
    }
}
