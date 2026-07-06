package com.aozijx.passly.data.repository.auth.internal

import android.content.Context
import com.aozijx.passly.core.auth.apppassword.AppPasswordComplexityPolicy
import com.aozijx.passly.core.auth.apppassword.AppPasswordPassphraseStore
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.UnlockResult
import com.aozijx.passly.security.crypto.VaultLockManager
import com.aozijx.passly.security.envelope.EnvelopeType
import kotlinx.coroutines.runBlocking
import java.security.SecureRandom

internal class AppPasswordHandler(
    private val application: Context,
    private val lockManager: VaultLockManager,
    private val dekManager: DekManager,
    private val isAuthorized: () -> Boolean,
    private val onAuthorized: () -> Unit,
    private val refreshAppPasswordState: () -> Unit
) {
    companion object {
        private const val DEK_LENGTH = 32
    }

    fun authenticate(password: CharArray): AppResult<Unit> {
        if (isAuthorized()) return AppResult.success(Unit)
        if (!AppPasswordPassphraseStore.isEnabled(application)) {
            return AppResult.failure(AppError.AuthFailed("尚未设置应用密码"))
        }
        if (password.isEmpty()) {
            return AppResult.failure(AppError.AuthFailed("请输入应用密码"))
        }

        // 通过旧存储解密 DEK
        val result = AppPasswordPassphraseStore.unlock(application, password)
            .map { dek ->
                // 将 DEK 注入 DekManager
                try {
                    val unlockResult = runBlocking {
                        dekManager.unlockWithVerifiedDek(dek, EnvelopeType.APP_PASSWORD.value)
                    }
                    if (unlockResult is UnlockResult.Failed) {
                        throw IllegalStateException("DEK 校验失败: ${unlockResult.reason}")
                    }
                } finally {
                    dek.fill(0)
                }
                onAuthorized()
            }
            .mapFailure { AppError.AuthFailed(it.message) }

        return result
    }

    fun setPassword(password: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(password)

        if (lockManager.isLocked()) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再设置应用密码"))
        }

        return runBlocking {
            dekManager.withDek { dek ->
                val appResult = AppPasswordPassphraseStore.configure(application, password, dek)
                appResult
                    .onSuccess { refreshAppPasswordState() }
                    .mapFailure { AppError.AuthFailed(it.message) }
            }
        }
    }

    fun bootstrapPassword(password: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(password)

        if (!lockManager.isLocked()) {
            return AppResult.failure(AppError.AuthFailed("应用已解锁，请在设置中管理应用密码"))
        }
        if (AppPasswordPassphraseStore.isEnabled(application)) {
            return AppResult.failure(AppError.AuthFailed("应用密码已存在，请直接输入密码解锁"))
        }

        // 生成 DEK 并通过 AppPassword Store 包装
        val dek = ByteArray(DEK_LENGTH).also { SecureRandom().nextBytes(it) }
        try {
            val result = AppPasswordPassphraseStore.configure(application, password, dek)
            return result.map {
                // DEK 注入 DekManager（创建 VerificationTag）
                runBlocking { dekManager.bootstrapDek(dek) }
                refreshAppPasswordState()
                onAuthorized()
            }.mapFailure { AppError.AuthFailed(it.message) }
        } finally {
            dek.fill(0)
        }
    }

    fun changePassword(oldPassword: CharArray, newPassword: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(newPassword)

        if (lockManager.isLocked()) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再修改应用密码"))
        }

        return runBlocking {
            dekManager.withDek { dek ->
                AppPasswordPassphraseStore.change(
                    application, oldPassword, newPassword, dek
                )
                    .onSuccess { refreshAppPasswordState() }
                    .mapFailure { AppError.AuthFailed(it.message) }
            }
        }
    }

    fun disablePassword(password: CharArray): AppResult<Unit> {
        if (lockManager.isLocked()) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再关闭应用密码"))
        }

        return runBlocking {
            dekManager.withDek { dek ->
                AppPasswordPassphraseStore.disable(application, password, dek)
                    .onSuccess { refreshAppPasswordState() }
                    .mapFailure { AppError.AuthFailed(it.message) }
            }
        }
    }
}