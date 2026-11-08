package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import com.aozijx.passly.domain.autofill.model.AutofillInboxLimits
import com.aozijx.passly.domain.autofill.model.AutofillInboxSource
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillInboxRecordCodecTest {
    private val keyMaterialCodec = AutofillInboxKeyMaterialCodec()
    private val codec = AutofillInboxRecordCodec(keyMaterialCodec)
    private val dek = ByteArray(32) { (it * 3).toByte() }

    @After
    fun wipeDekFixture() {
        dek.fill(0)
    }

    @Test
    fun sealedDraftRoundTripsAndRepeatsItsDraftIdInsideAuthenticatedPayload() {
        val material = keyMaterialCodec.generateAndWrap(dek)
        val draftId = codec.createDraftId()
        val draft = draft()
        val record =
            codec.seal(
                material = material,
                draftId = draftId,
                source = AutofillInboxSource.LEGACY_SERVICE,
                createdAtMs = 1_800_000_000_000,
                capturedAtMs = 1_799_999_999_000,
                draft = draft,
            )

        val decoded =
            keyMaterialCodec.withPrivateKey(material, dek) { privateKey ->
                codec.open(material, privateKey, record)
            }

        assertEquals(draftId, decoded.draftId)
        assertEquals(AutofillInboxSource.LEGACY_SERVICE, decoded.source)
        assertEquals(1_800_000_000_000, decoded.queuedAtMs)
        assertEquals(1_799_999_999_000, decoded.capturedAtMs)
        assertEquals(draft, decoded.draft)
        assertTrue(record.size <= AutofillInboxLimits.MAX_RECORD_BYTES)
    }

    @Test
    fun recordAndPayloadKeepTheirIndependentV1MagicAndVersionBytes() {
        val material = keyMaterialCodec.generateAndWrap(dek)
        val record =
            codec.seal(
                material = material,
                draftId = codec.createDraftId(),
                source = AutofillInboxSource.LEGACY_SERVICE,
                createdAtMs = 1_800_000_000_000,
                capturedAtMs = 1_799_999_999_000,
                draft = draft(),
            )

        assertArrayEquals("PASSLYINBOX".toByteArray(StandardCharsets.US_ASCII), record.copyOfRange(0, 11))
        assertEquals(1, record[11].toInt())

        val payload = decryptPayload(material, record)
        try {
            assertArrayEquals("PASSLYDRAFT".toByteArray(StandardCharsets.US_ASCII), payload.copyOfRange(0, 11))
            assertEquals(1, payload[11].toInt())
        } finally {
            payload.fill(0)
        }
    }

    @Test
    fun headerTamperingFailsGcmAuthentication() {
        val material = keyMaterialCodec.generateAndWrap(dek)
        val record =
            codec
                .seal(
                    material = material,
                    draftId = codec.createDraftId(),
                    source = AutofillInboxSource.CREDENTIAL_MANAGER,
                    createdAtMs = 1_800_000_000_000,
                    capturedAtMs = 1_800_000_000_000,
                    draft = draft(),
                ).also { it[44] = (it[44].toInt() xor 1).toByte() }

        assertThrows(AEADBadTagException::class.java) {
            keyMaterialCodec.withPrivateKey(material, dek) { privateKey ->
                codec.open(material, privateKey, record)
            }
        }
    }

    @Test
    fun ciphertextTamperingFailsGcmAuthentication() {
        val material = keyMaterialCodec.generateAndWrap(dek)
        val record =
            codec
                .seal(
                    material = material,
                    draftId = codec.createDraftId(),
                    source = AutofillInboxSource.CREDENTIAL_MANAGER,
                    createdAtMs = 1_800_000_000_000,
                    capturedAtMs = 1_800_000_000_000,
                    draft = draft(),
                ).also { it[it.lastIndex] = (it[it.lastIndex].toInt() xor 1).toByte() }

        assertThrows(AEADBadTagException::class.java) {
            keyMaterialCodec.withPrivateKey(material, dek) { privateKey ->
                codec.open(material, privateKey, record)
            }
        }
    }

    @Test
    fun truncatedAndOversizedRecordsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.open(
                material = AutofillInboxKeyMaterial(1, ByteArray(16), ByteArray(0), ByteArray(32), ByteArray(0)),
                privateKey =
                    object : java.security.PrivateKey {
                        override fun getAlgorithm() = "RSA"

                        override fun getFormat() = "PKCS#8"

                        override fun getEncoded() = byteArrayOf()
                    },
                encoded = byteArrayOf(1, 2),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            codec.open(
                material = AutofillInboxKeyMaterial(1, ByteArray(16), ByteArray(0), ByteArray(32), ByteArray(0)),
                privateKey =
                    object : java.security.PrivateKey {
                        override fun getAlgorithm() = "RSA"

                        override fun getFormat() = "PKCS#8"

                        override fun getEncoded() = byteArrayOf()
                    },
                encoded = ByteArray(AutofillInboxLimits.MAX_RECORD_BYTES + 1),
            )
        }
    }

    @Test
    fun oversizedPasswordIsRejectedBeforeEncryption() {
        val material = keyMaterialCodec.generateAndWrap(dek)

        assertThrows(IllegalArgumentException::class.java) {
            codec.seal(
                material = material,
                draftId = codec.createDraftId(),
                source = AutofillInboxSource.LEGACY_SERVICE,
                createdAtMs = 1_800_000_000_000,
                capturedAtMs = 1_800_000_000_000,
                draft = draft().copy(password = "p".repeat(32 * 1024 + 1)),
            )
        }
    }

    private fun draft() =
        AutofillSaveDraft(
            packageName = "example.app",
            webDomain = "example.com",
            pageTitle = "Sign in",
            username = "user@example.com",
            password = "correct horse battery staple",
        )

    private fun decryptPayload(
        material: AutofillInboxKeyMaterial,
        record: ByteArray,
    ): ByteArray =
        keyMaterialCodec.withPrivateKey(material, dek) { privateKey ->
            val buffer = ByteBuffer.wrap(record).order(ByteOrder.BIG_ENDIAN)
            buffer.position(RECORD_AAD_HEADER_BYTES)
            val wrappedKeyLength = buffer.short.toInt() and 0xffff
            val wrappedKey = ByteArray(wrappedKeyLength).also(buffer::get)
            val nonce = ByteArray(GCM_NONCE_BYTES).also(buffer::get)
            val ciphertextLength = buffer.int
            val ciphertextAndTag = ByteArray(ciphertextLength + GCM_TAG_BYTES).also(buffer::get)
            var contentKey: ByteArray? = null
            try {
                contentKey = rsaCipher(Cipher.DECRYPT_MODE, privateKey).doFinal(wrappedKey)
                Cipher.getInstance("AES/GCM/NoPadding").run {
                    init(Cipher.DECRYPT_MODE, SecretKeySpec(contentKey, "AES"), GCMParameterSpec(128, nonce))
                    updateAAD(record.copyOfRange(0, RECORD_AAD_HEADER_BYTES) + material.publicKeySha256)
                    doFinal(ciphertextAndTag)
                }
            } finally {
                contentKey?.fill(0)
                wrappedKey.fill(0)
                ciphertextAndTag.fill(0)
            }
        }

    private fun rsaCipher(
        mode: Int,
        key: PrivateKey,
    ): Cipher =
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

    private companion object {
        const val RECORD_AAD_HEADER_BYTES = 52
        const val GCM_NONCE_BYTES = 12
        const val GCM_TAG_BYTES = 16
    }
}
