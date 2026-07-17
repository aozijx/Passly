package com.aozijx.passly.data.repository.backup.internal

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
            snapshotJson = """[{"id":"entry-1"}]""".toByteArray(),
            images = mapOf("images/icon.bin" to byteArrayOf(1, 2, 3, 4))
        )

        val encoded = BackupArchiveCodec.encode(content, password, ::testKey)
        val decoded = BackupArchiveCodec.decode(
            encoded,
            "correct horse battery staple".toCharArray(),
            ::testKey
        )

        assertArrayEquals(content.snapshotJson, decoded.snapshotJson)
        assertEquals(content.images.keys, decoded.images.keys)
        assertArrayEquals(content.images.getValue("images/icon.bin"), decoded.images["images/icon.bin"])
    }

    @Test
    fun decode_rejectsWrongPassword() {
        val encoded = BackupArchiveCodec.encode(
            BackupArchiveContent("[]".toByteArray(), emptyMap()),
            "right-password".toCharArray(),
            ::testKey
        )

        assertThrows(Exception::class.java) {
            BackupArchiveCodec.decode(encoded, "wrong-password".toCharArray(), ::testKey)
        }
    }

    @Test
    fun decode_rejectsTruncatedContainer() {
        val encoded = BackupArchiveCodec.encode(
            BackupArchiveContent("[]".toByteArray(), emptyMap()),
            "password".toCharArray(),
            ::testKey
        )

        assertThrows(Exception::class.java) {
            BackupArchiveCodec.decode(
                encoded.copyOf(encoded.size - 5),
                "password".toCharArray(),
                ::testKey
            )
        }
    }

    @Test
    fun encode_rejectsUnsafeImagePath() {
        val content = BackupArchiveContent(
            snapshotJson = "[]".toByteArray(),
            images = mapOf("images/../secret" to byteArrayOf(1))
        )

        assertThrows(IllegalArgumentException::class.java) {
            BackupArchiveCodec.encode(content, "password".toCharArray(), ::testKey)
        }
    }

    private fun testKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(String(password).toByteArray())
        digest.update(salt)
        return SecretKeySpec(digest.digest(), "AES")
    }
}
