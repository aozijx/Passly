package com.aozijx.passly.security.envelope

/**
 * KDF（密钥派生函数）参数。
 *
 * 通过 algorithm + version 组合支持 KDF 升级：
 * - 同一 algorithm 的不同 version 可拥有不同的参数集
 * - 信封持久化时记下 version，旧版本信封可继续用旧参数解密
 * - 创建新信封时使用当前最新版本
 */
data class KdfParams(
    /** KDF 算法标识 */
    val algorithm: KdfAlgorithm,
    /** 算法版本，用于参数升级兼容 */
    val version: Int = 1,
    /** 盐值（16 bytes） */
    val salt: ByteArray,
    /** 迭代次数（PBKDF2）或 t_cost（Argon2id） */
    val iterations: Int,
    /** 内存消耗（KB），仅 Argon2id 使用 */
    val memoryKb: Int = 0,
    /** 并行度，仅 Argon2id 使用 */
    val parallelism: Int = 0
)

/**
 * KDF 算法标识（内联值类）。
 *
 * 与 [EnvelopeType] 同理，使用字符串而非 enum，允许未来新增算法
 * 无需重新编译核心模块。
 *
 * 如需升级参数（如 Argon2id v1 → v2），通过 [KdfParams.version] 字段区分，
 * 而非新建算法标识。
 */
@JvmInline
value class KdfAlgorithm(val value: String) {
    companion object {
        /** 无 KDF，密钥由 Android Keystore 直接提供 */
        val NONE = KdfAlgorithm("none")

        /** Argon2id，用于应用密码 */
        val ARGON2ID = KdfAlgorithm("argon2id")

        /** PBKDF2-HMAC-SHA256，用于恢复码（保证跨平台兼容性） */
        val PBKDF2 = KdfAlgorithm("pbkdf2")
    }
}