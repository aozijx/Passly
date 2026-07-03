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

        return AppPasswordPassphraseStore.unlock(application, password)
            .map { passphrase ->
                try {
                    passphraseManager.setDecryptedPassphrase(passphrase)
                    SessionCryptoKey.deriveAndSet(passphrase)
                } finally {
                    passphrase.fill(0)
                }
                onAuthorized()
            }
            .mapFailure { AppError.AuthFailed(it.message) }
    }

    fun setPassword(password: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(password)

        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再设置应用密码"))
        }

        val passphrase = passphraseManager.getPassphrase()
        try {
            return AppPasswordPassphraseStore.configure(application, password, passphrase)
                .onSuccess { refreshAppPasswordState() }
                .mapFailure { AppError.AuthFailed(it.message) }
        } finally {
            passphrase.fill(0)
        }
    }

    fun bootstrapPassword(password: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(password)

        if (!passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("应用已解锁，请在设置中管理应用密码"))
        }

        if (AppPasswordPassphraseStore.isEnabled(application)) {
            return AppResult.failure(AppError.AuthFailed("应用密码已存在，请直接输入密码解锁"))
        }

        val result =
            AppPasswordPassphraseStore.configureWithGeneratedPassphrase(application, password)
        // 即使后续 setPassphrase/deriveAndSet 失败，passphrase 已存入 SharedPreferences，
        // 必须刷新 isAppPasswordEnabled 状态，否则下次锁定后会再次显示"设置密码"按钮。
        refreshAppPasswordState()
        return result
            .map { generatedPassphrase ->
                try {
                    passphraseManager.setDecryptedPassphrase(generatedPassphrase)
                    SessionCryptoKey.deriveAndSet(generatedPassphrase)
                    onAuthorized()
                } finally {
                    generatedPassphrase.fill(0)
                }
            }
            .mapFailure { AppError.AuthFailed(it.message) }
    }

    fun changePassword(oldPassword: CharArray, newPassword: CharArray): AppResult<Unit> {
        AppPasswordComplexityPolicy.validate(newPassword)

        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再修改应用密码"))
        }

        val passphrase = passphraseManager.getPassphrase()
        try {
            return AppPasswordPassphraseStore.change(
                application,
                oldPassword,
                newPassword,
                passphrase
            )
                .onSuccess { refreshAppPasswordState() }
                .mapFailure { AppError.AuthFailed(it.message) }
        } finally {
            passphrase.fill(0)
        }
    }

    fun disablePassword(password: CharArray): AppResult<Unit> {
        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("请先解锁应用后再关闭应用密码"))
        }

        val passphrase = passphraseManager.getPassphrase()
        try {
            return AppPasswordPassphraseStore.disable(application, password, passphrase)
                .onSuccess { refreshAppPasswordState() }
                .mapFailure { AppError.AuthFailed(it.message) }
        } finally {
            passphrase.fill(0)
        }
    }
}