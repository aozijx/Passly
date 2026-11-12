package com.aozijx.passly.security.session

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
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.security.autofill.inbox.AutofillInboxKeyProvisioning
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.FieldKeyManager
import com.aozijx.passly.security.dek.SensitiveDataKeyClearer
import com.aozijx.passly.security.dek.SensitiveDataKeyManager
import com.aozijx.passly.security.lock.LockStateManager
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals

internal fun assertVaultSessionCancellation(
    actual: CancellationException?,
    expected: CancellationException,
) {
    assertEquals(expected::class, actual!!::class)
    assertEquals(expected.message, actual.message)
}

internal fun createVaultSessionTestController(
    dispatcher: CoroutineDispatcher,
    databaseSession: DatabaseSessionLifecycle,
    timeout: MutableStateFlow<Long>,
    lockOnBackground: MutableStateFlow<Boolean>,
    logger: VaultSessionTestLogger = VaultSessionTestLogger(),
    provisionInboxKey: suspend () -> Unit = {},
    revokeAuthorization: () -> Unit = {},
    clearSensitiveKey: SensitiveDataKeyClearer? = null,
): VaultSessionController {
    val bootstrap = VaultSessionTestBootstrapStore()
    val dekManager = DekManager(bootstrap, FieldKeyManager(), logger)
    val sensitiveDataKeyClearer =
        clearSensitiveKey
            ?: SensitiveDataKeyManager(bootstrap, dekManager, CoroutineScope(dispatcher), SecureRandom())
    val lockState = LockStateManager().apply { mark(SecureSessionState.UNLOCKED) }
    return VaultSessionController(
        dekManager = dekManager,
        sensitiveDataKeyClearer = sensitiveDataKeyClearer,
        authorizationPermitRevoker = revokeAuthorization,
        sessionManager = databaseSession,
        vaultBootstrapStore = bootstrap,
        lockStateManager = lockState,
        securityEvents = SecuritySessionEventLogger(logger),
        idleTimeoutSettings =
            object : IdleTimeoutSettings {
                override val isLockOnBackground = lockOnBackground
                override val lockTimeout = timeout
            },
        autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning(provisionInboxKey),
    )
}

internal class VaultSessionTestDatabaseSession(
    private val pauseSeal: Boolean = false,
    private val sealFailure: Throwable? = null,
    private val softLockFailure: Throwable? = null,
    private val pauseSoftLock: Boolean = false,
    private val pauseUnlock: Boolean = false,
    private val unlockFailure: Throwable? = null,
    private val sealResult: Result<Unit> = Result.success(Unit),
    initialLockState: SecureSessionState = SecureSessionState.UNLOCKED,
) : DatabaseSessionLifecycle {
    override var lockState: SecureSessionState = initialLockState
    var unlockResultError: Throwable? = null
    var softLockCompleted = false
    var sealCount = 0
    var unlockCalls = 0
    val sealStarted = CompletableDeferred<Unit>()
    val allowSealToFinish = CompletableDeferred<Unit>()
    val softLockStarted = CompletableDeferred<Unit>()
    val allowSoftLockToFinish = CompletableDeferred<Unit>()
    val unlockStarted = CompletableDeferred<Unit>()
    val allowUnlockToFinish = CompletableDeferred<Unit>()

    override suspend fun unlock(): Throwable? {
        unlockCalls += 1
        unlockStarted.complete(Unit)
        if (pauseUnlock) allowUnlockToFinish.await()
        unlockFailure?.let { throw it }
        unlockResultError?.let { return it }
        lockState = SecureSessionState.UNLOCKED
        return null
    }

    override suspend fun softLock() {
        lockState = SecureSessionState.SOFT_LOCKED
        softLockStarted.complete(Unit)
        if (pauseSoftLock) allowSoftLockToFinish.await()
        yield()
        softLockCompleted = true
        softLockFailure?.let { throw it }
    }

    override suspend fun seal(): Result<Unit> {
        sealStarted.complete(Unit)
        if (pauseSeal) allowSealToFinish.await()
        sealCount += 1
        lockState = SecureSessionState.SEALED
        sealFailure?.let { throw it }
        return sealResult
    }
}

internal class VaultSessionTestLogger : Logger {
    val infoEvents = mutableListOf<EventKey>()
    val errorEvents = mutableListOf<EventKey>()

    override fun debug(
        event: EventKey,
        fields: LogFields,
    ) = Unit

    override fun info(
        event: EventKey,
        fields: LogFields,
    ) {
        infoEvents += event
    }

    override fun degraded(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        fields: LogFields,
    ) = Unit

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
    ) {
        errorEvents += event
    }

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
    ): T = VaultSessionTestTraceScope.block()

    override suspend fun <T> inSpan(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T =
        withContext(CoroutineName("test-span")) {
            VaultSessionTestTraceScope.block()
        }

    override fun captureTraceToken(singleUse: Boolean): TraceToken = error("not used")

    override suspend fun <T> resumeTrace(
        token: TraceToken,
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): TraceResumeResult<T> = error("not used")
}

internal object VaultSessionTestTraceScope : TraceScope {
    override val traceId = TraceId("test_trace")
    override val spanId = SpanId("test_span")
    override val parentSpanId: SpanId? = null
}

internal class VaultSessionTestBootstrapStore : VaultBootstrapStore {
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

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runMainTest(block: suspend TestScope.(TestDispatcher) -> Unit) =
    runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            block(dispatcher)
        } finally {
            Dispatchers.resetMain()
        }
    }
