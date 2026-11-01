package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.domain.access.model.AuthenticationCredentialSnapshot
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.FieldKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRecoveryCredentialGeneratorTest {
    @Test
    fun `fatal KDF error propagates after generated recovery material is wiped`() = kotlinx.coroutines.runBlocking {
        val fatal = AssertionError("fatal KDF failure")
        val runner = CapturingFailingKdfRunner(fatal)
        val store = MemoryBootstrapStore()
        val manager = DefaultRecoveryCredentialGenerator(
            kdfRunner = runner,
            dekManager = DekManager(store, FieldKeyManager(), NoOpLogger),
            vaultBootstrapStore = store,
            authenticationManager = NoOpAuthenticationManager,
        )

        val actual = runCatching { manager.generate() }.exceptionOrNull()

        assertSame(fatal, actual)
        assertTrue(runCatching { runner.secret!!.copyForWorker() }.isFailure)
    }

    private class CapturingFailingKdfRunner(
        private val failure: Throwable,
    ) : KdfRunner {
        var secret: SecretChars? = null
        override suspend fun <T> execute(secret: SecretChars, operation: KdfOperation<T>): T {
            this.secret = secret
            throw failure
        }
        override fun close() = Unit
    }

    private object NoOpAuthenticationManager : AuthenticationManager {
        override val state = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
        override val methods = MutableStateFlow(AuthenticationMethods())
        override suspend fun authenticate(request: AuthenticationRequest, input: AuthInput): AuthenticationResult =
            error("unused")
        override suspend fun lock(request: SessionLockRequest) = Unit
        override suspend fun refreshAvailability() = Unit
        override fun snapshot() = AuthenticationSnapshot(state.value, methods.value)
    }

    private object NoOpLogger : Logger {
        private object Scope : TraceScope {
            override val traceId = TraceId("test")
            override val spanId = SpanId("test")
            override val parentSpanId: SpanId? = null
        }
        override fun debug(event: EventKey, fields: LogFields) = Unit
        override fun info(event: EventKey, fields: LogFields) = Unit
        override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) = Unit
        override fun warn(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
        override fun error(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
        override fun fatal(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
        override suspend fun <T> withRootTrace(operation: OperationCode, block: suspend TraceScope.() -> T): T = Scope.block()
        override suspend fun <T> inSpan(operation: OperationCode, block: suspend TraceScope.() -> T): T = Scope.block()
        override fun captureTraceToken(singleUse: Boolean): TraceToken = error("unused")
        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): TraceResumeResult<T> = error("unused")
    }

    private class MemoryBootstrapStore : VaultBootstrapStore {
        override suspend fun save(envelope: KeyEnvelope) = Unit
        override suspend fun load(type: EnvelopeType): KeyEnvelope? = null
        override suspend fun loadAll(): List<KeyEnvelope> = emptyList()
        override suspend fun loadAuthenticationCredentialSnapshot() = AuthenticationCredentialSnapshot()
        override suspend fun delete(type: EnvelopeType) = Unit
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
}
