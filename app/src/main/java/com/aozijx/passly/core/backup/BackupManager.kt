package com.aozijx.passly.core.backup

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import java.io.EOFException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份核心工具：只保留底层的加密算法、密钥衍生和流读取的基础封装。
 * 不再持有任何业务逻辑或对数据库实体的引用。
 */
object BackupManager {
    val MAGIC_NUMBER = "PASSLY".toByteArray(StandardCharsets.UTF_8)
    const val BACKUP_VERSION = 1

    const val SALT_LENGTH = 16
    const val IV_LENGTH = 12
    private const val KEY_LENGTH_BITS = 256
    private const val KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8

    private const val ARGON2_ITERATIONS = 3
    private const val ARGON2_MEMORY_KB = 65536
    private const val ARGON2_PARALLELISM = 4

    const val DATA_ENTRY_NAME = "data.json"
    const val IMAGE_ENTRY_PREFIX = "images/"

    private val argon2Kt = Argon2Kt()

    /**
     * 根据密码和盐，使用 Argon2id 派生 256 位密钥。
     */
    fun deriveKeyArgon2id(password: CharArray, salt: ByteArray): SecretKeySpec {
        val passBytes = String(password).toByteArray(StandardCharsets.UTF_8)
        try {
            val rawHash = argon2Kt.hash(
                mode = Argon2Mode.ARGON2_ID,
                password = passBytes,
                salt = salt,
                tCostInIterations = ARGON2_ITERATIONS,
                mCostInKibibyte = ARGON2_MEMORY_KB,
                parallelism = ARGON2_PARALLELISM,
                hashLengthInBytes = KEY_LENGTH_BYTES,
                version = Argon2Version.V13
            )
            val rawKey = rawHash.rawHashAsByteArray()
            return try {
                SecretKeySpec(rawKey, "AES")
            } finally {
                rawKey.fill(0)
            }
        } finally {
            passBytes.fill(0)
        }
    }

    /**
     * 生成随机盐。
     */
    fun generateSalt(): ByteArray = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }

    /**
     * 初始化 AES-GCM 加密/解密器。
     */
    fun getCipher(mode: Int, key: SecretKeySpec, iv: ByteArray? = null): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        if (iv != null) {
            cipher.init(mode, key, GCMParameterSpec(128, iv))
        } else {
            cipher.init(mode, key)
        }
        return cipher
    }

    /**
     * 将导入失败的底层异常映射为用户友好的提示。
     */
    fun mapImportFailure(error: Exception): Exception {
        if (error is IllegalArgumentException) return error
        if (error is IllegalStateException && error.message?.contains("不支持的备份版本") == true) {
            return error
        }

        var current: Throwable? = error
        while (current != null) {
            if (current is AEADBadTagException || current.message?.contains("BAD_DECRYPT") == true || current.message?.contains(
                    "tag mismatch"
                ) == true
            ) {
                return Exception("备份密码错误，请核对后重试", error)
            }
            current = current.cause
        }
        if (error is EOFException) return Exception("备份文件损坏或格式不正确", error)
        return error
    }

    /**
     * 辅助方法：从流中读取指定长度的字节数组，若不足则抛出异常。
     */
    fun readFullyOrThrow(input: InputStream, target: ByteArray, fieldName: String) {
        var offset = 0
        while (offset < target.size) {
            val count = input.read(target, offset, target.size - offset)
            if (count == -1) throw EOFException("文件损坏: $fieldName 不完整")
            offset += count
        }
    }

    /**
     * 辅助方法：从流中读取单个字节作为版本号。
     */
    fun readSingleByteOrThrow(input: InputStream): Int {
        val value = input.read()
        if (value == -1) throw EOFException("文件损坏: 无法读取版本号")
        return value and 0xFF
    }
}