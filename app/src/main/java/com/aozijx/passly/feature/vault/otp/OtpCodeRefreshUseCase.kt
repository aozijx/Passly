package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.otp.OtpResult
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.runtime.session.SessionLockedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * TOTP 状态协调器。
 *
 * ## 安全
 * - Secret 不进入 StateFlow，也不在协调器中缓存
 * - 每个验证码时间窗通过 [loadOtpConfig] 从加密 Credential 重新读取完整配置
 * - [clearAllSensitiveState] 在锁定、ViewModel 清理时调用
 *
 * ## 性能
 * - ticker 对齐到整秒边界（每秒一次）
 * - 每秒只更新进度，仅在 moving factor 变化时查询数据库并重新生成验证码
 * - 进度由 Compose 层独立动画，StateFlow 仅保存刷新时刻的快照值
 */
internal class OtpCodeRefreshUseCase(
    private val scope: CoroutineScope,
    private val codeGenerator: suspend (OtpConfig) -> OtpResult,
    private val loadOtpConfig: suspend (String) -> OtpConfig?,
    initiallyUnlocked: Boolean = true
) {
    private val _states = MutableStateFlow<Map<String, OtpCodeState>>(emptyMap())
    val states: StateFlow<Map<String, OtpCodeState>> = _states

    /** 仅缓存非敏感调度信息，不缓存 Secret。 */
    private data class OtpSchedule(
        val type: OtpType,
        val periodSeconds: Int,
        val movingFactor: Long
    )

    private val schedules = mutableMapOf<String, OtpSchedule>()
    private val activeEntryIds = mutableSetOf<String>()
    private val subscriptionCounts = mutableMapOf<String, Int>()
    private val entryEpochs = mutableMapOf<String, Long>()
    private var sessionEpoch = 0L

    private data class ActivationToken(val sessionEpoch: Long, val entryEpoch: Long)

    @Volatile
    private var sessionUnlocked = initiallyUnlocked

    fun start() {
        scope.launch {
            val now = System.currentTimeMillis()
            delay((1000 - (now % 1000)).milliseconds)
            while (currentCoroutineContext().isActive) {
                if (sessionUnlocked) {
                    reactivatePendingEntries()
                    refreshStates(System.currentTimeMillis() / 1000)
                }
                delay(1000.milliseconds)
            }
        }
    }

    private suspend fun refreshStates(nowSeconds: Long) {
        for ((entryId, schedule) in schedules.toMap()) {
            val token = activationToken(entryId)
            if (!isCurrent(entryId, token)) continue
            if (schedule.type == OtpType.HOTP) continue

            val movingFactor = nowSeconds / schedule.periodSeconds
            if (movingFactor != schedule.movingFactor) {
                val config = try {
                    loadOtpConfig(entryId)
                } catch (_: SessionLockedException) {
                    handleSessionLocked()
                    return
                }
                if (!isCurrent(entryId, token)) continue
                if (config == null || config.secret.isNullOrBlank()) {
                    _states.update { states ->
                        if (isCurrent(entryId, token)) {
                            states + (entryId to OtpCodeState(error = OtpGenerationError.InvalidSecret))
                        } else {
                            states
                        }
                    }
                    continue
                }
                applyConfig(entryId, config, nowSeconds, token)
            } else {
                _states.update { states ->
                    val existing = states[entryId]
                    if (
                        isCurrent(entryId, token) &&
                        schedules[entryId] == schedule &&
                        existing != null
                    ) {
                        states + (entryId to existing.copy(
                            progress = computeProgress(nowSeconds, schedule.periodSeconds)
                        ))
                    } else {
                        states
                    }
                }
            }
        }
    }

    private suspend fun generateUiState(
        config: OtpConfig,
        nowSeconds: Long,
        period: Int
    ): OtpCodeState = when (val genResult = codeGenerator(config)) {
        is OtpResult.Success -> OtpCodeState(
            code = genResult.code,
            progress = computeProgress(nowSeconds, period)
        )

        is OtpResult.Failure -> OtpCodeState(
            code = null,
            progress = computeProgress(nowSeconds, period),
            error = genResult.error
        )
    }

    /**
     * 生成 HOTP 验证码（用户主动触发）。
     */
    suspend fun generateHotpCode(entryId: String): OtpResult {
        if (!sessionUnlocked) return OtpResult.Failure(OtpGenerationError.InvalidSecret)
        val token = activationToken(entryId)
        val config = try {
            loadOtpConfig(entryId)
        } catch (_: SessionLockedException) {
            handleSessionLocked()
            return OtpResult.Failure(OtpGenerationError.InvalidSecret)
        } ?: return OtpResult.Failure(OtpGenerationError.InvalidSecret)
        if (config.type != OtpType.HOTP) return OtpResult.Failure(OtpGenerationError.InvalidSecret)
        val counter = config.counter ?: return OtpResult.Failure(OtpGenerationError.InvalidCounter)
        if (counter < 0) return OtpResult.Failure(OtpGenerationError.InvalidCounter)

        val result = codeGenerator(config)

        if (!isCurrent(entryId, token)) return result

        if (result is OtpResult.Success) {
            _states.update { it + (entryId to OtpCodeState(code = result.code)) }
        } else {
            _states.update { it + (entryId to OtpCodeState(error = (result as OtpResult.Failure).error)) }
        }
        return result
    }

    private fun computeProgress(nowSeconds: Long, period: Int): Float {
        val remaining = period - (nowSeconds % period)
        return remaining.toFloat() / period
    }

    /**
     * 激活条目：直接从 vault_credentials 查询并解密完整 OtpConfig。
     * 列表摘要、issuer 和 UI 缓存均不参与生成决策。
     */
    suspend fun activate(entryId: String) {
        activeEntryIds += entryId
        if (!sessionUnlocked) return
        activateTrackedEntry(entryId)
    }

    private suspend fun activateTrackedEntry(entryId: String) {
        val token = activationToken(entryId)
        val config = try {
            loadOtpConfig(entryId)
        } catch (_: SessionLockedException) {
            handleSessionLocked()
            return
        }
        if (!isCurrent(entryId, token)) return
        if (config == null || config.secret.isNullOrBlank()) {
            AppTelemetry.w("OtpCodeRefreshUseCase", "OTP activation failed: missing config for $entryId")
            _states.update { it + (entryId to OtpCodeState(error = OtpGenerationError.InvalidSecret)) }
            return
        }
        val nowSeconds = System.currentTimeMillis() / 1000
        applyConfig(entryId, config, nowSeconds, token)
    }

    private suspend fun applyConfig(
        entryId: String,
        config: OtpConfig,
        nowSeconds: Long,
        token: ActivationToken,
    ) {
        if (config.type == OtpType.HOTP) {
            if (!isCurrent(entryId, token)) return
            schedules[entryId] = OtpSchedule(OtpType.HOTP, 1, 0)
            _states.update { it + (entryId to OtpCodeState()) }
            return
        }

        val period = if (config.type == OtpType.STEAM) {
            30
        } else {
            config.periodSeconds?.coerceAtLeast(1) ?: 30
        }
        val schedule = OtpSchedule(
            type = config.type,
            periodSeconds = period,
            movingFactor = nowSeconds / period
        )
        val state = generateUiState(config, nowSeconds, period)
        if (!isCurrent(entryId, token)) return
        schedules[entryId] = schedule
        _states.update { it + (entryId to state) }
    }

    fun autoUnlock(entryId: String) {
        if (!activeEntryIds.add(entryId) || !sessionUnlocked) return
        scope.launch {
            activateTrackedEntry(entryId)
        }
    }

    private fun isRequested(entryId: String): Boolean =
        entryId in activeEntryIds || entryId in subscriptionCounts

    private fun activationToken(entryId: String) = ActivationToken(
        sessionEpoch = sessionEpoch,
        entryEpoch = entryEpochs[entryId] ?: 0L,
    )

    private fun isCurrent(entryId: String, token: ActivationToken): Boolean =
        sessionUnlocked &&
            isRequested(entryId) &&
            sessionEpoch == token.sessionEpoch &&
            (entryEpochs[entryId] ?: 0L) == token.entryEpoch

    private fun invalidateEntry(entryId: String) {
        entryEpochs[entryId] = (entryEpochs[entryId] ?: 0L) + 1L
    }

    fun subscribe(entryId: String) {
        val previousCount = subscriptionCounts[entryId] ?: 0
        subscriptionCounts[entryId] = previousCount + 1
        if (previousCount > 0 || entryId in schedules || !sessionUnlocked) return
        scope.launch { activateTrackedEntry(entryId) }
    }

    fun unsubscribe(entryId: String) {
        val remaining = (subscriptionCounts[entryId] ?: return) - 1
        if (remaining > 0) {
            subscriptionCounts[entryId] = remaining
            return
        }
        subscriptionCounts.remove(entryId)
        if (entryId !in activeEntryIds) {
            invalidateEntry(entryId)
            schedules.remove(entryId)
            _states.update { it - entryId }
        }
    }

    /**
     * 锁定时立即清除验证码和调度状态，但保留待恢复的条目 ID。
     * 解锁后重新从数据库加载配置，不缓存 Secret。
     */
    fun onSessionStateChanged(unlocked: Boolean) {
        if (sessionUnlocked == unlocked) return
        sessionUnlocked = unlocked
        if (!unlocked) {
            sessionEpoch++
            clearGeneratedState()
        } else {
            scope.launch { reactivatePendingEntries() }
        }
    }

    private suspend fun reactivatePendingEntries() {
        if (!sessionUnlocked) return
        (activeEntryIds + subscriptionCounts.keys)
            .filterNot(schedules::containsKey)
            .toList()
            .forEach { entryId ->
                if (!sessionUnlocked) return
                activateTrackedEntry(entryId)
            }
    }

    private fun handleSessionLocked() {
        sessionUnlocked = false
        sessionEpoch++
        clearGeneratedState()
    }

    private fun clearGeneratedState() {
        schedules.clear()
        _states.value = emptyMap()
    }

    fun clearSensitiveState(entryId: String) {
        invalidateEntry(entryId)
        activeEntryIds.remove(entryId)
        subscriptionCounts.remove(entryId)
        schedules.remove(entryId)
        _states.update { it - entryId }
    }

    fun clearAllSensitiveState() {
        sessionEpoch++
        activeEntryIds.clear()
        subscriptionCounts.clear()
        clearGeneratedState()
    }

    fun onEntryUpdated(entryId: String) {
        clearSensitiveState(entryId)
        autoUnlock(entryId)
    }
}
