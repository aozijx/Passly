package com.aozijx.passly.domain.failure

/**
 * 预注册错误代码。
 *
 * 格式：`[DOMAIN]_[ERROR_NAME]`，全大写 UPPER_SNAKE。
 */
object FailureCode {
    // ============================== 认证 ==============================
    const val AUTH_CREDENTIAL_INCORRECT = "AUTH_CREDENTIAL_INCORRECT"
    const val AUTH_SESSION_EXPIRED = "AUTH_SESSION_EXPIRED"
    const val AUTH_BIOMETRIC_UNAVAILABLE = "AUTH_BIOMETRIC_UNAVAILABLE"
    const val AUTH_LOCKED_OUT = "AUTH_LOCKED_OUT"
    const val AUTH_RECOVERY_INVALID = "AUTH_RECOVERY_INVALID"
    const val AUTH_MASTER_PASSWORD_WEAK = "AUTH_MASTER_PASSWORD_WEAK"
}
