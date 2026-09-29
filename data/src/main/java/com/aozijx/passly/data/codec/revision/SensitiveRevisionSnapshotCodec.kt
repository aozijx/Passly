package com.aozijx.passly.data.codec.revision

import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class SensitiveFieldCipherSnapshot(
    val key: SensitiveFieldKey,
    val valueCipher: ByteArray,
    val keyVersion: Int,
)

@Singleton
class SensitiveRevisionSnapshotCodec @Inject constructor() {
    fun encode(fields: List<EntrySecretFieldEntity>): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeInt(FORMAT_VERSION)
            data.writeInt(fields.size)
            fields.sortedBy { it.fieldKey }.forEach { field ->
                data.writeUTF(field.fieldKey)
                data.writeInt(field.keyVersion)
                data.writeInt(field.valueCipher.size)
                data.write(field.valueCipher)
            }
        }
        return output.toByteArray()
    }

    fun decode(blob: ByteArray): List<SensitiveFieldCipherSnapshot> {
        val input = DataInputStream(ByteArrayInputStream(blob))
        require(input.readInt() == FORMAT_VERSION) { "Unsupported sensitive revision format" }
        val count = input.readInt()
        require(count in 0..MAX_FIELDS) { "Invalid sensitive revision field count" }
        val fields = buildList(count) {
            repeat(count) {
                val key = SensitiveFieldKey.valueOf(input.readUTF())
                val keyVersion = input.readInt()
                val cipherSize = input.readInt()
                require(cipherSize in 1..MAX_CIPHER_BYTES && cipherSize <= input.available()) {
                    "Invalid sensitive revision cipher length"
                }
                add(
                    SensitiveFieldCipherSnapshot(
                        key = key,
                        valueCipher = ByteArray(cipherSize).also(input::readFully),
                        keyVersion = keyVersion,
                    )
                )
            }
        }
        require(input.available() == 0) { "Trailing sensitive revision data" }
        return fields
    }

    /** Returns field presence without exposing historical ciphertext bytes to ordinary queries. */
    fun decodeKeys(blob: ByteArray): Set<SensitiveFieldKey> =
        decode(blob).mapTo(linkedSetOf()) { it.key }

    /** Produces irreversible equality tokens without returning ciphertext to presentation layers. */
    fun decodeFingerprints(blob: ByteArray): Map<SensitiveFieldKey, String> =
        decode(blob).associate { field ->
            val digest = MessageDigest.getInstance("SHA-256").apply {
                update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(field.keyVersion).array())
                update(field.valueCipher)
            }.digest()
            field.key to digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
        }

    private companion object {
        const val FORMAT_VERSION = 1
        const val MAX_FIELDS = 64
        const val MAX_CIPHER_BYTES = 4 * 1024 * 1024
    }
}
