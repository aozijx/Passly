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
 */
class AuthCoordinator(
    private val scope: CoroutineScope,
    private val authUseCases: AuthUseCases,
    private val validationSupport: AuthValidationSupport = AuthValidationSupport()
) {
    /** 观察全局授权状态 */
    val isAuthorized: StateFlow<Boolean> = authUseCases.isAuthorized
    val isAppPasswordEnabled: StateFlow<Boolean> = authUseCases.isAppPasswordEnabled

    private val _authMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authMessage: SharedFlow<String> = _authMessage.asSharedFlow()

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

    fun authenticateWithAppPassword(
        password: CharArray,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        scope.launch {
            authUseCases.authenticateWithAppPassword(password).fold(
                onSuccess = { onSuccess() },
                onFailure = { error ->
                    val safeError = validationSupport.sanitizeMessage(error.message)
                    _authMessage.tryEmit(safeError)
                    onError?.invoke(safeError)
                }
            )
        }
    }

    fun setAppPassword(password: CharArray, onResult: (Result<Unit>) -> Unit) {
        scope.launch { onResult(authUseCases.setAppPassword(password)) }
    }

    fun bootstrapAppPassword(password: CharArray, onResult: (Result<Unit>) -> Unit) {
        scope.launch { onResult(authUseCases.bootstrapAppPassword(password)) }
    }

    fun changeAppPassword(
        oldPassword: CharArray,
        newPassword: CharArray,
        onResult: (Result<Unit>) -> Unit
    ) {
        scope.launch { onResult(authUseCases.changeAppPassword(oldPassword, newPassword)) }
    }

    fun disableAppPassword(password: CharArray, onResult: (Result<Unit>) -> Unit) {
        scope.launch { onResult(authUseCases.disableAppPassword(password)) }
    }

    fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        scope.launch {
            onResult(authUseCases.verifyIdentity(activity, title, subtitle))
        }
    }

    fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        scope.launch {
            onResult(
                authUseCases.rekeyWithInvalidationPolicy(
                    activity,
                    invalidateOnBiometricChange
                )
            )
        }
    }

    fun lock() = authUseCases.lock()
    fun onUserInteraction() = authUseCases.onUserInteraction()
    fun checkAndLock() = authUseCases.checkAndLock()
    fun updateLockTimeout(timeoutMs: Long) = authUseCases.updateLockTimeout(timeoutMs)
}