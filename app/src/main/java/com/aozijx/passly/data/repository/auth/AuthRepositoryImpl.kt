package com.aozijx.passly.data.repository.auth

import android.app.Application
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.auth.apppassword.AppPasswordPassphraseStore
import com.aozijx.passly.core.auth.authconstants.AuthLockConstants
import com.aozijx.passly.core.auth.biometric.BiometricAuthenticator
import com.aozijx.passly.core.auth.session.SessionAutoLockScheduler
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.core.auth.validation.AuthRequestValidator.AuthRequestValidationResult
import com.aozijx.passly.core.crypto.encryption.SessionCryptoKey
import com.aozijx.passly.core.crypto.keystore.DatabasePassphraseManager
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.repository.auth.internal.AppPasswordHandler
import com.aozijx.passly.domain.repository.auth.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AuthRepositoryImpl(
    private val application: Application
) : AuthRepository {

    private val authMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val requestValidator = AuthRequestValidator()

    private var currentTimeoutMs: Long = AuthLockConstants.DEFAULT_LOCK_TIMEOUT_MS
    private var lockScheduler: SessionAutoLockScheduler? = null

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _isAppPasswordEnabled =
        MutableStateFlow(AppPasswordPassphraseStore.isEnabled(application))
    override val isAppPasswordEnabled: StateFlow<Boolean> = _isAppPasswordEnabled.asStateFlow()

    private val appPasswordHandler = AppPasswordHandler(
        application = application,
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

        val cipher = DatabasePassphraseManager.getInitializedCipher(application)
            ?: return AppResult.failure(AppError.AuthFailed("无法准备认证环境，请重试"))

        return suspendCancellableCoroutine { continuation ->
            BiometricAuthenticator.authenticate(
                activity = activity,
                title = title,
                subtitle = subtitle,
                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                allowDeviceCredentialFallback = true,
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
                        val passphrase =
                            DatabasePassphraseManager.processResult(application, result)
                        try {
                            DatabasePassphraseManager.setDecryptedPassphrase(passphrase)
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
                        DatabasePassphraseManager.clearDecryptedPassphrase()
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

        if (DatabasePassphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用"))
        }

        suspendCancellableCoroutine { continuation ->
            BiometricAuthenticator.authenticate(
                activity = activity,
                title = title,
                subtitle = subtitle,
                allowDeviceCredentialFallback = true,
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
        DatabasePassphraseManager.clearDecryptedPassphrase()
        SessionCryptoKey.clearSessionKey()
        _isAuthorized.update { false }
        lockScheduler?.cancel()
    }

    override fun onUserInteraction() {
        lockScheduler?.cancel()
        lockScheduler?.schedule(currentTimeoutMs)
    }

    override fun checkAndLock() {
        if (_isAuthorized.value) return
        Logcat.i("AuthRepo", "Check: not authorized, ensuring locked state.")
        DatabasePassphraseManager.clearDecryptedPassphrase()
        SessionCryptoKey.clearSessionKey()
    }

    override fun updateLockTimeout(timeoutMs: Long) {
        val normalized = requestValidator.normalizeLockTimeout(timeoutMs)
        currentTimeoutMs = normalized
        if (_isAuthorized.value) {
            lockScheduler?.cancel()
            lockScheduler?.schedule(normalized)
        }
    }

    override suspend fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean
    ): AppResult<Unit> = authMutex.withLock {
        try {
            DatabasePassphraseManager.prepareForRekey(application, invalidateOnBiometricChange)

            val cipher = DatabasePassphraseManager.getInitializedCipher(application)
                ?: error("无法准备重加密环境")

            return suspendCancellableCoroutine { continuation ->
                BiometricAuthenticator.authenticate(
                    activity = activity,
                    title = "重加密身份验证",
                    subtitle = "请验证身份以更新安全策略",
                    cryptoObject = BiometricPrompt.CryptoObject(cipher),
                    allowDeviceCredentialFallback = false,
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
                            val passphrase = DatabasePassphraseManager.getPassphrase()
                            try {
                                DatabasePassphraseManager.completeRekey(
                                    application, result, passphrase
                                )
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
        lockScheduler = SessionAutoLockScheduler(scope) { lock() }
        lockScheduler?.schedule(currentTimeoutMs)
    }

    private fun recoverFromFailedRekey(invalidateOnBiometricChange: Boolean) {
        try {
            DatabasePassphraseManager.prepareForRekey(application, invalidateOnBiometricChange)
            val cipher = DatabasePassphraseManager.getInitializedCipher(application)
            if (cipher != null) {
                Logcat.i("AuthRepo", "Rekey recovery: new key ready.")
            }
        } catch (e: Exception) {
            Logcat.e("AuthRepo", "Rekey recovery failed", e)
        }
    }
}