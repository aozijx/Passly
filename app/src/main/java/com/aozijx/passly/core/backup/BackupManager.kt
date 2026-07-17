package com.aozijx.passly.core.backup

import com.aozijx.passly.core.security.KeyDerivation
import java.io.EOFException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份核心工具：只保留底层的加密算法、流读取和错误映射。
 *
 * [deriveKeyArgon2id] 和 [generateSalt] 已迁移至 [KeyDerivation]，
 * 供全应用复用（信封加密、备份加密、恢复码派生）。
 */
object BackupManager {
    val MAGIC_NUMBER = "PASSLYBK".toByteArray(StandardCharsets.UTF_8)
    const val BACKUP_VERSION = 1

    const val SALT_LENGTH = KeyDerivation.SALT_LENGTH
    const val IV_LENGTH = 12

    const val DATA_ENTRY_NAME = "data.json"
    const val IMAGE_ENTRY_PREFIX = "images/"

    fun getCipher(mode: Int, key: SecretKeySpec, iv: ByteArray? = null): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        if (iv != null) {
            cipher.init(mode, key, GCMParameterSpec(128, iv))
        } else {
            cipher.init(mode, key)
        }
        return cipher
    }

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

    fun readFullyOrThrow(input: InputStream, target: ByteArray, fieldName: String) {
        var offset = 0
        while (offset < target.size) {
            val count = input.read(target, offset, target.size - offset)
            if (count == -1) throw EOFException("文件损坏: $fieldName 不完整")
            offset += count
        }
    }

}
