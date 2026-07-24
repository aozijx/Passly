package com.aozijx.passly.data.backup.format.encrypted

import com.aozijx.passly.core.security.KeyDerivation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passly 正式加密备份容器 v1。
 *
 * 布局：
 * magic | formatVersion | headerLength |
 * kdfId | cipherId | argon2Version | iterations | memoryKiB | parallelism |
 * keyLengthBits | saltLength | nonceLength | tagLengthBits | ciphertextLength |
 * salt | nonce | ciphertext
 *
 * 从 magic 到 nonce 的完整头部作为 AES-GCM AAD，因此算法标识、KDF 参数、
 * 长度、salt 和 nonce 均受到认证。密文是完整 ZIP 归档，JSON 和所有资源都
 * 位于加密边界内。
 */
object EncryptedBackupContainerCodec {
    const val FORMAT_VERSION = 1
    const val KDF_ARGON2ID = 1
    const val CIPHER_AES_256_GCM = 1

    private const val TAG_LENGTH_BITS = 128
    private const val NONCE_LENGTH = 12
    private const val MAX_CONTAINER_BYTES = 256 * 1024 * 1024
    private const val PREFIX_LENGTH = 8 + Int.SIZE_BYTES + Int.SIZE_BYTES
    private const val HEADER_INT_COUNT = 11
    private const val FIXED_HEADER_LENGTH = PREFIX_LENGTH + HEADER_INT_COUNT * Int.SIZE_BYTES
    private const val MAX_HEADER_LENGTH = 1024
    private const val MAX_IMPORT_ITERATIONS = 10
    private const val MAX_IMPORT_MEMORY_KIB = 256 * 1024
    private const val MAX_IMPORT_PARALLELISM = 8
    private val MAGIC_NUMBER = "PSLYBKP1".toByteArray(StandardCharsets.UTF_8)
    private val secureRandom = SecureRandom()

    data class ContainerHeader(
        val formatVersion: Int,
        val kdfId: Int,
        val cipherId: Int,
        val kdfParameters: KeyDerivation.Argon2idParameters,
        val salt: ByteArray,
        val nonce: ByteArray,
        val tagLengthBits: Int,
        val ciphertextLength: Int
    )

    private data class ParsedContainer(
        val header: ContainerHeader,
        val authenticatedHeader: ByteArray,
        val ciphertext: ByteArray
    )

