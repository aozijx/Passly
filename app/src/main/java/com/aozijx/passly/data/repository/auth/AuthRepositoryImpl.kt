package com.aozijx.passly.data.repository.auth

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.auth.apppassword.AppPasswordPassphraseStore
import com.aozijx.passly.core.auth.biometric.BiometricAuthenticator
import com.aozijx.passly.core.auth.error.AuthErrorHandler
import com.aozijx.passly.core.auth.state.LockStateManager
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.core.auth.validation.AuthRequestValidator.AuthRequestValidationResult
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.repository.auth.internal.AppPasswordHandler
import com.aozijx.passly.domain.repository.auth.AuthRepository
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.UnlockResult
import com.aozijx.passly.security.crypto.VaultLockManager
import com.aozijx.passly.security.keystore.BiometricKeyProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
internal class AuthRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val application: android.content.Context,
    private val passphraseManager: BiometricKeyProvider,
    private val dekManager: DekManager,
    private val lockManager: VaultLockManager,
    private val lockStateManager: LockStateManager,
    private val errorHandler: AuthErrorHandler
) : AuthRepository {

    private val requestValidator = AuthRequestValidator()

    private val _isAppPasswordEnabled =
        MutableStateFlow(AppPasswordPassphraseStore.isEnabled(application))
    override val isAppPasswordEnabled: StateFlow<Boolean> = _isAppPasswordEnabled.asStateFlow()

    override val isAuthorized: StateFlow<Boolean> = lockStateManager.isAuthorized

    private val appPasswordHandler = AppPasswordHandler(
        application = application,
        lockManager = lockManager,
        dekManager = dekManager,
        isAuthorized = { lockStateManager.isAuthorized.value },
        onAuthorized = { lockStateManager.markAuthorizedSync() },
        refreshAppPasswordState = {
            _isAppPasswordEnabled.update { AppPasswordPassphraseStore.isEnabled(application) }
        }
    )

    override suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): AppResult<Unit> {
        when (val validation = requestValidator.validateRequest(activity, title)) {
            is AuthRequestValidationResult.Invalid -> {
                return AppResult.failure(
                    AppError.AuthFailed(requestValidator.sanitizeMessage(validation.message))
                )
            }
            AuthRequestValidationResult.Valid -> Unit
        }

        if (lockStateManager.isAuthorized.value) return AppResult.success(Unit)

        val cipher = passphraseManager.getInitializedCipher()
            ?: return AppResult.failure(AppError.AuthFailed("无法准备认证环境，请重试"))

        return suspendCancellableCoroutine { continuation ->
            BiometricAuthenticator.authenticate(
                activity = activity,
                title = title,
                subtitle = subtitle,
                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                onError = { errorCode, error ->
                    if (continuation.isActive) {
                        errorHandler.cleanupSensitiveState()
                        val authError = errorHandler.classifyBiometricError(errorCode, error)
                        if (authError.canFallback() && isAppPasswordEnabled.value) {
                            Logcat.w("AuthRepo", "Biometric error [$errorCode], fallback available")
                        }
                        continuation.resume(AppResult.failure(AppError.AuthFailed(authError.toUserMessage())))
                    }
                },
                onSuccess = { result ->
                    if (!continuation.isActive) return@authenticate

                    try {
                        val unlockResult =
                            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                                passphraseManager.processResult(result)
                            }
                        if (unlockResult is UnlockResult.Failed) {
                            continuation.resume(
                                AppResult.failure(AppError.AuthFailed(unlockResult.reason.name))
                            )
                            return@authenticate
                        }
                        lockStateManager.markAuthorizedSync()
                        continuation.resume(AppResult.success(Unit))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        errorHandler.handleAuthFailure(
                            errorHandler.classifyError(e, "authenticate"),
                            "authenticate"
                        )
                        continuation.resume(
                            AppResult.failure(AppError.AuthFailed(e.message ?: "认证失败"))
                        )
                    }
                }
            )
        }
    }

    override suspend fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): AppResult<Unit> {
        when (val validation = requestValidator.validateRequest(activity, title)) {
            is AuthRequestValidationResult.Invalid -> {
                return AppResult.failure(
                    AppError.AuthFailed(requestValidator.sanitizeMessage(validation.message))
                )
            }
            AuthRequestValidationResult.Valid -> Unit
        }

        if (lockManager.isLocked()) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用"))
        }

        return suspendCancellableCoroutine { continuation ->
            BiometricAuthenticator.authenticate(
                activity = activity,
                title = title,
                subtitle = subtitle,
                onError = { errorCode, error ->
                    if (continuation.isActive) {
                        val authError = errorHandler.classifyBiometricError(errorCode, error)
                        continuation.resume(AppResult.failure(AppError.AuthFailed(authError.toUserMessage())))
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

    override suspend fun authenticateWithAppPassword(password: CharArray): AppResult<Unit> {
        try {
            return appPasswordHandler.authenticate(password)
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun setAppPassword(password: CharArray): AppResult<Unit> {
        try {
            return appPasswordHandler.setPassword(password)
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun bootstrapAppPassword(password: CharArray): AppResult<Unit> {
        try {
            return appPasswordHandler.bootstrapPassword(password)
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun changeAppPassword(
        oldPassword: CharArray,
        newPassword: CharArray
    ): AppResult<Unit> {
        try {
            return appPasswordHandler.changePassword(oldPassword, newPassword)
        } finally {
            oldPassword.fill('\u0000')
            newPassword.fill('\u0000')
        }
    }

    override suspend fun disableAppPassword(password: CharArray): AppResult<Unit> {
        try {
            return appPasswordHandler.disablePassword(password)
        } finally {
            password.fill('\u0000')
        }
    }

    override fun onExternalAuthorized() {
        if (!lockStateManager.isAuthorized.value) {
            Logcat.i("AuthRepo", "External auth: setting authorized")
            lockStateManager.markAuthorizedSync()
        }
    }

    override fun lock() = lockStateManager.lock()

    override fun onUserInteraction() = lockStateManager.onUserInteraction()

    override fun checkAndLock() = lockStateManager.ensureLockedState()

    override fun updateLockTimeout(timeoutMs: Long) {
        val normalized = requestValidator.normalizeLockTimeout(timeoutMs)
        lockStateManager.updateTimeout(normalized)
    }

    override suspend fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean
    ): AppResult<Unit> {
        try {
            passphraseManager.prepareForRekey(invalidateOnBiometricChange)

            val cipher = passphraseManager.getInitializedCipher()
                ?: return AppResult.failure(AppError.AuthFailed("无法准备重加密环境"))

            return suspendCancellableCoroutine { continuation ->
                BiometricAuthenticator.authenticate(
                    activity = activity,
                    title = "重加密身份验证",
                    subtitle = "请验证身份以更新安全策略",
                    cryptoObject = BiometricPrompt.CryptoObject(cipher),
                    onError = { errorCode, error ->
                        if (continuation.isActive) {
                            val msg = requestValidator.sanitizeMessage(error)
                            Logcat.e("AuthRepo", "Rekey auth error [$errorCode]: $msg")
                            recoverFromFailedRekey(invalidateOnBiometricChange)
                            continuation.resume(AppResult.failure(AppError.AuthFailed(msg)))
                        }
                    },
                    onSuccess = { result ->
                        if (!continuation.isActive) return@authenticate
                        AppResult.runCatching("auth.rekey.complete") {
                            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                                passphraseManager.completeRekey(result)
                            }
                        }.fold(
                            onSuccess = { continuation.resume(AppResult.success(Unit)) },
                            onFailure = { error ->
                                val msg = requestValidator.sanitizeMessage(error.message)
                                Logcat.e("AuthRepo", "Rekey completion error: $msg", error)
                                recoverFromFailedRekey(false)
                                continuation.resume(AppResult.failure(AppError.AuthFailed(msg)))
                            }
                        )
                    }
                )
            }
        } catch (e: IllegalStateException) {
            return AppResult.failure(AppError.AuthFailed(requestValidator.sanitizeMessage(e.message)))
        } catch (e: java.security.InvalidKeyException) {
            Logcat.e("AuthRepo", "Invalid key during rekey", e)
            return AppResult.failure(AppError.AuthFailed("加密密钥无效"))
        } catch (e: java.security.KeyStoreException) {
            Logcat.e("AuthRepo", "KeyStore error during rekey", e)
            return AppResult.failure(AppError.AuthFailed("密钥存储异常"))
        }
    }

    private fun recoverFromFailedRekey(invalidateOnBiometricChange: Boolean) {
        try {
            passphraseManager.prepareForRekey(invalidateOnBiometricChange)
            val cipher = passphraseManager.getInitializedCipher()
            if (cipher != null) {
                Logcat.i("AuthRepo", "Rekey recovery: new key ready")
            }
        } catch (e: Exception) {
            Logcat.e("AuthRepo", "Rekey recovery error", e)
        }
    }
}