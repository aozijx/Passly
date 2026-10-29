package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import com.aozijx.passly.domain.autofill.model.AutofillInboxSource
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
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

        val recordCodec = AutofillInboxRecordCodec(codec)
        val draftId = recordCodec.createDraftId()
        val record = recordCodec.seal(
            material = material,
            draftId = draftId,
            source = AutofillInboxSource.CREDENTIAL_MANAGER,
            createdAtMs = 1_800_000_000_000,
            capturedAtMs = 1_799_999_999_000,
            draft = AutofillSaveDraft(
                packageName = "example.app",
                webDomain = "example.com",
                pageTitle = "Sign in",
                username = "person@example.com",
                password = "secret value",
            ),
        )
        val reopened = codec.withPrivateKey(material, dek) { privateKey ->
            recordCodec.open(material, privateKey, record)
        }

        assertEquals(draftId, reopened.draftId)
        assertEquals(AutofillInboxSource.CREDENTIAL_MANAGER, reopened.source)
        assertEquals("secret value", reopened.draft.password)
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
