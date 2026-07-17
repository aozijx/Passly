package com.aozijx.passly.feature.auth

import com.aozijx.passly.feature.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.feature.auth.biometric.BiometricAuthCoordinator
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ui.toUiMessage
import com.aozijx.passly.core.message.AppMessageCenter
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.security.session.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
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
class DefaultVerificationGateway @Inject constructor(
    private val scope: CoroutineScope,
    private val authUseCases: AuthUseCases,
    private val biometricAuth: BiometricAuthCoordinator,
    private val sessionManager: UserSessionManager
) : VerificationGateway {
    override val isAuthorized: StateFlow<Boolean> = sessionManager.isAuthorized
    override val isAppPasswordEnabled: StateFlow<Boolean> = authUseCases.isAppPasswordEnabled

    override fun verifyWithBiometric(
        launcher: BiometricPromptLauncher,
        title: String,
        subtitle: String,
        forceReauth: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        scope.launch {
            val result = if (forceReauth) {
                biometricAuth.verifyIdentity(launcher, title, subtitle)
            } else {
                biometricAuth.authenticate(launcher, title, subtitle)
            }
            result.onFailure { AppMessageCenter.publish(it.toUiMessage()) }
            onResult(result)
        }
    }

    override fun verifyWithAppPassword(
        password: CharArray,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        scope.launch {
            val result = authUseCases.authenticateWithAppPassword(password)
            result.onFailure { AppMessageCenter.publish(it.toUiMessage()) }
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
            biometricAuth.rekeyWithInvalidationPolicy(launcher, invalidateOnBiometricChange)
        }
    }

    override suspend fun verifyWithBiometricSuspended(
        launcher: BiometricPromptLauncher,
        title: String,
        subtitle: String
    ): AppResult<Unit> {
        val result = biometricAuth.authenticate(launcher, title, subtitle)
        result.onFailure { AppMessageCenter.publish(it.toUiMessage()) }
        return result
    }

    private fun launchResult(
        onResult: (AppResult<Unit>) -> Unit,
        block: suspend () -> AppResult<Unit>
    ) {
        scope.launch {
            val result = block()
            result.onFailure { AppMessageCenter.publish(it.toUiMessage()) }
            onResult(result)
        }
    }
}
