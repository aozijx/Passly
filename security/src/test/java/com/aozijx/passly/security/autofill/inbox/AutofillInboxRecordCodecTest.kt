package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import com.aozijx.passly.domain.autofill.model.AutofillInboxLimits
import com.aozijx.passly.domain.autofill.model.AutofillInboxSource
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class AutofillInboxRecordCodecTest {
    private val keyMaterialCodec = AutofillInboxKeyMaterialCodec()
    private val codec = AutofillInboxRecordCodec(keyMaterialCodec)
    private val dek = ByteArray(32) { (it * 3).toByte() }

    @Test
    fun sealedDraftRoundTripsAndRepeatsItsDraftIdInsideAuthenticatedPayload() {
        val material = keyMaterialCodec.generateAndWrap(dek)
        val draftId = codec.createDraftId()
        val draft = draft()
        val record = codec.seal(
            material = material,
            draftId = draftId,
            source = AutofillInboxSource.LEGACY_SERVICE,
            createdAtMs = 1_800_000_000_000,
            capturedAtMs = 1_799_999_999_000,
            draft = draft,
        )

        val decoded = keyMaterialCodec.withPrivateKey(material, dek) { privateKey ->
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
    fun headerTamperingFailsGcmAuthentication() {
        val material = keyMaterialCodec.generateAndWrap(dek)
        val record = codec.seal(
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
        val record = codec.seal(
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
                privateKey = object : java.security.PrivateKey {
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
                privateKey = object : java.security.PrivateKey {
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

    private fun draft() = AutofillSaveDraft(
        packageName = "example.app",
        webDomain = "example.com",
        pageTitle = "Sign in",
        username = "user@example.com",
        password = "correct horse battery staple",
    )
}
