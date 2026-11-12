package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.access.model.FreshAuthenticationRequiredException
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationConfig
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationMaterial
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.domain.settings.model.OtpRuntimePolicy
import com.aozijx.passly.domain.settings.port.OtpRuntimePolicySnapshotReader
import com.aozijx.passly.runtime.session.SessionLockedException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext

/**
 * TOTP 状态协调器。
 *
 * ## 安全
 * - Secret 不进入 StateFlow；Runtime 为仍有效的 TOTP/Steam lease 持有可清零种子，HOTP 预览后立即清理
 * - 同一租约跨周期复用种子；每次生成使用独立副本并在 finally 中清零
 * - Session 锁定时清除验证码与调度；页面只释放自己的条目租约
 *
 * ## 性能
 * - ticker 只在验证码时间窗边界或重试时间到达时唤醒
 * - moving factor 变化时复用租约种子重新生成，不重新读取存储
 * - 进度由 Compose 层按时间窗独立动画，StateFlow 不发布逐秒进度
 */
internal class OtpCodeRuntime(
    private val scope: CoroutineScope,
    private val codeGenerator: suspend (OtpGenerationMaterial, Long) -> OtpResult,
    private val loadOtpMaterial: suspend (String) -> OtpGenerationMaterial?,
    private val otpRuntimePolicyReader: OtpRuntimePolicySnapshotReader,
    private val stateDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    private val consumeHotpCode: suspend (String, Long) -> OtpResult = { _, _ ->
        OtpResult.Failure(OtpGenerationError.InvalidSecret)
    },
    private val rollbackHotpCounter: suspend (String, Long) -> Boolean = { _, _ -> false },
    initiallyUnlocked: Boolean = true,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : OtpCodeRuntimePort,
    OtpRuntimeInvalidator {
    private val _states = MutableStateFlow<Map<String, OtpCodeState>>(emptyMap())
    override val states: StateFlow<Map<String, OtpCodeState>> = _states

    /** Cached config contains no seed and is safe to retain for scheduling. */
    private sealed interface OtpSchedule {
        data class Timed(val periodSeconds: Int, val timeStep: Long) : OtpSchedule

        data object Counter : OtpSchedule
    }

    private val schedules = mutableMapOf<String, OtpSchedule>()

    /** One wipeable seed per active timed-OTP lease; never exposed through StateFlow. */
    private val generationMaterials = mutableMapOf<String, OtpGenerationMaterial>()
    private val leases = mutableMapOf<String, MutableSet<Long>>()
    private val activationRecovery = mutableMapOf<String, OtpActivationRecovery>()
    private val lastHotpNextCounters = mutableMapOf<String, Long>()
    private val hotpOperationMutex = Mutex()
    private val tickerWakeups = Channel<Unit>(Channel.CONFLATED)
    private val entryEpochs = mutableMapOf<String, Long>()
    private val activationEpochsInFlight = mutableMapOf<String, Long>()
    private var nextEntryEpoch = 0L
    private var sessionEpoch = 0L
    private var nextLeaseId = 0L
    private var started = false

    private data class ActivationToken(
        val sessionEpoch: Long,
        val entryEpoch: Long,
    )

    @Volatile
    private var sessionUnlocked = initiallyUnlocked

    fun start(signals: OtpRuntimeSignals) {
        if (started) return
        started = true
        scope.launch {
            while (currentCoroutineContext().isActive) {
                val nowMillis = currentTimeMillis()
                if (sessionUnlocked) {
                    retryDueEntries(nowMillis)
                    refreshStates(nowMillis / 1000)
                }
                awaitNextRuntimeChange(nowMillis)
            }
        }
        scope.launch {
            signals.unlocked.distinctUntilChanged().collect(::onSessionStateChanged)
        }
        scope.launch {
            signals.freshAuthentication.collect { onFreshAuthentication() }
        }
    }

    internal suspend fun refreshStates(nowSeconds: Long) {
        for ((entryId, schedule) in schedules.toMap()) {
            if (activationRecovery[entryId] is OtpActivationRecovery.RetryScheduled) continue
            if (activationRecovery[entryId] == OtpActivationRecovery.PermanentlyFailed) continue
            val token = currentActivationToken(entryId) ?: continue
            if (!isCurrent(entryId, token)) continue
            val timedSchedule = schedule as? OtpSchedule.Timed ?: continue

            val movingFactor = nowSeconds / timedSchedule.periodSeconds
            if (movingFactor != timedSchedule.timeStep) {
                val existingMaterial = generationMaterials[entryId]
                val config =
                    existingMaterial ?: try {
                        loadOtpMaterial(entryId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: FreshAuthenticationRequiredException) {
                        if (isCurrent(entryId, token) && schedules[entryId] == schedule) {
                            pauseUntilFreshAuthentication(entryId)
                        }
                        continue
                    } catch (_: SessionLockedException) {
                        if (isCurrent(entryId, token) && schedules[entryId] == schedule) {
                            handleSessionLocked()
                            return
                        }
                        continue
                    } catch (_: Exception) {
                        if (isCurrent(entryId, token) && schedules[entryId] == schedule) {
                            scheduleRetry(entryId)
                        }
                        continue
                    }
                if (!isCurrent(entryId, token)) {
                    if (existingMaterial == null) config?.close()
                    continue
                }
                if (config == null) {
                    schedules.remove(entryId)
                    markPermanentFailure(entryId)
                    _states.update { states ->
                        if (isCurrent(entryId, token)) {
                            states + (entryId to OtpCodeState.Failed(OtpGenerationError.InvalidSecret))
                        } else {
                            states
                        }
                    }
                    continue
                }
                try {
                    val generationMaterial =
                        existingMaterial ?: if (config.config is OtpGenerationConfig.Hotp) {
                            config
                        } else {
                            config.copyForGeneration().also { generationMaterials[entryId] = it }
                        }
                    applyConfig(entryId, generationMaterial, nowSeconds, token)
                } catch (failure: Error) {
                    discardGenerationMaterial(entryId)
                    throw failure
                } finally {
                    if (existingMaterial == null) config.close()
                }
            }
        }
    }

    private suspend fun awaitNextRuntimeChange(nowMillis: Long) {
        val nextWakeupMillis =
            listOfNotNull(
                schedules.values
                    .asSequence()
                    .filterIsInstance<OtpSchedule.Timed>()
                    .map { schedule -> (schedule.timeStep + 1) * schedule.periodSeconds * 1000L }
                    .minOrNull(),
                activationRecovery.values
                    .filterIsInstance<OtpActivationRecovery.RetryScheduled>()
                    .minOfOrNull(OtpActivationRecovery.RetryScheduled::atMillis),
            ).minOrNull()
        if (nextWakeupMillis == null) {
            tickerWakeups.receive()
        } else {
            val waitMillis =
                (nextWakeupMillis - nowMillis)
                    .coerceIn(1L, MAX_CLOCK_CORRECTION_INTERVAL_MILLIS)
            withTimeoutOrNull(waitMillis) { tickerWakeups.receive() }
        }
    }

    private fun wakeTicker() {
        tickerWakeups.trySend(Unit)
    }

    private suspend fun generateUiState(
        material: OtpGenerationMaterial,
        nowSeconds: Long,
        period: Int,
    ): OtpCodeState =
        when (val genResult = codeGenerator(material, nowSeconds)) {
            is OtpResult.Success -> {
                val nextResult = codeGenerator(material, (nowSeconds / period + 1) * period)
                when (nextResult) {
                    is OtpResult.Success -> {
                        OtpCodeState.TimedReady(
                            code = genResult.code,
                            nextCode = nextResult.code,
                            windowEndMillis = (nowSeconds / period + 1) * period * 1000L,
                            periodSeconds = period,
                        )
                    }

                    is OtpResult.Failure -> {
                        OtpCodeState.Failed(nextResult.error)
                    }

                    is OtpResult.CounterSuccess -> OtpCodeState.Failed(OtpGenerationError.InvalidCounter)
                }
            }

            is OtpResult.Failure -> {
                OtpCodeState.Failed(genResult.error)
            }

            is OtpResult.CounterSuccess -> OtpCodeState.Failed(OtpGenerationError.InvalidCounter)
        }

    private fun retryable(error: OtpGenerationError): Boolean = error is OtpGenerationError.CryptoError

    private fun recordStateOutcome(
        entryId: String,
        state: OtpCodeState,
    ) {
        when (state) {
            is OtpCodeState.Failed -> {
                if (retryable(state.error)) {
                    scheduleRetry(entryId)
                } else {
                    markPermanentFailure(entryId)
                }
            }

            else -> {
                transitionRecovery(entryId, OtpActivationRecoveryEvent.Recovered)
            }
        }
    }

    private fun scheduleRetry(entryId: String) {
        if (!isRequested(entryId) || isAwaitingFreshAuthentication(entryId) || !sessionUnlocked) return
        schedules.remove(entryId)
        transitionRecovery(
            entryId,
            OtpActivationRecoveryEvent.RetryableFailure(currentTimeMillis()),
        )
        _states.update { states -> states + (entryId to OtpCodeState.Pending) }
        wakeTicker()
    }

    private fun transitionRecovery(
        entryId: String,
        event: OtpActivationRecoveryEvent,
    ) {
        val next = OtpActivationRecoveryReducer.reduce(
            state = activationRecovery[entryId] ?: OtpActivationRecovery.Active,
            event = event,
        )
        if (next == OtpActivationRecovery.Active) {
            activationRecovery.remove(entryId)
        } else {
            activationRecovery[entryId] = next
        }
    }

    private fun isAwaitingFreshAuthentication(entryId: String): Boolean =
        activationRecovery[entryId] == OtpActivationRecovery.AwaitingFreshAuthentication

    private fun isPermanentlyFailed(entryId: String): Boolean =
        activationRecovery[entryId] == OtpActivationRecovery.PermanentlyFailed

    private fun markPermanentFailure(entryId: String) {
        schedules.remove(entryId)
        transitionRecovery(entryId, OtpActivationRecoveryEvent.PermanentFailure)
        discardGenerationMaterial(entryId)
        wakeTicker()
    }

    private fun discardGenerationMaterial(entryId: String) {
        generationMaterials.remove(entryId)?.close()
    }

    private fun retryDueEntries(nowMillis: Long) {
        activationRecovery
            .filter { (entryId, state) ->
                state is OtpActivationRecovery.RetryScheduled && state.atMillis <= nowMillis && isRequested(entryId)
            }
            .keys
            .toList()
            .forEach { entryId ->
                transitionRecovery(entryId, OtpActivationRecoveryEvent.RetryElapsed)
                schedules.remove(entryId)
                scope.launch { activateTrackedEntry(entryId) }
            }
    }

    /**
     * 生成 HOTP 验证码（用户主动触发）。
     */
    private suspend fun generateHotpCode(entryId: String): OtpResult {
        if (!sessionUnlocked) return OtpResult.Failure(OtpGenerationError.InvalidSecret)
        val token =
            currentActivationToken(entryId)
                ?: return OtpResult.Failure(OtpGenerationError.InvalidSecret)
        return try {
            hotpOperationMutex.withLock {
                if (!isCurrent(entryId, token)) return@withLock OtpResult.Failure(OtpGenerationError.InvalidSecret)
                val preview = states.value[entryId] as? OtpCodeState.HotpReady
                    ?: return@withLock OtpResult.Failure(OtpGenerationError.InvalidSecret)
                when (val result = consumeHotpCode(entryId, preview.counter)) {
                    is OtpResult.CounterSuccess -> {
                        val nextCounter = result.nextCounter
                        if (!isCurrent(entryId, token)) return@withLock OtpResult.Failure(OtpGenerationError.InvalidSecret)
                        lastHotpNextCounters[entryId] = nextCounter
                        _states.update { it + (entryId to OtpCodeState.Pending) }
                        schedules.remove(entryId)
                        wakeTicker()
                        activateTrackedEntry(entryId)
                        result
                    }

                    is OtpResult.Failure -> {
                        if (result.error == OtpGenerationError.CounterChanged && isCurrent(entryId, token)) {
                            _states.update { it + (entryId to OtpCodeState.Pending) }
                            schedules.remove(entryId)
                            wakeTicker()
                            activateTrackedEntry(entryId)
                        } else {
                            _states.update {
                                it +
                                    (entryId to
                                        OtpCodeState.Failed(
                                            error = result.error,
                                            canRollbackPrevious = lastHotpNextCounters[entryId] != null,
                                        ))
                            }
                        }
                        result
                    }

                    is OtpResult.Success -> OtpResult.Failure(OtpGenerationError.InvalidCounter)
                }
            }
        } catch (_: FreshAuthenticationRequiredException) {
            if (isCurrent(entryId, token)) pauseUntilFreshAuthentication(entryId)
            OtpResult.Failure(OtpGenerationError.InvalidSecret)
        } catch (_: SessionLockedException) {
            if (isCurrent(entryId, token)) handleSessionLocked()
            OtpResult.Failure(OtpGenerationError.InvalidSecret)
        }
    }

    override suspend fun rollbackLastHotpCounter(entryId: String): Boolean {
        if (!sessionUnlocked) return false
        val token = currentActivationToken(entryId) ?: return false
        return try {
            hotpOperationMutex.withLock {
                if (!isCurrent(entryId, token)) return@withLock false
                val expectedCurrentCounter = lastHotpNextCounters[entryId] ?: return@withLock false
                val rolledBack = rollbackHotpCounter(entryId, expectedCurrentCounter)
                if (!isCurrent(entryId, token)) return@withLock false
                lastHotpNextCounters.remove(entryId)
                _states.update { it + (entryId to OtpCodeState.Pending) }
                schedules.remove(entryId)
                wakeTicker()
                activateTrackedEntry(entryId)
                rolledBack
            }
        } catch (_: FreshAuthenticationRequiredException) {
            if (isCurrent(entryId, token)) pauseUntilFreshAuthentication(entryId)
            false
        } catch (_: SessionLockedException) {
            if (isCurrent(entryId, token)) handleSessionLocked()
            false
        }
    }

    /** Returns a fresh, consumed HOTP code or the current time-based code for authorized copy. */
    override suspend fun codeForCopy(entryId: String): String? {
        if (!sessionUnlocked) return null
        val copyToken = currentActivationToken(entryId)
        val copySessionEpoch = sessionEpoch
        return try {
            var policySnapshot: OtpRuntimePolicy? = null
            val nowSeconds = currentTimeMillis() / 1000
            val schedule = schedules[entryId]
            if (schedule is OtpSchedule.Counter) {
                return (generateHotpCode(entryId) as? OtpResult.CounterSuccess)?.code
            }
            val ready = states.value[entryId] as? OtpCodeState.TimedReady
            val timedSchedule = schedule as? OtpSchedule.Timed
            if (
                ready != null && timedSchedule != null &&
                nowSeconds / timedSchedule.periodSeconds == timedSchedule.timeStep
            ) {
                val policy = otpRuntimePolicyReader.readOtpRuntimePolicySnapshot().also { policySnapshot = it }
                val decisionSeconds = currentTimeMillis() / 1000
                if (decisionSeconds / timedSchedule.periodSeconds == timedSchedule.timeStep) {
                    val code =
                        if (shouldCopyNextWindow(decisionSeconds, timedSchedule.periodSeconds, policy)) {
                            ready.nextCode
                        } else {
                            ready.code
                        }
                    return code.takeIf { isCopyRequestCurrent(entryId, copyToken, copySessionEpoch) }
                }
            }

            val cachedMaterial = generationMaterials[entryId]
            val material = cachedMaterial ?: loadOtpMaterial(entryId) ?: return null
            try {
                if (!sessionUnlocked) return null
                val generationConfig = material.config
                if (generationConfig is OtpGenerationConfig.Hotp) {
                    val expectedCounter = generationConfig.counter
                    val result = consumeHotpCode(entryId, expectedCounter)
                    if (!isCopyRequestCurrent(entryId, copyToken, copySessionEpoch)) return null
                    return (result as? OtpResult.CounterSuccess)?.code
                }
                val period =
                    when (generationConfig) {
                        is OtpGenerationConfig.Timed -> generationConfig.periodSeconds
                        is OtpGenerationConfig.Hotp -> return null
                    }
                val policy = policySnapshot ?: otpRuntimePolicyReader.readOtpRuntimePolicySnapshot()
                val generationMaterial = if (cachedMaterial != null) material.copyForGeneration() else material
                val code =
                    try {
                        generateCodeForCopy(generationMaterial, period, policy)
                    } finally {
                        if (cachedMaterial != null) generationMaterial.close()
                    } ?: return null
                return code.takeIf { isCopyRequestCurrent(entryId, copyToken, copySessionEpoch) }
            } finally {
                if (cachedMaterial == null) material.close()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: FreshAuthenticationRequiredException) {
            if (copyToken != null && isCurrent(entryId, copyToken)) pauseUntilFreshAuthentication(entryId)
            null
        } catch (_: SessionLockedException) {
            if (sessionEpoch == copySessionEpoch) handleSessionLocked()
            null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun generateCodeForCopy(
        material: OtpGenerationMaterial,
        period: Int,
        policy: OtpRuntimePolicy,
    ): String? {
        repeat(MAX_COPY_WINDOW_ATTEMPTS) {
            val nowSeconds = currentTimeMillis() / 1000
            val current = codeGenerator(material, nowSeconds) as? OtpResult.Success ?: return null
            val code =
                if (shouldCopyNextWindow(nowSeconds, period, policy)) {
                    val next = codeGenerator(material, (nowSeconds / period + 1) * period) as? OtpResult.Success
                    next?.code ?: current.code
                } else {
                    current.code
                }
            if (currentTimeMillis() / 1000 / period == nowSeconds / period) return code
        }
        return null
    }

    private fun isCopyRequestCurrent(
        entryId: String,
        token: ActivationToken?,
        expectedSessionEpoch: Long,
    ): Boolean =
        sessionUnlocked &&
            sessionEpoch == expectedSessionEpoch &&
            (token == null || isCurrent(entryId, token))

    private fun shouldCopyNextWindow(
        nowSeconds: Long,
        period: Int,
        policy: OtpRuntimePolicy,
    ): Boolean = policy.copyNextTotpNearExpiry && period - (nowSeconds % period) <= 3

    private suspend fun activateTrackedEntry(entryId: String) {
        if (!sessionUnlocked || !isRequested(entryId)) return
        val currentEpoch = entryEpochs[entryId]
        if (currentEpoch != null && activationEpochsInFlight[entryId] == currentEpoch) return
        val token =
            ActivationToken(sessionEpoch, ++nextEntryEpoch).also {
                entryEpochs[entryId] = it.entryEpoch
            }
        activationEpochsInFlight[entryId] = token.entryEpoch
        try {
            val existingMaterial = generationMaterials[entryId]
            val config =
                existingMaterial ?: try {
                    loadOtpMaterial(entryId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: FreshAuthenticationRequiredException) {
                    if (isCurrent(entryId, token)) pauseUntilFreshAuthentication(entryId)
                    return
                } catch (_: SessionLockedException) {
                    if (isCurrent(entryId, token)) handleSessionLocked()
                    return
                } catch (_: Exception) {
                    if (isCurrent(entryId, token)) {
                        schedules.remove(entryId)
                        scheduleRetry(entryId)
                    }
                    return
                }
            if (!isCurrent(entryId, token)) {
                if (existingMaterial == null) config?.close()
                return
            }
            if (config == null) {
                markPermanentFailure(entryId)
                _states.update {
                    it + (entryId to OtpCodeState.Failed(OtpGenerationError.InvalidSecret))
                }
                return
            }
            val nowSeconds = currentTimeMillis() / 1000
            try {
                val generationMaterial =
                    existingMaterial ?: if (config.config is OtpGenerationConfig.Hotp) {
                        config
                    } else {
                        config.copyForGeneration().also { generationMaterials[entryId] = it }
                    }
                applyConfig(entryId, generationMaterial, nowSeconds, token)
            } catch (failure: Error) {
                discardGenerationMaterial(entryId)
                throw failure
            } finally {
                if (existingMaterial == null) config.close()
            }
        } finally {
            if (activationEpochsInFlight[entryId] == token.entryEpoch) {
                activationEpochsInFlight.remove(entryId)
            }
        }
    }

    private suspend fun applyConfig(
        entryId: String,
        material: OtpGenerationMaterial,
        nowSeconds: Long,
        token: ActivationToken,
    ) {
        val config = material.config
        if (config is OtpGenerationConfig.Hotp) {
            if (!isCurrent(entryId, token)) return
            val counter = config.counter
            val canRollbackPrevious = lastHotpNextCounters[entryId] == counter
            if (!canRollbackPrevious) lastHotpNextCounters.remove(entryId)
            val state =
                when (val result = codeGenerator(material, counter)) {
                    is OtpResult.Success ->
                        OtpCodeState.HotpReady(
                            code = result.code,
                            counter = counter,
                            canRollbackPrevious = canRollbackPrevious,
                        )

                    is OtpResult.CounterSuccess ->
                        OtpCodeState.HotpReady(
                            code = result.code,
                            counter = counter,
                            canRollbackPrevious = canRollbackPrevious,
                        )

                    is OtpResult.Failure ->
                        OtpCodeState.Failed(
                            error = result.error,
                            canRollbackPrevious = canRollbackPrevious,
                        )
                }
            if (!isCurrent(entryId, token)) return
            schedules[entryId] = OtpSchedule.Counter
            _states.update { it + (entryId to state) }
            recordStateOutcome(entryId, state)
            wakeTicker()
            return
        }

        val period =
            when (config) {
                is OtpGenerationConfig.Timed -> config.periodSeconds
                is OtpGenerationConfig.Hotp -> return
            }
        val schedule = OtpSchedule.Timed(periodSeconds = period, timeStep = nowSeconds / period)
        val generationCopy = material.copyForGeneration()
        val state =
            try {
                generateUiState(generationCopy, nowSeconds, period)
            } finally {
                generationCopy.close()
            }
        if (!isCurrent(entryId, token)) return
        schedules[entryId] = schedule
        _states.update { it + (entryId to state) }
        recordStateOutcome(entryId, state)
        wakeTicker()
    }

    override fun acquire(entryId: String): OtpLease {
        val wasRequested = isRequested(entryId)
        val leaseId = ++nextLeaseId
        leases.getOrPut(entryId, ::mutableSetOf).add(leaseId)
        val wasAwaitingAuthentication = isAwaitingFreshAuthentication(entryId)
        if (wasAwaitingAuthentication) {
            transitionRecovery(entryId, OtpActivationRecoveryEvent.FreshAuthenticationCompleted)
        }
        wakeTicker()
        if ((!wasRequested || wasAwaitingAuthentication) && !isPermanentlyFailed(entryId) && sessionUnlocked) {
            scope.launch { activateTrackedEntry(entryId) }
        }
        val closed = AtomicBoolean(false)
        return OtpLease {
            if (closed.compareAndSet(false, true)) {
                releaseLease(entryId, leaseId)
            }
        }
    }

    private fun releaseLease(
        entryId: String,
        leaseId: Long,
    ) {
        val owners = leases[entryId] ?: return
        if (!owners.remove(leaseId) || owners.isNotEmpty()) return
        leases.remove(entryId)
        lastHotpNextCounters.remove(entryId)
        entryEpochs.remove(entryId)
        activationEpochsInFlight.remove(entryId)
        activationRecovery.remove(entryId)
        schedules.remove(entryId)
        discardGenerationMaterial(entryId)
        _states.update { it - entryId }
        wakeTicker()
    }

    private fun isRequested(entryId: String): Boolean = !leases[entryId].isNullOrEmpty()

    private fun currentActivationToken(entryId: String): ActivationToken? = entryEpochs[entryId]?.let { ActivationToken(sessionEpoch, it) }

    private fun isCurrent(
        entryId: String,
        token: ActivationToken,
    ): Boolean =
        sessionUnlocked &&
            isRequested(entryId) &&
            sessionEpoch == token.sessionEpoch &&
            entryEpochs[entryId] == token.entryEpoch

    /**
     * 锁定时立即清除验证码、种子和调度状态，但保留待恢复的条目 ID。
     * 解锁后重新从数据库加载配置；每个租约只需完成一次种子读取。
     */
    fun onSessionStateChanged(unlocked: Boolean) {
        if (sessionUnlocked == unlocked) return
        sessionUnlocked = unlocked
        if (!unlocked) {
            sessionEpoch++
            entryEpochs.clear()
            activationEpochsInFlight.clear()
            clearGeneratedState()
        } else {
            activationRecovery
                .filterValues { it == OtpActivationRecovery.AwaitingFreshAuthentication }
                .keys
                .toList()
                .forEach { entryId ->
                    transitionRecovery(entryId, OtpActivationRecoveryEvent.FreshAuthenticationCompleted)
                }
            scope.launch { reactivatePendingEntries() }
        }
        wakeTicker()
    }

    private suspend fun reactivatePendingEntries() {
        if (!sessionUnlocked) return
        leases.keys
            .filterNot(schedules::containsKey)
            .filterNot(::isAwaitingFreshAuthentication)
            .filterNot(::isPermanentlyFailed)
            .filterNot { activationRecovery[it] is OtpActivationRecovery.RetryScheduled }
            .toList()
            .forEach { entryId ->
                if (!sessionUnlocked) return
                activateTrackedEntry(entryId)
            }
    }

    private fun handleSessionLocked() {
        sessionUnlocked = false
        sessionEpoch++
        entryEpochs.clear()
        activationEpochsInFlight.clear()
        clearGeneratedState()
    }

    private fun clearGeneratedState() {
        generationMaterials.values.forEach(OtpGenerationMaterial::close)
        generationMaterials.clear()
        schedules.clear()
        activationRecovery.entries.removeAll { (_, state) ->
            state != OtpActivationRecovery.AwaitingFreshAuthentication
        }
        _states.value = emptyMap()
        wakeTicker()
    }

    fun onFreshAuthentication() {
        if (!sessionUnlocked || activationRecovery.values.none { it == OtpActivationRecovery.AwaitingFreshAuthentication }) return
        activationRecovery
            .filterValues { it == OtpActivationRecovery.AwaitingFreshAuthentication }
            .keys
            .toList()
            .forEach { entryId ->
                transitionRecovery(entryId, OtpActivationRecoveryEvent.FreshAuthenticationCompleted)
            }
        scope.launch { reactivatePendingEntries() }
    }

    private fun pauseUntilFreshAuthentication(entryId: String) {
        schedules.remove(entryId)
        transitionRecovery(entryId, OtpActivationRecoveryEvent.FreshAuthenticationRequired)
        _states.update { it - entryId }
        wakeTicker()
    }

    override fun entryRemoved(entryId: String) {
        lastHotpNextCounters.remove(entryId)
        entryEpochs.remove(entryId)
        leases.remove(entryId)
        activationEpochsInFlight.remove(entryId)
        schedules.remove(entryId)
        discardGenerationMaterial(entryId)
        activationRecovery.remove(entryId)
        _states.update { it - entryId }
        wakeTicker()
    }

    override fun entryChanged(entryId: String) {
        lastHotpNextCounters.remove(entryId)
        val wasRequested = isRequested(entryId)
        entryEpochs.remove(entryId)
        activationEpochsInFlight.remove(entryId)
        activationRecovery.remove(entryId)
        schedules.remove(entryId)
        discardGenerationMaterial(entryId)
        _states.update { it - entryId }
        wakeTicker()
        if (wasRequested && sessionUnlocked) scope.launch { activateTrackedEntry(entryId) }
    }

    override suspend fun vaultReplaced() = withContext(NonCancellable + stateDispatcher) {
        leases.keys.toList().forEach(::entryChanged)
    }

    private companion object {
        const val MAX_COPY_WINDOW_ATTEMPTS = 2
        const val MAX_CLOCK_CORRECTION_INTERVAL_MILLIS = 30_000L
    }
}

internal data class OtpRuntimeSignals(
    val unlocked: Flow<Boolean>,
    val freshAuthentication: Flow<Unit>,
) {
    companion object {
        val NONE =
            OtpRuntimeSignals(
                unlocked = emptyFlow(),
                freshAuthentication = emptyFlow(),
            )
    }
}
