package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.crypto.KeyDerivation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChunkedBackupStreamsTest {
    @Test
    fun chunkedStreams_roundTripAcrossRecords() {
        val plain = ByteArray(BackupContainerV1Spec.CHUNK_SIZE + 37) { (it % 239).toByte() }
        val encoded = ByteArrayOutputStream()
        ChunkedBackupOutputStream(encoded, password, parameters, ::deriveKey).use { it.write(plain) }

        val decoded =
            ChunkedBackupInputStream(
                ByteArrayInputStream(encoded.toByteArray()),
                password,
                ::deriveKey,
            ).use { it.readBytes() }

        assertArrayEquals(plain, decoded)
    }

    @Test
    fun decryptingStream_rejectsDamagedRecordTag() {
        val encoded = ByteArrayOutputStream()
        ChunkedBackupOutputStream(encoded, password, parameters, ::deriveKey).use { it.write(byteArrayOf(1, 2, 3, 4)) }
        val position = BackupContainerV1Spec.HEADER_LENGTH + BackupContainerV1Spec.RECORD_HEADER_LENGTH + 1
        val tampered = encoded.toByteArray().also { it[position] = (it[position].toInt() xor 1).toByte() }

        assertThrows(Exception::class.java) {
            ChunkedBackupInputStream(ByteArrayInputStream(tampered), password, ::deriveKey).use { it.readBytes() }
        }
    }

    @Test
    fun decryptingStream_rejectsTrailingBytesAfterTerminal() {
        val encoded = ByteArrayOutputStream()
        ChunkedBackupOutputStream(encoded, password, parameters, ::deriveKey).use { it.write(byteArrayOf(1, 2, 3)) }
        val tampered = encoded.toByteArray() + byteArrayOf(9)

        assertThrows(IllegalArgumentException::class.java) {
            ChunkedBackupInputStream(ByteArrayInputStream(tampered), password, ::deriveKey).use { it.readBytes() }
        }
    }

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        parameters: KeyDerivation.Argon2idParameters,
    ): ByteArray = ByteArray(parameters.keyLengthBits / Byte.SIZE_BITS) { index ->
        (password[index % password.size].code + salt[index % salt.size]).toByte()
    }

    companion object {
        private val password = "stream-password".toCharArray()
        private val parameters =
            KeyDerivation.Argon2idParameters(
                version = KeyDerivation.ARGON2_VERSION_13,
                iterations = 1,
                memoryKiB = 8 * 1024,
                parallelism = 1,
                keyLengthBits = 256,
            )
    }
}
