package com.aozijx.passly.data.repository.auth

import android.app.Application
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.crypto.BiometricHelper
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.security.AutoLockScheduler
import com.aozijx.passly.core.security.DatabasePassphraseManager
import com.aozijx.passly.core.security.auth.AuthValidationResult
import com.aozijx.passly.core.security.auth.AuthValidationSupport
import com.aozijx.passly.domain.repository.auth.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 认证仓库实现类。
 * 口令由 DatabasePassphraseManager 统一持有，本类负责状态流转与自动锁定。
 */
internal class AuthRepositoryImpl(
    private val application: Application,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val validationSupport: AuthValidationSupport = AuthValidationSupport()
) : AuthRepository {

    private val tag = "AuthRepository"

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val authMutex = Mutex()

    private val autoLockScheduler = AutoLockScheduler(scope) {
        Logcat.i(tag, "Auto-lock timer triggered.")
        lock()
    }

    private var currentTimeoutMs: Long = AuthValidationSupport.DEFAULT_LOCK_TIMEOUT_MS
    private var lastInteractionAtMs: Long = 0

    override suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): Result<Unit> = authMutex.withLock {
        if (_isAuthorized.value) return Result.success(Unit)

        when (val validation = validationSupport.validateAuthenticationRequest(activity, title)) {
            is AuthValidationResult.Invalid -> return Result.failure(Exception(validation.message))
            AuthValidationResult.Valid -> Unit
        }

        val biometricCipher = DatabasePassphraseManager.getInitializedCipher(application)

        return runCatching {
            val authResult = withTimeoutOrNull(AUTH_TIMEOUT_MS) {
                kotlin.coroutines.suspendCoroutine<BiometricPrompt.AuthenticationResult> { continuation ->
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = title,
                        subtitle = subtitle,
                        cryptoObject = biometricCipher?.let {
                            BiometricPrompt.CryptoObject(it)
                        },
                        onSuccess = { result ->
                            continuation.resumeWith(Result.success(result))
                        },
                        onError = { error ->
                            continuation.resumeWith(Result.failure(Exception(error)))
                        }
                    )
                }
            } ?: throw Exception("认证超时，请重试")

            val passphrase = DatabasePassphraseManager.processResult(application, authResult)
            DatabasePassphraseManager.setDecryptedPassphrase(passphrase)
            onAuthorized()
            Logcat.i(tag, "Authentication and decryption successful.")
        }
    }

    override fun lock() {
        Logcat.i(tag, "Locking session.")
        _isAuthorized.update { false }
        DatabasePassphraseManager.clearDecryptedPassphrase()
        autoLockScheduler.cancel()
        lastInteractionAtMs = 0
    }

    override fun onUserInteraction() {
        if (_isAuthorized.value) {
            updateInteractionTimestamp()
            resetTimer()
        }
    }

    override fun checkAndLock() {
        if (!_isAuthorized.value) return
        val elapsed = System.currentTimeMillis() - lastInteractionAtMs
        if (lastInteractionAtMs == 0L || elapsed >= currentTimeoutMs) {
            lock()
        }
    }

    override fun updateLockTimeout(timeoutMs: Long) {
        val normalized = validationSupport.normalizeLockTimeout(timeoutMs)
        if (currentTimeoutMs != normalized) {
            currentTimeoutMs = normalized
            if (_isAuthorized.value) resetTimer()
        }
    }

    private fun onAuthorized() {
        _isAuthorized.update { true }
        updateInteractionTimestamp()
        resetTimer()
    }

    private fun updateInteractionTimestamp() {
        lastInteractionAtMs = System.currentTimeMillis()
    }

    private fun resetTimer() {
        autoLockScheduler.schedule(currentTimeoutMs)
    }

    companion object {
        private const val AUTH_TIMEOUT_MS = 60_000L
    }
}