    fun encrypt(
        plaintext: ByteArray,
        password: CharArray,
        parameters: KeyDerivation.Argon2idParameters =
            KeyDerivation.DEFAULT_ARGON2ID_PARAMETERS,
        deriveKey: (
            CharArray,
            ByteArray,
            KeyDerivation.Argon2idParameters
        ) -> ByteArray = KeyDerivation::deriveKeyBytesArgon2id
    ): ByteArray {
        require(password.isNotEmpty()) { "备份密码不能为空" }
        require(plaintext.size <= MAX_CONTAINER_BYTES - 256) { "备份内容过大" }
        validateContainerKdf(parameters)

        val salt = KeyDerivation.generateSalt()
        val nonce = ByteArray(NONCE_LENGTH).also(secureRandom::nextBytes)
        val expectedCiphertextLength = plaintext.size + TAG_LENGTH_BITS / Byte.SIZE_BITS
        val header = ContainerHeader(
            formatVersion = FORMAT_VERSION,
            kdfId = KDF_ARGON2ID,
            cipherId = CIPHER_AES_256_GCM,
            kdfParameters = parameters,
            salt = salt,
            nonce = nonce,
            tagLengthBits = TAG_LENGTH_BITS,
            ciphertextLength = expectedCiphertextLength
        )
        val authenticatedHeader = encodeHeader(header)
        val keyBytes = deriveKey(password, salt, parameters)
        return try {
            require(keyBytes.size == parameters.keyLengthBits / Byte.SIZE_BITS) {
                "派生密钥长度无效"
            }
            val key = SecretKeySpec(keyBytes, "AES")
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, nonce))
                cipher.updateAAD(authenticatedHeader)
                val ciphertext = cipher.doFinal(plaintext)
                require(ciphertext.size == expectedCiphertextLength) {
                    "加密输出长度与容器头不一致"
                }
                authenticatedHeader + ciphertext
            } finally {
                runCatching { key.destroy() }
            }
        } finally {
            keyBytes.fill(0)
            salt.fill(0)
            nonce.fill(0)
        }
    }

    fun decrypt(
        container: ByteArray,
        password: CharArray,
        deriveKey: (
            CharArray,
            ByteArray,
            KeyDerivation.Argon2idParameters
        ) -> ByteArray = KeyDerivation::deriveKeyBytesArgon2id
    ): ByteArray {
        require(password.isNotEmpty()) { "备份密码不能为空" }
        val parsed = parse(container)
        val header = parsed.header
        val keyBytes = deriveKey(password, header.salt, header.kdfParameters)
        return try {
            require(keyBytes.size == header.kdfParameters.keyLengthBits / Byte.SIZE_BITS) {
                "派生密钥长度无效"
            }
            val key = SecretKeySpec(keyBytes, "AES")
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(header.tagLengthBits, header.nonce)
                )
                cipher.updateAAD(parsed.authenticatedHeader)
                cipher.doFinal(parsed.ciphertext)
            } finally {
                runCatching { key.destroy() }
            }
        } finally {
            keyBytes.fill(0)
            parsed.ciphertext.fill(0)
        }
    }

    fun hasMagic(container: ByteArray): Boolean =
        container.size >= MAGIC_NUMBER.size &&
                MAGIC_NUMBER.indices.all { container[it] == MAGIC_NUMBER[it] }

    /** 无需密码即可读取的非敏感、自描述容器参数。 */
    fun inspectHeader(container: ByteArray): ContainerHeader = parse(container).header

    private fun encodeHeader(header: ContainerHeader): ByteArray {
        val headerLength = FIXED_HEADER_LENGTH + header.salt.size + header.nonce.size
        return ByteArrayOutputStream(headerLength).use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.write(MAGIC_NUMBER)
                output.writeInt(header.formatVersion)
                output.writeInt(headerLength)
                output.writeInt(header.kdfId)
                output.writeInt(header.cipherId)
                output.writeInt(header.kdfParameters.version)
                output.writeInt(header.kdfParameters.iterations)
                output.writeInt(header.kdfParameters.memoryKiB)
                output.writeInt(header.kdfParameters.parallelism)
                output.writeInt(header.kdfParameters.keyLengthBits)
                output.writeInt(header.salt.size)
                output.writeInt(header.nonce.size)
                output.writeInt(header.tagLengthBits)
                output.writeInt(header.ciphertextLength)
                output.write(header.salt)
                output.write(header.nonce)
            }
            bytes.toByteArray()
        }
    }

    private fun parse(container: ByteArray): ParsedContainer {
        require(
            container.size in (FIXED_HEADER_LENGTH + NONCE_LENGTH + KeyDerivation.SALT_LENGTH + 1)
                    ..MAX_CONTAINER_BYTES
        ) { "备份容器长度无效" }

        val prefixInput = DataInputStream(ByteArrayInputStream(container))
        val magic = ByteArray(MAGIC_NUMBER.size)
        readFullyOrThrow(prefixInput, magic, "magic")
        require(magic.contentEquals(MAGIC_NUMBER)) { "不支持的备份文件格式" }

        val formatVersion = prefixInput.readInt()
        require(formatVersion == FORMAT_VERSION) { "不支持的备份版本: $formatVersion" }
        val headerLength = prefixInput.readInt()
        require(headerLength in FIXED_HEADER_LENGTH..MAX_HEADER_LENGTH) { "备份头长度无效" }
        require(headerLength < container.size) { "备份头或密文不完整" }

        val authenticatedHeader = container.copyOfRange(0, headerLength)
        val headerInput = DataInputStream(
            ByteArrayInputStream(authenticatedHeader, PREFIX_LENGTH, headerLength - PREFIX_LENGTH)
        )
        val kdfId = headerInput.readInt()
        val cipherId = headerInput.readInt()
        require(kdfId == KDF_ARGON2ID) { "不支持的 KDF: $kdfId" }
        require(cipherId == CIPHER_AES_256_GCM) { "不支持的加密算法: $cipherId" }

        val argonVersion = headerInput.readInt()
        val iterations = headerInput.readInt()
        val memoryKiB = headerInput.readInt()
        val parallelism = headerInput.readInt()
        val keyLengthBits = headerInput.readInt()
        val saltLength = headerInput.readInt()
        val nonceLength = headerInput.readInt()
        val tagLengthBits = headerInput.readInt()
        val ciphertextLength = headerInput.readInt()

        require(saltLength in 16..64) { "备份 salt 长度无效" }
        require(nonceLength == NONCE_LENGTH) { "备份 nonce 长度无效" }
        require(tagLengthBits == TAG_LENGTH_BITS) { "备份认证标签长度无效" }
        require(ciphertextLength in 1..MAX_CONTAINER_BYTES) { "备份密文长度无效" }
        require(headerLength == FIXED_HEADER_LENGTH + saltLength + nonceLength) {
            "备份头声明长度不一致"
        }
        require(container.size - headerLength == ciphertextLength) {
            "备份密文声明长度不一致"
        }

        val salt = ByteArray(saltLength)
        val nonce = ByteArray(nonceLength)
        readFullyOrThrow(headerInput, salt, "salt")
        readFullyOrThrow(headerInput, nonce, "nonce")
        require(headerInput.read() == -1) { "备份头包含未知数据" }

        val kdfParameters = KeyDerivation.Argon2idParameters(
            version = argonVersion,
            iterations = iterations,
            memoryKiB = memoryKiB,
            parallelism = parallelism,
            keyLengthBits = keyLengthBits
        )
        validateContainerKdf(kdfParameters)
        return ParsedContainer(
            header = ContainerHeader(
                formatVersion = formatVersion,
                kdfId = kdfId,
                cipherId = cipherId,
                kdfParameters = kdfParameters,
                salt = salt,
                nonce = nonce,
                tagLengthBits = tagLengthBits,
                ciphertextLength = ciphertextLength
            ),
            authenticatedHeader = authenticatedHeader,
            ciphertext = container.copyOfRange(headerLength, container.size)
        )
    }

    private fun readFullyOrThrow(
        input: InputStream,
        target: ByteArray,
        fieldName: String
    ) {
        var offset = 0
        while (offset < target.size) {
            val count = input.read(target, offset, target.size - offset)
            if (count == -1) throw EOFException("文件损坏: $fieldName 不完整")
            offset += count
        }
    }

    private fun validateContainerKdf(parameters: KeyDerivation.Argon2idParameters) {
        require(parameters.iterations in 1..MAX_IMPORT_ITERATIONS) {
            "备份 KDF 迭代次数超限"
        }
        require(parameters.memoryKiB in 8 * 1024..MAX_IMPORT_MEMORY_KIB) {
            "备份 KDF 内存参数超限"
        }
        require(parameters.parallelism in 1..MAX_IMPORT_PARALLELISM) {
            "备份 KDF 并行度超限"
        }
    }
}
