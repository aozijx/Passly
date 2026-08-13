package com.aozijx.passly.data.codec.revision

import com.aozijx.passly.data.model.entity.EntrySensitiveFieldEntity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SensitiveRevisionSnapshotCodecTest {
    private val codec = SensitiveRevisionSnapshotCodec()

    @Test
    fun `cipher snapshots round trip without plaintext decryption`() {
        val fields = listOf(
            EntrySensitiveFieldEntity(
                entryId = "entry-1",
                fieldKey = SensitiveFieldKey.CARD_CVV.name,
                valueCipher = byteArrayOf(3, 1, 4, 1, 5),
                keyVersion = 2,
                updatedAt = 10L,
            ),
            EntrySensitiveFieldEntity(
                entryId = "entry-1",
                fieldKey = SensitiveFieldKey.CARD_NUMBER.name,
                valueCipher = byteArrayOf(9, 2, 6, 5),
                keyVersion = 1,
                updatedAt = 11L,
            ),
        )

        val decoded = codec.decode(codec.encode(fields))

        assertEquals(
            listOf(SensitiveFieldKey.CARD_CVV, SensitiveFieldKey.CARD_NUMBER),
            decoded.map { it.key },
        )
        assertArrayEquals(byteArrayOf(3, 1, 4, 1, 5), decoded[0].valueCipher)
        assertEquals(2, decoded[0].keyVersion)
    }
}
