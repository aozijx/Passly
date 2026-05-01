package com.aozijx.passly.features.auth

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.security.auth.AuthValidationResult
import com.aozijx.passly.core.security.auth.AuthValidationSupport
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.features.auth.ui.AuthScreenAuthGateway
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
) : AuthScreenAuthGateway {
    /** 观察全局授权状态 */
    val isAuthorized: StateFlow<Boolean> = authUseCases.isAuthorized
    override val isAppPasswordEnabled: StateFlow<Boolean> = authUseCases.isAppPasswordEnabled

    private val _authMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authMessage: SharedFlow<String> = _authMessage.asSharedFlow()

    override fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)?
    ) {
        when (val validation = validationSupport.validateAuthenticationRequest(activity, title)) {
            is AuthValidationResult.Invalid -> {
                emitAuthError(validation.message, onError, sanitize = false)
                return
            }
            AuthValidationResult.Valid -> Unit
        }

        scope.launch {
            authUseCases.authenticate(activity, title, subtitle).fold(
                onSuccess = { onSuccess() },
                onFailure = { error -> emitAuthError(error.message, onError) }
            )
        }
    }

    override fun authenticateWithAppPassword(
        password: CharArray,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)?
    ) {
        scope.launch {
            authUseCases.authenticateWithAppPassword(password).fold(
                onSuccess = { onSuccess() },
                onFailure = { error -> emitAuthError(error.message, onError) }
            )
        }
    }

    fun setAppPassword(password: CharArray, onResult: (Result<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.setAppPassword(password) }
    }

    override fun bootstrapAppPassword(password: CharArray, onResult: (Result<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.bootstrapAppPassword(password) }
    }

    fun changeAppPassword(
        oldPassword: CharArray,
        newPassword: CharArray,
        onResult: (Result<Unit>) -> Unit
    ) {
        launchResult(onResult) { authUseCases.changeAppPassword(oldPassword, newPassword) }
    }

    fun disableAppPassword(password: CharArray, onResult: (Result<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.disableAppPassword(password) }
    }

    fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        launchResult(onResult) { authUseCases.verifyIdentity(activity, title, subtitle) }
    }

    fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        launchResult(onResult) {
            authUseCases.rekeyWithInvalidationPolicy(activity, invalidateOnBiometricChange)
        }
    }

    private fun emitAuthError(
        message: String?,
        onError: ((String) -> Unit)?,
        sanitize: Boolean = true
    ) {
        val resolved =
            if (sanitize) validationSupport.sanitizeMessage(message) else message.orEmpty()
        _authMessage.tryEmit(resolved)
        onError?.invoke(resolved)
    }

    private fun launchResult(onResult: (Result<Unit>) -> Unit, block: suspend () -> Result<Unit>) {
        scope.launch { onResult(block()) }
    }

    fun lock() = authUseCases.lock()
    fun onUserInteraction() = authUseCases.onUserInteraction()
    fun checkAndLock() = authUseCases.checkAndLock()
    fun updateLockTimeout(timeoutMs: Long) = authUseCases.updateLockTimeout(timeoutMs)
}