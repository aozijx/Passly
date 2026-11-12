package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.observability.ReasonCode
import java.io.InputStream
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Shared canonical record encoding and per-record JCA operation. */
internal object BackupRecordCrypto {
    fun header(
        type: Int,
        sequence: Long,
        plaintextLength: Int,
    ): ByteArray =
        ByteBuffer
            .allocate(BackupContainerV1Spec.RECORD_HEADER_LENGTH)
            .put(type.toByte())
            .putLong(sequence)
            .putInt(plaintextLength)
            .array()

    fun nonce(
        prefix: ByteArray,
        sequence: Long,
    ): ByteArray =
        ByteBuffer
            .allocate(12)
            .put(prefix)
            .putLong(sequence)
            .array()

    fun aad(
        encodedHeader: ByteArray,
        encodedRecordHeader: ByteArray,
    ): ByteArray = encodedHeader + encodedRecordHeader

    fun crypt(
        mode: Int,
        keyBytes: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        input: ByteArray,
        length: Int,
    ): ByteArray {
        val key = SecretKeySpec(keyBytes, "AES")
        try {
            val cipher = Cipher.getInstance(BackupContainerV1Spec.CIPHER_TRANSFORMATION)
            cipher.init(
                mode,
                key,
                GCMParameterSpec(BackupContainerV1Spec.TAG_LENGTH * Byte.SIZE_BITS, nonce),
            )
            cipher.updateAAD(aad)
            return cipher.doFinal(input, 0, length)
        } finally {
            runCatching { key.destroy() }
        }
    }

    fun readFully(
        input: InputStream,
        target: ByteArray,
        offset: Int = 0,
        length: Int = target.size,
        reason: ReasonCode,
    ) {
        var position = offset
        val end = offset + length
        while (position < end) {
            val count = input.read(target, position, end - position)
            if (count < 0) throw BackupTruncatedException(reason)
            if (count == 0) continue
            position += count
        }
    }
}
