package com.aozijx.passly.security.authentication

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import com.aozijx.passly.security.crypto.CryptoConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
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
    internal fun createDecrypt(iv: ByteArray): BiometricCryptoPreparation = try {
        val alias = "${context.packageName}.${CryptoConfig.KEYSTORE_ALIAS_SUFFIX}"
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
}
