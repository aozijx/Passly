package com.aozijx.passly.core.auth.authconstants

object AuthLockConstants {
    /** 用户无操作后自动锁定的最短时间（毫秒），不可低于此值 */
    const val MIN_LOCK_TIMEOUT_MS: Long = 5_000L

    /** 用户无操作后自动锁定的默认时间（毫秒），即 1 分钟 */
    const val DEFAULT_LOCK_TIMEOUT_MS: Long = 60_000L

    /** 应用密码连续错误后的最短锁定时间（毫秒），即 30 秒 */
    const val MIN_APP_PASSWORD_LOCKOUT_MS: Long = 30_000L

    /** 应用密码连续错误次数上限，达到后触发锁定 */
    const val APP_PASSWORD_MAX_FAILED_ATTEMPTS: Int = 5
}