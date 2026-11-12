package com.aozijx.passly.data.codec.revision

import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SensitiveRevisionSnapshotCodecTest {
    private val codec = SensitiveRevisionSnapshotCodec()

    @Test
    fun `cipher snapshots round trip without plaintext decryption`() {
        val fields =
            listOf(
                EntrySecretFieldEntity(
                    entryId = "entry-1",
                    fieldKey = "CARD_NUMBER",
                    valueCipher = byteArrayOf(9, 2, 6, 5),
                    keyVersion = 1,
                    updatedAt = 11L,
                ),
                EntrySecretFieldEntity(
                    entryId = "entry-1",
                    fieldKey = "CARD_CVV",
                    valueCipher = byteArrayOf(3, 1, 4, 1, 5),
                    keyVersion = 2,
                    updatedAt = 10L,
                ),
            )

        val decoded = codec.decode(codec.encode(fields))

        assertEquals(
            listOf(SensitiveFieldKey.CARD_CVV, SensitiveFieldKey.CARD_NUMBER),
            decoded.map { it.key },
        )
        assertArrayEquals(byteArrayOf(3, 1, 4, 1, 5), decoded[0].valueCipher)
        assertEquals(2, decoded[0].keyVersion)
        assertArrayEquals(byteArrayOf(9, 2, 6, 5), decoded[1].valueCipher)
        assertEquals(1, decoded[1].keyVersion)
    }

    @Test
    fun `field presence can be decoded without returning ciphertext values`() {
        val encoded =
            codec.encode(
                listOf(
                    EntrySecretFieldEntity(
                        entryId = "entry-1",
                        fieldKey = "PASSWORD",
                        valueCipher = byteArrayOf(1, 2, 3),
                        keyVersion = 1,
                        updatedAt = 10L,
                    ),
                ),
            )

        assertEquals(setOf(SensitiveFieldKey.PASSWORD), codec.decodeKeys(encoded))
    }

    @Test
    fun `fingerprints are stable for identical ciphertext and change with stored value`() {
        val first = codec.decodeFingerprints(codec.encode(listOf(field(byteArrayOf(1, 2, 3)))))
        val same = codec.decodeFingerprints(codec.encode(listOf(field(byteArrayOf(1, 2, 3)))))
        val changed = codec.decodeFingerprints(codec.encode(listOf(field(byteArrayOf(4, 5, 6)))))
        val rotatedKey =
            codec.decodeFingerprints(
                codec.encode(listOf(field(byteArrayOf(1, 2, 3), keyVersion = 2))),
            )

        assertEquals(first, same)
        assertNotEquals(first, changed)
        assertNotEquals(first, rotatedKey)
        assertEquals(setOf(SensitiveFieldKey.PASSWORD), first.keys)
    }

    private fun field(
        cipher: ByteArray,
        keyVersion: Int = 1,
    ) = EntrySecretFieldEntity(
        entryId = "entry",
        fieldKey = "PASSWORD",
        valueCipher = cipher,
        keyVersion = keyVersion,
        updatedAt = 1L,
    )
}
