package com.aozijx.passly.data.model.serializer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * 历史快照的序列化载荷。
 *
 * 将 Metadata 和 Credential 的 JSON 字节序列化为长度前缀的二进制格式，
 * 然后整体加密为单一的 snapshotBlob，而不是将两个独立的加密 Blob 简单拼接。
 *
 * 格式：
 *   [4 bytes: metaJson 长度 (Int, BigEndian)][metaJson bytes]
 *   [4 bytes: credJson 长度 (Int, BigEndian)][credJson bytes]
 */
data class SnapshotPayload(
    val metadataJson: ByteArray,
    val credentialJson: ByteArray
) {
    /** 序列化为长度前缀的二进制格式。 */
    fun toBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { dos ->
            dos.writeInt(metadataJson.size)
            dos.write(metadataJson)
            dos.writeInt(credentialJson.size)
            dos.write(credentialJson)
        }
        return out.toByteArray()
    }

    companion object {
        /** 从长度前缀的二进制格式反序列化。 */
        fun fromBytes(data: ByteArray): SnapshotPayload {
            val input = DataInputStream(ByteArrayInputStream(data))
            val metaLen = input.readInt()
            val metaBytes = ByteArray(metaLen).also { input.readFully(it) }
            val credLen = input.readInt()
            val credBytes = ByteArray(credLen).also { input.readFully(it) }
            return SnapshotPayload(metaBytes, credBytes)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnapshotPayload) return false
        return metadataJson.contentEquals(other.metadataJson) &&
                credentialJson.contentEquals(other.credentialJson)
    }

    override fun hashCode(): Int {
        var result = metadataJson.contentHashCode()
        result = 31 * result + credentialJson.contentHashCode()
        return result
    }

    override fun toString(): String =
        "SnapshotPayload(meta=${metadataJson.size}B, cred=${credentialJson.size}B)"
}
