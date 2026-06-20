package com.aozijx.passly.core.auth.apppassword

import android.content.Context
import android.util.Base64
import com.aozijx.passly.core.auth.authconstants.AppPasswordConstants
import com.aozijx.passly.core.backup.BackupManager
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

internal fun encryptWrappedPassphrase(
    rawPassphrase: ByteArray,
    password: CharArray,
    salt: ByteArray
): ByteArray {
    val key = BackupManager.deriveKeyArgon2id(password, salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(Cipher.ENCRYPT_MODE, key)
    }
    val encrypted = cipher.doFinal(rawPassphrase)
    return ByteBuffer.allocate(cipher.iv.size + encrypted.size)
        .put(cipher.iv)
        .put(encrypted)
        .array()
}

internal fun decryptWrappedPassphrase(context: Context, password: CharArray): ByteArray {
    val prefs = context.getSharedPreferences(
        AppPasswordConstants.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    val wrappedPassphraseBase64 = prefs.getString(AppPasswordConstants.KEY_APP_PASSWORD_WRAP, null)
        ?: throw IllegalStateException("尚未设置应用密码")
    val passphraseSaltBase64 = prefs.getString(AppPasswordConstants.KEY_APP_PASSWORD_SALT, null)
        ?: throw IllegalStateException("应用密码配置不完整")

    val wrappedPassphrase = Base64.decode(wrappedPassphraseBase64, Base64.NO_WRAP)
    val passphraseSalt = Base64.decode(passphraseSaltBase64, Base64.NO_WRAP)
    val key = BackupManager.deriveKeyArgon2id(password, passphraseSalt)
    return try {
        val iv = ByteArray(AppPasswordConstants.PASSPHRASE_IV_LENGTH).also {
            ByteBuffer.wrap(wrappedPassphrase).get(it)
        }
        val encrypted =
            ByteArray(wrappedPassphrase.size - AppPasswordConstants.PASSPHRASE_IV_LENGTH).also {
                ByteBuffer.wrap(
                    wrappedPassphrase,
                    AppPasswordConstants.PASSPHRASE_IV_LENGTH,
                    wrappedPassphrase.size - AppPasswordConstants.PASSPHRASE_IV_LENGTH
                ).get(it)
            }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(
                Cipher.DECRYPT_MODE, key,
                GCMParameterSpec(AppPasswordConstants.PASSPHRASE_GCM_TAG_BITS, iv)
            )
        }
        cipher.doFinal(encrypted)
    } catch (_: javax.crypto.AEADBadTagException) {
        throw IllegalArgumentException(AppPasswordConstants.ERROR_APP_PASSWORD_MISMATCH)
    }
}