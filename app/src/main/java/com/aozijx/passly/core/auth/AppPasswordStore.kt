package com.aozijx.passly.core.auth

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.aozijx.passly.core.backup.BackupManager
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object AppPasswordStore {
    private const val PREFS_NAME = "secure_db_prefs"
    private const val KEY_APP_PASSWORD_WRAP = "db_phrase_app_wrap"
    private const val KEY_APP_PASSWORD_SALT = "db_phrase_app_salt"
    private const val KEY_APP_PASSWORD_FAILED_COUNT = "db_phrase_app_failed_count"
    private const val KEY_APP_PASSWORD_LOCKED_UNTIL = "db_phrase_app_locked_until"

    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val APP_PASSWORD_MAX_FAILED_ATTEMPTS = 5
    private const val APP_PASSWORD_LOCKOUT_MS = 30_000L
    private const val ERROR_APP_PASSWORD_INVALID = "应用密码错误"

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_APP_PASSWORD_WRAP, null).isNullOrBlank() &&
                !prefs.getString(KEY_APP_PASSWORD_SALT, null).isNullOrBlank()
    }

    fun configure(context: Context, password: CharArray, passphrase: ByteArray): Result<Unit> =
        runCatching {
            val salt = BackupManager.generateSalt()
            val wrapped = encryptPassphrase(passphrase, password, salt)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY_APP_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                putString(KEY_APP_PASSWORD_WRAP, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                putInt(KEY_APP_PASSWORD_FAILED_COUNT, 0)
                putLong(KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
            }
        }

    fun configureWithGeneratedPassphrase(context: Context, password: CharArray): Result<ByteArray> =
        runCatching {
            val generatedPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            configure(context, password, generatedPassphrase).getOrThrow()
            generatedPassphrase
        }

    fun change(
        context: Context,
        oldPassword: CharArray,
        newPassword: CharArray,
        currentPassphrase: ByteArray
    ): Result<Unit> = runCatching {
        val decrypted = decryptPassphrase(context, oldPassword)
        try {
            if (!MessageDigest.isEqual(decrypted, currentPassphrase)) {
                throw IllegalArgumentException("应用密码校验失败，请重新解锁后再试")
            }
        } finally {
            decrypted.fill(0)
        }

        val salt = BackupManager.generateSalt()
        val wrapped = encryptPassphrase(currentPassphrase, newPassword, salt)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_APP_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            putString(KEY_APP_PASSWORD_WRAP, Base64.encodeToString(wrapped, Base64.NO_WRAP))
            putInt(KEY_APP_PASSWORD_FAILED_COUNT, 0)
            putLong(KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
        }
    }

    fun disable(context: Context, password: CharArray, currentPassphrase: ByteArray): Result<Unit> =
        runCatching {
            val decrypted = decryptPassphrase(context, password)
            try {
                if (!MessageDigest.isEqual(decrypted, currentPassphrase)) {
                    throw IllegalArgumentException("应用密码校验失败")
                }
            } finally {
                decrypted.fill(0)
            }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                remove(KEY_APP_PASSWORD_SALT)
                remove(KEY_APP_PASSWORD_WRAP)
                remove(KEY_APP_PASSWORD_FAILED_COUNT)
                remove(KEY_APP_PASSWORD_LOCKED_UNTIL)
            }
        }

    fun unlock(context: Context, password: CharArray): Result<ByteArray> = runCatching {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockedUntil = prefs.getLong(KEY_APP_PASSWORD_LOCKED_UNTIL, 0L)
        val now = System.currentTimeMillis()
        if (lockedUntil > now) {
            val remainSeconds = ((lockedUntil - now) / 1000L).coerceAtLeast(1L)
            throw IllegalStateException("尝试过于频繁，请在 $remainSeconds 秒后重试")
        }

        runCatching { decryptPassphrase(context, password) }
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
                val shouldLock = nextCount >= APP_PASSWORD_MAX_FAILED_ATTEMPTS
                prefs.edit {
                    putInt(KEY_APP_PASSWORD_FAILED_COUNT, if (shouldLock) 0 else nextCount)
                    if (shouldLock) {
                        putLong(KEY_APP_PASSWORD_LOCKED_UNTIL, now + APP_PASSWORD_LOCKOUT_MS)
                    }
                }
                if (shouldLock) {
                    throw IllegalArgumentException("密码错误次数过多，请 30 秒后重试")
                }
                throw IllegalArgumentException(ERROR_APP_PASSWORD_INVALID)
            }.getOrThrow()
    }

    private fun isCredentialMismatch(error: Throwable): Boolean {
        return error is IllegalArgumentException && error.message == ERROR_APP_PASSWORD_INVALID
    }

    private fun encryptPassphrase(
        passphrase: ByteArray,
        password: CharArray,
        salt: ByteArray
    ): ByteArray {
        val key = BackupManager.deriveKeyArgon2id(password, salt)
        val cipher =
            Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        val encrypted = cipher.doFinal(passphrase)
        return ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
    }

    private fun decryptPassphrase(context: Context, password: CharArray): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wrapBase64 = prefs.getString(KEY_APP_PASSWORD_WRAP, null)
            ?: throw IllegalStateException("尚未设置应用密码")
        val saltBase64 = prefs.getString(KEY_APP_PASSWORD_SALT, null)
            ?: throw IllegalStateException("应用密码配置不完整")

        val wrapped = Base64.decode(wrapBase64, Base64.NO_WRAP)
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val key = BackupManager.deriveKeyArgon2id(password, salt)
        return try {
            val iv = ByteArray(IV_LENGTH).also { ByteBuffer.wrap(wrapped).get(it) }
            val encrypted = ByteArray(wrapped.size - IV_LENGTH).also {
                ByteBuffer.wrap(wrapped, IV_LENGTH, wrapped.size - IV_LENGTH).get(it)
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            cipher.doFinal(encrypted)
        } catch (_: AEADBadTagException) {
            throw IllegalArgumentException(ERROR_APP_PASSWORD_INVALID)
        }
    }
}