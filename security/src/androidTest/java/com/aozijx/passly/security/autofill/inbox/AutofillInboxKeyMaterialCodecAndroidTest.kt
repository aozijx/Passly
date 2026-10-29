package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.Key
import java.security.interfaces.RSAPublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class AutofillInboxKeyMaterialCodecAndroidTest {
    @Test
    fun generatesAndRestoresInboxKeyOnAndroidProviders() {
        val codec = AutofillInboxKeyMaterialCodec()
        val dek = ByteArray(32) { it.toByte() }
        val material = codec.generateAndWrap(dek)
        val publicKey = codec.decodePublicKey(material.publicKey)

        assertEquals(AutofillInboxKeyMaterial.FORMAT_VERSION_CURRENT, material.formatVersion)
        assertTrue(publicKey is RSAPublicKey)
        assertEquals(3072, (publicKey as RSAPublicKey).modulus.bitLength())

        val plaintext = byteArrayOf(4, 5, 6)
        val cipherText = rsaCipher(Cipher.ENCRYPT_MODE, publicKey).doFinal(plaintext)
        val restored = codec.withPrivateKey(material, dek) { privateKey ->
            rsaCipher(Cipher.DECRYPT_MODE, privateKey).doFinal(cipherText)
        }

        assertEquals(plaintext.toList(), restored.toList())
    }

    private fun rsaCipher(mode: Int, key: Key): Cipher =
        Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").apply {
            init(
                mode,
                key,
                OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT,
                ),
            )
        }
}
