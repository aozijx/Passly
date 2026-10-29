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
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.DekState
import com.aozijx.passly.security.dek.FieldKeyManager
import com.aozijx.passly.security.dek.SensitiveDataKeyManager
import com.aozijx.passly.security.autofill.inbox.AutofillInboxKeyProvisioning
import com.aozijx.passly.security.lock.LockStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun `cancelling a sealed lock completes key cleanup and reports cancellation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val logger = RecordingLogger()
            val bootstrap = EmptyVaultBootstrapStore()
            val dekManager = DekManager(bootstrap, FieldKeyManager(), logger)
            dekManager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 7 })
            val databaseSession = YieldingDatabaseSession(pauseSeal = true)
            val lockState = LockStateManager().apply { mark(SecureSessionState.UNLOCKED) }
            val controller = VaultSessionController(
                dekManager = dekManager,
                sensitiveDataKeyManager = SensitiveDataKeyManager(
                    bootstrap,
                    dekManager,
                    CoroutineScope(dispatcher),
                ),
                authorizationPermitRevoker = { },
                sessionManager = databaseSession,
                vaultBootstrapStore = bootstrap,
                lockStateManager = lockState,
                securityEvents = SecuritySessionEventLogger(logger),
                idleTimeoutSettings = object : IdleTimeoutSettings {
                    override val isLockOnBackground = MutableStateFlow(false)
                    override val lockTimeout = MutableStateFlow(1_000L)
                },
                autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning { },
            )
            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))

            val lockJob = launch(dispatcher) {
                controller.lock(SessionLockRequest(LockReason.APP_EXIT, LockTrigger.APP_EXIT))
            }
            runCurrent()
            databaseSession.sealStarted.await()

            lockJob.cancel()
            databaseSession.allowSealToFinish.complete(Unit)
            runCurrent()
            lockJob.join()

            assertTrue(dekManager.state is DekState.Locked)
            assertEquals(SecureSessionState.SEALED, lockState.state)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCancelled })
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockFailed })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `seal cancellation completes lock cleanup then propagates cancellation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val cancelled = CancellationException("database seal cancelled")
            val logger = RecordingLogger()
            val bootstrap = EmptyVaultBootstrapStore()
            val dekManager = DekManager(bootstrap, FieldKeyManager(), logger)
            dekManager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 7 })
            val databaseSession = YieldingDatabaseSession(sealFailure = cancelled)
            val lockState = LockStateManager().apply { mark(SecureSessionState.UNLOCKED) }
            val controller = VaultSessionController(
                dekManager = dekManager,
                sensitiveDataKeyManager = SensitiveDataKeyManager(
                    bootstrap,
                    dekManager,
                    CoroutineScope(dispatcher),
                ),
                authorizationPermitRevoker = { },
                sessionManager = databaseSession,
                vaultBootstrapStore = bootstrap,
                lockStateManager = lockState,
                securityEvents = SecuritySessionEventLogger(logger),
                idleTimeoutSettings = object : IdleTimeoutSettings {
                    override val isLockOnBackground = MutableStateFlow(false)
                    override val lockTimeout = MutableStateFlow(1_000L)
                },
                autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning { },
            )
            controller.transition(AuthenticationState.Authenticated(0L))

            val thrown = try {
                controller.lock(SessionLockRequest(LockReason.APP_EXIT, LockTrigger.APP_EXIT))
                null
            } catch (failure: CancellationException) {
                failure
            }

            assertTrue(thrown is CancellationException)
            assertEquals(cancelled.message, thrown?.message)
            assertEquals(1, databaseSession.sealCount)
            assertTrue(dekManager.state is DekState.Locked)
            assertEquals(SecureSessionState.SEALED, lockState.state)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCancelled })
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockFailed })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `soft lock cancellation completes state transition then propagates cancellation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val cancelled = CancellationException("database soft lock cancelled")
            val logger = RecordingLogger()
            val databaseSession = YieldingDatabaseSession(softLockFailure = cancelled)
            val controller = createController(
                dispatcher = dispatcher,
                databaseSession = databaseSession,
                timeout = MutableStateFlow(1_000L),
                lockOnBackground = MutableStateFlow(false),
                logger = logger,
            )
            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))

            val thrown = try {
                controller.lock(SessionLockRequest(LockReason.USER, LockTrigger.USER_ACTION))
                null
            } catch (failure: CancellationException) {
                failure
            }

            assertTrue(thrown is CancellationException)
            assertEquals(cancelled.message, thrown?.message)
            assertTrue(databaseSession.softLockCompleted)
            assertEquals(SecureSessionState.SOFT_LOCKED, databaseSession.lockState)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCancelled })
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockFailed })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `disabling background lock preserves the existing idle timeout`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val databaseSession = YieldingDatabaseSession()
            val controller = createController(
                dispatcher = dispatcher,
                databaseSession = databaseSession,
                timeout = MutableStateFlow(1_000L),
                lockOnBackground = MutableStateFlow(false),
            )

            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))
            controller.onAppForeground()
            advanceTimeBy(500L)
            controller.onAppBackground()
            advanceTimeBy(500L)
            runCurrent()

            assertTrue(databaseSession.softLockCompleted)
            assertEquals(0, databaseSession.sealCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `background lock waits for configured timeout and is cancelled on foreground return`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val databaseSession = YieldingDatabaseSession()
            val controller = createController(
                dispatcher = dispatcher,
                databaseSession = databaseSession,
                timeout = MutableStateFlow(1_000L),
                lockOnBackground = MutableStateFlow(true),
            )

            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))
            controller.onAppBackground()
            advanceTimeBy(999L)
            runCurrent()

            assertEquals(0, databaseSession.sealCount)

            controller.onAppForeground()
            advanceTimeBy(1L)
            runCurrent()

            assertEquals(0, databaseSession.sealCount)
            advanceUntilIdle()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `foreground return cannot cancel a background seal after timeout elapsed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val logger = RecordingLogger()
            val databaseSession = YieldingDatabaseSession(pauseSeal = true)
            val controller = createController(
                dispatcher = dispatcher,
                databaseSession = databaseSession,
                timeout = MutableStateFlow(1_000L),
                lockOnBackground = MutableStateFlow(true),
                logger = logger,
            )

            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))
            controller.onAppBackground()
            advanceTimeBy(1_000L)
            runCurrent()
            databaseSession.sealStarted.await()

            controller.onAppForeground()
            databaseSession.allowSealToFinish.complete(Unit)
            runCurrent()

            assertEquals(1, databaseSession.sealCount)
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockFailed })
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCompleted })
            advanceUntilIdle()
        } finally {
            Dispatchers.resetMain()
        }
    }

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
                autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning { },
            )

            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))
            controller.onAppForeground()
            controller.onUserInteraction()
            advanceTimeBy(1L)
            runCurrent()

            assertTrue(databaseSession.softLockCompleted)
            assertEquals(SecureSessionState.SOFT_LOCKED, lockState.state)
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockFailed })
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCompleted })
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createController(
        dispatcher: CoroutineDispatcher,
        databaseSession: DatabaseSessionLifecycle,
        timeout: MutableStateFlow<Long>,
        lockOnBackground: MutableStateFlow<Boolean>,
        logger: RecordingLogger = RecordingLogger(),
        provisionInboxKey: suspend () -> Unit = {},
    ): VaultSessionController {
        val bootstrap = EmptyVaultBootstrapStore()
        val dekManager = DekManager(bootstrap, FieldKeyManager(), logger)
        val sensitiveKeyManager = SensitiveDataKeyManager(bootstrap, dekManager, CoroutineScope(dispatcher))
        val lockState = LockStateManager().apply { mark(SecureSessionState.UNLOCKED) }
        return VaultSessionController(
            dekManager = dekManager,
            sensitiveDataKeyManager = sensitiveKeyManager,
            authorizationPermitRevoker = { },
            sessionManager = databaseSession,
            vaultBootstrapStore = bootstrap,
            lockStateManager = lockState,
            securityEvents = SecuritySessionEventLogger(logger),
            idleTimeoutSettings = object : IdleTimeoutSettings {
                override val isLockOnBackground = lockOnBackground
                override val lockTimeout = timeout
            },
            autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning(provisionInboxKey),
        )
    }

    @Test
    fun `only a full Vault unlock provisions the Autofill Inbox key`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            var provisionCount = 0
            val fullUnlockController = createController(
                dispatcher = dispatcher,
                databaseSession = YieldingDatabaseSession(),
                timeout = MutableStateFlow(1_000L),
                lockOnBackground = MutableStateFlow(false),
                provisionInboxKey = { provisionCount++ },
            )
            val autofillController = createController(
                dispatcher = dispatcher,
                databaseSession = YieldingDatabaseSession(),
                timeout = MutableStateFlow(1_000L),
                lockOnBackground = MutableStateFlow(false),
                provisionInboxKey = { provisionCount++ },
            )

            assertTrue(fullUnlockController.markAuthenticated(AuthenticationPurpose.UNLOCK_VAULT))
            assertEquals(1, provisionCount)
            assertTrue(autofillController.markAuthenticated(AuthenticationPurpose.AUTOFILL))
            assertEquals(1, provisionCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class YieldingDatabaseSession(
        private val pauseSeal: Boolean = false,
        private val sealFailure: Throwable? = null,
        private val softLockFailure: Throwable? = null,
    ) : DatabaseSessionLifecycle {
        override var lockState: SecureSessionState = SecureSessionState.UNLOCKED
        var softLockCompleted = false
        var sealCount = 0
        val sealStarted = CompletableDeferred<Unit>()
        val allowSealToFinish = CompletableDeferred<Unit>()

        override suspend fun unlock(): Throwable? = null

        override suspend fun softLock() {
            lockState = SecureSessionState.SOFT_LOCKED
            yield()
            softLockCompleted = true
            softLockFailure?.let { throw it }
        }

        override suspend fun seal() {
            sealStarted.complete(Unit)
            if (pauseSeal) allowSealToFinish.await()
            sealCount += 1
            lockState = SecureSessionState.SEALED
            sealFailure?.let { throw it }
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
