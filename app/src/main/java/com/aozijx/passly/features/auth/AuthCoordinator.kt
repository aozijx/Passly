package com.aozijx.passly.features.auth

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.crypto.BiometricHelper
import com.aozijx.passly.features.auth.internal.AuthValidationResult
import com.aozijx.passly.features.auth.internal.AuthValidationSupport
import com.aozijx.passly.features.auth.internal.AutoLockCoordinator
import com.aozijx.passly.features.auth.internal.AutoLockDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 认证模块协调器：负责管理全局授权状态、生物识别流程及自动锁定逻辑。
 * 它对外部屏蔽了内聚在 internal 中的复杂调度细节。
 */
class AuthCoordinator(
    private val scope: CoroutineScope,
    private val validationSupport: AuthValidationSupport = AuthValidationSupport()
) {
    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _authMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authMessage: SharedFlow<String> = _authMessage.asSharedFlow()

    private val autoLockCoordinator = AutoLockCoordinator(
        scope = scope,
        validationSupport = validationSupport
    )

    init {
        // 监听自动锁定信号
        scope.launch {
            autoLockCoordinator.shouldLock.collect {
                if (_isAuthorized.value) {
                    lock()
                }
            }
        }
    }

    /**
     * 触发身份验证流程（指纹/PIN等）。
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        when (val validation = validationSupport.validateAuthenticationRequest(activity, title)) {
            is AuthValidationResult.Invalid -> {
                val msg = validation.message
                _authMessage.tryEmit(msg)
                onError?.invoke(msg)
                return
            }
            AuthValidationResult.Valid -> Unit
        }

        BiometricHelper.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onError = { error ->
                val safeError = validationSupport.sanitizeMessage(error)
                _authMessage.tryEmit(safeError)
                onError?.invoke(safeError)
            },
            onSuccess = {
                authorize()
                onSuccess()
            }
        )
    }

    fun authorize() {
        _isAuthorized.update { true }
        autoLockCoordinator.onAuthorized()
    }

    fun lock() {
        _isAuthorized.update { false }
        autoLockCoordinator.onLocked()
    }

    fun onUserInteraction() {
        autoLockCoordinator.onInteraction(_isAuthorized.value)
    }

    fun checkAndLock() {
        autoLockCoordinator.checkNow(_isAuthorized.value)
    }

    fun applyTimeout(timeoutMs: Long): AutoLockDecision {
        return autoLockCoordinator.applyTimeout(timeoutMs, _isAuthorized.value)
    }
}