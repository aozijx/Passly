package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.core.observability.Reasons
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupContainerHeaderTest {
    @Test
    fun canonicalHeader_keepsContainerVersionOneAndArgonVersionFromParameters() {
        val salt = ByteArray(16) { it.toByte() }
        val noncePrefix = byteArrayOf(3, 5, 7, 9)
        val parameters =
            KeyDerivation.Argon2idParameters(
                version = KeyDerivation.ARGON2_VERSION_13,
                iterations = 3,
                memoryKiB = 32 * 1024,
                parallelism = 2,
                keyLengthBits = 256,
            )
        val encoded = BackupContainerHeader.encode(salt, noncePrefix, parameters)
        val decoded = BackupContainerHeader.read(ByteArrayInputStream(encoded))

        val fields = ByteBuffer.wrap(encoded).order(ByteOrder.BIG_ENDIAN)
        assertArrayEquals("PSLYBKP1".toByteArray(Charsets.US_ASCII), encoded.copyOfRange(0, 8))
        assertEquals(1, fields.getInt(8))
        assertEquals(80, fields.getInt(12))
        assertEquals(1, fields.getInt(16))
        assertEquals(1, fields.getInt(20))
        assertEquals(0x13, fields.getInt(24))
        assertEquals(3, fields.getInt(28))
        assertEquals(32 * 1024, fields.getInt(32))
        assertEquals(2, fields.getInt(36))
        assertEquals(256, fields.getInt(40))
        assertEquals(16, fields.getInt(44))
        assertEquals(4, fields.getInt(48))
        assertEquals(1024 * 1024, fields.getInt(52))
        assertEquals(16, fields.getInt(56))
        assertArrayEquals(salt, encoded.copyOfRange(60, 76))
        assertArrayEquals(noncePrefix, encoded.copyOfRange(76, 80))
        assertEquals(1, decoded.containerVersion)
        assertEquals(parameters.version, decoded.kdfParameters.version)
        assertEquals(parameters, decoded.kdfParameters)
        assertArrayEquals(salt, decoded.salt)
        assertArrayEquals(noncePrefix, decoded.noncePrefix)
    }

    @Test
    fun headerRead_rejectsLegacySingleBlockHeaderLength() {
        val oldHeader = ByteArray(88)
        ByteBuffer.wrap(oldHeader).order(ByteOrder.BIG_ENDIAN).apply {
            put("PSLYBKP1".toByteArray(Charsets.US_ASCII))
            putInt(1)
            putInt(88)
        }

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                BackupContainerHeader.read(ByteArrayInputStream(oldHeader))
            }
        assertEquals("备份头长度无效", error.message)
    }

    @Test
    fun headerRead_rejectsUnsupportedFixedFieldsBeforeKeyDerivation() {
        val salt = ByteArray(16) { it.toByte() }
        val noncePrefix = byteArrayOf(3, 5, 7, 9)
        val parameters =
            KeyDerivation.Argon2idParameters(
                version = KeyDerivation.ARGON2_VERSION_13,
                iterations = 3,
                memoryKiB = 32 * 1024,
                parallelism = 2,
                keyLengthBits = 256,
            )
        val validHeader = BackupContainerHeader.encode(salt, noncePrefix, parameters)
        val invalidHeaders =
            listOf(
                "magic" to validHeader.clone().also { it[0] = 'X'.code.toByte() },
                "container version" to withInt(validHeader, 8, 2),
                "header length" to withInt(validHeader, 12, 88),
                "KDF identifier" to withInt(validHeader, 16, 2),
                "cipher identifier" to withInt(validHeader, 20, 2),
                "Argon2 version" to withInt(validHeader, 24, 0x10),
                "iteration limit" to withInt(validHeader, 28, 11),
                "memory limit" to withInt(validHeader, 32, 256 * 1024 + 1),
                "parallelism limit" to withInt(validHeader, 36, 9),
                "key length" to withInt(validHeader, 40, 128),
                "salt length" to withInt(validHeader, 44, 15),
                "nonce prefix length" to withInt(validHeader, 48, 5),
                "chunk size" to withInt(validHeader, 52, 512),
                "tag length" to withInt(validHeader, 56, 12),
            )

        for ((field, invalidHeader) in invalidHeaders) {
            val password = "header-password".toCharArray()
            var derivationStarted = false
            try {
                assertThrows(IllegalArgumentException::class.java) {
                    ChunkedBackupInputStream(
                        input = ByteArrayInputStream(invalidHeader),
                        password = password,
                        deriveKey = { _, _, _ ->
                            derivationStarted = true
                            throw IllegalStateException("KDF started for an invalid header")
                        },
                    )
                }
                assertFalse("$field must be rejected before KDF", derivationStarted)
            } finally {
                password.fill('\u0000')
                invalidHeader.fill(0)
            }
        }
    }

    @Test
    fun headerRead_rejectsTruncationAtFixedHeaderSaltAndNoncePrefixBeforeKeyDerivation() {
        val validHeader =
            BackupContainerHeader.encode(
                salt = ByteArray(16) { it.toByte() },
                noncePrefix = byteArrayOf(3, 5, 7, 9),
                parameters = testParameters(),
            )
        val truncatedHeaders =
            listOf(
                "fixed header" to validHeader.copyOf(59),
                "salt" to validHeader.copyOf(60 + 15),
                "nonce prefix" to validHeader.copyOf(76 + 3),
            )

        for ((field, truncatedHeader) in truncatedHeaders) {
            val password = "header-password".toCharArray()
            var derivationStarted = false
            try {
                val failure = assertThrows(BackupTruncatedException::class.java) {
                    ChunkedBackupInputStream(
                        input = ByteArrayInputStream(truncatedHeader),
                        password = password,
                        deriveKey = { _, _, _ ->
                            derivationStarted = true
                            throw IllegalStateException("KDF started for truncated $field")
                        },
                    )
                }
                assertEquals(Reasons.Backup.Container.BackupContainerHeaderTruncated, failure.reasonCode)
                assertFalse("$field truncation must be rejected before KDF", derivationStarted)
            } finally {
                password.fill('\u0000')
                truncatedHeader.fill(0)
            }
        }
    }

    private fun testParameters() =
        KeyDerivation.Argon2idParameters(
            version = KeyDerivation.ARGON2_VERSION_13,
            iterations = 3,
            memoryKiB = 32 * 1024,
            parallelism = 2,
            keyLengthBits = 256,
        )

    private fun withInt(
        header: ByteArray,
        offset: Int,
        value: Int,
    ): ByteArray =
        header.clone().also { bytes ->
            ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).putInt(offset, value)
        }
}
