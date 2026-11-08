package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.crypto.KeyDerivation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
    fun decryptingStream_rejectsDamagedDataRecordTag() {
        val encoded = ByteArrayOutputStream()
        ChunkedBackupOutputStream(encoded, password, parameters, ::deriveKey).use { it.write(byteArrayOf(1, 2, 3, 4)) }
        val position =
            BackupContainerV1Spec.HEADER_LENGTH +
                BackupContainerV1Spec.RECORD_HEADER_LENGTH +
                4
        val tampered = encoded.toByteArray().also { it[position] = (it[position].toInt() xor 1).toByte() }

        assertThrows(AEADBadTagException::class.java) {
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

    @Test
    fun decryptingStream_acceptsEmptyArchiveWithAuthenticatedTerminal() {
        val encoded = encode(byteArrayOf())

        val decoded =
            ChunkedBackupInputStream(ByteArrayInputStream(encoded), password, ::deriveKey).use {
                it.readBytes()
            }

        assertEquals(
            BackupContainerV1Spec.HEADER_LENGTH + BackupContainerV1Spec.RECORD_HEADER_LENGTH + 32,
            encoded.size,
        )
        assertArrayEquals(byteArrayOf(), decoded)
    }

    @Test
    fun decryptingStream_rejectsMissingTerminal() {
        val encoded = encode(byteArrayOf(1, 2, 3))
        val withoutTerminal = encoded.copyOf(encoded.size - BackupContainerV1Spec.RECORD_HEADER_LENGTH - 32)

        assertThrows(EOFException::class.java) {
            ChunkedBackupInputStream(ByteArrayInputStream(withoutTerminal), password, ::deriveKey).use {
                it.readBytes()
            }
        }
    }

    @Test
    fun decryptingStream_rejectsDuplicateSkippedAndReorderedSequences() {
        val chunk = ByteArray(BackupContainerV1Spec.CHUNK_SIZE) { it.toByte() }
        val twoRecords = encode(chunk + chunk)
        val firstRecordSize =
            BackupContainerV1Spec.RECORD_HEADER_LENGTH +
                BackupContainerV1Spec.CHUNK_SIZE +
                BackupContainerV1Spec.TAG_LENGTH
        val recordsStart = BackupContainerV1Spec.HEADER_LENGTH
        val secondSequenceOffset = recordsStart + firstRecordSize + 1

        val duplicate = twoRecords.clone().also { putLong(it, secondSequenceOffset, 0L) }
        val skipped = encode(byteArrayOf(1, 2, 3)).also { putLong(it, BackupContainerV1Spec.HEADER_LENGTH + 1, 2L) }
        val reordered = twoRecords.clone().also { bytes ->
            val first = bytes.copyOfRange(recordsStart, recordsStart + firstRecordSize)
            val secondStart = recordsStart + firstRecordSize
            val second = bytes.copyOfRange(secondStart, secondStart + firstRecordSize)
            second.copyInto(bytes, recordsStart)
            first.copyInto(bytes, secondStart)
            first.fill(0)
            second.fill(0)
        }

        for (invalid in listOf(duplicate, skipped, reordered)) {
            assertThrows(IllegalArgumentException::class.java) {
                ChunkedBackupInputStream(ByteArrayInputStream(invalid), password, ::deriveKey).use { it.readBytes() }
            }
        }
    }

    @Test
    fun decryptingStream_rejectsUnknownTypeAndInvalidRecordLength() {
        val encoded = encode(byteArrayOf(1, 2, 3))
        val unknownType = encoded.clone().also { it[BackupContainerV1Spec.HEADER_LENGTH] = 9 }
        val invalidLength = encoded.clone().also { putInt(it, BackupContainerV1Spec.HEADER_LENGTH + 9, 0) }
        val invalidTerminalLength = encoded.clone().also {
            val terminalHeaderOffset =
                it.size -
                    BackupContainerV1Spec.RECORD_HEADER_LENGTH -
                    BackupContainerV1Spec.TERMINAL_PLAINTEXT_LENGTH -
                    BackupContainerV1Spec.TAG_LENGTH
            putInt(it, terminalHeaderOffset + 9, BackupContainerV1Spec.TERMINAL_PLAINTEXT_LENGTH - 1)
        }

        for (invalid in listOf(unknownType, invalidLength, invalidTerminalLength)) {
            assertThrows(IllegalArgumentException::class.java) {
                ChunkedBackupInputStream(ByteArrayInputStream(invalid), password, ::deriveKey).use { it.readBytes() }
            }
        }
    }

    @Test
    fun decryptingStream_rejectsTerminalCountAndTotalTampering() {
        val encoded = encode(byteArrayOf(1, 2, 3))
        val terminalOffset =
            BackupContainerV1Spec.HEADER_LENGTH +
                BackupContainerV1Spec.RECORD_HEADER_LENGTH +
                3 +
                BackupContainerV1Spec.TAG_LENGTH
        val wrongCount = rewriteTerminal(encoded) { putLong(it, 0, 2L) }
        val wrongTotal = rewriteTerminal(encoded) { putLong(it, 8, 4L) }
        val damagedTag = encoded.clone().also {
            val terminalTagOffset = it.size - BackupContainerV1Spec.TAG_LENGTH
            it[terminalTagOffset + 1] = (it[terminalTagOffset + 1].toInt() xor 1).toByte()
        }

        for (invalid in listOf(wrongCount, wrongTotal)) {
            assertThrows(IllegalArgumentException::class.java) {
                ChunkedBackupInputStream(ByteArrayInputStream(invalid), password, ::deriveKey).use { it.readBytes() }
            }
        }
        assertThrows(AEADBadTagException::class.java) {
            ChunkedBackupInputStream(ByteArrayInputStream(damagedTag), password, ::deriveKey).use { it.readBytes() }
        }
        assertEquals(BackupContainerV1Spec.TERMINAL_RECORD_TYPE, encoded[terminalOffset].toInt())
    }

    @Test
    fun decryptingStream_rejectsTruncatedRecordHeaderAndCiphertext() {
        val encoded = encode(byteArrayOf(1, 2, 3))
        val recordsStart = BackupContainerV1Spec.HEADER_LENGTH
        val headerTruncated = encoded.copyOf(recordsStart + BackupContainerV1Spec.RECORD_HEADER_LENGTH - 1)
        val ciphertextTruncated = encoded.copyOf(recordsStart + BackupContainerV1Spec.RECORD_HEADER_LENGTH + 2)

        for (invalid in listOf(headerTruncated, ciphertextTruncated)) {
            assertThrows(EOFException::class.java) {
                ChunkedBackupInputStream(ByteArrayInputStream(invalid), password, ::deriveKey).use { it.readBytes() }
            }
        }
    }

    private fun encode(plain: ByteArray): ByteArray {
        val encoded = ByteArrayOutputStream()
        ChunkedBackupOutputStream(encoded, password, parameters, ::deriveKey).use { it.write(plain) }
        return encoded.toByteArray()
    }

    private fun rewriteTerminal(
        encoded: ByteArray,
        mutateTotals: (ByteArray) -> Unit,
    ): ByteArray {
        val rewritten = encoded.clone()
        val header = rewritten.copyOfRange(0, BackupContainerV1Spec.HEADER_LENGTH)
        val parsed = BackupContainerHeader.read(ByteArrayInputStream(header))
        val terminalOffset =
            rewritten.size -
                BackupContainerV1Spec.RECORD_HEADER_LENGTH -
                BackupContainerV1Spec.TERMINAL_PLAINTEXT_LENGTH -
                BackupContainerV1Spec.TAG_LENGTH
        val recordHeader =
            rewritten.copyOfRange(terminalOffset, terminalOffset + BackupContainerV1Spec.RECORD_HEADER_LENGTH)
        val terminalPlaintext =
            ByteBuffer
                .allocate(BackupContainerV1Spec.TERMINAL_PLAINTEXT_LENGTH)
                .putLong(1)
                .putLong(3)
                .array()
        mutateTotals(terminalPlaintext)
        val nonce = BackupRecordCrypto.nonce(parsed.noncePrefix, 1)
        val aad = BackupRecordCrypto.aad(parsed.authenticatedBytes, recordHeader)
        var key = ByteArray(0)
        var ciphertext = ByteArray(0)
        try {
            key = deriveKey(password, parsed.salt, parsed.kdfParameters)
            ciphertext =
                BackupRecordCrypto.crypt(
                    javax.crypto.Cipher.ENCRYPT_MODE,
                    key,
                    nonce,
                    aad,
                    terminalPlaintext,
                    terminalPlaintext.size,
                )
            ciphertext.copyInto(rewritten, terminalOffset + recordHeader.size)
        } finally {
            parsed.authenticatedBytes.fill(0)
            parsed.salt.fill(0)
            parsed.noncePrefix.fill(0)
            header.fill(0)
            recordHeader.fill(0)
            terminalPlaintext.fill(0)
            nonce.fill(0)
            aad.fill(0)
            key.fill(0)
            ciphertext.fill(0)
        }
        return rewritten
    }

    private fun putLong(bytes: ByteArray, offset: Int, value: Long) {
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).putLong(offset, value)
    }

    private fun putInt(bytes: ByteArray, offset: Int, value: Int) {
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).putInt(offset, value)
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
