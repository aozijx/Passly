package com.aozijx.passly.core.auth.apppassword

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.aozijx.passly.core.auth.authconstants.AppPasswordConstants
import com.aozijx.passly.core.auth.authconstants.AuthLockConstants
import com.aozijx.passly.core.backup.BackupManager
import com.aozijx.passly.core.crypto.memory.MemoryCleaner
import java.security.MessageDigest
import java.security.SecureRandom

object AppPasswordPassphraseStore {

    private const val KEY_APP_PASSWORD_FAILED_COUNT = "db_phrase_app_failed_count"
    private const val KEY_APP_PASSWORD_LOCKED_UNTIL = "db_phrase_app_locked_until"

    fun isEnabled(context: Context): Boolean {
        val prefs =
            context.getSharedPreferences(AppPasswordConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(AppPasswordConstants.KEY_APP_PASSWORD_WRAP, null).isNullOrBlank() &&
                !prefs.getString(AppPasswordConstants.KEY_APP_PASSWORD_SALT, null).isNullOrBlank()
    }

    fun configure(context: Context, password: CharArray, passphrase: ByteArray): Result<Unit> =
        runCatching {
            val salt = BackupManager.generateSalt()
            val wrapped = encryptWrappedPassphrase(passphrase, password, salt)
            context.getSharedPreferences(AppPasswordConstants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    putString(
                        AppPasswordConstants.KEY_APP_PASSWORD_SALT,
                        Base64.encodeToString(salt, Base64.NO_WRAP)
                    )
                    putString(
                        AppPasswordConstants.KEY_APP_PASSWORD_WRAP,
                        Base64.encodeToString(wrapped, Base64.NO_WRAP)
                    )
                    putInt(KEY_APP_PASSWORD_FAILED_COUNT, 0)
                    putLong(KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
                }
        }

    fun configureWithGeneratedPassphrase(context: Context, password: CharArray): Result<ByteArray> =
        runCatching {
            val newPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            configure(context, password, newPassphrase).getOrThrow()
            newPassphrase
        }

    fun change(
        context: Context,
        oldPassword: CharArray,
        newPassword: CharArray,
        currentPassphrase: ByteArray
    ): Result<Unit> = runCatching {
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
        context.getSharedPreferences(AppPasswordConstants.PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(
                AppPasswordConstants.KEY_APP_PASSWORD_SALT,
                Base64.encodeToString(salt, Base64.NO_WRAP)
            )
            putString(
                AppPasswordConstants.KEY_APP_PASSWORD_WRAP,
                Base64.encodeToString(wrapped, Base64.NO_WRAP)
            )
            putInt(KEY_APP_PASSWORD_FAILED_COUNT, 0)
            putLong(KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
        }
    }

    fun disable(context: Context, password: CharArray, currentPassphrase: ByteArray): Result<Unit> =
        runCatching {
            val decryptedPassphrase = decryptWrappedPassphrase(context, password)
            try {
                if (!MessageDigest.isEqual(decryptedPassphrase, currentPassphrase)) {
                    throw IllegalArgumentException("应用密码校验失败")
                }
            } finally {
                MemoryCleaner.wipeByteArray(decryptedPassphrase)
            }

            context.getSharedPreferences(AppPasswordConstants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    remove(AppPasswordConstants.KEY_APP_PASSWORD_SALT)
                    remove(AppPasswordConstants.KEY_APP_PASSWORD_WRAP)
                    remove(KEY_APP_PASSWORD_FAILED_COUNT)
                    remove(KEY_APP_PASSWORD_LOCKED_UNTIL)
                }
        }

    fun unlock(context: Context, password: CharArray): Result<ByteArray> = runCatching {
        val prefs =
            context.getSharedPreferences(AppPasswordConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val lockedUntil = prefs.getLong(KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
        val now = System.currentTimeMillis()
        if (lockedUntil > now) {
            throw IllegalStateException("尝试过于频繁，请 ${lockedUntil - now} 秒后重试")
        }

        runCatching { decryptWrappedPassphrase(context, password) }
            .onSuccess {
                prefs.edit {
                    putInt(KEY_APP_PASSWORD_FAILED_COUNT, 0)
                    putLong(KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
                }
            }
            .onFailure { error ->
                if (!isCredentialMismatch(error)) {
                    throw error
                }

                val nextCount = prefs.getInt(KEY_APP_PASSWORD_FAILED_COUNT, 0) + 1
                val shouldLock = nextCount >= AuthLockConstants.APP_PASSWORD_MAX_FAILED_ATTEMPTS
                prefs.edit {
                    putInt(KEY_APP_PASSWORD_FAILED_COUNT, if (shouldLock) 0 else nextCount)
                    if (shouldLock) {
                        putLong(
                            KEY_APP_PASSWORD_LOCKED_UNTIL,
                            now + AuthLockConstants.MIN_APP_PASSWORD_LOCKOUT_MS
                        )
                    }
                }
                if (shouldLock) {
                    throw IllegalArgumentException("密码错误次数过多，请 ${AuthLockConstants.MIN_APP_PASSWORD_LOCKOUT_MS / 1000} 秒后重试")
                }
                throw IllegalArgumentException(AppPasswordConstants.ERROR_APP_PASSWORD_MISMATCH)
            }.getOrThrow()
    }

    private fun isCredentialMismatch(error: Throwable): Boolean {
        return error is IllegalArgumentException &&
                error.message == AppPasswordConstants.ERROR_APP_PASSWORD_MISMATCH
    }
}