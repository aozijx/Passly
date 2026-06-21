package com.aozijx.passly.data.repository.auth

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.auth.apppassword.AppPasswordPassphraseStore
import com.aozijx.passly.core.auth.authconstants.AuthLockConstants
import com.aozijx.passly.core.auth.biometric.BiometricAuthenticator
import com.aozijx.passly.core.auth.session.AppIdleMonitor
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.core.auth.validation.AuthRequestValidator.AuthRequestValidationResult
import com.aozijx.passly.core.crypto.encryption.SessionCryptoKey
import com.aozijx.passly.core.crypto.keystore.DatabasePassphraseManager
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.repository.auth.internal.AppPasswordHandler
import com.aozijx.passly.domain.repository.auth.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
internal class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val application: android.content.Context,
    private val passphraseManager: DatabasePassphraseManager,
    private val idleMonitor: AppIdleMonitor
) : AuthRepository {

    private val authMutex = Mutex()
    private val requestValidator = AuthRequestValidator()

    private var currentTimeoutMs: Long = AuthLockConstants.DEFAULT_LOCK_TIMEOUT_MS

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _isAppPasswordEnabled =
        MutableStateFlow(AppPasswordPassphraseStore.isEnabled(application))
    override val isAppPasswordEnabled: StateFlow<Boolean> = _isAppPasswordEnabled.asStateFlow()

    private val appPasswordHandler = AppPasswordHandler(
        application = application,
        passphraseManager = passphraseManager,
        isAuthorized = { _isAuthorized.value },
        onAuthorized = { onAuthorized() },
        refreshAppPasswordState = {
            _isAppPasswordEnabled.update {
                AppPasswordPassphraseStore.isEnabled(
                    application
                )
            }
        }
    )

    override suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): AppResult<Unit> = authMutex.withLock {
        when (val validation = requestValidator.validateRequest(activity, title)) {
            is AuthRequestValidationResult.Invalid -> {
                return AppResult.failure(
                    AppError.AuthFailed(
                        requestValidator.sanitizeMessage(validation.message)
                    )
                )
            }

            AuthRequestValidationResult.Valid -> Unit
        }

        if (_isAuthorized.value) return AppResult.success(Unit)

        val cipher = passphraseManager.getInitializedCipher()
            ?: return AppResult.failure(AppError.AuthFailed("无法准备认证环境，请重试"))

        return suspendCancellableCoroutine { continuation ->
            BiometricAuthenticator.authenticate(
                activity = activity,
                title = title,
                subtitle = subtitle,
                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                onError = { error ->
                    if (continuation.isActive) {
                        continuation.resume(
                            AppResult.failure(AppError.AuthFailed(error))
                        )
                    }
                },
                onSuccess = { result ->
                    if (!continuation.isActive) return@authenticate

                    try {
                        val passphrase = passphraseManager.processResult(result)
                        try {
                            passphraseManager.setDecryptedPassphrase(passphrase)
                            SessionCryptoKey.deriveAndSet(passphrase)
                        } finally {
                            passphrase.fill(0)
                        }
                        onAuthorized()
                        continuation.resume(AppResult.success(Unit))
                    } catch (e: CancellationException) {
                        continuation.resumeWithException(e)
                    } catch (e: Exception) {
                        Logcat.e("AuthRepo", "Auth process error", e)
                        passphraseManager.clearDecryptedPassphrase()
                        SessionCryptoKey.clearSessionKey()
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
    ): AppResult<Unit> = authMutex.withLock {
        when (val validation = requestValidator.validateRequest(activity, title)) {
            is AuthRequestValidationResult.Invalid -> {
                return AppResult.failure(
                    AppError.AuthFailed(
                        requestValidator.sanitizeMessage(validation.message)
                    )
                )
            }

            AuthRequestValidationResult.Valid -> Unit
        }

        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用"))
        }

        suspendCancellableCoroutine { continuation ->
            BiometricAuthenticator.authenticate(
                activity = activity,
                title = title,
                subtitle = subtitle,
                onError = { error ->
                    if (continuation.isActive) {
                        continuation.resume(AppResult.failure(AppError.AuthFailed(error)))
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

    override suspend fun authenticateWithAppPassword(password: CharArray): AppResult<Unit> =
        authMutex.withLock {
            try {
                appPasswordHandler.authenticate(password)
            } finally {
                password.fill('\u0000')
            }
        }

    override suspend fun setAppPassword(password: CharArray): AppResult<Unit> =
        authMutex.withLock {
            try {
                appPasswordHandler.setPassword(password)
            } finally {
                password.fill('\u0000')
            }
        }

    override suspend fun bootstrapAppPassword(password: CharArray): AppResult<Unit> =
        authMutex.withLock {
            try {
                appPasswordHandler.bootstrapPassword(password)
            } finally {
                password.fill('\u0000')
            }
        }

    override suspend fun changeAppPassword(
        oldPassword: CharArray,
        newPassword: CharArray
    ): AppResult<Unit> = authMutex.withLock {
        try {
            appPasswordHandler.changePassword(oldPassword, newPassword)
        } finally {
            oldPassword.fill('\u0000')
            newPassword.fill('\u0000')
        }
    }

    override suspend fun disableAppPassword(password: CharArray): AppResult<Unit> =
        authMutex.withLock {
            try {
                appPasswordHandler.disablePassword(password)
            } finally {
                password.fill('\u0000')
            }
        }

    override fun onExternalAuthorized() {
        if (!_isAuthorized.value) {
            Logcat.i("AuthRepo", "External auth: setting authorized")
            onAuthorized()
        }
    }

    override fun lock() {
        Logcat.i("AuthRepo", "Locking. Trim memory + clear secrets.")
        passphraseManager.clearDecryptedPassphrase()
        SessionCryptoKey.clearSessionKey()
        _isAuthorized.update { false }
        idleMonitor.cancel()
    }

    override fun onUserInteraction() {
        if (!_isAuthorized.value) return
        idleMonitor.resetIdleTimer()
    }

    override fun checkAndLock() {
        if (_isAuthorized.value) return
        Logcat.i("AuthRepo", "Check: not authorized, ensuring locked state.")
        passphraseManager.clearDecryptedPassphrase()
        SessionCryptoKey.clearSessionKey()
    }

    override fun updateLockTimeout(timeoutMs: Long) {
        val normalized = requestValidator.normalizeLockTimeout(timeoutMs)
        currentTimeoutMs = normalized
        idleMonitor.updateTimeout(normalized)
        if (_isAuthorized.value) {
            idleMonitor.resetIdleTimer()
        }
    }

    override suspend fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean
    ): AppResult<Unit> = authMutex.withLock {
        try {
            passphraseManager.prepareForRekey(invalidateOnBiometricChange)

            val cipher = passphraseManager.getInitializedCipher()
                ?: error("无法准备重加密环境")

            return suspendCancellableCoroutine { continuation ->
                BiometricAuthenticator.authenticate(
                    activity = activity,
                    title = "重加密身份验证",
                    subtitle = "请验证身份以更新安全策略",
                    cryptoObject = BiometricPrompt.CryptoObject(cipher),
                    onError = { error ->
                        if (continuation.isActive) {
                            val msg = requestValidator.sanitizeMessage(error)
                            Logcat.e("AuthRepo", "Rekey auth error: $msg")
                            recoverFromFailedRekey(invalidateOnBiometricChange)
                            continuation.resume(AppResult.failure(AppError.AuthFailed(msg)))
                        }
                    },
                    onSuccess = { result ->
                        if (!continuation.isActive) return@authenticate
                        runCatching {
                            val passphrase = passphraseManager.getPassphrase()
                            try {
                                passphraseManager.completeRekey(result, passphrase)
                            } finally {
                                passphrase.fill(0)
                            }
                        }.onSuccess {
                            continuation.resume(AppResult.success(Unit))
                        }.onFailure { error ->
                            val msg =
                                requestValidator.sanitizeMessage(error.message)
                            Logcat.e("AuthRepo", "Rekey completion error: $msg", error)
                            recoverFromFailedRekey(false)
                            continuation.resume(AppResult.failure(AppError.AuthFailed(msg)))
                        }
                    }
                )
            }
        } catch (e: Exception) {
            val msg = requestValidator.sanitizeMessage(e.message)
            AppResult.failure(AppError.AuthFailed(msg))
        }
    }

    private fun onAuthorized() {
        _isAuthorized.update { true }
        idleMonitor.configure(currentTimeoutMs) { lock() }
        idleMonitor.resetIdleTimer()
    }

    private fun recoverFromFailedRekey(invalidateOnBiometricChange: Boolean) {
        try {
            passphraseManager.prepareForRekey(invalidateOnBiometricChange)
            val cipher = passphraseManager.getInitializedCipher()
            if (cipher != null) {
                Logcat.i("AuthRepo", "Rekey recovery: new key ready.")
            }
        } catch (e: Exception) {
            Logcat.e("AuthRepo", "Rekey recovery failed", e)
        }
    }
}