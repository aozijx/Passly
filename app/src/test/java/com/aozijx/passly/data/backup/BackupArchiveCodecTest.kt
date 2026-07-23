package com.aozijx.passly.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec

class BackupArchiveCodecTest {

    @Test
    fun roundTrip_preservesSnapshotsAndImages() {
        val password = "correct horse battery staple".toCharArray()
        val content = BackupArchiveContent(
            snapshotJson = """{"entries":[]}""".toByteArray(),
            images = mapOf(
                "images/abc123.bin" to byteArrayOf(0x01, 0x02, 0x03)
            )
        )

        val encoded = BackupArchiveCodec.encode(content, password, deriveKey = ::stubDeriveKey)
        val decoded = BackupArchiveCodec.decode(encoded, password, deriveKey = ::stubDeriveKey)

        assertArrayEquals(content.snapshotJson, decoded.snapshotJson)
        assertEquals(content.images.size, decoded.images.size)
        content.images.forEach { (key, value) ->
            assertArrayEquals(value, decoded.images[key])
        }
    }

    @Test
    fun decode_invalidMagic_throws() {
        val password = "password".toCharArray()
        val bogus = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchiveCodec.decode(bogus, password, deriveKey = ::stubDeriveKey)
        }
    }

    @Test
    fun roundTrip_largeSnapshot() {
        val password = "test password".toCharArray()
        val largeJson = ByteArray(50_000) { 'A'.code.toByte() }
        val content = BackupArchiveContent(
            snapshotJson = largeJson,
            images = emptyMap()
        )
        val encoded = BackupArchiveCodec.encode(content, password, deriveKey = ::stubDeriveKey)
        val decoded = BackupArchiveCodec.decode(encoded, password, deriveKey = ::stubDeriveKey)
        assertArrayEquals(content.snapshotJson, decoded.snapshotJson)
    }

    @Test
    fun encode_withMultipleImages() {
        val password = "multi image".toCharArray()
        val content = BackupArchiveContent(
            snapshotJson = """{"data":"test"}""".toByteArray(),
            images = mapOf(
                "images/img1.bin" to byteArrayOf(0x10),
                "images/img2.bin" to byteArrayOf(0x20, 0x30),
                "images/img3.bin" to byteArrayOf(0x40, 0x50, 0x60)
            )
        )
        val encoded = BackupArchiveCodec.encode(content, password, deriveKey = ::stubDeriveKey)
        val decoded = BackupArchiveCodec.decode(encoded, password, deriveKey = ::stubDeriveKey)
        assertArrayEquals(content.snapshotJson, decoded.snapshotJson)
        assertEquals(content.images.size, decoded.images.size)
        content.images.forEach { (key, value) ->
            assertArrayEquals(value, decoded.images[key])
        }
    }

    private fun stubDeriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val hash = MessageDigest.getInstance("SHA-256")
        hash.update(String(password).toByteArray())
        hash.update(salt)
        val key = hash.digest()
        return SecretKeySpec(key, "AES")
    }
}
