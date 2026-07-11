package com.aozijx.passly.ui.features.verification

import com.aozijx.passly.core.auth.VerificationGateway
import com.aozijx.passly.core.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ui.toUiMessage
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证网关单例实现。
 *
 * 使用 Application 级别的 CoroutineScope（由 DI 提供），
 * 确保 isAuthorized 等核心状态在整个应用中同步。
 * 调用方（ViewModel）通过协程回调或直接调用与网关交互，
 * 不直接将 ViewModel 生命周期绑定到网关。
 */
@Singleton
class VerificationGatewayImpl @Inject constructor(
    private val scope: CoroutineScope,
    private val authUseCases: AuthUseCases
) : VerificationGateway {
    override val isAuthorized: StateFlow<Boolean> = authUseCases.isAuthorized
    override val isAppPasswordEnabled: StateFlow<Boolean> = authUseCases.isAppPasswordEnabled

    private val _authMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val authMessage: SharedFlow<String> = _authMessage.asSharedFlow()

    override fun verifyWithBiometric(
        launcher: BiometricPromptLauncher,
        title: String,
        subtitle: String,
        forceReauth: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        scope.launch {
            val result = if (forceReauth) {
                authUseCases.verifyIdentity(launcher, title, subtitle)
            } else {
                authUseCases.authenticate(launcher, title, subtitle)
            }
            result.onFailure {
                _authMessage.tryEmit(it.toUiMessage())
            }
            onResult(result)
        }
    }

    override fun verifyWithAppPassword(
        password: CharArray,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        scope.launch {
            val result = authUseCases.authenticateWithAppPassword(password)
            result.onFailure {
                _authMessage.tryEmit(it.toUiMessage())
            }
            onResult(result)
        }
    }

    override fun setAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.setAppPassword(password) }
    }

    override fun bootstrapAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.bootstrapAppPassword(password) }
    }

    override fun changeAppPassword(
        oldPassword: CharArray,
        newPassword: CharArray,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        launchResult(onResult) { authUseCases.changeAppPassword(oldPassword, newPassword) }
    }

    override fun disableAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.disableAppPassword(password) }
    }

    override fun rekeyWithInvalidationPolicy(
        launcher: BiometricPromptLauncher,
        invalidateOnBiometricChange: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        launchResult(onResult) {
            authUseCases.rekeyWithInvalidationPolicy(launcher, invalidateOnBiometricChange)
        }
    }

    override suspend fun lock() = authUseCases.lock()
    override fun onUserInteraction() = authUseCases.onUserInteraction()
    override suspend fun checkAndLock() = authUseCases.checkAndLock()
    override suspend fun updateLockTimeout(timeoutMs: Long) =
        authUseCases.updateLockTimeout(timeoutMs)

    override suspend fun verifyWithBiometricSuspended(
        launcher: BiometricPromptLauncher,
        title: String,
        subtitle: String
    ): AppResult<Unit> {
        val result = authUseCases.authenticate(launcher, title, subtitle)
        result.onFailure {
            _authMessage.tryEmit(it.toUiMessage())
        }
        return result
    }

    private fun launchResult(
        onResult: (AppResult<Unit>) -> Unit,
        block: suspend () -> AppResult<Unit>
    ) {
        scope.launch { onResult(block()) }
    }
}