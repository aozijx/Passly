package com.aozijx.passly.features.auth

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.security.auth.AuthValidationResult
import com.aozijx.passly.core.security.auth.AuthValidationSupport
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 认证模块协调器：负责 UI 层的认证流程调度。
 * 它完全对接 AuthUseCases，不再持有任何底层的计时器或生物识别细节。
 */
class AuthCoordinator(
    private val scope: CoroutineScope,
    private val authUseCases: AuthUseCases,
    private val validationSupport: AuthValidationSupport = AuthValidationSupport()
) {
    /** 观察全局授权状态：由领域层驱动 */
    val isAuthorized: StateFlow<Boolean> = authUseCases.isAuthorized

    private val _authMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authMessage: SharedFlow<String> = _authMessage.asSharedFlow()

    /**
     * 触发身份验证流程。
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        // 1. UI 层校验：检查 Activity 状态和标题
        when (val validation = validationSupport.validateAuthenticationRequest(activity, title)) {
            is AuthValidationResult.Invalid -> {
                val msg = validation.message
                _authMessage.tryEmit(msg)
                onError?.invoke(msg)
                return
            }
            AuthValidationResult.Valid -> Unit
        }

        // 2. 调用领域层用例：实际的指纹/系统认证逻辑
        scope.launch {
            authUseCases.authenticate(activity, title, subtitle).fold(
                onSuccess = { onSuccess() },
                onFailure = { error ->
                    val safeError = validationSupport.sanitizeMessage(error.message)
                    _authMessage.tryEmit(safeError)
                    onError?.invoke(safeError)
                }
            )
        }
    }

    fun lock() = authUseCases.lock()

    fun onUserInteraction() = authUseCases.onUserInteraction()

    fun checkAndLock() = authUseCases.checkAndLock()

    fun updateLockTimeout(timeoutMs: Long) = authUseCases.updateLockTimeout(timeoutMs)
}