package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.feature.backup.internal.archive.io.BackupFileStore
import com.aozijx.passly.feature.backup.internal.archive.io.LimitedOutputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher

/** Buffers one plaintext chunk and writes each authenticated record immediately. */
internal class ChunkedBackupOutputStream(
    output: OutputStream,
    password: CharArray,
    parameters: KeyDerivation.Argon2idParameters = KeyDerivation.BASELINE_ARGON2ID_PARAMETERS,
    deriveKey: (CharArray, ByteArray, KeyDerivation.Argon2idParameters) -> ByteArray = KeyDerivation::deriveKeyBytesArgon2id,
) : OutputStream() {
    private val salt = KeyDerivation.generateSalt()
    private val noncePrefix = ByteArray(4).also(random::nextBytes)
    private val outputLimit = LimitedOutputStream(output, BackupFileStore.MAX_BYTES)
    private var encodedHeader = ByteArray(0)
    private var keyBytes = ByteArray(0)
    private val chunk = ByteArray(BackupContainerV1Spec.CHUNK_SIZE)
    private var chunkSize = 0
    private var sequence = 0L
    private var totalBytes = 0L
    private var failed = false
    private var closed = false

    init {
        try {
            require(password.isNotEmpty()) { "备份密码不能为空" }
            encodedHeader = BackupContainerHeader.encode(salt, noncePrefix, parameters)
            keyBytes = deriveKey(password, salt, parameters)
            require(keyBytes.size == parameters.keyLengthBits / Byte.SIZE_BITS) { "派生密钥长度无效" }
            outputLimit.write(encodedHeader)
        } catch (error: Throwable) {
            failed = true
            clear()
            throw error
        }
    }

    override fun write(value: Int) {
        ensureWritable()
        chunk[chunkSize++] = value.toByte()
        if (chunkSize == chunk.size) {
            try {
                emitDataRecord()
            } catch (error: Throwable) {
                failed = true
                throw error
            }
        }
    }

    override fun write(
        source: ByteArray,
        offset: Int,
        length: Int,
    ) {
        ensureWritable()
        require(offset >= 0 && length >= 0 && offset <= source.size - length)
        var sourceOffset = offset
        var remaining = length
        try {
            while (remaining > 0) {
                val count = minOf(remaining, chunk.size - chunkSize)
                source.copyInto(chunk, chunkSize, sourceOffset, sourceOffset + count)
                chunkSize += count
                sourceOffset += count
                remaining -= count
                if (chunkSize == chunk.size) emitDataRecord()
            }
        } catch (error: Throwable) {
            failed = true
            throw error
        }
    }

    override fun flush() {
        ensureWritable()
        outputLimit.flush()
    }

    override fun close() {
        if (closed) return
        try {
            if (!failed) {
                if (chunkSize > 0) emitDataRecord()
                emitTerminalRecord()
                outputLimit.flush()
            }
        } finally {
            closed = true
            clear()
        }
    }

    fun abort() {
        if (closed) return
        failed = true
        closed = true
        clear()
    }

    private fun emitDataRecord() {
        val recordHeader = BackupRecordCrypto.header(BackupContainerV1Spec.DATA_RECORD_TYPE, sequence, chunkSize)
        val nonce = BackupRecordCrypto.nonce(noncePrefix, sequence)
        val aad = BackupRecordCrypto.aad(encodedHeader, recordHeader)
        val ciphertext = BackupRecordCrypto.crypt(Cipher.ENCRYPT_MODE, keyBytes, nonce, aad, chunk, chunkSize)
        try {
            outputLimit.write(recordHeader)
            outputLimit.write(ciphertext)
            totalBytes = Math.addExact(totalBytes, chunkSize.toLong())
            sequence = Math.addExact(sequence, 1L)
        } finally {
            recordHeader.fill(0)
            nonce.fill(0)
            aad.fill(0)
            ciphertext.fill(0)
            chunk.fill(0)
            chunkSize = 0
        }
    }

    private fun emitTerminalRecord() {
        val totals =
            java.nio.ByteBuffer
                .allocate(BackupContainerV1Spec.TERMINAL_PLAINTEXT_LENGTH)
                .putLong(sequence)
                .putLong(totalBytes)
                .array()
        val recordHeader = BackupRecordCrypto.header(BackupContainerV1Spec.TERMINAL_RECORD_TYPE, sequence, totals.size)
        val nonce = BackupRecordCrypto.nonce(noncePrefix, sequence)
        val aad = BackupRecordCrypto.aad(encodedHeader, recordHeader)
        val ciphertext = BackupRecordCrypto.crypt(Cipher.ENCRYPT_MODE, keyBytes, nonce, aad, totals, totals.size)
        try {
            outputLimit.write(recordHeader)
            outputLimit.write(ciphertext)
        } finally {
            totals.fill(0)
            recordHeader.fill(0)
            nonce.fill(0)
            aad.fill(0)
            ciphertext.fill(0)
        }
    }

    private fun ensureWritable() {
        check(!closed) { "加密备份流已关闭" }
        check(!failed) { "加密备份流已失败" }
    }

    private fun clear() {
        chunk.fill(0)
        encodedHeader.fill(0)
        keyBytes.fill(0)
        salt.fill(0)
        noncePrefix.fill(0)
    }

    private companion object {
        val random = SecureRandom()
    }
}
