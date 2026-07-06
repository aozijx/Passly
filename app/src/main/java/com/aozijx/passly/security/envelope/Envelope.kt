package com.aozijx.passly.security.envelope

/**
 * 多信封加密架构中的信封数据模型。
 *
 * 每个信封存储的是**同一个 DEK 的不同加密形态**。
 * 所有认证方式（生物识别、应用密码、恢复码等）最终目标都是解密各自的信封以获取 DEK。
 *
 * 信封持久化格式：
 *   [IV: 12 bytes][DEK_ciphertext: 32+16 bytes]
 *   可选前缀：[Salt: 16 bytes][KDF Params: 12 bytes]（仅 KDF 类信封需要）
 */
data class Envelope(
    /** 唯一标识，如 "biometric", "app_password", "recovery" */
    val id: String,
    /** 信封类型 */
    val type: EnvelopeType,
    /** AES-256-GCM 加密后的 DEK（含 GCM 认证标签，共 48 bytes） */
    val dekCiphertext: ByteArray,
    /** 加密使用的 IV（12 bytes） */
    val iv: ByteArray,
    /** KDF 参数，仅 KDF 类信封需要（AppPassword, Recovery）；非 KDF 类为 null */
    val kdfParams: KdfParams? = null,
    /** 创建时间戳（epoch millis） */
    val createdAt: Long = System.currentTimeMillis(),
    /** 信封格式版本，用于向后兼容 */
    val version: Int = VERSION_CURRENT
) {
    companion object {
        const val VERSION_CURRENT = 1

        fun destroy(envelope: Envelope) {
            envelope.dekCiphertext.fill(0)
            envelope.iv.fill(0)
        }
    }
}
