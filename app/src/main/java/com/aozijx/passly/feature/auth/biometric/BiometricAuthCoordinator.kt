package com.aozijx.passly.feature.auth.biometric

import androidx.biometric.BiometricPrompt
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.AuthFailed
import com.aozijx.passly.core.error.auth.AuthErrorHandler
import com.aozijx.passly.core.error.logFailureWithContext
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.domain.validation.AuthRequestValidator
import com.aozijx.passly.security.crypto.UnlockResult
import com.aozijx.passly.security.keystore.BiometricKeyProvider
import com.aozijx.passly.security.session.UserSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.InvalidKeyException
import java.security.KeyStoreException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Android 生物识别的 UI 编排边界。
 *
 * 领域仓库只负责应用密码和授权状态；Prompt、CryptoObject 与回调生命周期
 * 都留在 Feature 层。
 */
@Singleton
class BiometricAuthCoordinator @Inject constructor(
    private val scope: CoroutineScope,
    private val keyProvider: BiometricKeyProvider,
    private val sessionManager: UserSessionManager,
    private val errorHandler: AuthErrorHandler
) {
    private val requestValidator = AuthRequestValidator()

    suspend fun authenticate(
        launcher: BiometricPromptLauncher,
        title: String,
        subtitle: String
    ): AppResult<Unit> {
        validate(title)?.let { return it }
        if (sessionManager.isAuthorized.value) return AppResult.success(Unit)

        val cipher = keyProvider.getInitializedCipher()
            ?: return AppResult.failure(AuthFailed("无法准备认证环境，请重试"))

        return suspendCancellableCoroutine { continuation ->
            launcher.launchPrompt(
                title = title,
                subtitle = subtitle,
                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                onError = { errorCode, error ->
                    if (continuation.isActive) {
                        scope.launch { errorHandler.cleanupSensitiveState() }
                        continuation.resume(
                            AppResult.failure(
                                errorHandler.classifyBiometricError(errorCode, error)
                            )
                        )
                    }
                },
                onSuccess = { result ->
                    if (!continuation.isActive) return@launchPrompt
                    scope.launch {
                        try {
                            when (val unlock = withContext(Dispatchers.IO) {
                                keyProvider.processResult(result)
                            }) {
                                is UnlockResult.Failed -> continuation.resume(
                                    AppResult.failure(AuthFailed(unlock.reason.name))
                                )
                                is UnlockResult.Success -> {
                                    sessionManager.onAuthSuccess()
                                    continuation.resume(AppResult.success(Unit))
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            val appError = errorHandler.classifyError(e, "authenticate")
                            errorHandler.handleAuthFailure(appError, "authenticate")
                            if (continuation.isActive) {
                                continuation.resume(AppResult.failure(appError))
                            }
                        }
                    }
                }
            )
        }
    }

    suspend fun verifyIdentity(
        launcher: BiometricPromptLauncher,
        title: String,
        subtitle: String
    ): AppResult<Unit> {
        validate(title)?.let { return it }
        return suspendCancellableCoroutine { continuation ->
            launcher.launchPrompt(
                title = title,
                subtitle = subtitle,
                onError = { errorCode, error ->
                    if (continuation.isActive) {
                        continuation.resume(
                            AppResult.failure(
                                errorHandler.classifyBiometricError(errorCode, error)
                            )
                        )
                    }
                },
                onSuccess = {
                    if (continuation.isActive) {
                        continuation.resume(AppResult.success(Unit))
                    }
                }
            )
        }
    }

    suspend fun rekeyWithInvalidationPolicy(
        launcher: BiometricPromptLauncher,
        invalidateOnBiometricChange: Boolean
    ): AppResult<Unit> {
        try {
            keyProvider.prepareForRekey(invalidateOnBiometricChange)
            val cipher = keyProvider.getInitializedCipher()
                ?: return AppResult.failure(AuthFailed("无法准备重加密环境"))

            return suspendCancellableCoroutine { continuation ->
                launcher.launchPrompt(
                    title = "重加密身份验证",
                    subtitle = "请验证身份以更新安全策略",
                    cryptoObject = BiometricPrompt.CryptoObject(cipher),
                    onError = { errorCode, error ->
                        if (continuation.isActive) {
                            recoverFromFailedRekey(invalidateOnBiometricChange)
                            continuation.resume(
                                AppResult.failure(
                                    AuthFailed(requestValidator.sanitizeMessage(error))
                                ).logFailureWithContext(
                                    "BiometricAuth",
                                    "rekey.auth",
                                    mapOf("errorCode" to errorCode.toString())
                                )
                            )
                        }
                    },
                    onSuccess = { result ->
                        if (!continuation.isActive) return@launchPrompt
                        scope.launch {
                            val outcome = AppResult.runCatching("auth.rekey.complete") {
                                withContext(Dispatchers.IO) {
                                    keyProvider.completeRekey(result)
                                }
                            }
                            outcome.fold(
                                onSuccess = {
                                    if (continuation.isActive) {
                                        continuation.resume(AppResult.success(Unit))
                                    }
                                },
                                onFailure = { error ->
                                    recoverFromFailedRekey(false)
                                    if (continuation.isActive) {
                                        continuation.resume(
                                            AppResult.failure(
                                                AuthFailed(
                                                    requestValidator.sanitizeMessage(error.message)
                                                )
                                            ).logFailureWithContext(
                                                "BiometricAuth",
                                                "rekey.complete"
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                )
            }
        } catch (e: IllegalStateException) {
            return AppResult.failure(
                AuthFailed(requestValidator.sanitizeMessage(e.message))
            )
        } catch (e: InvalidKeyException) {
            return AppResult.failure(AuthFailed("加密密钥无效"))
                .logFailureWithContext("BiometricAuth", "rekey")
        } catch (e: KeyStoreException) {
            return AppResult.failure(AuthFailed("密钥存储异常"))
                .logFailureWithContext("BiometricAuth", "rekey")
        }
    }

    private fun validate(title: String): AppResult<Unit>? =
        when (val result = requestValidator.validateRequest(title)) {
            AuthRequestValidator.AuthRequestValidationResult.Valid -> null
            is AuthRequestValidator.AuthRequestValidationResult.Invalid ->
                AppResult.failure(
                    AuthFailed(requestValidator.sanitizeMessage(result.message))
                )
        }

    private fun recoverFromFailedRekey(invalidateOnBiometricChange: Boolean) {
        scope.launch {
            try {
                keyProvider.prepareForRekey(invalidateOnBiometricChange)
                if (keyProvider.getInitializedCipher() != null) {
                    Logcat.i("BiometricAuth", "Rekey recovery: new key ready")
                }
            } catch (e: Exception) {
                Logcat.e("BiometricAuth", "Rekey recovery error", e)
            }
        }
    }
}
