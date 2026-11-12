package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.feature.backup.internal.archive.io.BackupFileStore
import java.io.InputStream
import java.nio.ByteBuffer
import javax.crypto.Cipher

/** Releases a record only after its independent GCM tag has been verified. */
internal class ChunkedBackupInputStream(
    private val input: InputStream,
    password: CharArray,
    deriveKey: (CharArray, ByteArray, KeyDerivation.Argon2idParameters) -> ByteArray = KeyDerivation::deriveKeyBytesArgon2id,
) : InputStream() {
    private val containerHeader: BackupContainerHeader.Parsed
    private var keyBytes = ByteArray(0)
    private var sequence = 0L
    private var totalBytes = 0L
    private var plaintext = ByteArray(0)
    private var plaintextOffset = 0
    private var terminalVerified = false
    private var failed = false
    private var closed = false

    init {
        require(password.isNotEmpty()) { "备份密码不能为空" }
        containerHeader = BackupContainerHeader.read(input)
        try {
            keyBytes = deriveKey(password, containerHeader.salt, containerHeader.kdfParameters)
            require(keyBytes.size == containerHeader.kdfParameters.keyLengthBits / Byte.SIZE_BITS) {
                "派生密钥长度无效"
            }
        } catch (error: Throwable) {
            failed = true
            clear()
            throw error
        }
    }

    override fun read(): Int {
        val one = ByteArray(1)
        return try {
            val count = read(one, 0, one.size)
            if (count < 0) -1 else one[0].toInt() and 0xff
        } finally {
            one.fill(0)
        }
    }

    override fun read(
        destination: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        ensureReadable()
        require(offset >= 0 && length >= 0 && offset <= destination.size - length)
        if (length == 0) return 0
        return try {
            while (plaintextOffset >= plaintext.size && !terminalVerified) readNextRecord()
            if (plaintextOffset >= plaintext.size) return -1
            val count = minOf(length, plaintext.size - plaintextOffset)
            plaintext.copyInto(destination, offset, plaintextOffset, plaintextOffset + count)
            plaintextOffset += count
            if (plaintextOffset == plaintext.size) clearPlaintext()
            count
        } catch (error: Throwable) {
            failed = true
            clearPlaintext()
            throw error
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        clear()
    }

    private fun readNextRecord() {
        val firstType = input.read()
        if (firstType < 0) throw BackupTruncatedException(Reasons.BackupTerminalMissing)
        val recordHeader = ByteArray(BackupContainerV1Spec.RECORD_HEADER_LENGTH)
        recordHeader[0] = firstType.toByte()
        BackupRecordCrypto.readFully(input, recordHeader, 1, recordHeader.size - 1, Reasons.BackupRecordHeaderTruncated)
        val fields = ByteBuffer.wrap(recordHeader)
        val type = fields.get().toInt() and 0xff
        val recordSequence = fields.long
        val plaintextLength = fields.int
        require(recordSequence == sequence) { "加密备份记录序号无效" }
        require(type == BackupContainerV1Spec.DATA_RECORD_TYPE || type == BackupContainerV1Spec.TERMINAL_RECORD_TYPE) {
            "加密备份记录类型无效"
        }
        if (type == BackupContainerV1Spec.DATA_RECORD_TYPE) {
            require(plaintextLength in 1..BackupContainerV1Spec.CHUNK_SIZE) { "加密备份记录长度无效" }
        } else {
            require(plaintextLength == BackupContainerV1Spec.TERMINAL_PLAINTEXT_LENGTH) { "terminal record 长度无效" }
        }

        val ciphertext = ByteArray(plaintextLength + BackupContainerV1Spec.TAG_LENGTH)
        val nonce = BackupRecordCrypto.nonce(containerHeader.noncePrefix, sequence)
        val aad = BackupRecordCrypto.aad(containerHeader.authenticatedBytes, recordHeader)
        try {
            BackupRecordCrypto.readFully(input, ciphertext, reason = Reasons.BackupRecordDataTruncated)
            val decoded = BackupRecordCrypto.crypt(Cipher.DECRYPT_MODE, keyBytes, nonce, aad, ciphertext, ciphertext.size)
            if (type == BackupContainerV1Spec.DATA_RECORD_TYPE) {
                plaintext = decoded
                totalBytes = Math.addExact(totalBytes, decoded.size.toLong())
                require(totalBytes <= BackupFileStore.MAX_BYTES) { "加密备份展开内容过大" }
                sequence = Math.addExact(sequence, 1L)
            } else {
                try {
                    val totals = ByteBuffer.wrap(decoded)
                    require(totals.long == sequence && totals.long == totalBytes) { "terminal record 统计值无效" }
                    require(input.read() == -1) { "terminal record 后存在多余数据" }
                    terminalVerified = true
                } finally {
                    decoded.fill(0)
                }
            }
        } finally {
            recordHeader.fill(0)
            ciphertext.fill(0)
            nonce.fill(0)
            aad.fill(0)
        }
    }

    private fun ensureReadable() {
        check(!closed) { "解密备份流已关闭" }
        check(!failed) { "解密备份流已失败" }
    }

    private fun clearPlaintext() {
        plaintext.fill(0)
        plaintext = ByteArray(0)
        plaintextOffset = 0
    }

    private fun clear() {
        clearPlaintext()
        keyBytes.fill(0)
        containerHeader.authenticatedBytes.fill(0)
        containerHeader.salt.fill(0)
        containerHeader.noncePrefix.fill(0)
    }
}
