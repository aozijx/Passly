package com.aozijx.passly.security.authentication

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.aozijx.passly.core.crypto.CryptoConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

internal sealed interface BiometricCryptoPreparation {
    data class Ready(val cipher: Cipher) : BiometricCryptoPreparation
    data object KeyMissing : BiometricCryptoPreparation
    data object KeyInvalidated : BiometricCryptoPreparation
    data object Invalid : BiometricCryptoPreparation
}

@Singleton
class BiometricCryptoFactory @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    internal fun baseAlias(): String = "${context.packageName}.${CryptoConfig.KEYSTORE_ALIAS_SUFFIX}"

    internal fun createDecrypt(
        alias: String,
        iv: ByteArray
    ): BiometricCryptoPreparation = try {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(alias)) return BiometricCryptoPreparation.KeyMissing
        val key = (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: return BiometricCryptoPreparation.KeyMissing
        val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, iv)
        )
        BiometricCryptoPreparation.Ready(cipher)
    } catch (_: KeyPermanentlyInvalidatedException) {
        BiometricCryptoPreparation.KeyInvalidated
    } catch (_: IllegalArgumentException) {
        BiometricCryptoPreparation.Invalid
    } catch (_: Exception) {
        BiometricCryptoPreparation.Invalid
    }

    internal fun createEncrypt(
        alias: String,
        invalidateOnEnrollment: Boolean
    ): BiometricCryptoPreparation = try {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(alias)) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(CryptoConfig.KEY_SIZE_BITS)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    .setInvalidatedByBiometricEnrollment(invalidateOnEnrollment)
                    .build()
            )
            generator.generateKey()
        }
        val key = (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        BiometricCryptoPreparation.Ready(cipher)
    } catch (_: KeyPermanentlyInvalidatedException) {
        BiometricCryptoPreparation.KeyInvalidated
    } catch (_: Exception) {
        BiometricCryptoPreparation.Invalid
    }

    internal fun deleteAlias(alias: String): Boolean = runCatching {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        true
    }.getOrDefault(false)
}
