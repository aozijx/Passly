package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationCredentialSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KdfAlgorithm
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPasswordProvisioningCommitTest {
    @Test
    fun `failure before durable save remains a retryable failure`() =
        runBlocking {
            val store = RecordingStore()
            val result =
                runAppPasswordProvisioning(
                    store = store,
                    salt = SALT,
                    requestId = "request",
                    persist = { throw IOException("write failed") },
                    reconcileSession = {},
                )

            assertEquals(
                AuthenticationResult.Failure(
                    AuthenticationFailure(
                        AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
                        AuthenticationRequestId("request"),
                    ),
                ),
                result,
            )
        }

    @Test
    fun `save failure after durable write is reported as committed`() =
        runBlocking {
            val store = RecordingStore(saveFailure = IOException("post-write failure"), failAfterWrite = true)

            val result =
                runAppPasswordProvisioning(
                    store = store,
                    salt = SALT,
                    requestId = "request",
                    persist = { store.save(makeEnvelope(SALT)) },
                    reconcileSession = {},
                )

            assertEquals(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD), result)
            assertEquals(SALT.toList(), store.envelope?.salt?.toList())
            assertTrue(
                AuthenticationMethod.APP_PASSWORD in
                    store
                        .loadAuthenticationCredentialSnapshot()
                        .toAuthenticationMethods(biometricAvailable = false),
            )
        }

    @Test
    fun `session refresh failure after save is reported as committed`() =
        runBlocking {
            val store = RecordingStore()

            val result =
                runAppPasswordProvisioning(
                    store = store,
                    salt = SALT,
                    requestId = "request",
                    persist = { store.save(makeEnvelope(SALT)) },
                    reconcileSession = { throw IOException("availability refresh failed") },
                )

            assertEquals(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD), result)
        }

    @Test
    fun `cancellation after persistence propagates`() =
        runBlocking {
            val cancelled = CancellationException("cancelled after save")
            val store = RecordingStore()

            val actual =
                runCatching {
                    runAppPasswordProvisioning(
                        store = store,
                        salt = SALT,
                        requestId = "request",
                        persist = { store.save(makeEnvelope(SALT)) },
                        reconcileSession = { throw cancelled },
                    )
                }.exceptionOrNull()

            assertSame(cancelled, actual)
        }

    @Test
    fun `fatal error after persistence propagates without becoming a retryable failure`() =
        runBlocking {
            val fatal = AssertionError("fatal after save")
            val store = RecordingStore()

            val actual =
                runCatching {
                    runAppPasswordProvisioning(
                        store = store,
                        salt = SALT,
                        requestId = "request",
                        persist = { store.save(makeEnvelope(SALT)) },
                        reconcileSession = { throw fatal },
                    )
                }.exceptionOrNull()

            assertSame(fatal, actual)
            assertTrue(store.envelope != null)
        }

    private class RecordingStore(
        private val saveFailure: Throwable? = null,
        private val failAfterWrite: Boolean = false,
    ) : VaultBootstrapStore {
        var envelope: KeyEnvelope? = null
            private set

        override suspend fun save(envelope: KeyEnvelope) {
            if (saveFailure != null && !failAfterWrite) throw saveFailure
            this.envelope = makeEnvelope(envelope.salt)
            saveFailure?.let { throw it }
        }

        override suspend fun load(type: EnvelopeType): KeyEnvelope? =
            envelope
                ?.takeIf { it.type == type }
                ?.let { makeEnvelope(it.salt) }

        override suspend fun loadAll(): List<KeyEnvelope> = listOfNotNull(envelope?.let { makeEnvelope(it.salt) })

        override suspend fun loadAuthenticationCredentialSnapshot() =
            AuthenticationCredentialSnapshot(
                appPasswordConfigured = envelope?.type == EnvelopeType.APP_PASSWORD,
            )

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

    private companion object {
        val SALT = byteArrayOf(9, 10, 11, 12)

        fun makeEnvelope(salt: ByteArray) =
            KeyEnvelope(
                type = EnvelopeType.APP_PASSWORD,
                ciphertext = byteArrayOf(1, 2, 3, 4),
                iv = byteArrayOf(5, 6, 7, 8),
                salt = salt.copyOf(),
                algorithm = KdfAlgorithm.ARGON2ID,
            )
    }
}
