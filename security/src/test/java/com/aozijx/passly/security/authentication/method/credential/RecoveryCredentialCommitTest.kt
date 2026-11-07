package com.aozijx.passly.security.authentication.method.credential
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationCredentialSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KdfAlgorithm
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCredentialCommitTest {
    @Test
    fun `precommit store failure remains retryable and preserves the draft`() =
        runBlocking {
            val store = RecordingBootstrapStore(saveFailure = IOException("write failed"))
            val draft = draft(store)

            val result = draft.commit()

            assertTrue(result is AuthenticationResult.Failure)
            assertFalse(draft.isCommitted)
            assertTrue(draft.reveal()!!.isNotEmpty())
            assertNull(store.envelope)
        }

    @Test
    fun `store failure after durable write is resolved as committed`() =
        runBlocking {
            val store =
                RecordingBootstrapStore(
                    saveFailure = IOException("post-write failure"),
                    failAfterWrite = true,
                )
            val draft = draft(store)

            val result = draft.commit()

            assertEquals(AuthenticationResult.Success(AuthenticationMethod.RECOVERY_CODE), result)
            assertTrue(draft.isCommitted)
            assertNull(draft.reveal())
            assertNotNullEnvelope(store)
        }

    @Test
    fun `cancellation after durable write propagates while preserving commit truth`() =
        runBlocking {
            val cancelled = CancellationException("refresh cancelled")
            val store = RecordingBootstrapStore()
            val draft = draft(store, refresh = { throw cancelled })

            val actual = runCatching { draft.commit() }.exceptionOrNull()

            assertSame(cancelled, actual)
            assertTrue(draft.isCommitted)
            assertNull(draft.reveal())
            assertNotNullEnvelope(store)
        }

    @Test
    fun `fatal refresh failure propagates after committed draft secrets are cleared`() =
        runBlocking {
            val fatal = AssertionError("fatal refresh failure")
            val store = RecordingBootstrapStore()
            val draft = draft(store, refresh = { throw fatal })

            val actual = runCatching { draft.commit() }.exceptionOrNull()

            assertSame(fatal, actual)
            assertTrue(draft.isCommitted)
            assertNull(draft.reveal())
            assertNotNullEnvelope(store)
        }

    @Test
    fun `fatal store failure after write propagates after durable readback and cleanup`() =
        runBlocking {
            val fatal = AssertionError("fatal post-write failure")
            val store = RecordingBootstrapStore(saveFailure = fatal, failAfterWrite = true)
            val draft = draft(store)

            val actual = runCatching { draft.commit() }.exceptionOrNull()

            assertSame(fatal, actual)
            assertTrue(draft.isCommitted)
            assertNull(draft.reveal())
            assertNotNullEnvelope(store)
        }

    private fun draft(
        store: RecordingBootstrapStore,
        refresh: suspend () -> Unit = {},
    ) = SecureRecoveryCodeDraft(
        generationId = "test-draft",
        sourceCode = "RECOVERY-CODE".toCharArray(),
        sourceEnvelope =
            KeyEnvelope(
                type = EnvelopeType.RECOVERY,
                ciphertext = byteArrayOf(1, 2, 3, 4),
                iv = byteArrayOf(5, 6, 7, 8),
                salt = byteArrayOf(9, 10, 11, 12),
                algorithm = KdfAlgorithm.ARGON2ID,
            ),
        vaultBootstrapStore = store,
        authenticationManager = RecordingAuthenticationManager(refresh),
    )

    private fun assertNotNullEnvelope(store: RecordingBootstrapStore) {
        assertTrue(store.envelope != null)
        assertTrue(store.envelope!!.ciphertext.any { it != 0.toByte() })
    }

    private class RecordingBootstrapStore(
        private val saveFailure: Throwable? = null,
        private val failAfterWrite: Boolean = false,
    ) : VaultBootstrapStore {
        var envelope: KeyEnvelope? = null
            private set

        override suspend fun save(envelope: KeyEnvelope) {
            if (!failAfterWrite && saveFailure != null) throw saveFailure
            this.envelope =
                envelope.copy(
                    ciphertext = envelope.ciphertext.copyOf(),
                    iv = envelope.iv.copyOf(),
                    salt = envelope.salt.copyOf(),
                )
            saveFailure?.let { throw it }
        }

        override suspend fun load(type: EnvelopeType): KeyEnvelope? =
            envelope
                ?.takeIf { it.type == type }
                ?.let { it.copy(ciphertext = it.ciphertext.copyOf(), iv = it.iv.copyOf(), salt = it.salt.copyOf()) }

        override suspend fun loadAll(): List<KeyEnvelope> = listOfNotNull(envelope)

        override suspend fun loadAuthenticationCredentialSnapshot() = AuthenticationCredentialSnapshot()

        override suspend fun delete(type: EnvelopeType) {
            if (envelope?.type == type) envelope = null
        }

        override suspend fun saveVerificationTag(tag: ByteArray) = Unit

        override suspend fun loadVerificationTag(): ByteArray? = null

        override suspend fun saveSensitiveKeyEnvelope(envelope: ByteArray) = Unit

        override suspend fun loadSensitiveKeyEnvelope(): ByteArray? = null

        override suspend fun saveAttachmentKeyEnvelope(envelope: ByteArray) = Unit

        override suspend fun loadAttachmentKeyEnvelope(): ByteArray? = null

        override suspend fun loadBiometricState() = BiometricBootstrapState(null, null, emptySet())

        override suspend fun prepareBiometricRotation(journal: BiometricRotationJournal) = Unit

        override suspend fun commitBiometricRotation(
            envelope: KeyEnvelope,
            binding: BiometricBinding,
            obsoleteAlias: String?,
        ) = Unit

        override suspend fun clearBiometricCleanupAlias(alias: String) = Unit

        override suspend fun disableBiometric(activeAlias: String) = Unit

        override suspend fun clearBiometricRotationJournal() = Unit
    }

    private class RecordingAuthenticationManager(
        private val refresh: suspend () -> Unit,
    ) : AuthenticationManager {
        override val state = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
        override val methods = MutableStateFlow(AuthenticationMethods())

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult = error("unused")

        override suspend fun lock(request: SessionLockRequest) = Unit

        override suspend fun refreshAvailability() = refresh()

        override fun snapshot() = AuthenticationSnapshot(state.value, methods.value)
    }
}
