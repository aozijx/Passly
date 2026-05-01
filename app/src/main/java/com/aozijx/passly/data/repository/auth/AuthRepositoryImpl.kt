package com.aozijx.passly.data.repository.auth

import android.app.Application
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.crypto.BiometricHelper
import com.aozijx.passly.core.crypto.SessionCryptoKey
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.security.AppPasswordStore
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
    private val _isAppPasswordEnabled =
        MutableStateFlow(AppPasswordStore.isEnabled(application))
    override val isAppPasswordEnabled: StateFlow<Boolean> = _isAppPasswordEnabled.asStateFlow()

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
            SessionCryptoKey.deriveAndSet(passphrase)
            onAuthorized()
            Logcat.i(tag, "Authentication and decryption successful.")
        }
    }

    override suspend fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): Result<Unit> = authMutex.withLock {
        when (val validation = validationSupport.validateAuthenticationRequest(activity, title)) {
            is AuthValidationResult.Invalid -> return Result.failure(Exception(validation.message))
            AuthValidationResult.Valid -> Unit
        }

        return runCatching {
            withTimeoutOrNull(AUTH_TIMEOUT_MS) {
                kotlin.coroutines.suspendCoroutine<BiometricPrompt.AuthenticationResult> { continuation ->
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = title,
                        subtitle = subtitle,
                        cryptoObject = null,
                        onSuccess = { result ->
                            continuation.resumeWith(Result.success(result))
                        },
                        onError = { error ->
                            continuation.resumeWith(Result.failure(Exception(error)))
                        }
                    )
                }
            } ?: throw Exception("认证超时，请重试")

            Logcat.i(tag, "Identity verification successful.")
        }
    }

    override suspend fun authenticateWithAppPassword(password: CharArray): Result<Unit> =
        authMutex.withLock {
            if (_isAuthorized.value) return Result.success(Unit)
            if (!_isAppPasswordEnabled.value) {
                return Result.failure(IllegalStateException("尚未设置应用密码"))
            }
            if (password.isEmpty()) {
                return Result.failure(IllegalArgumentException("请输入应用密码"))
            }

            return try {
                val passphrase = AppPasswordStore.unlock(application, password)
                    .getOrThrow()
                try {
                    DatabasePassphraseManager.setDecryptedPassphrase(passphrase)
                    SessionCryptoKey.deriveAndSet(passphrase)
                } finally {
                    passphrase.fill(0)
                }
                onAuthorized()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                password.fill('\u0000')
            }
        }

    override suspend fun setAppPassword(password: CharArray): Result<Unit> = authMutex.withLock {
        return try {
            runCatching { validateAppPasswordPolicy(password) }
                .getOrElse { return Result.failure(it) }
            if (DatabasePassphraseManager.isLocked) {
                Result.failure(IllegalStateException("请先解锁应用后再设置应用密码"))
            } else {
                val passphrase = DatabasePassphraseManager.getPassphrase()
                AppPasswordStore.configure(application, password, passphrase)
                    .onSuccess { _isAppPasswordEnabled.update { true } }
                    .also { passphrase.fill(0) }
            }
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun bootstrapAppPassword(password: CharArray): Result<Unit> =
        authMutex.withLock {
            return try {
                runCatching { validateAppPasswordPolicy(password) }
                    .getOrElse { return Result.failure(it) }
                if (!DatabasePassphraseManager.isLocked) {
                    Result.failure(IllegalStateException("应用已解锁，请在设置中管理应用密码"))
                } else {
                    AppPasswordStore.configureWithGeneratedPassphrase(application, password)
                        .map { generatedPassphrase ->
                            try {
                                DatabasePassphraseManager.setDecryptedPassphrase(generatedPassphrase)
                                SessionCryptoKey.deriveAndSet(generatedPassphrase)
                                onAuthorized()
                            } finally {
                                generatedPassphrase.fill(0)
                            }
                        }
                        .onSuccess { _isAppPasswordEnabled.update { true } }
                }
            } finally {
                password.fill('\u0000')
            }
        }

    override suspend fun changeAppPassword(
        oldPassword: CharArray,
        newPassword: CharArray
    ): Result<Unit> = authMutex.withLock {
        return try {
            runCatching { validateAppPasswordPolicy(newPassword) }
                .getOrElse { return Result.failure(it) }
            if (DatabasePassphraseManager.isLocked) {
                Result.failure(IllegalStateException("请先解锁应用后再修改应用密码"))
            } else {
                val passphrase = DatabasePassphraseManager.getPassphrase()
                AppPasswordStore.change(application, oldPassword, newPassword, passphrase)
                    .onSuccess { refreshAppPasswordState() }
                    .also { passphrase.fill(0) }
            }
        } finally {
            oldPassword.fill('\u0000')
            newPassword.fill('\u0000')
        }
    }

    override suspend fun disableAppPassword(password: CharArray): Result<Unit> =
        authMutex.withLock {
            return try {
                if (DatabasePassphraseManager.isLocked) {
                    Result.failure(IllegalStateException("请先解锁应用后再关闭应用密码"))
                } else {
                    val passphrase = DatabasePassphraseManager.getPassphrase()
                    AppPasswordStore.disable(application, password, passphrase)
                        .onSuccess { refreshAppPasswordState() }
                        .also { passphrase.fill(0) }
                }
            } finally {
                password.fill('\u0000')
            }
        }

    override fun onExternalAuthorized() {
        if (DatabasePassphraseManager.isLocked) {
            Logcat.w(tag, "onExternalAuthorized called but passphrase is absent; ignoring.")
            return
        }
        if (!_isAuthorized.value) {
            Logcat.i(tag, "Accepting external authorization; engaging auto-lock timer.")
        }
        onAuthorized()
    }

    override fun lock() {
        Logcat.i(tag, "Locking session.")
        _isAuthorized.update { false }
        DatabasePassphraseManager.clearDecryptedPassphrase()
        SessionCryptoKey.clearSessionKey()
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

    override suspend fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean
    ): Result<Unit> {
        if (DatabasePassphraseManager.isLocked) {
            return Result.failure(Exception("请先解锁应用"))
        }

        val currentPassphrase = DatabasePassphraseManager.getPassphrase().copyOf()

        return runCatching {
            DatabasePassphraseManager.prepareForRekey(application, invalidateOnBiometricChange)

            val cipher = DatabasePassphraseManager.getInitializedCipher(application)
                ?: throw Exception("无法初始化加密器")

            val authResult = withTimeoutOrNull(AUTH_TIMEOUT_MS) {
                kotlin.coroutines.suspendCoroutine<BiometricPrompt.AuthenticationResult> { continuation ->
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = "重新加密保险箱密钥",
                        subtitle = "验证身份以应用新的安全策略",
                        cryptoObject = BiometricPrompt.CryptoObject(cipher),
                        onSuccess = { result ->
                            continuation.resumeWith(Result.success(result))
                        },
                        onError = { error ->
                            continuation.resumeWith(Result.failure(Exception(error)))
                        }
                    )
                }
            } ?: throw Exception("认证超时")

            DatabasePassphraseManager.completeRekey(application, authResult, currentPassphrase)
            Logcat.i(
                tag,
                "Rekey completed: invalidateOnBiometricChange=$invalidateOnBiometricChange"
            )
        }.onFailure { e ->
            Logcat.e(tag, "Rekey failed, recovering with previous policy...", e)
            recoverFromFailedRekey(!invalidateOnBiometricChange)
        }.also {
            currentPassphrase.fill(0)
        }
    }

    private fun recoverFromFailedRekey(previousPolicy: Boolean) {
        runCatching {
            DatabasePassphraseManager.prepareForRekey(application, previousPolicy)
            Logcat.w(
                tag,
                "Recovered key with previous policy, but passphrase needs re-encryption on next auth."
            )
        }.onFailure { e ->
            Logcat.e(tag, "Recovery also failed. Passphrase is still in memory.", e)
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

    private fun refreshAppPasswordState() {
        _isAppPasswordEnabled.update { AppPasswordStore.isEnabled(application) }
    }

    private fun validateAppPasswordPolicy(password: CharArray) {
        if (password.size < 10) throw IllegalArgumentException("应用密码至少需要 10 位")
        var hasLetter = false
        var hasDigit = false
        var hasSymbol = false
        password.forEach { ch ->
            when {
                ch.isLetter() -> hasLetter = true
                ch.isDigit() -> hasDigit = true
                !ch.isWhitespace() -> hasSymbol = true
            }
        }
        val strengthGroups = listOf(hasLetter, hasDigit, hasSymbol).count { it }
        if (strengthGroups < 2) {
            throw IllegalArgumentException("请至少混合两种字符类型：字母、数字或符号")
        }
    }

    private fun resetTimer() {
        autoLockScheduler.schedule(currentTimeoutMs)
    }

    companion object {
        private const val AUTH_TIMEOUT_MS = 60_000L
    }
}