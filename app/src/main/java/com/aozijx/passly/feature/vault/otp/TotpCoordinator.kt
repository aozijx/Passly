package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.otp.OtpError
import com.aozijx.passly.core.otp.OtpResult
import com.aozijx.passly.domain.authentication.SessionLockedException
import com.aozijx.passly.domain.entry.model.OtpUiState
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
internal class TotpCoordinator(
    private val scope: CoroutineScope,
    private val codeGenerator: suspend (OtpConfig) -> OtpResult,
    private val loadOtpConfig: suspend (String) -> OtpConfig?,
    initiallyUnlocked: Boolean = true
) {
    private val _states = MutableStateFlow<Map<String, OtpUiState>>(emptyMap())
    val states: StateFlow<Map<String, OtpUiState>> = _states

    /** 仅缓存非敏感调度信息，不缓存 Secret。 */
    private data class OtpSchedule(
        val type: OtpType,
        val periodSeconds: Int,
        val movingFactor: Long
    )

    private val schedules = mutableMapOf<String, OtpSchedule>()
    private val activeEntryIds = mutableSetOf<String>()

    @Volatile
    private var sessionUnlocked = initiallyUnlocked

    fun start() {
        scope.launch {
            val now = System.currentTimeMillis()
            delay(1000 - (now % 1000))
            while (currentCoroutineContext().isActive) {
                if (sessionUnlocked) {
                    reactivatePendingEntries()
                    refreshStates(System.currentTimeMillis() / 1000)
                }
                delay(1000)
            }
        }
    }

    private suspend fun refreshStates(nowSeconds: Long) {
        val refreshed = _states.value.toMutableMap()
        for ((entryId, schedule) in schedules.toMap()) {
            if (schedule.type == OtpType.HOTP) continue

            val movingFactor = nowSeconds / schedule.periodSeconds
            if (movingFactor != schedule.movingFactor) {
                val config = try {
                    loadOtpConfig(entryId)
                } catch (_: SessionLockedException) {
                    handleSessionLocked()
                    return
                }
                if (config == null || config.secret.isBlank()) {
                    refreshed[entryId] = OtpUiState(error = OtpError.InvalidSecret)
                    continue
                }
                applyConfig(entryId, config, nowSeconds, refreshed)
            } else {
                val existing = refreshed[entryId]
                if (existing != null) {
                    refreshed[entryId] = existing.copy(
                        progress = computeProgress(nowSeconds, schedule.periodSeconds)
                    )
                }
            }
        }
        _states.value = refreshed
    }

    private suspend fun generateUiState(
        config: OtpConfig,
        nowSeconds: Long,
        period: Int
    ): OtpUiState = when (val genResult = codeGenerator(config)) {
        is OtpResult.Success -> OtpUiState(
            code = genResult.code,
            progress = computeProgress(nowSeconds, period)
        )

        is OtpResult.Failure -> OtpUiState(
            code = null,
            progress = computeProgress(nowSeconds, period),
            error = genResult.error
        )
    }

    /**
     * 生成 HOTP 验证码（用户主动触发）。
     */
    suspend fun generateHotpCode(entryId: String): OtpResult {
        if (!sessionUnlocked) return OtpResult.Failure(OtpError.InvalidSecret)
        val config = try {
            loadOtpConfig(entryId)
        } catch (_: SessionLockedException) {
            handleSessionLocked()
            return OtpResult.Failure(OtpError.InvalidSecret)
        } ?: return OtpResult.Failure(OtpError.InvalidSecret)
        if (config.type != OtpType.HOTP) return OtpResult.Failure(OtpError.InvalidSecret)
        val counter = config.counter ?: return OtpResult.Failure(OtpError.InvalidCounter)
        if (counter < 0) return OtpResult.Failure(OtpError.InvalidCounter)

        val result = codeGenerator(config)

        if (result is OtpResult.Success) {
            _states.update { it + (entryId to OtpUiState(code = result.code)) }
        } else {
            _states.update { it + (entryId to OtpUiState(error = (result as OtpResult.Failure).error)) }
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
        val config = try {
            loadOtpConfig(entryId)
        } catch (_: SessionLockedException) {
            handleSessionLocked()
            return
        }
        if (config == null || config.secret.isBlank()) {
            AppTelemetry.w("TotpCoordinator", "OTP activation failed: missing config for $entryId")
            _states.update { it + (entryId to OtpUiState(error = OtpError.InvalidSecret)) }
            return
        }
        val nowSeconds = System.currentTimeMillis() / 1000
        val refreshed = _states.value.toMutableMap()
        applyConfig(entryId, config, nowSeconds, refreshed)
        _states.value = refreshed
    }

    private suspend fun applyConfig(
        entryId: String,
        config: OtpConfig,
        nowSeconds: Long,
        target: MutableMap<String, OtpUiState>
    ) {
        if (config.type == OtpType.HOTP) {
            schedules[entryId] = OtpSchedule(OtpType.HOTP, 1, 0)
            target[entryId] = OtpUiState()
            return
        }

        val period = if (config.type == OtpType.STEAM) {
            30
        } else {
            config.periodSeconds?.coerceAtLeast(1) ?: 30
        }
        schedules[entryId] = OtpSchedule(
            type = config.type,
            periodSeconds = period,
            movingFactor = nowSeconds / period
        )
        target[entryId] = generateUiState(config, nowSeconds, period)
    }

    fun autoUnlock(entryId: String) {
        if (!activeEntryIds.add(entryId) || !sessionUnlocked) return
        scope.launch {
            activateTrackedEntry(entryId)
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
            clearGeneratedState()
        } else {
            scope.launch { reactivatePendingEntries() }
        }
    }

    private suspend fun reactivatePendingEntries() {
        if (!sessionUnlocked) return
        activeEntryIds
            .filterNot(schedules::containsKey)
            .toList()
            .forEach { entryId ->
                if (!sessionUnlocked) return
                activateTrackedEntry(entryId)
            }
    }

    private fun handleSessionLocked() {
        sessionUnlocked = false
        clearGeneratedState()
    }

    private fun clearGeneratedState() {
        schedules.clear()
        _states.value = emptyMap()
    }

    fun clearSensitiveState(entryId: String) {
        activeEntryIds.remove(entryId)
        schedules.remove(entryId)
        _states.update { it - entryId }
    }

    fun clearAllSensitiveState() {
        activeEntryIds.clear()
        clearGeneratedState()
    }

    fun onEntryUpdated(entryId: String) {
        clearSensitiveState(entryId)
        autoUnlock(entryId)
    }
}
