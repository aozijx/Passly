package com.aozijx.passly.data.repository.auth.internal

import android.content.Context
import com.aozijx.passly.core.auth.apppassword.AppPasswordComplexityPolicy
import com.aozijx.passly.core.auth.apppassword.AppPasswordPassphraseStore
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.AuthFailed
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.UnlockResult
import com.aozijx.passly.security.crypto.VaultLockManager
import com.aozijx.passly.security.envelope.EnvelopeType
import java.security.SecureRandom

internal class AppPasswordHandler(
    private val application: Context,
    private val lockManager: VaultLockManager,
    private val dekManager: DekManager,
    private val isAuthorized: () -> Boolean,
    private val onAuthorized: suspend () -> Unit,
    private val refreshAppPasswordState: () -> Unit
) {
    companion object {
        private const val DEK_LENGTH = 32
    }

    suspend fun authenticate(password: CharArray): AppResult<Unit> {
        if (isAuthorized()) return AppResult.success(Unit)
        if (!AppPasswordPassphraseStore.isEnabled(application)) {
            return AppResult.failure(AuthFailed("尚未设置应用密码"))
        }
        if (password.isEmpty()) {
            return AppResult.failure(AuthFailed("请输入应用密码"))
        }

        // 通过旧存储解密 DEK
        val unlockResult = AppPasswordPassphraseStore.unlock(application, password)
        if (unlockResult.isFailure) {
            val error = (unlockResult as AppResult.Failure).error
            return AppResult.failure(AuthFailed(error.message))
        }

        val dek = unlockResult.getOrThrow()
        try {
            val result = dekManager.unlockWithVerifiedDek(dek, EnvelopeType.APP_PASSWORD.value)
            if (result is UnlockResult.Failed) {
                return AppResult.failure(AuthFailed("DEK 校验失败: ${result.reason}"))
            }
            onAuthorized()
            return AppResult.success(Unit)
        } finally {
            dek.fill(0)
        }
    }

    suspend fun setPassword(password: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(password)

        if (lockManager.isLocked()) {
            return AppResult.failure(AuthFailed("请先解锁应用后再设置应用密码"))
        }

        return dekManager.withDek { dek ->
            AppPasswordPassphraseStore.configure(application, password, dek)
                .onSuccess { refreshAppPasswordState() }
                .mapFailure { AuthFailed(it.message) }
        }
    }

    suspend fun bootstrapPassword(password: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(password)

        if (!lockManager.isLocked()) {
            return AppResult.failure(AuthFailed("应用已解锁，请在设置中管理应用密码"))
        }
        if (AppPasswordPassphraseStore.isEnabled(application)) {
            return AppResult.failure(AuthFailed("应用密码已存在，请直接输入密码解锁"))
        }

        // 生成 DEK 并通过 AppPassword Store 包装
        val dek = ByteArray(DEK_LENGTH).also { SecureRandom().nextBytes(it) }
        try {
            val configureResult = AppPasswordPassphraseStore.configure(application, password, dek)
            if (configureResult.isFailure) {
                val error = (configureResult as AppResult.Failure).error
                return AppResult.failure(AuthFailed(error.message))
            }
            // DEK 注入 DekManager（创建 VerificationTag）
            dekManager.bootstrapDek(dek)
            refreshAppPasswordState()
            onAuthorized()
            return AppResult.success(Unit)
        } finally {
            dek.fill(0)
        }
    }

    suspend fun changePassword(oldPassword: CharArray, newPassword: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(newPassword)

        if (lockManager.isLocked()) {
            return AppResult.failure(AuthFailed("请先解锁应用后再修改应用密码"))
        }

        return dekManager.withDek { dek ->
            AppPasswordPassphraseStore.change(application, oldPassword, newPassword, dek)
                .onSuccess { refreshAppPasswordState() }
                .mapFailure { AuthFailed(it.message) }
        }
    }

    suspend fun disablePassword(password: CharArray): AppResult<Unit> {
        if (lockManager.isLocked()) {
            return AppResult.failure(AuthFailed("请先解锁应用后再关闭应用密码"))
        }

        return dekManager.withDek { dek ->
            AppPasswordPassphraseStore.disable(application, password, dek)
                .onSuccess { refreshAppPasswordState() }
                .mapFailure { AuthFailed(it.message) }
        }
    }
}