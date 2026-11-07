package com.aozijx.passly.security.envelope

import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KdfAlgorithm
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthenticationEnvelopeContextTest {
    private val parameters = AuthenticationKdfParameters(0x13, 65_536, 3, 4, 256)

    @Test
    fun `authentication metadata is bound to the wrapped key`() {
        val key = SecretKeySpec(ByteArray(32) { 7 }, "AES")
        val dek = ByteArray(32) { 9 }
        val envelope =
            EnvelopeManager.wrapWithKey(
                type = EnvelopeType.APP_PASSWORD,
                dek = dek,
                wrappingKey = key,
                salt = ByteArray(16) { 3 },
                algorithm = KdfAlgorithm.ARGON2ID,
                parameters = parameters,
            )

        fun decrypt(
            type: EnvelopeType,
            context: AuthenticationKdfParameters,
        ): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, envelope.iv))
            cipher.updateAAD(EnvelopeManager.authenticationAad(envelope.copy(type = type), context))
            return cipher.doFinal(envelope.ciphertext)
        }

        assertArrayEquals(dek, decrypt(EnvelopeType.APP_PASSWORD, parameters))
        assertThrows(AEADBadTagException::class.java) { decrypt(EnvelopeType.RECOVERY, parameters) }
        assertThrows(AEADBadTagException::class.java) {
            decrypt(EnvelopeType.APP_PASSWORD, parameters.copy(iterations = 4))
        }
        assertThrows(IllegalArgumentException::class.java) {
            EnvelopeManager.authenticationAad(envelope.copy(iv = ByteArray(0)), parameters)
        }
    }
}
