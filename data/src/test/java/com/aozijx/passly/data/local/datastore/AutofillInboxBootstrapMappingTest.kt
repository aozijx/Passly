package com.aozijx.passly.data.local.datastore

import com.aozijx.passly.data.crypto.proto.BootstrapData
import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AutofillInboxBootstrapMappingTest {
    @Test
    fun absentInboxKeyMaterialRemainsUnprovisioned() {
        assertNull(BootstrapData.getDefaultInstance().readAutofillInboxKeyMaterial())
    }

    @Test
    fun keyMaterialRoundTripsWithoutChangingExistingBootstrapFields() {
        val material = AutofillInboxKeyMaterial(
            formatVersion = 1,
            keyId = byteArrayOf(1, 2, 3),
            publicKey = byteArrayOf(4, 5, 6),
            publicKeySha256 = byteArrayOf(7, 8, 9),
            privateKeyEnvelope = byteArrayOf(10, 11, 12),
        )
        val data = BootstrapData.newBuilder()
            .setVersion(1)
            .setVerificationTag(com.google.protobuf.ByteString.copyFrom(byteArrayOf(42)))
            .setAutofillInboxKeyMaterial(material)
            .build()

        val decoded = BootstrapData.parseFrom(data.toByteArray()).readAutofillInboxKeyMaterial()

        assertEquals(material, decoded)
        assertEquals(1, data.version)
        assertArrayEquals(byteArrayOf(42), data.verificationTag.toByteArray())
    }

    @Test
    fun malformedButPresentMaterialIsNotSilentlyTreatedAsMissing() {
        val data = BootstrapData.newBuilder()
            .setAutofillInboxKey(
                com.aozijx.passly.data.crypto.proto.AutofillInboxKeyMaterial.getDefaultInstance(),
            )
            .build()

        val decoded = data.readAutofillInboxKeyMaterial()

        assertNotNull(decoded)
        assertEquals(0, decoded?.formatVersion)
        assertArrayEquals(byteArrayOf(), decoded?.keyId)
    }

    @Test
    fun clearingInboxMaterialPreservesOtherBootstrapFields() {
        val data = BootstrapData.newBuilder()
            .setVersion(1)
            .setVerificationTag(com.google.protobuf.ByteString.copyFrom(byteArrayOf(42)))
            .setAutofillInboxKey(
                com.aozijx.passly.data.crypto.proto.AutofillInboxKeyMaterial.newBuilder()
                    .setFormatVersion(1),
            )
            .build()

        val cleared = data.toBuilder().clearAutofillInboxKeyMaterial().build()

        assertNull(cleared.readAutofillInboxKeyMaterial())
        assertEquals(1, cleared.version)
        assertArrayEquals(byteArrayOf(42), cleared.verificationTag.toByteArray())
    }
}
