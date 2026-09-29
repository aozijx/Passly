package com.aozijx.passly.data.codec.revision

import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SensitiveRevisionFingerprintTest {
    private val codec = SensitiveRevisionSnapshotCodec()

    @Test
    fun `fingerprints are stable for identical ciphertext and change with stored value`() {
        val first = codec.decodeFingerprints(codec.encode(listOf(field(byteArrayOf(1, 2, 3)))))
        val same = codec.decodeFingerprints(codec.encode(listOf(field(byteArrayOf(1, 2, 3)))))
        val changed = codec.decodeFingerprints(codec.encode(listOf(field(byteArrayOf(4, 5, 6)))))

        assertEquals(first, same)
        assertNotEquals(first, changed)
        assertEquals(setOf(SensitiveFieldKey.PASSWORD), first.keys)
    }

    private fun field(cipher: ByteArray) = EntrySecretFieldEntity(
        entryId = "entry",
        fieldKey = SensitiveFieldKey.PASSWORD.name,
        valueCipher = cipher,
        keyVersion = 1,
        updatedAt = 1L,
    )
}
