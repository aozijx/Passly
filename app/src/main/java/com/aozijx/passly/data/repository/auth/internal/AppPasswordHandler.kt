package com.aozijx.passly.data.repository.auth.internal

import android.app.Application
import com.aozijx.passly.core.auth.apppassword.AppPasswordComplexityPolicy
import com.aozijx.passly.core.auth.apppassword.AppPasswordPassphraseStore
import com.aozijx.passly.core.crypto.encryption.SessionCryptoKey
import com.aozijx.passly.core.crypto.keystore.DatabasePassphraseManager
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult

internal class AppPasswordHandler(
    private val application: Application,
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

        return runCatching {
            val passphrase = AppPasswordPassphraseStore.unlock(application, password).getOrThrow()
            try {
                DatabasePassphraseManager.setDecryptedPassphrase(passphrase)
                SessionCryptoKey.deriveAndSet(passphrase)
            } finally {
                passphrase.fill(0)
            }
            onAuthorized()
        }.map { AppResult.success(Unit) }
            .getOrElse { AppResult.failure(AppError.AuthFailed(it.message ?: "应用密码验证失败")) }
    }

    fun setPassword(password: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(password)

        if (DatabasePassphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再设置应用密码"))
        }

        return runCatching {
            val passphrase = DatabasePassphraseManager.getPassphrase()
            try {
                AppPasswordPassphraseStore.configure(application, password, passphrase).getOrThrow()
            } finally {
                passphrase.fill(0)
            }
        }.onSuccess { refreshAppPasswordState() }
            .map { AppResult.success(Unit) }
            .getOrElse { AppResult.failure(AppError.AuthFailed(it.message ?: "设置应用密码失败")) }
    }

    fun bootstrapPassword(password: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(password)

        if (!DatabasePassphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("应用已解锁，请在设置中管理应用密码"))
        }

        return AppPasswordPassphraseStore.configureWithGeneratedPassphrase(application, password)
            .map { generatedPassphrase ->
                try {
                    DatabasePassphraseManager.setDecryptedPassphrase(generatedPassphrase)
                    SessionCryptoKey.deriveAndSet(generatedPassphrase)
                    onAuthorized()
                } finally {
                    generatedPassphrase.fill(0)
                }
            }
            .onSuccess { refreshAppPasswordState() }
            .map { AppResult.success(Unit) }
            .getOrElse { AppResult.failure(AppError.AuthFailed(it.message ?: "设置应用密码失败")) }
    }

    fun changePassword(oldPassword: CharArray, newPassword: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(newPassword)

        if (DatabasePassphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再修改应用密码"))
        }

        return runCatching {
            val passphrase = DatabasePassphraseManager.getPassphrase()
            try {
                AppPasswordPassphraseStore.change(application, oldPassword, newPassword, passphrase)
                    .getOrThrow()
            } finally {
                passphrase.fill(0)
            }
        }.onSuccess { refreshAppPasswordState() }
            .map { AppResult.success(Unit) }
            .getOrElse { AppResult.failure(AppError.AuthFailed(it.message ?: "修改应用密码失败")) }
    }

    fun disablePassword(password: CharArray): AppResult<Unit> {
        if (DatabasePassphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再关闭应用密码"))
        }

        return runCatching {
            val passphrase = DatabasePassphraseManager.getPassphrase()
            try {
                AppPasswordPassphraseStore.disable(application, password, passphrase).getOrThrow()
            } finally {
                passphrase.fill(0)
            }
        }.onSuccess { refreshAppPasswordState() }
            .map { AppResult.success(Unit) }
            .getOrElse { AppResult.failure(AppError.AuthFailed(it.message ?: "关闭应用密码失败")) }
    }
}