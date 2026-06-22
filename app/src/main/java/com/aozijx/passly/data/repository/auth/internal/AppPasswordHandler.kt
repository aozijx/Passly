package com.aozijx.passly.data.repository.auth.internal

import android.content.Context
import com.aozijx.passly.core.auth.apppassword.AppPasswordComplexityPolicy
import com.aozijx.passly.core.auth.apppassword.AppPasswordPassphraseStore
import com.aozijx.passly.core.crypto.encryption.SessionCryptoKey
import com.aozijx.passly.core.crypto.keystore.DatabasePassphraseManager
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult

internal class AppPasswordHandler(
    private val application: Context,
    private val passphraseManager: DatabasePassphraseManager,
    private val isAuthorized: () -> Boolean,
    private val onAuthorized: () -> Unit,
    private val refreshAppPasswordState: () -> Unit
) {

    fun authenticate(password: CharArray): AppResult<Unit> {
        if (isAuthorized()) return AppResult.success(Unit)
        if (!AppPasswordPassphraseStore.isEnabled(application)) {
            return AppResult.failure(AppError.AuthFailed("尚未设置应用密码"))
        }
        if (password.isEmpty()) {
            return AppResult.failure(AppError.AuthFailed("请输入应用密码"))
        }

        return AppResult.runCatching("appPassword.authenticate") {
            val passphrase = AppPasswordPassphraseStore.unlock(application, password).getOrThrow()
            try {
                passphraseManager.setDecryptedPassphrase(passphrase)
                SessionCryptoKey.deriveAndSet(passphrase)
            } finally {
                passphrase.fill(0)
            }
            onAuthorized()
        }.mapFailure { AppError.AuthFailed(it.message ?: "应用密码验证失败") }
    }

    fun setPassword(password: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(password)

        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再设置应用密码"))
        }

        return AppResult.runCatching("appPassword.set") {
            val passphrase = passphraseManager.getPassphrase()
            try {
                AppPasswordPassphraseStore.configure(application, password, passphrase).getOrThrow()
            } finally {
                passphrase.fill(0)
            }
        }.onSuccess { refreshAppPasswordState() }
            .mapFailure { AppError.AuthFailed(it.message ?: "设置应用密码失败") }
    }

    fun bootstrapPassword(password: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(password)

        if (!passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("应用已解锁，请在设置中管理应用密码"))
        }

        return AppResult.runCatching("appPassword.bootstrap") {
            AppPasswordPassphraseStore.configureWithGeneratedPassphrase(application, password)
                .getOrThrow()
        }.map { generatedPassphrase ->
            try {
                passphraseManager.setDecryptedPassphrase(generatedPassphrase)
                SessionCryptoKey.deriveAndSet(generatedPassphrase)
                onAuthorized()
            } finally {
                generatedPassphrase.fill(0)
            }
        }.onSuccess { refreshAppPasswordState() }
            .mapFailure { AppError.AuthFailed(it.message ?: "设置应用密码失败") }
    }

    fun changePassword(oldPassword: CharArray, newPassword: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(newPassword)

        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再修改应用密码"))
        }

        return AppResult.runCatching("appPassword.change") {
            val passphrase = passphraseManager.getPassphrase()
            try {
                AppPasswordPassphraseStore.change(application, oldPassword, newPassword, passphrase)
                    .getOrThrow()
            } finally {
                passphrase.fill(0)
            }
        }.onSuccess { refreshAppPasswordState() }
            .mapFailure { AppError.AuthFailed(it.message ?: "修改应用密码失败") }
    }

    fun disablePassword(password: CharArray): AppResult<Unit> {
        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再关闭应用密码"))
        }

        return AppResult.runCatching("appPassword.disable") {
            val passphrase = passphraseManager.getPassphrase()
            try {
                AppPasswordPassphraseStore.disable(application, password, passphrase).getOrThrow()
            } finally {
                passphrase.fill(0)
            }
        }.onSuccess { refreshAppPasswordState() }
            .mapFailure { AppError.AuthFailed(it.message ?: "关闭应用密码失败") }
    }
}