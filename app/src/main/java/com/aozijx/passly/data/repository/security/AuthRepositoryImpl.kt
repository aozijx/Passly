package com.aozijx.passly.data.repository.security

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.AuthFailed
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.core.security.KeyDerivation
import com.aozijx.passly.domain.model.auth.AppPasswordComplexityPolicy
import com.aozijx.passly.domain.model.envelope.EnvelopeType
import com.aozijx.passly.domain.model.envelope.KdfAlgorithm
import com.aozijx.passly.domain.repository.security.AuthRepository
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.UnlockResult
import com.aozijx.passly.security.session.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuthRepositoryImpl @Inject constructor(
    private val dekManager: DekManager,
    private val sessionManager: UserSessionManager,
    private val scope: CoroutineScope
) : AuthRepository {

    private val _isAppPasswordEnabled = MutableStateFlow(false)
    override val isAppPasswordEnabled: StateFlow<Boolean> = _isAppPasswordEnabled.asStateFlow()

    override val isAuthorized: StateFlow<Boolean> = sessionManager.isAuthorized

    init {
        scope.launch {
            _isAppPasswordEnabled.value = dekManager.getEnvelope(EnvelopeType.APP_PASSWORD) != null
        }
    }

    // ─────────────────────────────────────────────────────────
    //  AppPassword（直接使用 DekManager + KeyDerivation）
    // ─────────────────────────────────────────────────────────

    override suspend fun authenticateWithAppPassword(password: CharArray): AppResult<Unit> {
        try {
            if (sessionManager.isAuthorized.value) return AppResult.success(Unit)

            val envelope = dekManager.getEnvelope(EnvelopeType.APP_PASSWORD)
                ?: return AppResult.failure(AuthFailed("尚未设置应用密码"))

            if (password.isEmpty()) {
                return AppResult.failure(AuthFailed("请输入应用密码"))
            }

            val key = KeyDerivation.deriveKeyArgon2id(password, envelope.salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, envelope.iv))

            val result = dekManager.unlock(EnvelopeType.APP_PASSWORD, cipher)
            return when (result) {
                is UnlockResult.Success -> {
                    sessionManager.onAuthSuccess()
                    AppResult.success(Unit)
                }

                is UnlockResult.Failed -> {
                    AppResult.failure(AuthFailed("应用密码错误"))
                }
            }
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun setAppPassword(password: CharArray): AppResult<Unit> {
        try {
            AppPasswordComplexityPolicy.validate(password)

            if (!sessionManager.isAuthorized.value) {
                return AppResult.failure(AuthFailed("请先解锁应用后再设置应用密码"))
            }

            val salt = KeyDerivation.generateSalt()
            val key = KeyDerivation.deriveKeyArgon2id(password, salt)
            dekManager.rekeyWithKey(EnvelopeType.APP_PASSWORD, key, salt, KdfAlgorithm.ARGON2ID)

            _isAppPasswordEnabled.update { true }
            return AppResult.success(Unit)
        } catch (e: IllegalStateException) {
            return AppResult.failure(AuthFailed(e.message ?: "设置失败"))
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun bootstrapAppPassword(password: CharArray): AppResult<Unit> {
        try {
            AppPasswordComplexityPolicy.validate(password)

            if (sessionManager.isAuthorized.value) {
                return AppResult.failure(AuthFailed("应用已解锁，请在设置中管理应用密码"))
            }

            val salt = KeyDerivation.generateSalt()
            val key = KeyDerivation.deriveKeyArgon2id(password, salt)
            dekManager.initializeWithKey(
                EnvelopeType.APP_PASSWORD,
                key,
                salt,
                KdfAlgorithm.ARGON2ID
            )

            _isAppPasswordEnabled.update { true }
            sessionManager.onAuthSuccess()
            return AppResult.success(Unit)
        } catch (e: IllegalStateException) {
            return AppResult.failure(AuthFailed(e.message ?: "引导失败"))
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun changeAppPassword(
        oldPassword: CharArray,
        newPassword: CharArray
    ): AppResult<Unit> {
        try {
            AppPasswordComplexityPolicy.validate(newPassword)

            if (!sessionManager.isAuthorized.value) {
                return AppResult.failure(AuthFailed("请先解锁应用后再修改应用密码"))
            }

            // 验证旧密码
            val oldEnvelope = dekManager.getEnvelope(EnvelopeType.APP_PASSWORD)
                ?: return AppResult.failure(AuthFailed("应用密码不存在"))
            val oldKey = KeyDerivation.deriveKeyArgon2id(oldPassword, oldEnvelope.salt)
            val verifyCipher = Cipher.getInstance("AES/GCM/NoPadding")
            try {
                verifyCipher.init(
                    Cipher.DECRYPT_MODE,
                    oldKey,
                    GCMParameterSpec(128, oldEnvelope.iv)
                )
                verifyCipher.doFinal(oldEnvelope.ciphertext)
            } catch (e: javax.crypto.AEADBadTagException) {
                return AppResult.failure(AuthFailed("旧密码错误"))
            }

            // 用新密码重新加密 DEK
            val newSalt = KeyDerivation.generateSalt()
            val newKey = KeyDerivation.deriveKeyArgon2id(newPassword, newSalt)
            dekManager.rekeyWithKey(
                EnvelopeType.APP_PASSWORD,
                newKey,
                newSalt,
                KdfAlgorithm.ARGON2ID
            )

            return AppResult.success(Unit)
        } catch (e: IllegalStateException) {
            return AppResult.failure(AuthFailed(e.message ?: "修改失败"))
        } finally {
            oldPassword.fill('\u0000')
            newPassword.fill('\u0000')
        }
    }

    override suspend fun disableAppPassword(password: CharArray): AppResult<Unit> {
        try {
            if (!sessionManager.isAuthorized.value) {
                return AppResult.failure(AuthFailed("请先解锁应用后再关闭应用密码"))
            }

            // 验证密码
            val envelope = dekManager.getEnvelope(EnvelopeType.APP_PASSWORD)
                ?: return AppResult.failure(AuthFailed("应用密码不存在"))
            val key = KeyDerivation.deriveKeyArgon2id(password, envelope.salt)
            val verifyCipher = Cipher.getInstance("AES/GCM/NoPadding")
            try {
                verifyCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, envelope.iv))
                verifyCipher.doFinal(envelope.ciphertext)
            } catch (e: javax.crypto.AEADBadTagException) {
                return AppResult.failure(AuthFailed("密码错误"))
            }

            dekManager.removeEnvelope(EnvelopeType.APP_PASSWORD)
            _isAppPasswordEnabled.update { false }
            return AppResult.success(Unit)
        } finally {
            password.fill('\u0000')
        }
    }

    override fun onExternalAuthorized() {
        if (!sessionManager.isAuthorized.value) {
            Logcat.Companion.i("AuthRepo", "External auth: setting authorized")
            scope.launch { sessionManager.onAuthSuccess() }
        }
    }

}
