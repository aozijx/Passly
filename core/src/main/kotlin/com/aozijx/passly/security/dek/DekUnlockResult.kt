package com.aozijx.passly.security.dek

/**
 * DEK 解锁结果。
 */
sealed interface DekUnlockResult {
    /** 解锁成功 */
    data object Success : DekUnlockResult

    /** 解锁失败 */
    data class Failed(val reason: DekUnlockError) : DekUnlockResult
}

/**
 * 解锁失败原因。
 */
enum class DekUnlockError {
    /** 认证失败（密码/生物识别错误） */
    AUTH_FAILED,

    /** DEK 校验失败（VerificationTag 不匹配） */
    DEK_VERIFY_FAILED,

    /** Envelope 数据损坏或丢失 */
    ENVELOPE_CORRUPTED,

    /** KDF 派生错误 */
    KDF_ERROR,

    /** 数据库损坏 */
    DATABASE_CORRUPTED,

    /** 未知错误 */
    UNKNOWN
}