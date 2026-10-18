package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.FieldKeyManager
import com.aozijx.passly.security.dek.SensitiveDataKeyManager
import com.aozijx.passly.security.lock.LockStateManager
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultSessionIdleTimeoutTest {
    @Test
    fun `idle timeout does not cancel its own lock operation or report a false failure`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val logger = RecordingLogger()
            val bootstrap = EmptyVaultBootstrapStore()
            val dekManager = DekManager(bootstrap, FieldKeyManager(), logger)
            val sensitiveKeyManager = SensitiveDataKeyManager(bootstrap, dekManager, CoroutineScope(dispatcher))
            val databaseSession = YieldingDatabaseSession()
            val lockState = LockStateManager().apply { mark(SecureSessionState.UNLOCKED) }
            val timeout = MutableStateFlow(1L)
            val controller = VaultSessionController(
                dekManager = dekManager,
                sensitiveDataKeyManager = sensitiveKeyManager,
                authorizationPermitRevoker = { },
                sessionManager = databaseSession,
                vaultBootstrapStore = bootstrap,
                lockStateManager = lockState,
                securityEvents = SecuritySessionEventLogger(logger),
                idleTimeoutSettings = object : IdleTimeoutSettings {
                    override val isLockOnBackground = MutableStateFlow(false)
                    override val lockTimeout = timeout
                },
            )

            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))
            controller.onUserInteraction()
            advanceTimeBy(1L)
            runCurrent()

            assertTrue(databaseSession.softLockCompleted)
            assertEquals(SecureSessionState.SOFT_LOCKED, lockState.state)
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockCompleted })
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCompleted })
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class YieldingDatabaseSession : DatabaseSessionLifecycle {
        override var lockState: SecureSessionState = SecureSessionState.UNLOCKED
        var softLockCompleted = false

        override suspend fun unlock(): Throwable? = null

        override suspend fun softLock() {
            lockState = SecureSessionState.SOFT_LOCKED
            yield()
            softLockCompleted = true
        }

        override suspend fun seal() {
            lockState = SecureSessionState.SEALED
        }
    }

    private class RecordingLogger : Logger {
        val infoEvents = mutableListOf<EventKey>()
        val errorEvents = mutableListOf<EventKey>()

        override fun debug(event: EventKey, fields: LogFields) = Unit
        override fun info(event: EventKey, fields: LogFields) { infoEvents += event }
        override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) = Unit
        override fun warn(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit
        override fun error(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) { errorEvents += event }
        override fun fatal(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit

        override suspend fun <T> withRootTrace(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = TestTraceScope.block()

        override suspend fun <T> inSpan(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = withContext(CoroutineName("test-span")) {
            TestTraceScope.block()
        }

        override fun captureTraceToken(singleUse: Boolean): TraceToken = error("not used")
        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): TraceResumeResult<T> = error("not used")
    }

    private object TestTraceScope : TraceScope {
        override val traceId = TraceId("test_trace")
        override val spanId = SpanId("test_span")
        override val parentSpanId: SpanId? = null
    }

    private class EmptyVaultBootstrapStore : VaultBootstrapStore {
        override suspend fun save(envelope: KeyEnvelope) = Unit
        override suspend fun load(type: EnvelopeType): KeyEnvelope? = null
        override suspend fun loadAll(): List<KeyEnvelope> = emptyList()
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
        override suspend fun clear() = Unit
    }
}
