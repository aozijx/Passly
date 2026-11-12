package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.core.observability.Reasons
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/** Encodes and validates the fixed v1 container header. */
internal object BackupContainerHeader {
    data class Parsed(
        val containerVersion: Int,
        val kdfParameters: KeyDerivation.Argon2idParameters,
        val salt: ByteArray,
        val noncePrefix: ByteArray,
        val authenticatedBytes: ByteArray,
    )

    fun hasMagic(
        prefix: ByteArray,
        length: Int = prefix.size,
    ): Boolean =
        length >= BackupContainerV1Spec.MAGIC.length &&
            BackupContainerV1Spec.MAGIC.indices.all { index ->
                prefix[index] == BackupContainerV1Spec.MAGIC[index].code.toByte()
            }

    fun encode(
        salt: ByteArray,
        noncePrefix: ByteArray,
        parameters: KeyDerivation.Argon2idParameters,
    ): ByteArray {
        require(salt.size == BackupContainerV1Spec.SALT_LENGTH) { "备份 salt 长度无效" }
        require(noncePrefix.size == BackupContainerV1Spec.NONCE_PREFIX_LENGTH) { "备份 nonce prefix 长度无效" }
        validate(parameters)
        return ByteArrayOutputStream(BackupContainerV1Spec.HEADER_LENGTH).use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.write(BackupContainerV1Spec.MAGIC.toByteArray(StandardCharsets.US_ASCII))
                output.writeInt(BackupContainerV1Spec.CONTAINER_VERSION)
                output.writeInt(BackupContainerV1Spec.HEADER_LENGTH)
                output.writeInt(BackupContainerV1Spec.KDF_ARGON2ID_ID)
                output.writeInt(BackupContainerV1Spec.CIPHER_AES_256_GCM_ID)
                output.writeInt(parameters.version)
                output.writeInt(parameters.iterations)
                output.writeInt(parameters.memoryKiB)
                output.writeInt(parameters.parallelism)
                output.writeInt(parameters.keyLengthBits)
                output.writeInt(BackupContainerV1Spec.SALT_LENGTH)
                output.writeInt(BackupContainerV1Spec.NONCE_PREFIX_LENGTH)
                output.writeInt(BackupContainerV1Spec.CHUNK_SIZE)
                output.writeInt(BackupContainerV1Spec.TAG_LENGTH)
                output.write(salt)
                output.write(noncePrefix)
            }
            bytes.toByteArray()
        }
    }

    fun read(input: InputStream): Parsed {
        val fixed = ByteArray(BackupContainerV1Spec.FIXED_HEADER_LENGTH)
        readFully(input, fixed)
        try {
            val data = DataInputStream(ByteArrayInputStream(fixed))
            val magic = ByteArray(BackupContainerV1Spec.MAGIC.length)
            readFully(data, magic)
            require(magic.contentEquals(BackupContainerV1Spec.MAGIC.toByteArray(StandardCharsets.US_ASCII))) {
                "不支持的备份文件格式"
            }
            val version = data.readInt()
            require(version == BackupContainerV1Spec.CONTAINER_VERSION) { "不支持的备份版本: $version" }
            require(data.readInt() == BackupContainerV1Spec.HEADER_LENGTH) { "备份头长度无效" }
            require(data.readInt() == BackupContainerV1Spec.KDF_ARGON2ID_ID) { "不支持的 KDF" }
            require(data.readInt() == BackupContainerV1Spec.CIPHER_AES_256_GCM_ID) { "不支持的加密算法" }
            val parameters =
                KeyDerivation.Argon2idParameters(
                    version = data.readInt(),
                    iterations = data.readInt(),
                    memoryKiB = data.readInt(),
                    parallelism = data.readInt(),
                    keyLengthBits = data.readInt(),
                )
            require(data.readInt() == BackupContainerV1Spec.SALT_LENGTH) { "备份 salt 长度无效" }
            require(data.readInt() == BackupContainerV1Spec.NONCE_PREFIX_LENGTH) { "备份 nonce prefix 长度无效" }
            require(data.readInt() == BackupContainerV1Spec.CHUNK_SIZE) { "备份分块长度无效" }
            require(data.readInt() == BackupContainerV1Spec.TAG_LENGTH) { "备份认证标签长度无效" }
            require(data.read() == -1) { "备份头字段长度无效" }
            validate(parameters)

            val salt = ByteArray(BackupContainerV1Spec.SALT_LENGTH)
            val noncePrefix = ByteArray(BackupContainerV1Spec.NONCE_PREFIX_LENGTH)
            try {
                readFully(input, salt)
                readFully(input, noncePrefix)
                return Parsed(version, parameters, salt, noncePrefix, fixed + salt + noncePrefix)
            } catch (error: Throwable) {
                salt.fill(0)
                noncePrefix.fill(0)
                throw error
            }
        } finally {
            fixed.fill(0)
        }
    }

    private fun validate(parameters: KeyDerivation.Argon2idParameters) {
        require(parameters.iterations in BackupContainerV1Spec.MIN_KDF_ITERATIONS..BackupContainerV1Spec.MAX_KDF_ITERATIONS) {
            "备份 KDF 迭代次数超限"
        }
        require(parameters.memoryKiB in BackupContainerV1Spec.MIN_KDF_MEMORY_KIB..BackupContainerV1Spec.MAX_KDF_MEMORY_KIB) {
            "备份 KDF 内存参数超限"
        }
        require(parameters.parallelism in BackupContainerV1Spec.MIN_KDF_PARALLELISM..BackupContainerV1Spec.MAX_KDF_PARALLELISM) {
            "备份 KDF 并行度超限"
        }
    }

    private fun readFully(
        input: InputStream,
        target: ByteArray,
    ) {
        var offset = 0
        while (offset < target.size) {
            val count = input.read(target, offset, target.size - offset)
            if (count < 0) throw BackupTruncatedException(Reasons.BackupContainerHeaderTruncated)
            if (count == 0) continue
            offset += count
        }
    }
}
