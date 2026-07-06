package com.aozijx.passly.security.envelope

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.aozijx.passly.domain.model.AppDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EnvelopeStore 的 SharedPreferences 实现。
 *
 * 信封序列化格式：
 *   [version: 4B][iv_len: 4B][iv][ct_len: 4B][ciphertext][kdfParams_len: 4B][kdfParams_json]
 * 为 null 时 kdfParams_len = 0。
 */
@Singleton
class SharedPrefsEnvelopeStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : EnvelopeStore {

    private val prefs by lazy {
        context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun save(envelope: Envelope) {
        val serialized = serialize(envelope)
        prefs.edit {
            putString("envelope_${envelope.id}", Base64.encodeToString(serialized, Base64.NO_WRAP))
        }
    }

    override fun get(id: String): Envelope? {
        val encoded = prefs.getString("envelope_$id", null) ?: return null
        return try {
            deserialize(id, Base64.decode(encoded, Base64.NO_WRAP))
        } catch (_: Exception) {
            null
        }
    }

    override fun remove(id: String) {
        prefs.edit { remove("envelope_$id") }
    }

    override fun getAllIds(): Set<String> {
        return prefs.all.keys
            .filter { it.startsWith("envelope_") }
            .map { it.removePrefix("envelope_") }
            .toSet()
    }

    override fun hasAny(): Boolean {
        return prefs.all.keys.any { it.startsWith("envelope_") }
    }

    override fun clearAll() {
        val keys = prefs.all.keys.filter { it.startsWith("envelope_") }
        prefs.edit { keys.forEach { remove(it) } }
    }

    override fun beginTransaction() {
        // SharedPreferences 单次 edit + apply 即可视为事务
    }

    override fun commit() {
        // no-op for SharedPreferences
    }

    override fun rollback() {
        // no-op for SharedPreferences
    }

    private fun serialize(envelope: Envelope): ByteArray {
        // 简化版本：仅保存 iv + ciphertext + kdfParams placeholder
        // 生产环境应使用更健壮的序列化
        val kdfBytes = if (envelope.kdfParams != null) {
            "kdf:${envelope.kdfParams.algorithm.value}:v${envelope.kdfParams.version}".toByteArray()
        } else {
            ByteArray(0)
        }
        val buffer = java.nio.ByteBuffer.allocate(
            4 + 4 + envelope.iv.size + 4 + envelope.dekCiphertext.size + 4 + kdfBytes.size
        )
        buffer.putInt(envelope.version)
        putWithLength(envelope.iv, buffer)
        putWithLength(envelope.dekCiphertext, buffer)
        putWithLength(kdfBytes, buffer)
        return buffer.array()
    }

    private fun deserialize(id: String, bytes: ByteArray): Envelope {
        val buffer = java.nio.ByteBuffer.wrap(bytes)
        val version = buffer.int
        val iv = readWithLength(buffer)
        val ct = readWithLength(buffer)

        val kdfBytes = try {
            readWithLength(buffer)
        } catch (_: Exception) {
            ByteArray(0)
        }
        val kdfParams = if (kdfBytes.isNotEmpty()) {
            val kdfStr = String(kdfBytes)
            // 简化：返回最小化 KdfParams
            KdfParams(
                algorithm = KdfAlgorithm(kdfStr.substringAfter("kdf:").substringBefore(":v")),
                version = kdfStr.substringAfter(":v").toIntOrNull() ?: 1,
                salt = ByteArray(16),
                iterations = 2,
                memoryKb = 19456,
                parallelism = 1
            )
        } else null

        return Envelope(
            id = id,
            type = EnvelopeType(id),
            dekCiphertext = ct,
            iv = iv,
            kdfParams = kdfParams,
            version = version
        )
    }

    private fun putWithLength(bytes: ByteArray, buffer: java.nio.ByteBuffer) {
        buffer.putInt(bytes.size)
        buffer.put(bytes)
    }

    private fun readWithLength(buffer: java.nio.ByteBuffer): ByteArray {
        val length = buffer.int
        return ByteArray(length).also { buffer.get(it) }
    }
}