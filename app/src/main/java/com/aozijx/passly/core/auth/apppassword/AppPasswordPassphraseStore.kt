package com.aozijx.passly.core.auth.apppassword

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.aozijx.passly.core.backup.BackupManager
import com.aozijx.passly.core.crypto.memory.MemoryCleaner
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.AppDefaults
import java.security.MessageDigest
import java.security.SecureRandom

object AppPasswordPassphraseStore {

    fun isEnabled(context: Context): Boolean {
        val prefs =
            context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(AppDefaults.Auth.KEY_APP_PASSWORD_WRAP, null).isNullOrBlank() &&
                !prefs.getString(AppDefaults.Auth.KEY_APP_PASSWORD_SALT, null).isNullOrBlank()
    }

    fun configure(context: Context, password: CharArray, passphrase: ByteArray): AppResult<Unit> =
        AppResult.runCatching("appPasswordStore.configure") {
            val salt = BackupManager.generateSalt()
            val wrapped = encryptWrappedPassphrase(passphrase, password, salt)
            context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    putString(
                        AppDefaults.Auth.KEY_APP_PASSWORD_SALT,
                        Base64.encodeToString(salt, Base64.NO_WRAP)
                    )
                    putString(
                        AppDefaults.Auth.KEY_APP_PASSWORD_WRAP,
                        Base64.encodeToString(wrapped, Base64.NO_WRAP)
                    )
                    putInt(AppDefaults.Auth.KEY_APP_PASSWORD_FAILED_COUNT, 0)
                    putLong(AppDefaults.Auth.KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
                }
        }

    fun configureWithGeneratedPassphrase(
        context: Context,
        password: CharArray
    ): AppResult<ByteArray> {
        val newPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return configure(context, password, newPassphrase).map { newPassphrase }
    }

    fun change(
        context: Context,
        oldPassword: CharArray,
        newPassword: CharArray,
        currentPassphrase: ByteArray
    ): AppResult<Unit> = AppResult.runCatching("appPasswordStore.change") {
        val decryptedPassphrase = decryptWrappedPassphrase(context, oldPassword)
        try {
            if (!MessageDigest.isEqual(decryptedPassphrase, currentPassphrase)) {
                throw IllegalArgumentException("应用密码校验失败，请重新解锁后再试")
            }
        } finally {
            MemoryCleaner.wipeByteArray(decryptedPassphrase)
        }

        val salt = BackupManager.generateSalt()
        val wrapped = encryptWrappedPassphrase(currentPassphrase, newPassword, salt)
        context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(
                AppDefaults.Auth.KEY_APP_PASSWORD_SALT,
                Base64.encodeToString(salt, Base64.NO_WRAP)
            )
            putString(
                AppDefaults.Auth.KEY_APP_PASSWORD_WRAP,
                Base64.encodeToString(wrapped, Base64.NO_WRAP)
            )
            putInt(AppDefaults.Auth.KEY_APP_PASSWORD_FAILED_COUNT, 0)
            putLong(AppDefaults.Auth.KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
        }
    }

    fun disable(
        context: Context,
        password: CharArray,
        currentPassphrase: ByteArray
    ): AppResult<Unit> =
        AppResult.runCatching("appPasswordStore.disable") {
            val decryptedPassphrase = decryptWrappedPassphrase(context, password)
            try {
                if (!MessageDigest.isEqual(decryptedPassphrase, currentPassphrase)) {
                    throw IllegalArgumentException("应用密码校验失败")
                }
            } finally {
                MemoryCleaner.wipeByteArray(decryptedPassphrase)
            }

            context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    remove(AppDefaults.Auth.KEY_APP_PASSWORD_SALT)
                    remove(AppDefaults.Auth.KEY_APP_PASSWORD_WRAP)
                    remove(AppDefaults.Auth.KEY_APP_PASSWORD_FAILED_COUNT)
                    remove(AppDefaults.Auth.KEY_APP_PASSWORD_LOCKED_UNTIL)
                }
        }

    fun unlock(context: Context, password: CharArray): AppResult<ByteArray> {
        val prefs =
            context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
        val lockedUntil = prefs.getLong(AppDefaults.Auth.KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
        val now = System.currentTimeMillis()
        if (lockedUntil > now) {
            return AppResult.failure(AppError.AuthFailed("尝试过于频繁，请 ${lockedUntil - now} 秒后重试"))
        }

        return AppResult.runCatching("appPasswordStore.unlock") {
            decryptWrappedPassphrase(context, password)
        }.onSuccess {
            prefs.edit {
                putInt(AppDefaults.Auth.KEY_APP_PASSWORD_FAILED_COUNT, 0)
                putLong(AppDefaults.Auth.KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
            }
        }.mapFailure { error ->
            if (!isCredentialMismatch(error)) {
                return@mapFailure error
            }
            val nextCount = prefs.getInt(AppDefaults.Auth.KEY_APP_PASSWORD_FAILED_COUNT, 0) + 1
            val shouldLock = nextCount >= AppDefaults.Lock.APP_PASSWORD_MAX_FAILED_ATTEMPTS
            prefs.edit {
                putInt(
                    AppDefaults.Auth.KEY_APP_PASSWORD_FAILED_COUNT,
                    if (shouldLock) 0 else nextCount
                )
                if (shouldLock) {
                    putLong(
                        AppDefaults.Auth.KEY_APP_PASSWORD_LOCKED_UNTIL,
                        now + AppDefaults.Lock.MIN_APP_PASSWORD_LOCKOUT_MS
                    )
                }
            }
            if (shouldLock) {
                AppError.AuthFailed("密码错误次数过多，请 ${AppDefaults.Lock.MIN_APP_PASSWORD_LOCKOUT_MS / 1000} 秒后重试")
            } else {
                AppError.AuthFailed(AppDefaults.Auth.ERROR_APP_PASSWORD_MISMATCH)
            }
        }
    }

    private fun isCredentialMismatch(error: AppError): Boolean {
        return error.message == AppDefaults.Auth.ERROR_APP_PASSWORD_MISMATCH
    }
}