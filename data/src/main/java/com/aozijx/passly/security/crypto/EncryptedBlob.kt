package com.aozijx.passly.security.crypto

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * 密文装箱格式 —— 自描述的加密数据二进制容器。
 *
 * ```
 * [version: BE Int32]     -- 加密方案版本，当前: 1
 * [keyIdLen: BE Int32]    -- keyId 字节数，0 表示无 keyId
 * [keyId: UTF-8 bytes]    -- 可选密钥标识（keyIdLen = 0 时省略）
 * [nonceLen: BE Int32]    -- nonce 字节数
 * [nonce: bytes]          -- AES-GCM IV
 * [ciphertextLen: BE Int32] -- 密文字节数
 * [ciphertext: bytes]     -- 加密后的 VaultPayload (不含 GCM tag)
 * [tagLen: BE Int32]      -- GCM tag 字节数 (16 for 128-bit)
 * [tag: bytes]            -- GCM 认证标签
 * ```
 *
 * ## 设计目标
 * - **自描述**: 无需依赖外部常量（如 IV_LENGTH）即可解析
 * - **前向兼容**: 新增字段只需递增 [version]，旧版本按老格式解析
 * - **密钥轮换**: [keyId] 支持多 DEK 场景，解码时定位正确密钥
 * - **格式检测**: [version = 1] (4 字节 BE) 不会与旧格式的随机 IV 混淆
 *
 * ## 向后兼容
 *
 * 旧格式为 `IV(12B) + ciphertext_with_tag(N+16B)`，无头部元数据。
 * [deserializeIfNew] 检测首 4 字节：
 * - 首 4 字节 BE Int32 == 1 → 新格式 → [deserialize]
 * - 否则 → 旧格式 → 回退到旧解析路径
 */
data class EncryptedBlob(
    val version: Int = VERSION,
    val keyId: String? = null,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
    val tag: ByteArray
) {
    companion object {
        const val VERSION = 1
        private const val TAG_FIXED = 16

        // ────────────────────────────────────────────────────
        //  反序列化
        // ────────────────────────────────────────────────────

        /** 尝试反序列化，旧格式返回 null */
        fun deserializeIfNew(data: ByteArray): EncryptedBlob? {
            if (data.size < 4) return null
            val dis = DataInputStream(data.inputStream())
            val version = dis.readInt()
            if (version != VERSION) return null
            return deserializeRest(dis)
        }

        /** 新格式反序列化 */
        fun deserialize(data: ByteArray): EncryptedBlob {
            val dis = DataInputStream(data.inputStream())
            val version = dis.readInt()
            check(version == VERSION) {
                "Unsupported EncryptedBlob version: $version (expected $VERSION)"
            }
            return deserializeRest(dis)
        }

        private fun deserializeRest(dis: DataInputStream): EncryptedBlob {
            val keyIdLen = dis.readInt()
            val keyId = if (keyIdLen > 0) {
                val buf = ByteArray(keyIdLen)
                dis.readFully(buf)
                String(buf, Charsets.UTF_8)
            } else null

            val nonceLen = dis.readInt()
            val nonce = ByteArray(nonceLen)
            dis.readFully(nonce)

            val ciphertextLen = dis.readInt()
            val ciphertext = ByteArray(ciphertextLen)
            dis.readFully(ciphertext)

            val tagLen = dis.readInt()
            val tag = ByteArray(tagLen)
            dis.readFully(tag)

            return EncryptedBlob(
                version = VERSION,
                keyId = keyId,
                nonce = nonce,
                ciphertext = ciphertext,
                tag = tag
            )
        }

        /** 从 GCM doFinal 输出（ciphertext + tag 连接）创建新格式 Blob */
        fun fromGcm(
            nonce: ByteArray,
            gcmOutput: ByteArray,
            keyId: String? = null
        ): EncryptedBlob {
            val tagOffset = gcmOutput.size - TAG_FIXED
            return EncryptedBlob(
                version = VERSION,
                keyId = keyId,
                nonce = nonce.copyOf(),
                ciphertext = gcmOutput.copyOfRange(0, tagOffset),
                tag = gcmOutput.copyOfRange(tagOffset, gcmOutput.size)
            )
        }
    }

    // ────────────────────────────────────────────────────────
    //  序列化
    // ────────────────────────────────────────────────────────

    fun serialize(): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        dos.writeInt(version)

        val keyIdBytes = keyId?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        dos.writeInt(keyIdBytes.size)
        if (keyIdBytes.isNotEmpty()) dos.write(keyIdBytes)

        dos.writeInt(nonce.size)
        dos.write(nonce)

        dos.writeInt(ciphertext.size)
        dos.write(ciphertext)

        dos.writeInt(tag.size)
        dos.write(tag)

        return bos.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedBlob) return false
        return version == other.version &&
            keyId == other.keyId &&
            nonce.contentEquals(other.nonce) &&
            ciphertext.contentEquals(other.ciphertext) &&
            tag.contentEquals(other.tag)
    }

    override fun hashCode(): Int = version * 31 + (keyId?.hashCode() ?: 0)
}
